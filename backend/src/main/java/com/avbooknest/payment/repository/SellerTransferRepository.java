package com.avbooknest.payment.repository;

import com.avbooknest.payment.model.SellerTransfer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerTransferRepository extends JpaRepository<SellerTransfer, Long> {
  Optional<SellerTransfer> findBySellerOrderId(Long sellerOrderId);
}
