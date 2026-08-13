package com.avbooknest.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @Size(max = 30)
        @Pattern(
            regexp = "^$|^\\+?[0-9 ()-]{7,25}$",
            message = "Phone number has an invalid format")
        String phoneNumber) {}
