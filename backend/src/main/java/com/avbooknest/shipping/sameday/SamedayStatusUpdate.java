package com.avbooknest.shipping.sameday;

import java.time.Instant;

public record SamedayStatusUpdate(
    String parcelAwbNumber,
    String status,
    String statusLabel,
    String statusState,
    Instant occurredAt) {}
