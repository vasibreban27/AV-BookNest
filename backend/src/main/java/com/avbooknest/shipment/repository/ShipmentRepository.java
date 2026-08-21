package com.avbooknest.shipment.repository;

import com.avbooknest.shipment.model.Shipment;
import com.avbooknest.shipment.model.ShipmentStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
  boolean existsByTrackingNumber(String trackingNumber);

  Optional<Shipment> findByTrackingNumber(String trackingNumber);

  Optional<Shipment> findByTrackingNumberOrSamedayParcelId(
      String trackingNumber, String samedayParcelId);

  Optional<Shipment> findBySellerOrderId(Long sellerOrderId);

  @EntityGraph(attributePaths = "sellerOrder")
  Page<Shipment> findAllByStatusIn(Collection<ShipmentStatus> statuses, Pageable pageable);

  long countByStatusIn(Collection<ShipmentStatus> statuses);
}
