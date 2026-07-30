package com.avbooknest.payment.service;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.stripe", name = "enabled", havingValue = "true")
public class StripePaymentExpirationScheduler {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(StripePaymentExpirationScheduler.class);

  private final StripePaymentService stripePaymentService;

  public StripePaymentExpirationScheduler(StripePaymentService stripePaymentService) {
    this.stripePaymentService = stripePaymentService;
  }

  @Scheduled(
      initialDelayString = "${app.stripe.expiration-initial-delay-ms:60000}",
      fixedDelayString = "${app.stripe.expiration-delay-ms:60000}")
  public void expireAbandonedPayments() {
    for (Long paymentId : stripePaymentService.expiredPaymentIds(Instant.now())) {
      try {
        stripePaymentService.expirePayment(paymentId);
      } catch (RuntimeException exception) {
        LOGGER.error("Could not expire Stripe payment {}", paymentId, exception);
      }
    }
  }
}
