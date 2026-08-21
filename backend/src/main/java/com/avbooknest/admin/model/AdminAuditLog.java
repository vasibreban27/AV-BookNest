package com.avbooknest.admin.model;

import com.avbooknest.auth.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "admin_audit_logs")
public class AdminAuditLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "admin_user_id", nullable = false)
  private User administrator;

  @Column(nullable = false, length = 80)
  private String action;

  @Column(name = "target_type", nullable = false, length = 50)
  private String targetType;

  @Column(name = "target_id")
  private Long targetId;

  @Column(length = 500)
  private String reason;

  @Column(columnDefinition = "TEXT")
  private String details;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected AdminAuditLog() {}

  private AdminAuditLog(
      User administrator,
      String action,
      String targetType,
      Long targetId,
      String reason,
      String details,
      Instant createdAt) {
    this.administrator = administrator;
    this.action = action;
    this.targetType = targetType;
    this.targetId = targetId;
    this.reason = reason;
    this.details = details;
    this.createdAt = createdAt;
  }

  public static AdminAuditLog create(
      User administrator,
      String action,
      String targetType,
      Long targetId,
      String reason,
      String details,
      Instant now) {
    return new AdminAuditLog(administrator, action, targetType, targetId, reason, details, now);
  }

  public Long getId() {
    return id;
  }

  public User getAdministrator() {
    return administrator;
  }

  public String getAction() {
    return action;
  }

  public String getTargetType() {
    return targetType;
  }

  public Long getTargetId() {
    return targetId;
  }

  public String getReason() {
    return reason;
  }

  public String getDetails() {
    return details;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
