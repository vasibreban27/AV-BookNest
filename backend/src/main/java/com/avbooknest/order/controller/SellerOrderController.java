package com.avbooknest.order.controller;

import com.avbooknest.order.dto.SellerOrderResponse;
import com.avbooknest.order.service.SellerOrderService;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seller-orders")
public class SellerOrderController {
  private final SellerOrderService sellerOrderService;

  public SellerOrderController(SellerOrderService sellerOrderService) {
    this.sellerOrderService = sellerOrderService;
  }

  @GetMapping("/mine")
  public List<SellerOrderResponse> mine(Authentication authentication) {
    return sellerOrderService.listForSeller(authentication.getName());
  }

  @PatchMapping("/{sellerOrderId}/accept")
  public SellerOrderResponse accept(
      @PathVariable Long sellerOrderId, Authentication authentication) {
    return sellerOrderService.accept(sellerOrderId, authentication.getName());
  }

  @PatchMapping("/{sellerOrderId}/cancel")
  public SellerOrderResponse cancel(
      @PathVariable Long sellerOrderId, Authentication authentication) {
    return sellerOrderService.cancel(sellerOrderId, authentication.getName());
  }
}
