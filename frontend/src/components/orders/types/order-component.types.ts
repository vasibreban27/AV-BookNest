import type { Order, OrderItem, OrderStatus, Shipment } from '../../../features/orders/types/orders.types'

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
  shipment: Shipment
  currency: string
}

export type OrdersErrorStateProps = {
  onRetry: () => void
}
