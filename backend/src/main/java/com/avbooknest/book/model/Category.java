package com.avbooknest.book.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "categories")
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Category() { }
    private Category(Builder builder) {
        id = builder.id; name = builder.name; slug = builder.slug; description = builder.description;
        createdAt = builder.createdAt; updatedAt = builder.updatedAt;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Long id; private String name; private String slug; private String description; private Instant createdAt; private Instant updatedAt;
        public Builder id(Long value) { id = value; return this; }
        public Builder name(String value) { name = value; return this; }
        public Builder slug(String value) { slug = value; return this; }
        public Builder description(String value) { description = value; return this; }
        public Builder createdAt(Instant value) { createdAt = value; return this; }
        public Builder updatedAt(Instant value) { updatedAt = value; return this; }
        public Category build() { return new Category(this); }
    }
}
