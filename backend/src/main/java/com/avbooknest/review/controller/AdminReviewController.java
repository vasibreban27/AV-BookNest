package com.avbooknest.review.controller;

import com.avbooknest.common.dto.PageResponse;
import com.avbooknest.review.dto.ModerateReviewRequest;
import com.avbooknest.review.dto.PrivateReviewResponse;
import com.avbooknest.review.model.ReviewModerationStatus;
import com.avbooknest.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {
  private final ReviewService reviews;

  public AdminReviewController(ReviewService reviews) {
    this.reviews = reviews;
  }

  @GetMapping
  public PageResponse<PrivateReviewResponse> list(
      Authentication auth,
      @RequestParam(required = false) ReviewModerationStatus status,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    return reviews.adminReviews(auth.getName(), status, q, page, size);
  }

  @PatchMapping("/{reviewId}/moderation")
  public PrivateReviewResponse moderate(
      @PathVariable Long reviewId,
      @Valid @RequestBody ModerateReviewRequest request,
      Authentication auth) {
    return reviews.moderate(reviewId, auth.getName(), request);
  }
}
