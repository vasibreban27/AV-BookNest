package com.avbooknest.support.dto;

import com.avbooknest.support.model.SupportStatus;
import jakarta.validation.constraints.*;

public final class SupportRequests {
  private SupportRequests() {}

  public record Reply(@NotBlank @Size(max = 4000) String body) {}

  public record StatusChange(
      @NotNull SupportStatus status, @NotBlank @Size(max = 500) String reason) {}

  public record Assignment(
      @Positive Long administratorId, @NotBlank @Size(max = 500) String reason) {}
}
