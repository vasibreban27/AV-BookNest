package com.avbooknest.notification.model;

import com.avbooknest.auth.model.User;
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
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private NotificationType type;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String message;

  @Column(name = "read_at")
  private Instant readAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Notification() {}

  private Notification(Builder b) {
    id = b.id;
    user = b.user;
    type = b.type;
    title = b.title;
    message = b.message;
    readAt = b.readAt;
    createdAt = b.createdAt;
  }

  public Long getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public NotificationType getType() {
    return type;
  }

  public String getTitle() {
    return title;
  }

  public String getMessage() {
    return message;
  }

  public Instant getReadAt() {
    return readAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void markRead() {
    if (readAt == null) readAt = Instant.now();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Long id;
    private User user;
    private NotificationType type;
    private String title;
    private String message;
    private Instant readAt;
    private Instant createdAt;

    public Builder id(Long v) {
      id = v;
      return this;
    }

    public Builder user(User v) {
      user = v;
      return this;
    }

    public Builder type(NotificationType v) {
      type = v;
      return this;
    }

    public Builder title(String v) {
      title = v;
      return this;
    }

    public Builder message(String v) {
      message = v;
      return this;
    }

    public Builder readAt(Instant v) {
      readAt = v;
      return this;
    }

    public Builder createdAt(Instant v) {
      createdAt = v;
      return this;
    }

    public Notification build() {
      return new Notification(this);
    }
  }
}
