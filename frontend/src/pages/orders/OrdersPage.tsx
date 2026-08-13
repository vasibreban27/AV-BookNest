import { OrderCard } from '../../components/orders/OrderCard'
import { OrdersEmptyState, OrdersErrorState, OrdersLoadingState } from '../../components/orders/OrdersStates'
import { ReceiptIcon } from '../../components/common/icons/AppIcons'
import { useOrders } from '../../features/orders/hooks/useOrders'
import type { OrderStatus } from '../../features/orders/types/orders.types'

export function OrdersPage() {
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState<OrderStatus | ''>('')
  const deferredQuery = useDeferredValue(query)
  const ordersQuery = useOrders({
    query: deferredQuery,
    status: status || undefined,
  })

  return (
    <main className="orders-page">
      <section className="orders-hero">
        <div className="orders-page__container orders-hero__inner">
          <div>
            <span className="home-kicker">Biblioteca în mișcare</span>
            <h1>Comenzile mele</h1>
            <p>Urmărește fiecare comandă, plată și livrare BookNest.</p>
          </div>
          <span className="orders-hero__icon" aria-hidden="true"><ReceiptIcon /></span>
        </div>
      </section>

      <section className="orders-content">
        <div className="orders-page__container">
          <div className="marketplace-filters">
            <label>
              <span>Caută în comenzi</span>
              <input
                type="search"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Număr comandă sau titlu"
              />
            </label>
            <label>
              <span>Status</span>
              <select
                value={status}
                onChange={(event) => setStatus(event.target.value as OrderStatus | '')}
              >
                <option value="">Toate statusurile</option>
                <option value="PENDING">În așteptarea plății</option>
                <option value="PAID">Plătită</option>
                <option value="PROCESSING">În procesare</option>
                <option value="SHIPPED">Expediată</option>
                <option value="DELIVERED">Livrată</option>
                <option value="CANCELLED">Anulată</option>
                <option value="REFUNDED">Rambursată</option>
              </select>
            </label>
          </div>
          {ordersQuery.isLoading && <OrdersLoadingState />}
          {ordersQuery.isError && <OrdersErrorState onRetry={() => void ordersQuery.refetch()} />}
          {!ordersQuery.isLoading && !ordersQuery.isError && ordersQuery.data?.length === 0 && <OrdersEmptyState />}
          {!ordersQuery.isLoading && !ordersQuery.isError && ordersQuery.data && ordersQuery.data.length > 0 && (
            <div className="orders-list">
              <div className="orders-list__heading">
                <div>
                  <span className="home-kicker">Istoric</span>
                  <h2>Rezultate</h2>
                </div>
                <span>{ordersQuery.data.length} {ordersQuery.data.length === 1 ? 'comandă' : 'comenzi'}</span>
              </div>
              {ordersQuery.data.map((order) => <OrderCard order={order} key={order.id} />)}
            </div>
          )}
        </div>
      </section>
    </main>
  )
}
import { useDeferredValue, useState } from 'react'
