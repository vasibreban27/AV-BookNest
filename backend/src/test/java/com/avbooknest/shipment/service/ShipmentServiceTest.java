package com.avbooknest.shipment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avbooknest.integration.model.IntegrationEvent;
import com.avbooknest.integration.repository.IntegrationEventRepository;
import com.avbooknest.order.model.SellerOrder;
import com.avbooknest.order.model.SellerOrderStatus;
import com.avbooknest.payment.model.SellerTransfer;
import com.avbooknest.payment.repository.SellerTransferRepository;
import com.avbooknest.shipment.model.Shipment;
import com.avbooknest.shipment.model.ShipmentStatus;
import com.avbooknest.shipment.repository.ShipmentRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {
  @Mock private ShipmentRepository shipmentRepository;
  @Mock private SellerTransferRepository sellerTransferRepository;
  @Mock private IntegrationEventRepository integrationEventRepository;

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
    when(shipmentRepository.findByTrackingNumberOrSamedayParcelId("AWB-1", "AWB-1"))
        .thenReturn(Optional.of(shipment));
    ShipmentService service =
        new ShipmentService(
            shipmentRepository, sellerTransferRepository, integrationEventRepository);

    service.registerSamedayAwb(5L, "AWB-1", "parcel-1", "https://label");
    assertEquals(ShipmentStatus.AWB_CREATED, shipment.getStatus());

    service.updateFromSameday("AWB-1", ShipmentStatus.IN_TRANSIT, "picked_up", now.plusSeconds(60));
    assertEquals(ShipmentStatus.IN_TRANSIT, shipment.getStatus());
    assertEquals("picked_up", shipment.getProviderStatus());
  }

  @Test
  void deliveredShipmentSchedulesStripeTransferAfterTwentyFourHours() {
    Instant deliveredAt = Instant.parse("2026-07-24T10:00:00Z");
    SellerOrder sellerOrder =
        SellerOrder.builder()
            .id(5L)
            .status(SellerOrderStatus.ACCEPTED)
            .createdAt(deliveredAt.minusSeconds(3600))
            .updatedAt(deliveredAt.minusSeconds(3600))
            .build();
    Shipment shipment =
        Shipment.builder()
            .id(10L)
            .sellerOrder(sellerOrder)
            .easyboxId("locker-1")
            .easyboxName("Easybox Central")
            .status(ShipmentStatus.IN_TRANSIT)
            .createdAt(deliveredAt.minusSeconds(3600))
            .updatedAt(deliveredAt.minusSeconds(3600))
            .build();
    sellerOrder.assignShipment(shipment);
    SellerTransfer transfer =
        SellerTransfer.blocked(sellerOrder, new BigDecimal("95.00"), "RON", deliveredAt);
    when(shipmentRepository.findByTrackingNumberOrSamedayParcelId("AWB-1", "AWB-1"))
        .thenReturn(Optional.of(shipment));
    when(sellerTransferRepository.findBySellerOrderId(5L)).thenReturn(Optional.of(transfer));
    when(integrationEventRepository.save(any(IntegrationEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    ShipmentService service =
        new ShipmentService(
            shipmentRepository, sellerTransferRepository, integrationEventRepository);

    service.updateFromSameday("AWB-1", ShipmentStatus.DELIVERED, "delivered", deliveredAt);

    assertEquals(SellerOrderStatus.FULFILLED, sellerOrder.getStatus());
    assertEquals(Duration.ofHours(24), Duration.between(deliveredAt, transfer.getEligibleAt()));
    verify(integrationEventRepository).save(any(IntegrationEvent.class));
  }
}
