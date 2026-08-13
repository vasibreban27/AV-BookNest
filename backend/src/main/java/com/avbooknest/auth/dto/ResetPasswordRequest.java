package com.avbooknest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank @Size(max = 512) String token,
    @NotBlank
        @Size(min = 8, max = 72, message = "Password must contain between 8 and 72 characters")
        @Pattern(regexp = ".*[A-Za-z].*", message = "Password must contain a letter")
        @Pattern(regexp = ".*\\d.*", message = "Password must contain a digit")
        String password) {}
