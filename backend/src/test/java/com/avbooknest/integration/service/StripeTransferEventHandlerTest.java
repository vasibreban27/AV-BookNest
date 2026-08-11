package com.avbooknest.integration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.avbooknest.integration.model.IntegrationEvent;
import com.avbooknest.integration.model.IntegrationEventStatus;
import com.avbooknest.integration.repository.IntegrationEventRepository;
import com.avbooknest.notification.service.NotificationService;
import com.avbooknest.order.model.SellerOrder;
import com.avbooknest.order.repository.PaymentRepository;
import com.avbooknest.order.repository.SellerOrderRepository;
import com.avbooknest.payment.model.SellerTransfer;
import com.avbooknest.payment.repository.SellerTransferRepository;
import com.avbooknest.payment.stripe.StripeGateway;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StripeTransferEventHandlerTest {
  @Mock private IntegrationEventRepository eventRepository;
  @Mock private SellerOrderRepository sellerOrderRepository;
  @Mock private SellerTransferRepository transferRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private StripeGateway stripeGateway;
  @Mock private NotificationService notificationService;
  @Mock private SellerOrder sellerOrder;
  @Mock private SellerTransfer transfer;

  @Test
  void openBuyerIssueDefersTransferWithoutConsumingARetry() {
    Instant before = Instant.now();
    IntegrationEvent event =
        IntegrationEvent.pending(
            "SELLER_TRANSFER", 20L, "STRIPE_CREATE_TRANSFER", "{\"sellerOrderId\":20}", before);
    when(eventRepository
            .findFirstByEventTypeAndStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                any(), any(), any()))
        .thenReturn(Optional.of(event));
    when(sellerOrderRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(sellerOrder));
    when(transferRepository.findBySellerOrderId(20L)).thenReturn(Optional.of(transfer));
    when(sellerOrder.getId()).thenReturn(20L);
    when(sellerOrder.hasOpenIssue()).thenReturn(true);
    StripeTransferEventHandler handler =
        new StripeTransferEventHandler(
            eventRepository,
            sellerOrderRepository,
            transferRepository,
            paymentRepository,
            stripeGateway,
            notificationService);

    assertTrue(handler.processNext());

    assertEquals(IntegrationEventStatus.PENDING, event.getStatus());
    assertEquals(0, event.getAttempts());
    assertTrue(event.getNextAttemptAt().isAfter(before));
    verifyNoInteractions(paymentRepository, stripeGateway, notificationService);
  }
}
