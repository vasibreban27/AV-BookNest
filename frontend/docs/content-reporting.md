# Reporting UI

Requires backend migrations V22–V23. No new frontend environment variables are required.

- Listing details: **Raportează anunțul**.
- Seller reputation, order sellers and sales buyers: **Raportează utilizatorul**. Own content is excluded.
- `/report?targetType=BOOK|USER&targetId=...`: authenticated form with reasons, additional text, evidence validation and removal. Login redirects preserve the target query parameters. Server validation remains authoritative.
- `/reports` and `/reports/:reportId`: **Raportările mele**, current status, private evidence and decision reason. Submission failures preserve the draft and selected files.
- `/admin/reports`: moderation queue with status filter, target-user filter and pagination.
- `/admin/reports/:reportId`: private photographs, claim/takeover, event timeline, prior sanctions and decisions. Suspension duration is 1–90 days. A checkbox confirms evidence/history review before applying the decision; reasons are shared with affected users.
- `/admin/users`: suspension expiry and prior moderation history, alongside existing manual reactivation controls.

Photos are retrieved with authenticated API requests, rendered using temporary object URLs and released when the view unmounts. They are not public image URLs. Reports and account history use user-scoped query keys. Report lists/details refresh every 30 seconds and relevant caches are invalidated after moderation actions.

Verification: `npm test`, `npm run build`, `npm run lint`, `npm run test:e2e:reports`. Playwright covers submission with a photo, user reporting, moderation/history and suspension at widths 320/390/1440 px using simulated APIs. Backend tests independently exercise real PostgreSQL persistence and authorization; browser mocks are not a substitute for live production acceptance testing.
