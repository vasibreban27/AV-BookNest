package com.avbooknest.book.dto;

import com.avbooknest.book.model.BookCondition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record BookRequest(
    @NotBlank @Size(max = 255) String title,
    @NotBlank @Size(max = 255) String author,
    @Size(max = 20) String isbn,
    String description,
    @NotNull @DecimalMin(value = "0.00") BigDecimal price,
    @NotNull BookCondition bookCondition,
    @NotBlank @Size(max = 100) String language,
    @Size(max = 255) String publisher,
    @Min(1000) @Max(9999) Short publishedYear,
    @NotNull Long categoryId) {}
