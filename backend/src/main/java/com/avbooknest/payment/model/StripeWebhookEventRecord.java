package com.avbooknest.payment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "stripe_webhook_events")
public class StripeWebhookEventRecord {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "stripe_event_id", nullable = false, unique = true, length = 255)
  private String stripeEventId;

  @Column(name = "event_type", nullable = false, length = 100)
  private String eventType;

  @Column(name = "processed_at", nullable = false)
  private Instant processedAt;

  protected StripeWebhookEventRecord() {}

  public StripeWebhookEventRecord(String stripeEventId, String eventType, Instant processedAt) {
    this.stripeEventId = stripeEventId;
    this.eventType = eventType;
    this.processedAt = processedAt;
  }
}
