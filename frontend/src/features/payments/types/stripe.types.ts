export type StripeConnectStatus = {
  sandboxConfigured: boolean
  connected: boolean
  detailsSubmitted: boolean
  chargesEnabled: boolean
  payoutsEnabled: boolean
}

export type StripeOnboardingLink = {
  url: string
}
