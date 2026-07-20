import { Link } from 'react-router-dom'
import { formatOrderDate, formatOrderPrice } from '../../features/orders/utils/orderFormatters'
import { ArrowUpRightIcon, BookOutlineIcon } from '../common/icons/AppIcons'
import { OrderStatusBadge } from './OrderStatusBadge'
import type { OrderCardProps } from './types/order-component.types'

export function OrderCard({ order }: OrderCardProps) {
  return (
    <article className="order-card">
      <div className="order-card__top">
        <div>
          <span>Comanda</span>
          <h2>{order.orderNumber}</h2>
          <p>{formatOrderDate(order.placedAt)}</p>
        </div>
        <OrderStatusBadge status={order.status} />
      </div>

      <div className="order-card__books">
        <span className="order-card__books-icon"><BookOutlineIcon /></span>
        <div>
          {order.items.slice(0, 2).map((item) => item.title).join(', ')}
          {order.items.length > 2 && <small> + încă {order.items.length - 2}</small>}
        </div>
      </div>

      <div className="order-card__bottom">
        <div>
          <small>{order.items.length} {order.items.length === 1 ? 'carte' : 'cărți'}</small>
          <strong>{formatOrderPrice(order.totalAmount, order.currency)}</strong>
        </div>
        <Link to={`/orders/${order.id}`}>Vezi detaliile <ArrowUpRightIcon /></Link>
      </div>
    </article>
  )
}
