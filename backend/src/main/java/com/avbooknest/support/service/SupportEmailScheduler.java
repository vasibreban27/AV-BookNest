package com.avbooknest.support.service;

import com.avbooknest.support.repository.SupportEmailRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SupportEmailScheduler {
  private static final Logger LOG = LoggerFactory.getLogger(SupportEmailScheduler.class);
  private final SupportEmailRepository emails;
  private final SupportEmailService service;

  public SupportEmailScheduler(SupportEmailRepository emails, SupportEmailService service) {
    this.emails = emails;
    this.service = service;
  }

  @Scheduled(
      scheduler = "supportEmailTaskScheduler",
      fixedDelayString = "${app.support.email.poll-ms:30000}",
      initialDelayString = "${app.support.email.poll-ms:30000}")
  public void deliverPending() {
    for (Long id : emails.findDueIds(Instant.now(), PageRequest.of(0, 20))) {
      try {
        service.deliver(id);
      } catch (RuntimeException exception) {
        LOG.warn(
            "Could not process support email {} ({})", id, exception.getClass().getSimpleName());
      }
    }
  }
}
