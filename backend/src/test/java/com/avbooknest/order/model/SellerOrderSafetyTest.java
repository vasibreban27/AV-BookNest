package com.avbooknest.order.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SellerOrderSafetyTest {

  @Test
  void acceptanceAndDropoffDeadlinesExpireAtTheirBoundary() {
    Instant now = Instant.parse("2026-08-11T10:00:00Z");
    SellerOrder awaiting =
        SellerOrder.builder()
            .status(SellerOrderStatus.AWAITING_SELLER)
            .acceptBy(now.plus(Duration.ofHours(24)))
            .createdAt(now)
            .updatedAt(now)
            .build();
    SellerOrder accepted =
        SellerOrder.builder()
            .status(SellerOrderStatus.ACCEPTED)
            .dropoffBy(now.plus(Duration.ofHours(48)))
            .createdAt(now)
            .updatedAt(now)
            .build();

    assertFalse(awaiting.acceptanceExpired(now.plus(Duration.ofHours(24)).minusMillis(1)));
    assertTrue(awaiting.acceptanceExpired(now.plus(Duration.ofHours(24))));
    assertFalse(accepted.dropoffExpired(now.plus(Duration.ofHours(48)).minusMillis(1)));
    assertTrue(accepted.dropoffExpired(now.plus(Duration.ofHours(48))));
  }

  @Test
  void buyerCanReportOnlyOnceAndOnlyInsideDeliveryWindow() {
    Instant deliveredAt = Instant.parse("2026-08-11T10:00:00Z");
    SellerOrder sellerOrder =
        SellerOrder.builder()
            .status(SellerOrderStatus.FULFILLED)
            .fulfilledAt(deliveredAt)
            .createdAt(deliveredAt)
            .updatedAt(deliveredAt)
            .build();

    assertTrue(sellerOrder.canReportIssue(deliveredAt.plus(Duration.ofHours(23))));
    assertFalse(sellerOrder.canReportIssue(deliveredAt.plus(Duration.ofHours(24))));

    sellerOrder.openIssue("Colet deteriorat", deliveredAt.plus(Duration.ofHours(1)));
    assertTrue(sellerOrder.hasOpenIssue());
    assertFalse(sellerOrder.canReportIssue(deliveredAt.plus(Duration.ofHours(2))));

    sellerOrder.resolveIssue(deliveredAt.plus(Duration.ofHours(3)));
    assertFalse(sellerOrder.hasOpenIssue());
    assertFalse(sellerOrder.canReportIssue(deliveredAt.plus(Duration.ofHours(4))));
  }
}
