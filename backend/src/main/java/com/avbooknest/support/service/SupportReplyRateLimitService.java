package com.avbooknest.support.service;

import com.avbooknest.contact.service.ContactRateLimitService;
import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class SupportReplyRateLimitService {
  private final ContactRateLimitService limiter =
      new ContactRateLimitService(30, Duration.ofHours(1));

  public void check(Long userId) {
    limiter.removeExpiredWindows();
    limiter.check("support-user-" + userId, "support-user-" + userId);
  }
}
