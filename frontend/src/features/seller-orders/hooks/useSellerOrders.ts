import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../../auth/hooks/useAuth'
import { sellerOrdersApi } from '../api/sellerOrdersApi'
import type { SellerOrderFilters } from '../api/sellerOrdersApi'

const sellerOrderKeys = {
  mine: (userId: number | undefined, filters?: SellerOrderFilters) =>
    ['seller-orders', 'seller', userId, filters ?? {}] as const,
}

function useRefreshSales() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  return () => {
    void queryClient.invalidateQueries({ queryKey: ['seller-orders', 'seller', user?.id] })
    void queryClient.invalidateQueries({ queryKey: ['orders'] })
    void queryClient.invalidateQueries({ queryKey: ['catalog', 'books'] })
    void queryClient.invalidateQueries({ queryKey: ['listings', 'mine', user?.id] })
  }
}

export function useSellerOrders(filters: SellerOrderFilters = {}) {
  const { user } = useAuth()
  return useQuery({
    queryKey: sellerOrderKeys.mine(user?.id, filters),
    queryFn: () => sellerOrdersApi.listMine(filters),
    enabled: Boolean(user),
  })
}

export function useAcceptSellerOrder() {
  const refresh = useRefreshSales()
  return useMutation({ mutationFn: sellerOrdersApi.accept, onSuccess: refresh })
}

export function useCancelSellerOrder() {
  const refresh = useRefreshSales()
  return useMutation({ mutationFn: sellerOrdersApi.cancel, onSuccess: refresh })
}
