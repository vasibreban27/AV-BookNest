import { getApiError, isNetworkError } from '../../../api/client'
import type {
  BookModerationReason,
  BookModerationStatus,
  IssueResolution,
} from '../types/admin.types'

const dateFormatter = new Intl.DateTimeFormat('ro-RO', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

const numberFormatter = new Intl.NumberFormat('ro-RO')

export const moderationReasonLabels: Record<BookModerationReason, string> = {
  POLICY_VIOLATION: 'Încălcarea regulilor',
  SPAM: 'Spam',
  FRAUD_SUSPECTED: 'Suspiciune de fraudă',
  ACCOUNT_SUSPENDED: 'Cont suspendat',
  OTHER: 'Alt motiv',
}

export const moderationStatusLabels: Record<BookModerationStatus, string> = {
  VISIBLE: 'Vizibil',
  HIDDEN: 'Ascuns',
}

export const issueResolutionLabels: Record<IssueResolution, string> = {
  REFUND_BUYER: 'Rambursează cumpărătorul',
  RELEASE_SELLER_PAYOUT: 'Eliberează plata vânzătorului',
}

export function formatAdminDate(value: string | null | undefined) {
  return value ? dateFormatter.format(new Date(value)) : '—'
}

export function formatAdminNumber(value: number) {
  return numberFormatter.format(value)
}

export function formatAdminMoney(value: number, currency = 'RON') {
  return new Intl.NumberFormat('ro-RO', {
    style: 'currency',
    currency,
    maximumFractionDigits: 2,
  }).format(value)
}

export function getAdminErrorMessage(error: unknown) {
  const apiError = getApiError(error)
  if (apiError?.message) return apiError.message
  if (isNetworkError(error)) return 'Backend-ul nu poate fi contactat. Verifică dacă rulează.'
  return 'Acțiunea nu a putut fi finalizată. Încearcă din nou.'
}

export function formatAuditAction(action: string) {
  return action.toLowerCase().split('_').map((part) => part[0].toUpperCase() + part.slice(1)).join(' ')
}
