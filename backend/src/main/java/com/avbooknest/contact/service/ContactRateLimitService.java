package com.avbooknest.contact.service;

import com.avbooknest.common.exception.RateLimitExceededException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ContactRateLimitService {
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
  private final int attempts;
  private final Duration duration;

  public ContactRateLimitService(
      @Value("${app.contact.rate-limit.attempts:5}") int attempts,
      @Value("${app.contact.rate-limit.window:1h}") Duration duration) {
    this.attempts = attempts;
    this.duration = duration;
  }

  public void check(String ip, String email) {
    consume("contact:ip:" + ip);
    consume("contact:email:" + email.trim().toLowerCase(Locale.ROOT));
  }

  @Scheduled(fixedDelayString = "${app.contact.rate-limit.cleanup-ms:600000}")
  public void removeExpiredWindows() {
    Instant now = Instant.now();
    windows.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
  }

  private void consume(String key) {
    Instant now = Instant.now();
    windows.compute(
        key,
        (ignored, current) -> {
          Window window =
              current == null || !current.expiresAt().isAfter(now)
                  ? new Window(0, now.plus(duration))
                  : current;
          if (window.attempts() >= attempts) {
            throw new RateLimitExceededException(
                Duration.between(now, window.expiresAt()).toSeconds());
          }
          return new Window(window.attempts() + 1, window.expiresAt());
        });
  }

  private record Window(int attempts, Instant expiresAt) {}
}
