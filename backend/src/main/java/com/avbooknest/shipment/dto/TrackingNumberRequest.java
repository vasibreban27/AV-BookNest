package com.avbooknest.shipment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrackingNumberRequest(@NotBlank @Size(max = 100) String trackingNumber) {}
