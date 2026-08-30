package com.avbooknest.support;

import static org.junit.jupiter.api.Assertions.*;

import com.avbooknest.support.config.SupportSchedulingConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class SupportSchedulingTest {
  @Configuration(proxyBeanMethods = false)
  @EnableScheduling
  static class SchedulingEnabled {}

  @Test
  void slowSmtpDoesNotShareTheOrderAndPaymentScheduler() {
    new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
        .withUserConfiguration(SchedulingEnabled.class, SupportSchedulingConfiguration.class)
        .run(
            context -> {
              assertNull(context.getStartupFailure());
              var support =
                  context.getBean("supportEmailTaskScheduler", ThreadPoolTaskScheduler.class);
              var regular = context.getBean("taskScheduler", ThreadPoolTaskScheduler.class);
              assertNotSame(regular, support);
              assertEquals("support-email-", support.getThreadNamePrefix());
            });
  }
}
