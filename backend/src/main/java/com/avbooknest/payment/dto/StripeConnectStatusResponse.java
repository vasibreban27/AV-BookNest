package com.avbooknest.payment.dto;

public record StripeConnectStatusResponse(
    boolean sandboxConfigured,
    boolean connected,
    boolean detailsSubmitted,
    boolean chargesEnabled,
    boolean payoutsEnabled) {}
