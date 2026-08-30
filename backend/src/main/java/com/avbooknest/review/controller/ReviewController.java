package com.avbooknest.review.controller;

import com.avbooknest.common.dto.PageResponse;
import com.avbooknest.review.dto.*;
import com.avbooknest.review.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ReviewController {
  private final ReviewService reviews;

  public ReviewController(ReviewService reviews) {
    this.reviews = reviews;
  }

  @GetMapping("/orders/{orderId}/reviews")
  public List<OrderReviewResponse> forOrder(@PathVariable Long orderId, Authentication auth) {
    return reviews.forOrder(orderId, auth.getName());
  }

  @PostMapping("/orders/{orderId}/items/{itemId}/review")
  @ResponseStatus(HttpStatus.CREATED)
  public PrivateReviewResponse create(
      @PathVariable Long orderId,
      @PathVariable Long itemId,
      @Valid @RequestBody CreateReviewRequest request,
      Authentication auth) {
    return reviews.create(orderId, itemId, auth.getName(), request);
  }

  @GetMapping("/sellers/{sellerId}/reputation")
  public SellerReputationResponse reputation(@PathVariable Long sellerId) {
    return reviews.reputation(sellerId);
  }

  @GetMapping("/sellers/{sellerId}/reviews")
  public PageResponse<ReviewResponse> publicReviews(
      @PathVariable Long sellerId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    return reviews.publicReviews(sellerId, page, size);
  }
}
