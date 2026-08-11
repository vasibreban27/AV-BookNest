package com.avbooknest.payment.controller;

import com.avbooknest.payment.dto.StripeConnectStatusResponse;
import com.avbooknest.payment.dto.StripeDashboardLinkResponse;
import com.avbooknest.payment.dto.StripeOnboardingLinkResponse;
import com.avbooknest.payment.service.StripeConnectService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stripe/connect")
public class StripeConnectController {
  private final StripeConnectService stripeConnectService;

  public StripeConnectController(StripeConnectService stripeConnectService) {
    this.stripeConnectService = stripeConnectService;
  }

  @GetMapping("/status")
  public StripeConnectStatusResponse status(Authentication authentication) {
    return stripeConnectService.status(authentication.getName());
  }

  @PostMapping("/onboarding-link")
  public StripeOnboardingLinkResponse onboardingLink(Authentication authentication) {
    return stripeConnectService.onboardingLink(authentication.getName());
  }

  @PostMapping("/dashboard-link")
  public StripeDashboardLinkResponse dashboardLink(Authentication authentication) {
    return stripeConnectService.dashboardLink(authentication.getName());
  }
}
