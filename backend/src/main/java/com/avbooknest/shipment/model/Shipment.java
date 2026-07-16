package com.avbooknest.shipment.model;

import com.avbooknest.auth.model.User;
import com.avbooknest.order.model.Order;
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
@Table(name = "shipments")
public class Shipment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "seller_id", nullable = false)
  private User seller;

  @Column(name = "easybox_id", nullable = false, length = 100)
  private String easyboxId;

  @Column(name = "easybox_name", nullable = false, length = 255)
  private String easyboxName;

  @Column(name = "tracking_number", unique = true, length = 100)
  private String trackingNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ShipmentStatus status;

  @Column(name = "cod_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal codAmount;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ShipmentItem> items = new ArrayList<>();

  protected Shipment() {}

  private Shipment(Builder builder) {
    id = builder.id;
    order = builder.order;
    seller = builder.seller;
    easyboxId = builder.easyboxId;
    easyboxName = builder.easyboxName;
    trackingNumber = builder.trackingNumber;
    status = builder.status;
    codAmount = builder.codAmount;
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

  public String getEasyboxId() {
    return easyboxId;
  }

  public String getEasyboxName() {
    return easyboxName;
  }

  public String getTrackingNumber() {
    return trackingNumber;
  }

  public ShipmentStatus getStatus() {
    return status;
  }

  public BigDecimal getCodAmount() {
    return codAmount;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public List<ShipmentItem> getItems() {
    return items;
  }

  public void addItem(ShipmentItem item) {
    items.add(item);
  }

  public void registerTrackingNumber(String value) {
    trackingNumber = value;
    status = ShipmentStatus.AWB_CREATED;
    updatedAt = Instant.now();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Long id;
    private Order order;
    private User seller;
    private String easyboxId;
    private String easyboxName;
    private String trackingNumber;
    private ShipmentStatus status;
    private BigDecimal codAmount;
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

    public Builder easyboxId(String value) {
      easyboxId = value;
      return this;
    }

    public Builder easyboxName(String value) {
      easyboxName = value;
      return this;
    }

    public Builder trackingNumber(String value) {
      trackingNumber = value;
      return this;
    }

    public Builder status(ShipmentStatus value) {
      status = value;
      return this;
    }

    public Builder codAmount(BigDecimal value) {
      codAmount = value;
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

    public Shipment build() {
      return new Shipment(this);
    }
  }
}
