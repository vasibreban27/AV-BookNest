import {
  formatOrderDate,
  formatOrderPrice,
  formatPaymentProvider,
  formatPaymentStatus,
} from '../../features/orders/utils/orderFormatters'
import { PackageIcon, ReceiptIcon } from '../common/icons/AppIcons'
import { OrderCancellation } from './OrderCancellation'
import type { OrderDetailsSummaryProps } from './types/order-component.types'

export function OrderDetailsSummary({ order }: OrderDetailsSummaryProps) {
  return (
    <aside className="order-details-summary">
      <span className="home-kicker">Sumar comandă</span>
      <h2>{order.orderNumber}</h2>
      <p>{formatOrderDate(order.placedAt)}</p>

      <div className="order-details-summary__line">
        <span>Subtotal</span>
        <strong>{formatOrderPrice(order.subtotal, order.currency)}</strong>
      </div>
      <div className="order-details-summary__line">
        <span>Livrare</span>
        <strong>{order.shippingCost === 0 ? 'Gratuită' : formatOrderPrice(order.shippingCost, order.currency)}</strong>
      </div>
      <div className="order-details-summary__total">
        <span>Total</span>
        <strong>{formatOrderPrice(order.totalAmount, order.currency)}</strong>
      </div>

      {order.payment && (
        <div className="order-payment-card">
          <span><ReceiptIcon /></span>
          <div>
            <small>Metodă de plată</small>
            <strong>{formatPaymentProvider(order.payment.provider)}</strong>
            <p>{formatPaymentStatus(order.payment.status)}</p>
          </div>
        </div>
      )}

      <div className="order-details-summary__shipments">
        <PackageIcon />
        <span>{order.sellerOrders.length} {order.sellerOrders.length === 1 ? 'colet' : 'colete'} pentru această comandă</span>
      </div>

      <OrderCancellation order={order} />
    </aside>
  )
}
