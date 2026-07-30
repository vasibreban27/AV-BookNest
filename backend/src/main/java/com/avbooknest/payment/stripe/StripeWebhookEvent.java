package com.avbooknest.payment.stripe;

public record StripeWebhookEvent(
    String eventId,
    String type,
    String objectId,
    String relatedId,
    String message,
    Boolean detailsSubmitted,
    Boolean chargesEnabled,
    Boolean payoutsEnabled) {}
