package com.avbooknest.payment.stripe;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.stripe")
public record StripeProperties(
    boolean enabled,
    String secretKey,
    String publishableKey,
    String webhookSecret,
    String connectReturnUrl,
    String connectRefreshUrl,
    String paymentReturnUrl,
    int paymentExpirationMinutes) {

  public boolean sandboxConfigured() {
    return enabled
        && secretKey != null
        && secretKey.startsWith("sk_test_")
        && publishableKey != null
        && publishableKey.startsWith("pk_test_");
  }
}
