CREATE TABLE support_tickets (
    id BIGSERIAL PRIMARY KEY,
    reference VARCHAR(40) NOT NULL UNIQUE,
    requester_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    contact_name VARCHAR(200) NOT NULL,
    contact_email VARCHAR(254) NOT NULL,
    topic VARCHAR(20) NOT NULL,
    subject VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    related_order_id BIGINT REFERENCES orders(id) ON DELETE SET NULL,
    related_book_id BIGINT REFERENCES books(id) ON DELETE SET NULL,
    assigned_to_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    privacy_accepted_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,
    CONSTRAINT chk_support_topic CHECK (topic IN ('GENERAL','ORDER','PAYMENT','DELIVERY','ACCOUNT','LISTING','PRIVACY','OTHER')),
    CONSTRAINT chk_support_status CHECK (status IN ('NEW','IN_PROGRESS','RESOLVED','CLOSED')),
    CONSTRAINT chk_support_subject CHECK (length(trim(subject)) > 0)
);

CREATE TABLE support_messages (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES support_tickets(id),
    author_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    kind VARCHAR(20) NOT NULL,
    body VARCHAR(4000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_support_message_kind CHECK (kind IN ('REQUESTER','ADMIN','SYSTEM')),
    CONSTRAINT chk_support_message_body CHECK (length(trim(body)) > 0)
);

-- Persist the email intent in the same transaction as the message. SMTP failure
-- must never roll back a ticket/reply or require the user to submit it again.
CREATE TABLE support_email_deliveries (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL UNIQUE REFERENCES support_messages(id),
    recipient VARCHAR(254) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMPTZ,
    last_error VARCHAR(300),
    CONSTRAINT chk_support_email_status CHECK (status IN ('PENDING','SENT','FAILED','DISABLED')),
    CONSTRAINT chk_support_email_attempts CHECK (attempts >= 0)
);

CREATE INDEX idx_support_requester ON support_tickets(requester_id, updated_at DESC, id DESC);
CREATE INDEX idx_support_status ON support_tickets(status, updated_at DESC, id DESC);
CREATE INDEX idx_support_assignee ON support_tickets(assigned_to_id, updated_at DESC, id DESC);
CREATE INDEX idx_support_messages_ticket ON support_messages(ticket_id, id DESC);
CREATE INDEX idx_support_email_due ON support_email_deliveries(next_attempt_at) WHERE status IN ('PENDING','FAILED');
CREATE TRIGGER trg_support_tickets_updated_at BEFORE UPDATE ON support_tickets
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
