package com.avbooknest.support.model;

import com.avbooknest.auth.model.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "support_messages")
public class SupportMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "ticket_id", nullable = false)
  private SupportTicket ticket;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_id")
  private User author;

  @Enumerated(EnumType.STRING)
  @Column(name = "kind", nullable = false, length = 20)
  private SupportMessageKind kind;

  @Column(name = "body", nullable = false, length = 4000)
  private String body;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected SupportMessage() {}

  public static SupportMessage create(
      SupportTicket ticket, User author, SupportMessageKind kind, String body, Instant now) {
    SupportMessage message = new SupportMessage();
    message.ticket = ticket;
    message.author = author;
    message.kind = kind;
    message.body = body.trim();
    message.createdAt = now;
    return message;
  }

  public Long getId() {
    return id;
  }

  public SupportTicket getTicket() {
    return ticket;
  }

  public User getAuthor() {
    return author;
  }

  public SupportMessageKind getKind() {
    return kind;
  }

  public String getBody() {
    return body;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
