package com.avbooknest.support.controller;

import com.avbooknest.common.dto.PageResponse;
import com.avbooknest.support.dto.SupportDtos.*;
import com.avbooknest.support.dto.SupportRequests.*;
import com.avbooknest.support.model.SupportStatus;
import com.avbooknest.support.service.SupportService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/support/tickets")
public class SupportController {
  private final SupportService support;

  public SupportController(SupportService support) {
    this.support = support;
  }

  @GetMapping
  public PageResponse<Ticket> list(
      Authentication auth,
      @RequestParam(required = false) SupportStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return support.mine(auth.getName(), status, page, size);
  }

  @GetMapping("/{id}")
  public Ticket get(@PathVariable Long id, Authentication auth) {
    return support.getMine(id, auth.getName());
  }

  @GetMapping("/{id}/messages")
  public PageResponse<Message> messages(
      @PathVariable Long id,
      Authentication auth,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "30") int size) {
    return support.myMessages(id, auth.getName(), page, size);
  }

  @PostMapping("/{id}/messages")
  public Message reply(
      @PathVariable Long id, @Valid @RequestBody Reply request, Authentication auth) {
    return support.reply(id, auth.getName(), request);
  }
}
