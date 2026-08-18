-- Existing BookNest accounts predate email verification. Keep those accounts usable;
-- registrations created after this migration start unverified in the application.
UPDATE users SET email_verified = TRUE WHERE email_verified = FALSE;

CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_active
    ON email_verification_tokens (user_id, expires_at) WHERE used_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_active
    ON password_reset_tokens (user_id, expires_at) WHERE used_at IS NULL;
