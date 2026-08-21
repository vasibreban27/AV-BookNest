package com.avbooknest.auth.security;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.avbooknest.auth.service.AuthCookieService;
import com.avbooknest.auth.service.JwtService;
import com.avbooknest.auth.service.UserSecurityService;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
  @Mock private JwtService jwtService;
  @Mock private UserSecurityService userSecurityService;
  @Mock private AuthCookieService authCookieService;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void disabledUserIsNotAuthenticatedByAnExistingAccessToken() throws Exception {
    JwtAuthenticationFilter filter =
        new JwtAuthenticationFilter(jwtService, userSecurityService, authCookieService);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/books/mine");
    when(authCookieService.accessToken(request)).thenReturn(Optional.of("access-token"));
    when(jwtService.extractUsername("access-token")).thenReturn("blocked@example.com");
    when(userSecurityService.loadUserByUsername("blocked@example.com"))
        .thenReturn(
            User.withUsername("blocked@example.com")
                .password("password")
                .authorities("ROLE_USER")
                .disabled(true)
                .build());

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }
}
