package com.avbooknest.payment.model;

import com.avbooknest.order.model.SellerOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "seller_transfers")
public class SellerTransfer {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "seller_order_id", nullable = false, unique = true)
  private SellerOrder sellerOrder;

  @Column(name = "provider_transfer_id", unique = true, length = 255)
  private String providerTransferId;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, length = 3)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private SellerTransferStatus status;

  @Column(name = "eligible_at")
  private Instant eligibleAt;

  @Column(name = "failure_reason", length = 500)
  private String failureReason;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected SellerTransfer() {}

  private SellerTransfer(
      SellerOrder sellerOrder, BigDecimal amount, String currency, Instant createdAt) {
    this.sellerOrder = sellerOrder;
    this.amount = amount;
    this.currency = currency;
    status = SellerTransferStatus.BLOCKED;
    this.createdAt = createdAt;
    updatedAt = createdAt;
  }

  public static SellerTransfer blocked(
      SellerOrder sellerOrder, BigDecimal amount, String currency, Instant now) {
    return new SellerTransfer(sellerOrder, amount, currency, now);
  }
}
