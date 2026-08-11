ALTER TABLE notifications
    ADD COLUMN action_url VARCHAR(500);

ALTER TABLE seller_orders
    ADD COLUMN issue_status VARCHAR(20) NOT NULL DEFAULT 'NONE',
    ADD COLUMN issue_reason VARCHAR(500),
    ADD COLUMN issue_opened_at TIMESTAMPTZ,
    ADD COLUMN issue_resolved_at TIMESTAMPTZ,
    ADD CONSTRAINT chk_seller_orders_issue_status CHECK (
        issue_status IN ('NONE', 'OPEN', 'RESOLVED')
    );

CREATE INDEX idx_seller_orders_dropoff_by ON seller_orders (dropoff_by)
    WHERE status = 'ACCEPTED';

CREATE INDEX idx_seller_orders_open_issue ON seller_orders (issue_opened_at)
    WHERE issue_status = 'OPEN';
