package com.avbooknest.review.dto;

import com.avbooknest.review.model.Review;
import com.avbooknest.review.model.ReviewModerationStatus;
import java.time.Instant;

public record PrivateReviewResponse(
    ReviewResponse review,
    Long orderItemId,
    ReviewModerationStatus moderationStatus,
    String moderationReason,
    Instant moderatedAt) {
  public static PrivateReviewResponse from(Review review) {
    return new PrivateReviewResponse(
        ReviewResponse.from(review),
        review.getOrderItem() == null ? null : review.getOrderItem().getId(),
        review.getModerationStatus(),
        review.getModerationReason(),
        review.getModeratedAt());
  }
}
