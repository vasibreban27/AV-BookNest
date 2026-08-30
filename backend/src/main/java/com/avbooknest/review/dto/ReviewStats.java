package com.avbooknest.review.dto;

public record ReviewStats(
    Long reviewCount, Double sellerRating, Double descriptionRating, Double conditionRating) {}
