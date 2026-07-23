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
import com.avbooknest.order.model.SellerOrder;
import com.avbooknest.order.model.SellerOrderStatus;
import com.avbooknest.order.repository.OrderRepository;
import com.avbooknest.order.repository.PaymentRepository;
import com.avbooknest.payment.model.SellerTransfer;
import com.avbooknest.payment.repository.SellerTransferRepository;
import com.avbooknest.shipment.model.Shipment;
import com.avbooknest.shipment.model.ShipmentStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
  private final SellerOrderService sellerOrderService;
  private final SellerTransferRepository sellerTransferRepository;
  private final NotificationService notificationService;

  public OrderService(
      OrderRepository orderRepository,
      PaymentRepository paymentRepository,
      CartRepository cartRepository,
      BookRepository bookRepository,
      UserRepository userRepository,
      SellerOrderService sellerOrderService,
      SellerTransferRepository sellerTransferRepository,
      NotificationService notificationService) {
    this.orderRepository = orderRepository;
    this.paymentRepository = paymentRepository;
    this.cartRepository = cartRepository;
    this.bookRepository = bookRepository;
    this.userRepository = userRepository;
    this.sellerOrderService = sellerOrderService;
    this.sellerTransferRepository = sellerTransferRepository;
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
            .recipientName(request.recipientName().trim())
            .recipientEmail(request.recipientEmail().trim().toLowerCase())
            .recipientPhone(request.recipientPhone().replace(" ", ""))
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
    createSellerOrders(order, request, now);
    Order savedOrder = orderRepository.save(order);
    Payment payment =
        paymentRepository.save(
            Payment.builder()
                .order(savedOrder)
                .provider(PaymentProvider.STRIPE)
                .amount(savedOrder.getTotalAmount())
                .currency(CURRENCY)
                .status(PaymentStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build());
    cart.clearItems();
    savedOrder
        .getSellerOrders()
        .forEach(
            sellerOrder ->
                sellerTransferRepository.save(
                    SellerTransfer.blocked(
                        sellerOrder, sellerOrder.getSellerProceeds(), CURRENCY, now)));
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
    return OrderResponse.from(
        savedOrder,
        PaymentResponse.from(payment),
        savedOrder.getSellerOrders().stream()
            .map(com.avbooknest.order.dto.SellerOrderResponse::from)
            .toList());
  }

  public OrderResponse cancel(Long orderId, String email) {
    User buyer = user(email);
    Order order =
        orderRepository
            .findByIdAndBuyerIdForUpdate(orderId, buyer.getId())
            .orElseThrow(() -> new NotFoundException("Order not found"));
    if (order.getStatus() != OrderStatus.CANCELLED) {
      sellerOrderService.cancelOrder(order);
    }
    return response(order);
  }

  private OrderResponse response(Order order) {
    return OrderResponse.from(
        order,
        paymentRepository.findByOrderId(order.getId()).map(PaymentResponse::from).orElse(null),
        sellerOrderService.listForOrder(order.getId()));
  }

  private void createSellerOrders(Order order, CheckoutRequest request, Instant now) {
    Map<User, List<OrderItem>> itemsBySeller =
        order.getItems().stream().collect(Collectors.groupingBy(OrderItem::getSeller));
    itemsBySeller.forEach(
        (seller, items) -> {
          BigDecimal itemSubtotal =
              items.stream()
                  .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                  .reduce(BigDecimal.ZERO, BigDecimal::add);
          BigDecimal commissionAmount =
              itemSubtotal
                  .multiply(SellerOrder.COMMISSION_RATE)
                  .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
          SellerOrder sellerOrder =
              SellerOrder.builder()
                  .order(order)
                  .seller(seller)
                  .status(SellerOrderStatus.AWAITING_SELLER)
                  .itemSubtotal(itemSubtotal)
                  .commissionRate(SellerOrder.COMMISSION_RATE)
                  .commissionAmount(commissionAmount)
                  .sellerProceeds(itemSubtotal.subtract(commissionAmount))
                  .shippingCost(BigDecimal.ZERO)
                  .acceptBy(now.plus(SellerOrder.ACCEPTANCE_WINDOW))
                  .createdAt(now)
                  .updatedAt(now)
                  .build();
          Shipment shipment =
              Shipment.builder()
                  .sellerOrder(sellerOrder)
                  .easyboxId(request.easyboxId().trim())
                  .easyboxName(request.easyboxName().trim())
                  .easyboxAddress(request.easyboxAddress().trim())
                  .easyboxCity(request.easyboxCity().trim())
                  .easyboxCounty(request.easyboxCounty().trim())
                  .easyboxPostalCode(
                      request.easyboxPostalCode() == null
                          ? null
                          : request.easyboxPostalCode().trim())
                  .status(ShipmentStatus.NOT_CREATED)
                  .statusUpdatedAt(now)
                  .createdAt(now)
                  .updatedAt(now)
                  .build();
          sellerOrder.assignShipment(shipment);
          items.forEach(sellerOrder::addItem);
          order.addSellerOrder(sellerOrder);
        });
  }

  private User user(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new NotFoundException("User not found"));
  }
}
