ALTER TABLE users
    ADD COLUMN suspended_at TIMESTAMPTZ,
    ADD COLUMN suspended_by BIGINT,
    ADD COLUMN suspension_reason VARCHAR(500),
    ADD CONSTRAINT fk_users_suspended_by FOREIGN KEY (suspended_by) REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE categories
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE books
    ADD COLUMN moderation_status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    ADD COLUMN moderation_reason VARCHAR(40),
    ADD COLUMN moderation_note VARCHAR(500),
    ADD COLUMN moderated_by BIGINT,
    ADD COLUMN moderated_at TIMESTAMPTZ,
    ADD CONSTRAINT chk_books_moderation_status CHECK (moderation_status IN ('VISIBLE', 'HIDDEN')),
    ADD CONSTRAINT chk_books_moderation_reason CHECK (
        moderation_reason IS NULL OR moderation_reason IN (
            'POLICY_VIOLATION', 'SPAM', 'FRAUD_SUSPECTED', 'ACCOUNT_SUSPENDED', 'OTHER'
        )
    ),
    ADD CONSTRAINT fk_books_moderated_by FOREIGN KEY (moderated_by) REFERENCES users (id) ON DELETE SET NULL;

ALTER TABLE seller_orders
    ADD COLUMN issue_resolution VARCHAR(40),
    ADD COLUMN issue_resolution_note VARCHAR(500),
    ADD COLUMN issue_resolved_by BIGINT,
    ADD CONSTRAINT chk_seller_orders_issue_resolution CHECK (
        issue_resolution IS NULL OR issue_resolution IN ('REFUND_BUYER', 'RELEASE_SELLER_PAYOUT')
    ),
    ADD CONSTRAINT fk_seller_orders_issue_resolved_by FOREIGN KEY (issue_resolved_by) REFERENCES users (id) ON DELETE SET NULL;

CREATE TABLE admin_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    admin_user_id BIGINT NOT NULL,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT,
    reason VARCHAR(500),
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_admin_audit_user FOREIGN KEY (admin_user_id) REFERENCES users (id)
);

CREATE INDEX idx_users_suspended_at ON users (suspended_at) WHERE suspended_at IS NOT NULL;
CREATE INDEX idx_categories_active ON categories (active);
CREATE INDEX idx_books_moderation_status ON books (moderation_status);
CREATE INDEX idx_admin_audit_created_at ON admin_audit_logs (created_at DESC);
CREATE INDEX idx_admin_audit_target ON admin_audit_logs (target_type, target_id);
