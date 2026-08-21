package com.avbooknest.auth.security;

import com.avbooknest.auth.service.AuthCookieService;
import com.avbooknest.auth.service.JwtService;
import com.avbooknest.auth.service.UserSecurityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserSecurityService userSecurityService;
  private final AuthCookieService authCookieService;

  public JwtAuthenticationFilter(
      JwtService jwtService,
      UserSecurityService userSecurityService,
      AuthCookieService authCookieService) {
    this.jwtService = jwtService;
    this.userSecurityService = userSecurityService;
    this.authCookieService = authCookieService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String token = resolveToken(request);
    if (token == null) {
      filterChain.doFilter(request, response);
      return;
    }

    try {
      String email = jwtService.extractUsername(token);
      if (SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails userDetails = userSecurityService.loadUserByUsername(email);
        if (jwtService.isValidAccessToken(token, userDetails)) {
          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(
                  userDetails, null, userDetails.getAuthorities());
          authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
      }
    } catch (RuntimeException ignored) {
      // Invalid credentials are handled by Spring Security at protected endpoints.
    }

    filterChain.doFilter(request, response);
  }

  private String resolveToken(HttpServletRequest request) {
    return authCookieService.accessToken(request).orElse(null);
  }
}
