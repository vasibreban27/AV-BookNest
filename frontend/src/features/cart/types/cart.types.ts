import type { Book } from '../../catalog/types/catalog.types'

export type CartItem = {
  id: number
  book: Book
  addedAt: string
}

export type Cart = {
  id: number
  items: CartItem[]
  total: number
  createdAt: string
  updatedAt: string
}

export type AddCartItemPayload = {
  bookId: number
}
