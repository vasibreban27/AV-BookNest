import { api } from '../../../api/client'
import type {
  CheckoutPayload,
  Order,
  OrderStatus,
  SellerOrder,
  StripeCheckoutSession,
} from '../types/orders.types'

export type OrderFilters = {
  status?: OrderStatus
  query?: string
}

export const ordersApi = {
  async list(filters: OrderFilters = {}) {
    const { data } = await api.get<Order[]>('/orders', {
      params: { status: filters.status, q: filters.query?.trim() || undefined },
    })
    return data
  },

  async get(orderId: number) {
    const { data } = await api.get<Order>(`/orders/${orderId}`)
    return data
  },

  async checkout(payload: CheckoutPayload) {
    const { data } = await api.post<StripeCheckoutSession>('/orders/checkout', payload)
    return data
  },

  async cancel(orderId: number) {
    const { data } = await api.patch<Order>(`/orders/${orderId}/cancel`)
    return data
  },

  async reportIssue(orderId: number, sellerOrderId: number, reason: string) {
    const { data } = await api.post<SellerOrder>(
      `/orders/${orderId}/seller-orders/${sellerOrderId}/issue`,
      { reason },
    )
    return data
  },

  async resolveIssue(orderId: number, sellerOrderId: number) {
    const { data } = await api.patch<SellerOrder>(
      `/orders/${orderId}/seller-orders/${sellerOrderId}/issue/resolve`,
    )
    return data
  },
}
