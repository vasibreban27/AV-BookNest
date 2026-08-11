package com.avbooknest.payment.service;

import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.integration.model.IntegrationEvent;
import com.avbooknest.integration.repository.IntegrationEventRepository;
import com.avbooknest.notification.model.NotificationType;
import com.avbooknest.notification.service.NotificationService;
import com.avbooknest.order.model.Order;
import com.avbooknest.order.model.OrderItem;
import com.avbooknest.order.model.OrderStatus;
import com.avbooknest.order.model.Payment;
import com.avbooknest.order.model.PaymentStatus;
import com.avbooknest.order.repository.PaymentRepository;
import com.avbooknest.order.service.SellerOrderService;
import com.avbooknest.payment.model.SellerTransfer;
import com.avbooknest.payment.model.StripeWebhookEventRecord;
import com.avbooknest.payment.repository.SellerTransferRepository;
import com.avbooknest.payment.repository.StripeWebhookEventRepository;
import com.avbooknest.payment.stripe.StripeGateway;
import com.avbooknest.payment.stripe.StripeWebhookEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StripePaymentService {
  private final StripeGateway stripeGateway;
  private final PaymentRepository paymentRepository;
  private final UserRepository userRepository;
  private final SellerTransferRepository sellerTransferRepository;
  private final StripeWebhookEventRepository webhookEventRepository;
  private final IntegrationEventRepository integrationEventRepository;
  private final SellerOrderService sellerOrderService;
  private final NotificationService notificationService;

  public StripePaymentService(
      StripeGateway stripeGateway,
      PaymentRepository paymentRepository,
      UserRepository userRepository,
      SellerTransferRepository sellerTransferRepository,
      StripeWebhookEventRepository webhookEventRepository,
      IntegrationEventRepository integrationEventRepository,
      SellerOrderService sellerOrderService,
      NotificationService notificationService) {
    this.stripeGateway = stripeGateway;
    this.paymentRepository = paymentRepository;
    this.userRepository = userRepository;
    this.sellerTransferRepository = sellerTransferRepository;
    this.webhookEventRepository = webhookEventRepository;
    this.integrationEventRepository = integrationEventRepository;
    this.sellerOrderService = sellerOrderService;
    this.notificationService = notificationService;
  }

  public void processWebhook(String payload, String signature) {
    StripeWebhookEvent event = stripeGateway.parseWebhook(payload, signature);
    if (webhookEventRepository.existsByStripeEventId(event.eventId())) {
      return;
    }
    switch (event.type()) {
      case "payment_intent.succeeded" -> paymentSucceeded(event);
      case "payment_intent.payment_failed" -> paymentFailed(event);
      case "payment_intent.canceled" -> paymentCancelled(event);
      case "account.updated" -> accountUpdated(event);
      case "transfer.reversed" -> transferReversed(event);
      default -> {
        // Persist ignored events too, so Stripe retries remain idempotent.
      }
    }
    webhookEventRepository.save(
        new StripeWebhookEventRecord(event.eventId(), event.type(), Instant.now()));
  }

  @Transactional(readOnly = true)
  public List<Long> expiredPaymentIds(Instant now) {
    return paymentRepository
        .findAllByStatusInAndExpiresAtLessThanEqual(
            List.of(PaymentStatus.PENDING, PaymentStatus.FAILED), now)
        .stream()
        .map(Payment::getId)
        .toList();
  }

  public void expirePayment(Long paymentId) {
    Payment payment = paymentRepository.findByIdForUpdate(paymentId).orElse(null);
    Instant now = Instant.now();
    if (payment == null || !payment.canExpire(now)) {
      return;
    }
    String paymentIntentId = payment.getProviderPaymentId();
    if (paymentIntentId == null || paymentIntentId.isBlank()) {
      // Payments created before Stripe integration have no remote object to cancel.
      payment.stopExpirationTracking();
      return;
    }
    stripeGateway.cancelPaymentIntent(paymentIntentId);
    payment.cancel();
    sellerOrderService.cancelUnpaidOrder(payment.getOrder());
    notificationService.create(
        payment.getOrder().getBuyer(),
        NotificationType.ORDER_CANCELLED,
        "Payment expired",
        "The reserved books were released because Stripe payment for "
            + payment.getOrder().getOrderNumber()
            + " was not completed.",
        "/orders/" + payment.getOrder().getId());
  }

  private void paymentSucceeded(StripeWebhookEvent event) {
    Payment payment = paymentRepository.findByProviderPaymentId(event.objectId()).orElse(null);
    if (payment == null
        || payment.getStatus() == PaymentStatus.SUCCEEDED
        || payment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED
        || payment.getStatus() == PaymentStatus.REFUNDED) {
      return;
    }
    Instant now = Instant.now();
    payment.succeed(event.relatedId(), now);
    Order order = payment.getOrder();
    if (order.getStatus() == OrderStatus.CANCELLED) {
      integrationEventRepository.save(
          IntegrationEvent.pending(
              "ORDER",
              order.getId(),
              "STRIPE_REFUND_ORDER",
              "{\"orderId\":" + order.getId() + "}",
              now));
      return;
    }
    sellerOrderService.activateAfterPayment(order, now);
    notificationService.create(
        order.getBuyer(),
        NotificationType.PAYMENT_SUCCEEDED,
        "Payment confirmed",
        "Stripe confirmed payment for order " + order.getOrderNumber() + ".",
        "/orders/" + order.getId());
    for (OrderItem item : order.getItems()) {
      notificationService.create(
          item.getSeller(),
          NotificationType.BOOK_RESERVED,
          "Book sold",
          "Stripe confirmed payment for '"
              + item.getTitle()
              + "'. You have 24 hours to accept the sale.",
          "/sales");
    }
  }

  private void paymentFailed(StripeWebhookEvent event) {
    Payment payment = paymentRepository.findByProviderPaymentId(event.objectId()).orElse(null);
    if (payment != null
        && payment.getStatus() != PaymentStatus.SUCCEEDED
        && payment.getStatus() != PaymentStatus.REFUNDED) {
      payment.fail(event.message() == null ? "Stripe payment failed" : event.message());
    }
  }

  private void paymentCancelled(StripeWebhookEvent event) {
    Payment payment = paymentRepository.findByProviderPaymentId(event.objectId()).orElse(null);
    if (payment != null
        && payment.getStatus() != PaymentStatus.SUCCEEDED
        && payment.getStatus() != PaymentStatus.REFUNDED) {
      payment.cancel();
      sellerOrderService.cancelUnpaidOrder(payment.getOrder());
    }
  }

  private void accountUpdated(StripeWebhookEvent event) {
    User user = userRepository.findByStripeAccountId(event.objectId()).orElse(null);
    if (user != null) {
      user.updateStripeStatus(
          Boolean.TRUE.equals(event.detailsSubmitted()),
          Boolean.TRUE.equals(event.chargesEnabled()),
          Boolean.TRUE.equals(event.payoutsEnabled()));
    }
  }

  private void transferReversed(StripeWebhookEvent event) {
    SellerTransfer transfer =
        sellerTransferRepository.findByProviderTransferId(event.objectId()).orElse(null);
    if (transfer != null) {
      transfer.markReversed(Instant.now());
    }
  }
}
