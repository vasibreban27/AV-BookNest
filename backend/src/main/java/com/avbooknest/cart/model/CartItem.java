package com.avbooknest.cart.model;

import com.avbooknest.book.model.Book;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "cart_items")
public class CartItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cart_id", nullable = false)
  private Cart cart;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "book_id", nullable = false)
  private Book book;

  @Column(name = "added_at", nullable = false, updatable = false)
  private Instant addedAt;

  protected CartItem() {}

  private CartItem(Builder builder) {
    cart = builder.cart;
    book = builder.book;
    addedAt = builder.addedAt;
  }

  public Long getId() {
    return id;
  }

  public Cart getCart() {
    return cart;
  }

  public Book getBook() {
    return book;
  }

  public Instant getAddedAt() {
    return addedAt;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Cart cart;
    private Book book;
    private Instant addedAt;

    public Builder cart(Cart value) {
      cart = value;
      return this;
    }

    public Builder book(Book value) {
      book = value;
      return this;
    }

    public Builder addedAt(Instant value) {
      addedAt = value;
      return this;
    }

    public CartItem build() {
      return new CartItem(this);
    }
  }
}
