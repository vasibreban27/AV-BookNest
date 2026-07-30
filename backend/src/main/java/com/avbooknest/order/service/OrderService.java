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
import com.avbooknest.notification.service.NotificationService;
import com.avbooknest.order.dto.CheckoutRequest;
import com.avbooknest.order.dto.OrderResponse;
import com.avbooknest.order.dto.PaymentResponse;
import com.avbooknest.order.dto.StripeCheckoutResponse;
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
import com.avbooknest.payment.stripe.StripeGateway;
import com.avbooknest.payment.stripe.StripePaymentIntentResult;
import com.avbooknest.payment.stripe.StripeProperties;
import com.avbooknest.shipment.model.Shipment;
import com.avbooknest.shipment.model.ShipmentStatus;
import com.avbooknest.shipping.dto.SellerShippingQuoteResponse;
import com.avbooknest.shipping.dto.ShippingQuoteRequest;
import com.avbooknest.shipping.dto.ShippingQuoteResponse;
import com.avbooknest.shipping.service.ShippingQuoteService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
  private final ShippingQuoteService shippingQuoteService;
  private final StripeGateway stripeGateway;
  private final StripeProperties stripeProperties;

  public OrderService(
      OrderRepository orderRepository,
      PaymentRepository paymentRepository,
      CartRepository cartRepository,
      BookRepository bookRepository,
      UserRepository userRepository,
      SellerOrderService sellerOrderService,
      SellerTransferRepository sellerTransferRepository,
      NotificationService notificationService,
      ShippingQuoteService shippingQuoteService,
      StripeGateway stripeGateway,
      StripeProperties stripeProperties) {
    this.orderRepository = orderRepository;
    this.paymentRepository = paymentRepository;
    this.cartRepository = cartRepository;
    this.bookRepository = bookRepository;
    this.userRepository = userRepository;
    this.sellerOrderService = sellerOrderService;
    this.sellerTransferRepository = sellerTransferRepository;
    this.notificationService = notificationService;
    this.shippingQuoteService = shippingQuoteService;
    this.stripeGateway = stripeGateway;
    this.stripeProperties = stripeProperties;
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

  public StripeCheckoutResponse checkout(CheckoutRequest request, String email) {
    if (!stripeProperties.sandboxConfigured()) {
      throw new com.avbooknest.common.exception.ExternalServiceException(
          "Stripe sandbox is not configured");
    }
    User buyer = user(email);
    Cart cart =
        cartRepository
            .findByUserId(buyer.getId())
            .orElseThrow(() -> new ConflictException("Your cart is empty"));
    if (cart.getItems().isEmpty()) throw new ConflictException("Your cart is empty");
    boolean sellerWithoutStripe =
        cart.getItems().stream()
            .map(item -> item.getBook().getSeller())
            .anyMatch(
                seller -> seller.getStripeAccountId() == null || !seller.isStripePayoutsEnabled());
    if (sellerWithoutStripe) {
      throw new ConflictException(
          "Every seller must complete Stripe sandbox onboarding before checkout");
    }
    ShippingQuoteResponse shippingQuote =
        shippingQuoteService.quote(cart, shippingQuoteRequest(request));
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
            .shippingCost(shippingQuote.shippingCost())
            .totalAmount(subtotal.add(shippingQuote.shippingCost()))
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
    createSellerOrders(order, request, shippingQuote, now);
    Order savedOrder = orderRepository.save(order);
    Payment payment =
        paymentRepository.save(
            Payment.builder()
                .order(savedOrder)
                .provider(PaymentProvider.STRIPE)
                .amount(savedOrder.getTotalAmount())
                .currency(CURRENCY)
                .status(PaymentStatus.PENDING)
                .refundedAmount(BigDecimal.ZERO)
                .createdAt(now)
                .updatedAt(now)
                .build());
    Instant expiresAt =
        now.plus(Math.max(stripeProperties.paymentExpirationMinutes(), 5), ChronoUnit.MINUTES);
    StripePaymentIntentResult paymentIntent =
        stripeGateway.createPaymentIntent(
            savedOrder.getId(),
            savedOrder.getOrderNumber(),
            savedOrder.getTotalAmount(),
            CURRENCY,
            savedOrder.getRecipientEmail());
    payment.bindPaymentIntent(paymentIntent.paymentIntentId(), expiresAt);
    cart.clearItems();
    savedOrder
        .getSellerOrders()
        .forEach(
            sellerOrder ->
                sellerTransferRepository.save(
                    SellerTransfer.blocked(
                        sellerOrder, sellerOrder.getSellerProceeds(), CURRENCY, now)));
    OrderResponse orderResponse =
        OrderResponse.from(
            savedOrder,
            PaymentResponse.from(payment),
            savedOrder.getSellerOrders().stream()
                .map(com.avbooknest.order.dto.SellerOrderResponse::from)
                .toList());
    return new StripeCheckoutResponse(
        orderResponse,
        paymentIntent.clientSecret(),
        stripeProperties.publishableKey(),
        stripeProperties.paymentReturnUrl(),
        expiresAt);
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

  private void createSellerOrders(
      Order order, CheckoutRequest request, ShippingQuoteResponse shippingQuote, Instant now) {
    Map<Long, SellerShippingQuoteResponse> quotesBySeller =
        shippingQuote.packages().stream()
            .collect(Collectors.toMap(SellerShippingQuoteResponse::sellerId, quote -> quote));
    Map<User, List<OrderItem>> itemsBySeller =
        order.getItems().stream().collect(Collectors.groupingBy(OrderItem::getSeller));
    itemsBySeller.forEach(
        (seller, items) -> {
          SellerShippingQuoteResponse sellerQuote = quotesBySeller.get(seller.getId());
          if (sellerQuote == null) {
            throw new ConflictException("Missing Sameday quote for seller " + seller.getId());
          }
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
                  .status(SellerOrderStatus.PAYMENT_PENDING)
                  .itemSubtotal(itemSubtotal)
                  .commissionRate(SellerOrder.COMMISSION_RATE)
                  .commissionAmount(commissionAmount)
                  .sellerProceeds(itemSubtotal.subtract(commissionAmount))
                  .shippingCost(sellerQuote.cost())
                  .acceptBy(null)
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
                  .packageSize(sellerQuote.packageSize())
                  .packageWeightGrams(sellerQuote.weightGrams())
                  .packageLengthMm(sellerQuote.lengthMm())
                  .packageWidthMm(sellerQuote.widthMm())
                  .packageHeightMm(sellerQuote.heightMm())
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

  private ShippingQuoteRequest shippingQuoteRequest(CheckoutRequest request) {
    return new ShippingQuoteRequest(
        request.easyboxId(),
        request.easyboxName(),
        request.easyboxAddress(),
        request.easyboxCity(),
        request.easyboxCounty(),
        request.easyboxPostalCode(),
        request.recipientName(),
        request.recipientEmail(),
        request.recipientPhone());
  }

  private User user(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new NotFoundException("User not found"));
  }
}
