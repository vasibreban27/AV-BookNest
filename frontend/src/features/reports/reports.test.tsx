import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CreateReportPage } from '../../pages/reports/CreateReportPage'
import { ReportDetailsPage } from '../../pages/reports/ReportDetailsPage'
import { ReportsPage } from '../../pages/reports/ReportsPage'
import { ReportLink } from '../../components/reports/ReportLink'
import { evidenceError, reportsApi, type ContentReport, type ReportDetails } from './api'
vi.mock('./api', async importOriginal => ({ ...await importOriginal<typeof import('./api')>(), reportsApi: { create: vi.fn(), list: vi.fn(), details: vi.fn(), claim: vi.fn(), resolve: vi.fn(), history: vi.fn(), evidence: vi.fn() } }))
const auth = vi.hoisted(() => ({ user: { id: 1, role: 'ADMIN' } }))
vi.mock('../auth/hooks/useAuth', () => ({ useAuth: () => auth }))
const report: ContentReport = { id: 10, reporterId: 1, targetType: 'BOOK', targetUserId: 2, bookId: 100, targetLabel: 'Carte raportată', reason: 'FRAUD', description: 'Descriere detaliată.', status: 'NEW', assignedToId: null, decision: null, decisionNote: null, suspendedUntil: null, createdAt: '2026-09-06T10:00:00Z', updatedAt: '2026-09-06T10:00:00Z' }
const details: ReportDetails = { report, evidence: [], events: [{ id: 1, actorId: 1, action: 'CREATED', note: 'Raportare trimisă', createdAt: report.createdAt }] }
function show(path = '/reports/10', admin = false) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(<QueryClientProvider client={client}><MemoryRouter initialEntries={[path]}><Routes>
    <Route path="/report" element={<CreateReportPage />} /><Route path="/reports" element={<ReportsPage admin={admin} />} /><Route path="/reports/:reportId" element={<ReportDetailsPage admin={admin} />} />
  </Routes></MemoryRouter></QueryClientProvider>)
}
beforeEach(() => {
  vi.resetAllMocks()
  auth.user = { id: 1, role: 'ADMIN' }
  vi.mocked(reportsApi.details).mockResolvedValue(details)
  vi.mocked(reportsApi.list).mockResolvedValue({ content: [], page: 0, size: 20, totalPages: 0, totalElements: 0, hasNext: false })
  vi.mocked(reportsApi.history).mockResolvedValue({ enabled: true, suspendedUntil: null, suspensionReason: null, entries: [] })
})
describe('content reporting', () => {
  it('validates evidence count size and type before uploading', () => {
    const png = new File(['png'], 'image.png', { type: 'image/png' })
    expect(evidenceError([png])).toBeNull()
    expect(evidenceError([png, png, png, png])).toMatch(/maximum 3/)
    expect(evidenceError([new File(['<svg/>'], 'script.svg', { type: 'image/svg+xml' })])).toMatch(/PNG sau JPEG/)
    expect(evidenceError([new File([new Uint8Array(2 * 1024 * 1024 + 1)], 'large.png', { type: 'image/png' })])).toMatch(/2 MB/)
  })
  it('requires a valid target instead of showing a submit form', () => {
    show('/report?targetType=BOOK&targetId=oops')
    expect(screen.getByRole('heading', { name: 'Alege conținutul de raportat' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Trimite raportarea' })).not.toBeInTheDocument()
  })
  it('submits the target and photo then opens the persisted report', async () => {
    vi.mocked(reportsApi.create).mockResolvedValue(report)
    show('/report?targetType=BOOK&targetId=100')
    const user = userEvent.setup()
    await user.selectOptions(screen.getByLabelText('Motivul raportării'), 'FRAUD')
    await user.type(screen.getByLabelText('Descriere suplimentară'), 'Dovezi pentru această problemă.')
    const photo = new File(['png'], 'photo.png', { type: 'image/png' })
    await user.upload(screen.getByLabelText('Fotografii / dovezi (opțional)'), photo)
    await user.click(screen.getByRole('button', { name: 'Trimite raportarea' }))
    await screen.findByRole('heading', { name: 'Raportare #10' })
    expect(reportsApi.create).toHaveBeenCalledWith('BOOK', 100, 'FRAUD', 'Dovezi pentru această problemă.', [photo])
  })
  it('keeps the draft when submission fails and validates Other', async () => {
    vi.mocked(reportsApi.create).mockRejectedValue(new Error('offline'))
    show('/report?targetType=USER&targetId=2')
    const user = userEvent.setup()
    await user.selectOptions(screen.getByLabelText('Motivul raportării'), 'OTHER')
    await user.type(screen.getByLabelText('Descriere suplimentară'), 'scurt')
    await user.click(screen.getByRole('button', { name: 'Trimite raportarea' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('cel puțin 10')
    expect(reportsApi.create).not.toHaveBeenCalled()
    await user.type(screen.getByLabelText('Descriere suplimentară'), ' dar acum suficient de lung')
    await user.click(screen.getByRole('button', { name: 'Trimite raportarea' }))
    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('conexiunea'))
    expect(screen.getByLabelText('Descriere suplimentară')).toHaveValue('scurt dar acum suficient de lung')
  })
  it('shows no administrative tools or history to the reporter', async () => {
    auth.user.role = 'USER'
    show()
    await screen.findByRole('heading', { name: 'Raportare #10' })
    expect(screen.queryByRole('heading', { name: 'Decizie de moderare' })).not.toBeInTheDocument()
    expect(reportsApi.history).not.toHaveBeenCalled()
  })
  it('requires confirmation and sends the suspension duration', async () => {
    vi.mocked(reportsApi.resolve).mockResolvedValue(details)
    show('/reports/10', true)
    const user = userEvent.setup()
    await screen.findByRole('heading', { name: 'Decizie de moderare' })
    await user.selectOptions(screen.getByLabelText('Acțiune'), 'SUSPEND')
    await user.clear(screen.getByLabelText('Durata suspendării (zile)'))
    await user.type(screen.getByLabelText('Durata suspendării (zile)'), '14')
    await user.type(screen.getByLabelText('Motivul deciziei'), 'Abatere confirmată după verificare')
    expect(screen.getByRole('button', { name: 'Confirmă decizia' })).toBeDisabled()
    await user.click(screen.getByRole('checkbox'))
    await user.click(screen.getByRole('button', { name: 'Confirmă decizia' }))
    await waitFor(() => expect(reportsApi.resolve).toHaveBeenCalledWith(10, { decision: 'SUSPEND', note: 'Abatere confirmată după verificare', suspensionDays: 14 }))
  })
  it('does not allow a second decision on a resolved report', async () => {
    vi.mocked(reportsApi.details).mockResolvedValue({ ...details, report: { ...report, status: 'DISMISSED', decision: 'DISMISS', decisionNote: 'Nu se confirmă' } })
    show('/reports/10', true)
    await screen.findByText('Raportarea a fost respinsă.')
    expect(screen.queryByRole('button', { name: 'Confirmă decizia' })).not.toBeInTheDocument()
  })
  it('renders the empty queue', async () => {
    show('/reports', true)
    expect(await screen.findByText('Nicio raportare găsită')).toBeInTheDocument()
  })
  it('does not offer self reporting', () => {
    render(<MemoryRouter><ReportLink targetType="BOOK" targetId={10} ownerId={1} /></MemoryRouter>)
    expect(screen.queryByRole('link')).not.toBeInTheDocument()
  })
})
