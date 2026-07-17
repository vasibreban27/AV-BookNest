import type { CartItem } from '../../../features/cart/types/cart.types'

export type AddToCartButtonProps = {
  bookId: number
  sellerId: number
}

export type NavbarCartProps = {
  onNavigate?: () => void
}

export type CartItemCardProps = {
  item: CartItem
  isRemoving: boolean
  onRemove: (bookId: number) => void
}

export type CartSummaryProps = {
  itemCount: number
  total: number
  isClearing: boolean
  onClear: () => void
}

export type CartErrorStateProps = {
  onRetry: () => void
}
