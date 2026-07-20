import { getApiError, isNetworkError } from '../../../api/client'

const backendMessageTranslations: Record<string, string> = {
  'Your cart is empty': 'Coșul este gol. Adaugă cel puțin o carte înainte de checkout.',
  'Order not found': 'Comanda nu a fost găsită.',
  'Book not found': 'Una dintre cărți nu mai este disponibilă.',
}

export function getOrderErrorMessage(error: unknown) {
  if (isNetworkError(error)) {
    return 'Nu ne putem conecta la server. Verifică dacă backend-ul rulează.'
  }

  const apiError = getApiError(error)
  if (!apiError) return 'Nu am putut procesa comanda. Încearcă din nou.'

  if (apiError.message.includes('is no longer available')) {
    return 'Una dintre cărțile din coș nu mai este disponibilă. Actualizează coșul și încearcă din nou.'
  }

  return backendMessageTranslations[apiError.message] ?? apiError.message
}
