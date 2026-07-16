package com.avbooknest.wishlist.model;

import java.io.Serializable;
import java.util.Objects;

public class WishlistItemId implements Serializable {
  private Long user;
  private Long book;

  protected WishlistItemId() {}

  public WishlistItemId(Long user, Long book) {
    this.user = user;
    this.book = book;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof WishlistItemId that)) {
      return false;
    }
    return Objects.equals(user, that.user) && Objects.equals(book, that.book);
  }

  @Override
  public int hashCode() {
    return Objects.hash(user, book);
  }
}
