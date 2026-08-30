package com.avbooknest.support.controller;

import com.avbooknest.common.dto.PageResponse;
import com.avbooknest.support.dto.SupportDtos.*;
import com.avbooknest.support.dto.SupportRequests.*;
import com.avbooknest.support.model.SupportStatus;
import com.avbooknest.support.service.SupportService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/support")
public class AdminSupportController {
  private final SupportService support;

  public AdminSupportController(SupportService support) {
    this.support = support;
  }

  @GetMapping("/tickets")
  public PageResponse<AdminTicket> list(
      Authentication auth,
      @RequestParam(required = false) SupportStatus status,
      @RequestParam(required = false) Long assignedToId,
      @RequestParam(defaultValue = "false") boolean unassigned,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return support.adminList(auth.getName(), status, assignedToId, unassigned, q, page, size);
  }

  @GetMapping("/administrators")
  public List<Administrator> administrators(Authentication auth) {
    return support.administrators(auth.getName());
  }

  @GetMapping("/tickets/{id}")
  public AdminTicket get(@PathVariable Long id, Authentication auth) {
    return support.adminGet(id, auth.getName());
  }

  @GetMapping("/tickets/{id}/messages")
  public PageResponse<AdminMessage> messages(
      @PathVariable Long id,
      Authentication auth,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "30") int size) {
    return support.adminMessages(id, auth.getName(), page, size);
  }

  @PostMapping("/tickets/{id}/messages")
  public Message reply(
      @PathVariable Long id, @Valid @RequestBody Reply request, Authentication auth) {
    return support.adminReply(id, auth.getName(), request);
  }

  @PatchMapping("/tickets/{id}/status")
  public AdminTicket status(
      @PathVariable Long id, @Valid @RequestBody StatusChange request, Authentication auth) {
    return support.changeStatus(id, auth.getName(), request);
  }

  @PatchMapping("/tickets/{id}/assignment")
  public AdminTicket assign(
      @PathVariable Long id, @Valid @RequestBody Assignment request, Authentication auth) {
    return support.assign(id, auth.getName(), request);
  }

  @PostMapping("/tickets/{id}/messages/{messageId}/retry-email")
  public void retry(@PathVariable Long id, @PathVariable Long messageId, Authentication auth) {
    support.retryEmail(id, messageId, auth.getName());
  }
}
