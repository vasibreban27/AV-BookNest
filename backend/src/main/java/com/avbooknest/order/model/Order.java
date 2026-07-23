package com.avbooknest.order.model;

import com.avbooknest.auth.model.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "order_number", nullable = false, unique = true, length = 40)
  private String orderNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "buyer_id", nullable = false)
  private User buyer;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private OrderStatus status;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal subtotal;

  @Column(name = "shipping_cost", nullable = false, precision = 12, scale = 2)
  private BigDecimal shippingCost;

  @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal totalAmount;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "recipient_name", nullable = false, length = 200)
  private String recipientName;

  @Column(name = "recipient_email", nullable = false, length = 255)
  private String recipientEmail;

  @Column(name = "recipient_phone", nullable = false, length = 30)
  private String recipientPhone;

  @Column(name = "placed_at", nullable = false, updatable = false)
  private Instant placedAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItem> items = new ArrayList<>();

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<SellerOrder> sellerOrders = new ArrayList<>();

  protected Order() {}

  private Order(Builder b) {
    id = b.id;
    orderNumber = b.orderNumber;
    buyer = b.buyer;
    status = b.status;
    subtotal = b.subtotal;
    shippingCost = b.shippingCost;
    totalAmount = b.totalAmount;
    currency = b.currency;
    recipientName = b.recipientName;
    recipientEmail = b.recipientEmail;
    recipientPhone = b.recipientPhone;
    placedAt = b.placedAt;
    updatedAt = b.updatedAt;
  }

  public Long getId() {
    return id;
  }

  public String getOrderNumber() {
    return orderNumber;
  }

  public User getBuyer() {
    return buyer;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public BigDecimal getSubtotal() {
    return subtotal;
  }

  public BigDecimal getShippingCost() {
    return shippingCost;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public String getCurrency() {
    return currency;
  }

  public String getRecipientName() {
    return recipientName;
  }

  public String getRecipientEmail() {
    return recipientEmail;
  }

  public String getRecipientPhone() {
    return recipientPhone;
  }

  public Instant getPlacedAt() {
    return placedAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public List<OrderItem> getItems() {
    return items;
  }

  public List<SellerOrder> getSellerOrders() {
    return sellerOrders;
  }

  public void addItem(OrderItem item) {
    items.add(item);
  }

  public void addSellerOrder(SellerOrder sellerOrder) {
    sellerOrders.add(sellerOrder);
  }

  public void updateProgress(
      OrderStatus newStatus, BigDecimal newSubtotal, BigDecimal newTotalAmount) {
    status = newStatus;
    subtotal = newSubtotal;
    totalAmount = newTotalAmount;
    updatedAt = Instant.now();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Long id;
    private String orderNumber;
    private User buyer;
    private OrderStatus status;
    private BigDecimal subtotal;
    private BigDecimal shippingCost;
    private BigDecimal totalAmount;
    private String currency;
    private String recipientName;
    private String recipientEmail;
    private String recipientPhone;
    private Instant placedAt;
    private Instant updatedAt;

    public Builder id(Long v) {
      id = v;
      return this;
    }

    public Builder orderNumber(String v) {
      orderNumber = v;
      return this;
    }

    public Builder buyer(User v) {
      buyer = v;
      return this;
    }

    public Builder status(OrderStatus v) {
      status = v;
      return this;
    }

    public Builder subtotal(BigDecimal v) {
      subtotal = v;
      return this;
    }

    public Builder shippingCost(BigDecimal v) {
      shippingCost = v;
      return this;
    }

    public Builder totalAmount(BigDecimal v) {
      totalAmount = v;
      return this;
    }

    public Builder currency(String v) {
      currency = v;
      return this;
    }

    public Builder recipientName(String value) {
      recipientName = value;
      return this;
    }

    public Builder recipientEmail(String value) {
      recipientEmail = value;
      return this;
    }

    public Builder recipientPhone(String value) {
      recipientPhone = value;
      return this;
    }

    public Builder placedAt(Instant v) {
      placedAt = v;
      return this;
    }

    public Builder updatedAt(Instant v) {
      updatedAt = v;
      return this;
    }

    public Order build() {
      return new Order(this);
    }
  }
}
