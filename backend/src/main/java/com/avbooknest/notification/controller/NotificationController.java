package com.avbooknest.notification.controller;

import com.avbooknest.notification.dto.NotificationResponse;
import com.avbooknest.notification.service.NotificationService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping
  public List<NotificationResponse> list(
      @RequestParam(defaultValue = "false") boolean unreadOnly, Authentication auth) {
    return notificationService.list(auth.getName(), unreadOnly);
  }

  @PatchMapping("/{notificationId}/read")
  public NotificationResponse markRead(@PathVariable Long notificationId, Authentication auth) {
    return notificationService.markRead(notificationId, auth.getName());
  }

  @PatchMapping("/read-all")
  public ResponseEntity<Void> markAllRead(Authentication auth) {
    notificationService.markAllRead(auth.getName());
    return ResponseEntity.noContent().build();
  }
}
