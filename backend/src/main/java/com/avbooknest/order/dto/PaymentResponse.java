package com.avbooknest.order.dto;

import com.avbooknest.order.model.Payment;
import com.avbooknest.order.model.PaymentProvider;
import com.avbooknest.order.model.PaymentStatus;
import java.math.BigDecimal;

public record PaymentResponse(
    Long id, PaymentProvider provider, BigDecimal amount, String currency, PaymentStatus status) {
  public static PaymentResponse from(Payment payment) {
    return new PaymentResponse(
        payment.getId(),
        payment.getProvider(),
        payment.getAmount(),
        payment.getCurrency(),
        payment.getStatus());
  }
}
