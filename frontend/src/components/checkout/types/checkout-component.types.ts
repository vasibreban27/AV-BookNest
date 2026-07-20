import type { Cart } from '../../../features/cart/types/cart.types'
import type { Order } from '../../../features/orders/types/orders.types'

export type CheckoutFormProps = {
  onCompleted: (order: Order) => void
}

export type CheckoutSummaryProps = {
  cart: Cart
}

export type CheckoutErrorStateProps = {
  onRetry: () => void
}
