package com.avbooknest.shipment.service;

import com.avbooknest.integration.model.IntegrationEvent;
import com.avbooknest.integration.repository.IntegrationEventRepository;
import com.avbooknest.notification.model.NotificationType;
import com.avbooknest.notification.service.NotificationService;
import com.avbooknest.order.model.SellerOrder;
import com.avbooknest.order.model.SellerOrderStatus;
import com.avbooknest.order.service.SellerOrderService;
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
  private final SellerOrderService sellerOrderService;
  private final NotificationService notificationService;

  public ShipmentService(
      ShipmentRepository shipmentRepository,
      SellerTransferRepository sellerTransferRepository,
      IntegrationEventRepository integrationEventRepository,
      SellerOrderService sellerOrderService,
      NotificationService notificationService) {
    this.shipmentRepository = shipmentRepository;
    this.sellerTransferRepository = sellerTransferRepository;
    this.integrationEventRepository = integrationEventRepository;
    this.sellerOrderService = sellerOrderService;
    this.notificationService = notificationService;
  }

  public void registerSamedayAwb(
      Long sellerOrderId, String awbNumber, String parcelId, String labelUrl) {
    Shipment shipment =
        shipmentRepository
            .findBySellerOrderId(sellerOrderId)
            .orElseThrow(() -> new IllegalArgumentException("Shipment not found"));
    shipment.registerAwb(awbNumber, parcelId, labelUrl, Instant.now());
    SellerOrder sellerOrder = shipment.getSellerOrder();
    if (sellerOrder != null && sellerOrder.getSeller() != null && sellerOrder.getOrder() != null) {
      notificationService.create(
          sellerOrder.getSeller(),
          NotificationType.AWB_CREATED,
          "Sameday AWB created",
          "AWB "
              + awbNumber
              + " is ready for order "
              + sellerOrder.getOrder().getOrderNumber()
              + ".",
          "/sales");
    }
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
      notifyBuyer(
          shipment,
          NotificationType.SHIPMENT_DELIVERED,
          "Parcel delivered",
          "The parcel was delivered. You have 24 hours to report a problem.");
    } else if (applied
        && mappedStatus == ShipmentStatus.IN_TRANSIT
        && previousStatus != ShipmentStatus.IN_TRANSIT) {
      notifyBuyer(
          shipment,
          NotificationType.SHIPMENT_IN_TRANSIT,
          "Parcel in transit",
          "Sameday picked up the parcel and it is on the way.");
    } else if (applied
        && (mappedStatus == ShipmentStatus.RETURNED
            || mappedStatus == ShipmentStatus.LOST
            || mappedStatus == ShipmentStatus.CANCELLED)) {
      sellerOrderService.handleShipmentFailure(
          shipment.getSellerOrder().getId(), mappedStatus, providerUpdatedAt);
    }
    return shipment;
  }

  private void notifyBuyer(Shipment shipment, NotificationType type, String title, String message) {
    if (shipment.getSellerOrder() == null
        || shipment.getSellerOrder().getOrder() == null
        || shipment.getSellerOrder().getOrder().getBuyer() == null) {
      return;
    }
    notificationService.create(
        shipment.getSellerOrder().getOrder().getBuyer(),
        type,
        title,
        message,
        "/orders/" + shipment.getSellerOrder().getOrder().getId());
  }

  private void scheduleSellerPayout(SellerOrder sellerOrder, Instant deliveredAt) {
    Instant eligibleAt = deliveredAt.plus(PAYOUT_DELAY);
    sellerOrder.fulfill(deliveredAt);
    sellerOrder.getItems().stream()
        .map(com.avbooknest.order.model.OrderItem::getBook)
        .filter(java.util.Objects::nonNull)
        .forEach(com.avbooknest.book.model.Book::markSold);
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
