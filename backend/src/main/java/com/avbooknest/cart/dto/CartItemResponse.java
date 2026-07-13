package com.avbooknest.cart.dto;

import com.avbooknest.book.dto.BookResponse;
import com.avbooknest.cart.model.CartItem;
import java.time.Instant;

public record CartItemResponse(Long id, BookResponse book, Instant addedAt) {
  public static CartItemResponse from(CartItem item) {
    return new CartItemResponse(item.getId(), BookResponse.from(item.getBook()), item.getAddedAt());
  }
}
