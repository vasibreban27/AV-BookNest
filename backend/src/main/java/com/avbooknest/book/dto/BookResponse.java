package com.avbooknest.book.dto;

import com.avbooknest.book.model.Book;
import com.avbooknest.book.model.BookCondition;
import com.avbooknest.book.model.BookStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record BookResponse(
    Long id,
    String title,
    String author,
    String isbn,
    String description,
    BigDecimal price,
    BookCondition bookCondition,
    String language,
    String publisher,
    Short publishedYear,
    Integer weightGrams,
    Integer lengthMm,
    Integer widthMm,
    Integer heightMm,
    String coverImageUrl,
    Long sellerId,
    String sellerName,
    CategoryResponse category,
    BookStatus status,
    Instant createdAt,
    Instant updatedAt) {
  public static BookResponse from(Book book) {
    return new BookResponse(
        book.getId(),
        book.getTitle(),
        book.getAuthor(),
        book.getIsbn(),
        book.getDescription(),
        book.getPrice(),
        book.getBookCondition(),
        book.getLanguage(),
        book.getPublisher(),
        book.getPublishedYear(),
        book.getWeightGrams(),
        book.getLengthMm(),
        book.getWidthMm(),
        book.getHeightMm(),
        book.getCoverImageUrl(),
        book.getSeller().getId(),
        book.getSeller().getFirstName() + " " + book.getSeller().getLastName(),
        CategoryResponse.from(book.getCategory()),
        book.getStatus(),
        book.getCreatedAt(),
        book.getUpdatedAt());
  }
}
