import { api } from '../../../api/client'
import type {
  StripeConnectStatus,
  StripeDashboardLink,
  StripeOnboardingLink,
} from '../types/stripe.types'

export const stripeApi = {
  async connectStatus() {
    const { data } = await api.get<StripeConnectStatus>('/stripe/connect/status')
    return data
  },

  async onboardingLink() {
    const { data } = await api.post<StripeOnboardingLink>(
      '/stripe/connect/onboarding-link',
    )
    return data
  },

  async dashboardLink() {
    const { data } = await api.post<StripeDashboardLink>(
      '/stripe/connect/dashboard-link',
    )
    return data
  },
}
