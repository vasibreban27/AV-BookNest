package com.avbooknest.review.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
    @JsonDeserialize(using = ReviewRatingDeserializer.class) @NotNull @Min(1) @Max(5)
        Integer sellerRating,
    @JsonDeserialize(using = ReviewRatingDeserializer.class) @NotNull @Min(1) @Max(5)
        Integer descriptionRating,
    @JsonDeserialize(using = ReviewRatingDeserializer.class) @NotNull @Min(1) @Max(5)
        Integer conditionRating,
    @Size(max = 2000) String comment) {}
