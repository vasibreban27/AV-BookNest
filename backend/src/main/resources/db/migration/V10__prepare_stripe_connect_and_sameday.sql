-- V10 is intentionally the first marketplace-fulfilment migration.
-- It may replace the previous, unapplied V10 during development.

ALTER TABLE books
    ADD COLUMN weight_grams INTEGER NOT NULL DEFAULT 500,
    ADD COLUMN length_mm INTEGER NOT NULL DEFAULT 210,
    ADD COLUMN width_mm INTEGER NOT NULL DEFAULT 140,
    ADD COLUMN height_mm INTEGER NOT NULL DEFAULT 30,
    ADD CONSTRAINT chk_books_shipping_measurements CHECK (
        weight_grams BETWEEN 1 AND 19850
        AND length_mm BETWEEN 10 AND 450
        AND width_mm BETWEEN 10 AND 425
        AND height_mm BETWEEN 1 AND 370
    );

ALTER TABLE orders
    ADD COLUMN recipient_name VARCHAR(200),
    ADD COLUMN recipient_email VARCHAR(255),
    ADD COLUMN recipient_phone VARCHAR(30);

UPDATE orders o
SET recipient_name = CONCAT_WS(' ', u.first_name, u.last_name),
    recipient_email = u.email,
    recipient_phone = 'NOT-PROVIDED'
FROM users u
WHERE u.id = o.buyer_id;

ALTER TABLE orders
    ALTER COLUMN recipient_name SET NOT NULL,
    ALTER COLUMN recipient_email SET NOT NULL,
    ALTER COLUMN recipient_phone SET NOT NULL;

CREATE TABLE seller_orders (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AWAITING_SELLER',
    item_subtotal NUMERIC(12, 2) NOT NULL,
    commission_rate NUMERIC(5, 2) NOT NULL DEFAULT 5.00,
    commission_amount NUMERIC(12, 2) NOT NULL,
    seller_proceeds NUMERIC(12, 2) NOT NULL,
    shipping_cost NUMERIC(12, 2) NOT NULL DEFAULT 0,
    accept_by TIMESTAMPTZ NOT NULL,
    dropoff_by TIMESTAMPTZ,
    accepted_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    fulfilled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_seller_orders_order_seller UNIQUE (order_id, seller_id),
    CONSTRAINT chk_seller_orders_status CHECK (
        status IN ('AWAITING_SELLER', 'ACCEPTED', 'FULFILLED', 'CANCELLED')
    ),
    CONSTRAINT chk_seller_orders_amounts CHECK (
        item_subtotal >= 0
        AND commission_rate >= 0
        AND commission_rate <= 100
        AND commission_amount >= 0
        AND seller_proceeds >= 0
        AND shipping_cost >= 0
    ),
    CONSTRAINT fk_seller_orders_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_seller_orders_seller FOREIGN KEY (seller_id) REFERENCES users (id)
);

INSERT INTO seller_orders (
    order_id,
    seller_id,
    status,
    item_subtotal,
    commission_rate,
    commission_amount,
    seller_proceeds,
    shipping_cost,
    accept_by,
    dropoff_by,
    accepted_at,
    cancelled_at,
    fulfilled_at,
    created_at,
    updated_at
)
SELECT
    oi.order_id,
    oi.seller_id,
    CASE
        WHEN s.status = 'CANCELLED' THEN 'CANCELLED'
        WHEN s.status = 'DELIVERED' THEN 'FULFILLED'
        WHEN s.status IN ('AWB_CREATED', 'IN_TRANSIT') THEN 'ACCEPTED'
        ELSE 'AWAITING_SELLER'
    END,
    SUM(oi.unit_price * oi.quantity),
    5.00,
    ROUND(SUM(oi.unit_price * oi.quantity) * 0.05, 2),
    SUM(oi.unit_price * oi.quantity) - ROUND(SUM(oi.unit_price * oi.quantity) * 0.05, 2),
    0,
    o.placed_at + INTERVAL '24 hours',
    CASE
        WHEN s.status IN ('AWB_CREATED', 'IN_TRANSIT', 'DELIVERED')
            THEN COALESCE(s.updated_at, o.placed_at) + INTERVAL '48 hours'
        ELSE NULL
    END,
    CASE
        WHEN s.status IN ('AWB_CREATED', 'IN_TRANSIT', 'DELIVERED')
            THEN COALESCE(s.updated_at, o.placed_at)
        ELSE NULL
    END,
    CASE WHEN s.status = 'CANCELLED' THEN COALESCE(s.updated_at, o.updated_at) ELSE NULL END,
    CASE WHEN s.status = 'DELIVERED' THEN COALESCE(s.updated_at, o.updated_at) ELSE NULL END,
    o.placed_at,
    o.updated_at
FROM order_items oi
JOIN orders o ON o.id = oi.order_id
LEFT JOIN shipments s ON s.order_id = oi.order_id AND s.seller_id = oi.seller_id
GROUP BY oi.order_id, oi.seller_id, o.placed_at, o.updated_at, s.status, s.updated_at;

ALTER TABLE order_items ADD COLUMN seller_order_id BIGINT;

UPDATE order_items oi
SET seller_order_id = so.id
FROM seller_orders so
WHERE so.order_id = oi.order_id AND so.seller_id = oi.seller_id;

ALTER TABLE order_items
    ALTER COLUMN seller_order_id SET NOT NULL,
    ADD CONSTRAINT fk_order_items_seller_order
        FOREIGN KEY (seller_order_id) REFERENCES seller_orders (id) ON DELETE CASCADE;

ALTER TABLE shipments
    ADD COLUMN seller_order_id BIGINT,
    ADD COLUMN easybox_address VARCHAR(255),
    ADD COLUMN easybox_city VARCHAR(100),
    ADD COLUMN easybox_county VARCHAR(100),
    ADD COLUMN easybox_postal_code VARCHAR(20),
    ADD COLUMN package_size VARCHAR(10),
    ADD COLUMN package_weight_grams INTEGER,
    ADD COLUMN package_length_mm INTEGER,
    ADD COLUMN package_width_mm INTEGER,
    ADD COLUMN package_height_mm INTEGER,
    ADD COLUMN sameday_parcel_id VARCHAR(100),
    ADD COLUMN provider_status VARCHAR(100),
    ADD COLUMN status_updated_at TIMESTAMPTZ,
    ADD COLUMN label_url VARCHAR(2048);

ALTER TABLE shipments DROP CONSTRAINT chk_shipments_status;

UPDATE shipments s
SET seller_order_id = so.id,
    status = CASE WHEN s.status = 'PREPARING' THEN 'AWB_PENDING' ELSE s.status END,
    package_size = CASE
        WHEN (SELECT COUNT(*) FROM shipment_items si WHERE si.shipment_id = s.id) <= 2 THEN 'S'
        WHEN (SELECT COUNT(*) FROM shipment_items si WHERE si.shipment_id = s.id) <= 5 THEN 'M'
        ELSE 'L'
    END,
    package_weight_grams = 150 + (
        SELECT COALESCE(SUM(b.weight_grams), 0)
        FROM shipment_items si
        JOIN order_items oi ON oi.id = si.order_item_id
        JOIN books b ON b.id = oi.book_id
        WHERE si.shipment_id = s.id
    ),
    package_length_mm = 20 + (
        SELECT COALESCE(MAX(b.length_mm), 210)
        FROM shipment_items si
        JOIN order_items oi ON oi.id = si.order_item_id
        JOIN books b ON b.id = oi.book_id
        WHERE si.shipment_id = s.id
    ),
    package_width_mm = 20 + (
        SELECT COALESCE(MAX(b.width_mm), 140)
        FROM shipment_items si
        JOIN order_items oi ON oi.id = si.order_item_id
        JOIN books b ON b.id = oi.book_id
        WHERE si.shipment_id = s.id
    ),
    package_height_mm = 20 + (
        SELECT COALESCE(SUM(b.height_mm), 30)
        FROM shipment_items si
        JOIN order_items oi ON oi.id = si.order_item_id
        JOIN books b ON b.id = oi.book_id
        WHERE si.shipment_id = s.id
    ),
    status_updated_at = s.updated_at
FROM seller_orders so
WHERE so.order_id = s.order_id AND so.seller_id = s.seller_id;

ALTER TABLE shipments DROP CONSTRAINT uk_shipments_order_seller;
ALTER TABLE shipments DROP CONSTRAINT chk_shipments_cod_amount;
ALTER TABLE shipments DROP CONSTRAINT fk_shipments_order;
ALTER TABLE shipments DROP CONSTRAINT fk_shipments_seller;

ALTER TABLE shipments
    ALTER COLUMN seller_order_id SET NOT NULL,
    DROP COLUMN order_id,
    DROP COLUMN seller_id,
    DROP COLUMN cod_amount,
    ADD CONSTRAINT uk_shipments_seller_order UNIQUE (seller_order_id),
    ADD CONSTRAINT chk_shipments_status CHECK (
        status IN (
            'NOT_CREATED',
            'AWB_PENDING',
            'AWB_CREATED',
            'AWAITING_DROPOFF',
            'IN_TRANSIT',
            'DELIVERED',
            'RETURNED',
            'LOST',
            'CANCELLED'
        )
    ),
    ADD CONSTRAINT chk_shipments_package_size CHECK (package_size IN ('S', 'M', 'L')),
    ADD CONSTRAINT chk_shipments_package_measurements CHECK (
        package_weight_grams IS NULL
        OR (
            package_weight_grams BETWEEN 1 AND 20000
            AND package_length_mm > 0
            AND package_width_mm > 0
            AND package_height_mm > 0
        )
    ),
    ADD CONSTRAINT fk_shipments_seller_order
        FOREIGN KEY (seller_order_id) REFERENCES seller_orders (id) ON DELETE CASCADE;

DROP INDEX IF EXISTS idx_shipments_order_id;
DROP INDEX IF EXISTS idx_shipments_seller_id;
DROP TABLE shipment_items;

ALTER TABLE payments DROP CONSTRAINT chk_payments_status;
ALTER TABLE payments
    ADD COLUMN provider_checkout_session_id VARCHAR(255),
    ADD CONSTRAINT uk_payments_provider_checkout_session_id UNIQUE (provider_checkout_session_id),
    ADD CONSTRAINT chk_payments_status CHECK (
        status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'PARTIALLY_REFUNDED', 'REFUNDED')
    );

UPDATE payments SET provider = 'STRIPE' WHERE provider = 'CASH_ON_DELIVERY';

CREATE TABLE seller_transfers (
    id BIGSERIAL PRIMARY KEY,
    seller_order_id BIGINT NOT NULL,
    provider_transfer_id VARCHAR(255),
    amount NUMERIC(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'RON',
    status VARCHAR(30) NOT NULL DEFAULT 'BLOCKED',
    eligible_at TIMESTAMPTZ,
    failure_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_seller_transfers_seller_order UNIQUE (seller_order_id),
    CONSTRAINT uk_seller_transfers_provider_transfer_id UNIQUE (provider_transfer_id),
    CONSTRAINT chk_seller_transfers_amount CHECK (amount >= 0),
    CONSTRAINT chk_seller_transfers_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_seller_transfers_status CHECK (
        status IN ('BLOCKED', 'READY', 'CREATED', 'PAID', 'FAILED', 'REVERSED')
    ),
    CONSTRAINT fk_seller_transfers_seller_order
        FOREIGN KEY (seller_order_id) REFERENCES seller_orders (id) ON DELETE CASCADE
);

INSERT INTO seller_transfers (seller_order_id, amount, currency, status, created_at, updated_at)
SELECT id, seller_proceeds, 'RON', 'BLOCKED', created_at, updated_at
FROM seller_orders;

UPDATE seller_transfers st
SET eligible_at = so.fulfilled_at + INTERVAL '24 hours'
FROM seller_orders so
WHERE so.id = st.seller_order_id AND so.fulfilled_at IS NOT NULL;

CREATE TABLE integration_events (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,
    last_error VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_integration_events_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED')
    ),
    CONSTRAINT chk_integration_events_attempts CHECK (attempts >= 0)
);

CREATE INDEX idx_seller_orders_order_id ON seller_orders (order_id);
CREATE INDEX idx_seller_orders_seller_status ON seller_orders (seller_id, status);
CREATE INDEX idx_seller_orders_accept_by ON seller_orders (accept_by)
    WHERE status = 'AWAITING_SELLER';
CREATE INDEX idx_order_items_seller_order_id ON order_items (seller_order_id);
CREATE INDEX idx_shipments_seller_order_id ON shipments (seller_order_id);
CREATE INDEX idx_integration_events_pending
    ON integration_events (status, next_attempt_at, created_at);

CREATE TRIGGER trg_seller_orders_set_updated_at
    BEFORE UPDATE ON seller_orders
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_seller_transfers_set_updated_at
    BEFORE UPDATE ON seller_transfers
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
