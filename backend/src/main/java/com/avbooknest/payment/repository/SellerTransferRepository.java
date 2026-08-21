package com.avbooknest.payment.repository;

import com.avbooknest.payment.model.SellerTransfer;
import com.avbooknest.payment.model.SellerTransferStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerTransferRepository extends JpaRepository<SellerTransfer, Long> {
  Optional<SellerTransfer> findBySellerOrderId(Long sellerOrderId);

  Optional<SellerTransfer> findByProviderTransferId(String providerTransferId);

  @EntityGraph(attributePaths = "sellerOrder")
  Page<SellerTransfer> findAllByStatus(SellerTransferStatus status, Pageable pageable);

  long countByStatus(SellerTransferStatus status);
}
