import { useState } from 'react'
import { useCancelOrder } from '../../features/orders/hooks/useOrders'
import type { Order } from '../../features/orders/types/orders.types'
import { getOrderErrorMessage } from '../../features/orders/utils/orderErrors'

type OrderCancellationProps = {
  order: Order
}

export function OrderCancellation({ order }: OrderCancellationProps) {
  const [errorMessage, setErrorMessage] = useState('')
  const cancelMutation = useCancelOrder()
  const canCancel =
    (order.status === 'PENDING' || order.status === 'PROCESSING')
    && order.sellerOrders.every(({ shipment }) =>
      ['NOT_CREATED', 'AWB_PENDING', 'AWB_CREATED', 'CANCELLED'].includes(shipment.status),
    )

  if (!canCancel) return null

  const cancelOrder = async () => {
    if (!window.confirm('Sigur vrei să anulezi întreaga comandă?')) return
    setErrorMessage('')
    try {
      await cancelMutation.mutateAsync(order.id)
    } catch (error) {
      setErrorMessage(getOrderErrorMessage(error))
    }
  }

  return (
    <div className="order-cancellation">
      <button
        type="button"
        disabled={cancelMutation.isPending}
        onClick={() => void cancelOrder()}
      >
        {cancelMutation.isPending ? 'Se anulează...' : 'Anulează comanda'}
      </button>
      <small>Poți anula până când primul colet este predat către Sameday.</small>
      {errorMessage && <p role="alert">{errorMessage}</p>}
    </div>
  )
}
