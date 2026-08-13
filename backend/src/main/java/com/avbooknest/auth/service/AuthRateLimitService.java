package com.avbooknest.auth.service;

import com.avbooknest.common.exception.RateLimitExceededException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AuthRateLimitService {
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
  private final int loginAccountAttempts;
  private final int loginIpAttempts;
  private final Duration loginWindow;
  private final int registerAttempts;
  private final Duration registerWindow;
  private final int emailAttempts;
  private final Duration emailWindow;

  public AuthRateLimitService(
      @Value("${app.auth.rate-limit.login-account-attempts:5}") int loginAccountAttempts,
      @Value("${app.auth.rate-limit.login-ip-attempts:20}") int loginIpAttempts,
      @Value("${app.auth.rate-limit.login-window:15m}") Duration loginWindow,
      @Value("${app.auth.rate-limit.register-attempts:5}") int registerAttempts,
      @Value("${app.auth.rate-limit.register-window:1h}") Duration registerWindow,
      @Value("${app.auth.rate-limit.email-attempts:3}") int emailAttempts,
      @Value("${app.auth.rate-limit.email-window:1h}") Duration emailWindow) {
    this.loginAccountAttempts = loginAccountAttempts;
    this.loginIpAttempts = loginIpAttempts;
    this.loginWindow = loginWindow;
    this.registerAttempts = registerAttempts;
    this.registerWindow = registerWindow;
    this.emailAttempts = emailAttempts;
    this.emailWindow = emailWindow;
  }

  public void checkLogin(String ip, String email) {
    consume("login:ip:" + ip, loginIpAttempts, loginWindow);
    consume("login:account:" + normalize(email), loginAccountAttempts, loginWindow);
  }

  public void loginSucceeded(String email) {
    windows.remove("login:account:" + normalize(email));
  }

  public void checkRegister(String ip) {
    consume("register:ip:" + ip, registerAttempts, registerWindow);
  }

  public void checkEmailAction(String ip, String email, String action) {
    consume(action + ":ip:" + ip, emailAttempts, emailWindow);
    consume(action + ":account:" + normalize(email), emailAttempts, emailWindow);
  }

  @Scheduled(fixedDelayString = "${app.auth.rate-limit.cleanup-ms:600000}")
  public void removeExpiredWindows() {
    Instant now = Instant.now();
    windows.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
  }

  private void consume(String key, int limit, Duration duration) {
    Instant now = Instant.now();
    windows.compute(
        key,
        (ignored, current) -> {
          Window window =
              current == null || !current.expiresAt().isAfter(now)
                  ? new Window(0, now.plus(duration))
                  : current;
          if (window.attempts() >= limit) {
            throw new RateLimitExceededException(
                Duration.between(now, window.expiresAt()).toSeconds());
          }
          return new Window(window.attempts() + 1, window.expiresAt());
        });
  }

  private String normalize(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private record Window(int attempts, Instant expiresAt) {}
}
