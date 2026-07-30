ALTER TABLE users
    ADD COLUMN stripe_account_id VARCHAR(255),
    ADD COLUMN stripe_details_submitted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN stripe_charges_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN stripe_payouts_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT uk_users_stripe_account_id UNIQUE (stripe_account_id);

ALTER TABLE seller_orders
    DROP CONSTRAINT chk_seller_orders_status,
    ALTER COLUMN accept_by DROP NOT NULL,
    ADD CONSTRAINT chk_seller_orders_status CHECK (
        status IN ('PAYMENT_PENDING', 'AWAITING_SELLER', 'ACCEPTED', 'FULFILLED', 'CANCELLED')
    );

ALTER TABLE payments
    ADD COLUMN provider_charge_id VARCHAR(255),
    ADD COLUMN expires_at TIMESTAMPTZ,
    ADD COLUMN refunded_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD CONSTRAINT uk_payments_provider_charge_id UNIQUE (provider_charge_id),
    ADD CONSTRAINT chk_payments_refunded_amount CHECK (
        refunded_amount >= 0 AND refunded_amount <= amount
    );

UPDATE payments
SET expires_at = created_at + INTERVAL '30 minutes'
WHERE status IN ('PENDING', 'FAILED') AND expires_at IS NULL;

CREATE TABLE stripe_webhook_events (
    id BIGSERIAL PRIMARY KEY,
    stripe_event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_stripe_webhook_events_event_id UNIQUE (stripe_event_id)
);

CREATE INDEX idx_payments_pending_expiration
    ON payments (expires_at)
    WHERE status IN ('PENDING', 'FAILED');
