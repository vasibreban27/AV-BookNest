import { api } from '../../../api/client'
import type { Book } from '../../catalog/types/catalog.types'
import type { ListingPayload } from '../types/listing.types'

export const listingsApi = {
  async listMine() {
    const { data } = await api.get<Book[]>('/books/mine')
    return data
  },

  async get(bookId: number) {
    const { data } = await api.get<Book>(`/books/${bookId}`)
    return data
  },

  async create(payload: ListingPayload) {
    const { data } = await api.post<Book>('/books', payload)
    return data
  },

  async update(bookId: number, payload: ListingPayload) {
    const { data } = await api.put<Book>(`/books/${bookId}`, payload)
    return data
  },

  async uploadCover(bookId: number, coverFile: File) {
    const formData = new FormData()
    formData.append('file', coverFile)
    const { data } = await api.post<Book>(`/books/${bookId}/cover`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return data
  },

  async removeCover(bookId: number) {
    const { data } = await api.delete<Book>(`/books/${bookId}/cover`)
    return data
  },

  async archive(bookId: number) {
    const { data } = await api.patch<Book>(`/books/${bookId}/archive`)
    return data
  },

  async publish(bookId: number) {
    const { data } = await api.patch<Book>(`/books/${bookId}/publish`)
    return data
  },
}
