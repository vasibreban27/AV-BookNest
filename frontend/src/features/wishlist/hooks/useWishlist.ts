import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../../auth/hooks/useAuth'
import { wishlistApi } from '../api/wishlistApi'
import type { WishlistItem } from '../types/wishlist.types'

const wishlistQueryKey = (userId: number | undefined) => ['wishlist', 'current', userId] as const

export function useWishlist() {
  const { user } = useAuth()

  return useQuery({
    queryKey: wishlistQueryKey(user?.id),
    queryFn: wishlistApi.list,
    enabled: Boolean(user),
  })
}

export function useAddToWishlist() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const queryKey = wishlistQueryKey(user?.id)

  return useMutation({
    mutationFn: wishlistApi.add,
    onSuccess: (item) => {
      queryClient.setQueryData<WishlistItem[]>(queryKey, (currentItems = []) => [
        item,
        ...currentItems.filter((currentItem) => currentItem.book.id !== item.book.id),
      ])
    },
  })
}

export function useRemoveFromWishlist() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const queryKey = wishlistQueryKey(user?.id)

  return useMutation({
    mutationFn: wishlistApi.remove,
    onMutate: async (bookId) => {
      await queryClient.cancelQueries({ queryKey })
      const previousItems = queryClient.getQueryData<WishlistItem[]>(queryKey)
      queryClient.setQueryData<WishlistItem[]>(queryKey, (currentItems = []) =>
        currentItems.filter((item) => item.book.id !== bookId),
      )
      return { previousItems }
    },
    onError: (_error, _bookId, context) => {
      if (context?.previousItems) {
        queryClient.setQueryData(queryKey, context.previousItems)
      }
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey }),
  })
}
