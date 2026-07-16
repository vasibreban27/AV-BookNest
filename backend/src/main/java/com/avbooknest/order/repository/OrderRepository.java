package com.avbooknest.order.repository;

import com.avbooknest.order.model.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
  @EntityGraph(attributePaths = {"items", "items.seller"})
  List<Order> findAllByBuyerIdOrderByPlacedAtDesc(Long buyerId);

  @EntityGraph(attributePaths = {"items", "items.seller"})
  Optional<Order> findByIdAndBuyerId(Long id, Long buyerId);
}
