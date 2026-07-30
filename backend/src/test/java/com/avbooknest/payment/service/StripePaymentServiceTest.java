package com.avbooknest.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.avbooknest.auth.model.Role;
import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.integration.repository.IntegrationEventRepository;
import com.avbooknest.notification.service.NotificationService;
import com.avbooknest.order.model.Order;
import com.avbooknest.order.model.OrderStatus;
import com.avbooknest.order.model.Payment;
import com.avbooknest.order.model.PaymentProvider;
import com.avbooknest.order.model.PaymentStatus;
import com.avbooknest.order.repository.PaymentRepository;
import com.avbooknest.order.service.SellerOrderService;
import com.avbooknest.payment.model.StripeWebhookEventRecord;
import com.avbooknest.payment.repository.SellerTransferRepository;
import com.avbooknest.payment.repository.StripeWebhookEventRepository;
import com.avbooknest.payment.stripe.StripeGateway;
import com.avbooknest.payment.stripe.StripeWebhookEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StripePaymentServiceTest {
  @Mock private StripeGateway stripeGateway;
  @Mock private PaymentRepository paymentRepository;
  @Mock private UserRepository userRepository;
  @Mock private SellerTransferRepository sellerTransferRepository;
  @Mock private StripeWebhookEventRepository webhookEventRepository;
  @Mock private IntegrationEventRepository integrationEventRepository;
  @Mock private SellerOrderService sellerOrderService;
  @Mock private NotificationService notificationService;
  private StripePaymentService service;

  @BeforeEach
  void setUp() {
    service =
        new StripePaymentService(
            stripeGateway,
            paymentRepository,
            userRepository,
            sellerTransferRepository,
            webhookEventRepository,
            integrationEventRepository,
            sellerOrderService,
            notificationService);
  }

  @Test
  void succeededWebhookActivatesSellerAcceptanceWindow() {
    Instant now = Instant.now();
    User buyer = user(now);
    Order order =
        Order.builder()
            .id(10L)
            .orderNumber("ORD-10")
            .buyer(buyer)
            .status(OrderStatus.PENDING)
            .subtotal(new BigDecimal("30.00"))
            .shippingCost(new BigDecimal("13.99"))
            .totalAmount(new BigDecimal("43.99"))
            .currency("RON")
            .recipientName("Test Buyer")
            .recipientEmail("buyer@example.com")
            .recipientPhone("+40700111222")
            .placedAt(now)
            .updatedAt(now)
            .build();
    Payment payment =
        Payment.builder()
            .id(20L)
            .order(order)
            .provider(PaymentProvider.STRIPE)
            .amount(new BigDecimal("43.99"))
            .currency("RON")
            .status(PaymentStatus.PENDING)
            .createdAt(now)
            .updatedAt(now)
            .build();
    payment.bindPaymentIntent("pi_test_10", now.plusSeconds(1800));
    StripeWebhookEvent webhook =
        new StripeWebhookEvent(
            "evt_test_10",
            "payment_intent.succeeded",
            "pi_test_10",
            "ch_test_10",
            null,
            null,
            null,
            null);
    when(stripeGateway.parseWebhook("{}", "signature")).thenReturn(webhook);
    when(webhookEventRepository.existsByStripeEventId("evt_test_10")).thenReturn(false);
    when(paymentRepository.findByProviderPaymentId("pi_test_10")).thenReturn(Optional.of(payment));

    service.processWebhook("{}", "signature");

    assertEquals(PaymentStatus.SUCCEEDED, payment.getStatus());
    assertEquals("ch_test_10", payment.getProviderChargeId());
    verify(sellerOrderService).activateAfterPayment(any(Order.class), any(Instant.class));
    verify(webhookEventRepository).save(any(StripeWebhookEventRecord.class));
  }

  @Test
  void expiredPaymentIsCancelledBeforeBooksAreReleased() {
    Instant now = Instant.now();
    Order order =
        Order.builder()
            .id(11L)
            .orderNumber("ORD-11")
            .buyer(user(now))
            .status(OrderStatus.PENDING)
            .subtotal(new BigDecimal("20.00"))
            .shippingCost(BigDecimal.ZERO)
            .totalAmount(new BigDecimal("20.00"))
            .currency("RON")
            .recipientName("Test Buyer")
            .recipientEmail("buyer@example.com")
            .recipientPhone("+40700111222")
            .placedAt(now.minusSeconds(3600))
            .updatedAt(now.minusSeconds(3600))
            .build();
    Payment payment =
        Payment.builder()
            .id(21L)
            .order(order)
            .provider(PaymentProvider.STRIPE)
            .amount(new BigDecimal("20.00"))
            .currency("RON")
            .status(PaymentStatus.PENDING)
            .createdAt(now.minusSeconds(3600))
            .updatedAt(now.minusSeconds(3600))
            .build();
    payment.bindPaymentIntent("pi_test_11", now.minusSeconds(60));
    when(paymentRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(payment));

    service.expirePayment(21L);

    verify(stripeGateway).cancelPaymentIntent("pi_test_11");
    verify(sellerOrderService).cancelUnpaidOrder(order);
    assertEquals(PaymentStatus.CANCELLED, payment.getStatus());
  }

  @Test
  void legacyExpiredPaymentWithoutStripeIntentIsIgnored() {
    Instant now = Instant.now();
    Order order =
        Order.builder()
            .id(12L)
            .orderNumber("ORD-12")
            .buyer(user(now))
            .status(OrderStatus.PENDING)
            .subtotal(new BigDecimal("20.00"))
            .shippingCost(BigDecimal.ZERO)
            .totalAmount(new BigDecimal("20.00"))
            .currency("RON")
            .recipientName("Legacy Buyer")
            .recipientEmail("legacy@example.com")
            .recipientPhone("+40700111222")
            .placedAt(now.minusSeconds(3600))
            .updatedAt(now.minusSeconds(3600))
            .build();
    Payment payment =
        Payment.builder()
            .id(22L)
            .order(order)
            .provider(PaymentProvider.STRIPE)
            .amount(new BigDecimal("20.00"))
            .currency("RON")
            .status(PaymentStatus.PENDING)
            .expiresAt(now.minusSeconds(60))
            .createdAt(now.minusSeconds(3600))
            .updatedAt(now.minusSeconds(3600))
            .build();
    when(paymentRepository.findByIdForUpdate(22L)).thenReturn(Optional.of(payment));

    service.expirePayment(22L);

    verifyNoInteractions(stripeGateway, sellerOrderService);
    assertEquals(PaymentStatus.PENDING, payment.getStatus());
    assertNull(payment.getExpiresAt());
  }

  private User user(Instant now) {
    return User.builder()
        .id(1L)
        .firstName("Test")
        .lastName("Buyer")
        .email("buyer@example.com")
        .passwordHash("password")
        .role(Role.builder().id(1L).name("USER").build())
        .enabled(true)
        .emailVerified(true)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }
}
