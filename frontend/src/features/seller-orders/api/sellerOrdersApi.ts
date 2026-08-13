import { api } from '../../../api/client'
import type { SellerOrder, SellerOrderStatus } from '../../orders/types/orders.types'

export type AcceptSellerOrderInput = {
  sellerOrderId: number
}

export type SellerOrderFilters = {
  status?: SellerOrderStatus
  query?: string
}

export const sellerOrdersApi = {
  async listMine(filters: SellerOrderFilters = {}) {
    const { data } = await api.get<SellerOrder[]>('/seller-orders/mine', {
      params: { status: filters.status, q: filters.query?.trim() || undefined },
    })
    return data
  },

  async accept({ sellerOrderId }: AcceptSellerOrderInput) {
    const { data } = await api.patch<SellerOrder>(
      `/seller-orders/${sellerOrderId}/accept`,
    )
    return data
  },

  async cancel(sellerOrderId: number) {
    const { data } = await api.patch<SellerOrder>(`/seller-orders/${sellerOrderId}/cancel`)
    return data
  },
}
