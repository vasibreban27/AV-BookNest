package com.avbooknest.contact.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.avbooknest.common.exception.RateLimitExceededException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ContactRateLimitServiceTest {
  @Test
  void contactMessagesAreLimitedByIpAndNormalizedEmail() {
    ContactRateLimitService service = new ContactRateLimitService(2, Duration.ofHours(1));

    service.check("127.0.0.1", "ANA@example.com");
    service.check("127.0.0.2", "ana@example.com");

    assertThrows(
        RateLimitExceededException.class, () -> service.check("127.0.0.3", " ana@example.com "));
    assertDoesNotThrow(() -> service.check("127.0.0.4", "other@example.com"));
  }
}
