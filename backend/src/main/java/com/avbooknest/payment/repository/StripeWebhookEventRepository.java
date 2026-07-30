package com.avbooknest.payment.repository;

import com.avbooknest.payment.model.StripeWebhookEventRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StripeWebhookEventRepository
    extends JpaRepository<StripeWebhookEventRecord, Long> {
  boolean existsByStripeEventId(String stripeEventId);
}
