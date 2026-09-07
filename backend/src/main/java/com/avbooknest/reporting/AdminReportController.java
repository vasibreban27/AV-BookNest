package com.avbooknest.reporting;

import static com.avbooknest.reporting.ReportDtos.*;

import com.avbooknest.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {
  private final ReportingService service;

  public AdminReportController(ReportingService service) {
    this.service = service;
  }

  @GetMapping
  public PageResponse<Summary> queue(
      @RequestParam(required = false) Status status,
      @RequestParam(required = false) Long targetUserId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      Authentication auth) {
    return service.list(auth.getName(), true, status, targetUserId, page, size);
  }

  @GetMapping("/{id}")
  public Details details(@PathVariable long id, Authentication auth) {
    return service.details(auth.getName(), id, true);
  }

  @PostMapping("/{id}/claim")
  public Details claim(@PathVariable long id, Authentication auth) {
    return service.start(auth.getName(), id);
  }

  @PostMapping("/{id}/resolve")
  public Details resolve(
      @PathVariable long id, @Valid @RequestBody Resolve request, Authentication auth) {
    return service.resolve(auth.getName(), id, request);
  }

  @GetMapping("/users/{id}/history")
  public AccountHistory history(@PathVariable long id, Authentication auth) {
    return service.history(auth.getName(), id);
  }
}
