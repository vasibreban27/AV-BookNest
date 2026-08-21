package com.avbooknest.admin.controller;

import com.avbooknest.admin.dto.AdminDtos.AuditResponse;
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
import com.avbooknest.admin.dto.AdminRequests.ReasonRequest;
import com.avbooknest.admin.service.AdminAuditService;
import com.avbooknest.admin.service.AdminService;
import com.avbooknest.book.dto.CreateCategoryRequest;
import com.avbooknest.book.model.BookModerationStatus;
import com.avbooknest.book.model.BookStatus;
import com.avbooknest.common.dto.PageResponse;
import com.avbooknest.order.model.OrderIssueStatus;
import com.avbooknest.order.model.OrderStatus;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final AdminService adminService;
  private final AdminAuditService auditService;

  public AdminController(AdminService adminService, AdminAuditService auditService) {
    this.adminService = adminService;
    this.auditService = auditService;
  }

  @GetMapping("/dashboard")
  public DashboardResponse dashboard(Authentication authentication) {
    return adminService.dashboard(authentication.getName());
  }

  @GetMapping("/users")
  public PageResponse<UserSummaryResponse> users(
      @RequestParam(defaultValue = "") String query,
      @RequestParam(required = false) Boolean enabled,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size,
      Authentication authentication) {
    return adminService.users(authentication.getName(), query, enabled, page, size);
  }

  @GetMapping("/users/{userId}")
  public UserDetailsResponse user(@PathVariable Long userId, Authentication authentication) {
    return adminService.user(authentication.getName(), userId);
  }

  @PostMapping("/users/{userId}/suspend")
  public UserDetailsResponse suspendUser(
      @PathVariable Long userId,
      @Valid @RequestBody ReasonRequest request,
      Authentication authentication) {
    return adminService.suspendUser(authentication.getName(), userId, request.reason());
  }

  @PostMapping("/users/{userId}/reactivate")
  public UserDetailsResponse reactivateUser(
      @PathVariable Long userId,
      @Valid @RequestBody ReasonRequest request,
      Authentication authentication) {
    return adminService.reactivateUser(authentication.getName(), userId, request.reason());
  }

  @PostMapping("/users/{userId}/revoke-sessions")
  public ResponseEntity<Void> revokeSessions(
      @PathVariable Long userId,
      @Valid @RequestBody ReasonRequest request,
      Authentication authentication) {
    adminService.revokeUserSessions(authentication.getName(), userId, request.reason());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/users/{userId}/resend-verification")
  public ResponseEntity<Void> resendVerification(
      @PathVariable Long userId, Authentication authentication) {
    adminService.resendVerification(authentication.getName(), userId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/books")
  public PageResponse<BookAdminResponse> books(
      @RequestParam(defaultValue = "") String query,
      @RequestParam(required = false) BookStatus status,
      @RequestParam(required = false) BookModerationStatus moderationStatus,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size,
      Authentication authentication) {
    return adminService.books(
        authentication.getName(), query, status, moderationStatus, page, size);
  }

  @GetMapping("/books/{bookId}")
  public BookAdminResponse book(@PathVariable Long bookId, Authentication authentication) {
    return adminService.book(authentication.getName(), bookId);
  }

  @PostMapping("/books/{bookId}/hide")
  public BookAdminResponse hideBook(
      @PathVariable Long bookId,
      @Valid @RequestBody BookModerationRequest request,
      Authentication authentication) {
    return adminService.hideBook(authentication.getName(), bookId, request);
  }

  @PostMapping("/books/{bookId}/restore")
  public BookAdminResponse restoreBook(
      @PathVariable Long bookId,
      @Valid @RequestBody ReasonRequest request,
      Authentication authentication) {
    return adminService.restoreBook(authentication.getName(), bookId, request.reason());
  }

  @GetMapping("/categories")
  public List<CategoryAdminResponse> categories(Authentication authentication) {
    return adminService.categories(authentication.getName());
  }

  @PostMapping("/categories")
  public ResponseEntity<CategoryAdminResponse> createCategory(
      @Valid @RequestBody CreateCategoryRequest request, Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(adminService.createCategory(authentication.getName(), request));
  }

  @PutMapping("/categories/{categoryId}")
  public CategoryAdminResponse updateCategory(
      @PathVariable Long categoryId,
      @Valid @RequestBody CategoryUpdateRequest request,
      Authentication authentication) {
    return adminService.updateCategory(authentication.getName(), categoryId, request);
  }

  @PatchMapping("/categories/{categoryId}/activate")
  public CategoryAdminResponse activateCategory(
      @PathVariable Long categoryId,
      @Valid @RequestBody ReasonRequest request,
      Authentication authentication) {
    return adminService.setCategoryActive(
        authentication.getName(), categoryId, true, request.reason());
  }

  @PatchMapping("/categories/{categoryId}/deactivate")
  public CategoryAdminResponse deactivateCategory(
      @PathVariable Long categoryId,
      @Valid @RequestBody ReasonRequest request,
      Authentication authentication) {
    return adminService.setCategoryActive(
        authentication.getName(), categoryId, false, request.reason());
  }

  @GetMapping("/orders")
  public PageResponse<OrderSummaryResponse> orders(
      @RequestParam(defaultValue = "") String query,
      @RequestParam(required = false) OrderStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size,
      Authentication authentication) {
    return adminService.orders(authentication.getName(), query, status, page, size);
  }

  @GetMapping("/orders/{orderId}")
  public OrderDetailsResponse order(@PathVariable Long orderId, Authentication authentication) {
    return adminService.order(authentication.getName(), orderId);
  }

  @GetMapping("/issues")
  public PageResponse<IssueAdminResponse> issues(
      @RequestParam(defaultValue = "OPEN") OrderIssueStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size,
      Authentication authentication) {
    return adminService.issues(authentication.getName(), status, page, size);
  }

  @PostMapping("/issues/{sellerOrderId}/resolve")
  public IssueAdminResponse resolveIssue(
      @PathVariable Long sellerOrderId,
      @Valid @RequestBody IssueResolutionRequest request,
      Authentication authentication) {
    return adminService.resolveIssue(authentication.getName(), sellerOrderId, request);
  }

  @GetMapping("/operations/integrations/failed")
  public PageResponse<IntegrationEventResponse> failedIntegrations(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size,
      Authentication authentication) {
    return adminService.failedIntegrations(authentication.getName(), page, size);
  }

  @PostMapping("/operations/integrations/{eventId}/retry")
  public IntegrationEventResponse retryIntegration(
      @PathVariable Long eventId,
      @Valid @RequestBody ReasonRequest request,
      Authentication authentication) {
    return adminService.retryIntegration(authentication.getName(), eventId, request.reason());
  }

  @GetMapping("/operations/transfers/failed")
  public PageResponse<TransferOperationResponse> failedTransfers(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size,
      Authentication authentication) {
    return adminService.failedTransfers(authentication.getName(), page, size);
  }

  @GetMapping("/operations/shipments/exceptions")
  public PageResponse<ShipmentOperationResponse> shipmentExceptions(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size,
      Authentication authentication) {
    return adminService.shipmentExceptions(authentication.getName(), page, size);
  }

  @GetMapping("/audit-logs")
  public PageResponse<AuditResponse> auditLogs(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size,
      Authentication authentication) {
    adminService.authorize(authentication.getName());
    return auditService.list(page, size);
  }
}
