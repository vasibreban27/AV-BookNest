import { api } from '../../../api/client'
import type { WishlistItem } from '../types/wishlist.types'

export const wishlistApi = {
  async list() {
    const { data } = await api.get<WishlistItem[]>('/wishlist')
    return data
  },

  async add(bookId: number) {
    const { data } = await api.post<WishlistItem>(`/wishlist/books/${bookId}`)
    return data
  },

  async remove(bookId: number) {
    await api.delete(`/wishlist/books/${bookId}`)
  },
}
