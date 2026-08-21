package com.avbooknest.admin.service;

import com.avbooknest.admin.dto.AdminDtos.AuditResponse;
import com.avbooknest.admin.model.AdminAuditLog;
import com.avbooknest.admin.repository.AdminAuditLogRepository;
import com.avbooknest.auth.model.User;
import com.avbooknest.common.dto.PageResponse;
import com.avbooknest.common.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class AdminAuditService {
  private final AdminAuditLogRepository repository;

  public AdminAuditService(AdminAuditLogRepository repository) {
    this.repository = repository;
  }

  public void record(
      User administrator,
      String action,
      String targetType,
      Long targetId,
      String reason,
      String details) {
    repository.save(
        AdminAuditLog.create(
            administrator,
            action,
            targetType,
            targetId,
            trimToNull(reason),
            trimToNull(details),
            java.time.Instant.now()));
  }

  public PageResponse<AuditResponse> list(int page, int size) {
    if (page < 0) throw new BadRequestException("Page must be zero or greater");
    if (size < 1 || size > 100) {
      throw new BadRequestException("Page size must be between 1 and 100");
    }
    return PageResponse.from(
        repository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))),
        AuditResponse::from);
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
