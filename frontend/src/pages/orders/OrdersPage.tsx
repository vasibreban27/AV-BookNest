import { OrderCard } from '../../components/orders/OrderCard'
import { OrdersEmptyState, OrdersErrorState, OrdersLoadingState } from '../../components/orders/OrdersStates'
import { ReceiptIcon } from '../../components/common/icons/AppIcons'
import { useOrders } from '../../features/orders/hooks/useOrders'

export function OrdersPage() {
  const ordersQuery = useOrders()

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
          {ordersQuery.isLoading && <OrdersLoadingState />}
          {ordersQuery.isError && <OrdersErrorState onRetry={() => void ordersQuery.refetch()} />}
          {!ordersQuery.isLoading && !ordersQuery.isError && ordersQuery.data?.length === 0 && <OrdersEmptyState />}
          {!ordersQuery.isLoading && !ordersQuery.isError && ordersQuery.data && ordersQuery.data.length > 0 && (
            <div className="orders-list">
              <div className="orders-list__heading">
                <div>
                  <span className="home-kicker">Istoric</span>
                  <h2>Toate comenzile</h2>
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
