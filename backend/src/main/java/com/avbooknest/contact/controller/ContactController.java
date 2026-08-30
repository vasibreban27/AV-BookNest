package com.avbooknest.contact.controller;

import com.avbooknest.contact.dto.ContactRequest;
import com.avbooknest.contact.dto.ContactResponse;
import com.avbooknest.contact.service.ContactRateLimitService;
import com.avbooknest.support.service.SupportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contact")
public class ContactController {
  private final SupportService support;
  private final ContactRateLimitService rateLimitService;

  public ContactController(SupportService support, ContactRateLimitService rateLimitService) {
    this.support = support;
    this.rateLimitService = rateLimitService;
  }

  @PostMapping
  public ContactResponse submit(
      @Valid @RequestBody ContactRequest request,
      HttpServletRequest httpRequest,
      Authentication auth) {
    rateLimitService.check(httpRequest.getRemoteAddr(), request.email());

    // A real visitor never sees or fills this honeypot. Return the normal response to avoid
    // teaching automated senders how the spam check works.
    if (request.website() == null || request.website().isBlank()) {
      return support.create(
          request,
          auth == null || auth instanceof AnonymousAuthenticationToken ? null : auth.getName());
    }

    return new ContactResponse(
        "Solicitarea a fost înregistrată. Echipa de suport îți va răspunde în cont sau pe email.",
        null,
        null);
  }
}
