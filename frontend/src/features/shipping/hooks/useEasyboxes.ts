import { useQuery } from '@tanstack/react-query'
import { shippingApi } from '../api/shippingApi'

export function useEasyboxes(query: string) {
  const normalizedQuery = query.trim()
  return useQuery({
    queryKey: ['shipping', 'easyboxes', normalizedQuery],
    queryFn: () => shippingApi.searchEasyboxes(normalizedQuery),
    enabled: normalizedQuery.length >= 3,
    staleTime: 15 * 60 * 1000,
  })
}
