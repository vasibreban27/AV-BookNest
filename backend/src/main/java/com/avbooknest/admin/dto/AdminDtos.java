package com.avbooknest.admin.dto;

import com.avbooknest.admin.model.AdminAuditLog;
import com.avbooknest.auth.model.User;
import com.avbooknest.book.dto.BookResponse;
import com.avbooknest.book.model.Book;
import com.avbooknest.book.model.BookModerationReason;
import com.avbooknest.book.model.BookModerationStatus;
import com.avbooknest.book.model.Category;
import com.avbooknest.integration.model.IntegrationEvent;
import com.avbooknest.integration.model.IntegrationEventStatus;
import com.avbooknest.order.dto.OrderResponse;
import com.avbooknest.order.model.IssueResolution;
import com.avbooknest.order.model.Order;
import com.avbooknest.order.model.OrderIssueStatus;
import com.avbooknest.order.model.OrderStatus;
import com.avbooknest.order.model.SellerOrder;
import com.avbooknest.order.model.SellerOrderStatus;
import com.avbooknest.payment.model.SellerTransfer;
import com.avbooknest.payment.model.SellerTransferStatus;
import com.avbooknest.shipment.dto.ShipmentResponse;
import com.avbooknest.shipment.model.Shipment;
import java.math.BigDecimal;
import java.time.Instant;

public final class AdminDtos {
  private AdminDtos() {}

  public record DashboardResponse(
      long newUsersLast7Days,
      long activeListings,
      long paidOrders,
      long openIssues,
      long shipmentExceptions,
      long failedTransfers,
      long failedIntegrations,
      BigDecimal netGmv,
      BigDecimal activeCommission) {}

  public record UserSummaryResponse(
      Long id,
      String firstName,
      String lastName,
      String email,
      String phoneNumber,
      String role,
      boolean enabled,
      boolean emailVerified,
      boolean stripePayoutsEnabled,
      Instant suspendedAt,
      String suspensionReason,
      Instant createdAt) {
    public static UserSummaryResponse from(User user) {
      return new UserSummaryResponse(
          user.getId(),
          user.getFirstName(),
          user.getLastName(),
          user.getEmail(),
          user.getPhoneNumber(),
          user.getRole().getName(),
          user.isEnabled(),
          user.isEmailVerified(),
          user.isStripePayoutsEnabled(),
          user.getSuspendedAt(),
          user.getSuspensionReason(),
          user.getCreatedAt());
    }
  }

  public record UserDetailsResponse(
      UserSummaryResponse user, long listingCount, long buyerOrderCount, long sellerOrderCount) {}

  public record BookAdminResponse(
      BookResponse book,
      BookModerationStatus moderationStatus,
      BookModerationReason moderationReason,
      String moderationNote,
      Long moderatedById,
      Instant moderatedAt) {
    public static BookAdminResponse from(Book book) {
      return new BookAdminResponse(
          BookResponse.from(book),
          book.getModerationStatus(),
          book.getModerationReason(),
          book.getModerationNote(),
          book.getModeratedBy() == null ? null : book.getModeratedBy().getId(),
          book.getModeratedAt());
    }
  }

  public record CategoryAdminResponse(
      Long id, String name, String slug, String description, boolean active, Instant createdAt) {
    public static CategoryAdminResponse from(Category category) {
      return new CategoryAdminResponse(
          category.getId(),
          category.getName(),
          category.getSlug(),
          category.getDescription(),
          category.isActive(),
          category.getCreatedAt());
    }
  }

  public record OrderSummaryResponse(
      Long id,
      String orderNumber,
      Long buyerId,
      String buyerEmail,
      OrderStatus status,
      BigDecimal totalAmount,
      String currency,
      Instant placedAt) {
    public static OrderSummaryResponse from(Order order) {
      return new OrderSummaryResponse(
          order.getId(),
          order.getOrderNumber(),
          order.getBuyer().getId(),
          order.getBuyer().getEmail(),
          order.getStatus(),
          order.getTotalAmount(),
          order.getCurrency(),
          order.getPlacedAt());
    }
  }

  public record OrderDetailsResponse(
      OrderResponse order, Long buyerId, String buyerEmail, String recipientPhone) {}

  public record IssueAdminResponse(
      Long sellerOrderId,
      Long orderId,
      String orderNumber,
      Long buyerId,
      String buyerEmail,
      Long sellerId,
      String sellerEmail,
      SellerOrderStatus sellerOrderStatus,
      BigDecimal itemSubtotal,
      BigDecimal sellerProceeds,
      OrderIssueStatus issueStatus,
      String issueReason,
      Instant issueOpenedAt,
      Instant issueResolvedAt,
      IssueResolution resolution,
      String resolutionNote,
      Long resolvedById) {
    public static IssueAdminResponse from(SellerOrder sellerOrder) {
      return new IssueAdminResponse(
          sellerOrder.getId(),
          sellerOrder.getOrder().getId(),
          sellerOrder.getOrder().getOrderNumber(),
          sellerOrder.getOrder().getBuyer().getId(),
          sellerOrder.getOrder().getBuyer().getEmail(),
          sellerOrder.getSeller().getId(),
          sellerOrder.getSeller().getEmail(),
          sellerOrder.getStatus(),
          sellerOrder.getItemSubtotal(),
          sellerOrder.getSellerProceeds(),
          sellerOrder.getIssueStatus(),
          sellerOrder.getIssueReason(),
          sellerOrder.getIssueOpenedAt(),
          sellerOrder.getIssueResolvedAt(),
          sellerOrder.getIssueResolution(),
          sellerOrder.getIssueResolutionNote(),
          sellerOrder.getIssueResolvedBy() == null
              ? null
              : sellerOrder.getIssueResolvedBy().getId());
    }
  }

  public record IntegrationEventResponse(
      Long id,
      String aggregateType,
      Long aggregateId,
      String eventType,
      IntegrationEventStatus status,
      int attempts,
      Instant nextAttemptAt,
      String lastError,
      Instant createdAt) {
    public static IntegrationEventResponse from(IntegrationEvent event) {
      return new IntegrationEventResponse(
          event.getId(),
          event.getAggregateType(),
          event.getAggregateId(),
          event.getEventType(),
          event.getStatus(),
          event.getAttempts(),
          event.getNextAttemptAt(),
          event.getLastError(),
          event.getCreatedAt());
    }
  }

  public record TransferOperationResponse(
      Long id,
      Long sellerOrderId,
      BigDecimal amount,
      String currency,
      SellerTransferStatus status,
      String providerTransferId,
      String failureReason,
      Instant updatedAt) {
    public static TransferOperationResponse from(SellerTransfer transfer) {
      return new TransferOperationResponse(
          transfer.getId(),
          transfer.getSellerOrder().getId(),
          transfer.getAmount(),
          transfer.getCurrency(),
          transfer.getStatus(),
          transfer.getProviderTransferId(),
          transfer.getFailureReason(),
          transfer.getUpdatedAt());
    }
  }

  public record ShipmentOperationResponse(
      Long id, Long sellerOrderId, ShipmentResponse shipment, Instant updatedAt) {
    public static ShipmentOperationResponse from(Shipment shipment) {
      return new ShipmentOperationResponse(
          shipment.getId(),
          shipment.getSellerOrder().getId(),
          ShipmentResponse.from(shipment),
          shipment.getUpdatedAt());
    }
  }

  public record AuditResponse(
      Long id,
      Long administratorId,
      String administratorEmail,
      String action,
      String targetType,
      Long targetId,
      String reason,
      String details,
      Instant createdAt) {
    public static AuditResponse from(AdminAuditLog log) {
      return new AuditResponse(
          log.getId(),
          log.getAdministrator().getId(),
          log.getAdministrator().getEmail(),
          log.getAction(),
          log.getTargetType(),
          log.getTargetId(),
          log.getReason(),
          log.getDetails(),
          log.getCreatedAt());
    }
  }
}
