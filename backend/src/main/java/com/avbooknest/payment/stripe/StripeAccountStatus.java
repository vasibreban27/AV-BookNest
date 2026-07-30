package com.avbooknest.payment.stripe;

public record StripeAccountStatus(
    String accountId, boolean detailsSubmitted, boolean chargesEnabled, boolean payoutsEnabled) {}
