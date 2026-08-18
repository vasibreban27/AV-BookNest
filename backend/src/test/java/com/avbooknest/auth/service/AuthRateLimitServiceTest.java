package com.avbooknest.auth.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.avbooknest.common.exception.RateLimitExceededException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class AuthRateLimitServiceTest {
  @Test
  void loginIsLimitedPerNormalizedAccountAndResetAfterSuccess() {
    AuthRateLimitService service =
        new AuthRateLimitService(
            2, 20, Duration.ofMinutes(15), 5, Duration.ofHours(1), 3, Duration.ofHours(1));

    service.checkLogin("127.0.0.1", "ANA@example.com");
    service.checkLogin("127.0.0.2", "ana@example.com");
    assertThrows(
        RateLimitExceededException.class,
        () -> service.checkLogin("127.0.0.3", " ana@example.com "));

    service.loginSucceeded("ANA@example.com");
    assertDoesNotThrow(() -> service.checkLogin("127.0.0.3", "ana@example.com"));
  }

  @Test
  void registrationIsLimitedPerIp() {
    AuthRateLimitService service =
        new AuthRateLimitService(
            5, 20, Duration.ofMinutes(15), 2, Duration.ofHours(1), 3, Duration.ofHours(1));

    service.checkRegister("127.0.0.1");
    service.checkRegister("127.0.0.1");
    assertThrows(RateLimitExceededException.class, () -> service.checkRegister("127.0.0.1"));
  }
}
