package com.avbooknest.shipping.sameday;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sameday")
public record SamedayProperties(
    boolean enabled,
    String baseUrl,
    String username,
    String password,
    Integer serviceId,
    Integer pickupPointId) {}
