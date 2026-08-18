package com.avbooknest.contact.controller;

import com.avbooknest.contact.dto.ContactRequest;
import com.avbooknest.contact.dto.ContactResponse;
import com.avbooknest.contact.service.ContactEmailService;
import com.avbooknest.contact.service.ContactRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contact")
public class ContactController {
  private final ContactEmailService contactEmailService;
  private final ContactRateLimitService rateLimitService;

  public ContactController(
      ContactEmailService contactEmailService, ContactRateLimitService rateLimitService) {
    this.contactEmailService = contactEmailService;
    this.rateLimitService = rateLimitService;
  }

  @PostMapping
  public ContactResponse submit(
      @Valid @RequestBody ContactRequest request, HttpServletRequest httpRequest) {
    rateLimitService.check(httpRequest.getRemoteAddr(), request.email());

    // A real visitor never sees or fills this honeypot. Return the normal response to avoid
    // teaching automated senders how the spam check works.
    if (request.website() == null || request.website().isBlank()) {
      contactEmailService.send(request);
    }

    return new ContactResponse(
        "Mesajul a fost trimis. Îți vom răspunde la adresa de email indicată");
  }
}
