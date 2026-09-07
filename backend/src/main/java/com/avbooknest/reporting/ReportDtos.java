package com.avbooknest.reporting;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class ReportDtos {
  private ReportDtos() {}

  public enum Target {
    BOOK,
    USER
  }

  public enum Reason {
    SPAM,
    FRAUD,
    PROHIBITED_CONTENT,
    HARASSMENT,
    MISLEADING,
    OTHER
  }

  public enum Status {
    NEW,
    IN_PROGRESS,
    RESOLVED,
    DISMISSED
  }

  public enum Decision {
    DISMISS,
    WARN,
    HIDE_BOOK,
    SUSPEND
  }

  public record Create(
      @NotNull Target targetType,
      @NotNull @Positive Long targetId,
      @NotNull Reason reason,
      @Size(max = 2000) String description) {}

  public record Resolve(
      @NotNull Decision decision,
      @NotBlank @Size(max = 500) String note,
      @Min(1) @Max(90) Integer suspensionDays) {}

  public record Summary(
      long id,
      long reporterId,
      Target targetType,
      long targetUserId,
      Long bookId,
      String targetLabel,
      Reason reason,
      String description,
      Status status,
      Long assignedToId,
      Decision decision,
      String decisionNote,
      Instant suspendedUntil,
      Instant createdAt,
      Instant updatedAt) {}

  public record EvidenceInfo(long id, String contentType) {}

  public record Evidence(String contentType, byte[] content) {}

  public record Event(long id, long actorId, String action, String note, Instant createdAt) {}

  public record Details(Summary report, List<EvidenceInfo> evidence, List<Event> events) {}

  public record AccountHistory(
      boolean enabled,
      Instant suspendedUntil,
      String suspensionReason,
      List<HistoryEntry> entries) {}

  public record HistoryEntry(String action, String reason, Instant createdAt) {}
}
