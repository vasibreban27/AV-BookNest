package com.avbooknest.shipping.service;

import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.book.model.Book;
import com.avbooknest.cart.model.Cart;
import com.avbooknest.cart.model.CartItem;
import com.avbooknest.cart.repository.CartRepository;
import com.avbooknest.common.exception.ConflictException;
import com.avbooknest.common.exception.ExternalServiceException;
import com.avbooknest.common.exception.NotFoundException;
import com.avbooknest.shipping.dto.SellerShippingQuoteResponse;
import com.avbooknest.shipping.dto.ShippingQuoteRequest;
import com.avbooknest.shipping.dto.ShippingQuoteResponse;
import com.avbooknest.shipping.model.ParcelMetrics;
import com.avbooknest.shipping.sameday.SamedayClient;
import com.avbooknest.shipping.sameday.SamedayEstimate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShippingQuoteService {
  private static final String CURRENCY = "RON";
  private final CartRepository cartRepository;
  private final UserRepository userRepository;
  private final ParcelCalculator parcelCalculator;
  private final SamedayClient samedayClient;

  public ShippingQuoteService(
      CartRepository cartRepository,
      UserRepository userRepository,
      ParcelCalculator parcelCalculator,
      SamedayClient samedayClient) {
    this.cartRepository = cartRepository;
    this.userRepository = userRepository;
    this.parcelCalculator = parcelCalculator;
    this.samedayClient = samedayClient;
  }

  @Transactional(readOnly = true)
  public ShippingQuoteResponse quote(String email, ShippingQuoteRequest request) {
    User buyer =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new NotFoundException("User not found"));
    Cart cart =
        cartRepository
            .findByUserId(buyer.getId())
            .orElseThrow(() -> new ConflictException("Your cart is empty"));
    return quote(cart, request);
  }

  public ShippingQuoteResponse quote(Cart cart, ShippingQuoteRequest request) {
    if (cart.getItems().isEmpty()) {
      throw new ConflictException("Your cart is empty");
    }
    Map<Long, List<Book>> booksBySeller =
        cart.getItems().stream()
            .map(CartItem::getBook)
            .collect(Collectors.groupingBy(book -> book.getSeller().getId()));
    List<SellerShippingQuoteResponse> packages =
        booksBySeller.entrySet().stream()
            .map(entry -> quoteSeller(entry.getKey(), entry.getValue(), request))
            .toList();
    BigDecimal shippingCost =
        packages.stream()
            .map(SellerShippingQuoteResponse::cost)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    return new ShippingQuoteResponse(shippingCost, CURRENCY, packages);
  }

  private SellerShippingQuoteResponse quoteSeller(
      Long sellerId, List<Book> books, ShippingQuoteRequest request) {
    ParcelMetrics parcel = parcelCalculator.calculate(books);
    SamedayEstimate estimate = samedayClient.estimateCost(parcel, request);
    if (!CURRENCY.equalsIgnoreCase(estimate.currency())) {
      throw new ExternalServiceException("Sameday returned an unsupported quote currency");
    }
    return new SellerShippingQuoteResponse(
        sellerId,
        estimate.amount().setScale(2, RoundingMode.HALF_UP),
        parcel.packageSize(),
        parcel.weightGrams(),
        parcel.lengthMm(),
        parcel.widthMm(),
        parcel.heightMm());
  }
}
