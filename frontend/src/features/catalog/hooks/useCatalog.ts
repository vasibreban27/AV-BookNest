import { useDeferredValue, useMemo } from 'react'
import { useInfiniteQuery, useQuery } from '@tanstack/react-query'
import { catalogApi } from '../api/catalogApi'
import type { CatalogFilters } from '../types/catalog.types'

export function useCatalog({
  searchTerm,
  categorySlug,
  condition,
  sort,
  minimumPrice,
  maximumPrice,
  language,
  minimumYear,
  maximumYear,
}: CatalogFilters, pageSize = 5, enabled = true) {
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
      language,
      minimumYear,
      maximumYear,
      pageSize,
    ],
    queryFn: ({ pageParam }) => catalogApi.listBooks({
      q: deferredSearchTerm.trim() || undefined,
      category: categorySlug || undefined,
      condition: condition || undefined,
      minPrice: minimumPrice ? Number(minimumPrice) : undefined,
      maxPrice: maximumPrice ? Number(maximumPrice) : undefined,
      language: language || undefined,
      minYear: minimumYear ? Number(minimumYear) : undefined,
      maxYear: maximumYear ? Number(maximumYear) : undefined,
      sort,
      page: pageParam,
      size: pageSize,
    }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) => lastPage.hasNext ? lastPage.page + 1 : undefined,
    enabled,
  })
  const featuredQuery = useQuery({
    queryKey: ['catalog', 'books', 'featured'],
    queryFn: () => catalogApi.listBooks({ sort: 'newest', page: 0, size: 4 }),
  })
  const categoriesQuery = useQuery({
    queryKey: ['catalog', 'available-categories'],
    queryFn: catalogApi.listCatalogCategories,
  })
  const languagesQuery = useQuery({
    queryKey: ['catalog', 'languages'],
    queryFn: catalogApi.listLanguages,
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
    languages: languagesQuery.data ?? [],
    isLoading:
      booksQuery.isLoading ||
      featuredQuery.isLoading ||
      categoriesQuery.isLoading ||
      languagesQuery.isLoading,
    isError:
      booksQuery.isError ||
      featuredQuery.isError ||
      categoriesQuery.isError ||
      languagesQuery.isError,
    hasMore: booksQuery.hasNextPage,
    isLoadingMore: booksQuery.isFetchingNextPage,
    loadMore: () => booksQuery.fetchNextPage(),
    retry: () => {
      void booksQuery.refetch()
      void featuredQuery.refetch()
      void categoriesQuery.refetch()
      void languagesQuery.refetch()
    },
  }
}
