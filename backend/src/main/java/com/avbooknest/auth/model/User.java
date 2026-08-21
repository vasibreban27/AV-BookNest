package com.avbooknest.auth.model;

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
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "first_name", nullable = false, length = 100)
  private String firstName;

  @Column(name = "last_name", nullable = false, length = 100)
  private String lastName;

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "phone_number", length = 30)
  private String phoneNumber;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "role_id", nullable = false)
  private Role role;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified;

  @Column(nullable = false)
  private boolean enabled;

  @Column(name = "suspended_at")
  private Instant suspendedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "suspended_by")
  private User suspendedBy;

  @Column(name = "suspension_reason", length = 500)
  private String suspensionReason;

  @Column(name = "stripe_account_id", unique = true, length = 255)
  private String stripeAccountId;

  @Column(name = "stripe_details_submitted", nullable = false)
  private boolean stripeDetailsSubmitted;

  @Column(name = "stripe_charges_enabled", nullable = false)
  private boolean stripeChargesEnabled;

  @Column(name = "stripe_payouts_enabled", nullable = false)
  private boolean stripePayoutsEnabled;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected User() {}

  private User(Builder builder) {
    this.id = builder.id;
    this.firstName = builder.firstName;
    this.lastName = builder.lastName;
    this.email = builder.email;
    this.phoneNumber = builder.phoneNumber;
    this.passwordHash = builder.passwordHash;
    this.role = builder.role;
    this.emailVerified = builder.emailVerified;
    this.enabled = builder.enabled;
    this.suspendedAt = builder.suspendedAt;
    this.suspendedBy = builder.suspendedBy;
    this.suspensionReason = builder.suspensionReason;
    this.stripeAccountId = builder.stripeAccountId;
    this.stripeDetailsSubmitted = builder.stripeDetailsSubmitted;
    this.stripeChargesEnabled = builder.stripeChargesEnabled;
    this.stripePayoutsEnabled = builder.stripePayoutsEnabled;
    this.createdAt = builder.createdAt;
    this.updatedAt = builder.updatedAt;
  }

  public Long getId() {
    return id;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getEmail() {
    return email;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public Role getRole() {
    return role;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public Instant getSuspendedAt() {
    return suspendedAt;
  }

  public User getSuspendedBy() {
    return suspendedBy;
  }

  public String getSuspensionReason() {
    return suspensionReason;
  }

  public void suspend(User administrator, String reason, Instant now) {
    enabled = false;
    suspendedAt = now;
    suspendedBy = administrator;
    suspensionReason = reason;
    updatedAt = now;
  }

  public void reactivate(Instant now) {
    enabled = true;
    suspendedAt = null;
    suspendedBy = null;
    suspensionReason = null;
    updatedAt = now;
  }

  public String getStripeAccountId() {
    return stripeAccountId;
  }

  public boolean isStripeDetailsSubmitted() {
    return stripeDetailsSubmitted;
  }

  public boolean isStripeChargesEnabled() {
    return stripeChargesEnabled;
  }

  public boolean isStripePayoutsEnabled() {
    return stripePayoutsEnabled;
  }

  public void connectStripeAccount(String accountId) {
    stripeAccountId = accountId;
    updatedAt = Instant.now();
  }

  public void updateStripeStatus(
      boolean detailsSubmitted, boolean chargesEnabled, boolean payoutsEnabled) {
    stripeDetailsSubmitted = detailsSubmitted;
    stripeChargesEnabled = chargesEnabled;
    stripePayoutsEnabled = payoutsEnabled;
    updatedAt = Instant.now();
  }

  public void verifyEmail(Instant now) {
    emailVerified = true;
    updatedAt = now;
  }

  public void changePassword(String encodedPassword, Instant now) {
    passwordHash = encodedPassword;
    updatedAt = now;
  }

  public void updateProfile(
      String newFirstName, String newLastName, String newPhoneNumber, Instant now) {
    firstName = newFirstName;
    lastName = newLastName;
    phoneNumber = newPhoneNumber;
    updatedAt = now;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String passwordHash;
    private Role role;
    private boolean emailVerified;
    private boolean enabled;
    private Instant suspendedAt;
    private User suspendedBy;
    private String suspensionReason;
    private String stripeAccountId;
    private boolean stripeDetailsSubmitted;
    private boolean stripeChargesEnabled;
    private boolean stripePayoutsEnabled;
    private Instant createdAt;
    private Instant updatedAt;

    public Builder id(Long id) {
      this.id = id;
      return this;
    }

    public Builder firstName(String firstName) {
      this.firstName = firstName;
      return this;
    }

    public Builder lastName(String lastName) {
      this.lastName = lastName;
      return this;
    }

    public Builder email(String email) {
      this.email = email;
      return this;
    }

    public Builder phoneNumber(String value) {
      phoneNumber = value;
      return this;
    }

    public Builder passwordHash(String passwordHash) {
      this.passwordHash = passwordHash;
      return this;
    }

    public Builder role(Role role) {
      this.role = role;
      return this;
    }

    public Builder emailVerified(boolean emailVerified) {
      this.emailVerified = emailVerified;
      return this;
    }

    public Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public Builder suspendedAt(Instant value) {
      suspendedAt = value;
      return this;
    }

    public Builder suspendedBy(User value) {
      suspendedBy = value;
      return this;
    }

    public Builder suspensionReason(String value) {
      suspensionReason = value;
      return this;
    }

    public Builder stripeAccountId(String value) {
      stripeAccountId = value;
      return this;
    }

    public Builder stripeDetailsSubmitted(boolean value) {
      stripeDetailsSubmitted = value;
      return this;
    }

    public Builder stripeChargesEnabled(boolean value) {
      stripeChargesEnabled = value;
      return this;
    }

    public Builder stripePayoutsEnabled(boolean value) {
      stripePayoutsEnabled = value;
      return this;
    }

    public Builder createdAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder updatedAt(Instant updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public User build() {
      return new User(this);
    }
  }
}
