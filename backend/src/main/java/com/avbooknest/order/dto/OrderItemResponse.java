package com.avbooknest.order.dto;

import com.avbooknest.order.model.OrderItem;
import java.math.BigDecimal;

public record OrderItemResponse(
    Long id,
    Long bookId,
    Long sellerId,
    String title,
    String author,
    String isbn,
    BigDecimal unitPrice,
    int quantity) {
  public static OrderItemResponse from(OrderItem item) {
    return new OrderItemResponse(
        item.getId(),
        item.getBook() == null ? null : item.getBook().getId(),
        item.getSeller().getId(),
        item.getTitle(),
        item.getAuthor(),
        item.getIsbn(),
        item.getUnitPrice(),
        item.getQuantity());
  }
}
