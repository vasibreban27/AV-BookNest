package com.avbooknest.shipping.sameday;

import com.avbooknest.common.exception.ExternalServiceException;
import com.avbooknest.order.model.Order;
import com.avbooknest.order.model.SellerOrder;
import com.avbooknest.shipment.model.Shipment;
import com.avbooknest.shipping.dto.EasyboxResponse;
import com.avbooknest.shipping.dto.ShippingQuoteRequest;
import com.avbooknest.shipping.model.ParcelMetrics;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class SamedayClient {
  private final SamedayProperties properties;
  private final RestClient restClient;
  private String token;
  private Instant tokenExpiresAt = Instant.EPOCH;

  public SamedayClient(SamedayProperties properties, RestClient.Builder builder) {
    this.properties = properties;
    restClient = builder.baseUrl(properties.baseUrl()).build();
  }

  public List<EasyboxResponse> lockers(String search) {
    requireCredentials();
    try {
      JsonNode response =
          restClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/api/client/lockers")
                          .queryParam("page", 1)
                          .queryParam("countPerPage", 500)
                          .build())
              .header("X-AUTH-TOKEN", accessToken())
              .retrieve()
              .body(JsonNode.class);
      if (response == null || !response.path("data").isArray()) {
        throw new ExternalServiceException("Sameday returned an invalid locker response");
      }
      String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
      List<EasyboxResponse> lockers = new ArrayList<>();
      for (JsonNode locker : response.path("data")) {
        EasyboxResponse mapped = mapLocker(locker);
        String searchable =
            (mapped.name() + " " + mapped.address() + " " + mapped.city() + " " + mapped.county())
                .toLowerCase(Locale.ROOT);
        if ((normalizedSearch.isBlank() || searchable.contains(normalizedSearch))
            && lockers.size() < 50) {
          lockers.add(mapped);
        }
      }
      return lockers;
    } catch (ExternalServiceException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new ExternalServiceException(
          "Sameday locker service is temporarily unavailable", exception);
    }
  }

  public SamedayEstimate estimateCost(ParcelMetrics parcel, ShippingQuoteRequest destination) {
    requireAwbConfiguration();
    try {
      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
      body.add("pickupPoint", properties.pickupPointId().toString());
      body.add("packageType", "0");
      body.add("packageNumber", "1");
      body.add("packageWeight", parcel.weightKg().toPlainString());
      body.add("service", properties.serviceId().toString());
      body.add("awbPayment", "1");
      body.add("cashOnDelivery", "0");
      body.add("insuredValue", "0");
      body.add("thirdPartyPickup", "0");
      body.add("currency", "RON");
      body.add("lockerLastMile", destination.easyboxId().trim());
      body.add("awbRecipient[city]", destination.easyboxCity().trim());
      body.add("awbRecipient[county]", destination.easyboxCounty().trim());
      body.add("awbRecipient[address]", destination.easyboxAddress().trim());
      body.add("awbRecipient[name]", destination.recipientName().trim());
      body.add("awbRecipient[phoneNumber]", destination.recipientPhone().replace(" ", ""));
      body.add("awbRecipient[email]", destination.recipientEmail().trim().toLowerCase());
      if (destination.easyboxPostalCode() != null && !destination.easyboxPostalCode().isBlank()) {
        body.add("awbRecipient[postalCode]", destination.easyboxPostalCode().trim());
      }
      body.add("parcels[0][weight]", parcel.weightKg().toPlainString());
      body.add("parcels[0][length]", parcel.lengthCm().toPlainString());
      body.add("parcels[0][width]", parcel.widthCm().toPlainString());
      body.add("parcels[0][height]", parcel.heightCm().toPlainString());

      JsonNode response =
          restClient
              .post()
              .uri("/api/awb/estimate-cost")
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .header("X-AUTH-TOKEN", accessToken())
              .body(body)
              .retrieve()
              .body(JsonNode.class);
      if (response == null || !response.path("amount").isNumber()) {
        throw new ExternalServiceException("Sameday returned an invalid cost estimate");
      }
      return new SamedayEstimate(
          response.path("amount").decimalValue(), response.path("currency").asText("RON"));
    } catch (ExternalServiceException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new ExternalServiceException(
          "Sameday cost estimation is temporarily unavailable", exception);
    }
  }

  public SamedayAwb createAwb(SellerOrder sellerOrder) {
    requireAwbConfiguration();
    Shipment shipment = sellerOrder.getShipment();
    Order order = sellerOrder.getOrder();
    ParcelMetrics parcel =
        new ParcelMetrics(
            shipment.getPackageWeightGrams(),
            shipment.getPackageLengthMm(),
            shipment.getPackageWidthMm(),
            shipment.getPackageHeightMm(),
            shipment.getPackageSize());
    try {
      MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
      addParcelConfiguration(body, parcel, shipment.getEasyboxId());
      body.add("clientInternalReference", order.getOrderNumber() + "-" + sellerOrder.getId());
      body.add("awbRecipient[city]", shipment.getEasyboxCity());
      body.add("awbRecipient[county]", shipment.getEasyboxCounty());
      body.add("awbRecipient[address]", shipment.getEasyboxAddress());
      body.add("awbRecipient[name]", order.getRecipientName());
      body.add("awbRecipient[phoneNumber]", order.getRecipientPhone().replace(" ", ""));
      body.add("awbRecipient[email]", order.getRecipientEmail().trim().toLowerCase());
      if (shipment.getEasyboxPostalCode() != null && !shipment.getEasyboxPostalCode().isBlank()) {
        body.add("awbRecipient[postalCode]", shipment.getEasyboxPostalCode());
      }

      JsonNode response =
          restClient
              .post()
              .uri("/api/awb")
              .contentType(MediaType.APPLICATION_FORM_URLENCODED)
              .header("X-AUTH-TOKEN", accessToken())
              .body(body)
              .retrieve()
              .body(JsonNode.class);
      String awbNumber = response == null ? null : response.path("awbNumber").asText(null);
      if (awbNumber == null || awbNumber.isBlank()) {
        throw new ExternalServiceException("Sameday returned an invalid AWB response");
      }
      JsonNode parcels = response.path("parcels");
      String parcelAwb =
          parcels.isArray() && !parcels.isEmpty()
              ? parcels.get(0).path("awbNumber").asText(awbNumber)
              : awbNumber;
      return new SamedayAwb(awbNumber, parcelAwb);
    } catch (ExternalServiceException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new ExternalServiceException("Sameday AWB creation failed", exception);
    }
  }

  public List<SamedayStatusUpdate> statusUpdates(Instant start, Instant end) {
    requireCredentials();
    try {
      JsonNode response =
          restClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/api/client/status-sync")
                          .queryParam("startTimestamp", start.getEpochSecond())
                          .queryParam("endTimestamp", end.getEpochSecond())
                          .queryParam("page", 1)
                          .queryParam("countPerPage", 500)
                          .build())
              .header("X-AUTH-TOKEN", accessToken())
              .retrieve()
              .body(JsonNode.class);
      if (response == null || !response.path("data").isArray()) {
        throw new ExternalServiceException("Sameday returned an invalid status response");
      }
      List<SamedayStatusUpdate> updates = new ArrayList<>();
      for (JsonNode item : response.path("data")) {
        String parcelAwb = item.path("parcelAwbNumber").asText(null);
        if (parcelAwb != null && !parcelAwb.isBlank()) {
          updates.add(
              new SamedayStatusUpdate(
                  parcelAwb,
                  item.path("status").asText(""),
                  item.path("statusLabel").asText(""),
                  item.path("statusState").asText(""),
                  parseStatusDate(item.path("statusDate"), end)));
        }
      }
      return updates;
    } catch (ExternalServiceException exception) {
      throw exception;
    } catch (RuntimeException exception) {
      throw new ExternalServiceException("Sameday status synchronization failed", exception);
    }
  }

  private synchronized String accessToken() {
    if (token != null && Instant.now().isBefore(tokenExpiresAt)) {
      return token;
    }
    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("remember_me", "true");
    JsonNode response =
        restClient
            .post()
            .uri("/api/authenticate")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header("X-Auth-Username", properties.username())
            .header("X-Auth-Password", properties.password())
            .body(body)
            .retrieve()
            .body(JsonNode.class);
    token = response == null ? null : response.path("token").asText(null);
    if (token == null || token.isBlank()) {
      throw new ExternalServiceException("Sameday authentication failed");
    }
    tokenExpiresAt = Instant.now().plus(9, ChronoUnit.MINUTES);
    return token;
  }

  private EasyboxResponse mapLocker(JsonNode locker) {
    return new EasyboxResponse(
        locker.path("lockerId").asText(),
        locker.path("name").asText(),
        locker.path("address").asText(),
        locker.path("city").asText(),
        locker.path("county").asText(),
        locker.path("postalCode").asText(),
        decimal(locker.path("lat")),
        decimal(locker.path("lng")));
  }

  private BigDecimal decimal(JsonNode value) {
    return value.isNumber() ? value.decimalValue() : new BigDecimal(value.asText("0"));
  }

  private void addParcelConfiguration(
      MultiValueMap<String, String> body, ParcelMetrics parcel, String easyboxId) {
    body.add("pickupPoint", properties.pickupPointId().toString());
    body.add("packageType", "0");
    body.add("packageNumber", "1");
    body.add("packageWeight", parcel.weightKg().toPlainString());
    body.add("service", properties.serviceId().toString());
    body.add("awbPayment", "1");
    body.add("cashOnDelivery", "0");
    body.add("insuredValue", "0");
    body.add("thirdPartyPickup", "0");
    body.add("currency", "RON");
    body.add("lockerLastMile", easyboxId);
    body.add("parcels[0][weight]", parcel.weightKg().toPlainString());
    body.add("parcels[0][length]", parcel.lengthCm().toPlainString());
    body.add("parcels[0][width]", parcel.widthCm().toPlainString());
    body.add("parcels[0][height]", parcel.heightCm().toPlainString());
  }

  private Instant parseStatusDate(JsonNode value, Instant fallback) {
    if (value.isNumber()) {
      long timestamp = value.asLong();
      return Instant.ofEpochMilli(timestamp > 10_000_000_000L ? timestamp : timestamp * 1000);
    }
    String text = value.asText("");
    if (text.isBlank()) {
      return fallback;
    }
    try {
      return Instant.parse(text);
    } catch (DateTimeParseException ignored) {
      try {
        return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .atZone(ZoneId.of("Europe/Bucharest"))
            .toInstant();
      } catch (DateTimeParseException invalidDate) {
        return fallback;
      }
    }
  }

  private void requireCredentials() {
    if (!properties.enabled()
        || properties.username() == null
        || properties.username().isBlank()
        || properties.password() == null
        || properties.password().isBlank()) {
      throw new ExternalServiceException("Sameday integration is not configured");
    }
  }

  private void requireAwbConfiguration() {
    requireCredentials();
    if (properties.serviceId() == null || properties.pickupPointId() == null) {
      throw new ExternalServiceException(
          "Sameday Basic service and pickup point are not configured");
    }
  }
}
