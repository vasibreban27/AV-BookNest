package com.avbooknest.review.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.avbooknest.admin.service.AdminAuditService;
import com.avbooknest.auth.model.Role;
import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.common.exception.*;
import com.avbooknest.order.model.*;
import com.avbooknest.order.repository.OrderRepository;
import com.avbooknest.review.dto.*;
import com.avbooknest.review.model.*;
import com.avbooknest.review.repository.ReviewRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
  @Mock ReviewRepository reviews;
  @Mock OrderRepository orders;
  @Mock UserRepository users;
  @Mock AdminAuditService audit;
  ReviewService service;
  User buyer = user(1L, "USER", true);
  User seller = user(2L, "USER", true);
  User admin = user(3L, "ADMIN", true);
  Order order;
  OrderItem item;
  SellerOrder parcel;
  CreateReviewRequest request = new CreateReviewRequest(5, 4, 3, "  Cartea a ajuns.  ");

  @BeforeEach
  void setup() {
    service = new ReviewService(reviews, orders, users, audit);
    order = Order.builder().id(10L).buyer(buyer).status(OrderStatus.SHIPPED).build();
    parcel =
        SellerOrder.builder()
            .id(20L)
            .order(order)
            .seller(seller)
            .status(SellerOrderStatus.FULFILLED)
            .fulfilledAt(Instant.now().minusSeconds(60))
            .build();
    item =
        OrderItem.builder().id(30L).order(order).seller(seller).title("Titlu la cumpărare").build();
    order.addItem(item);
    parcel.addItem(item);
  }

  @Test
  void deliveredParcelCanBeReviewedWhileOtherParcelsAreStillInTransit() {
    createSetup();
    when(reviews.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
    var result = service.create(10L, 30L, buyer.getEmail(), request);
    assertTrue(result.review().verifiedPurchase());
    assertEquals(5, result.review().sellerRating().intValue());
    assertEquals(4, result.review().descriptionRating().intValue());
    assertEquals(3, result.review().conditionRating().intValue());
    assertEquals("Cartea a ajuns.", result.review().comment());
    assertEquals("Titlu la cumpărare", result.review().bookTitle());
    assertEquals("Ana P.", result.review().reviewerName());
    assertEquals(ReviewModerationStatus.VISIBLE, result.moderationStatus());
    verify(orders).findByIdAndBuyerIdForUpdate(10L, buyer.getId());
  }

  @Test
  void duplicateIncludingHiddenReviewIsRejected() {
    createSetup();
    when(reviews.existsByOrderItemId(30L)).thenReturn(true);
    assertThrows(
        ConflictException.class, () -> service.create(10L, 30L, buyer.getEmail(), request));
    verify(reviews, never()).saveAndFlush(any());
  }

  @Test
  void unfulfilledPurchaseIsRejected() {
    createSetup();
    SellerOrder.builder().status(SellerOrderStatus.ACCEPTED).build().addItem(item);
    assertEquals(
        "NOT_DELIVERED",
        assertThrows(
                ConflictException.class, () -> service.create(10L, 30L, buyer.getEmail(), request))
            .getMessage());
  }

  @Test
  void futureDeliveryDoesNotGrantEligibility() {
    createSetup();
    SellerOrder.builder().fulfilledAt(Instant.now().plusSeconds(3600)).build().addItem(item);
    assertThrows(
        ConflictException.class, () -> service.create(10L, 30L, buyer.getEmail(), request));
  }

  @Test
  void selfReviewIsRejected() {
    createSetup();
    OrderItem own = OrderItem.builder().id(31L).seller(buyer).build();
    parcel.addItem(own);
    order.addItem(own);
    assertEquals(
        "SELF_REVIEW",
        assertThrows(
                ConflictException.class, () -> service.create(10L, 31L, buyer.getEmail(), request))
            .getMessage());
  }

  @Test
  void anotherBuyersOrderIsNotExposed() {
    active(buyer);
    assertThrows(
        NotFoundException.class, () -> service.create(10L, 30L, buyer.getEmail(), request));
    assertThrows(NotFoundException.class, () -> service.forOrder(10L, buyer.getEmail()));
    verifyNoInteractions(reviews);
  }

  @Test
  void itemFromDifferentOrderIsRejected() {
    createSetup();
    assertThrows(
        NotFoundException.class, () -> service.create(10L, 999L, buyer.getEmail(), request));
  }

  @Test
  void disabledBuyerCannotSubmit() {
    User suspended = user(1L, "USER", false);
    active(suspended);
    assertThrows(
        ForbiddenException.class, () -> service.create(10L, 30L, suspended.getEmail(), request));
    verifyNoInteractions(orders, reviews);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(ints = {-1, 0, 6, 100})
  void allRatingDimensionsAreValidated(Integer invalid) {
    createSetup();
    for (CreateReviewRequest input :
        List.of(
            new CreateReviewRequest(invalid, 4, 3, null),
            new CreateReviewRequest(5, invalid, 3, null),
            new CreateReviewRequest(5, 4, invalid, null))) {
      assertThrows(
          BadRequestException.class, () -> service.create(10L, 30L, buyer.getEmail(), input));
    }
    verify(reviews, never()).saveAndFlush(any());
  }

  @Test
  void oversizedCommentIsRejected() {
    createSetup();
    assertThrows(
        BadRequestException.class,
        () ->
            service.create(
                10L, 30L, buyer.getEmail(), new CreateReviewRequest(1, 1, 1, "a".repeat(2001))));
  }

  @Test
  void buyerCanSeeOwnHiddenReviewButCannotSubmitAgain() {
    active(buyer);
    when(orders.findByIdAndBuyerId(10L, 1L)).thenReturn(Optional.of(order));
    Review review = Review.create(item, buyer, request, Instant.now());
    review.moderate(ReviewModerationStatus.HIDDEN, "Date personale", admin, Instant.now());
    when(reviews.findAllByOrderItemOrderId(10L)).thenReturn(List.of(review));
    var result = service.forOrder(10L, buyer.getEmail()).getFirst();
    assertFalse(result.canReview());
    assertEquals("ALREADY_REVIEWED", result.ineligibilityReason());
    assertEquals("Date personale", result.existingReview().moderationReason());
  }

  @Test
  void openDisputeAndLaterRefundDoNotSilenceDeliveredPurchaseFeedback() {
    active(buyer);
    when(orders.findByIdAndBuyerId(10L, 1L)).thenReturn(Optional.of(order));
    parcel.openIssue("Cartea este deteriorată", Instant.now());
    parcel.cancelPreservingShipment(Instant.now());
    assertTrue(service.forOrder(10L, buyer.getEmail()).getFirst().canReview());
  }

  @Test
  void moderationRequiresAdminEvenWhenServiceIsCalledDirectly() {
    active(buyer);
    assertThrows(
        ForbiddenException.class, () -> service.adminReviews(buyer.getEmail(), null, "", 0, 10));
    assertThrows(
        ForbiddenException.class,
        () ->
            service.moderate(
                40L,
                buyer.getEmail(),
                new ModerateReviewRequest(ReviewModerationStatus.HIDDEN, "Spam")));
    verifyNoInteractions(reviews, audit);
  }

  @Test
  void hideAndRestoreAreAuditedWithoutChangingRatings() {
    active(admin);
    Review review = Review.create(item, buyer, request, Instant.now());
    when(reviews.findByIdForUpdate(40L)).thenReturn(Optional.of(review));
    service.moderate(
        40L,
        admin.getEmail(),
        new ModerateReviewRequest(ReviewModerationStatus.HIDDEN, " Date personale "));
    assertEquals(ReviewModerationStatus.HIDDEN, review.getModerationStatus());
    assertEquals("Date personale", review.getModerationReason());
    verify(audit)
        .record(
            eq(admin),
            eq("REVIEW_HIDDEN"),
            eq("REVIEW"),
            eq(40L),
            anyString(),
            eq("VISIBLE -> HIDDEN"));
    service.moderate(
        40L,
        admin.getEmail(),
        new ModerateReviewRequest(ReviewModerationStatus.VISIBLE, "Contestație acceptată"));
    assertEquals(ReviewModerationStatus.VISIBLE, review.getModerationStatus());
    assertEquals(5, review.getSellerRating().intValue());
    verify(audit)
        .record(
            eq(admin),
            eq("REVIEW_RESTORED"),
            eq("REVIEW"),
            eq(40L),
            anyString(),
            eq("HIDDEN -> VISIBLE"));
  }

  @Test
  void legacyReviewCannotBeRestored() {
    active(admin);
    Review legacy = mock(Review.class);
    when(reviews.findByIdForUpdate(40L)).thenReturn(Optional.of(legacy));
    assertThrows(
        ConflictException.class,
        () ->
            service.moderate(
                40L,
                admin.getEmail(),
                new ModerateReviewRequest(ReviewModerationStatus.VISIBLE, "Restaurare")));
    verifyNoInteractions(audit);
  }

  @Test
  void blankModerationReasonIsRejected() {
    active(admin);
    assertThrows(
        BadRequestException.class,
        () ->
            service.moderate(
                40L,
                admin.getEmail(),
                new ModerateReviewRequest(ReviewModerationStatus.HIDDEN, "  ")));
    verifyNoInteractions(reviews, audit);
  }

  @Test
  void invalidPaginationIsRejectedBeforeQuerying() {
    assertThrows(BadRequestException.class, () -> service.publicReviews(2L, -1, 10));
    assertThrows(BadRequestException.class, () -> service.publicReviews(2L, 0, 101));
    assertThrows(BadRequestException.class, () -> service.publicReviews(2L, 0, 0));
    verifyNoInteractions(reviews);
  }

  private void active(User user) {
    when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
  }

  private void createSetup() {
    active(buyer);
    when(orders.findByIdAndBuyerIdForUpdate(10L, 1L)).thenReturn(Optional.of(order));
  }

  private static User user(Long id, String role, boolean enabled) {
    return User.builder()
        .id(id)
        .firstName("Ana")
        .lastName("Popescu")
        .email("user" + id + "@example.com")
        .enabled(enabled)
        .role(Role.builder().name(role).build())
        .build();
  }
}
