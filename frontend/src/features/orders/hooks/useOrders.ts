import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../../auth/hooks/useAuth'
import { cartQueryKey } from '../../cart/hooks/useCart'
import type { Cart } from '../../cart/types/cart.types'
import { ordersApi } from '../api/ordersApi'
import type { CheckoutPayload, Order } from '../types/orders.types'

const orderQueryKeys = {
  list: (userId: number | undefined) => ['orders', 'list', userId] as const,
  detail: (userId: number | undefined, orderId: number) =>
    ['orders', 'detail', userId, orderId] as const,
}

export function useOrders() {
  const { user } = useAuth()

  return useQuery({
    queryKey: orderQueryKeys.list(user?.id),
    queryFn: ordersApi.list,
    enabled: Boolean(user),
  })
}

export function useOrder(orderId: number) {
  const { user } = useAuth()

  return useQuery({
    queryKey: orderQueryKeys.detail(user?.id, orderId),
    queryFn: () => ordersApi.get(orderId),
    enabled: Boolean(user) && Number.isInteger(orderId) && orderId > 0,
  })
}

export function useCheckout() {
  const { user } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: CheckoutPayload) => ordersApi.checkout(payload),
    onError: () => {
      // Checkout-ul poate eșua după ce stocul s-a schimbat pe server.
      // Reîmprospătăm sursele care pot fi afectate înainte de o nouă încercare.
      void queryClient.invalidateQueries({ queryKey: cartQueryKey(user?.id) })
      void queryClient.invalidateQueries({ queryKey: ['catalog', 'books'] })
    },
    onSuccess: (order) => {
      queryClient.setQueryData(orderQueryKeys.detail(user?.id, order.id), order)
      queryClient.setQueryData<Order[]>(orderQueryKeys.list(user?.id), (orders = []) => [
        order,
        ...orders.filter((currentOrder) => currentOrder.id !== order.id),
      ])
      queryClient.setQueryData<Cart>(cartQueryKey(user?.id), (cart) =>
        cart ? { ...cart, items: [], total: 0 } : cart,
      )
      void queryClient.invalidateQueries({ queryKey: cartQueryKey(user?.id) })
      void queryClient.invalidateQueries({ queryKey: ['catalog', 'books'] })
      void queryClient.invalidateQueries({ queryKey: ['wishlist', 'current', user?.id] })
    },
  })
}

export function useCancelOrder() {
  const { user } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ordersApi.cancel,
    onSuccess: (order) => {
      queryClient.setQueryData(orderQueryKeys.detail(user?.id, order.id), order)
      queryClient.setQueryData<Order[]>(orderQueryKeys.list(user?.id), (orders = []) =>
        orders.map((currentOrder) => (currentOrder.id === order.id ? order : currentOrder)),
      )
      void queryClient.invalidateQueries({ queryKey: ['seller-orders', 'seller'] })
      void queryClient.invalidateQueries({ queryKey: ['catalog', 'books'] })
      void queryClient.invalidateQueries({ queryKey: ['listings', 'mine', user?.id] })
      void queryClient.invalidateQueries({ queryKey: ['wishlist', 'current', user?.id] })
    },
  })
}
