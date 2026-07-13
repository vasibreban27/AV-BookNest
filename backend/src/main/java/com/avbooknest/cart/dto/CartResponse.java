package com.avbooknest.cart.dto;

import com.avbooknest.cart.model.Cart;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CartResponse(Long id, List<CartItemResponse> items, BigDecimal total, Instant createdAt, Instant updatedAt) {
    public static CartResponse from(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream().map(CartItemResponse::from).toList();
        BigDecimal total = cart.getItems().stream().map(item -> item.getBook().getPrice()).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(cart.getId(), items, total, cart.getCreatedAt(), cart.getUpdatedAt());
    }
}
