import { PackageIcon } from '../../components/common/icons/AppIcons'
import { SellerReputation } from '../../components/reviews/SellerReputation'
import { useAuth } from '../../features/auth/hooks/useAuth'
import { SellerOrderCard } from '../../components/sales/SellerOrderCard'
import { SalesEmptyState, SalesErrorState, SalesLoadingState } from '../../components/sales/SalesStates'
import { useSellerOrders } from '../../features/seller-orders/hooks/useSellerOrders'
import type { SellerOrderStatus } from '../../features/orders/types/orders.types'

export function SalesPage() {
  const { user } = useAuth()
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState<SellerOrderStatus | ''>('')
  const deferredQuery = useDeferredValue(query)
  const salesQuery = useSellerOrders({
    query: deferredQuery,
    status: status || undefined,
  })
  return (
    <main className="seller-shipments-page">
      <section className="seller-shipments-hero">
        <div className="seller-shipments-container seller-shipments-hero__inner">
          <div>
            <span className="home-kicker">Centrul vânzătorului</span>
            <h1>Vânzările mele</h1>
            <p>Ai 24h să accepți și 48h de la acceptare să predai coletul.</p>
          </div>
          <span className="seller-shipments-hero__icon" aria-hidden="true"><PackageIcon /></span>
        </div>
      </section>
      <section className="seller-shipments-content">
        <div className="seller-shipments-container">
          {user && <SellerReputation sellerId={user.id} key={user.id} />}
          <div className="marketplace-filters">
            <label>
              <span>Caută în vânzări</span>
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
                onChange={(event) => setStatus(event.target.value as SellerOrderStatus | '')}
              >
                <option value="">Toate statusurile</option>
                <option value="PAYMENT_PENDING">Plată în așteptare</option>
                <option value="AWAITING_SELLER">Așteaptă acceptarea</option>
                <option value="ACCEPTED">Acceptată</option>
                <option value="FULFILLED">Finalizată</option>
                <option value="CANCELLED">Anulată</option>
              </select>
            </label>
          </div>
          {salesQuery.isLoading && <SalesLoadingState />}
          {salesQuery.isError && <SalesErrorState onRetry={() => void salesQuery.refetch()} />}
          {salesQuery.data?.length === 0 && <SalesEmptyState />}
          {salesQuery.data && salesQuery.data.length > 0 && (
            <div className="seller-shipments-list">
              {salesQuery.data.map((sellerOrder) => <SellerOrderCard sellerOrder={sellerOrder} key={sellerOrder.id} />)}
            </div>
          )}
        </div>
      </section>
    </main>
  )
}
import { useDeferredValue, useState } from 'react'
