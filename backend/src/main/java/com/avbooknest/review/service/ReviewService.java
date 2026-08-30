package com.avbooknest.review.service;

import com.avbooknest.admin.service.AdminAuditService;
import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.common.dto.PageResponse;
import com.avbooknest.common.exception.*;
import com.avbooknest.order.model.Order;
import com.avbooknest.order.model.OrderItem;
import com.avbooknest.order.repository.OrderRepository;
import com.avbooknest.review.dto.*;
import com.avbooknest.review.model.Review;
import com.avbooknest.review.model.ReviewModerationStatus;
import com.avbooknest.review.repository.ReviewRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReviewService {
  private final ReviewRepository reviews;
  private final OrderRepository orders;
  private final UserRepository users;
  private final AdminAuditService audit;

  public ReviewService(
      ReviewRepository reviews,
      OrderRepository orders,
      UserRepository users,
      AdminAuditService audit) {
    this.reviews = reviews;
    this.orders = orders;
    this.users = users;
    this.audit = audit;
  }

  public PrivateReviewResponse create(
      Long orderId, Long itemId, String email, CreateReviewRequest request) {
    User buyer = activeUser(email);
    // Serialize duplicate submissions with the same lock used by order state changes.
    Order order =
        orders
            .findByIdAndBuyerIdForUpdate(orderId, buyer.getId())
            .orElseThrow(() -> new NotFoundException("Order not found"));
    OrderItem item =
        order.getItems().stream()
            .filter(candidate -> candidate.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Order item not found"));
    if (reviews.existsByOrderItemId(itemId))
      throw new ConflictException("This purchase already has a review");
    String reason = ineligibility(item, buyer);
    if (reason != null) throw new ConflictException(reason);
    validate(request);
    return PrivateReviewResponse.from(
        reviews.saveAndFlush(Review.create(item, buyer, request, Instant.now())));
  }

  @Transactional(readOnly = true)
  public List<OrderReviewResponse> forOrder(Long orderId, String email) {
    User buyer = activeUser(email);
    Order order =
        orders
            .findByIdAndBuyerId(orderId, buyer.getId())
            .orElseThrow(() -> new NotFoundException("Order not found"));
    var existing =
        reviews.findAllByOrderItemOrderId(orderId).stream()
            .collect(Collectors.toMap(r -> r.getOrderItem().getId(), Function.identity()));
    return order.getItems().stream()
        .map(
            item -> {
              Review review = existing.get(item.getId());
              String reason = review == null ? ineligibility(item, buyer) : "ALREADY_REVIEWED";
              return new OrderReviewResponse(
                  item.getId(),
                  item.getSeller().getId(),
                  item.getTitle(),
                  reason == null,
                  reason,
                  review == null ? null : PrivateReviewResponse.from(review));
            })
        .toList();
  }

  @Transactional(readOnly = true)
  public SellerReputationResponse reputation(Long sellerId) {
    User seller = seller(sellerId);
    return new SellerReputationResponse(
        sellerId, seller.getFirstName() + " " + seller.getLastName(), reviews.reputation(sellerId));
  }

  @Transactional(readOnly = true)
  public PageResponse<ReviewResponse> publicReviews(Long sellerId, int page, int size) {
    var pageable = pageable(page, size);
    seller(sellerId);
    return PageResponse.from(reviews.publicReviews(sellerId, pageable), ReviewResponse::from);
  }

  @Transactional(readOnly = true)
  public PageResponse<PrivateReviewResponse> adminReviews(
      String email, ReviewModerationStatus status, String query, int page, int size) {
    administrator(email);
    String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
    if (normalized.length() > 200)
      throw new BadRequestException("Search must not exceed 200 characters");
    return PageResponse.from(
        reviews.searchForAdmin(status, normalized, pageable(page, size)),
        PrivateReviewResponse::from);
  }

  public PrivateReviewResponse moderate(
      Long reviewId, String email, ModerateReviewRequest request) {
    User admin = administrator(email);
    if (request.status() == null
        || request.reason() == null
        || request.reason().isBlank()
        || request.reason().length() > 500) {
      throw new BadRequestException(
          "A moderation status and a reason of up to 500 characters are required");
    }
    Review review =
        reviews
            .findByIdForUpdate(reviewId)
            .orElseThrow(() -> new NotFoundException("Review not found"));
    if (request.status() == ReviewModerationStatus.VISIBLE && review.getOrderItem() == null) {
      throw new ConflictException("Legacy reviews cannot be published as verified purchases");
    }
    if (review.getModerationStatus() == request.status()) return PrivateReviewResponse.from(review);
    String previous = review.getModerationStatus().name();
    review.moderate(request.status(), request.reason().trim(), admin, Instant.now());
    audit.record(
        admin,
        request.status() == ReviewModerationStatus.HIDDEN ? "REVIEW_HIDDEN" : "REVIEW_RESTORED",
        "REVIEW",
        reviewId,
        request.reason(),
        previous + " -> " + request.status());
    return PrivateReviewResponse.from(review);
  }

  private String ineligibility(OrderItem item, User buyer) {
    if (item.getSeller().getId().equals(buyer.getId())) return "SELF_REVIEW";
    // Delivery is per seller parcel, not the aggregate order. A later dispute/refund
    // does not erase the buyer's delivered-purchase experience or prevent negative feedback.
    if (item.getSellerOrder() == null
        || item.getSellerOrder().getFulfilledAt() == null
        || item.getSellerOrder().getFulfilledAt().isAfter(Instant.now())) return "NOT_DELIVERED";
    return null;
  }

  private void validate(CreateReviewRequest request) {
    if (!validRating(request.sellerRating())
        || !validRating(request.descriptionRating())
        || !validRating(request.conditionRating())) {
      throw new BadRequestException("Ratings must be between 1 and 5");
    }
    if (request.comment() != null && request.comment().length() > 2000)
      throw new BadRequestException("Review comment must not exceed 2000 characters");
  }

  private boolean validRating(Integer value) {
    return value != null && value >= 1 && value <= 5;
  }

  private User activeUser(String email) {
    User user =
        users
            .findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    if (!user.isEnabled()) throw new ForbiddenException("Account is suspended");
    return user;
  }

  private User administrator(String email) {
    User user = activeUser(email);
    if (user.getRole() == null || !"ADMIN".equals(user.getRole().getName()))
      throw new ForbiddenException("Administrator access required");
    return user;
  }

  private User seller(Long id) {
    return users.findSellerById(id).orElseThrow(() -> new NotFoundException("Seller not found"));
  }

  private PageRequest pageable(int page, int size) {
    if (page < 0 || size < 1 || size > 100)
      throw new BadRequestException("Page must be non-negative and size between 1 and 100");
    return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
  }
}
