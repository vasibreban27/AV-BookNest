import { useDeferredValue, useState } from 'react'
import { AdminBadge, AdminModal, AdminPageHeader, AdminPagination, AdminPanel, AdminState } from '../../components/admin/AdminUi'
import { useAdminOrder, useAdminOrders } from '../../features/admin/hooks/useAdmin'
import { formatAdminDate, formatAdminMoney, getAdminErrorMessage } from '../../features/admin/utils/adminFormatters'
import type { OrderStatus } from '../../features/orders/types/orders.types'
import { formatOrderStatus, formatPaymentStatus, formatSellerOrderStatus, formatShipmentStatus } from '../../features/orders/utils/orderFormatters'

const statuses: OrderStatus[] = ['PENDING', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED']

export function AdminOrdersPage() {
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState<OrderStatus | ''>('')
  const [page, setPage] = useState(0)
  const [selectedOrderId, setSelectedOrderId] = useState<number | null>(null)
  const deferredQuery = useDeferredValue(query)
  const orders = useAdminOrders({ query: deferredQuery, status: status || undefined, page, size: 25 })
  const detail = useAdminOrder(selectedOrderId)

  return (
    <main className="admin-page">
      <AdminPageHeader eyebrow="Marketplace" title="Comenzi" description="Vizibilitate completă asupra cumpărătorului, plății, vânzătorilor și fiecărui colet, fără editări manuale de status." />
      <AdminPanel title="Toate comenzile" description="Caută după numărul comenzii sau emailul cumpărătorului.">
        <div className="admin-filters"><label className="admin-field admin-field--search"><span>Caută</span><input type="search" value={query} onChange={(event) => { setQuery(event.target.value); setPage(0) }} placeholder="Număr comandă sau email" /></label><label className="admin-field"><span>Status</span><select value={status} onChange={(event) => { setStatus(event.target.value as OrderStatus | ''); setPage(0) }}><option value="">Toate statusurile</option>{statuses.map((value) => <option value={value} key={value}>{formatOrderStatus(value)}</option>)}</select></label></div>
        {orders.isLoading && <AdminState kind="loading" title="Încărcăm comenzile" message="Centralizăm datele comerciale." />}
        {orders.isError && <AdminState kind="error" title="Comenzile nu pot fi încărcate" message={getAdminErrorMessage(orders.error)} onRetry={() => void orders.refetch()} />}
        {orders.data?.content.length === 0 && <AdminState kind="empty" title="Nicio comandă găsită" message="Schimbă filtrele și încearcă din nou." />}
        {orders.data && orders.data.content.length > 0 && <><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>Comandă</th><th>Cumpărător</th><th>Status</th><th>Total</th><th>Plasată</th><th><span className="sr-only">Acțiuni</span></th></tr></thead><tbody>{orders.data.content.map((order) => <tr key={order.id}><td data-label="Comandă"><strong>{order.orderNumber}</strong><small className="admin-table__subline">ID {order.id}</small></td><td data-label="Cumpărător">{order.buyerEmail}<small className="admin-table__subline">ID {order.buyerId}</small></td><td data-label="Status"><AdminBadge tone={order.status === 'DELIVERED' || order.status === 'PAID' ? 'success' : order.status === 'CANCELLED' || order.status === 'REFUNDED' ? 'danger' : 'info'}>{formatOrderStatus(order.status)}</AdminBadge></td><td data-label="Total"><strong>{formatAdminMoney(order.totalAmount, order.currency)}</strong></td><td data-label="Plasată">{formatAdminDate(order.placedAt)}</td><td><button type="button" className="admin-button admin-button--compact admin-button--secondary" onClick={() => setSelectedOrderId(order.id)}>Deschide</button></td></tr>)}</tbody></table></div><AdminPagination page={orders.data.page} totalPages={orders.data.totalPages} totalElements={orders.data.totalElements} onPageChange={setPage} /></>}
      </AdminPanel>

      {selectedOrderId !== null && <AdminModal title="Detalii comandă" description="Plată, articole și expedieri grupate per vânzător." onClose={() => setSelectedOrderId(null)} size="large">
        {detail.isLoading && <AdminState kind="loading" title="Încărcăm comanda" message="Pregătim detaliile complete." />}
        {detail.isError && <AdminState kind="error" title="Comanda nu poate fi încărcată" message={getAdminErrorMessage(detail.error)} />}
        {detail.data && <div className="admin-order-detail">
          <div className="admin-order-detail__summary"><div><span>Comandă</span><strong>{detail.data.order.orderNumber}</strong></div><div><span>Total</span><strong>{formatAdminMoney(detail.data.order.totalAmount, detail.data.order.currency)}</strong></div><div><span>Status</span><AdminBadge tone="info">{formatOrderStatus(detail.data.order.status)}</AdminBadge></div><div><span>Plasată</span><strong>{formatAdminDate(detail.data.order.placedAt)}</strong></div></div>
          <div className="admin-callout"><strong>Cumpărător</strong><p>{detail.data.buyerEmail} · {detail.data.recipientPhone}</p><small>ID utilizator {detail.data.buyerId}</small></div>
          {detail.data.order.payment && <div className="admin-section-block"><h3>Plată</h3><div className="admin-inline-facts"><span>Stripe</span><strong>{formatAdminMoney(detail.data.order.payment.amount, detail.data.order.payment.currency)}</strong><AdminBadge tone={detail.data.order.payment.status === 'SUCCEEDED' ? 'success' : detail.data.order.payment.status.includes('REFUND') ? 'danger' : 'warning'}>{formatPaymentStatus(detail.data.order.payment.status)}</AdminBadge></div></div>}
          <div className="admin-section-block"><h3>Articole</h3><div className="admin-line-items">{detail.data.order.items.map((item) => <div key={item.id}><span><strong>{item.title}</strong><small>{item.author} · Vânzător #{item.sellerId}</small></span><b>{item.quantity} × {formatAdminMoney(item.unitPrice, detail.data!.order.currency)}</b></div>)}</div></div>
          <div className="admin-section-block"><h3>Expedieri per vânzător</h3><div className="admin-seller-orders">{detail.data.order.sellerOrders.map((sellerOrder) => <article key={sellerOrder.id}><header><span><strong>{sellerOrder.sellerName}</strong><small>Subcomandă #{sellerOrder.id}</small></span><AdminBadge tone={sellerOrder.status === 'FULFILLED' ? 'success' : sellerOrder.status === 'CANCELLED' ? 'danger' : 'info'}>{formatSellerOrderStatus(sellerOrder.status)}</AdminBadge></header><dl><div><dt>Încasare vânzător</dt><dd>{formatAdminMoney(sellerOrder.sellerProceeds)}</dd></div><div><dt>Comision</dt><dd>{formatAdminMoney(sellerOrder.commissionAmount)}</dd></div><div><dt>Livrare</dt><dd>{sellerOrder.shipment ? formatShipmentStatus(sellerOrder.shipment.status) : 'Necreată'}</dd></div><div><dt>AWB</dt><dd>{sellerOrder.shipment?.trackingNumber || '—'}</dd></div></dl>{sellerOrder.issueStatus === 'OPEN' && <AdminBadge tone="danger">Dispută deschisă</AdminBadge>}</article>)}</div></div>
        </div>}
      </AdminModal>}
    </main>
  )
}
