package com.avbooknest.shipment.repository;

import com.avbooknest.shipment.model.Shipment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
  boolean existsByTrackingNumber(String trackingNumber);

  Optional<Shipment> findByTrackingNumber(String trackingNumber);

  Optional<Shipment> findByTrackingNumberOrSamedayParcelId(
      String trackingNumber, String samedayParcelId);

  Optional<Shipment> findBySellerOrderId(Long sellerOrderId);
}
