package com.avbooknest.shipping.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ShippingQuoteRequest(
    @NotBlank @Size(max = 100) String easyboxId,
    @NotBlank @Size(max = 255) String easyboxName,
    @NotBlank @Size(max = 255) String easyboxAddress,
    @NotBlank @Size(max = 100) String easyboxCity,
    @NotBlank @Size(max = 100) String easyboxCounty,
    @Size(max = 20) String easyboxPostalCode,
    @NotBlank @Size(max = 200) String recipientName,
    @NotBlank @Email @Size(max = 255) String recipientEmail,
    @NotBlank @Pattern(regexp = "^\\+?[0-9 ]{9,20}$") String recipientPhone) {}
