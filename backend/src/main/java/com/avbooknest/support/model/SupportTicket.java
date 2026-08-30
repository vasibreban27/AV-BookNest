package com.avbooknest.support.model;

import com.avbooknest.auth.model.User;
import com.avbooknest.book.model.Book;
import com.avbooknest.order.model.Order;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "support_tickets")
public class SupportTicket {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "reference", nullable = false, unique = true, length = 40)
  private String reference;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requester_id")
  private User requester;

  @Column(name = "contact_name", nullable = false, length = 201)
  private String contactName;

  @Column(name = "contact_email", nullable = false, length = 254)
  private String contactEmail;

  @Enumerated(EnumType.STRING)
  @Column(name = "topic", nullable = false, length = 20)
  private SupportTopic topic;

  @Column(name = "subject", nullable = false, length = 150)
  private String subject;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private SupportStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "related_order_id")
  private Order relatedOrder;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "related_book_id")
  private Book relatedBook;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assigned_to_id")
  private User assignedTo;

  @Column(name = "privacy_accepted_at", nullable = false)
  private Instant privacyAcceptedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "resolved_at")
  private Instant resolvedAt;

  @Column(name = "closed_at")
  private Instant closedAt;

  protected SupportTicket() {}

  public static SupportTicket create(
      User requester,
      String name,
      String email,
      SupportTopic topic,
      String subject,
      Order order,
      Book book,
      Instant now) {
    SupportTicket ticket = new SupportTicket();
    ticket.reference =
        "BN-SUP-"
            + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 20)
                .toUpperCase(Locale.ROOT);
    ticket.requester = requester;
    ticket.contactName = name.trim();
    ticket.contactEmail = email.trim().toLowerCase(Locale.ROOT);
    ticket.topic = topic;
    ticket.subject = subject.trim();
    ticket.relatedOrder = order;
    ticket.relatedBook = book;
    ticket.status = SupportStatus.NEW;
    ticket.privacyAcceptedAt = now;
    ticket.createdAt = now;
    ticket.updatedAt = now;
    return ticket;
  }

  public void changeStatus(SupportStatus status, Instant now) {
    this.status = status;
    resolvedAt = status == SupportStatus.RESOLVED ? now : null;
    closedAt = status == SupportStatus.CLOSED ? now : null;
    updatedAt = now;
  }

  public void assign(User administrator, Instant now) {
    assignedTo = administrator;
    updatedAt = now;
  }

  public void touch(Instant now) {
    updatedAt = now;
  }

  public Long getId() {
    return id;
  }

  public String getReference() {
    return reference;
  }

  public User getRequester() {
    return requester;
  }

  public String getContactName() {
    return contactName;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public SupportTopic getTopic() {
    return topic;
  }

  public String getSubject() {
    return subject;
  }

  public SupportStatus getStatus() {
    return status;
  }

  public Order getRelatedOrder() {
    return relatedOrder;
  }

  public Book getRelatedBook() {
    return relatedBook;
  }

  public User getAssignedTo() {
    return assignedTo;
  }

  public Instant getPrivacyAcceptedAt() {
    return privacyAcceptedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getResolvedAt() {
    return resolvedAt;
  }

  public Instant getClosedAt() {
    return closedAt;
  }
}
