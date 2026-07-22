import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../../auth/hooks/useAuth'
import { catalogApi } from '../../catalog/api/catalogApi'
import type { Book } from '../../catalog/types/catalog.types'
import { listingsApi } from '../api/listingsApi'
import type { CreateListingInput, UpdateListingInput } from '../types/listing.types'
import { ListingCreatedWithoutCoverError } from '../utils/listingErrors'

export const listingQueryKeys = {
  mine: (userId: number | undefined) => ['listings', 'mine', userId] as const,
  detail: (userId: number | undefined, bookId: number) => ['listings', 'detail', userId, bookId] as const,
}

function useRefreshListingQueries() {
  const { user } = useAuth()
  const queryClient = useQueryClient()

  return (book?: Book) => {
    if (book) {
      queryClient.setQueryData(listingQueryKeys.detail(user?.id, book.id), book)
      queryClient.setQueryData(['catalog', 'book', book.id], book)
    }
    void queryClient.invalidateQueries({ queryKey: listingQueryKeys.mine(user?.id) })
    void queryClient.invalidateQueries({ queryKey: ['catalog', 'books'] })
    void queryClient.invalidateQueries({ queryKey: ['wishlist', 'current', user?.id] })
  }
}

export function useMyListings() {
  const { user } = useAuth()
  return useQuery({
    queryKey: listingQueryKeys.mine(user?.id),
    queryFn: listingsApi.listMine,
    enabled: Boolean(user),
  })
}

export function useListing(bookId: number) {
  const { user } = useAuth()
  return useQuery({
    queryKey: listingQueryKeys.detail(user?.id, bookId),
    queryFn: () => listingsApi.get(bookId),
    enabled: Boolean(user) && Number.isInteger(bookId) && bookId > 0,
  })
}

export function useListingCategories() {
  return useQuery({
    queryKey: ['catalog', 'categories'],
    queryFn: catalogApi.listCategories,
  })
}

export function useCreateListing() {
  const refreshQueries = useRefreshListingQueries()

  return useMutation({
    mutationFn: async ({ payload, coverFile }: CreateListingInput) => {
      const listing = await listingsApi.create(payload)
      if (!coverFile) return listing
      try {
        return await listingsApi.uploadCover(listing.id, coverFile)
      } catch (error) {
        throw new ListingCreatedWithoutCoverError(listing, error)
      }
    },
    onSuccess: refreshQueries,
    onError: (error) => {
      if (error instanceof ListingCreatedWithoutCoverError) refreshQueries(error.listing)
    },
  })
}

export function useUpdateListing() {
  const refreshQueries = useRefreshListingQueries()

  return useMutation({
    mutationFn: async ({ bookId, payload, coverFile, removeCover }: UpdateListingInput) => {
      let listing = await listingsApi.update(bookId, payload)
      if (coverFile) listing = await listingsApi.uploadCover(bookId, coverFile)
      else if (removeCover) listing = await listingsApi.removeCover(bookId)
      return listing
    },
    onSuccess: refreshQueries,
    onError: () => refreshQueries(),
  })
}

export function useArchiveListing() {
  const refreshQueries = useRefreshListingQueries()
  return useMutation({ mutationFn: listingsApi.archive, onSuccess: refreshQueries })
}

export function usePublishListing() {
  const refreshQueries = useRefreshListingQueries()
  return useMutation({ mutationFn: listingsApi.publish, onSuccess: refreshQueries })
}
