package com.avbooknest.order.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private PaymentProvider provider;

  @Column(name = "provider_payment_id", unique = true, length = 255)
  private String providerPaymentId;

  @Column(name = "provider_checkout_session_id", unique = true, length = 255)
  private String providerCheckoutSessionId;

  @Column(name = "provider_charge_id", unique = true, length = 255)
  private String providerChargeId;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 3)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private PaymentStatus status;

  @Column(name = "failure_reason", length = 500)
  private String failureReason;

  @Column(name = "paid_at")
  private Instant paidAt;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "refunded_amount", nullable = false, precision = 12, scale = 2)
  private BigDecimal refundedAmount;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Payment() {}

  private Payment(Builder b) {
    id = b.id;
    order = b.order;
    provider = b.provider;
    providerPaymentId = b.providerPaymentId;
    providerCheckoutSessionId = b.providerCheckoutSessionId;
    providerChargeId = b.providerChargeId;
    amount = b.amount;
    currency = b.currency;
    status = b.status;
    failureReason = b.failureReason;
    paidAt = b.paidAt;
    expiresAt = b.expiresAt;
    refundedAmount = b.refundedAmount == null ? BigDecimal.ZERO : b.refundedAmount;
    createdAt = b.createdAt;
    updatedAt = b.updatedAt;
  }

  public Long getId() {
    return id;
  }

  public Order getOrder() {
    return order;
  }

  public PaymentProvider getProvider() {
    return provider;
  }

  public String getProviderPaymentId() {
    return providerPaymentId;
  }

  public String getProviderCheckoutSessionId() {
    return providerCheckoutSessionId;
  }

  public String getProviderChargeId() {
    return providerChargeId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public String getFailureReason() {
    return failureReason;
  }

  public Instant getPaidAt() {
    return paidAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public BigDecimal getRefundedAmount() {
    return refundedAmount;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void updatePendingAmount(BigDecimal newAmount) {
    amount = newAmount;
    status = PaymentStatus.PENDING;
    paidAt = null;
    updatedAt = Instant.now();
  }

  public void cancel() {
    status = PaymentStatus.CANCELLED;
    paidAt = null;
    updatedAt = Instant.now();
  }

  public void bindPaymentIntent(String paymentIntentId, Instant expiration) {
    providerPaymentId = paymentIntentId;
    expiresAt = expiration;
    status = PaymentStatus.PENDING;
    updatedAt = Instant.now();
  }

  public void stopExpirationTracking() {
    expiresAt = null;
    updatedAt = Instant.now();
  }

  public void fail(String reason) {
    status = PaymentStatus.FAILED;
    failureReason = reason;
    updatedAt = Instant.now();
  }

  public void succeed(String chargeId, Instant now) {
    providerChargeId = chargeId;
    status = PaymentStatus.SUCCEEDED;
    failureReason = null;
    paidAt = now;
    updatedAt = paidAt;
  }

  public void refund(BigDecimal value, Instant now) {
    refundedAmount = refundedAmount.add(value);
    status =
        refundedAmount.compareTo(amount) >= 0
            ? PaymentStatus.REFUNDED
            : PaymentStatus.PARTIALLY_REFUNDED;
    updatedAt = now;
  }

  public boolean canExpire(Instant now) {
    return (status == PaymentStatus.PENDING || status == PaymentStatus.FAILED)
        && expiresAt != null
        && !now.isBefore(expiresAt);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Long id;
    private Order order;
    private PaymentProvider provider;
    private String providerPaymentId;
    private String providerCheckoutSessionId;
    private String providerChargeId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String failureReason;
    private Instant paidAt;
    private Instant expiresAt;
    private BigDecimal refundedAmount;
    private Instant createdAt;
    private Instant updatedAt;

    public Builder id(Long v) {
      id = v;
      return this;
    }

    public Builder order(Order v) {
      order = v;
      return this;
    }

    public Builder provider(PaymentProvider v) {
      provider = v;
      return this;
    }

    public Builder providerPaymentId(String v) {
      providerPaymentId = v;
      return this;
    }

    public Builder providerCheckoutSessionId(String value) {
      providerCheckoutSessionId = value;
      return this;
    }

    public Builder providerChargeId(String value) {
      providerChargeId = value;
      return this;
    }

    public Builder amount(BigDecimal v) {
      amount = v;
      return this;
    }

    public Builder currency(String v) {
      currency = v;
      return this;
    }

    public Builder status(PaymentStatus v) {
      status = v;
      return this;
    }

    public Builder failureReason(String v) {
      failureReason = v;
      return this;
    }

    public Builder paidAt(Instant v) {
      paidAt = v;
      return this;
    }

    public Builder expiresAt(Instant value) {
      expiresAt = value;
      return this;
    }

    public Builder refundedAmount(BigDecimal value) {
      refundedAmount = value;
      return this;
    }

    public Builder createdAt(Instant v) {
      createdAt = v;
      return this;
    }

    public Builder updatedAt(Instant v) {
      updatedAt = v;
      return this;
    }

    public Payment build() {
      return new Payment(this);
    }
  }
}
