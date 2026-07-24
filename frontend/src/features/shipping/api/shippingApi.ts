import { api } from '../../../api/client'
import type {
  CheckoutPayload,
  Easybox,
  ShippingQuote,
} from '../../orders/types/orders.types'

export const shippingApi = {
  async searchEasyboxes(query: string) {
    const { data } = await api.get<Easybox[]>('/shipping/easyboxes', {
      params: { query },
    })
    return data
  },

  async quote(payload: CheckoutPayload) {
    const { data } = await api.post<ShippingQuote>('/shipping/quote', payload)
    return data
  },
}
