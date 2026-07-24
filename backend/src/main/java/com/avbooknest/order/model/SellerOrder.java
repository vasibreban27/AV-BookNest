package com.avbooknest.order.model;

import com.avbooknest.auth.model.User;
import com.avbooknest.shipment.model.Shipment;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "seller_orders")
public class SellerOrder {
  public static final BigDecimal COMMISSION_RATE = new BigDecimal("5.00");
  public static final Duration ACCEPTANCE_WINDOW = Duration.ofHours(24);
  public static final Duration DROPOFF_WINDOW = Duration.ofHours(48);

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "seller_id", nullable = false)
  private User seller;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private SellerOrderStatus status;

  @Column(name = "item_subtotal", nullable = false, precision = 12, scale = 2)
  private BigDecimal itemSubtotal;

  @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
  private BigDecimal commissionRate;

  @Column(name = "commission_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal commissionAmount;

  @Column(name = "seller_proceeds", nullable = false, precision = 12, scale = 2)
  private BigDecimal sellerProceeds;

  @Column(name = "shipping_cost", nullable = false, precision = 12, scale = 2)
  private BigDecimal shippingCost;

  @Column(name = "accept_by", nullable = false)
  private Instant acceptBy;

  @Column(name = "dropoff_by")
  private Instant dropoffBy;

  @Column(name = "accepted_at")
  private Instant acceptedAt;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @Column(name = "fulfilled_at")
  private Instant fulfilledAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "sellerOrder")
  private List<OrderItem> items = new ArrayList<>();

  @OneToOne(mappedBy = "sellerOrder", cascade = CascadeType.ALL, orphanRemoval = true)
  private Shipment shipment;

  protected SellerOrder() {}

  private SellerOrder(Builder builder) {
    id = builder.id;
    order = builder.order;
    seller = builder.seller;
    status = builder.status;
    itemSubtotal = builder.itemSubtotal;
    commissionRate = builder.commissionRate;
    commissionAmount = builder.commissionAmount;
    sellerProceeds = builder.sellerProceeds;
    shippingCost = builder.shippingCost;
    acceptBy = builder.acceptBy;
    dropoffBy = builder.dropoffBy;
    acceptedAt = builder.acceptedAt;
    cancelledAt = builder.cancelledAt;
    fulfilledAt = builder.fulfilledAt;
    createdAt = builder.createdAt;
    updatedAt = builder.updatedAt;
  }

  public Long getId() {
    return id;
  }

  public Order getOrder() {
    return order;
  }

  public User getSeller() {
    return seller;
  }

  public SellerOrderStatus getStatus() {
    return status;
  }

  public BigDecimal getItemSubtotal() {
    return itemSubtotal;
  }

  public BigDecimal getCommissionRate() {
    return commissionRate;
  }

  public BigDecimal getCommissionAmount() {
    return commissionAmount;
  }

  public BigDecimal getSellerProceeds() {
    return sellerProceeds;
  }

  public BigDecimal getShippingCost() {
    return shippingCost;
  }

  public Instant getAcceptBy() {
    return acceptBy;
  }

  public Instant getDropoffBy() {
    return dropoffBy;
  }

  public Instant getAcceptedAt() {
    return acceptedAt;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }

  public Instant getFulfilledAt() {
    return fulfilledAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public List<OrderItem> getItems() {
    return items;
  }

  public Shipment getShipment() {
    return shipment;
  }

  public void addItem(OrderItem item) {
    items.add(item);
    item.assignSellerOrder(this);
  }

  public void assignShipment(Shipment value) {
    shipment = value;
  }

  public void accept(Instant now) {
    status = SellerOrderStatus.ACCEPTED;
    acceptedAt = now;
    dropoffBy = now.plus(DROPOFF_WINDOW);
    updatedAt = now;
    shipment.queueAwb(now);
  }

  public void cancel(Instant now) {
    status = SellerOrderStatus.CANCELLED;
    cancelledAt = now;
    updatedAt = now;
    shipment.cancel(now);
  }

  public void fulfill(Instant now) {
    status = SellerOrderStatus.FULFILLED;
    fulfilledAt = now;
    updatedAt = now;
  }

  public boolean acceptanceExpired(Instant now) {
    return !now.isBefore(acceptBy);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Long id;
    private Order order;
    private User seller;
    private SellerOrderStatus status;
    private BigDecimal itemSubtotal;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private BigDecimal sellerProceeds;
    private BigDecimal shippingCost;
    private Instant acceptBy;
    private Instant dropoffBy;
    private Instant acceptedAt;
    private Instant cancelledAt;
    private Instant fulfilledAt;
    private Instant createdAt;
    private Instant updatedAt;

    public Builder id(Long value) {
      id = value;
      return this;
    }

    public Builder order(Order value) {
      order = value;
      return this;
    }

    public Builder seller(User value) {
      seller = value;
      return this;
    }

    public Builder status(SellerOrderStatus value) {
      status = value;
      return this;
    }

    public Builder itemSubtotal(BigDecimal value) {
      itemSubtotal = value;
      return this;
    }

    public Builder commissionRate(BigDecimal value) {
      commissionRate = value;
      return this;
    }

    public Builder commissionAmount(BigDecimal value) {
      commissionAmount = value;
      return this;
    }

    public Builder sellerProceeds(BigDecimal value) {
      sellerProceeds = value;
      return this;
    }

    public Builder shippingCost(BigDecimal value) {
      shippingCost = value;
      return this;
    }

    public Builder acceptBy(Instant value) {
      acceptBy = value;
      return this;
    }

    public Builder dropoffBy(Instant value) {
      dropoffBy = value;
      return this;
    }

    public Builder acceptedAt(Instant value) {
      acceptedAt = value;
      return this;
    }

    public Builder cancelledAt(Instant value) {
      cancelledAt = value;
      return this;
    }

    public Builder fulfilledAt(Instant value) {
      fulfilledAt = value;
      return this;
    }

    public Builder createdAt(Instant value) {
      createdAt = value;
      return this;
    }

    public Builder updatedAt(Instant value) {
      updatedAt = value;
      return this;
    }

    public SellerOrder build() {
      return new SellerOrder(this);
    }
  }
}
