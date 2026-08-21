package com.avbooknest.admin.dto;

import com.avbooknest.book.model.BookModerationReason;
import com.avbooknest.order.model.IssueResolution;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AdminRequests {
  private AdminRequests() {}

  public record ReasonRequest(@NotBlank @Size(max = 500) String reason) {}

  public record BookModerationRequest(
      @NotNull BookModerationReason reason, @NotBlank @Size(max = 500) String note) {}

  public record CategoryUpdateRequest(
      @NotBlank @Size(max = 100) String name, @Size(max = 500) String description) {}

  public record IssueResolutionRequest(
      @NotNull IssueResolution resolution, @NotBlank @Size(max = 500) String note) {}
}
