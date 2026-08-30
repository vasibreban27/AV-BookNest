import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { SupportPage } from '../../pages/support/SupportPage'
import { SupportTicketPage } from '../../pages/support/SupportTicketPage'
import { ContactPage } from '../../pages/contact/ContactPage'
import { contactApi } from '../contact/api/contactApi'
import { contactSchema } from '../contact/schemas/contact.schema'
import { supportApi } from './api'
import type { AdminMessage, AdminTicket, SupportPage as Page, SupportTicket } from './types'
import { referenceId } from './utils'

vi.mock('./api', () => ({ supportApi: { list: vi.fn(), ticket: vi.fn(), messages: vi.fn(), administrators: vi.fn(), action: vi.fn() } }))
vi.mock('../contact/api/contactApi', () => ({ contactApi: { send: vi.fn() } }))
const auth = vi.hoisted(() => ({ user: { id: 1, role: 'ADMIN', firstName: 'Ana', lastName: 'Popescu', email: 'ana@test.ro' } as { id: number; role: string; firstName: string; lastName: string; email: string } | null }))
vi.mock('../auth/hooks/useAuth', () => ({ useAuth: () => auth }))
const ticket: SupportTicket = { id: 10, reference: 'BN-SUP-TEST', topic: 'ORDER', subject: 'Ajutor pentru comandă', status: 'NEW', orderId: 20, bookId: null, createdAt: '2026-08-30T10:00:00Z', updatedAt: '2026-08-30T10:00:00Z', closedAt: null, resolvedAt: null, canReply: true }
const adminTicket: AdminTicket = { ticket, requesterId: 1, contactName: 'Ana Popescu', contactEmail: 'ana@test.ro', assignedToId: null, assignedToName: null }
const message = { id: 30, kind: 'REQUESTER' as const, authorName: 'Ana Popescu', body: 'Am nevoie de ajutor cu această comandă.', createdAt: ticket.createdAt }
const pageOf = <T,>(content: T[]): Page<T> => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1, hasNext: false })
function renderPage(path = '/support/10', admin = false) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const result = render(<QueryClientProvider client={client}><MemoryRouter initialEntries={[path]}><Routes>
    <Route path="/support" element={<SupportPage admin={admin} />} /><Route path="/support/:ticketId" element={<SupportTicketPage admin={admin} />} /><Route path="/contact" element={<ContactPage />} />
  </Routes></MemoryRouter></QueryClientProvider>)
  return { ...result, client }
}
beforeEach(() => {
  vi.resetAllMocks()
  auth.user = { id: 1, role: 'ADMIN', firstName: 'Ana', lastName: 'Popescu', email: 'ana@test.ro' }
  vi.mocked(supportApi.list).mockResolvedValue(pageOf([ticket]))
  vi.mocked(supportApi.ticket).mockResolvedValue(ticket)
  vi.mocked(supportApi.messages).mockResolvedValue(pageOf([message]))
  vi.mocked(supportApi.administrators).mockResolvedValue([{ id: 1, name: 'Admin Test' }])
})
describe('support portal', () => {
  it('shows an empty state and a link to create a ticket', async () => {
    vi.mocked(supportApi.list).mockResolvedValue(pageOf([]))
    renderPage('/support')
    expect(await screen.findByText('Nicio solicitare găsită')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Solicitare nouă' })).toHaveAttribute('href', '/contact')
  })
  it('scopes private cache keys to the signed-in user', async () => {
    const { client } = renderPage()
    await screen.findByText(message.body)
    const keys = client.getQueryCache().getAll().filter(q => q.queryKey[0] === 'support')
    expect(keys.length).toBeGreaterThan(0)
    expect(keys.every(q => q.queryKey[1] === 1)).toBe(true)
  })
  it('submits a trimmed reply and clears input after success', async () => {
    const user = userEvent.setup(); renderPage()
    const input = await screen.findByRole('textbox', { name: 'Adaugă un răspuns' })
    expect(screen.getByRole('button', { name: 'Trimite răspunsul' })).toBeDisabled()
    await user.type(input, '  Detalii suplimentare  ')
    await user.click(screen.getByRole('button', { name: 'Trimite răspunsul' }))
    await waitFor(() => expect(supportApi.action).toHaveBeenCalledWith(10, false, { kind: 'reply', body: 'Detalii suplimentare' }))
    expect(await screen.findByText('Răspunsul a fost salvat în conversație.')).toBeInTheDocument()
    expect(input).toHaveValue('')
  })
  it('preserves the draft on network failure', async () => {
    vi.mocked(supportApi.action).mockRejectedValue(new Error('offline'))
    const user = userEvent.setup(); renderPage()
    const input = await screen.findByRole('textbox', { name: 'Adaugă un răspuns' })
    await user.type(input, 'Text păstrat')
    await user.click(screen.getByRole('button', { name: 'Trimite răspunsul' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Încearcă din nou')
    expect(input).toHaveValue('Text păstrat')
  })
  it('does not render a reply form for a closed ticket', async () => {
    vi.mocked(supportApi.ticket).mockResolvedValue({ ...ticket, status: 'CLOSED', canReply: false })
    renderPage()
    expect(await screen.findByText('Solicitarea este închisă.')).toBeInTheDocument()
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
  })
  it('renders a safe access error without fetching messages', async () => {
    vi.mocked(supportApi.ticket).mockRejectedValue({ isAxiosError: true, response: { data: { status: 404 } } })
    renderPage()
    expect(await screen.findByRole('alert')).toHaveTextContent('nu ai acces')
    expect(supportApi.messages).not.toHaveBeenCalled()
  })
  it('pages through older conversation history', async () => {
    vi.mocked(supportApi.messages).mockResolvedValue({ ...pageOf([message]), hasNext: true, totalPages: 2 })
    const user = userEvent.setup(); renderPage()
    await user.click(await screen.findByRole('button', { name: 'Mesaje mai vechi' }))
    await waitFor(() => expect(supportApi.messages).toHaveBeenCalledWith(10, false, 1))
  })
  it('requires an audit reason for status changes', async () => {
    vi.mocked(supportApi.ticket).mockResolvedValue(adminTicket)
    const user = userEvent.setup(); renderPage('/support/10', true)
    const button = await screen.findByRole('button', { name: 'Actualizează starea' })
    expect(button).toBeDisabled()
    await user.selectOptions(screen.getByRole('combobox', { name: 'Stare nouă' }), 'RESOLVED')
    await user.type(screen.getByRole('textbox', { name: 'Motivul schimbării stării' }), '  Problema rezolvată  ')
    await user.click(button)
    await waitFor(() => expect(supportApi.action).toHaveBeenCalledWith(10, true, { kind: 'status', status: 'RESOLVED', reason: 'Problema rezolvată' }))
  })
  it('assigns only through the administrative endpoint', async () => {
    vi.mocked(supportApi.ticket).mockResolvedValue(adminTicket)
    const user = userEvent.setup(); renderPage('/support/10', true)
    await screen.findByRole('option', { name: 'Admin Test' })
    await user.selectOptions(screen.getByRole('combobox', { name: 'Repartizează către' }), '1')
    await user.type(screen.getByRole('textbox', { name: 'Motivul repartizării' }), 'Preluare')
    await user.click(screen.getByRole('button', { name: 'Salvează repartizarea' }))
    await waitFor(() => expect(supportApi.action).toHaveBeenCalledWith(10, true, { kind: 'assignment', administratorId: 1, reason: 'Preluare' }))
  })
  it('shows disabled delivery honestly and allows manual retry', async () => {
    vi.mocked(supportApi.ticket).mockResolvedValue(adminTicket)
    vi.mocked(supportApi.messages).mockResolvedValue(pageOf<AdminMessage>([{ message, emailStatus: 'DISABLED', emailAttempts: 0, emailError: null }]))
    const user = userEvent.setup(); renderPage('/support/10', true)
    expect(await screen.findByText(/trimitere dezactivată/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Reîncearcă emailul' }))
    await waitFor(() => expect(supportApi.action).toHaveBeenCalledWith(10, true, { kind: 'retry', messageId: 30 }))
  })
  it('contact creates a linked ticket and displays its reference', async () => {
    vi.mocked(contactApi.send).mockResolvedValue({ message: 'Înregistrată', ticketId: 10, reference: ticket.reference })
    const user = userEvent.setup(); renderPage('/contact?topic=ORDER&orderId=20')
    await user.type(screen.getByRole('textbox', { name: 'Subiect' }), 'Ajutor comandă')
    await user.type(screen.getByRole('textbox', { name: 'Mesaj' }), message.body)
    await user.click(screen.getByRole('checkbox'))
    await user.click(screen.getByRole('button', { name: 'Trimite mesajul' }))
    await waitFor(() => expect(contactApi.send).toHaveBeenCalledWith(expect.objectContaining({ orderId: 20, topic: 'ORDER', email: 'ana@test.ro' })))
    expect(await screen.findByRole('link', { name: 'Vezi solicitarea' })).toHaveAttribute('href', '/support/10')
    expect(screen.getByText(ticket.reference)).toBeInTheDocument()
  })
  it('rejects malformed, negative and unsafe numeric references', () => {
    for (const value of [null, '', '-1', '0', '2.2', 'abc', '1e3', '9007199254740992']) expect(referenceId(value)).toBeUndefined()
    expect(referenceId('20')).toBe(20)
  })
  it('accepts the longest valid combined account name without blocking the readonly form', () => {
    const payload = { name: 'A'.repeat(100) + ' ' + 'B'.repeat(100), email: 'ana@test.ro', topic: 'GENERAL', subject: 'Ajutor cont', message: message.body, privacyAccepted: true, website: '' }
    expect(contactSchema.safeParse(payload).success).toBe(true)
    expect(contactSchema.safeParse({ ...payload, name: 'A'.repeat(202) }).success).toBe(false)
  })
})
