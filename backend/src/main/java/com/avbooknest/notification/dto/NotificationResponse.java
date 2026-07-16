package com.avbooknest.notification.dto;

import com.avbooknest.notification.model.Notification;
import com.avbooknest.notification.model.NotificationType;
import java.time.Instant;

public record NotificationResponse(
    Long id,
    NotificationType type,
    String title,
    String message,
    Instant readAt,
    Instant createdAt) {
  public static NotificationResponse from(Notification notification) {
    return new NotificationResponse(
        notification.getId(),
        notification.getType(),
        notification.getTitle(),
        notification.getMessage(),
        notification.getReadAt(),
        notification.getCreatedAt());
  }
}
