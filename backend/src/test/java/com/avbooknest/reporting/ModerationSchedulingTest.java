package com.avbooknest.reporting;

import static org.junit.jupiter.api.Assertions.*;

import com.avbooknest.support.config.SupportSchedulingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class ModerationSchedulingTest {
  @Configuration(proxyBeanMethods = false)
  @EnableScheduling
  static class SchedulingEnabled {}

  @Test
  void expiryNeverBlocksPaymentDeadlinesOrSupportMailScheduler() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
        .withUserConfiguration(
            SchedulingEnabled.class,
            ModerationSchedulingConfiguration.class,
            SupportSchedulingConfiguration.class)
        .run(
            context -> {
              assertNull(context.getStartupFailure());
              var regular = context.getBean("taskScheduler", ThreadPoolTaskScheduler.class);
              var expiry =
                  context.getBean("moderationTaskScheduler", ThreadPoolTaskScheduler.class);
              var mail =
                  context.getBean("supportEmailTaskScheduler", ThreadPoolTaskScheduler.class);
              assertNotSame(regular, expiry);
              assertNotSame(mail, expiry);
              assertNotSame(regular, mail);
              assertEquals("moderation-expiry-", expiry.getThreadNamePrefix());
            });
  }
}
