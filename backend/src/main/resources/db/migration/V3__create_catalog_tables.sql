CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_categories_name UNIQUE (name),
    CONSTRAINT uk_categories_slug UNIQUE (slug)
);

CREATE TABLE books (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    isbn VARCHAR(20),
    description TEXT,
    price NUMERIC(12, 2) NOT NULL,
    book_condition VARCHAR(20) NOT NULL,
    language VARCHAR(100) NOT NULL,
    publisher VARCHAR(255),
    published_year SMALLINT,
    cover_image_url VARCHAR(2048),
    seller_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_books_price CHECK (price >= 0),
    CONSTRAINT chk_books_condition CHECK (book_condition IN ('NEW', 'LIKE_NEW', 'VERY_GOOD', 'GOOD', 'ACCEPTABLE')),
    CONSTRAINT chk_books_status CHECK (status IN ('DRAFT', 'AVAILABLE', 'RESERVED', 'SOLD', 'ARCHIVED')),
    CONSTRAINT chk_books_published_year CHECK (published_year IS NULL OR published_year BETWEEN 1000 AND 9999),
    CONSTRAINT fk_books_seller FOREIGN KEY (seller_id) REFERENCES users (id),
    CONSTRAINT fk_books_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE favorites (
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_favorites PRIMARY KEY (user_id, book_id),
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_book FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE
);

CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    rating SMALLINT NOT NULL,
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT uk_reviews_book_reviewer UNIQUE (book_id, reviewer_id),
    CONSTRAINT fk_reviews_book FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_books_seller_id ON books (seller_id);
CREATE INDEX idx_books_category_id ON books (category_id);
CREATE INDEX idx_books_status ON books (status);
CREATE INDEX idx_books_price ON books (price);
CREATE INDEX idx_books_title ON books (title);
CREATE INDEX idx_books_author ON books (author);
CREATE INDEX idx_favorites_book_id ON favorites (book_id);
CREATE INDEX idx_reviews_book_id ON reviews (book_id);
