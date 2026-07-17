import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../../auth/hooks/useAuth'
import { cartApi } from '../api/cartApi'
import type { Cart } from '../types/cart.types'

const cartQueryKey = (userId: number | undefined) => ['cart', 'current', userId] as const

export function useCart() {
  const { user } = useAuth()

  return useQuery({
    queryKey: cartQueryKey(user?.id),
    queryFn: cartApi.getCart,
    enabled: Boolean(user),
  })
}

export function useAddToCart() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const queryKey = cartQueryKey(user?.id)

  return useMutation({
    mutationFn: (bookId: number) => cartApi.addItem({ bookId }),
    onSuccess: (cart) => {
      queryClient.setQueryData(queryKey, cart)
    },
  })
}

export function useRemoveFromCart() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const queryKey = cartQueryKey(user?.id)

  return useMutation({
    mutationFn: cartApi.removeItem,
    onMutate: async (bookId) => {
      await queryClient.cancelQueries({ queryKey })
      const previousCart = queryClient.getQueryData<Cart>(queryKey)

      queryClient.setQueryData<Cart>(queryKey, (currentCart) => {
        if (!currentCart) return currentCart
        const items = currentCart.items.filter((item) => item.book.id !== bookId)
        const total = items.reduce((sum, item) => sum + item.book.price, 0)
        return { ...currentCart, items, total }
      })

      return { previousCart }
    },
    onError: (_error, _bookId, context) => {
      if (context?.previousCart) {
        queryClient.setQueryData(queryKey, context.previousCart)
      }
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey }),
  })
}

export function useClearCart() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const queryKey = cartQueryKey(user?.id)

  return useMutation({
    mutationFn: cartApi.clearCart,
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey })
      const previousCart = queryClient.getQueryData<Cart>(queryKey)

      queryClient.setQueryData<Cart>(queryKey, (currentCart) =>
        currentCart ? { ...currentCart, items: [], total: 0 } : currentCart,
      )

      return { previousCart }
    },
    onError: (_error, _variables, context) => {
      if (context?.previousCart) {
        queryClient.setQueryData(queryKey, context.previousCart)
      }
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey }),
  })
}
