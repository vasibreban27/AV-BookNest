package com.avbooknest.shipping.sameday;

import com.avbooknest.shipment.model.ShipmentStatus;
import com.avbooknest.shipment.service.ShipmentService;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.sameday", name = "enabled", havingValue = "true")
public class SamedayStatusScheduler {
  private static final Logger LOGGER = LoggerFactory.getLogger(SamedayStatusScheduler.class);
  private static final Duration OVERLAP = Duration.ofMinutes(5);

  private final SamedayClient samedayClient;
  private final ShipmentService shipmentService;
  private Instant lastSuccessfulSync = Instant.now().minus(Duration.ofHours(24));

  public SamedayStatusScheduler(SamedayClient samedayClient, ShipmentService shipmentService) {
    this.samedayClient = samedayClient;
    this.shipmentService = shipmentService;
  }

  @Scheduled(
      initialDelayString = "${app.sameday.status-initial-delay-ms:15000}",
      fixedDelayString = "${app.sameday.status-delay-ms:60000}")
  public synchronized void synchronizeStatuses() {
    Instant end = Instant.now();
    Instant start = lastSuccessfulSync.minus(OVERLAP);
    try {
      for (SamedayStatusUpdate update : samedayClient.statusUpdates(start, end)) {
        try {
          shipmentService.updateFromSameday(
              update.parcelAwbNumber(),
              mapStatus(update),
              providerStatus(update),
              update.occurredAt());
        } catch (IllegalArgumentException unknownAwb) {
          LOGGER.debug("Ignoring Sameday status for unknown AWB {}", update.parcelAwbNumber());
        }
      }
      lastSuccessfulSync = end;
    } catch (RuntimeException exception) {
      LOGGER.warn("Sameday status synchronization failed; it will be retried", exception);
    }
  }

  private ShipmentStatus mapStatus(SamedayStatusUpdate update) {
    String status = normalize(providerStatus(update));
    if (containsAny(status, "delivered", "livrat")) {
      return ShipmentStatus.DELIVERED;
    }
    if (containsAny(status, "return", "retur")) {
      return ShipmentStatus.RETURNED;
    }
    if (containsAny(status, "lost", "pierdut")) {
      return ShipmentStatus.LOST;
    }
    if (containsAny(status, "cancel", "anulat")) {
      return ShipmentStatus.CANCELLED;
    }
    if (containsAny(status, "transit", "courier", "curier", "delivery", "livrare")) {
      return ShipmentStatus.IN_TRANSIT;
    }
    if (containsAny(status, "dropoff", "predare")) {
      return ShipmentStatus.AWAITING_DROPOFF;
    }
    return null;
  }

  private String providerStatus(SamedayStatusUpdate update) {
    return String.join(" | ", update.status(), update.statusLabel(), update.statusState());
  }

  private String normalize(String value) {
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }

  private boolean containsAny(String value, String... candidates) {
    for (String candidate : candidates) {
      if (value.contains(candidate)) {
        return true;
      }
    }
    return false;
  }
}
