import { getApiError, isNetworkError } from '../../../api/client'
import type { Book } from '../../catalog/types/catalog.types'

const backendMessageTranslations: Record<string, string> = {
  'Book not found': 'Cartea nu a fost găsită.',
  'Category not found': 'Categoria selectată nu mai există.',
  'Only the seller can modify this book': 'Doar vânzătorul poate modifica această carte.',
  'Reserved or sold books cannot be modified': 'Cărțile rezervate sau vândute nu mai pot fi modificate.',
  'Only available books can be archived': 'Doar cărțile disponibile pot fi arhivate.',
  'Only draft or archived books can be published': 'Doar ciornele sau cărțile arhivate pot fi publicate.',
  'Cloudinary is not configured': 'Serviciul pentru imagini nu este configurat.',
  'The cover image could not be uploaded': 'Coperta nu a putut fi încărcată. Încearcă din nou.',
  'The cover image content does not match its file type': 'Fișierul selectat nu este o imagine validă.',
}

export class ListingCreatedWithoutCoverError extends Error {
  readonly listing: Book
  readonly originalError: unknown

  constructor(listing: Book, originalError: unknown) {
    super('Listing created without cover')
    this.name = 'ListingCreatedWithoutCoverError'
    this.listing = listing
    this.originalError = originalError
  }
}

export function getListingErrorMessage(error: unknown) {
  if (error instanceof ListingCreatedWithoutCoverError) {
    return getListingErrorMessage(error.originalError)
  }
  if (isNetworkError(error)) {
    return 'Nu ne putem conecta la server. Verifică dacă backend-ul rulează.'
  }

  const apiError = getApiError(error)
  if (!apiError) return 'Operațiunea nu a putut fi finalizată. Încearcă din nou.'

  return backendMessageTranslations[apiError.message] ?? apiError.message
}
