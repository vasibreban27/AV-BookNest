package com.avbooknest.payment.controller;

import com.avbooknest.payment.service.StripePaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stripe")
public class StripeWebhookController {
  private final StripePaymentService stripePaymentService;

  public StripeWebhookController(StripePaymentService stripePaymentService) {
    this.stripePaymentService = stripePaymentService;
  }

  @PostMapping("/webhook")
  public ResponseEntity<Void> webhook(
      @RequestBody String payload, @RequestHeader(name = "Stripe-Signature") String signature) {
    stripePaymentService.processWebhook(payload, signature);
    return ResponseEntity.noContent().build();
  }
}
