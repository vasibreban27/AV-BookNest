import { formatOrderStatus, getOrderStatusTone } from '../../features/orders/utils/orderFormatters'
import type { OrderStatusBadgeProps } from './types/order-component.types'

export function OrderStatusBadge({ status }: OrderStatusBadgeProps) {
  return (
    <span className={`order-status order-status--${getOrderStatusTone(status)}`}>
      <i aria-hidden="true" />
      {formatOrderStatus(status)}
    </span>
  )
}
