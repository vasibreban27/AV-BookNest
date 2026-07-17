import { api } from '../../../api/client'
import type { AddCartItemPayload, Cart } from '../types/cart.types'

export const cartApi = {
  async getCart() {
    const { data } = await api.get<Cart>('/cart')
    return data
  },

  async addItem(payload: AddCartItemPayload) {
    const { data } = await api.post<Cart>('/cart/items', payload)
    return data
  },

  async removeItem(bookId: number) {
    await api.delete(`/cart/items/${bookId}`)
  },

  async clearCart() {
    await api.delete('/cart')
  },
}
