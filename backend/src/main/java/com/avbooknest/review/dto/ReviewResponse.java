package com.avbooknest.review.dto;

import com.avbooknest.review.model.Review;
import java.time.Instant;

/** Public feedback: no email, order identifiers or moderation notes. */
public record ReviewResponse(
    Long id,
    Long bookId,
    String bookTitle,
    Long sellerId,
    String reviewerName,
    Short sellerRating,
    Short descriptionRating,
    Short conditionRating,
    String comment,
    boolean verifiedPurchase,
    Instant createdAt) {
  public static ReviewResponse from(Review review) {
    String lastName = review.getReviewer().getLastName();
    String name =
        review.getReviewer().getFirstName()
            + (lastName == null || lastName.isBlank() ? "" : " " + lastName.charAt(0) + ".");
    return new ReviewResponse(
        review.getId(),
        review.getBook() == null ? null : review.getBook().getId(),
        review.getBookTitle(),
        review.getSeller().getId(),
        name,
        review.getSellerRating(),
        review.getDescriptionRating(),
        review.getConditionRating(),
        review.getComment(),
        review.getOrderItem() != null,
        review.getCreatedAt());
  }
}
