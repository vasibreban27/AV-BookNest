import type { ContactTopic } from '../contact/types/contact.types'

export const supportStatuses = { NEW: 'Nouă', IN_PROGRESS: 'În lucru', RESOLVED: 'Rezolvată', CLOSED: 'Închisă' } as const
export type SupportStatus = keyof typeof supportStatuses
export type SupportTicket = {
  id: number; reference: string; topic: ContactTopic; subject: string; status: SupportStatus
  orderId: number | null; bookId: number | null; createdAt: string; updatedAt: string
  resolvedAt: string | null; closedAt: string | null; canReply: boolean
}
export type AdminTicket = {
  ticket: SupportTicket; requesterId: number | null; contactName: string; contactEmail: string
  assignedToId: number | null; assignedToName: string | null
}
export type SupportMessage = { id: number; kind: 'REQUESTER' | 'ADMIN' | 'SYSTEM'; authorName: string; body: string; createdAt: string }
export type AdminMessage = {
  message: SupportMessage; emailStatus: 'PENDING' | 'SENT' | 'FAILED' | 'DISABLED' | null; emailAttempts: number; emailError: string | null
}
export type SupportPage<T> = { content: T[]; page: number; size: number; totalElements: number; totalPages: number; hasNext: boolean }
export type SupportFilters = { page: number; status: SupportStatus | ''; q: string; assignment: string }
export type Administrator = { id: number; name: string }
export type SupportAction = { kind: 'reply'; body: string } | { kind: 'status'; status: SupportStatus; reason: string }
  | { kind: 'assignment'; administratorId: number | null; reason: string } | { kind: 'retry'; messageId: number }
