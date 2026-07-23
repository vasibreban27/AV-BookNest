import { api } from '../../../api/client'
import type { CheckoutPayload, Order } from '../types/orders.types'

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
    const { data } = await api.post<Order>('/orders/checkout', payload)
    return data
  },

  async cancel(orderId: number) {
    const { data } = await api.patch<Order>(`/orders/${orderId}/cancel`)
    return data
  },
}
