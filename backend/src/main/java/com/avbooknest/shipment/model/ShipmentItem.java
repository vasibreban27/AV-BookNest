package com.avbooknest.shipment.model;

import com.avbooknest.order.model.OrderItem;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "shipment_items")
public class ShipmentItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "shipment_id", nullable = false)
  private Shipment shipment;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_item_id", nullable = false, unique = true)
  private OrderItem orderItem;

  protected ShipmentItem() {}

  private ShipmentItem(Shipment shipment, OrderItem orderItem) {
    this.shipment = shipment;
    this.orderItem = orderItem;
  }

  public OrderItem getOrderItem() {
    return orderItem;
  }

  public static ShipmentItem of(Shipment shipment, OrderItem orderItem) {
    return new ShipmentItem(shipment, orderItem);
  }
}
