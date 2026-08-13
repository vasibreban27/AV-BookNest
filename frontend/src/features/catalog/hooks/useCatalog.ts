import { useDeferredValue, useMemo } from 'react'
import { useInfiniteQuery, useQuery } from '@tanstack/react-query'
import { catalogApi } from '../api/catalogApi'
import type { CatalogFilters } from '../types/catalog.types'

const CATALOG_PAGE_SIZE = 5

export function useCatalog({
  searchTerm,
  categorySlug,
  condition,
  sort,
  minimumPrice,
  maximumPrice,
}: CatalogFilters) {
  const deferredSearchTerm = useDeferredValue(searchTerm)
  const booksQuery = useInfiniteQuery({
    queryKey: [
      'catalog',
      'books',
      deferredSearchTerm,
      categorySlug,
      condition,
      sort,
      minimumPrice,
      maximumPrice,
    ],
    queryFn: ({ pageParam }) => catalogApi.listBooks({
      q: deferredSearchTerm.trim() || undefined,
      category: categorySlug || undefined,
      condition: condition || undefined,
      minPrice: minimumPrice ? Number(minimumPrice) : undefined,
      maxPrice: maximumPrice ? Number(maximumPrice) : undefined,
      sort,
      page: pageParam,
      size: CATALOG_PAGE_SIZE,
    }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => lastPage.hasNext ? lastPage.page + 1 : undefined,
  })
  const featuredQuery = useQuery({
    queryKey: ['catalog', 'books', 'featured'],
    queryFn: () => catalogApi.listBooks({ sort: 'newest', page: 0, size: 4 }),
  })
  const categoriesQuery = useQuery({
    queryKey: ['catalog', 'categories'],
    queryFn: catalogApi.listCategories,
  })

  const books = useMemo(
    () => booksQuery.data?.pages.flatMap((page) => page.content) ?? [],
    [booksQuery.data],
  )

  return {
    books,
    totalBooks: booksQuery.data?.pages[0]?.totalElements ?? 0,
    featuredBooks: featuredQuery.data?.content ?? [],
    categories: categoriesQuery.data ?? [],
    isLoading: booksQuery.isLoading || featuredQuery.isLoading || categoriesQuery.isLoading,
    isError: booksQuery.isError || featuredQuery.isError || categoriesQuery.isError,
    hasMore: booksQuery.hasNextPage,
    isLoadingMore: booksQuery.isFetchingNextPage,
    loadMore: () => booksQuery.fetchNextPage(),
    retry: () => {
      void booksQuery.refetch()
      void featuredQuery.refetch()
      void categoriesQuery.refetch()
    },
  }
}
