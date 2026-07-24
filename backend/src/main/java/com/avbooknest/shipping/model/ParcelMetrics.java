package com.avbooknest.shipping.model;

import com.avbooknest.shipment.model.PackageSize;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record ParcelMetrics(
    int weightGrams, int lengthMm, int widthMm, int heightMm, PackageSize packageSize) {

  public BigDecimal weightKg() {
    return BigDecimal.valueOf(weightGrams).divide(BigDecimal.valueOf(1000), 3, RoundingMode.UP);
  }

  public BigDecimal lengthCm() {
    return millimetresToCentimetres(lengthMm);
  }

  public BigDecimal widthCm() {
    return millimetresToCentimetres(widthMm);
  }

  public BigDecimal heightCm() {
    return millimetresToCentimetres(heightMm);
  }

  private BigDecimal millimetresToCentimetres(int millimetres) {
    return BigDecimal.valueOf(millimetres)
        .divide(BigDecimal.TEN, 1, RoundingMode.UP)
        .stripTrailingZeros();
  }
}
