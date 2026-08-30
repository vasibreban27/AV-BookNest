package com.avbooknest.support.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "support_email_deliveries")
public class SupportEmailDelivery {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "message_id", nullable = false, unique = true)
  private SupportMessage message;

  @Column(name = "recipient", nullable = false, length = 254)
  private String recipient;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private SupportEmailStatus status;

  @Column(name = "attempts", nullable = false)
  private int attempts;

  @Column(name = "next_attempt_at", nullable = false)
  private Instant nextAttemptAt;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "last_error", length = 300)
  private String lastError;

  protected SupportEmailDelivery() {}

  public static final int MAX_ATTEMPTS = 5;

  public static SupportEmailDelivery create(
      SupportMessage message, String recipient, boolean enabled, Instant now) {
    SupportEmailDelivery delivery = new SupportEmailDelivery();
    delivery.message = message;
    delivery.recipient = recipient;
    delivery.status = enabled ? SupportEmailStatus.PENDING : SupportEmailStatus.DISABLED;
    delivery.nextAttemptAt = now;
    return delivery;
  }

  public boolean isDue(Instant now) {
    return (status == SupportEmailStatus.PENDING || status == SupportEmailStatus.FAILED)
        && attempts < MAX_ATTEMPTS
        && !nextAttemptAt.isAfter(now);
  }

  public void sent(Instant now) {
    status = SupportEmailStatus.SENT;
    attempts++;
    sentAt = now;
    lastError = null;
  }

  public void failed(Instant now) {
    status = SupportEmailStatus.FAILED;
    attempts++;
    // Do not persist provider exceptions: they can contain credentials or message content.
    lastError = "SMTP delivery failed. Check server mail configuration.";
    nextAttemptAt = now.plusSeconds(60L << Math.min(attempts - 1, 6));
  }

  public void retry(Instant now) {
    status = SupportEmailStatus.PENDING;
    attempts = 0;
    nextAttemptAt = now;
    lastError = null;
  }

  public Long getId() {
    return id;
  }

  public SupportMessage getMessage() {
    return message;
  }

  public String getRecipient() {
    return recipient;
  }

  public SupportEmailStatus getStatus() {
    return status;
  }

  public int getAttempts() {
    return attempts;
  }

  public Instant getNextAttemptAt() {
    return nextAttemptAt;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public String getLastError() {
    return lastError;
  }
}
