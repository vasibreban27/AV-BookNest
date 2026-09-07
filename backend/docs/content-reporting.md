# Content reporting and moderation

Migrations: **V22–V23**. V22 creates the reporting system and remains immutable; V23 safely evolves already-applied V22 databases so reports survive listing deletion. Run the backend normally with Flyway enabled; no manual repair or database reset is necessary.

## User workflow

- An authenticated, enabled account can report a public, available listing or a public seller. Buyers and sellers can also report their trading partner from an existing order, even if the partner has no public seller profile.
- Reasons: `SPAM`, `FRAUD`, `PROHIBITED_CONTENT`, `HARASSMENT`, `MISLEADING`, `OTHER`. Optional description: up to 2,000 characters; `OTHER` requires at least 10 non-padding characters.
- No self-reports. One open report per account/target; at most 10 submissions per rolling 24 hours. A database uniqueness constraint and a reporter row lock protect concurrent submissions across application instances.
- Reports are not public. Only their author and administrators can read them or their evidence. The reported user receives the decision notice, not the reporter's identity or photographs.
- Submission, evidence, moderation events, sanctions, notifications and audit writes participate in database transactions. Closed decisions cannot be applied twice.

## Private photographs

Up to three PNG/JPEG images, each at most 2 MiB, 4,096 pixels per side and 12 megapixels total. The server detects the real format, checks dimensions before decoding, and re-encodes pixels to remove metadata (including GPS) and appended payloads. SVG, other formats, invalid files and oversized output are rejected.

Evidence is stored in PostgreSQL `BYTEA`, **not** in public Cloudinary URLs. Downloads are authenticated, scoped to their report, and served with `Cache-Control: no-store` and `X-Content-Type-Options: nosniff`. No original filenames are persisted. The multipart request limit is 7 MB, allowing three 2 MiB images plus JSON and framing; book-cover file limits remain unchanged. Configure any reverse proxy to accept this request size.

This keeps deployment self-contained but increases database/backup size. A retention policy and migration to private object storage can be introduced separately; no automatic evidence deletion is enabled. Deleting an eligible listing preserves the report's target ID/title snapshot, user association, evidence and decision history, while removing its live book association.

## Moderation

Queue: `NEW` → `IN_PROGRESS` → `RESOLVED` or `DISMISSED`. Claiming assigns the acting admin; another admin can explicitly take over. Decisions require a reason of 1–500 nonblank characters:

- `DISMISS`: no confirmed violation or account sanction.
- `WARN`: notification to the user and a persistent history entry.
- `HIDE_BOOK`: hide only the linked listing using existing moderation controls.
- `SUSPEND`: suspend the target user for 1–90 days, revoke refresh sessions and hide currently visible listings. Already suspended accounts and administrator accounts cannot receive a new temporary sanction here.

The decision reason is shown to the reporter and, for sanctions, to the affected account. Do not include reporter identities or internal/private information in it. Notification delivery is in-app; no new SMTP or external service configuration is required. Suspended users can contact support as guests to appeal; administrative reactivation remains available under Users.

The expiry worker checks every 30 seconds (`app.moderation.expiry-delay-ms`) in batches of 100, including after restart. A dedicated `moderationTaskScheduler` isolates database waits from payment/order deadlines and support email delivery. Each user is locked and rechecked transactionally before reactivation. Only listings hidden for `ACCOUNT_SUSPENDED` are restored; policy-hidden listings stay hidden. A permanent suspension or manual reactivation clears the pending expiry. Historical report decisions and audit records remain unchanged. Expiry audit entries are explicitly marked automatic and retain the original sanction issuer.

`/api/admin/reports/users/{id}/history` includes the latest 100 sanctions/reactivations, including pre-existing administrative suspensions. Older administrative history remains in the paginated audit panel. Reports alone or dismissed reports are not counted as confirmed violations.

## API

- `POST /api/reports`: multipart `report` (application/json: `targetType`, `targetId`, `reason`, `description`) and optional repeated `evidence` file parts; 201.
- `GET /api/reports?page=0&size=20`: current user's reports.
- `GET /api/reports/{id}`: own report and evidence metadata.
- `GET /api/reports/{id}/evidence/{evidenceId}`: evidence bytes, owner/admin only.
- `GET /api/admin/reports?status=NEW&targetUserId=123&page=0&size=20`: filtered queue.
- `GET /api/admin/reports/{id}`: detail and event history.
- `POST /api/admin/reports/{id}/claim`: claim/take over an open report.
- `POST /api/admin/reports/{id}/resolve`: JSON `decision`, `note`, and `suspensionDays` only for `SUSPEND`.
- `GET /api/admin/reports/users/{id}/history`: account state and moderation history.

CSRF protection applies to all writes. Page size is bounded to 1–100. The existing seller-order response additionally exposes the buyer ID to the transaction participants for reporting.

## Verification

Run `./mvnw test` for local unit/security tests; PostgreSQL tests are conditional on `BOOKNEST_TEST_DATABASE_URL` (and optional `BOOKNEST_TEST_DATABASE_USER` / `BOOKNEST_TEST_DATABASE_PASSWORD`). `scripts/test-reviews-postgres.ps1` runs the entire suite on an isolated local PostgreSQL cluster, including reports, upgrades, evidence access, suspension expiry and full Spring application startup. It never uses the application's database.
