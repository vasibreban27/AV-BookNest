package com.avbooknest.shipping.dto;

import com.avbooknest.shipment.model.PackageSize;
import java.math.BigDecimal;

public record SellerShippingQuoteResponse(
    Long sellerId,
    BigDecimal cost,
    PackageSize packageSize,
    Integer weightGrams,
    Integer lengthMm,
    Integer widthMm,
    Integer heightMm) {}
