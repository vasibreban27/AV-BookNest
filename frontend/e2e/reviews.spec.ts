import { expect, test, type Page } from '@playwright/test'

// These UI contract tests intercept every /api request: no live account, order,
// payment or application database is touched.
const review = { id: 40, bookId: 3, bookTitle: 'Povestea unei cărți regăsite', sellerId: 2, reviewerName: 'Ana P.', sellerRating: 5, descriptionRating: 4, conditionRating: 3, comment: 'Ambalată cu grijă, exact cum era descrisă. Recomand!', verifiedPurchase: true, createdAt: '2026-08-30T10:00:00Z' }
const published = { review, orderItemId: 30, moderationStatus: 'VISIBLE', moderationReason: null as string | null, moderatedAt: null as string | null }
const book = { id: 3, title: review.bookTitle, author: 'Autor test', isbn: '1234567890', description: 'O carte care își caută următorul cititor.', price: 35, bookCondition: 'GOOD', language: 'Română', publisher: 'Editura test', publishedYear: 2020, coverImageUrl: null, sellerId: 2, sellerName: 'Ion Ionescu', category: { id: 1, name: 'Literatură', slug: 'literatura' }, status: 'AVAILABLE', createdAt: review.createdAt, updatedAt: review.createdAt }
const orderItem = { id: 30, bookId: 3, sellerId: 2, title: book.title, author: book.author, isbn: book.isbn, unitPrice: 35, quantity: 1 }
const order = { id: 10, orderNumber: 'BN-REVIEW-TEST', status: 'DELIVERED', items: [orderItem], sellerOrders: [{ id: 20, orderId: 10, orderNumber: 'BN-REVIEW-TEST', buyerName: 'Ana Popescu', sellerId: 2, sellerName: book.sellerName, status: 'FULFILLED', itemSubtotal: 35, commissionRate: 5, commissionAmount: 1.75, sellerProceeds: 33.25, shippingCost: 15, fulfilledAt: review.createdAt, issueStatus: 'NONE', canReportIssue: false, items: [orderItem], shipment: { id: 50, easyboxName: 'Easybox test', easyboxAddress: 'Strada Cărții 1', easyboxCity: 'București', status: 'DELIVERED', trackingNumber: 'TEST-50' } }], subtotal: 35, shippingCost: 15, totalAmount: 50, currency: 'RON', placedAt: review.createdAt, payment: { id: 60, provider: 'STRIPE', amount: 50, currency: 'RON', status: 'SUCCEEDED' } }
const pageOf = (content: unknown[]) => ({ content, totalElements: content.length, totalPages: content.length ? 1 : 0, page: 0, size: 25, hasNext: false })

async function mockApi(page: Page, admin = false) {
  let created = false
  let moderated = { ...published }
  const user = { id: 1, firstName: 'Ana', lastName: 'Popescu', email: 'test@example.com', role: admin ? 'ADMIN' : 'USER', emailVerified: true, phoneNumber: null }
  await page.route((url) => url.pathname.startsWith('/api/'), async (route) => {
    const path = new URL(route.request().url()).pathname
    let body: unknown
    if (path === '/api/auth/csrf') body = { token: 'test' }
    else if (path === '/api/auth/me') body = user
    else if (path === '/api/notifications' || path === '/api/wishlist') body = []
    else if (path === '/api/cart') body = { id: 1, items: [], total: 0 }
    else if (path === '/api/books/3') body = book
    else if (path === '/api/orders/10') body = order
    else if (path === '/api/orders/10/reviews') body = [{ orderItemId: 30, sellerId: 2, bookTitle: book.title, canReview: !created, ineligibilityReason: created ? 'ALREADY_REVIEWED' : null, existingReview: created ? published : null }]
    else if (path === '/api/orders/10/items/30/review') { created = true; body = published }
    else if (path === '/api/sellers/2/reputation') body = { sellerId: 2, sellerName: book.sellerName, ratings: { reviewCount: 1, sellerRating: 5, descriptionRating: 4, conditionRating: 3 } }
    else if (path === '/api/sellers/2/reviews') body = pageOf([review])
    else if (path === '/api/admin/reviews/40/moderation') {
      const payload = route.request().postDataJSON()
      moderated = { ...moderated, moderationStatus: payload.status, moderationReason: payload.reason }
      body = moderated
    }
    else if (path === '/api/admin/reviews') body = pageOf([moderated])
    else { await route.fulfill({ status: 404, json: { message: `Unmocked API: ${path}` } }); return }
    await route.fulfill({ status: path.endsWith('/review') ? 201 : 200, json: body })
  })
}

async function expectNoOverflow(page: Page) {
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  // Overflow-x: clip on the app shell must not hide a broken review layout.
  const outside = await page.locator('.reviews-panel *, .admin-review-entry *').evaluateAll((elements) => elements.filter((element) => {
    const rect = element.getBoundingClientRect()
    const style = getComputedStyle(element)
    return style.position !== 'absolute' && rect.width > 0 && (rect.right > window.innerWidth + 1 || rect.left < -1)
  }).map((element) => element.className))
  expect(outside).toEqual([])
}

test('buyer submits a verified review with all rating dimensions', async ({ page }, info) => {
  await mockApi(page)
  await page.goto('/orders/10')
  await page.getByRole('button', { name: 'Scrie o recenzie' }).click()
  await page.getByRole('group', { name: 'Experiența cu vânzătorul' }).getByRole('radio', { name: '5 din 5' }).check()
  await page.getByRole('group', { name: 'Corectitudinea descrierii' }).getByRole('radio', { name: '4 din 5' }).check()
  await page.getByRole('group', { name: 'Starea cărții' }).getByRole('radio', { name: '3 din 5' }).check()
  await page.getByRole('textbox', { name: 'Comentariu (opțional)' }).fill('Ambalată cu grijă, exact cum era descrisă. Recomand!')
  await expectNoOverflow(page)
  await page.locator('.reviews-panel').screenshot({ path: info.outputPath('review-form.png') })
  const sent = page.waitForRequest((request) => request.method() === 'POST' && request.url().endsWith('/orders/10/items/30/review'))
  await page.getByRole('button', { name: 'Publică recenzia' }).click()
  expect((await sent).postDataJSON()).toEqual({ sellerRating: 5, descriptionRating: 4, conditionRating: 3, comment: review.comment })
  await expect(page.getByText('Recenzia ta este publicată. Mulțumim!')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Scrie o recenzie' })).toHaveCount(0)
})

test('public book details display reputation and verified feedback without overflow', async ({ page }, info) => {
  await mockApi(page)
  await page.goto('/books/3')
  await expect(page.getByRole('heading', { name: 'Reputația vânzătorului' })).toBeVisible()
  await expect(page.getByText('1 recenzie verificată')).toBeVisible()
  await expect(page.getByText(review.comment)).toBeVisible()
  await expectNoOverflow(page)
  await page.locator('.seller-reputation').screenshot({ path: info.outputPath('seller-reputation.png') })
})

test('admin moderates and restores with a recorded reason', async ({ page }, info) => {
  await mockApi(page, true)
  await page.goto('/admin/reviews')
  await page.getByRole('button', { name: 'Ascunde recenzia' }).click()
  await expect(page.getByRole('button', { name: 'Confirmă ascunderea' })).toBeDisabled()
  await page.getByRole('textbox', { name: 'Motivul moderării' }).fill('Date personale în comentariu')
  await expectNoOverflow(page)
  await page.locator('.admin-review-entry').screenshot({ path: info.outputPath('admin-review.png') })
  await page.getByRole('button', { name: 'Confirmă ascunderea' }).click()
  await expect(page.getByText('Ascunsă', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: 'Restaurează recenzia' }).click()
  await page.getByRole('textbox', { name: 'Motivul moderării' }).fill('Contestație acceptată')
  await page.getByRole('button', { name: 'Confirmă restaurarea' }).click()
  await expect(page.getByText('Vizibilă', { exact: true })).toBeVisible()
})
