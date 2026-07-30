package com.avbooknest.payment.stripe;

import java.math.BigDecimal;

public interface StripeGateway {
  StripePaymentIntentResult createPaymentIntent(
      Long orderId, String orderNumber, BigDecimal amount, String currency, String receiptEmail);

  void cancelPaymentIntent(String paymentIntentId);

  String refund(String paymentIntentId, BigDecimal amount, String currency, String idempotencyKey);

  String createConnectedAccount(String email);

  String createOnboardingLink(String accountId);

  StripeAccountStatus accountStatus(String accountId);

  String createTransfer(
      BigDecimal amount,
      String currency,
      String destinationAccountId,
      String transferGroup,
      String sourceChargeId,
      String idempotencyKey);

  StripeWebhookEvent parseWebhook(String payload, String signature);
}
