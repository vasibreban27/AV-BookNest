import { getApiError, isNetworkError } from '../../../api/client'

const backendMessageTranslations: Record<string, string> = {
  'This book is already in the wishlist': 'Cartea se află deja în lista de favorite.',
  'Book is not in the wishlist': 'Cartea nu se mai află în lista de favorite.',
  'Book not found': 'Cartea nu a fost găsită.',
}

export function getWishlistErrorMessage(error: unknown) {
  if (isNetworkError(error)) {
    return 'Nu ne putem conecta la server. Verifică dacă backend-ul rulează.'
  }

  const apiError = getApiError(error)
  if (!apiError) return 'Nu am putut actualiza favoritele. Încearcă din nou.'

  return backendMessageTranslations[apiError.message] ?? apiError.message
}
