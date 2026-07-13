package com.avbooknest.book.model;

import com.avbooknest.auth.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "books")
public class Book {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 255)
    private String author;

    @Column(length = 20)
    private String isbn;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING) @Column(name = "book_condition", nullable = false, length = 20)
    private BookCondition bookCondition;

    @Column(nullable = false, length = 100)
    private String language;

    @Column(length = 255)
    private String publisher;

    @Column(name = "published_year")
    private Short publishedYear;

    @Column(name = "cover_image_url", length = 2048)
    private String coverImageUrl;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private BookStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Book() { }
    private Book(Builder builder) {
        id = builder.id; title = builder.title; author = builder.author; isbn = builder.isbn; description = builder.description;
        price = builder.price; bookCondition = builder.bookCondition; language = builder.language; publisher = builder.publisher;
        publishedYear = builder.publishedYear; coverImageUrl = builder.coverImageUrl; seller = builder.seller; category = builder.category;
        status = builder.status; createdAt = builder.createdAt; updatedAt = builder.updatedAt;
    }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public BookCondition getBookCondition() { return bookCondition; }
    public String getLanguage() { return language; }
    public String getPublisher() { return publisher; }
    public Short getPublishedYear() { return publishedYear; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public User getSeller() { return seller; }
    public Category getCategory() { return category; }
    public BookStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void update(String newTitle, String newAuthor, String newIsbn, String newDescription, BigDecimal newPrice,
                       BookCondition newCondition, String newLanguage, String newPublisher, Short newPublishedYear,
                       String newCoverImageUrl, Category newCategory, BookStatus newStatus) {
        title = newTitle; author = newAuthor; isbn = newIsbn; description = newDescription; price = newPrice;
        bookCondition = newCondition; language = newLanguage; publisher = newPublisher; publishedYear = newPublishedYear;
        coverImageUrl = newCoverImageUrl; category = newCategory; status = newStatus; updatedAt = Instant.now();
    }
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Long id; private String title; private String author; private String isbn; private String description; private BigDecimal price;
        private BookCondition bookCondition; private String language; private String publisher; private Short publishedYear; private String coverImageUrl;
        private User seller; private Category category; private BookStatus status; private Instant createdAt; private Instant updatedAt;
        public Builder id(Long value) { id = value; return this; }
        public Builder title(String value) { title = value; return this; }
        public Builder author(String value) { author = value; return this; }
        public Builder isbn(String value) { isbn = value; return this; }
        public Builder description(String value) { description = value; return this; }
        public Builder price(BigDecimal value) { price = value; return this; }
        public Builder bookCondition(BookCondition value) { bookCondition = value; return this; }
        public Builder language(String value) { language = value; return this; }
        public Builder publisher(String value) { publisher = value; return this; }
        public Builder publishedYear(Short value) { publishedYear = value; return this; }
        public Builder coverImageUrl(String value) { coverImageUrl = value; return this; }
        public Builder seller(User value) { seller = value; return this; }
        public Builder category(Category value) { category = value; return this; }
        public Builder status(BookStatus value) { status = value; return this; }
        public Builder createdAt(Instant value) { createdAt = value; return this; }
        public Builder updatedAt(Instant value) { updatedAt = value; return this; }
        public Book build() { return new Book(this); }
    }
}
