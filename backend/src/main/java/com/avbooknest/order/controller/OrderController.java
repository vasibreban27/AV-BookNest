package com.avbooknest.order.controller;

import com.avbooknest.order.dto.CheckoutRequest;
import com.avbooknest.order.dto.OrderIssueRequest;
import com.avbooknest.order.dto.OrderResponse;
import com.avbooknest.order.dto.SellerOrderResponse;
import com.avbooknest.order.dto.StripeCheckoutResponse;
import com.avbooknest.order.model.OrderStatus;
import com.avbooknest.order.service.OrderService;
import com.avbooknest.order.service.SellerOrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
  private final OrderService orderService;
  private final SellerOrderService sellerOrderService;

  public OrderController(OrderService orderService, SellerOrderService sellerOrderService) {
    this.orderService = orderService;
    this.sellerOrderService = sellerOrderService;
  }

  @GetMapping
  public List<OrderResponse> list(
      @RequestParam(required = false) OrderStatus status,
      @RequestParam(required = false, name = "q") String query,
      Authentication auth) {
    return orderService.list(auth.getName(), status, query);
  }

  @GetMapping("/{orderId}")
  public OrderResponse get(@PathVariable Long orderId, Authentication auth) {
    return orderService.get(orderId, auth.getName());
  }

  @PostMapping("/checkout")
  public ResponseEntity<StripeCheckoutResponse> checkout(
      @Valid @RequestBody CheckoutRequest request, Authentication auth) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(orderService.checkout(request, auth.getName()));
  }

  @PatchMapping("/{orderId}/cancel")
  public OrderResponse cancel(@PathVariable Long orderId, Authentication auth) {
    return orderService.cancel(orderId, auth.getName());
  }

  @PostMapping("/{orderId}/seller-orders/{sellerOrderId}/issue")
  public SellerOrderResponse reportIssue(
      @PathVariable Long orderId,
      @PathVariable Long sellerOrderId,
      @Valid @RequestBody OrderIssueRequest request,
      Authentication auth) {
    return sellerOrderService.reportIssue(orderId, sellerOrderId, auth.getName(), request.reason());
  }

  @PatchMapping("/{orderId}/seller-orders/{sellerOrderId}/issue/resolve")
  public SellerOrderResponse resolveIssue(
      @PathVariable Long orderId, @PathVariable Long sellerOrderId, Authentication auth) {
    return sellerOrderService.resolveIssue(orderId, sellerOrderId, auth.getName());
  }
}
