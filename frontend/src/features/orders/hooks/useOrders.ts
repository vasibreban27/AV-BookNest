import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../../auth/hooks/useAuth'
import { cartQueryKey } from '../../cart/hooks/useCart'
import type { Cart } from '../../cart/types/cart.types'
import { ordersApi } from '../api/ordersApi'
import type { OrderFilters } from '../api/ordersApi'
import type { CheckoutPayload, Order } from '../types/orders.types'

const orderQueryKeys = {
  list: (userId: number | undefined, filters?: OrderFilters) =>
    ['orders', 'list', userId, filters ?? {}] as const,
  detail: (userId: number | undefined, orderId: number) =>
    ['orders', 'detail', userId, orderId] as const,
}

export function useOrders(filters: OrderFilters = {}) {
  const { user } = useAuth()

  return useQuery({
    queryKey: orderQueryKeys.list(user?.id, filters),
    queryFn: () => ordersApi.list(filters),
    enabled: Boolean(user),
  })
}

export function useOrder(orderId: number) {
  const { user } = useAuth()

  return useQuery({
    queryKey: orderQueryKeys.detail(user?.id, orderId),
    queryFn: () => ordersApi.get(orderId),
    enabled: Boolean(user) && Number.isInteger(orderId) && orderId > 0,
    refetchInterval: (query) =>
      query.state.data?.payment?.status === 'PENDING' ? 2_000 : false,
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
    onSuccess: (session) => {
      const order = session.order
      queryClient.setQueryData(orderQueryKeys.detail(user?.id, order.id), order)
      void queryClient.invalidateQueries({ queryKey: ['orders', 'list', user?.id] })
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
      void queryClient.invalidateQueries({ queryKey: ['orders', 'list', user?.id] })
      void queryClient.invalidateQueries({ queryKey: ['seller-orders', 'seller'] })
      void queryClient.invalidateQueries({ queryKey: ['catalog', 'books'] })
      void queryClient.invalidateQueries({ queryKey: ['listings', 'mine', user?.id] })
      void queryClient.invalidateQueries({ queryKey: ['wishlist', 'current', user?.id] })
    },
  })
}

export function useReportOrderIssue(orderId: number) {
  const { user } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ sellerOrderId, reason }: { sellerOrderId: number; reason: string }) =>
      ordersApi.reportIssue(orderId, sellerOrderId, reason),
    onSuccess: (sellerOrder) => {
      queryClient.setQueryData<Order>(
        orderQueryKeys.detail(user?.id, orderId),
        (order) => order ? {
          ...order,
          sellerOrders: order.sellerOrders.map((item) =>
            item.id === sellerOrder.id ? sellerOrder : item,
          ),
        } : order,
      )
    },
  })
}

export function useResolveOrderIssue(orderId: number) {
  const { user } = useAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (sellerOrderId: number) => ordersApi.resolveIssue(orderId, sellerOrderId),
    onSuccess: (sellerOrder) => {
      queryClient.setQueryData<Order>(
        orderQueryKeys.detail(user?.id, orderId),
        (order) => order ? {
          ...order,
          sellerOrders: order.sellerOrders.map((item) =>
            item.id === sellerOrder.id ? sellerOrder : item,
          ),
        } : order,
      )
    },
  })
}
