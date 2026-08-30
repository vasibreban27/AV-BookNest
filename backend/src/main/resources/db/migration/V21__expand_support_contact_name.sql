-- V20 may already be applied. Keep its checksum unchanged and widen the column here.
-- A user's full name can contain 100 + 1 + 100 characters.
ALTER TABLE support_tickets
    ALTER COLUMN contact_name TYPE VARCHAR(201);
