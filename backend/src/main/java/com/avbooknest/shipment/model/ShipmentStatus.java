package com.avbooknest.shipment.model;

public enum ShipmentStatus {
  NOT_CREATED,
  AWB_PENDING,
  AWB_CREATED,
  AWAITING_DROPOFF,
  IN_TRANSIT,
  DELIVERED,
  RETURNED,
  LOST,
  CANCELLED
}
