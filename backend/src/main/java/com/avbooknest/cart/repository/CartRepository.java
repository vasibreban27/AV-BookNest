package com.avbooknest.cart.repository;

import com.avbooknest.cart.model.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {
  @EntityGraph(attributePaths = {"items", "items.book", "items.book.seller", "items.book.category"})
  Optional<Cart> findByUserId(Long userId);
}
