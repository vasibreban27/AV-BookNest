import { api } from '../../../api/client'
import type { Easybox } from '../../orders/types/orders.types'

export const shippingApi = {
  async searchEasyboxes(query: string) {
    const { data } = await api.get<Easybox[]>('/shipping/easyboxes', {
      params: { query },
    })
    return data
  },
}
