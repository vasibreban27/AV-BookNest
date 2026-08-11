import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../../auth/hooks/useAuth'
import { stripeApi } from '../api/stripeApi'

const stripeConnectKey = (userId: number | undefined) =>
  ['stripe', 'connect', userId] as const

export function useStripeConnectStatus() {
  const { user } = useAuth()
  return useQuery({
    queryKey: stripeConnectKey(user?.id),
    queryFn: stripeApi.connectStatus,
    enabled: Boolean(user),
    staleTime: 15_000,
  })
}

export function useStripeOnboarding() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: stripeApi.onboardingLink,
    onSuccess: ({ url }) => {
      void queryClient.invalidateQueries({ queryKey: stripeConnectKey(user?.id) })
      window.location.assign(url)
    },
  })
}

export function useStripeDashboard() {
  return useMutation({
    mutationFn: stripeApi.dashboardLink,
    onSuccess: ({ url }) => window.location.assign(url),
  })
}
