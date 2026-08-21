package com.avbooknest.admin.service;

import com.avbooknest.admin.dto.AdminDtos.BookAdminResponse;
import com.avbooknest.admin.dto.AdminDtos.CategoryAdminResponse;
import com.avbooknest.admin.dto.AdminDtos.DashboardResponse;
import com.avbooknest.admin.dto.AdminDtos.IntegrationEventResponse;
import com.avbooknest.admin.dto.AdminDtos.IssueAdminResponse;
import com.avbooknest.admin.dto.AdminDtos.OrderDetailsResponse;
import com.avbooknest.admin.dto.AdminDtos.OrderSummaryResponse;
import com.avbooknest.admin.dto.AdminDtos.ShipmentOperationResponse;
import com.avbooknest.admin.dto.AdminDtos.TransferOperationResponse;
import com.avbooknest.admin.dto.AdminDtos.UserDetailsResponse;
import com.avbooknest.admin.dto.AdminDtos.UserSummaryResponse;
import com.avbooknest.admin.dto.AdminRequests.BookModerationRequest;
import com.avbooknest.admin.dto.AdminRequests.CategoryUpdateRequest;
import com.avbooknest.admin.dto.AdminRequests.IssueResolutionRequest;
import com.avbooknest.auth.model.RefreshToken;
import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.RefreshTokenRepository;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.auth.service.AccountSecurityService;
import com.avbooknest.book.dto.CreateCategoryRequest;
import com.avbooknest.book.model.Book;
import com.avbooknest.book.model.BookModerationReason;
import com.avbooknest.book.model.BookModerationStatus;
import com.avbooknest.book.model.BookStatus;
import com.avbooknest.book.model.Category;
import com.avbooknest.book.repository.BookRepository;
import com.avbooknest.book.repository.CategoryRepository;
import com.avbooknest.common.dto.PageResponse;
import com.avbooknest.common.exception.BadRequestException;
import com.avbooknest.common.exception.ConflictException;
import com.avbooknest.common.exception.ForbiddenException;
import com.avbooknest.common.exception.NotFoundException;
import com.avbooknest.integration.model.IntegrationEvent;
import com.avbooknest.integration.model.IntegrationEventStatus;
import com.avbooknest.integration.repository.IntegrationEventRepository;
import com.avbooknest.notification.model.NotificationType;
import com.avbooknest.notification.service.NotificationService;
import com.avbooknest.order.dto.OrderResponse;
import com.avbooknest.order.dto.PaymentResponse;
import com.avbooknest.order.dto.SellerOrderResponse;
import com.avbooknest.order.model.IssueResolution;
import com.avbooknest.order.model.OrderIssueStatus;
import com.avbooknest.order.model.OrderStatus;
import com.avbooknest.order.model.SellerOrder;
import com.avbooknest.order.repository.OrderRepository;
import com.avbooknest.order.repository.PaymentRepository;
import com.avbooknest.order.repository.SellerOrderRepository;
import com.avbooknest.payment.model.SellerTransfer;
import com.avbooknest.payment.model.SellerTransferStatus;
import com.avbooknest.payment.repository.SellerTransferRepository;
import com.avbooknest.shipment.model.ShipmentStatus;
import com.avbooknest.shipment.repository.ShipmentRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminService {
  private static final List<ShipmentStatus> SHIPMENT_EXCEPTIONS =
      List.of(ShipmentStatus.RETURNED, ShipmentStatus.LOST);

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final BookRepository bookRepository;
  private final CategoryRepository categoryRepository;
  private final OrderRepository orderRepository;
  private final SellerOrderRepository sellerOrderRepository;
  private final PaymentRepository paymentRepository;
  private final SellerTransferRepository sellerTransferRepository;
  private final ShipmentRepository shipmentRepository;
  private final IntegrationEventRepository integrationEventRepository;
  private final NotificationService notificationService;
  private final AccountSecurityService accountSecurityService;
  private final AdminAuditService auditService;

  public AdminService(
      UserRepository userRepository,
      RefreshTokenRepository refreshTokenRepository,
      BookRepository bookRepository,
      CategoryRepository categoryRepository,
      OrderRepository orderRepository,
      SellerOrderRepository sellerOrderRepository,
      PaymentRepository paymentRepository,
      SellerTransferRepository sellerTransferRepository,
      ShipmentRepository shipmentRepository,
      IntegrationEventRepository integrationEventRepository,
      NotificationService notificationService,
      AccountSecurityService accountSecurityService,
      AdminAuditService auditService) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.bookRepository = bookRepository;
    this.categoryRepository = categoryRepository;
    this.orderRepository = orderRepository;
    this.sellerOrderRepository = sellerOrderRepository;
    this.paymentRepository = paymentRepository;
    this.sellerTransferRepository = sellerTransferRepository;
    this.shipmentRepository = shipmentRepository;
    this.integrationEventRepository = integrationEventRepository;
    this.notificationService = notificationService;
    this.accountSecurityService = accountSecurityService;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public DashboardResponse dashboard(String administratorEmail) {
    administrator(administratorEmail);
    return new DashboardResponse(
        userRepository.countByCreatedAtGreaterThanEqual(Instant.now().minus(7, ChronoUnit.DAYS)),
        bookRepository.countByStatusAndModerationStatus(
            BookStatus.AVAILABLE, BookModerationStatus.VISIBLE),
        orderRepository.countByStatusIn(
            List.of(
                OrderStatus.PAID,
                OrderStatus.PROCESSING,
                OrderStatus.SHIPPED,
                OrderStatus.DELIVERED)),
        sellerOrderRepository.countByIssueStatus(OrderIssueStatus.OPEN),
        shipmentRepository.countByStatusIn(SHIPMENT_EXCEPTIONS),
        sellerTransferRepository.countByStatus(SellerTransferStatus.FAILED),
        integrationEventRepository.countByStatus(IntegrationEventStatus.FAILED),
        paymentRepository.sumNetCapturedAmount(),
        sellerOrderRepository.sumActiveCommission());
  }

  @Transactional(readOnly = true)
  public void authorize(String administratorEmail) {
    administrator(administratorEmail);
  }

  @Transactional(readOnly = true)
  public PageResponse<UserSummaryResponse> users(
      String administratorEmail, String query, Boolean enabled, int page, int size) {
    administrator(administratorEmail);
    validatePage(page, size);
    return PageResponse.from(
        userRepository.searchForAdmin(
            normalize(query),
            enabled,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))),
        UserSummaryResponse::from);
  }

  @Transactional(readOnly = true)
  public UserDetailsResponse user(String administratorEmail, Long userId) {
    administrator(administratorEmail);
    User user = findUser(userId);
    return new UserDetailsResponse(
        UserSummaryResponse.from(user),
        bookRepository.countBySellerId(userId),
        orderRepository.countByBuyerId(userId),
        sellerOrderRepository.countBySellerId(userId));
  }

  public UserDetailsResponse suspendUser(String administratorEmail, Long userId, String reason) {
    User administrator = administrator(administratorEmail);
    User target = mutableRegularUser(userId, administrator);
    Instant now = Instant.now();
    target.suspend(administrator, reason.trim(), now);
    int revokedSessions = revokeSessions(target);
    List<Book> hiddenBooks =
        bookRepository.findAllBySellerIdAndModerationStatus(
            target.getId(), BookModerationStatus.VISIBLE);
    hiddenBooks.forEach(
        book ->
            book.hide(
                BookModerationReason.ACCOUNT_SUSPENDED,
                "Anunț ascuns automat cât timp contul este suspendat.",
                administrator,
                now));
    auditService.record(
        administrator,
        "USER_SUSPENDED",
        "USER",
        target.getId(),
        reason,
        "{\"revokedSessions\":"
            + revokedSessions
            + ",\"hiddenListings\":"
            + hiddenBooks.size()
            + "}");
    return user(administratorEmail, userId);
  }

  public UserDetailsResponse reactivateUser(String administratorEmail, Long userId, String reason) {
    User administrator = administrator(administratorEmail);
    User target = mutableRegularUser(userId, administrator);
    Instant now = Instant.now();
    target.reactivate(now);
    List<Book> restoredBooks =
        bookRepository.findAllBySellerIdAndModerationStatusAndModerationReason(
            target.getId(), BookModerationStatus.HIDDEN, BookModerationReason.ACCOUNT_SUSPENDED);
    restoredBooks.forEach(book -> book.restore(administrator, now));
    auditService.record(
        administrator,
        "USER_REACTIVATED",
        "USER",
        target.getId(),
        reason,
        "{\"restoredListings\":" + restoredBooks.size() + "}");
    return user(administratorEmail, userId);
  }

  public void revokeUserSessions(String administratorEmail, Long userId, String reason) {
    User administrator = administrator(administratorEmail);
    User target = findUser(userId);
    int revoked = revokeSessions(target);
    auditService.record(
        administrator,
        "USER_SESSIONS_REVOKED",
        "USER",
        target.getId(),
        reason,
        "{\"revokedSessions\":" + revoked + "}");
  }

  public void resendVerification(String administratorEmail, Long userId) {
    User administrator = administrator(administratorEmail);
    User target = findUser(userId);
    if (target.isEmailVerified()) {
      throw new ConflictException("Email is already verified");
    }
    accountSecurityService.sendVerification(target);
    auditService.record(
        administrator, "VERIFICATION_EMAIL_RESENT", "USER", target.getId(), null, null);
  }

  @Transactional(readOnly = true)
  public PageResponse<BookAdminResponse> books(
      String administratorEmail,
      String query,
      BookStatus status,
      BookModerationStatus moderationStatus,
      int page,
      int size) {
    administrator(administratorEmail);
    validatePage(page, size);
    return PageResponse.from(
        bookRepository.searchForAdmin(
            normalize(query),
            status,
            moderationStatus,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))),
        BookAdminResponse::from);
  }

  @Transactional(readOnly = true)
  public BookAdminResponse book(String administratorEmail, Long bookId) {
    administrator(administratorEmail);
    return BookAdminResponse.from(findBook(bookId));
  }

  public BookAdminResponse hideBook(
      String administratorEmail, Long bookId, BookModerationRequest request) {
    User administrator = administrator(administratorEmail);
    Book book = lockBook(bookId);
    book.hide(request.reason(), request.note().trim(), administrator, Instant.now());
    auditService.record(
        administrator,
        "BOOK_HIDDEN",
        "BOOK",
        book.getId(),
        request.note(),
        "{\"moderationReason\":\"" + request.reason() + "\"}");
    return BookAdminResponse.from(book);
  }

  public BookAdminResponse restoreBook(String administratorEmail, Long bookId, String reason) {
    User administrator = administrator(administratorEmail);
    Book book = lockBook(bookId);
    book.restore(administrator, Instant.now());
    auditService.record(administrator, "BOOK_RESTORED", "BOOK", book.getId(), reason, null);
    return BookAdminResponse.from(book);
  }

  @Transactional(readOnly = true)
  public List<CategoryAdminResponse> categories(String administratorEmail) {
    administrator(administratorEmail);
    return categoryRepository.findAll(Sort.by("name")).stream()
        .map(CategoryAdminResponse::from)
        .toList();
  }

  public CategoryAdminResponse createCategory(
      String administratorEmail, CreateCategoryRequest request) {
    User administrator = administrator(administratorEmail);
    String name = request.name().trim();
    String slug = request.slug().trim();
    if (categoryRepository.existsByNameIgnoreCase(name)
        || categoryRepository.findBySlug(slug).isPresent()) {
      throw new ConflictException("A category with this name or slug already exists");
    }
    Instant now = Instant.now();
    Category category =
        categoryRepository.save(
            Category.builder()
                .name(name)
                .slug(slug)
                .description(trimToNull(request.description()))
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build());
    auditService.record(
        administrator, "CATEGORY_CREATED", "CATEGORY", category.getId(), null, null);
    return CategoryAdminResponse.from(category);
  }

  public CategoryAdminResponse updateCategory(
      String administratorEmail, Long categoryId, CategoryUpdateRequest request) {
    User administrator = administrator(administratorEmail);
    Category category = findCategory(categoryId);
    if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), categoryId)) {
      throw new ConflictException("A category with this name already exists");
    }
    category.update(request.name().trim(), trimToNull(request.description()), Instant.now());
    auditService.record(
        administrator, "CATEGORY_UPDATED", "CATEGORY", category.getId(), null, null);
    return CategoryAdminResponse.from(category);
  }

  public CategoryAdminResponse setCategoryActive(
      String administratorEmail, Long categoryId, boolean active, String reason) {
    User administrator = administrator(administratorEmail);
    Category category = findCategory(categoryId);
    category.setActive(active, Instant.now());
    auditService.record(
        administrator,
        active ? "CATEGORY_ACTIVATED" : "CATEGORY_DEACTIVATED",
        "CATEGORY",
        category.getId(),
        reason,
        null);
    return CategoryAdminResponse.from(category);
  }

  @Transactional(readOnly = true)
  public PageResponse<OrderSummaryResponse> orders(
      String administratorEmail, String query, OrderStatus status, int page, int size) {
    administrator(administratorEmail);
    validatePage(page, size);
    return PageResponse.from(
        orderRepository.searchForAdmin(
            normalize(query),
            status,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "placedAt"))),
        OrderSummaryResponse::from);
  }

  @Transactional(readOnly = true)
  public OrderDetailsResponse order(String administratorEmail, Long orderId) {
    administrator(administratorEmail);
    com.avbooknest.order.model.Order order =
        orderRepository
            .findDetailedById(orderId)
            .orElseThrow(() -> new NotFoundException("Order not found"));
    List<SellerOrderResponse> sellerOrders =
        sellerOrderRepository.findAllByOrderId(orderId).stream()
            .map(SellerOrderResponse::from)
            .toList();
    PaymentResponse payment =
        paymentRepository.findByOrderId(orderId).map(PaymentResponse::from).orElse(null);
    return new OrderDetailsResponse(
        OrderResponse.from(order, payment, sellerOrders),
        order.getBuyer().getId(),
        order.getBuyer().getEmail(),
        order.getRecipientPhone());
  }

  @Transactional(readOnly = true)
  public PageResponse<IssueAdminResponse> issues(
      String administratorEmail, OrderIssueStatus status, int page, int size) {
    administrator(administratorEmail);
    validatePage(page, size);
    return PageResponse.from(
        sellerOrderRepository.searchIssuesForAdmin(
            status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "issueOpenedAt"))),
        IssueAdminResponse::from);
  }

  public IssueAdminResponse resolveIssue(
      String administratorEmail, Long sellerOrderId, IssueResolutionRequest request) {
    User administrator = administrator(administratorEmail);
    SellerOrder sellerOrder =
        sellerOrderRepository
            .findByIdForUpdate(sellerOrderId)
            .orElseThrow(() -> new NotFoundException("Sale not found"));
    if (!sellerOrder.hasOpenIssue()) {
      throw new ConflictException("This sale has no open issue");
    }
    Instant now = Instant.now();
    if (request.resolution() == IssueResolution.REFUND_BUYER) {
      SellerTransfer transfer =
          sellerTransferRepository
              .findBySellerOrderId(sellerOrderId)
              .orElseThrow(() -> new ConflictException("Seller transfer record is missing"));
      if (transfer.getProviderTransferId() != null) {
        throw new ConflictException(
            "The seller payout was already created; automatic refund is unsafe");
      }
      integrationEventRepository.save(
          IntegrationEvent.pending(
              "SELLER_ORDER",
              sellerOrderId,
              "STRIPE_REFUND_SELLER_ORDER",
              "{\"sellerOrderId\":" + sellerOrderId + "}",
              now));
    } else {
      integrationEventRepository
          .findFirstByAggregateTypeAndAggregateIdAndEventTypeAndStatusOrderByCreatedAtDesc(
              "SELLER_TRANSFER",
              sellerOrderId,
              "STRIPE_CREATE_TRANSFER",
              IntegrationEventStatus.PENDING)
          .ifPresent(event -> event.deferUntil(now));
    }
    sellerOrder.resolveIssue(request.resolution(), request.note().trim(), administrator, now);
    String orderNumber = sellerOrder.getOrder().getOrderNumber();
    notificationService.create(
        sellerOrder.getOrder().getBuyer(),
        NotificationType.ORDER_ISSUE_RESOLVED,
        "Problemă soluționată",
        request.resolution() == IssueResolution.REFUND_BUYER
            ? "Rambursarea pentru comanda " + orderNumber + " a fost inițiată."
            : "Sesizarea pentru comanda "
                + orderNumber
                + " a fost soluționată în favoarea vânzătorului.");
    notificationService.create(
        sellerOrder.getSeller(),
        NotificationType.ORDER_ISSUE_RESOLVED,
        "Problemă soluționată",
        request.resolution() == IssueResolution.REFUND_BUYER
            ? "Sesizarea pentru comanda "
                + orderNumber
                + " a fost soluționată prin rambursarea cumpărătorului."
            : "Plata pentru comanda " + orderNumber + " a fost deblocată.");
    auditService.record(
        administrator,
        "ORDER_ISSUE_RESOLVED",
        "SELLER_ORDER",
        sellerOrderId,
        request.note(),
        "{\"resolution\":\"" + request.resolution() + "\"}");
    return IssueAdminResponse.from(sellerOrder);
  }

  @Transactional(readOnly = true)
  public PageResponse<IntegrationEventResponse> failedIntegrations(
      String administratorEmail, int page, int size) {
    administrator(administratorEmail);
    validatePage(page, size);
    return PageResponse.from(
        integrationEventRepository.findAllByStatus(
            IntegrationEventStatus.FAILED,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))),
        IntegrationEventResponse::from);
  }

  public IntegrationEventResponse retryIntegration(
      String administratorEmail, Long eventId, String reason) {
    User administrator = administrator(administratorEmail);
    IntegrationEvent event =
        integrationEventRepository
            .findByIdAndStatus(eventId, IntegrationEventStatus.FAILED)
            .orElseThrow(
                () -> new ConflictException("Only failed integration events can be retried"));
    event.retry(Instant.now());
    auditService.record(
        administrator, "INTEGRATION_RETRIED", "INTEGRATION_EVENT", eventId, reason, null);
    return IntegrationEventResponse.from(event);
  }

  @Transactional(readOnly = true)
  public PageResponse<TransferOperationResponse> failedTransfers(
      String administratorEmail, int page, int size) {
    administrator(administratorEmail);
    validatePage(page, size);
    return PageResponse.from(
        sellerTransferRepository.findAllByStatus(
            SellerTransferStatus.FAILED,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))),
        TransferOperationResponse::from);
  }

  @Transactional(readOnly = true)
  public PageResponse<ShipmentOperationResponse> shipmentExceptions(
      String administratorEmail, int page, int size) {
    administrator(administratorEmail);
    validatePage(page, size);
    return PageResponse.from(
        shipmentRepository.findAllByStatusIn(
            SHIPMENT_EXCEPTIONS,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"))),
        ShipmentOperationResponse::from);
  }

  private User administrator(String email) {
    User administrator =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException("Administrator not found"));
    if (!"ADMIN".equals(administrator.getRole().getName()) || !administrator.isEnabled()) {
      throw new ForbiddenException("Administrator access is required");
    }
    return administrator;
  }

  private User mutableRegularUser(Long userId, User administrator) {
    User target = findUser(userId);
    if (target.getId().equals(administrator.getId())
        || "ADMIN".equals(target.getRole().getName())) {
      throw new ForbiddenException("Administrator accounts cannot be changed here");
    }
    return target;
  }

  private User findUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new NotFoundException("User not found"));
  }

  private Book findBook(Long bookId) {
    return bookRepository
        .findById(bookId)
        .orElseThrow(() -> new NotFoundException("Book not found"));
  }

  private Book lockBook(Long bookId) {
    return bookRepository
        .findByIdForUpdate(bookId)
        .orElseThrow(() -> new NotFoundException("Book not found"));
  }

  private Category findCategory(Long categoryId) {
    return categoryRepository
        .findById(categoryId)
        .orElseThrow(() -> new NotFoundException("Category not found"));
  }

  private int revokeSessions(User user) {
    List<RefreshToken> tokens = refreshTokenRepository.findAllByUserIdAndRevokedFalse(user.getId());
    tokens.forEach(RefreshToken::revoke);
    return tokens.size();
  }

  private void validatePage(int page, int size) {
    if (page < 0) throw new BadRequestException("Page must be zero or greater");
    if (size < 1 || size > 100) {
      throw new BadRequestException("Page size must be between 1 and 100");
    }
  }

  private String normalize(String value) {
    return value == null || value.isBlank() ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
