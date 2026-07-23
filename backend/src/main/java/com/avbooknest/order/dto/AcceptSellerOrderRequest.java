package com.avbooknest.order.dto;

import com.avbooknest.shipment.model.PackageSize;
import jakarta.validation.constraints.NotNull;

public record AcceptSellerOrderRequest(@NotNull PackageSize packageSize) {}
