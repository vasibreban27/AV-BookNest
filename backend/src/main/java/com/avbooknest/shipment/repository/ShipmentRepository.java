package com.avbooknest.shipment.repository;

import com.avbooknest.shipment.model.Shipment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

  @EntityGraph(attributePaths = {"seller", "items", "items.orderItem"})
  List<Shipment> findAllByOrderId(Long orderId);

  @EntityGraph(attributePaths = {"seller", "items", "items.orderItem"})
  List<Shipment> findAllBySellerIdOrderByCreatedAtDesc(Long sellerId);

  @EntityGraph(attributePaths = {"seller", "items", "items.orderItem"})
  Optional<Shipment> findByIdAndSellerId(Long id, Long sellerId);
}
