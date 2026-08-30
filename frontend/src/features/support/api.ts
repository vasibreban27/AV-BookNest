import { api } from '../../api/client'
import type { AdminMessage, Administrator, AdminTicket, SupportAction, SupportFilters, SupportMessage, SupportPage, SupportTicket } from './types'

const base = (admin: boolean) => admin ? '/admin/support/tickets' : '/support/tickets'
export const supportApi = {
  async list(admin: boolean, filters: SupportFilters): Promise<SupportPage<AdminTicket | SupportTicket>> {
    return (await api.get(base(admin), { params: { page: filters.page, size: 20, status: filters.status || undefined,
      ...(admin ? { q: filters.q, unassigned: filters.assignment === 'unassigned', assignedToId: /^\d+$/.test(filters.assignment) ? Number(filters.assignment) : undefined } : {}) } })).data
  },
  async ticket(id: number, admin: boolean): Promise<AdminTicket | SupportTicket> { return (await api.get(`${base(admin)}/${id}`)).data },
  async messages(id: number, admin: boolean, page: number): Promise<SupportPage<AdminMessage | SupportMessage>> {
    return (await api.get(`${base(admin)}/${id}/messages`, { params: { page, size: 30 } })).data
  },
  async administrators() { return (await api.get<Administrator[]>('/admin/support/administrators')).data },
  async action(id: number, admin: boolean, action: SupportAction) {
    const url = `${base(admin)}/${id}`
    if (action.kind === 'reply') return api.post(`${url}/messages`, { body: action.body })
    if (!admin) throw new Error('Administrator access required')
    if (action.kind === 'status') return api.patch(`${url}/status`, { status: action.status, reason: action.reason })
    if (action.kind === 'assignment') return api.patch(`${url}/assignment`, { administratorId: action.administratorId, reason: action.reason })
    return api.post(`${url}/messages/${action.messageId}/retry-email`)
  },
}
