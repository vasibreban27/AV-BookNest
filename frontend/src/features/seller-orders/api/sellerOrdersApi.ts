import { api } from '../../../api/client'
import type { SellerOrder } from '../../orders/types/orders.types'

export type AcceptSellerOrderInput = {
  sellerOrderId: number
}

export const sellerOrdersApi = {
  async listMine() {
    const { data } = await api.get<SellerOrder[]>('/seller-orders/mine')
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
