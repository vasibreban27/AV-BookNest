package com.avbooknest.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.avbooknest.auth.model.Role;
import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.book.model.Book;
import com.avbooknest.book.model.BookCondition;
import com.avbooknest.book.model.BookStatus;
import com.avbooknest.book.model.Category;
import com.avbooknest.book.repository.BookRepository;
import com.avbooknest.cart.model.Cart;
import com.avbooknest.cart.model.CartItem;
import com.avbooknest.cart.repository.CartRepository;
import com.avbooknest.notification.service.NotificationService;
import com.avbooknest.order.dto.CheckoutRequest;
import com.avbooknest.order.dto.OrderResponse;
import com.avbooknest.order.model.Order;
import com.avbooknest.order.model.OrderStatus;
import com.avbooknest.order.model.Payment;
import com.avbooknest.order.model.PaymentProvider;
import com.avbooknest.order.repository.OrderRepository;
import com.avbooknest.order.repository.PaymentRepository;
import com.avbooknest.payment.model.SellerTransfer;
import com.avbooknest.payment.repository.SellerTransferRepository;
import com.avbooknest.shipment.model.PackageSize;
import com.avbooknest.shipping.dto.SellerShippingQuoteResponse;
import com.avbooknest.shipping.dto.ShippingQuoteResponse;
import com.avbooknest.shipping.service.ShippingQuoteService;
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
class OrderServiceTest {
  @Mock private OrderRepository orderRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private CartRepository cartRepository;
  @Mock private BookRepository bookRepository;
  @Mock private UserRepository userRepository;
  @Mock private SellerOrderService sellerOrderService;
  @Mock private SellerTransferRepository sellerTransferRepository;
  @Mock private NotificationService notificationService;
  @Mock private ShippingQuoteService shippingQuoteService;
  private OrderService orderService;

  @BeforeEach
  void setUp() {
    orderService =
        new OrderService(
            orderRepository,
            paymentRepository,
            cartRepository,
            bookRepository,
            userRepository,
            sellerOrderService,
            sellerTransferRepository,
            notificationService,
            shippingQuoteService);
  }

  @Test
  void checkoutCreatesStripeMarketplaceSplitWithFivePercentCommission() {
    User buyer = user(1L, "buyer@example.com");
    User seller = user(2L, "seller@example.com");
    Book book = book(seller);
    Cart cart =
        Cart.builder().user(buyer).createdAt(Instant.now()).updatedAt(Instant.now()).build();
    cart.addItem(CartItem.builder().cart(cart).book(book).addedAt(Instant.now()).build());
    when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(buyer));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
    when(shippingQuoteService.quote(any(Cart.class), any()))
        .thenReturn(
            new ShippingQuoteResponse(
                new BigDecimal("17.99"),
                "RON",
                List.of(
                    new SellerShippingQuoteResponse(
                        2L, new BigDecimal("17.99"), PackageSize.S, 650, 230, 160, 50))));
    when(bookRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(book));
    when(orderRepository.save(any(Order.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(paymentRepository.save(any(Payment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(sellerTransferRepository.save(any(SellerTransfer.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    OrderResponse response =
        orderService.checkout(
            new CheckoutRequest(
                "locker-1",
                "Easybox Central",
                "Strada Test 1",
                "Cluj-Napoca",
                "Cluj",
                "400000",
                "Ana Pop",
                "buyer@example.com",
                "+40700111222"),
            "buyer@example.com");

    assertEquals(PaymentProvider.STRIPE, response.payment().provider());
    assertEquals(new BigDecimal("1.50"), response.sellerOrders().getFirst().commissionAmount());
    assertEquals(new BigDecimal("28.50"), response.sellerOrders().getFirst().sellerProceeds());
    assertEquals(new BigDecimal("47.99"), response.totalAmount());
    assertEquals(BookStatus.RESERVED, book.getStatus());
    assertEquals(0, cart.getItems().size());
    verify(sellerTransferRepository).save(any(SellerTransfer.class));
  }

  @Test
  void buyerCancellationDelegatesToSellerOrderWorkflow() {
    User buyer = user(1L, "buyer@example.com");
    Instant now = Instant.now();
    Order order =
        Order.builder()
            .id(77L)
            .orderNumber("ORD-77")
            .buyer(buyer)
            .status(OrderStatus.PENDING)
            .subtotal(new BigDecimal("30.00"))
            .shippingCost(BigDecimal.ZERO)
            .totalAmount(new BigDecimal("30.00"))
            .currency("RON")
            .recipientName("Ana Pop")
            .recipientEmail("buyer@example.com")
            .recipientPhone("+40700111222")
            .placedAt(now)
            .updatedAt(now)
            .build();
    when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(buyer));
    when(orderRepository.findByIdAndBuyerIdForUpdate(77L, 1L)).thenReturn(Optional.of(order));
    when(paymentRepository.findByOrderId(77L)).thenReturn(Optional.empty());
    when(sellerOrderService.listForOrder(77L)).thenReturn(java.util.List.of());

    orderService.cancel(77L, "buyer@example.com");

    verify(sellerOrderService).cancelOrder(order);
  }

  private Book book(User seller) {
    Instant now = Instant.now();
    Category category =
        Category.builder()
            .id(5L)
            .name("Programming")
            .slug("programming")
            .createdAt(now)
            .updatedAt(now)
            .build();
    return Book.builder()
        .id(9L)
        .title("Domain-Driven Design")
        .author("Eric Evans")
        .price(new BigDecimal("30.00"))
        .bookCondition(BookCondition.GOOD)
        .language("English")
        .seller(seller)
        .category(category)
        .status(BookStatus.AVAILABLE)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }

  private User user(Long id, String email) {
    Instant now = Instant.now();
    return User.builder()
        .id(id)
        .firstName("Test")
        .lastName("User")
        .email(email)
        .passwordHash("password")
        .role(Role.builder().id(1L).name("USER").build())
        .enabled(true)
        .emailVerified(true)
        .createdAt(now)
        .updatedAt(now)
        .build();
  }
}
