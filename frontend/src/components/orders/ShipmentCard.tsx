import { formatOrderPrice, formatShipmentStatus } from '../../features/orders/utils/orderFormatters'
import { PackageIcon, PinIcon } from '../common/icons/AppIcons'
import type { ShipmentCardProps } from './types/order-component.types'

export function ShipmentCard({ shipment, currency }: ShipmentCardProps) {
  return (
    <article className="shipment-card">
      <div className="shipment-card__top">
        <span><PackageIcon /></span>
        <div>
          <small>Colet de la</small>
          <h3>{shipment.sellerName}</h3>
        </div>
        <strong>{formatShipmentStatus(shipment.status)}</strong>
      </div>

      <div className="shipment-card__destination">
        <PinIcon />
        <div>
          <small>Destinație Easybox</small>
          <strong>{shipment.easyboxName}</strong>
          <span>Cod: {shipment.easyboxId}</span>
        </div>
      </div>

      <div className="shipment-card__footer">
        <span>{shipment.items.length} {shipment.items.length === 1 ? 'carte' : 'cărți'}</span>
        <span>Ramburs: <strong>{formatOrderPrice(shipment.codAmount, currency)}</strong></span>
      </div>
      {shipment.trackingNumber && (
        <p className="shipment-card__tracking">AWB: <strong>{shipment.trackingNumber}</strong></p>
      )}
    </article>
  )
}
