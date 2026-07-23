import { getApiError, isNetworkError } from '../../../api/client'

const translations: Record<string, string> = {
  'Sale not found': 'Vânzarea nu a fost găsită.',
  'This sale can no longer be accepted': 'Vânzarea nu mai poate fi acceptată.',
  'The 24 hour acceptance window has expired': 'Termenul de acceptare de 24h a expirat.',
  'This sale can no longer be cancelled': 'Vânzarea nu mai poate fi anulată.',
}

export function getSellerOrderErrorMessage(error: unknown) {
  if (isNetworkError(error)) return 'Nu ne putem conecta la server.'
  const apiError = getApiError(error)
  if (!apiError) return 'Acțiunea nu a putut fi finalizată.'
  return translations[apiError.message] ?? apiError.message
}
