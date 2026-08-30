import { expect, test, type Page } from '@playwright/test'

// Contract/UI tests only: all API calls are intercepted, with no real accounts or email.
const timestamp = '2026-08-30T10:00:00Z'
const initial = { id: 10, reference: 'BN-SUP-ABCDEF0123456789ABCD', topic: 'ORDER', subject: 'Ajutor pentru comanda mea și clarificarea livrării', status: 'NEW', orderId: 20, bookId: null, createdAt: timestamp, updatedAt: timestamp, closedAt: null as string | null, resolvedAt: null as string | null, canReply: true }
const pageOf = (content: unknown[]) => ({ content, totalElements: content.length, totalPages: 1, page: 0, size: 30, hasNext: false })
async function mockApi(page: Page, mode: 'user' | 'admin' | 'guest' = 'user') {
  let ticket = { ...initial }
  let assignee: number | null = null
  const messages = [{ id: 30, kind: 'REQUESTER', authorName: 'Ana Popescu', body: 'Am nevoie de ajutor cu o comandă. Referință detaliată: ' + 'abcdefghij'.repeat(14), createdAt: timestamp }]
  const states = new Map<number, string>([[30, 'DISABLED']])
  const adminEntry = () => ({ ticket, requesterId: 1, contactName: 'Ana Popescu', contactEmail: 'ana.popescu.cititoare@example.test', assignedToId: assignee, assignedToName: assignee ? 'Admin Test' : null })
  await page.route(url => url.pathname.startsWith('/api/'), async route => {
    const path = new URL(route.request().url()).pathname
    const method = route.request().method()
    let data: unknown
    if (path === '/api/auth/csrf') data = { token: 'test' }
    else if (path === '/api/auth/me') {
      if (mode === 'guest') { await route.fulfill({ status: 401, json: { status: 401 } }); return }
      data = { id: 1, firstName: 'Ana', lastName: 'Popescu', email: 'ana@test.ro', role: mode === 'admin' ? 'ADMIN' : 'USER', emailVerified: true }
    }
    else if (path === '/api/auth/refresh') { await route.fulfill({ status: 401, json: { status: 401 } }); return }
    else if (path === '/api/notifications' || path === '/api/wishlist') data = []
    else if (path === '/api/cart') data = { id: 1, items: [], total: 0 }
    else if (path === '/api/contact') data = { message: 'Solicitarea a fost înregistrată.', ticketId: mode === 'guest' ? null : 10, reference: ticket.reference }
    else if (path === '/api/admin/support/administrators') data = [{ id: 1, name: 'Admin Test' }]
    else if (path.endsWith('/retry-email')) { states.set(30, 'PENDING'); data = null }
    else if (path.endsWith('/10/status')) {
      ticket = { ...ticket, status: route.request().postDataJSON().status }
      ticket.canReply = ticket.status !== 'CLOSED'
      data = adminEntry()
    }
    else if (path.endsWith('/10/assignment')) { assignee = route.request().postDataJSON().administratorId; data = adminEntry() }
    else if (path.endsWith('/10/messages') && method === 'POST') {
      const message = { id: 31 + messages.length, kind: mode === 'admin' ? 'ADMIN' : 'REQUESTER', authorName: mode === 'admin' ? 'Echipa BookNest' : 'Ana Popescu', body: route.request().postDataJSON().body, createdAt: timestamp }
      messages.push(message); states.set(message.id, 'PENDING'); ticket.status = 'IN_PROGRESS'; data = message
    }
    else if (path.endsWith('/10/messages')) data = pageOf([...messages].reverse().map(message => mode === 'admin' ? { message, emailStatus: states.get(message.id), emailAttempts: 0, emailError: null } : message))
    else if (path.endsWith('/tickets/10')) data = mode === 'admin' ? adminEntry() : ticket
    else if (path.endsWith('/support/tickets')) data = pageOf([mode === 'admin' ? adminEntry() : ticket])
    else { await route.fulfill({ status: 404, json: { message: `Unmocked API: ${path}` } }); return }
    await route.fulfill({ status: 200, json: data })
  })
}
async function noOverflow(page: Page) {
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  const outside = await page.locator('.support-page *, .contact-form-card *').evaluateAll(elements => elements.filter(element => {
    const rect = element.getBoundingClientRect()
    const style = getComputedStyle(element)
    return !element.closest('[aria-hidden="true"]') && style.position !== 'absolute' && rect.width > 0 && (rect.right > window.innerWidth + 1 || rect.left < -1)
  }).map(element => `${element.tagName}.${element.className}`))
  expect(outside).toEqual([])
}
test('signed-in user creates a linked ticket and follows its conversation', async ({ page }, info) => {
  await mockApi(page)
  await page.goto('/contact?topic=ORDER&orderId=20')
  await page.getByRole('textbox', { name: 'Subiect' }).fill('Ajutor pentru comanda mea')
  await page.getByRole('textbox', { name: 'Mesaj', exact: true }).fill('Am nevoie de ajutor cu livrarea acestei comenzi.')
  await page.getByRole('checkbox').check()
  const sent = page.waitForRequest(request => request.method() === 'POST' && request.url().endsWith('/api/contact'))
  await page.getByRole('button', { name: 'Trimite mesajul' }).click()
  expect((await sent).postDataJSON()).toMatchObject({ orderId: 20, topic: 'ORDER' })
  await expect(page.getByText(initial.reference)).toBeVisible()
  await page.getByRole('link', { name: 'Vezi solicitarea', exact: true }).click()
  await expect(page.getByRole('heading', { name: initial.subject })).toBeVisible()
  await page.getByRole('textbox', { name: 'Adaugă un răspuns' }).fill('Detalii suplimentare despre situație.')
  await page.getByRole('button', { name: 'Trimite răspunsul' }).click()
  await expect(page.getByText('Răspunsul a fost salvat în conversație.')).toBeVisible()
  await expect(page.getByText('Detalii suplimentare despre situație.', { exact: true })).toBeVisible()
  await noOverflow(page)
  await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'instant' }))
  await page.screenshot({ path: info.outputPath('support-conversation.png'), fullPage: true })
})
test('administrator assigns replies closes reopens and retries email', async ({ page }, info) => {
  await mockApi(page, 'admin')
  await page.goto('/admin/support')
  await page.getByRole('link', { name: new RegExp(initial.reference) }).click()
  await expect(page.getByText(/trimitere dezactivată/)).toBeVisible()
  await page.getByRole('button', { name: 'Reîncearcă emailul' }).click()
  await expect(page.getByText(/Email către echipă: în așteptare/)).toBeVisible()
  await page.getByRole('combobox', { name: 'Repartizează către' }).selectOption('1')
  await page.getByRole('textbox', { name: 'Motivul repartizării' }).fill('Preluare solicitare')
  await page.getByRole('button', { name: 'Salvează repartizarea' }).click()
  await expect(page.locator('.support-tools dd').filter({ hasText: 'Admin Test' })).toBeVisible()
  await page.getByRole('textbox', { name: 'Răspuns către solicitant' }).fill('Am verificat comanda și revenim cu detalii.')
  await page.getByRole('button', { name: 'Trimite răspunsul' }).click()
  await expect(page.getByText('Am verificat comanda și revenim cu detalii.', { exact: true })).toBeVisible()
  await page.getByRole('combobox', { name: 'Stare nouă' }).selectOption('CLOSED')
  await page.getByRole('textbox', { name: 'Motivul schimbării stării' }).fill('Solicitarea a fost soluționată')
  await page.getByRole('button', { name: 'Actualizează starea' }).click()
  await expect(page.getByText('Solicitarea este închisă.')).toBeVisible()
  await expect(page.getByRole('textbox', { name: 'Răspuns către solicitant' })).toHaveCount(0)
  await page.getByRole('combobox', { name: 'Stare nouă' }).selectOption('IN_PROGRESS')
  await page.getByRole('textbox', { name: 'Motivul schimbării stării' }).fill('Detalii noi')
  await page.getByRole('button', { name: 'Actualizează starea' }).click()
  await expect(page.getByRole('textbox', { name: 'Răspuns către solicitant' })).toBeVisible()
  await noOverflow(page)
  await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'instant' }))
  await page.screenshot({ path: info.outputPath('support-admin.png'), fullPage: true })
})
test('guest receives a reference but no private ticket URL', async ({ page }, info) => {
  await mockApi(page, 'guest')
  await page.goto('/contact')
  await page.getByRole('textbox', { name: 'Nume', exact: true }).fill('Vizitator Test')
  await page.getByRole('textbox', { name: 'Email pentru răspuns' }).fill('visitor@example.test')
  await page.getByRole('textbox', { name: 'Subiect' }).fill('Întrebare despre aplicație')
  await page.getByRole('textbox', { name: 'Mesaj', exact: true }).fill('Am o întrebare despre funcționarea aplicației BookNest.')
  await page.getByRole('checkbox').check()
  await page.getByRole('button', { name: 'Trimite mesajul' }).click()
  await expect(page.getByText(initial.reference)).toBeVisible()
  await expect(page.getByRole('link', { name: 'Vezi solicitarea', exact: true })).toHaveCount(0)
  await noOverflow(page)
  await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'instant' }))
  await page.screenshot({ path: info.outputPath('support-guest.png'), fullPage: true })
})
