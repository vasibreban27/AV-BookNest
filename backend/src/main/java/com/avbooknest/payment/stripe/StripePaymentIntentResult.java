package com.avbooknest.payment.stripe;

public record StripePaymentIntentResult(String paymentIntentId, String clientSecret) {}
