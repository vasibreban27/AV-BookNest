package com.avbooknest.review.model;

import com.avbooknest.auth.model.User;
import com.avbooknest.book.model.Book;
import com.avbooknest.order.model.OrderItem;
import com.avbooknest.review.dto.CreateReviewRequest;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reviews")
public class Review {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "book_id")
  private Book book;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_item_id", unique = true)
  private OrderItem orderItem;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reviewer_id", nullable = false)
  private User reviewer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "seller_id", nullable = false)
  private User seller;

  @Column(name = "book_title", nullable = false, length = 255)
  private String bookTitle;

  @Column(name = "rating", nullable = false)
  private Short sellerRating;

  @Column(name = "description_rating")
  private Short descriptionRating;

  @Column(name = "condition_rating")
  private Short conditionRating;

  @Column(columnDefinition = "text")
  private String comment;

  @Enumerated(EnumType.STRING)
  @Column(name = "moderation_status", nullable = false, length = 20)
  private ReviewModerationStatus moderationStatus;

  @Column(name = "moderation_reason", length = 500)
  private String moderationReason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "moderated_by")
  private User moderatedBy;

  @Column(name = "moderated_at")
  private Instant moderatedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Review() {}

  public static Review create(
      OrderItem item, User buyer, CreateReviewRequest request, Instant now) {
    Review review = new Review();
    review.book = item.getBook();
    review.bookTitle = item.getTitle();
    review.orderItem = item;
    review.reviewer = buyer;
    review.seller = item.getSeller();
    review.sellerRating = request.sellerRating().shortValue();
    review.descriptionRating = request.descriptionRating().shortValue();
    review.conditionRating = request.conditionRating().shortValue();
    review.comment =
        request.comment() == null || request.comment().isBlank() ? null : request.comment().trim();
    review.moderationStatus = ReviewModerationStatus.VISIBLE;
    review.createdAt = now;
    review.updatedAt = now;
    return review;
  }

  public void moderate(
      ReviewModerationStatus status, String reason, User administrator, Instant now) {
    moderationStatus = status;
    moderationReason = reason;
    moderatedBy = administrator;
    moderatedAt = now;
    updatedAt = now;
  }

  public Long getId() {
    return id;
  }

  public Book getBook() {
    return book;
  }

  public OrderItem getOrderItem() {
    return orderItem;
  }

  public User getReviewer() {
    return reviewer;
  }

  public User getSeller() {
    return seller;
  }

  public String getBookTitle() {
    return bookTitle;
  }

  public Short getSellerRating() {
    return sellerRating;
  }

  public Short getDescriptionRating() {
    return descriptionRating;
  }

  public Short getConditionRating() {
    return conditionRating;
  }

  public String getComment() {
    return comment;
  }

  public ReviewModerationStatus getModerationStatus() {
    return moderationStatus;
  }

  public String getModerationReason() {
    return moderationReason;
  }

  public User getModeratedBy() {
    return moderatedBy;
  }

  public Instant getModeratedAt() {
    return moderatedAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
