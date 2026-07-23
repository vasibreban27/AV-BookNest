import { PackageIcon } from '../../components/common/icons/AppIcons'
import { SellerOrderCard } from '../../components/sales/SellerOrderCard'
import { SalesEmptyState, SalesErrorState, SalesLoadingState } from '../../components/sales/SalesStates'
import { useSellerOrders } from '../../features/seller-orders/hooks/useSellerOrders'

export function SalesPage() {
  const salesQuery = useSellerOrders()
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
