package com.avbooknest.auth.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class AuthResponseTest {
  @Test
  void rawTokensAreNeverSerializedInTheResponseBody() throws Exception {
    AuthResponse response =
        new AuthResponse(
            "access-secret",
            "refresh-secret",
            "Bearer",
            900,
            new UserResponse(1L, "Ana", "Pop", "ana@example.com", null, "USER", true));

    String json = new ObjectMapper().writeValueAsString(response);

    assertFalse(json.contains("access-secret"));
    assertFalse(json.contains("refresh-secret"));
    assertFalse(json.contains("accessToken"));
    assertFalse(json.contains("refreshToken"));
    assertTrue(json.contains("ana@example.com"));
  }
}
