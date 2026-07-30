package com.avbooknest.payment.stripe;

import com.avbooknest.common.exception.ExternalServiceException;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.model.Transfer;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.TransferCreateParams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class StripeSdkGateway implements StripeGateway {
  private final StripeProperties properties;

  public StripeSdkGateway(StripeProperties properties) {
    this.properties = properties;
  }

  @Override
  public StripePaymentIntentResult createPaymentIntent(
      Long orderId, String orderNumber, BigDecimal amount, String currency, String receiptEmail) {
    requireSandbox();
    try {
      PaymentIntentCreateParams params =
          PaymentIntentCreateParams.builder()
              .setAmount(minorUnits(amount))
              .setCurrency(currency.toLowerCase(Locale.ROOT))
              .setReceiptEmail(receiptEmail)
              .setDescription("BookNest order " + orderNumber)
              .setTransferGroup(orderNumber)
              .putMetadata("orderId", orderId.toString())
              .putMetadata("orderNumber", orderNumber)
              .setAutomaticPaymentMethods(
                  PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                      .setEnabled(true)
                      .build())
              .build();
      PaymentIntent intent =
          PaymentIntent.create(params, requestOptions("booknest-payment-intent-order-" + orderId));
      return new StripePaymentIntentResult(intent.getId(), intent.getClientSecret());
    } catch (StripeException exception) {
      throw stripeFailure("Stripe could not create the sandbox payment", exception);
    }
  }

  @Override
  public void cancelPaymentIntent(String paymentIntentId) {
    requireSandbox();
    try {
      PaymentIntent.retrieve(paymentIntentId, requestOptions(null)).cancel(requestOptions(null));
    } catch (StripeException exception) {
      throw stripeFailure("Stripe could not cancel the sandbox payment", exception);
    }
  }

  @Override
  public String refund(
      String paymentIntentId, BigDecimal amount, String currency, String idempotencyKey) {
    requireSandbox();
    try {
      Refund refund =
          Refund.create(
              RefundCreateParams.builder()
                  .setPaymentIntent(paymentIntentId)
                  .setAmount(minorUnits(amount))
                  .putMetadata("booknestIdempotencyKey", idempotencyKey)
                  .build(),
              requestOptions(idempotencyKey));
      return refund.getId();
    } catch (StripeException exception) {
      throw stripeFailure("Stripe could not refund the sandbox payment", exception);
    }
  }

  @Override
  public String createConnectedAccount(String email) {
    requireSandbox();
    try {
      AccountCreateParams params =
          AccountCreateParams.builder()
              .setType(AccountCreateParams.Type.EXPRESS)
              .setCountry("RO")
              .setEmail(email)
              .setCapabilities(
                  AccountCreateParams.Capabilities.builder()
                      .setTransfers(
                          AccountCreateParams.Capabilities.Transfers.builder()
                              .setRequested(true)
                              .build())
                      .build())
              .putMetadata("platform", "BookNest sandbox")
              .build();
      return Account.create(
              params, requestOptions("booknest-connect-account-" + email.toLowerCase(Locale.ROOT)))
          .getId();
    } catch (StripeException exception) {
      throw stripeFailure("Stripe could not create the seller sandbox account", exception);
    }
  }

  @Override
  public String createOnboardingLink(String accountId) {
    requireSandbox();
    try {
      return com.stripe.model.AccountLink.create(
              AccountLinkCreateParams.builder()
                  .setAccount(accountId)
                  .setRefreshUrl(properties.connectRefreshUrl())
                  .setReturnUrl(properties.connectReturnUrl())
                  .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                  .build(),
              requestOptions(null))
          .getUrl();
    } catch (StripeException exception) {
      throw stripeFailure("Stripe could not create the seller onboarding link", exception);
    }
  }

  @Override
  public StripeAccountStatus accountStatus(String accountId) {
    requireSandbox();
    try {
      Account account = Account.retrieve(accountId, requestOptions(null));
      return new StripeAccountStatus(
          account.getId(),
          Boolean.TRUE.equals(account.getDetailsSubmitted()),
          Boolean.TRUE.equals(account.getChargesEnabled()),
          Boolean.TRUE.equals(account.getPayoutsEnabled()));
    } catch (StripeException exception) {
      throw stripeFailure("Stripe could not read the seller sandbox account", exception);
    }
  }

  @Override
  public String createTransfer(
      BigDecimal amount,
      String currency,
      String destinationAccountId,
      String transferGroup,
      String sourceChargeId,
      String idempotencyKey) {
    requireSandbox();
    try {
      TransferCreateParams params =
          TransferCreateParams.builder()
              .setAmount(minorUnits(amount))
              .setCurrency(currency.toLowerCase(Locale.ROOT))
              .setDestination(destinationAccountId)
              .setTransferGroup(transferGroup)
              .setSourceTransaction(sourceChargeId)
              .putMetadata("booknestTransfer", idempotencyKey)
              .build();
      return Transfer.create(params, requestOptions(idempotencyKey)).getId();
    } catch (StripeException exception) {
      throw stripeFailure("Stripe could not release the seller sandbox transfer", exception);
    }
  }

  @Override
  public StripeWebhookEvent parseWebhook(String payload, String signature) {
    requireWebhook();
    try {
      Event event = Webhook.constructEvent(payload, signature, properties.webhookSecret());
      StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
      if (object == null) {
        object = event.getDataObjectDeserializer().deserializeUnsafe();
      }
      if (object instanceof PaymentIntent intent) {
        String message =
            intent.getLastPaymentError() == null ? null : intent.getLastPaymentError().getMessage();
        return new StripeWebhookEvent(
            event.getId(),
            event.getType(),
            intent.getId(),
            intent.getLatestCharge(),
            message,
            null,
            null,
            null);
      }
      if (object instanceof Account account) {
        return new StripeWebhookEvent(
            event.getId(),
            event.getType(),
            account.getId(),
            null,
            null,
            account.getDetailsSubmitted(),
            account.getChargesEnabled(),
            account.getPayoutsEnabled());
      }
      if (object instanceof Transfer transfer) {
        return new StripeWebhookEvent(
            event.getId(), event.getType(), transfer.getId(), null, null, null, null, null);
      }
      return new StripeWebhookEvent(
          event.getId(), event.getType(), null, null, null, null, null, null);
    } catch (SignatureVerificationException exception) {
      throw new ExternalServiceException("Invalid Stripe webhook signature", exception);
    } catch (EventDataObjectDeserializationException exception) {
      throw new ExternalServiceException("Stripe webhook payload could not be parsed", exception);
    }
  }

  private RequestOptions requestOptions(String idempotencyKey) {
    RequestOptions.RequestOptionsBuilder builder =
        RequestOptions.builder().setApiKey(properties.secretKey());
    if (idempotencyKey != null) {
      builder.setIdempotencyKey(idempotencyKey);
    }
    return builder.build();
  }

  private long minorUnits(BigDecimal value) {
    return value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
  }

  private void requireSandbox() {
    if (!properties.sandboxConfigured()) {
      throw new ExternalServiceException(
          "Stripe sandbox is not configured with pk_test_ and sk_test_ keys");
    }
  }

  private void requireWebhook() {
    requireSandbox();
    if (properties.webhookSecret() == null || !properties.webhookSecret().startsWith("whsec_")) {
      throw new ExternalServiceException("Stripe sandbox webhook secret is not configured");
    }
  }

  private ExternalServiceException stripeFailure(String message, StripeException exception) {
    String detail =
        exception.getStripeError() == null
            ? exception.getMessage()
            : exception.getStripeError().getMessage();
    return new ExternalServiceException(
        detail == null || detail.isBlank() ? message : message + ": " + detail, exception);
  }
}
