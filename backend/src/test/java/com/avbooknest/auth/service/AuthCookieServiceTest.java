package com.avbooknest.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.avbooknest.auth.config.JwtProperties;
import com.avbooknest.auth.dto.AuthResponse;
import com.avbooknest.auth.dto.UserResponse;
import com.avbooknest.common.exception.UnauthorizedException;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthCookieServiceTest {
  @Test
  void writesHttpOnlySameSiteCookiesWithoutExposingTokensToJavaScript() {
    AuthCookieService service = new AuthCookieService(properties(), false, "Lax");
    MockHttpServletResponse response = new MockHttpServletResponse();

    service.write(response, authentication());

    List<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);
    assertEquals(2, cookies.size());
    assertTrue(cookies.get(0).startsWith("booknest-access=access.jwt;"));
    assertTrue(cookies.get(0).contains("Path=/"));
    assertTrue(cookies.get(0).contains("Max-Age=900"));
    assertTrue(cookies.get(0).contains("HttpOnly"));
    assertTrue(cookies.get(0).contains("SameSite=Lax"));
    assertTrue(cookies.get(1).contains("Max-Age=604800"));
  }

  @Test
  void productionCookiesUseSecureHostPrefix() {
    AuthCookieService service = new AuthCookieService(properties(), true, "Lax");
    MockHttpServletResponse response = new MockHttpServletResponse();

    service.write(response, authentication());

    List<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);
    assertTrue(cookies.get(0).startsWith("__Host-booknest-access="));
    assertTrue(cookies.get(1).startsWith("__Host-booknest-refresh="));
    assertTrue(cookies.stream().allMatch(cookie -> cookie.contains("Secure")));
    assertTrue(cookies.stream().noneMatch(cookie -> cookie.contains("Domain=")));
  }

  @Test
  void readsRefreshTokenFromCookieAndRejectsMissingCookie() {
    AuthCookieService service = new AuthCookieService(properties(), false, "Lax");
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new Cookie("booknest-refresh", "refresh.jwt"));

    assertEquals("refresh.jwt", service.requireRefreshToken(request));
    assertThrows(
        UnauthorizedException.class,
        () -> service.requireRefreshToken(new MockHttpServletRequest()));
  }

  private JwtProperties properties() {
    JwtProperties properties = new JwtProperties();
    properties.setSecret("a-secret-that-is-at-least-32-characters-long");
    properties.setAccessTokenExpiration(Duration.ofMinutes(15));
    properties.setRefreshTokenExpiration(Duration.ofDays(7));
    return properties;
  }

  private AuthResponse authentication() {
    return new AuthResponse(
        "access.jwt",
        "refresh.jwt",
        "Bearer",
        900,
        new UserResponse(1L, "Ana", "Pop", "ana@example.com", null, "USER", true));
  }
}
