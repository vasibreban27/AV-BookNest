package com.avbooknest.order.dto;

import com.avbooknest.order.model.SellerOrder;
import com.avbooknest.order.model.SellerOrderStatus;
import com.avbooknest.shipment.dto.ShipmentResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SellerOrderResponse(
    Long id,
    Long orderId,
    String orderNumber,
    String buyerName,
    Long sellerId,
    String sellerName,
    SellerOrderStatus status,
    BigDecimal itemSubtotal,
    BigDecimal commissionRate,
    BigDecimal commissionAmount,
    BigDecimal sellerProceeds,
    BigDecimal shippingCost,
    Instant acceptBy,
    Instant dropoffBy,
    Instant acceptedAt,
    Instant createdAt,
    List<OrderItemResponse> items,
    ShipmentResponse shipment) {

  public static SellerOrderResponse from(SellerOrder sellerOrder) {
    return new SellerOrderResponse(
        sellerOrder.getId(),
        sellerOrder.getOrder().getId(),
        sellerOrder.getOrder().getOrderNumber(),
        sellerOrder.getOrder().getRecipientName(),
        sellerOrder.getSeller().getId(),
        sellerOrder.getSeller().getFirstName() + " " + sellerOrder.getSeller().getLastName(),
        sellerOrder.getStatus(),
        sellerOrder.getItemSubtotal(),
        sellerOrder.getCommissionRate(),
        sellerOrder.getCommissionAmount(),
        sellerOrder.getSellerProceeds(),
        sellerOrder.getShippingCost(),
        sellerOrder.getAcceptBy(),
        sellerOrder.getDropoffBy(),
        sellerOrder.getAcceptedAt(),
        sellerOrder.getCreatedAt(),
        sellerOrder.getItems().stream().map(OrderItemResponse::from).toList(),
        ShipmentResponse.from(sellerOrder.getShipment()));
  }
}
