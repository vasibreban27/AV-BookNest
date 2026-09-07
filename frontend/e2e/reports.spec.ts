import { expect, test, type Page } from '@playwright/test'
const timestamp = '2026-09-06T12:00:00Z'
const initial = { id: 10, reporterId: 1, targetType: 'BOOK', targetUserId: 2, bookId: 100, targetLabel: 'Un anunț cu o descriere foarte lungă ' + 'abcdefghij'.repeat(12), reason: 'FRAUD', description: 'Situație semnalată de cumpărător.', status: 'NEW', assignedToId: null as number | null, decision: null as string | null, decisionNote: null as string | null, suspendedUntil: null as string | null, createdAt: timestamp, updatedAt: timestamp }
const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl6FHwAAAAASUVORK5CYII=', 'base64')
async function mockApi(page: Page, admin = false, guest = false) {
  let report = { ...initial }
  await page.route(url => url.pathname.startsWith('/api/'), async route => {
    const path = new URL(route.request().url()).pathname
    const method = route.request().method()
    let data: unknown
    if (path === '/api/auth/csrf') data = { token: 'test' }
    else if (path === '/api/auth/me') {
      if (guest) { await route.fulfill({ status: 401, json: { status: 401 } }); return }
      data = { id: 1, firstName: 'Ana', lastName: 'Test', email: 'ana@example.test', role: admin ? 'ADMIN' : 'USER', emailVerified: true }
    }
    else if (path === '/api/auth/refresh') { await route.fulfill({ status: 401, json: { status: 401 } }); return }
    else if (['/api/notifications', '/api/wishlist'].includes(path)) data = []
    else if (path === '/api/cart') data = { id: 1, items: [], total: 0 }
    else if (path === '/api/reports' && method === 'POST') data = report
    else if (path.endsWith('/reports/10/claim')) { report.assignedToId = 1; report.status = 'IN_PROGRESS'; data = { report, evidence: [{ id: 20, contentType: 'image/png' }], events: [] } }
    else if (path.endsWith('/reports/10/resolve')) {
      const body = route.request().postDataJSON()
      report = { ...report, status: body.decision === 'DISMISS' ? 'DISMISSED' : 'RESOLVED', decision: body.decision, decisionNote: body.note, suspendedUntil: body.decision === 'SUSPEND' ? '2026-09-13T12:00:00Z' : null }
      data = { report, evidence: [{ id: 20, contentType: 'image/png' }], events: [] }
    }
    else if (path === '/api/admin/reports/users/2/history') data = { enabled: report.decision !== 'SUSPEND', suspendedUntil: report.suspendedUntil, suspensionReason: report.decisionNote, entries: [{ action: 'REPORT_WARNING', reason: 'Avertisment anterior verificat', createdAt: timestamp }] }
    else if (path.endsWith('/reports/10/evidence/20')) { await route.fulfill({ contentType: 'image/png', body: png }); return }
    else if (path.endsWith('/reports/10')) data = { report, evidence: [{ id: 20, contentType: 'image/png' }], events: [{ id: 30, actorId: 1, action: 'CREATED', note: 'Raportare trimisă', createdAt: timestamp }] }
    else if (path.endsWith('/reports')) data = { content: [report], page: 0, size: 20, totalPages: 1, totalElements: 1, hasNext: false }
    else { await route.fulfill({ status: 404, json: { message: `Unmocked ${path}` } }); return }
    await route.fulfill({ json: data })
  })
}
async function noOverflow(page: Page) {
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  expect(await page.locator('.reports-page *').evaluateAll(elements => elements.filter(e => {
    const r = e.getBoundingClientRect(); return r.width > 0 && (r.right > innerWidth + 1 || r.left < -1)
  }).map(e => `${e.tagName}.${e.className}`))).toEqual([])
}
test('report a listing with private evidence and follow its status', async ({ page }, info) => {
  await mockApi(page)
  await page.goto('/report?targetType=BOOK&targetId=100')
  await page.getByLabel('Motivul raportării').selectOption('FRAUD')
  await page.getByLabel('Descriere suplimentară').fill('Am identificat o problemă cu acest anunț.')
  await page.getByLabel('Fotografii / dovezi (opțional)').setInputFiles({ name: 'dovada.png', mimeType: 'image/png', buffer: png })
  await noOverflow(page)
  await page.screenshot({ path: info.outputPath('report-form.png'), fullPage: true })
  const sent = page.waitForRequest(r => r.method() === 'POST' && r.url().endsWith('/api/reports'))
  await page.getByRole('button', { name: 'Trimite raportarea' }).click()
  expect((await sent).postData()).toContain('"targetType":"BOOK","targetId":100')
  await expect(page.getByRole('heading', { name: 'Raportare #10' })).toBeVisible()
  await expect(page.getByAltText('Dovadă 1')).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Decizie de moderare' })).toHaveCount(0)
  await noOverflow(page)
})
test('moderator reviews evidence and history then applies a temporary suspension', async ({ page }, info) => {
  await mockApi(page, true)
  await page.goto('/admin/reports')
  await page.getByRole('link', { name: /#10 ·/ }).click()
  await expect(page.getByText('Avertisment anterior verificat')).toBeVisible()
  await expect(page.getByAltText('Dovadă 1')).toBeVisible()
  await page.getByRole('button', { name: 'Preia raportarea', exact: true }).click()
  await expect(page.getByText('Responsabil: administrator #1.')).toBeVisible()
  await page.getByRole('combobox', { name: 'Acțiune', exact: true }).selectOption('SUSPEND')
  await page.getByLabel('Durata suspendării (zile)').fill('7')
  await page.getByLabel('Motivul deciziei').fill('Abatere confirmată prin verificarea dovezilor.')
  await page.getByRole('checkbox').check()
  await noOverflow(page)
  await page.screenshot({ path: info.outputPath('report-moderation.png'), fullPage: true })
  const sent = page.waitForRequest(r => r.url().endsWith('/resolve'))
  await page.getByRole('button', { name: 'Confirmă decizia' }).click()
  expect((await sent).postDataJSON()).toMatchObject({ decision: 'SUSPEND', suspensionDays: 7 })
  await expect(page.getByRole('heading', { name: 'Raportare soluționată' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Confirmă decizia' })).toHaveCount(0)
  await noOverflow(page)
})
test('user reporting supports predefined reasons and additional details', async ({ page }) => {
  await mockApi(page)
  await page.goto('/report?targetType=USER&targetId=2')
  await expect(page.getByRole('heading', { name: 'Raportează utilizatorul' })).toBeVisible()
  await page.getByLabel('Motivul raportării').selectOption('OTHER')
  await page.getByLabel('Descriere suplimentară').fill('scurt')
  await page.getByRole('button', { name: 'Trimite raportarea' }).click()
  await expect(page.getByRole('alert')).toContainText('cel puțin 10')
  await page.getByLabel('Descriere suplimentară').fill('Comportament abuziv în legătură cu tranzacția.')
  const sent = page.waitForRequest(r => r.method() === 'POST' && r.url().endsWith('/api/reports'))
  await page.getByRole('button', { name: 'Trimite raportarea' }).click()
  expect((await sent).postData()).toContain('"targetType":"USER","targetId":2')
  await expect(page.getByRole('heading', { name: 'Raportare #10' })).toBeVisible()
})
