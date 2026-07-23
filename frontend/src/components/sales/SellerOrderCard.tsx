import { useState } from 'react'
import type { PackageSize, SellerOrder } from '../../features/orders/types/orders.types'
import {
  formatOrderDate,
  formatOrderPrice,
  formatSellerOrderStatus,
  formatShipmentStatus,
} from '../../features/orders/utils/orderFormatters'
import {
  useAcceptSellerOrder,
  useCancelSellerOrder,
} from '../../features/seller-orders/hooks/useSellerOrders'
import { getSellerOrderErrorMessage } from '../../features/seller-orders/utils/sellerOrderErrors'
import { BookOutlineIcon, PackageIcon, PinIcon } from '../common/icons/AppIcons'

export function SellerOrderCard({ sellerOrder }: { sellerOrder: SellerOrder }) {
  const [packageSize, setPackageSize] = useState<PackageSize>('S')
  const [errorMessage, setErrorMessage] = useState('')
  const acceptMutation = useAcceptSellerOrder()
  const cancelMutation = useCancelSellerOrder()
  const isPending = acceptMutation.isPending || cancelMutation.isPending
  const { shipment } = sellerOrder

  const accept = async () => {
    setErrorMessage('')
    try {
      await acceptMutation.mutateAsync({ sellerOrderId: sellerOrder.id, packageSize })
    } catch (error) {
      setErrorMessage(getSellerOrderErrorMessage(error))
    }
  }

  const cancel = async () => {
    if (!window.confirm('Refuzi această vânzare? Cărțile vor redeveni disponibile.')) return
    setErrorMessage('')
    try {
      await cancelMutation.mutateAsync(sellerOrder.id)
    } catch (error) {
      setErrorMessage(getSellerOrderErrorMessage(error))
    }
  }

  return (
    <article className="seller-shipment-card">
      <header className="seller-shipment-card__header">
        <span className="seller-shipment-card__icon"><PackageIcon /></span>
        <div>
          <small>{formatOrderDate(sellerOrder.createdAt)}</small>
          <h2>{sellerOrder.orderNumber}</h2>
          <p>Cumpărător: <strong>{sellerOrder.buyerName}</strong></p>
        </div>
        <span className={`seller-shipment-status seller-shipment-status--${sellerOrder.status.toLowerCase()}`}>
          {formatSellerOrderStatus(sellerOrder.status)}
        </span>
      </header>

      <div className="seller-shipment-card__body">
        <div className="seller-shipment-card__destination">
          <PinIcon />
          <span>
            <small>Destinație</small>
            <strong>{shipment.easyboxName}</strong>
            <em>{shipment.easyboxAddress}, {shipment.easyboxCity}</em>
          </span>
        </div>
        <div className="seller-shipment-card__books">
          <BookOutlineIcon />
          <div>
            {sellerOrder.items.map((item) => (
              <span key={item.id}><strong>{item.title}</strong><small>{item.author}</small></span>
            ))}
          </div>
        </div>
        <div className="seller-shipment-card__amount">
          <span>Încasezi după comisionul de {sellerOrder.commissionRate}%</span>
          <strong>{formatOrderPrice(sellerOrder.sellerProceeds)}</strong>
          <small>Comision BookNest: {formatOrderPrice(sellerOrder.commissionAmount)}</small>
        </div>
      </div>

      <p className="seller-shipment-card__tracking">
        Sameday: <strong>{formatShipmentStatus(shipment.status)}</strong>
        {shipment.trackingNumber && <> · AWB {shipment.trackingNumber}</>}
      </p>

      <footer className="seller-shipment-card__actions">
        {sellerOrder.status === 'AWAITING_SELLER' && (
          <>
            <label className="seller-package-size">
              Mărime colet
              <select value={packageSize} onChange={(event) => setPackageSize(event.target.value as PackageSize)}>
                <option value="S">S · până la 445 × 100 × 470 mm</option>
                <option value="M">M · până la 445 × 200 × 470 mm</option>
                <option value="L">L · până la 445 × 390 × 470 mm</option>
              </select>
            </label>
            <button className="seller-action seller-action--primary" type="button" disabled={isPending} onClick={() => void accept()}>
              Acceptă și generează AWB
            </button>
            <button className="seller-action seller-action--danger" type="button" disabled={isPending} onClick={() => void cancel()}>
              Refuză
            </button>
          </>
        )}
        {sellerOrder.status === 'ACCEPTED' && (
          <p className="seller-shipment-card__final-state">
            Predă coletul până la {sellerOrder.dropoffBy ? formatOrderDate(sellerOrder.dropoffBy) : '—'}.
            Statusurile următoare sunt actualizate automat de Sameday.
          </p>
        )}
        {sellerOrder.status === 'FULFILLED' && (
          <p className="seller-shipment-card__final-state">Livrare finalizată. Transferul Stripe va deveni eligibil conform politicii de payout.</p>
        )}
        {sellerOrder.status === 'CANCELLED' && (
          <p className="seller-shipment-card__final-state">Vânzare anulată.</p>
        )}
      </footer>
      {errorMessage && <p className="seller-shipment-card__error" role="alert">{errorMessage}</p>}
    </article>
  )
}
