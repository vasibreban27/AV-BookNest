package com.avbooknest.order.repository;

import com.avbooknest.order.model.Payment;
import com.avbooknest.order.model.PaymentStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
  Optional<Payment> findByOrderId(Long orderId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Payment> findByProviderPaymentId(String providerPaymentId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Payment p where p.id = :id")
  Optional<Payment> findByIdForUpdate(@Param("id") Long id);

  List<Payment> findAllByStatusInAndExpiresAtLessThanEqual(
      List<PaymentStatus> statuses, Instant now);

  @Query(
      """
      select coalesce(sum(payment.amount - payment.refundedAmount), 0)
      from Payment payment
      where payment.status in (
        com.avbooknest.order.model.PaymentStatus.SUCCEEDED,
        com.avbooknest.order.model.PaymentStatus.PARTIALLY_REFUNDED
      )
      """)
  java.math.BigDecimal sumNetCapturedAmount();
}
