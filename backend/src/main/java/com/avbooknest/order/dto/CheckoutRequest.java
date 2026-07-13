package com.avbooknest.order.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckoutRequest(@NotBlank String easyboxId, @NotBlank String easyboxName) {}
