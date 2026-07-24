package com.avbooknest.shipment.service;

import com.avbooknest.integration.model.IntegrationEvent;
import com.avbooknest.integration.repository.IntegrationEventRepository;
import com.avbooknest.order.model.SellerOrder;
import com.avbooknest.order.model.SellerOrderStatus;
import com.avbooknest.payment.model.SellerTransfer;
import com.avbooknest.payment.repository.SellerTransferRepository;
import com.avbooknest.shipment.model.Shipment;
import com.avbooknest.shipment.model.ShipmentStatus;
import com.avbooknest.shipment.repository.ShipmentRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ShipmentService {
  private static final Duration PAYOUT_DELAY = Duration.ofHours(24);
  private final ShipmentRepository shipmentRepository;
  private final SellerTransferRepository sellerTransferRepository;
  private final IntegrationEventRepository integrationEventRepository;

  public ShipmentService(
      ShipmentRepository shipmentRepository,
      SellerTransferRepository sellerTransferRepository,
      IntegrationEventRepository integrationEventRepository) {
    this.shipmentRepository = shipmentRepository;
    this.sellerTransferRepository = sellerTransferRepository;
    this.integrationEventRepository = integrationEventRepository;
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
            .findByTrackingNumberOrSamedayParcelId(awbNumber, awbNumber)
            .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));
    ShipmentStatus previousStatus = shipment.getStatus();
    boolean applied =
        shipment.updateProviderStatus(mappedStatus, providerStatus, providerUpdatedAt);
    if (applied
        && mappedStatus == ShipmentStatus.DELIVERED
        && previousStatus != ShipmentStatus.DELIVERED
        && shipment.getSellerOrder().getStatus() != SellerOrderStatus.FULFILLED) {
      scheduleSellerPayout(shipment.getSellerOrder(), providerUpdatedAt);
    }
    return shipment;
  }

  private void scheduleSellerPayout(SellerOrder sellerOrder, Instant deliveredAt) {
    Instant eligibleAt = deliveredAt.plus(PAYOUT_DELAY);
    sellerOrder.fulfill(deliveredAt);
    SellerTransfer transfer =
        sellerTransferRepository
            .findBySellerOrderId(sellerOrder.getId())
            .orElseThrow(() -> new IllegalStateException("Seller transfer not found"));
    transfer.scheduleEligibility(eligibleAt, Instant.now());
    integrationEventRepository.save(
        IntegrationEvent.scheduled(
            "SELLER_TRANSFER",
            sellerOrder.getId(),
            "STRIPE_CREATE_TRANSFER",
            "{\"sellerOrderId\":" + sellerOrder.getId() + "}",
            Instant.now(),
            eligibleAt));
  }
}
