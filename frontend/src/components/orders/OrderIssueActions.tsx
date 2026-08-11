import { useState } from 'react'
import {
  useReportOrderIssue,
  useResolveOrderIssue,
} from '../../features/orders/hooks/useOrders'
import type { SellerOrder } from '../../features/orders/types/orders.types'
import { formatOrderDate } from '../../features/orders/utils/orderFormatters'

type OrderIssueActionsProps = {
  orderId: number
  sellerOrder: SellerOrder
}

export function OrderIssueActions({ orderId, sellerOrder }: OrderIssueActionsProps) {
  const [editing, setEditing] = useState(false)
  const [reason, setReason] = useState('')
  const reportIssue = useReportOrderIssue(orderId)
  const resolveIssue = useResolveOrderIssue(orderId)

  if (sellerOrder.issueStatus === 'OPEN') {
    return (
      <div className="order-issue order-issue--open">
        <strong>Transferul către vânzător este suspendat</strong>
        <p>{sellerOrder.issueReason}</p>
        <button
          type="button"
          disabled={resolveIssue.isPending}
          onClick={() => resolveIssue.mutate(sellerOrder.id)}
        >
          {resolveIssue.isPending ? 'Se salvează...' : 'Problema s-a rezolvat'}
        </button>
      </div>
    )
  }

  if (!sellerOrder.canReportIssue) return null

  if (!editing) {
    return (
      <div className="order-issue">
        <p>
          Poți raporta o problemă până la{' '}
          {sellerOrder.issueDeadline ? formatOrderDate(sellerOrder.issueDeadline) : 'expirarea perioadei'}.
        </p>
        <button type="button" onClick={() => setEditing(true)}>Am o problemă cu acest colet</button>
      </div>
    )
  }

  return (
    <form
      className="order-issue order-issue--form"
      onSubmit={(event) => {
        event.preventDefault()
        const value = reason.trim()
        if (value) reportIssue.mutate({ sellerOrderId: sellerOrder.id, reason: value })
      }}
    >
      <label htmlFor={`issue-${sellerOrder.id}`}>Descrie problema</label>
      <textarea
        id={`issue-${sellerOrder.id}`}
        value={reason}
        maxLength={500}
        rows={3}
        onChange={(event) => setReason(event.target.value)}
        placeholder="Exemplu: colet deteriorat sau carte diferită de descriere"
      />
      <div>
        <button type="button" onClick={() => setEditing(false)}>Renunță</button>
        <button type="submit" disabled={!reason.trim() || reportIssue.isPending}>
          {reportIssue.isPending ? 'Trimitem...' : 'Suspendă payoutul și raportează'}
        </button>
      </div>
      {reportIssue.isError && <small>Sesizarea nu a putut fi trimisă.</small>}
    </form>
  )
}
