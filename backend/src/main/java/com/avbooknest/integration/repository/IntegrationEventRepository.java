package com.avbooknest.integration.repository;

import com.avbooknest.integration.model.IntegrationEvent;
import com.avbooknest.integration.model.IntegrationEventStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface IntegrationEventRepository extends JpaRepository<IntegrationEvent, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<IntegrationEvent>
      findFirstByEventTypeAndStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
          String eventType, IntegrationEventStatus status, Instant now);
}
