package com.avbooknest.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record AuthResponse(
    @JsonIgnore String accessToken,
    @JsonIgnore String refreshToken,
    String tokenType,
    long expiresIn,
    UserResponse user) {}
