package com.avbooknest.order.controller;

import com.avbooknest.order.dto.CheckoutRequest;
import com.avbooknest.order.dto.OrderResponse;
import com.avbooknest.order.service.OrderService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
  private final OrderService orderService;

  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @GetMapping
  public List<OrderResponse> list(Authentication auth) {
    return orderService.list(auth.getName());
  }

  @GetMapping("/{orderId}")
  public OrderResponse get(@PathVariable Long orderId, Authentication auth) {
    return orderService.get(orderId, auth.getName());
  }

  @PostMapping("/checkout")
  public ResponseEntity<OrderResponse> checkout(
      @Valid @RequestBody CheckoutRequest request, Authentication auth) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(orderService.checkout(request, auth.getName()));
  }

  @PatchMapping("/{orderId}/cancel")
  public OrderResponse cancel(@PathVariable Long orderId, Authentication auth) {
    return orderService.cancel(orderId, auth.getName());
  }
}
