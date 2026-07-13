package com.avbooknest.order.service;

import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.book.model.Book;
import com.avbooknest.book.model.BookStatus;
import com.avbooknest.book.repository.BookRepository;
import com.avbooknest.cart.model.Cart;
import com.avbooknest.cart.model.CartItem;
import com.avbooknest.cart.repository.CartRepository;
import com.avbooknest.common.exception.ConflictException;
import com.avbooknest.common.exception.NotFoundException;
import com.avbooknest.notification.model.NotificationType;
import com.avbooknest.notification.service.NotificationService;
import com.avbooknest.order.dto.CheckoutRequest;
import com.avbooknest.order.dto.OrderResponse;
import com.avbooknest.order.dto.PaymentResponse;
import com.avbooknest.order.model.Order;
import com.avbooknest.order.model.OrderItem;
import com.avbooknest.order.model.OrderStatus;
import com.avbooknest.order.model.Payment;
import com.avbooknest.order.model.PaymentProvider;
import com.avbooknest.order.model.PaymentStatus;
import com.avbooknest.order.repository.OrderRepository;
import com.avbooknest.order.repository.PaymentRepository;
import com.avbooknest.shipment.dto.ShipmentResponse;
import com.avbooknest.shipment.service.ShipmentService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService {
  private static final String CURRENCY = "RON";
  private final OrderRepository orderRepository;
  private final PaymentRepository paymentRepository;
  private final CartRepository cartRepository;
  private final BookRepository bookRepository;
  private final UserRepository userRepository;
  private final ShipmentService shipmentService;
  private final NotificationService notificationService;

  public OrderService(
      OrderRepository orderRepository,
      PaymentRepository paymentRepository,
      CartRepository cartRepository,
      BookRepository bookRepository,
      UserRepository userRepository,
      ShipmentService shipmentService,
      NotificationService notificationService) {
    this.orderRepository = orderRepository;
    this.paymentRepository = paymentRepository;
    this.cartRepository = cartRepository;
    this.bookRepository = bookRepository;
    this.userRepository = userRepository;
    this.shipmentService = shipmentService;
    this.notificationService = notificationService;
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> list(String email) {
    return orderRepository.findAllByBuyerIdOrderByPlacedAtDesc(user(email).getId()).stream()
        .map(this::response)
        .toList();
  }

  @Transactional(readOnly = true)
  public OrderResponse get(Long orderId, String email) {
    return response(
        orderRepository
            .findByIdAndBuyerId(orderId, user(email).getId())
            .orElseThrow(() -> new NotFoundException("Order not found")));
  }

  public OrderResponse checkout(CheckoutRequest request, String email) {
    User buyer = user(email);
    Cart cart =
        cartRepository
            .findByUserId(buyer.getId())
            .orElseThrow(() -> new ConflictException("Your cart is empty"));
    if (cart.getItems().isEmpty()) throw new ConflictException("Your cart is empty");
    BigDecimal subtotal = BigDecimal.ZERO;
    for (CartItem cartItem : cart.getItems())
      subtotal = subtotal.add(cartItem.getBook().getPrice());
    Instant now = Instant.now();
    Order order =
        Order.builder()
            .orderNumber("ORD-" + UUID.randomUUID())
            .buyer(buyer)
            .status(OrderStatus.PENDING)
            .subtotal(subtotal)
            .shippingCost(BigDecimal.ZERO)
            .totalAmount(subtotal)
            .currency(CURRENCY)
            .placedAt(now)
            .updatedAt(now)
            .build();
    for (CartItem cartItem : cart.getItems()) {
      Book book =
          bookRepository
              .findByIdForUpdate(cartItem.getBook().getId())
              .orElseThrow(() -> new NotFoundException("Book not found"));
      if (book.getStatus() != BookStatus.AVAILABLE)
        throw new ConflictException("Book '" + book.getTitle() + "' is no longer available");
      book.reserve();
      OrderItem item =
          OrderItem.builder()
              .order(order)
              .book(book)
              .seller(book.getSeller())
              .title(book.getTitle())
              .author(book.getAuthor())
              .isbn(book.getIsbn())
              .unitPrice(book.getPrice())
              .quantity(1)
              .createdAt(now)
              .build();
      order.addItem(item);
    }
    Order savedOrder = orderRepository.save(order);
    Payment payment =
        paymentRepository.save(
            Payment.builder()
                .order(savedOrder)
                .provider(PaymentProvider.CASH_ON_DELIVERY)
                .amount(savedOrder.getTotalAmount())
                .currency(CURRENCY)
                .status(PaymentStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build());
    cart.clearItems();
    List<ShipmentResponse> shipments =
        shipmentService.createForOrder(
            savedOrder, request.easyboxId().trim(), request.easyboxName().trim());
    notificationService.create(
        buyer,
        NotificationType.ORDER_PLACED,
        "Order placed",
        "Your order " + savedOrder.getOrderNumber() + " has been placed.");
    for (OrderItem item : savedOrder.getItems())
      notificationService.create(
          item.getSeller(),
          NotificationType.BOOK_RESERVED,
          "Book reserved",
          "Your book '"
              + item.getTitle()
              + "' was reserved by order "
              + savedOrder.getOrderNumber()
              + ".");
    return OrderResponse.from(savedOrder, PaymentResponse.from(payment), shipments);
  }

  private OrderResponse response(Order order) {
    return OrderResponse.from(
        order,
        paymentRepository.findByOrderId(order.getId()).map(PaymentResponse::from).orElse(null),
        shipmentService.listForOrder(order.getId()));
  }

  private User user(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new NotFoundException("User not found"));
  }
}
