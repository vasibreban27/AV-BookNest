package com.avbooknest.cart.controller;

import com.avbooknest.cart.dto.AddCartItemRequest;
import com.avbooknest.cart.dto.CartResponse;
import com.avbooknest.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {
  private final CartService cartService;

  public CartController(CartService cartService) {
    this.cartService = cartService;
  }

  @GetMapping
  public CartResponse get(Authentication authentication) {
    return cartService.get(authentication.getName());
  }

  @PostMapping("/items")
  public ResponseEntity<CartResponse> addItem(
      @Valid @RequestBody AddCartItemRequest request, Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(cartService.addItem(request.bookId(), authentication.getName()));
  }

  @DeleteMapping("/items/{bookId}")
  public ResponseEntity<Void> removeItem(@PathVariable Long bookId, Authentication authentication) {
    cartService.removeItem(bookId, authentication.getName());
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping
  public ResponseEntity<Void> clear(Authentication authentication) {
    cartService.clear(authentication.getName());
    return ResponseEntity.noContent().build();
  }
}
