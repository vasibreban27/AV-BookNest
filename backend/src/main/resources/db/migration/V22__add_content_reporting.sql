ALTER TABLE users ADD COLUMN suspended_until TIMESTAMPTZ;
CREATE INDEX idx_users_suspension_expiry ON users(suspended_until) WHERE suspended_until IS NOT NULL;

CREATE TABLE content_reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL REFERENCES users(id),
    target_type VARCHAR(10) NOT NULL CHECK (target_type IN ('BOOK','USER')),
    target_user_id BIGINT NOT NULL REFERENCES users(id),
    book_id BIGINT REFERENCES books(id),
    target_label VARCHAR(500) NOT NULL,
    reason VARCHAR(30) NOT NULL CHECK (reason IN ('SPAM','FRAUD','PROHIBITED_CONTENT','HARASSMENT','MISLEADING','OTHER')),
    description VARCHAR(2000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW','IN_PROGRESS','RESOLVED','DISMISSED')),
    assigned_to_id BIGINT REFERENCES users(id),
    decision VARCHAR(30),
    decision_note VARCHAR(500),
    suspended_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (reporter_id <> target_user_id),
    CHECK ((target_type = 'BOOK' AND book_id IS NOT NULL) OR (target_type = 'USER' AND book_id IS NULL)),
    CHECK (decision IS NULL OR decision IN ('DISMISS','WARN','HIDE_BOOK','SUSPEND'))
);
CREATE INDEX idx_reports_queue ON content_reports(status, created_at, id);
CREATE INDEX idx_reports_reporter ON content_reports(reporter_id, created_at DESC);
CREATE INDEX idx_reports_history ON content_reports(target_user_id, status, created_at DESC);
CREATE UNIQUE INDEX idx_reports_open_duplicate ON content_reports(reporter_id, target_type, target_user_id, COALESCE(book_id,0)) WHERE status IN ('NEW','IN_PROGRESS');

CREATE TABLE report_evidence (
    id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL REFERENCES content_reports(id),
    content_type VARCHAR(20) NOT NULL CHECK (content_type IN ('image/png','image/jpeg')),
    content BYTEA NOT NULL CHECK (octet_length(content) BETWEEN 1 AND 2097152)
);
CREATE INDEX idx_report_evidence_report ON report_evidence(report_id);

CREATE TABLE report_events (
    id BIGSERIAL PRIMARY KEY,
    report_id BIGINT NOT NULL REFERENCES content_reports(id),
    actor_id BIGINT NOT NULL REFERENCES users(id),
    action VARCHAR(30) NOT NULL,
    note VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_report_events_report ON report_events(report_id, id);
