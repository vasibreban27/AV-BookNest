package com.avbooknest.reporting;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration(proxyBeanMethods = false)
public class ModerationSchedulingConfiguration {
  @Bean(defaultCandidate = false)
  public ThreadPoolTaskScheduler moderationTaskScheduler() {
    var scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(1);
    scheduler.setThreadNamePrefix("moderation-expiry-");
    return scheduler;
  }
}
