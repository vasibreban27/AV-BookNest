import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../../auth/hooks/useAuth'
import { sellerOrdersApi } from '../api/sellerOrdersApi'

const sellerOrderKeys = {
  mine: (userId: number | undefined) => ['seller-orders', 'seller', userId] as const,
}

function useRefreshSales() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  return () => {
    void queryClient.invalidateQueries({ queryKey: sellerOrderKeys.mine(user?.id) })
    void queryClient.invalidateQueries({ queryKey: ['orders'] })
    void queryClient.invalidateQueries({ queryKey: ['catalog', 'books'] })
    void queryClient.invalidateQueries({ queryKey: ['listings', 'mine', user?.id] })
  }
}

export function useSellerOrders() {
  const { user } = useAuth()
  return useQuery({
    queryKey: sellerOrderKeys.mine(user?.id),
    queryFn: sellerOrdersApi.listMine,
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
