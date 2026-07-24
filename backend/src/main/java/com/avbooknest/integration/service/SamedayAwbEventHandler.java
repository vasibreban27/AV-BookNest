package com.avbooknest.integration.service;

import com.avbooknest.integration.model.IntegrationEvent;
import com.avbooknest.integration.model.IntegrationEventStatus;
import com.avbooknest.integration.repository.IntegrationEventRepository;
import com.avbooknest.order.model.SellerOrder;
import com.avbooknest.order.repository.SellerOrderRepository;
import com.avbooknest.shipment.model.ShipmentStatus;
import com.avbooknest.shipment.service.ShipmentService;
import com.avbooknest.shipping.sameday.SamedayAwb;
import com.avbooknest.shipping.sameday.SamedayClient;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SamedayAwbEventHandler {
  static final String EVENT_TYPE = "SAMEDAY_CREATE_AWB";
  private static final Logger LOGGER = LoggerFactory.getLogger(SamedayAwbEventHandler.class);

  private final IntegrationEventRepository eventRepository;
  private final SellerOrderRepository sellerOrderRepository;
  private final SamedayClient samedayClient;
  private final ShipmentService shipmentService;

  public SamedayAwbEventHandler(
      IntegrationEventRepository eventRepository,
      SellerOrderRepository sellerOrderRepository,
      SamedayClient samedayClient,
      ShipmentService shipmentService) {
    this.eventRepository = eventRepository;
    this.sellerOrderRepository = sellerOrderRepository;
    this.samedayClient = samedayClient;
    this.shipmentService = shipmentService;
  }

  @Transactional
  public boolean processNext() {
    Instant now = Instant.now();
    IntegrationEvent event =
        eventRepository
            .findFirstByEventTypeAndStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                EVENT_TYPE, IntegrationEventStatus.PENDING, now)
            .orElse(null);
    if (event == null) {
      return false;
    }

    event.startProcessing();
    try {
      SellerOrder sellerOrder =
          sellerOrderRepository
              .findDetailedById(event.getAggregateId())
              .orElseThrow(() -> new IllegalStateException("Seller order not found"));
      if (sellerOrder.getShipment().getTrackingNumber() != null
          || sellerOrder.getShipment().getStatus() != ShipmentStatus.AWB_PENDING) {
        event.markProcessed(now);
        return true;
      }

      SamedayAwb awb = samedayClient.createAwb(sellerOrder);
      shipmentService.registerSamedayAwb(
          sellerOrder.getId(), awb.awbNumber(), awb.parcelAwbNumber(), null);
      event.markProcessed(Instant.now());
    } catch (RuntimeException exception) {
      LOGGER.warn(
          "Sameday AWB event {} failed on attempt {}",
          event.getId(),
          event.getAttempts() + 1,
          exception);
      event.scheduleRetry(rootMessage(exception), Instant.now());
    }
    return true;
  }

  private String rootMessage(Throwable throwable) {
    Throwable current = throwable;
    while (current.getCause() != null) {
      current = current.getCause();
    }
    return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
  }
}
