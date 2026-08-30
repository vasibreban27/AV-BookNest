package com.avbooknest.contact.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ContactRequest(
    @NotBlank(message = "Name is required")
        @Size(max = 201, message = "Name must contain at most 201 characters")
        String name,
    @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 254, message = "Email must contain at most 254 characters")
        String email,
    @NotBlank(message = "Topic is required")
        @Pattern(
            regexp = "GENERAL|ORDER|PAYMENT|DELIVERY|ACCOUNT|LISTING|PRIVACY|OTHER",
            message = "Topic is invalid")
        String topic,
    @NotBlank(message = "Subject is required")
        @Size(max = 150, message = "Subject must contain at most 150 characters")
        String subject,
    @NotBlank(message = "Message is required")
        @Size(min = 20, max = 4000, message = "Message must contain between 20 and 4000 characters")
        String message,
    @NotNull(message = "Privacy consent is required")
        @AssertTrue(message = "Privacy policy must be accepted")
        Boolean privacyAccepted,
    @Size(max = 100) String website,
    @Positive Long orderId,
    @Positive Long bookId) {
  public ContactRequest(
      String name,
      String email,
      String topic,
      String subject,
      String message,
      Boolean privacyAccepted,
      String website) {
    this(name, email, topic, subject, message, privacyAccepted, website, null, null);
  }
}
