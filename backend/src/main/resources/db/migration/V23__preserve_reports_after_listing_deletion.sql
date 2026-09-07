ALTER TABLE content_reports ADD COLUMN target_id BIGINT;
UPDATE content_reports SET target_id = COALESCE(book_id, target_user_id);
ALTER TABLE content_reports ALTER COLUMN target_id SET NOT NULL;

DROP INDEX idx_reports_open_duplicate;
CREATE UNIQUE INDEX idx_reports_open_duplicate
    ON content_reports(reporter_id, target_type, target_id)
    WHERE status IN ('NEW','IN_PROGRESS');

DO $$
DECLARE constraint_name TEXT;
BEGIN
    SELECT con.conname INTO constraint_name
    FROM pg_constraint con
    WHERE con.conrelid = 'content_reports'::regclass
      AND con.contype = 'c'
      AND pg_get_constraintdef(con.oid) LIKE '%target_type%book_id%';
    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE content_reports DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

ALTER TABLE content_reports DROP CONSTRAINT content_reports_book_id_fkey;
ALTER TABLE content_reports
    ADD CONSTRAINT fk_content_reports_book
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE SET NULL;
ALTER TABLE content_reports
    ADD CONSTRAINT chk_content_reports_target_shape
    CHECK (target_type = 'BOOK' OR book_id IS NULL);
