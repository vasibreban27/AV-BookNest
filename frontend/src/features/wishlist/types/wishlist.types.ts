import type { Book } from '../../catalog/types/catalog.types'

export type WishlistItem = {
  book: Book
  addedAt: string
}
