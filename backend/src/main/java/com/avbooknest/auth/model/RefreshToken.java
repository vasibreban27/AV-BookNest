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
@Table(name = "refresh_tokens")
public class RefreshToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  /** SHA-256 hash of the JWT; the raw token is never stored. */
  @Column(nullable = false, unique = true, length = 512)
  private String token;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(nullable = false)
  private boolean revoked;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  public void revoke() {
    this.revoked = true;
  }

  protected RefreshToken() {}

  private RefreshToken(Builder builder) {
    this.id = builder.id;
    this.user = builder.user;
    this.token = builder.token;
    this.expiresAt = builder.expiresAt;
    this.revoked = builder.revoked;
    this.createdAt = builder.createdAt;
  }

  public Long getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public String getToken() {
    return token;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public boolean isRevoked() {
    return revoked;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Long id;
    private User user;
    private String token;
    private Instant expiresAt;
    private boolean revoked;
    private Instant createdAt;

    public Builder id(Long id) {
      this.id = id;
      return this;
    }

    public Builder user(User user) {
      this.user = user;
      return this;
    }

    public Builder token(String token) {
      this.token = token;
      return this;
    }

    public Builder expiresAt(Instant expiresAt) {
      this.expiresAt = expiresAt;
      return this;
    }

    public Builder revoked(boolean revoked) {
      this.revoked = revoked;
      return this;
    }

    public Builder createdAt(Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public RefreshToken build() {
      return new RefreshToken(this);
    }
  }
}
