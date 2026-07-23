package com.avbooknest.integration.repository;

import com.avbooknest.integration.model.IntegrationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationEventRepository extends JpaRepository<IntegrationEvent, Long> {}
