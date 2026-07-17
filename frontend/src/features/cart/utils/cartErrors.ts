import { getApiError, isNetworkError } from '../../../api/client'

const backendMessageTranslations: Record<string, string> = {
  'Only available books can be added to the cart': 'Doar cărțile disponibile pot fi adăugate în coș.',
  'You cannot add your own book to the cart': 'Nu poți adăuga în coș propria carte.',
  'This book is already in the cart': 'Cartea se află deja în coș.',
  'Book is not in the cart': 'Cartea nu se mai află în coș.',
  'Book not found': 'Cartea nu a fost găsită.',
}

export function getCartErrorMessage(error: unknown) {
  if (isNetworkError(error)) {
    return 'Nu ne putem conecta la server. Verifică dacă backend-ul rulează.'
  }

  const apiError = getApiError(error)
  if (!apiError) return 'A apărut o eroare. Încearcă din nou.'

  return backendMessageTranslations[apiError.message] ?? apiError.message
}
