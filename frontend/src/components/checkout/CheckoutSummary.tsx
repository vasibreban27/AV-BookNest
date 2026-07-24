import { Link } from 'react-router-dom'
import { formatOrderPrice } from '../../features/orders/utils/orderFormatters'
import { BookOutlineIcon } from '../common/icons/AppIcons'
import type { CheckoutSummaryProps } from './types/checkout-component.types'

export function CheckoutSummary({ cart, quote }: CheckoutSummaryProps) {
  return (
    <aside className="checkout-summary" aria-label="Sumarul comenzii">
      <div className="checkout-summary__heading">
        <div>
          <span className="home-kicker">Pasul 2</span>
          <h2>Comanda ta</h2>
        </div>
        <Link to="/cart">Editează coșul</Link>
      </div>

      <div className="checkout-summary__items">
        {cart.items.map((item) => (
          <div className="checkout-summary__item" key={item.id}>
            <span><BookOutlineIcon /></span>
            <div>
              <strong>{item.book.title}</strong>
              <small>{item.book.author}</small>
            </div>
            <b>{formatOrderPrice(item.book.price)}</b>
          </div>
        ))}
      </div>

      <div className="checkout-summary__line">
        <span>Subtotal</span>
        <strong>{formatOrderPrice(cart.total)}</strong>
      </div>
      <div className="checkout-summary__line">
        <span>Livrare</span>
        <strong>{quote ? formatOrderPrice(quote.shippingCost) : 'Se calculează'}</strong>
      </div>
      <div className="checkout-summary__total">
        <span>Total</span>
        <strong>{formatOrderPrice(cart.total + (quote?.shippingCost ?? 0))}</strong>
      </div>
      <p>Comanda poate genera colete separate dacă volumele provin de la vânzători diferiți.</p>
    </aside>
  )
}
