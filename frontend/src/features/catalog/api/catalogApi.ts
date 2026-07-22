import { api } from '../../../api/client'
import type { Book, Category } from '../types/catalog.types'

export const catalogApi = {
  async listBooks() {
    const { data } = await api.get<Book[]>('/books')
    return data
  },

  async getBook(bookId: number) {
    const { data } = await api.get<Book>(`/books/${bookId}`)
    return data
  },

  async listCategories() {
    const { data } = await api.get<Category[]>('/categories')
    return data
  },
}
