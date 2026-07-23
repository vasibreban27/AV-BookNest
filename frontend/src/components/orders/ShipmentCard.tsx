import { formatOrderPrice, formatShipmentStatus } from '../../features/orders/utils/orderFormatters'
import { PackageIcon, PinIcon } from '../common/icons/AppIcons'
import type { ShipmentCardProps } from './types/order-component.types'

export function ShipmentCard({ sellerOrder, currency }: ShipmentCardProps) {
  const { shipment } = sellerOrder
  return (
    <article className="shipment-card">
      <div className="shipment-card__top">
        <span><PackageIcon /></span>
        <div>
          <small>Colet de la</small>
          <h3>{sellerOrder.sellerName}</h3>
        </div>
        <strong>{formatShipmentStatus(shipment.status)}</strong>
      </div>

      <div className="shipment-card__destination">
        <PinIcon />
        <div>
          <small>Destinație Easybox</small>
          <strong>{shipment.easyboxName}</strong>
          <span>{shipment.easyboxAddress}, {shipment.easyboxCity}</span>
        </div>
      </div>

      <div className="shipment-card__footer">
        <span>{sellerOrder.items.length} {sellerOrder.items.length === 1 ? 'carte' : 'cărți'}</span>
        <span>Produse: <strong>{formatOrderPrice(sellerOrder.itemSubtotal, currency)}</strong></span>
      </div>
      {shipment.trackingNumber && (
        <p className="shipment-card__tracking">AWB Sameday: <strong>{shipment.trackingNumber}</strong></p>
      )}
    </article>
  )
}
