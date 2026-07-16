import { useDeferredValue, useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { catalogApi } from '../api/catalogApi'
import type { CatalogFilters } from '../types/catalog.types'
import { normalizeCatalogText } from '../utils/catalogFormatters'

export function useCatalog({ searchTerm, categorySlug }: CatalogFilters) {
  const deferredSearchTerm = useDeferredValue(searchTerm)
  const booksQuery = useQuery({
    queryKey: ['catalog', 'books'],
    queryFn: catalogApi.listBooks,
  })
  const categoriesQuery = useQuery({
    queryKey: ['catalog', 'categories'],
    queryFn: catalogApi.listCategories,
  })

  const availableBooks = useMemo(
    () =>
      (booksQuery.data ?? [])
        .filter((book) => book.status === 'AVAILABLE')
        .sort(
          (first, second) =>
            new Date(second.createdAt).getTime() - new Date(first.createdAt).getTime(),
        ),
    [booksQuery.data],
  )

  const filteredBooks = useMemo(() => {
    const normalizedSearch = normalizeCatalogText(deferredSearchTerm)

    return availableBooks.filter((book) => {
      const matchesCategory =
        !categorySlug || book.category.slug === categorySlug
      const searchableValue = normalizeCatalogText(`${book.title} ${book.author}`)
      const matchesSearch =
        !normalizedSearch || searchableValue.includes(normalizedSearch)
      return matchesCategory && matchesSearch
    })
  }, [availableBooks, categorySlug, deferredSearchTerm])

  return {
    books: filteredBooks,
    featuredBooks: availableBooks.slice(0, 8),
    categories: categoriesQuery.data ?? [],
    isLoading: booksQuery.isLoading || categoriesQuery.isLoading,
    isError: booksQuery.isError || categoriesQuery.isError,
    retry: () => {
      void booksQuery.refetch()
      void categoriesQuery.refetch()
    },
  }
}
