package com.avbooknest.shipping.controller;

import com.avbooknest.shipping.dto.EasyboxResponse;
import com.avbooknest.shipping.dto.ShippingQuoteRequest;
import com.avbooknest.shipping.dto.ShippingQuoteResponse;
import com.avbooknest.shipping.sameday.SamedayClient;
import com.avbooknest.shipping.service.ShippingQuoteService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shipping")
public class ShippingController {
  private final SamedayClient samedayClient;
  private final ShippingQuoteService shippingQuoteService;

  public ShippingController(
      SamedayClient samedayClient, ShippingQuoteService shippingQuoteService) {
    this.samedayClient = samedayClient;
    this.shippingQuoteService = shippingQuoteService;
  }

  @GetMapping("/easyboxes")
  public List<EasyboxResponse> easyboxes(
      @RequestParam(name = "query", required = false) String query) {
    return samedayClient.lockers(query);
  }

  @PostMapping("/quote")
  public ShippingQuoteResponse quote(
      @Valid @RequestBody ShippingQuoteRequest request, Principal principal) {
    return shippingQuoteService.quote(principal.getName(), request);
  }
}
