import { useQuery } from '@tanstack/react-query'
import { catalogApi } from '../api/catalogApi'

export const bookDetailsQueryKey = (bookId: number) =>
  ['catalog', 'book', bookId] as const

export function useBook(bookId: number) {
  return useQuery({
    queryKey: bookDetailsQueryKey(bookId),
    queryFn: () => catalogApi.getBook(bookId),
    enabled: Number.isInteger(bookId) && bookId > 0,
  })
}
