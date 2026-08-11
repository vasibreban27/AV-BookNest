import { api } from '../../../api/client'
import type { CheckoutPayload, Order, SellerOrder, StripeCheckoutSession } from '../types/orders.types'

export const ordersApi = {
  async list() {
    const { data } = await api.get<Order[]>('/orders')
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
