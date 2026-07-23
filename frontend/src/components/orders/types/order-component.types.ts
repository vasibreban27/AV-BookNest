import type { Order, OrderItem, OrderStatus, SellerOrder } from '../../../features/orders/types/orders.types'

export type OrderStatusBadgeProps = {
  status: OrderStatus
}

export type OrderCardProps = {
  order: Order
}

export type OrderDetailsSummaryProps = {
  order: Order
}

export type OrderItemsListProps = {
  items: OrderItem[]
  currency: string
}

export type ShipmentCardProps = {
  sellerOrder: SellerOrder
  currency: string
}

export type OrdersErrorStateProps = {
  onRetry: () => void
}
