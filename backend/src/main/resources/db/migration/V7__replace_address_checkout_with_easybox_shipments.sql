ALTER TABLE orders
    ALTER COLUMN shipping_address_id DROP NOT NULL;

CREATE TABLE shipments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    easybox_id VARCHAR(100) NOT NULL,
    easybox_name VARCHAR(255) NOT NULL,
    tracking_number VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'AWAITING_SELLER',
    cod_amount NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_shipments_order_seller UNIQUE (order_id, seller_id),
    CONSTRAINT uk_shipments_tracking_number UNIQUE (tracking_number),
    CONSTRAINT chk_shipments_status CHECK (status IN ('AWAITING_SELLER', 'AWB_CREATED', 'IN_TRANSIT', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT chk_shipments_cod_amount CHECK (cod_amount >= 0),
    CONSTRAINT fk_shipments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_shipments_seller FOREIGN KEY (seller_id) REFERENCES users (id)
);

CREATE TABLE shipment_items (
    id BIGSERIAL PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    CONSTRAINT uk_shipment_items_order_item UNIQUE (order_item_id),
    CONSTRAINT fk_shipment_items_shipment FOREIGN KEY (shipment_id) REFERENCES shipments (id) ON DELETE CASCADE,
    CONSTRAINT fk_shipment_items_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id) ON DELETE CASCADE
);

CREATE INDEX idx_shipments_order_id ON shipments (order_id);
CREATE INDEX idx_shipments_seller_id ON shipments (seller_id);

CREATE TRIGGER trg_shipments_set_updated_at
    BEFORE UPDATE ON shipments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
