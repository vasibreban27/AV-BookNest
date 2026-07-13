package com.avbooknest.cart.repository;

import com.avbooknest.cart.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    boolean existsByCartIdAndBookId(Long cartId, Long bookId);
}
