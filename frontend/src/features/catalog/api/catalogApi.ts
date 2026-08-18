import { api } from '../../../api/client'
import type {
  Book,
  CatalogCategory,
  CatalogRequest,
  Category,
  PageResponse,
} from '../types/catalog.types'

export const catalogApi = {
  async listBooks(params: CatalogRequest = {}) {
    const { data } = await api.get<PageResponse<Book>>('/books', { params })
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

  async listCatalogCategories() {
    const { data } = await api.get<CatalogCategory[]>('/books/catalog-categories')
    return data
  },

  async listLanguages() {
    const { data } = await api.get<string[]>('/books/languages')
    return data
  },
}
