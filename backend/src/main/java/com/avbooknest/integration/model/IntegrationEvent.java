package com.avbooknest.integration.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "integration_events")
public class IntegrationEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "aggregate_type", nullable = false, length = 50)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private Long aggregateId;

  @Column(name = "event_type", nullable = false, length = 80)
  private String eventType;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private IntegrationEventStatus status;

  @Column(nullable = false)
  private int attempts;

  @Column(name = "next_attempt_at", nullable = false)
  private Instant nextAttemptAt;

  @Column(name = "processed_at")
  private Instant processedAt;

  @Column(name = "last_error", length = 1000)
  private String lastError;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected IntegrationEvent() {}

  private IntegrationEvent(
      String aggregateType, Long aggregateId, String eventType, String payload, Instant now) {
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.payload = payload;
    status = IntegrationEventStatus.PENDING;
    attempts = 0;
    nextAttemptAt = now;
    createdAt = now;
  }

  public static IntegrationEvent pending(
      String aggregateType, Long aggregateId, String eventType, String payload, Instant now) {
    return new IntegrationEvent(aggregateType, aggregateId, eventType, payload, now);
  }
}
