package com.avbooknest.order.dto;

import java.time.Instant;

public record StripeCheckoutResponse(
    OrderResponse order,
    String clientSecret,
    String publishableKey,
    String returnUrl,
    Instant expiresAt) {}
