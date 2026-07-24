import { useQuery } from '@tanstack/react-query'
import type { CheckoutPayload } from '../../orders/types/orders.types'
import { shippingApi } from '../api/shippingApi'

export function useShippingQuote(payload: CheckoutPayload | null) {
  return useQuery({
    queryKey: ['shipping', 'quote', payload],
    queryFn: () => shippingApi.quote(payload!),
    enabled: payload !== null,
    staleTime: 60 * 1000,
    retry: false,
  })
}
