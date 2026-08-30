import { getApiError, isNetworkError } from '../../api/client'
import type { AdminTicket, SupportTicket } from './types'

export function unwrapTicket(entry: AdminTicket | SupportTicket): SupportTicket {
  return 'ticket' in entry ? entry.ticket : entry
}

export function supportError(error: unknown) {
  if (isNetworkError(error)) return 'Conexiunea cu serverul s-a întrerupt. Încearcă din nou.'
  const status = getApiError(error)?.status
  if (status === 404) return 'Solicitarea nu există sau nu ai acces la ea.'
  if (status === 403) return 'Nu ai permisiunea necesară pentru această acțiune.'
  if (status === 429) return 'Ai trimis prea multe mesaje. Încearcă din nou mai târziu.'
  if (status === 409) return 'Acțiunea nu mai este disponibilă. Verifică starea solicitării; pentru email, administratorul trebuie să activeze SMTP înainte de reîncercare.'
  if (status === 400) return 'Verifică datele introduse și referințele la comandă sau anunț.'
  return 'Nu am putut finaliza acțiunea. Încearcă din nou.'
}
export function supportDate(value: string) { return new Date(value).toLocaleString('ro-RO', { dateStyle: 'medium', timeStyle: 'short' }) }
export function referenceId(value: string | null) {
  if (!value || !/^\d+$/.test(value)) return undefined
  const id = Number(value)
  return Number.isSafeInteger(id) && id > 0 ? id : undefined
}
