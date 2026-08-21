package com.avbooknest.auth.service;

import com.avbooknest.auth.config.JwtProperties;
import com.avbooknest.auth.dto.AuthResponse;
import com.avbooknest.common.exception.UnauthorizedException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class AuthCookieService {
  private static final String ACCESS_COOKIE = "booknest-access";
  private static final String REFRESH_COOKIE = "booknest-refresh";

  private final Duration accessExpiration;
  private final Duration refreshExpiration;
  private final boolean secure;
  private final String sameSite;

  public AuthCookieService(
      JwtProperties jwtProperties,
      @Value("${app.auth.cookie.secure:false}") boolean secure,
      @Value("${app.auth.cookie.same-site:Lax}") String sameSite) {
    this.accessExpiration = jwtProperties.getAccessTokenExpiration();
    this.refreshExpiration = jwtProperties.getRefreshTokenExpiration();
    this.secure = secure;
    this.sameSite = sameSite;
  }

  public void write(HttpServletResponse response, AuthResponse authentication) {
    add(response, accessCookieName(), authentication.accessToken(), accessExpiration);
    add(response, refreshCookieName(), authentication.refreshToken(), refreshExpiration);
  }

  public void clear(HttpServletResponse response) {
    add(response, accessCookieName(), "", Duration.ZERO);
    add(response, refreshCookieName(), "", Duration.ZERO);
  }

  public Optional<String> accessToken(HttpServletRequest request) {
    return cookie(request, accessCookieName());
  }

  public Optional<String> refreshToken(HttpServletRequest request) {
    return cookie(request, refreshCookieName());
  }

  public String requireRefreshToken(HttpServletRequest request) {
    return refreshToken(request)
        .orElseThrow(() -> new UnauthorizedException("Refresh cookie is missing"));
  }

  String accessCookieName() {
    return secure ? "__Host-" + ACCESS_COOKIE : ACCESS_COOKIE;
  }

  String refreshCookieName() {
    return secure ? "__Host-" + REFRESH_COOKIE : REFRESH_COOKIE;
  }

  private Optional<String> cookie(HttpServletRequest request, String name) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) return Optional.empty();
    return Arrays.stream(cookies)
        .filter(cookie -> name.equals(cookie.getName()))
        .map(Cookie::getValue)
        .filter(value -> !value.isBlank())
        .findFirst();
  }

  private void add(HttpServletResponse response, String name, String value, Duration maxAge) {
    ResponseCookie cookie =
        ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/")
            .maxAge(maxAge)
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }
}
