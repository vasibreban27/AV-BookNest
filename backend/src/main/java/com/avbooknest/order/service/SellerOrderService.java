package com.avbooknest.order.service;

import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.book.model.Book;
import com.avbooknest.common.exception.ConflictException;
import com.avbooknest.common.exception.NotFoundException;
import com.avbooknest.integration.model.IntegrationEvent;
import com.avbooknest.integration.repository.IntegrationEventRepository;
import com.avbooknest.notification.model.NotificationType;
import com.avbooknest.notification.service.NotificationService;
import com.avbooknest.order.dto.SellerOrderResponse;
import com.avbooknest.order.model.Order;
import com.avbooknest.order.model.OrderItem;
import com.avbooknest.order.model.OrderStatus;
import com.avbooknest.order.model.SellerOrder;
import com.avbooknest.order.model.SellerOrderStatus;
import com.avbooknest.order.repository.SellerOrderRepository;
import com.avbooknest.shipment.model.ShipmentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SellerOrderService {
  private final SellerOrderRepository sellerOrderRepository;
  private final UserRepository userRepository;
  private final IntegrationEventRepository integrationEventRepository;
  private final NotificationService notificationService;

  public SellerOrderService(
      SellerOrderRepository sellerOrderRepository,
      UserRepository userRepository,
      IntegrationEventRepository integrationEventRepository,
      NotificationService notificationService) {
    this.sellerOrderRepository = sellerOrderRepository;
    this.userRepository = userRepository;
    this.integrationEventRepository = integrationEventRepository;
    this.notificationService = notificationService;
  }

  @Transactional(readOnly = true)
  public List<SellerOrderResponse> listForSeller(String email) {
    return sellerOrderRepository
        .findAllBySellerIdOrderByCreatedAtDesc(currentUser(email).getId())
        .stream()
        .map(SellerOrderResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<SellerOrderResponse> listForOrder(Long orderId) {
    return sellerOrderRepository.findAllByOrderId(orderId).stream()
        .map(SellerOrderResponse::from)
        .toList();
  }

  public SellerOrderResponse accept(Long sellerOrderId, String email) {
    SellerOrder sellerOrder = lockForSeller(sellerOrderId, email);
    Instant now = Instant.now();
    if (sellerOrder.getStatus() != SellerOrderStatus.AWAITING_SELLER) {
      throw new ConflictException("This sale can no longer be accepted");
    }
    if (sellerOrder.acceptanceExpired(now)) {
      throw new ConflictException("The 24 hour acceptance window has expired");
    }
    sellerOrder.accept(now);
    integrationEventRepository.save(
        IntegrationEvent.pending(
            "SELLER_ORDER",
            sellerOrder.getId(),
            "SAMEDAY_CREATE_AWB",
            "{\"sellerOrderId\":"
                + sellerOrder.getId()
                + ",\"packageSize\":\""
                + sellerOrder.getShipment().getPackageSize()
                + "\"}",
            now));
    notificationService.create(
        sellerOrder.getOrder().getBuyer(),
        NotificationType.SHIPMENT_ACCEPTED,
        "Sale accepted",
        "The seller accepted shipment for order "
            + sellerOrder.getOrder().getOrderNumber()
            + ". Sameday AWB generation is pending.");
    syncOrder(sellerOrder.getOrder());
    return SellerOrderResponse.from(sellerOrder);
  }

  public SellerOrderResponse cancel(Long sellerOrderId, String email) {
    SellerOrder sellerOrder = lockForSeller(sellerOrderId, email);
    ShipmentStatus shipmentStatus = sellerOrder.getShipment().getStatus();
    if (sellerOrder.getStatus() == SellerOrderStatus.FULFILLED
        || shipmentStatus == ShipmentStatus.IN_TRANSIT
        || shipmentStatus == ShipmentStatus.DELIVERED) {
      throw new ConflictException("This sale can no longer be cancelled");
    }
    if (sellerOrder.getStatus() != SellerOrderStatus.CANCELLED) {
      Instant now = Instant.now();
      cancelSellerOrder(sellerOrder, now);
      integrationEventRepository.save(
          IntegrationEvent.pending(
              "SELLER_ORDER",
              sellerOrder.getId(),
              "STRIPE_REFUND_SELLER_ORDER",
              "{\"sellerOrderId\":" + sellerOrder.getId() + "}",
              now));
      notificationService.create(
          sellerOrder.getOrder().getBuyer(),
          NotificationType.ORDER_CANCELLED,
          "Sale cancelled",
          "A seller cancelled part of order " + sellerOrder.getOrder().getOrderNumber() + ".");
      syncOrder(sellerOrder.getOrder());
    }
    return SellerOrderResponse.from(sellerOrder);
  }

  public void cancelOrder(Order order) {
    List<SellerOrder> sellerOrders = sellerOrderRepository.findAllByOrderIdForUpdate(order.getId());
    boolean cannotCancel =
        sellerOrders.stream()
            .map(SellerOrder::getShipment)
            .anyMatch(
                shipment ->
                    shipment.getStatus() == ShipmentStatus.IN_TRANSIT
                        || shipment.getStatus() == ShipmentStatus.DELIVERED);
    if (cannotCancel) {
      throw new ConflictException("The order can no longer be cancelled after drop-off");
    }
    Instant now = Instant.now();
    sellerOrders.stream()
        .filter(sellerOrder -> sellerOrder.getStatus() != SellerOrderStatus.CANCELLED)
        .forEach(
            sellerOrder -> {
              cancelSellerOrder(sellerOrder, now);
              notificationService.create(
                  sellerOrder.getSeller(),
                  NotificationType.ORDER_CANCELLED,
                  "Order cancelled",
                  "Order " + order.getOrderNumber() + " was cancelled by the buyer.");
            });
    integrationEventRepository.save(
        IntegrationEvent.pending(
            "ORDER",
            order.getId(),
            "STRIPE_REFUND_ORDER",
            "{\"orderId\":" + order.getId() + "}",
            now));
    syncOrder(order, sellerOrders);
  }

  private SellerOrder lockForSeller(Long sellerOrderId, String email) {
    return sellerOrderRepository
        .findByIdAndSellerIdForUpdate(sellerOrderId, currentUser(email).getId())
        .orElseThrow(() -> new NotFoundException("Sale not found"));
  }

  private void cancelSellerOrder(SellerOrder sellerOrder, Instant now) {
    sellerOrder.getItems().stream()
        .map(OrderItem::getBook)
        .filter(java.util.Objects::nonNull)
        .forEach(Book::releaseReservation);
    sellerOrder.cancel(now);
  }

  private void syncOrder(Order order) {
    syncOrder(order, sellerOrderRepository.findAllByOrderIdForUpdate(order.getId()));
  }

  private void syncOrder(Order order, List<SellerOrder> sellerOrders) {
    List<SellerOrder> active =
        sellerOrders.stream()
            .filter(sellerOrder -> sellerOrder.getStatus() != SellerOrderStatus.CANCELLED)
            .toList();
    BigDecimal subtotal =
        active.stream().map(SellerOrder::getItemSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal shipping =
        active.stream().map(SellerOrder::getShippingCost).reduce(BigDecimal.ZERO, BigDecimal::add);
    OrderStatus status =
        active.isEmpty()
            ? OrderStatus.CANCELLED
            : active.stream()
                    .allMatch(sellerOrder -> sellerOrder.getStatus() == SellerOrderStatus.FULFILLED)
                ? OrderStatus.DELIVERED
                : active.stream()
                        .anyMatch(
                            sellerOrder -> sellerOrder.getStatus() == SellerOrderStatus.ACCEPTED)
                    ? OrderStatus.PROCESSING
                    : OrderStatus.PENDING;
    order.updateProgress(status, subtotal, subtotal.add(shipping));
  }

  private User currentUser(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new NotFoundException("User not found"));
  }
}
