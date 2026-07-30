import type { Cart } from '../../../features/cart/types/cart.types'
import type {
  ShippingQuote,
  StripeCheckoutSession,
} from '../../../features/orders/types/orders.types'

export type CheckoutFormProps = {
  onPrepared: (session: StripeCheckoutSession) => void
  onQuoteChange: (quote: ShippingQuote | null) => void
}

export type CheckoutSummaryProps = {
  cart: Cart
  quote: ShippingQuote | null
}

export type CheckoutErrorStateProps = {
  onRetry: () => void
}

export type StripePaymentFormProps = {
  session: StripeCheckoutSession
  onCompleted: () => void
}
