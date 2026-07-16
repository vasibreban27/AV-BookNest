package com.avbooknest.wishlist.controller;

import com.avbooknest.wishlist.dto.WishlistItemResponse;
import com.avbooknest.wishlist.service.WishlistService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {
  private final WishlistService wishlistService;

  public WishlistController(WishlistService wishlistService) {
    this.wishlistService = wishlistService;
  }

  @GetMapping
  public List<WishlistItemResponse> list(Authentication authentication) {
    return wishlistService.list(authentication.getName());
  }

  @PostMapping("/books/{bookId}")
  public ResponseEntity<WishlistItemResponse> add(
      @PathVariable Long bookId, Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(wishlistService.add(bookId, authentication.getName()));
  }

  @DeleteMapping("/books/{bookId}")
  public ResponseEntity<Void> remove(@PathVariable Long bookId, Authentication authentication) {
    wishlistService.remove(bookId, authentication.getName());
    return ResponseEntity.noContent().build();
  }
}
