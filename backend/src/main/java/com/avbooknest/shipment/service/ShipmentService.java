package com.avbooknest.shipment.service;

import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.common.exception.NotFoundException;
import com.avbooknest.order.model.Order;
import com.avbooknest.order.model.OrderItem;
import com.avbooknest.shipment.dto.ShipmentResponse;
import com.avbooknest.shipment.model.Shipment;
import com.avbooknest.shipment.model.ShipmentItem;
import com.avbooknest.shipment.model.ShipmentStatus;
import com.avbooknest.shipment.repository.ShipmentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShipmentService {

  private final ShipmentRepository shipmentRepository;
  private final UserRepository userRepository;

  public ShipmentService(ShipmentRepository shipmentRepository, UserRepository userRepository) {
    this.shipmentRepository = shipmentRepository;
    this.userRepository = userRepository;
  }

  public List<ShipmentResponse> createForOrder(Order order, String easyboxId, String easyboxName) {
    Map<User, List<OrderItem>> itemsBySeller =
        order.getItems().stream().collect(Collectors.groupingBy(OrderItem::getSeller));
    Instant now = Instant.now();

    List<Shipment> shipments =
        itemsBySeller.entrySet().stream()
            .map(
                entry ->
                    createShipment(
                        order, entry.getKey(), entry.getValue(), easyboxId, easyboxName, now))
            .toList();

    return shipmentRepository.saveAll(shipments).stream().map(ShipmentResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public List<ShipmentResponse> listForOrder(Long orderId) {
    return shipmentRepository.findAllByOrderId(orderId).stream()
        .map(ShipmentResponse::from)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ShipmentResponse> listForSeller(String email) {
    return shipmentRepository
        .findAllBySellerIdOrderByCreatedAtDesc(currentUser(email).getId())
        .stream()
        .map(ShipmentResponse::from)
        .toList();
  }

  public ShipmentResponse registerTrackingNumber(
      Long shipmentId, String trackingNumber, String email) {
    Shipment shipment =
        shipmentRepository
            .findByIdAndSellerId(shipmentId, currentUser(email).getId())
            .orElseThrow(() -> new NotFoundException("Shipment not found"));
    shipment.registerTrackingNumber(trackingNumber.trim());
    return ShipmentResponse.from(shipment);
  }

  private Shipment createShipment(
      Order order,
      User seller,
      List<OrderItem> orderItems,
      String easyboxId,
      String easyboxName,
      Instant now) {
    BigDecimal codAmount =
        orderItems.stream()
            .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    Shipment shipment =
        Shipment.builder()
            .order(order)
            .seller(seller)
            .easyboxId(easyboxId)
            .easyboxName(easyboxName)
            .status(ShipmentStatus.AWAITING_SELLER)
            .codAmount(codAmount)
            .createdAt(now)
            .updatedAt(now)
            .build();
    orderItems.forEach(orderItem -> shipment.addItem(ShipmentItem.of(shipment, orderItem)));
    return shipment;
  }

  private User currentUser(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new NotFoundException("User not found"));
  }
}
