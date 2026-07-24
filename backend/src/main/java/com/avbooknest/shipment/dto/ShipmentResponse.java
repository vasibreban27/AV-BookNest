package com.avbooknest.shipment.dto;

import com.avbooknest.shipment.model.PackageSize;
import com.avbooknest.shipment.model.Shipment;
import com.avbooknest.shipment.model.ShipmentStatus;
import java.time.Instant;

public record ShipmentResponse(
    Long id,
    String easyboxId,
    String easyboxName,
    String easyboxAddress,
    String easyboxCity,
    String easyboxCounty,
    String easyboxPostalCode,
    String trackingNumber,
    ShipmentStatus status,
    PackageSize packageSize,
    Integer packageWeightGrams,
    Integer packageLengthMm,
    Integer packageWidthMm,
    Integer packageHeightMm,
    String providerStatus,
    Instant statusUpdatedAt,
    String labelUrl,
    Instant createdAt) {

  public static ShipmentResponse from(Shipment shipment) {
    return new ShipmentResponse(
        shipment.getId(),
        shipment.getEasyboxId(),
        shipment.getEasyboxName(),
        shipment.getEasyboxAddress(),
        shipment.getEasyboxCity(),
        shipment.getEasyboxCounty(),
        shipment.getEasyboxPostalCode(),
        shipment.getTrackingNumber(),
        shipment.getStatus(),
        shipment.getPackageSize(),
        shipment.getPackageWeightGrams(),
        shipment.getPackageLengthMm(),
        shipment.getPackageWidthMm(),
        shipment.getPackageHeightMm(),
        shipment.getProviderStatus(),
        shipment.getStatusUpdatedAt(),
        shipment.getLabelUrl(),
        shipment.getCreatedAt());
  }
}
