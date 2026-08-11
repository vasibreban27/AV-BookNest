package com.avbooknest.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrderIssueRequest(@NotBlank @Size(max = 500) String reason) {}
