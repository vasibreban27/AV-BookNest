package com.avbooknest.order.dto;

import com.avbooknest.order.model.Order;
import com.avbooknest.order.model.OrderStatus;
import com.avbooknest.shipment.dto.ShipmentResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
    Long id,
    String orderNumber,
    OrderStatus status,
    List<OrderItemResponse> items,
    List<ShipmentResponse> shipments,
    BigDecimal subtotal,
    BigDecimal shippingCost,
    BigDecimal totalAmount,
    String currency,
    Instant placedAt,
    PaymentResponse payment) {
  public static OrderResponse from(
      Order order, PaymentResponse payment, List<ShipmentResponse> shipments) {
    return new OrderResponse(
        order.getId(),
        order.getOrderNumber(),
        order.getStatus(),
        order.getItems().stream().map(OrderItemResponse::from).toList(),
        shipments,
        order.getSubtotal(),
        order.getShippingCost(),
        order.getTotalAmount(),
        order.getCurrency(),
        order.getPlacedAt(),
        payment);
  }
}
