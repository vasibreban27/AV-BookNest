package com.avbooknest.order.service;

import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.book.model.Book;
import com.avbooknest.common.exception.ConflictException;
import com.avbooknest.common.exception.NotFoundException;
import com.avbooknest.integration.model.IntegrationEvent;
import com.avbooknest.integration.model.IntegrationEventStatus;
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
            + ". Sameday AWB generation is pending.",
        "/orders/" + sellerOrder.getOrder().getId());
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
          "A seller cancelled part of order " + sellerOrder.getOrder().getOrderNumber() + ".",
          "/orders/" + sellerOrder.getOrder().getId());
      syncOrder(sellerOrder.getOrder());
    }
    return SellerOrderResponse.from(sellerOrder);
  }

  @Transactional(readOnly = true)
  public List<Long> expiredAcceptanceIds(Instant now) {
    return sellerOrderRepository.findExpiredAcceptanceIds(now);
  }

  @Transactional(readOnly = true)
  public List<Long> expiredDropoffIds(Instant now) {
    return sellerOrderRepository.findExpiredDropoffIds(now);
  }

  public void expireAcceptance(Long sellerOrderId, Instant now) {
    SellerOrder sellerOrder = sellerOrderRepository.findByIdForUpdate(sellerOrderId).orElse(null);
    if (sellerOrder == null
        || sellerOrder.getStatus() != SellerOrderStatus.AWAITING_SELLER
        || !sellerOrder.acceptanceExpired(now)) {
      return;
    }
    cancelForDeadline(
        sellerOrder, now, "Sale expired", "The seller did not accept this sale within 24 hours.");
  }

  public void expireDropoff(Long sellerOrderId, Instant now) {
    SellerOrder sellerOrder = sellerOrderRepository.findByIdForUpdate(sellerOrderId).orElse(null);
    if (sellerOrder == null
        || sellerOrder.getStatus() != SellerOrderStatus.ACCEPTED
        || !sellerOrder.dropoffExpired(now)
        || sellerOrder.getShipment().getStatus() == ShipmentStatus.IN_TRANSIT
        || sellerOrder.getShipment().getStatus() == ShipmentStatus.DELIVERED) {
      return;
    }
    cancelForDeadline(
        sellerOrder,
        now,
        "Drop-off deadline expired",
        "The seller did not hand the parcel to Sameday before the deadline.");
  }

  public void handleShipmentFailure(Long sellerOrderId, ShipmentStatus status, Instant now) {
    SellerOrder sellerOrder = sellerOrderRepository.findByIdForUpdate(sellerOrderId).orElse(null);
    if (sellerOrder == null || sellerOrder.getStatus() == SellerOrderStatus.CANCELLED) {
      return;
    }
    sellerOrder.getItems().stream()
        .map(OrderItem::getBook)
        .filter(java.util.Objects::nonNull)
        .forEach(Book::releaseReservation);
    sellerOrder.cancelPreservingShipment(now);
    queueSellerRefund(sellerOrder, now);
    String label = status == ShipmentStatus.LOST ? "lost" : status.name().toLowerCase();
    notificationService.create(
        sellerOrder.getOrder().getBuyer(),
        NotificationType.SHIPMENT_PROBLEM,
        "Shipment problem",
        "The parcel for "
            + sellerOrder.getOrder().getOrderNumber()
            + " was marked "
            + label
            + ". A Stripe refund was requested.",
        "/orders/" + sellerOrder.getOrder().getId());
    notificationService.create(
        sellerOrder.getSeller(),
        NotificationType.SHIPMENT_PROBLEM,
        "Shipment problem",
        "The parcel for " + sellerOrder.getOrder().getOrderNumber() + " was marked " + label + ".",
        "/sales");
    syncOrder(sellerOrder.getOrder());
  }

  public SellerOrderResponse reportIssue(
      Long orderId, Long sellerOrderId, String email, String reason) {
    User buyer = currentUser(email);
    SellerOrder sellerOrder =
        sellerOrderRepository
            .findForBuyerIssue(sellerOrderId, orderId, buyer.getId())
            .orElseThrow(() -> new NotFoundException("Sale not found"));
    Instant now = Instant.now();
    if (!sellerOrder.canReportIssue(now)) {
      throw new ConflictException("The 24 hour issue reporting window is closed");
    }
    sellerOrder.openIssue(reason.trim(), now);
    notificationService.create(
        sellerOrder.getSeller(),
        NotificationType.ORDER_ISSUE_OPENED,
        "Buyer reported a problem",
        "A problem was reported for "
            + sellerOrder.getOrder().getOrderNumber()
            + ". The Stripe payout is on hold.",
        "/sales");
    return SellerOrderResponse.from(sellerOrder);
  }

  public SellerOrderResponse resolveIssue(Long orderId, Long sellerOrderId, String email) {
    User buyer = currentUser(email);
    SellerOrder sellerOrder =
        sellerOrderRepository
            .findForBuyerIssue(sellerOrderId, orderId, buyer.getId())
            .orElseThrow(() -> new NotFoundException("Sale not found"));
    if (!sellerOrder.hasOpenIssue()) {
      throw new ConflictException("This sale has no open issue");
    }
    Instant now = Instant.now();
    sellerOrder.resolveIssue(now);
    integrationEventRepository
        .findFirstByAggregateTypeAndAggregateIdAndEventTypeAndStatusOrderByCreatedAtDesc(
            "SELLER_TRANSFER",
            sellerOrder.getId(),
            "STRIPE_CREATE_TRANSFER",
            IntegrationEventStatus.PENDING)
        .ifPresent(event -> event.deferUntil(now));
    notificationService.create(
        sellerOrder.getSeller(),
        NotificationType.ORDER_ISSUE_RESOLVED,
        "Buyer resolved the problem",
        "The payout hold for " + sellerOrder.getOrder().getOrderNumber() + " was removed.",
        "/sales");
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
                  "Order " + order.getOrderNumber() + " was cancelled by the buyer.",
                  "/sales");
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

  public void cancelUnpaidOrder(Order order) {
    List<SellerOrder> sellerOrders = sellerOrderRepository.findAllByOrderIdForUpdate(order.getId());
    Instant now = Instant.now();
    sellerOrders.stream()
        .filter(sellerOrder -> sellerOrder.getStatus() != SellerOrderStatus.CANCELLED)
        .forEach(sellerOrder -> cancelSellerOrder(sellerOrder, now));
    syncOrder(order, sellerOrders);
  }

  public void activateAfterPayment(Order order, Instant paidAt) {
    List<SellerOrder> sellerOrders = sellerOrderRepository.findAllByOrderIdForUpdate(order.getId());
    sellerOrders.forEach(sellerOrder -> sellerOrder.activateAfterPayment(paidAt));
    order.markPaid(paidAt);
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

  private void cancelForDeadline(
      SellerOrder sellerOrder, Instant now, String title, String message) {
    cancelSellerOrder(sellerOrder, now);
    queueSellerRefund(sellerOrder, now);
    notificationService.create(
        sellerOrder.getOrder().getBuyer(),
        NotificationType.ORDER_CANCELLED,
        title,
        message + " A Stripe refund was requested.",
        "/orders/" + sellerOrder.getOrder().getId());
    notificationService.create(
        sellerOrder.getSeller(), NotificationType.SELLER_ACTION_REQUIRED, title, message, "/sales");
    syncOrder(sellerOrder.getOrder());
  }

  private void queueSellerRefund(SellerOrder sellerOrder, Instant now) {
    integrationEventRepository.save(
        IntegrationEvent.pending(
            "SELLER_ORDER",
            sellerOrder.getId(),
            "STRIPE_REFUND_SELLER_ORDER",
            "{\"sellerOrderId\":" + sellerOrder.getId() + "}",
            now));
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
                    .anyMatch(
                        sellerOrder -> sellerOrder.getStatus() == SellerOrderStatus.PAYMENT_PENDING)
                ? OrderStatus.PENDING
                : active.stream()
                        .allMatch(
                            sellerOrder -> sellerOrder.getStatus() == SellerOrderStatus.FULFILLED)
                    ? OrderStatus.DELIVERED
                    : active.stream()
                            .anyMatch(
                                sellerOrder ->
                                    sellerOrder.getStatus() == SellerOrderStatus.ACCEPTED)
                        ? OrderStatus.PROCESSING
                        : OrderStatus.PAID;
    order.updateProgress(status, subtotal, subtotal.add(shipping));
  }

  private User currentUser(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new NotFoundException("User not found"));
  }
}
