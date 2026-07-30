package com.avbooknest.integration.service;

import com.avbooknest.auth.model.User;
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
import com.avbooknest.payment.model.SellerTransferStatus;
import com.avbooknest.payment.repository.SellerTransferRepository;
import com.avbooknest.payment.stripe.StripeAccountStatus;
import com.avbooknest.payment.stripe.StripeGateway;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StripeTransferEventHandler {
  static final String EVENT_TYPE = "STRIPE_CREATE_TRANSFER";
  private static final Logger LOGGER = LoggerFactory.getLogger(StripeTransferEventHandler.class);

  private final IntegrationEventRepository eventRepository;
  private final SellerOrderRepository sellerOrderRepository;
  private final SellerTransferRepository transferRepository;
  private final PaymentRepository paymentRepository;
  private final StripeGateway stripeGateway;
  private final NotificationService notificationService;

  public StripeTransferEventHandler(
      IntegrationEventRepository eventRepository,
      SellerOrderRepository sellerOrderRepository,
      SellerTransferRepository transferRepository,
      PaymentRepository paymentRepository,
      StripeGateway stripeGateway,
      NotificationService notificationService) {
    this.eventRepository = eventRepository;
    this.sellerOrderRepository = sellerOrderRepository;
    this.transferRepository = transferRepository;
    this.paymentRepository = paymentRepository;
    this.stripeGateway = stripeGateway;
    this.notificationService = notificationService;
  }

  @Transactional
  public boolean processNext() {
    IntegrationEvent event =
        eventRepository
            .findFirstByEventTypeAndStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                EVENT_TYPE, IntegrationEventStatus.PENDING, Instant.now())
            .orElse(null);
    if (event == null) {
      return false;
    }
    event.startProcessing();
    try {
      SellerOrder sellerOrder =
          sellerOrderRepository
              .findDetailedById(event.getAggregateId())
              .orElseThrow(() -> new IllegalStateException("Seller order not found"));
      SellerTransfer transfer =
          transferRepository
              .findBySellerOrderId(sellerOrder.getId())
              .orElseThrow(() -> new IllegalStateException("Seller transfer not found"));
      if (transfer.getStatus() == SellerTransferStatus.CREATED
          || transfer.getStatus() == SellerTransferStatus.PAID) {
        event.markProcessed(Instant.now());
        return true;
      }
      User seller = sellerOrder.getSeller();
      if (seller.getStripeAccountId() == null) {
        throw new IllegalStateException("Seller has no Stripe sandbox account");
      }
      StripeAccountStatus account = stripeGateway.accountStatus(seller.getStripeAccountId());
      seller.updateStripeStatus(
          account.detailsSubmitted(), account.chargesEnabled(), account.payoutsEnabled());
      if (!account.payoutsEnabled()) {
        throw new IllegalStateException("Seller Stripe sandbox payouts are not enabled");
      }
      Payment payment =
          paymentRepository
              .findByOrderId(sellerOrder.getOrder().getId())
              .orElseThrow(() -> new IllegalStateException("Payment not found"));
      if (payment.getStatus() != PaymentStatus.SUCCEEDED
          && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
        throw new IllegalStateException("Stripe payment is not transferable");
      }
      if (payment.getProviderChargeId() == null) {
        throw new IllegalStateException("Stripe charge is missing");
      }
      transfer.markReady(Instant.now());
      String transferId =
          stripeGateway.createTransfer(
              transfer.getAmount(),
              transfer.getCurrency(),
              seller.getStripeAccountId(),
              sellerOrder.getOrder().getOrderNumber(),
              payment.getProviderChargeId(),
              "booknest-transfer-event-" + event.getId());
      transfer.markCreated(transferId, Instant.now());
      notificationService.create(
          seller,
          NotificationType.SELLER_TRANSFER_CREATED,
          "Stripe transfer released",
          transfer.getAmount()
              + " "
              + transfer.getCurrency()
              + " was released to your Stripe sandbox balance.");
      event.markProcessed(Instant.now());
    } catch (RuntimeException exception) {
      LOGGER.warn("Stripe transfer event {} failed", event.getId(), exception);
      event.scheduleRetry(rootMessage(exception), Instant.now());
    }
    return true;
  }

  private String rootMessage(Throwable throwable) {
    Throwable current = throwable;
    while (current.getCause() != null) {
      current = current.getCause();
    }
    return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
  }
}
