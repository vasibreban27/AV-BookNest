ALTER TABLE books
    ADD CONSTRAINT chk_books_hidden_reason CHECK (
        moderation_status <> 'HIDDEN' OR moderation_reason IS NOT NULL
    );
