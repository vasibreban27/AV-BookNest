package com.avbooknest.admin.repository;

import com.avbooknest.admin.model.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
  @Override
  @EntityGraph(attributePaths = "administrator")
  Page<AdminAuditLog> findAll(Pageable pageable);
}
