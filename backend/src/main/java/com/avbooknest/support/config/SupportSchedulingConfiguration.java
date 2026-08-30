package com.avbooknest.support.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration(proxyBeanMethods = false)
public class SupportSchedulingConfiguration {
  // SMTP waits must not delay payment expiration, shipment processing or order deadlines.
  // Not a default candidate: Spring Boot still creates the application's normal scheduler.
  @Bean(name = "supportEmailTaskScheduler", defaultCandidate = false)
  public ThreadPoolTaskScheduler supportEmailTaskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(1);
    scheduler.setThreadNamePrefix("support-email-");
    scheduler.setWaitForTasksToCompleteOnShutdown(true);
    scheduler.setAwaitTerminationSeconds(15);
    return scheduler;
  }
}
