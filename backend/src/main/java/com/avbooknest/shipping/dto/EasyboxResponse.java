package com.avbooknest.shipping.dto;

import java.math.BigDecimal;

public record EasyboxResponse(
    String id,
    String name,
    String address,
    String city,
    String county,
    String postalCode,
    BigDecimal latitude,
    BigDecimal longitude) {}
