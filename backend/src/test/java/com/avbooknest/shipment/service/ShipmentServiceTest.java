package com.avbooknest.shipment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.avbooknest.shipment.model.Shipment;
import com.avbooknest.shipment.model.ShipmentStatus;
import com.avbooknest.shipment.repository.ShipmentRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {
  @Mock private ShipmentRepository shipmentRepository;

  @Test
  void samedayOwnsAwbAndTrackingTransitions() {
    Instant now = Instant.now();
    Shipment shipment =
        Shipment.builder()
            .id(10L)
            .easyboxId("locker-1")
            .easyboxName("Easybox Central")
            .status(ShipmentStatus.AWB_PENDING)
            .createdAt(now)
            .updatedAt(now)
            .build();
    when(shipmentRepository.findBySellerOrderId(5L)).thenReturn(Optional.of(shipment));
    when(shipmentRepository.findByTrackingNumber("AWB-1")).thenReturn(Optional.of(shipment));
    ShipmentService service = new ShipmentService(shipmentRepository);

    service.registerSamedayAwb(5L, "AWB-1", "parcel-1", "https://label");
    assertEquals(ShipmentStatus.AWB_CREATED, shipment.getStatus());

    service.updateFromSameday("AWB-1", ShipmentStatus.IN_TRANSIT, "picked_up", now.plusSeconds(60));
    assertEquals(ShipmentStatus.IN_TRANSIT, shipment.getStatus());
    assertEquals("picked_up", shipment.getProviderStatus());
  }
}
