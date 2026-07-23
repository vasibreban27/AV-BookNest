package com.avbooknest.shipping.sameday;

import com.avbooknest.common.exception.ExternalServiceException;
import com.avbooknest.shipping.dto.EasyboxResponse;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
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
    requireConfigured();
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

  private void requireConfigured() {
    if (!properties.enabled()
        || properties.username() == null
        || properties.username().isBlank()
        || properties.password() == null
        || properties.password().isBlank()) {
      throw new ExternalServiceException("Sameday integration is not configured");
    }
  }
}
