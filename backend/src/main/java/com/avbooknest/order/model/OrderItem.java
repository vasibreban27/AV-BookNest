package com.avbooknest.order.model;

import com.avbooknest.auth.model.User;
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
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_items")
public class OrderItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "book_id")
  private Book book;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "seller_id", nullable = false)
  private User seller;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "seller_order_id", nullable = false)
  private SellerOrder sellerOrder;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(nullable = false, length = 255)
  private String author;

  @Column(length = 20)
  private String isbn;

  @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
  private BigDecimal unitPrice;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected OrderItem() {}

  private OrderItem(Builder b) {
    id = b.id;
    order = b.order;
    book = b.book;
    seller = b.seller;
    title = b.title;
    author = b.author;
    isbn = b.isbn;
    unitPrice = b.unitPrice;
    quantity = b.quantity;
    createdAt = b.createdAt;
  }

  public Long getId() {
    return id;
  }

  public Order getOrder() {
    return order;
  }

  public Book getBook() {
    return book;
  }

  public User getSeller() {
    return seller;
  }

  public SellerOrder getSellerOrder() {
    return sellerOrder;
  }

  void assignSellerOrder(SellerOrder value) {
    sellerOrder = value;
  }

  public String getTitle() {
    return title;
  }

  public String getAuthor() {
    return author;
  }

  public String getIsbn() {
    return isbn;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public int getQuantity() {
    return quantity;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Long id;
    private Order order;
    private Book book;
    private User seller;
    private String title;
    private String author;
    private String isbn;
    private BigDecimal unitPrice;
    private int quantity;
    private Instant createdAt;

    public Builder id(Long v) {
      id = v;
      return this;
    }

    public Builder order(Order v) {
      order = v;
      return this;
    }

    public Builder book(Book v) {
      book = v;
      return this;
    }

    public Builder seller(User v) {
      seller = v;
      return this;
    }

    public Builder title(String v) {
      title = v;
      return this;
    }

    public Builder author(String v) {
      author = v;
      return this;
    }

    public Builder isbn(String v) {
      isbn = v;
      return this;
    }

    public Builder unitPrice(BigDecimal v) {
      unitPrice = v;
      return this;
    }

    public Builder quantity(int v) {
      quantity = v;
      return this;
    }

    public Builder createdAt(Instant v) {
      createdAt = v;
      return this;
    }

    public OrderItem build() {
      return new OrderItem(this);
    }
  }
}
