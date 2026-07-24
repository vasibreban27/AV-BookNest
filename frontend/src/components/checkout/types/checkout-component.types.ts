import type { Cart } from '../../../features/cart/types/cart.types'
import type { Order, ShippingQuote } from '../../../features/orders/types/orders.types'

export type CheckoutFormProps = {
  onCompleted: (order: Order) => void
  onQuoteChange: (quote: ShippingQuote | null) => void
}

export type CheckoutSummaryProps = {
  cart: Cart
  quote: ShippingQuote | null
}

export type CheckoutErrorStateProps = {
  onRetry: () => void
}
