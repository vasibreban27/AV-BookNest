package com.avbooknest.review.dto;

public record OrderReviewResponse(
    Long orderItemId,
    Long sellerId,
    String bookTitle,
    boolean canReview,
    String ineligibilityReason,
    PrivateReviewResponse existingReview) {}
