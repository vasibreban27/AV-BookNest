package com.avbooknest.notification.repository;

import com.avbooknest.notification.model.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId);

  List<Notification> findAllByUserIdAndReadAtIsNullOrderByCreatedAtDesc(Long userId);

  Optional<Notification> findByIdAndUserId(Long id, Long userId);
}
