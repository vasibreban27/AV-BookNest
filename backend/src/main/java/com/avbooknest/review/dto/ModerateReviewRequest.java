package com.avbooknest.review.dto;

import com.avbooknest.review.model.ReviewModerationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModerateReviewRequest(
    @NotNull ReviewModerationStatus status, @NotBlank @Size(max = 500) String reason) {}
