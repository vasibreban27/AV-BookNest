package com.avbooknest.payment.service;

import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.common.exception.ConflictException;
import com.avbooknest.common.exception.NotFoundException;
import com.avbooknest.payment.dto.StripeConnectStatusResponse;
import com.avbooknest.payment.dto.StripeDashboardLinkResponse;
import com.avbooknest.payment.dto.StripeOnboardingLinkResponse;
import com.avbooknest.payment.stripe.StripeAccountStatus;
import com.avbooknest.payment.stripe.StripeGateway;
import com.avbooknest.payment.stripe.StripeProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StripeConnectService {
  private final UserRepository userRepository;
  private final StripeGateway stripeGateway;
  private final StripeProperties stripeProperties;

  public StripeConnectService(
      UserRepository userRepository,
      StripeGateway stripeGateway,
      StripeProperties stripeProperties) {
    this.userRepository = userRepository;
    this.stripeGateway = stripeGateway;
    this.stripeProperties = stripeProperties;
  }

  public StripeOnboardingLinkResponse onboardingLink(String email) {
    User user = user(email);
    if (user.getStripeAccountId() == null) {
      user.connectStripeAccount(stripeGateway.createConnectedAccount(user.getEmail()));
    }
    return new StripeOnboardingLinkResponse(
        stripeGateway.createOnboardingLink(user.getStripeAccountId()));
  }

  public StripeConnectStatusResponse status(String email) {
    User user = user(email);
    if (!stripeProperties.sandboxConfigured()) {
      return new StripeConnectStatusResponse(false, false, false, false, false);
    }
    if (user.getStripeAccountId() == null) {
      return new StripeConnectStatusResponse(true, false, false, false, false);
    }
    StripeAccountStatus status = stripeGateway.accountStatus(user.getStripeAccountId());
    user.updateStripeStatus(
        status.detailsSubmitted(), status.chargesEnabled(), status.payoutsEnabled());
    return response(user);
  }

  public StripeDashboardLinkResponse dashboardLink(String email) {
    User user = user(email);
    if (!stripeProperties.sandboxConfigured()
        || user.getStripeAccountId() == null
        || !user.isStripePayoutsEnabled()) {
      throw new ConflictException("Stripe Express account is not ready");
    }
    return new StripeDashboardLinkResponse(
        stripeGateway.createDashboardLoginLink(user.getStripeAccountId()));
  }

  private StripeConnectStatusResponse response(User user) {
    return new StripeConnectStatusResponse(
        true,
        user.getStripeAccountId() != null,
        user.isStripeDetailsSubmitted(),
        user.isStripeChargesEnabled(),
        user.isStripePayoutsEnabled());
  }

  private User user(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new NotFoundException("User not found"));
  }
}
