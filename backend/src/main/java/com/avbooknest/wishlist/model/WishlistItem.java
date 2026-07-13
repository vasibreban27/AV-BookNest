package com.avbooknest.wishlist.model;

import com.avbooknest.auth.model.User;
import com.avbooknest.book.model.Book;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "favorites")
@IdClass(WishlistItemId.class)
public class WishlistItem {
  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Id
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "book_id", nullable = false)
  private Book book;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected WishlistItem() {}

  private WishlistItem(Builder builder) {
    user = builder.user;
    book = builder.book;
    createdAt = builder.createdAt;
  }

  public User getUser() {
    return user;
  }

  public Book getBook() {
    return book;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private User user;
    private Book book;
    private Instant createdAt;

    public Builder user(User value) {
      user = value;
      return this;
    }

    public Builder book(Book value) {
      book = value;
      return this;
    }

    public Builder createdAt(Instant value) {
      createdAt = value;
      return this;
    }

    public WishlistItem build() {
      return new WishlistItem(this);
    }
  }
}
