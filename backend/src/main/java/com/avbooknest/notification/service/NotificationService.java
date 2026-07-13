package com.avbooknest.notification.service;

import com.avbooknest.auth.model.User;
import com.avbooknest.auth.repository.UserRepository;
import com.avbooknest.common.exception.NotFoundException;
import com.avbooknest.notification.dto.NotificationResponse;
import com.avbooknest.notification.model.Notification;
import com.avbooknest.notification.model.NotificationType;
import com.avbooknest.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {
  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;

  public NotificationService(
      NotificationRepository notificationRepository, UserRepository userRepository) {
    this.notificationRepository = notificationRepository;
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public List<NotificationResponse> list(String email, boolean unreadOnly) {
    Long userId = user(email).getId();
    return (unreadOnly
            ? notificationRepository.findAllByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId)
            : notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId))
        .stream().map(NotificationResponse::from).toList();
  }

  public Notification create(User user, NotificationType type, String title, String message) {
    return notificationRepository.save(
        Notification.builder()
            .user(user)
            .type(type)
            .title(title)
            .message(message)
            .createdAt(Instant.now())
            .build());
  }

  public NotificationResponse markRead(Long notificationId, String email) {
    Notification notification =
        notificationRepository
            .findByIdAndUserId(notificationId, user(email).getId())
            .orElseThrow(() -> new NotFoundException("Notification not found"));
    notification.markRead();
    return NotificationResponse.from(notification);
  }

  public void markAllRead(String email) {
    notificationRepository
        .findAllByUserIdAndReadAtIsNullOrderByCreatedAtDesc(user(email).getId())
        .forEach(Notification::markRead);
  }

  private User user(String email) {
    return userRepository
        .findByEmail(email)
        .orElseThrow(() -> new NotFoundException("User not found"));
  }
}
