package com.avbooknest.admin.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avbooknest.admin.dto.AdminDtos.IntegrationEventResponse;
import com.avbooknest.auth.model.RefreshToken;
import com.avbooknest.auth.model.Role;
import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.RefreshTokenRepository;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.auth.service.AccountSecurityService;
import com.avbooknest.book.model.Book;
import com.avbooknest.book.model.BookCondition;
import com.avbooknest.book.model.BookModerationReason;
import com.avbooknest.book.model.BookModerationStatus;
import com.avbooknest.book.model.BookStatus;
import com.avbooknest.book.model.Category;
import com.avbooknest.book.repository.BookRepository;
import com.avbooknest.book.repository.CategoryRepository;
import com.avbooknest.common.exception.ConflictException;
import com.avbooknest.common.exception.ForbiddenException;
import com.avbooknest.integration.model.IntegrationEvent;
import com.avbooknest.integration.model.IntegrationEventStatus;
import com.avbooknest.integration.repository.IntegrationEventRepository;
import com.avbooknest.notification.service.NotificationService;
import com.avbooknest.order.model.OrderIssueStatus;
import com.avbooknest.order.model.SellerOrder;
import com.avbooknest.order.repository.OrderRepository;
import com.avbooknest.order.repository.PaymentRepository;
import com.avbooknest.order.repository.SellerOrderRepository;
import com.avbooknest.payment.model.SellerTransfer;
import com.avbooknest.payment.repository.SellerTransferRepository;
import com.avbooknest.shipment.repository.ShipmentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {
  @Mock private UserRepository userRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private BookRepository bookRepository;
  @Mock private CategoryRepository categoryRepository;
  @Mock private OrderRepository orderRepository;
  @Mock private SellerOrderRepository sellerOrderRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private SellerTransferRepository sellerTransferRepository;
  @Mock private ShipmentRepository shipmentRepository;
  @Mock private IntegrationEventRepository integrationEventRepository;
  @Mock private NotificationService notificationService;
  @Mock private AccountSecurityService accountSecurityService;
  @Mock private AdminAuditService auditService;
  private AdminService adminService;

  @BeforeEach
  void setUp() {
    adminService =
        new AdminService(
            userRepository,
            refreshTokenRepository,
            bookRepository,
            categoryRepository,
            orderRepository,
            sellerOrderRepository,
            paymentRepository,
            sellerTransferRepository,
            shipmentRepository,
            integrationEventRepository,
            notificationService,
            accountSecurityService,
            auditService);
  }

  @Test
  void regularUserCannotUseAdminService() {
    when(userRepository.findByEmail("user@example.com"))
        .thenReturn(Optional.of(user(2L, "user@example.com", "USER")));

    assertThrows(
        ForbiddenException.class, () -> adminService.users("user@example.com", "", null, 0, 25));
  }

  @Test
  void suspendingUserRevokesSessionsAndHidesListings() {
    User administrator = user(1L, "admin@example.com", "ADMIN");
    User target = user(2L, "user@example.com", "USER");
    RefreshToken token =
        RefreshToken.builder()
            .id(4L)
            .user(target)
            .token("hash")
            .expiresAt(Instant.now().plusSeconds(3600))
            .createdAt(Instant.now())
            .build();
    Book book = book(10L, target);
    when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(administrator));
    when(userRepository.findById(2L)).thenReturn(Optional.of(target));
    when(refreshTokenRepository.findAllByUserIdAndRevokedFalse(2L)).thenReturn(List.of(token));
    when(bookRepository.findAllBySellerIdAndModerationStatus(2L, BookModerationStatus.VISIBLE))
        .thenReturn(List.of(book));

    adminService.suspendUser("admin@example.com", 2L, "Suspicious activity");

    assertFalse(target.isEnabled());
    assertTrue(token.isRevoked());
    assertEquals(BookModerationStatus.HIDDEN, book.getModerationStatus());
    assertEquals(BookModerationReason.ACCOUNT_SUSPENDED, book.getModerationReason());
    verify(auditService)
        .record(
            eq(administrator),
            eq("USER_SUSPENDED"),
            eq("USER"),
            eq(2L),
            eq("Suspicious activity"),
            any(String.class));
  }

  @Test
  void administratorAccountsCannotBeSuspended() {
    User administrator = user(1L, "admin@example.com", "ADMIN");
    User anotherAdministrator = user(3L, "other-admin@example.com", "ADMIN");
    when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(administrator));
    when(userRepository.findById(3L)).thenReturn(Optional.of(anotherAdministrator));

    assertThrows(
        ForbiddenException.class,
        () -> adminService.suspendUser("admin@example.com", 3L, "No longer needed"));
    verify(refreshTokenRepository, never()).findAllByUserIdAndRevokedFalse(any());
  }

  @Test
  void onlyFailedIntegrationEventsCanBeRetried() {
    User administrator = user(1L, "admin@example.com", "ADMIN");
    when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(administrator));
    when(integrationEventRepository.findByIdAndStatus(9L, IntegrationEventStatus.FAILED))
        .thenReturn(Optional.empty());

    assertThrows(
        ConflictException.class,
        () -> adminService.retryIntegration("admin@example.com", 9L, "Retry after fix"));
  }

  @Test
  void retryResetsFailedIntegrationEvent() {
    User administrator = user(1L, "admin@example.com", "ADMIN");
    IntegrationEvent event =
        IntegrationEvent.pending("ORDER", 5L, "STRIPE_REFUND_ORDER", "{}", Instant.now());
    for (int index = 0; index < 5; index++) {
      event.scheduleRetry("provider timeout", Instant.now());
    }
    when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(administrator));
    when(integrationEventRepository.findByIdAndStatus(9L, IntegrationEventStatus.FAILED))
        .thenReturn(Optional.of(event));

    IntegrationEventResponse response =
        adminService.retryIntegration("admin@example.com", 9L, "Provider recovered");

    assertEquals(IntegrationEventStatus.PENDING, response.status());
    assertEquals(0, response.attempts());
    verify(auditService)
        .record(
            administrator,
            "INTEGRATION_RETRIED",
            "INTEGRATION_EVENT",
            9L,
            "Provider recovered",
            null);
  }

  @Test
  void refundResolutionIsRejectedAfterSellerTransferWasCreated() {
    User administrator = user(1L, "admin@example.com", "ADMIN");
    User seller = user(2L, "seller@example.com", "USER");
    SellerOrder sellerOrder =
        SellerOrder.builder()
            .id(7L)
            .seller(seller)
            .issueStatus(OrderIssueStatus.OPEN)
            .issueReason("Damaged")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    SellerTransfer transfer =
        SellerTransfer.blocked(sellerOrder, new BigDecimal("50.00"), "RON", Instant.now());
    transfer.markCreated("tr_123", Instant.now());
    when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(administrator));
    when(sellerOrderRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(sellerOrder));
    when(sellerTransferRepository.findBySellerOrderId(7L)).thenReturn(Optional.of(transfer));

    assertThrows(
        ConflictException.class,
        () ->
            adminService.resolveIssue(
                "admin@example.com",
                7L,
                new com.avbooknest.admin.dto.AdminRequests.IssueResolutionRequest(
                    com.avbooknest.order.model.IssueResolution.REFUND_BUYER, "Refund approved")));
    verify(integrationEventRepository, never()).save(any());
  }

  private User user(Long id, String email, String roleName) {
    Instant now = Instant.now();
    return User.builder()
        .id(id)
        .firstName("Test")
        .lastName("User")
        .email(email)
        .passwordHash("password")
        .role(Role.builder().id(id).name(roleName).build())
        .enabled(true)
        .emailVerified(true)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  private Book book(Long id, User seller) {
    Instant now = Instant.now();
    Category category =
        Category.builder()
            .id(3L)
            .name("Fiction")
            .slug("fiction")
            .createdAt(now)
            .updatedAt(now)
            .build();
    return Book.builder()
        .id(id)
        .title("Book")
        .author("Author")
        .price(new BigDecimal("20.00"))
        .bookCondition(BookCondition.GOOD)
        .language("Romanian")
        .seller(seller)
        .category(category)
        .status(BookStatus.AVAILABLE)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }
}
