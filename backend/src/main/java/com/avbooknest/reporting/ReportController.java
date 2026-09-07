package com.avbooknest.reporting;

import static com.avbooknest.reporting.ReportDtos.*;

import com.avbooknest.common.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
  private final ReportingService service;

  public ReportController(ReportingService service) {
    this.service = service;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public Summary create(
      @Valid @RequestPart("report") Create request,
      @RequestPart(value = "evidence", required = false) List<MultipartFile> evidence,
      Authentication auth) {
    return service.create(auth.getName(), request, evidence);
  }

  @GetMapping
  public PageResponse<Summary> mine(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      Authentication auth) {
    return service.list(auth.getName(), false, null, null, page, size);
  }

  @GetMapping("/{id}")
  public Details get(@PathVariable long id, Authentication auth) {
    return service.details(auth.getName(), id, false);
  }

  @GetMapping("/{id}/evidence/{evidenceId}")
  public ResponseEntity<byte[]> evidence(
      @PathVariable long id, @PathVariable long evidenceId, Authentication auth) {
    Evidence evidence = service.evidence(auth.getName(), id, evidenceId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(evidence.contentType()))
        .cacheControl(CacheControl.noStore())
        .header("X-Content-Type-Options", "nosniff")
        .header(
            "Content-Disposition",
            "inline; filename=\"evidence-"
                + evidenceId
                + (evidence.contentType().equals("image/png") ? ".png" : ".jpg")
                + "\"")
        .body(evidence.content());
  }
}
