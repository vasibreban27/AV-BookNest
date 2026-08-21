package com.avbooknest.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;

class SpaCsrfProtectionTest {
  @Test
  void unsafeRequestWithoutCsrfTokenIsRejected() throws Exception {
    CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    CsrfFilter filter = new CsrfFilter(repository);
    filter.setRequestHandler(new SpaCsrfTokenRequestHandler());
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertEquals(403, response.getStatus());
    assertNull(chain.getRequest());
  }

  @Test
  void unsafeRequestWithMatchingCookieAndHeaderIsAccepted() throws Exception {
    CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    CsrfToken token = repository.generateToken(new MockHttpServletRequest());
    CsrfFilter filter = new CsrfFilter(repository);
    filter.setRequestHandler(new SpaCsrfTokenRequestHandler());
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
    request.setCookies(new Cookie("XSRF-TOKEN", token.getToken()));
    request.addHeader(token.getHeaderName(), token.getToken());
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertEquals(200, response.getStatus());
    assertNotNull(chain.getRequest());
  }
}
