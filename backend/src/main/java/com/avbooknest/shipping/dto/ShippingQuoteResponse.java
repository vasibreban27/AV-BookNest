package com.avbooknest.shipping.dto;

import java.math.BigDecimal;
import java.util.List;

public record ShippingQuoteResponse(
    BigDecimal shippingCost, String currency, List<SellerShippingQuoteResponse> packages) {}
