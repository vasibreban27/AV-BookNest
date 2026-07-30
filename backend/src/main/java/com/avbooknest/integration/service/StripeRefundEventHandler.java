package com.avbooknest.integration.service;

import com.avbooknest.integration.model.IntegrationEvent;
import com.avbooknest.integration.model.IntegrationEventStatus;
import com.avbooknest.integration.repository.IntegrationEventRepository;
import com.avbooknest.notification.model.NotificationType;
import com.avbooknest.notification.service.NotificationService;
import com.avbooknest.order.model.Payment;
import com.avbooknest.order.model.PaymentStatus;
import com.avbooknest.order.model.SellerOrder;
import com.avbooknest.order.repository.PaymentRepository;
import com.avbooknest.order.repository.SellerOrderRepository;
import com.avbooknest.payment.model.SellerTransfer;
import com.avbooknest.payment.repository.SellerTransferRepository;
import com.avbooknest.payment.stripe.StripeGateway;
import java.math.BigDecimal;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StripeRefundEventHandler {
  static final String ORDER_EVENT = "STRIPE_REFUND_ORDER";
  static final String SELLER_ORDER_EVENT = "STRIPE_REFUND_SELLER_ORDER";
  private static final Logger LOGGER = LoggerFactory.getLogger(StripeRefundEventHandler.class);

  private final IntegrationEventRepository eventRepository;
  private final PaymentRepository paymentRepository;
  private final SellerOrderRepository sellerOrderRepository;
  private final SellerTransferRepository sellerTransferRepository;
  private final StripeGateway stripeGateway;
  private final NotificationService notificationService;

  public StripeRefundEventHandler(
      IntegrationEventRepository eventRepository,
      PaymentRepository paymentRepository,
      SellerOrderRepository sellerOrderRepository,
      SellerTransferRepository sellerTransferRepository,
      StripeGateway stripeGateway,
      NotificationService notificationService) {
    this.eventRepository = eventRepository;
    this.paymentRepository = paymentRepository;
    this.sellerOrderRepository = sellerOrderRepository;
    this.sellerTransferRepository = sellerTransferRepository;
    this.stripeGateway = stripeGateway;
    this.notificationService = notificationService;
  }

  @Transactional
  public boolean processNext(String eventType) {
    IntegrationEvent event =
        eventRepository
            .findFirstByEventTypeAndStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eventType, IntegrationEventStatus.PENDING, Instant.now())
            .orElse(null);
    if (event == null) {
      return false;
    }
    event.startProcessing();
    try {
      RefundContext context =
          ORDER_EVENT.equals(eventType)
              ? orderRefund(event.getAggregateId())
              : sellerOrderRefund(event.getAggregateId());
      Payment payment = context.payment();
      if (payment.getStatus() == PaymentStatus.PENDING
          || payment.getStatus() == PaymentStatus.FAILED) {
        stripeGateway.cancelPaymentIntent(payment.getProviderPaymentId());
        payment.cancel();
      } else if (context.amount().signum() > 0) {
        stripeGateway.refund(
            payment.getProviderPaymentId(),
            context.amount(),
            payment.getCurrency(),
            "booknest-refund-event-" + event.getId());
        payment.refund(context.amount(), Instant.now());
        notificationService.create(
            payment.getOrder().getBuyer(),
            NotificationType.PAYMENT_REFUNDED,
            "Stripe refund created",
            context.message());
      }
      event.markProcessed(Instant.now());
    } catch (RuntimeException exception) {
      LOGGER.warn("Stripe refund event {} failed", event.getId(), exception);
      event.scheduleRetry(rootMessage(exception), Instant.now());
    }
    return true;
  }

  private RefundContext orderRefund(Long orderId) {
    Payment payment =
        paymentRepository
            .findByOrderId(orderId)
            .orElseThrow(() -> new IllegalStateException("Payment not found"));
    BigDecimal remaining = payment.getAmount().subtract(payment.getRefundedAmount());
    return new RefundContext(
        payment,
        remaining.max(BigDecimal.ZERO),
        "The full payment for " + payment.getOrder().getOrderNumber() + " was refunded.");
  }

  private RefundContext sellerOrderRefund(Long sellerOrderId) {
    SellerOrder sellerOrder =
        sellerOrderRepository
            .findDetailedById(sellerOrderId)
            .orElseThrow(() -> new IllegalStateException("Seller order not found"));
    Payment payment =
        paymentRepository
            .findByOrderId(sellerOrder.getOrder().getId())
            .orElseThrow(() -> new IllegalStateException("Payment not found"));
    SellerTransfer transfer =
        sellerTransferRepository
            .findBySellerOrderId(sellerOrderId)
            .orElseThrow(() -> new IllegalStateException("Seller transfer not found"));
    if (transfer.getProviderTransferId() != null) {
      throw new IllegalStateException("A released Stripe transfer must be reversed before refund");
    }
    transfer.markReversed(Instant.now());
    BigDecimal requested = sellerOrder.getItemSubtotal().add(sellerOrder.getShippingCost());
    BigDecimal remaining = payment.getAmount().subtract(payment.getRefundedAmount());
    BigDecimal amount = requested.min(remaining).max(BigDecimal.ZERO);
    return new RefundContext(
        payment,
        amount,
        "The cancelled part of " + payment.getOrder().getOrderNumber() + " was refunded.");
  }

  private String rootMessage(Throwable throwable) {
    Throwable current = throwable;
    while (current.getCause() != null) {
      current = current.getCause();
    }
    return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
  }

  private record RefundContext(Payment payment, BigDecimal amount, String message) {}
}
