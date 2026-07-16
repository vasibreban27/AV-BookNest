package com.avbooknest.shipment.dto;

import com.avbooknest.order.dto.OrderItemResponse;
import com.avbooknest.shipment.model.Shipment;
import com.avbooknest.shipment.model.ShipmentStatus;
import java.math.BigDecimal;
import java.util.List;

public record ShipmentResponse(
    Long id,
    Long sellerId,
    String sellerName,
    String easyboxId,
    String easyboxName,
    String trackingNumber,
    ShipmentStatus status,
    BigDecimal codAmount,
    List<OrderItemResponse> items) {

  public static ShipmentResponse from(Shipment shipment) {
    return new ShipmentResponse(
        shipment.getId(),
        shipment.getSeller().getId(),
        shipment.getSeller().getFirstName() + " " + shipment.getSeller().getLastName(),
        shipment.getEasyboxId(),
        shipment.getEasyboxName(),
        shipment.getTrackingNumber(),
        shipment.getStatus(),
        shipment.getCodAmount(),
        shipment.getItems().stream()
            .map(item -> OrderItemResponse.from(item.getOrderItem()))
            .toList());
  }
}
