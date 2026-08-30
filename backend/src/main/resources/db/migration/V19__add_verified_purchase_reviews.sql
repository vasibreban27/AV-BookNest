-- Keep legacy feedback, but never present it as a verified purchase.
ALTER TABLE reviews
    DROP CONSTRAINT uk_reviews_book_reviewer,
    DROP CONSTRAINT fk_reviews_book,
    ALTER COLUMN book_id DROP NOT NULL,
    ADD COLUMN order_item_id BIGINT REFERENCES order_items(id),
    ADD COLUMN seller_id BIGINT REFERENCES users(id),
    ADD COLUMN book_title VARCHAR(255),
    ADD COLUMN description_rating SMALLINT,
    ADD COLUMN condition_rating SMALLINT,
    ADD COLUMN moderation_status VARCHAR(20) NOT NULL DEFAULT 'HIDDEN',
    ADD COLUMN moderation_reason VARCHAR(500),
    ADD COLUMN moderated_by BIGINT REFERENCES users(id),
    ADD COLUMN moderated_at TIMESTAMPTZ;

UPDATE reviews r SET seller_id = b.seller_id, book_title = b.title,
    moderation_reason = 'Legacy review without a verified purchase'
FROM books b WHERE b.id = r.book_id;

ALTER TABLE reviews
    ALTER COLUMN seller_id SET NOT NULL,
    ALTER COLUMN book_title SET NOT NULL,
    ADD CONSTRAINT fk_reviews_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE SET NULL,
    ADD CONSTRAINT uk_reviews_order_item UNIQUE (order_item_id),
    ADD CONSTRAINT chk_reviews_description_rating CHECK (description_rating BETWEEN 1 AND 5),
    ADD CONSTRAINT chk_reviews_condition_rating CHECK (condition_rating BETWEEN 1 AND 5),
    ADD CONSTRAINT chk_reviews_moderation CHECK (moderation_status IN ('VISIBLE', 'HIDDEN')),
    ADD CONSTRAINT chk_reviews_verified_ratings CHECK (
        order_item_id IS NULL OR (description_rating IS NOT NULL AND condition_rating IS NOT NULL)),
    ADD CONSTRAINT chk_reviews_visible_verified CHECK (
        moderation_status <> 'VISIBLE' OR order_item_id IS NOT NULL),
    ADD CONSTRAINT chk_reviews_hidden_reason CHECK (
        moderation_status <> 'HIDDEN' OR length(trim(moderation_reason)) > 0 AND moderation_reason IS NOT NULL);

CREATE INDEX idx_reviews_seller_visible ON reviews(seller_id, created_at DESC, id DESC)
    WHERE moderation_status = 'VISIBLE' AND order_item_id IS NOT NULL;
CREATE INDEX idx_reviews_moderation ON reviews(moderation_status, created_at DESC, id DESC);
