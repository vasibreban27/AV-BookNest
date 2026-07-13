package com.avbooknest.cart.dto;

import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(@NotNull Long bookId) { }
