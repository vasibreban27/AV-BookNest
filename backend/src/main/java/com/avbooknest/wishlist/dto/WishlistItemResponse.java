package com.avbooknest.wishlist.dto;

import com.avbooknest.book.dto.BookResponse;
import com.avbooknest.wishlist.model.WishlistItem;
import java.time.Instant;

public record WishlistItemResponse(BookResponse book, Instant addedAt) {
  public static WishlistItemResponse from(WishlistItem item) {
    return new WishlistItemResponse(BookResponse.from(item.getBook()), item.getCreatedAt());
  }
}
