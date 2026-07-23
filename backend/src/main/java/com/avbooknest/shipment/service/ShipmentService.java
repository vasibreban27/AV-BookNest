package com.avbooknest.shipment.service;

import com.avbooknest.shipment.model.Shipment;
import com.avbooknest.shipment.model.ShipmentStatus;
import com.avbooknest.shipment.repository.ShipmentRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShipmentService {
  private final ShipmentRepository shipmentRepository;

  public ShipmentService(ShipmentRepository shipmentRepository) {
    this.shipmentRepository = shipmentRepository;
  }

  public void registerSamedayAwb(
      Long sellerOrderId, String awbNumber, String parcelId, String labelUrl) {
    Shipment shipment =
        shipmentRepository
            .findBySellerOrderId(sellerOrderId)
            .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));
    shipment.registerAwb(awbNumber, parcelId, labelUrl, Instant.now());
  }

  public Shipment updateFromSameday(
      String awbNumber,
      ShipmentStatus mappedStatus,
      String providerStatus,
      Instant providerUpdatedAt) {
    Shipment shipment =
        shipmentRepository
            .findByTrackingNumber(awbNumber)
            .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));
    shipment.updateProviderStatus(mappedStatus, providerStatus, providerUpdatedAt);
    return shipment;
  }
}
