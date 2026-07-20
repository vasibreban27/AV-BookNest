import { formatOrderPrice } from '../../features/orders/utils/orderFormatters'
import { BookOutlineIcon } from '../common/icons/AppIcons'
import type { OrderItemsListProps } from './types/order-component.types'

export function OrderItemsList({ items, currency }: OrderItemsListProps) {
  return (
    <section className="order-panel order-items-panel">
      <div className="order-panel__heading">
        <div>
          <span className="home-kicker">Volume comandate</span>
          <h2>Cărțile tale</h2>
        </div>
        <span>{items.length} {items.length === 1 ? 'volum' : 'volume'}</span>
      </div>

      <div className="order-items-list">
        {items.map((item) => (
          <article className="order-item-row" key={item.id}>
            <span className="order-item-row__icon"><BookOutlineIcon /></span>
            <div>
              <h3>{item.title}</h3>
              <p>{item.author}</p>
              {item.isbn && <small>ISBN {item.isbn}</small>}
            </div>
            <strong>{formatOrderPrice(item.unitPrice * item.quantity, currency)}</strong>
          </article>
        ))}
      </div>
    </section>
  )
}
