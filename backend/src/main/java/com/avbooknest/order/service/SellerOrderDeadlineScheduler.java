package com.avbooknest.order.service;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SellerOrderDeadlineScheduler {
  private static final Logger LOGGER = LoggerFactory.getLogger(SellerOrderDeadlineScheduler.class);
  private final SellerOrderService sellerOrderService;

  public SellerOrderDeadlineScheduler(SellerOrderService sellerOrderService) {
    this.sellerOrderService = sellerOrderService;
  }

  @Scheduled(
      initialDelayString = "${app.orders.deadline-initial-delay-ms:30000}",
      fixedDelayString = "${app.orders.deadline-delay-ms:60000}")
  public void processDeadlines() {
    Instant now = Instant.now();
    for (Long id : sellerOrderService.expiredAcceptanceIds(now)) {
      process(() -> sellerOrderService.expireAcceptance(id, now), id, "acceptance");
    }
    for (Long id : sellerOrderService.expiredDropoffIds(now)) {
      process(() -> sellerOrderService.expireDropoff(id, now), id, "drop-off");
    }
  }

  private void process(Runnable action, Long sellerOrderId, String deadline) {
    try {
      action.run();
    } catch (RuntimeException exception) {
      LOGGER.warn(
          "Could not process {} deadline for seller order {}", deadline, sellerOrderId, exception);
    }
  }
}
