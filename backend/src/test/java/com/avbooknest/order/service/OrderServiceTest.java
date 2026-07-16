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
import com.avbooknest.order.model.Payment;
import com.avbooknest.order.repository.OrderRepository;
import com.avbooknest.order.repository.PaymentRepository;
import com.avbooknest.shipment.service.ShipmentService;
import java.math.BigDecimal;
import java.time.Instant;
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
  @Mock private ShipmentService shipmentService;
  @Mock private NotificationService notificationService;
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
            shipmentService,
            notificationService);
  }

  @Test
  void checkoutCreatesSnapshotPaymentNotificationsAndReservesBook() {
    User buyer = user(1L, "buyer@example.com");
    User seller = user(2L, "seller@example.com");
    Book book = book(seller);
    Cart cart = cart(buyer);
    cart.addItem(CartItem.builder().cart(cart).book(book).addedAt(Instant.now()).build());
    when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(buyer));
    when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
    when(bookRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(book));
    when(orderRepository.save(any(com.avbooknest.order.model.Order.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(paymentRepository.save(any(Payment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(shipmentService.createForOrder(any(), any(), any())).thenReturn(java.util.List.of());

    OrderResponse response =
        orderService.checkout(
            new CheckoutRequest("easybox-123", "Easybox Universitate"), "buyer@example.com");

    assertEquals(1, response.items().size());
    assertEquals(new BigDecimal("30.00"), response.totalAmount());
    assertEquals(BookStatus.RESERVED, book.getStatus());
    assertEquals(0, cart.getItems().size());
    assertEquals(
        com.avbooknest.order.model.PaymentProvider.CASH_ON_DELIVERY, response.payment().provider());
    verify(notificationService, org.mockito.Mockito.times(2)).create(any(), any(), any(), any());
  }

  private Cart cart(User buyer) {
    Instant now = Instant.now();
    return Cart.builder().user(buyer).createdAt(now).updatedAt(now).build();
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
