package com.avbooknest.integration.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.sameday", name = "enabled", havingValue = "true")
public class SamedayAwbEventScheduler {
  private static final int MAX_EVENTS_PER_RUN = 10;
  private final SamedayAwbEventHandler handler;

  public SamedayAwbEventScheduler(SamedayAwbEventHandler handler) {
    this.handler = handler;
  }

  @Scheduled(
      initialDelayString = "${app.sameday.outbox-initial-delay-ms:5000}",
      fixedDelayString = "${app.sameday.outbox-delay-ms:5000}")
  public void processPendingEvents() {
    for (int processed = 0; processed < MAX_EVENTS_PER_RUN && handler.processNext(); processed++) {
      // Bound each run so a large backlog cannot monopolize the scheduler.
    }
  }
}
