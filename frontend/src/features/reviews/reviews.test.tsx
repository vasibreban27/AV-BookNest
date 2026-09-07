import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactNode } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { OrderReviews } from '../../components/reviews/OrderReviews'
import { SellerReputation } from '../../components/reviews/SellerReputation'
import { AdminReviewsPage } from '../../pages/admin/AdminReviewsPage'
import { reviewsApi } from './api/reviewsApi'
import type { OrderReview, PrivateReview, ReviewPage } from './types/review.types'

vi.mock('./api/reviewsApi', () => ({ reviewsApi: { order: vi.fn(), create: vi.fn(), reputation: vi.fn(), publicReviews: vi.fn(), admin: vi.fn(), moderate: vi.fn() } }))
vi.mock('../auth/hooks/useAuth', () => ({ useAuth: () => ({ user: { id: 1, role: 'ADMIN' } }) }))

const published: PrivateReview = {
  review: { id: 40, bookId: 3, bookTitle: 'Cartea test', sellerId: 2, reviewerName: 'Ana P.', sellerRating: 5, descriptionRating: 4, conditionRating: 3, comment: 'Cartea a ajuns.', verifiedPurchase: true, createdAt: '2026-08-30T10:00:00Z' },
  orderItemId: 30, moderationStatus: 'VISIBLE', moderationReason: null, moderatedAt: null,
}
const eligible: OrderReview = { orderItemId: 30, sellerId: 2, bookTitle: 'Cartea test', canReview: true, ineligibilityReason: null, existingReview: null }
const pageOf = <T,>(content: T[]): ReviewPage<T> => ({ content, totalElements: content.length, totalPages: content.length ? 1 : 0, page: 0, size: 5, hasNext: false })

function renderUi(component: ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(<QueryClientProvider client={client}><MemoryRouter>{component}</MemoryRouter></QueryClientProvider>)
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(reviewsApi.order).mockResolvedValue([eligible])
  vi.mocked(reviewsApi.reputation).mockResolvedValue({ sellerId: 2, sellerName: 'Ion Ionescu', ratings: { reviewCount: 0, sellerRating: null, descriptionRating: null, conditionRating: null } })
  vi.mocked(reviewsApi.publicReviews).mockResolvedValue(pageOf([]))
  vi.mocked(reviewsApi.admin).mockResolvedValue(pageOf([published]))
})

describe('purchase reviews', () => {
  it('requires all three ratings, then submits only review fields for the purchased item', async () => {
    const user = userEvent.setup()
    vi.mocked(reviewsApi.create).mockResolvedValue(published)
    renderUi(<OrderReviews orderId={10} />)
    await user.click(await screen.findByRole('button', { name: 'Scrie o recenzie' }))
    await user.click(screen.getByRole('button', { name: 'Publică recenzia' }))
    expect(reviewsApi.create).not.toHaveBeenCalled()
    for (const [label, value] of [['Experiența cu vânzătorul', 5], ['Corectitudinea descrierii', 4], ['Starea cărții', 3]] as const) {
      await user.click(within(screen.getByRole('group', { name: label })).getByRole('radio', { name: `${value} din 5` }))
    }
    await user.type(screen.getByRole('textbox', { name: 'Comentariu (opțional)' }), '  Foarte bine!  ')
    await user.click(screen.getByRole('button', { name: 'Publică recenzia' }))
    await waitFor(() => expect(reviewsApi.create).toHaveBeenCalledWith(10, 30, { sellerRating: 5, descriptionRating: 4, conditionRating: 3, comment: 'Foarte bine!' }))
    expect(await screen.findByText('Mulțumim! Recenzia ta a fost publicată.')).toBeInTheDocument()
  })

  it('does not offer a form before delivery', async () => {
    vi.mocked(reviewsApi.order).mockResolvedValue([{ ...eligible, canReview: false, ineligibilityReason: 'NOT_DELIVERED' }])
    renderUi(<OrderReviews orderId={10} />)
    expect(await screen.findByText(/Vei putea scrie o recenzie după confirmarea livrării/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Scrie o recenzie' })).not.toBeInTheDocument()
  })

  it('shows the buyers hidden feedback and reason without allowing a duplicate', async () => {
    vi.mocked(reviewsApi.order).mockResolvedValue([{ ...eligible, canReview: false, ineligibilityReason: 'ALREADY_REVIEWED', existingReview: { ...published, moderationStatus: 'HIDDEN', moderationReason: 'Date personale' } }])
    renderUi(<OrderReviews orderId={10} />)
    expect(await screen.findByText(/Recenzia ta este ascunsă/)).toHaveTextContent('Date personale')
    expect(screen.queryByRole('button', { name: 'Scrie o recenzie' })).not.toBeInTheDocument()
  })

  it('preserves input and allows retry when publishing fails', async () => {
    const user = userEvent.setup()
    vi.mocked(reviewsApi.create).mockRejectedValue(new Error('offline'))
    renderUi(<OrderReviews orderId={10} />)
    await user.click(await screen.findByRole('button', { name: 'Scrie o recenzie' }))
    for (const group of screen.getAllByRole('group').filter((group) => group.classList.contains('review-rating-input'))) {
      await user.click(within(group).getByRole('radio', { name: '5 din 5' }))
    }
    await user.type(screen.getByRole('textbox'), 'Text păstrat')
    await user.click(screen.getByRole('button', { name: 'Publică recenzia' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Încearcă din nou')
    expect(screen.getByRole('textbox')).toHaveValue('Text păstrat')
    expect(screen.getByRole('button', { name: 'Publică recenzia' })).toBeEnabled()
  })
})

describe('seller reputation', () => {
  it('does not display a public profile for a buyer-only account', async () => {
    vi.mocked(reviewsApi.reputation).mockRejectedValue({ isAxiosError: true, response: { status: 404 } })
    renderUi(<SellerReputation sellerId={2} />)
    await waitFor(() => expect(screen.queryByRole('region', { name: 'Reputația vânzătorului' })).not.toBeInTheDocument())
  })

  it('does not invent a zero-star reputation for a new seller', async () => {
    renderUi(<SellerReputation sellerId={2} />)
    expect(await screen.findByText('Încă nu există recenzii.')).toBeInTheDocument()
    expect(screen.getByText('0 recenzii verificate')).toBeInTheDocument()
    expect(screen.queryByText('0,0')).not.toBeInTheDocument()
  })

  it('shows all three reputation dimensions and paginates public reviews', async () => {
    const user = userEvent.setup()
    vi.mocked(reviewsApi.reputation).mockResolvedValue({ sellerId: 2, sellerName: 'Ion Ionescu', ratings: { reviewCount: 6, sellerRating: 4.5, descriptionRating: 4, conditionRating: 3.5 } })
    vi.mocked(reviewsApi.publicReviews).mockResolvedValue({ ...pageOf([published.review]), totalElements: 6, totalPages: 2, hasNext: true })
    renderUi(<SellerReputation sellerId={2} />)
    expect(await screen.findByText('6 recenzii verificate')).toBeInTheDocument()
    expect(screen.getByText('3,5 / 5')).toBeInTheDocument()
    await user.click(await screen.findByRole('button', { name: 'Următor' }))
    await waitFor(() => expect(reviewsApi.publicReviews).toHaveBeenCalledWith(2, 1))
  })
})

describe('admin moderation', () => {
  it('requires a reason and sends the visibility change to the moderation endpoint', async () => {
    const user = userEvent.setup()
    vi.mocked(reviewsApi.moderate).mockResolvedValue({ ...published, moderationStatus: 'HIDDEN' })
    renderUi(<AdminReviewsPage />)
    await user.click(await screen.findByRole('button', { name: 'Ascunde recenzia' }))
    expect(screen.getByRole('button', { name: 'Confirmă ascunderea' })).toBeDisabled()
    await user.type(screen.getByRole('textbox', { name: 'Motivul moderării' }), '  Date personale  ')
    await user.click(screen.getByRole('button', { name: 'Confirmă ascunderea' }))
    await waitFor(() => expect(reviewsApi.moderate).toHaveBeenCalledWith(40, 'HIDDEN', 'Date personale'))
    expect(await screen.findByRole('status')).toHaveTextContent('Recenzia a fost ascunsă')
  })

  it('never offers restoration for unverified legacy feedback', async () => {
    vi.mocked(reviewsApi.admin).mockResolvedValue(pageOf([{ ...published, orderItemId: null, moderationStatus: 'HIDDEN', review: { ...published.review, verifiedPurchase: false } }]))
    renderUi(<AdminReviewsPage />)
    expect(await screen.findByText(/Achiziție neverificată/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Restaurează recenzia' })).not.toBeInTheDocument()
  })

  it('can restore verified feedback and filter by visibility', async () => {
    const user = userEvent.setup()
    vi.mocked(reviewsApi.admin).mockResolvedValue(pageOf([{ ...published, moderationStatus: 'HIDDEN' }]))
    vi.mocked(reviewsApi.moderate).mockResolvedValue(published)
    renderUi(<AdminReviewsPage />)
    await user.click(await screen.findByRole('button', { name: 'Restaurează recenzia' }))
    await user.type(screen.getByRole('textbox', { name: 'Motivul moderării' }), 'Contestație acceptată')
    await user.click(screen.getByRole('button', { name: 'Confirmă restaurarea' }))
    await waitFor(() => expect(reviewsApi.moderate).toHaveBeenCalledWith(40, 'VISIBLE', 'Contestație acceptată'))
    await user.selectOptions(screen.getByRole('combobox', { name: 'Vizibilitate' }), 'HIDDEN')
    await waitFor(() => expect(reviewsApi.admin).toHaveBeenCalledWith({ q: '', status: 'HIDDEN', page: 0 }))
  })
})
