import { api } from '../../../api/client'
import type { PackageSize, SellerOrder } from '../../orders/types/orders.types'

export type AcceptSellerOrderInput = {
  sellerOrderId: number
  packageSize: PackageSize
}

export const sellerOrdersApi = {
  async listMine() {
    const { data } = await api.get<SellerOrder[]>('/seller-orders/mine')
    return data
  },

  async accept({ sellerOrderId, packageSize }: AcceptSellerOrderInput) {
    const { data } = await api.patch<SellerOrder>(
      `/seller-orders/${sellerOrderId}/accept`,
      { packageSize },
    )
    return data
  },

  async cancel(sellerOrderId: number) {
    const { data } = await api.patch<SellerOrder>(`/seller-orders/${sellerOrderId}/cancel`)
    return data
  },
}
