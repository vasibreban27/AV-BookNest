package com.avbooknest.integration.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.stripe", name = "enabled", havingValue = "true")
public class StripeRefundEventScheduler {
  private static final int MAX_EVENTS_PER_RUN = 10;
  private final StripeRefundEventHandler handler;

  public StripeRefundEventScheduler(StripeRefundEventHandler handler) {
    this.handler = handler;
  }

  @Scheduled(
      initialDelayString = "${app.stripe.refund-initial-delay-ms:5000}",
      fixedDelayString = "${app.stripe.refund-delay-ms:5000}")
  public void processPendingRefunds() {
    process(StripeRefundEventHandler.ORDER_EVENT);
    process(StripeRefundEventHandler.SELLER_ORDER_EVENT);
  }

  private void process(String eventType) {
    for (int processed = 0;
        processed < MAX_EVENTS_PER_RUN && handler.processNext(eventType);
        processed++) {
      // Bound each run so a large backlog cannot monopolize the scheduler.
    }
  }
}
