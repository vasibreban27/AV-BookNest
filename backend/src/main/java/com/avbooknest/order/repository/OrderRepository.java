package com.avbooknest.order.repository;

import com.avbooknest.order.model.Order;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
  @EntityGraph(attributePaths = {"items", "items.seller"})
  List<Order> findAllByBuyerIdOrderByPlacedAtDesc(Long buyerId);

  @EntityGraph(attributePaths = {"items", "items.seller"})
  Optional<Order> findByIdAndBuyerId(Long id, Long buyerId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from Order o where o.id = :id")
  Optional<Order> findByIdForUpdate(@Param("id") Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from Order o where o.id = :id and o.buyer.id = :buyerId")
  Optional<Order> findByIdAndBuyerIdForUpdate(@Param("id") Long id, @Param("buyerId") Long buyerId);
}
