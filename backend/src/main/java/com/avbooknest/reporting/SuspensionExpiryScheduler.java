package com.avbooknest.reporting;

import com.avbooknest.auth.repository.UserRepository;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SuspensionExpiryScheduler {
  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(SuspensionExpiryScheduler.class);
  private final UserRepository users;
  private final SuspensionExpiryService expiry;

  public SuspensionExpiryScheduler(UserRepository users, SuspensionExpiryService expiry) {
    this.users = users;
    this.expiry = expiry;
  }

  @Scheduled(
      fixedDelayString = "${app.moderation.expiry-delay-ms:30000}",
      scheduler = "moderationTaskScheduler")
  public void expire() {
    for (Long id : users.findExpiredSuspensions(Instant.now(), PageRequest.of(0, 100))) {
      try {
        expiry.expire(id);
      } catch (RuntimeException e) {
        log.warn("Could not expire suspension for user {}", id);
      }
    }
  }
}
