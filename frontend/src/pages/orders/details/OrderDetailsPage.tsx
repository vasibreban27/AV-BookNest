import { Link, Navigate, useLocation, useParams } from 'react-router-dom'
import { ArrowUpRightIcon } from '../../../components/common/icons/AppIcons'
import { OrderDetailsSummary } from '../../../components/orders/OrderDetailsSummary'
import { OrderItemsList } from '../../../components/orders/OrderItemsList'
import { OrdersErrorState, OrdersLoadingState } from '../../../components/orders/OrdersStates'
import { OrderStatusBadge } from '../../../components/orders/OrderStatusBadge'
import { ShipmentCard } from '../../../components/orders/ShipmentCard'
import { useOrder } from '../../../features/orders/hooks/useOrders'
import type { OrderDetailsLocationState } from '../../../features/orders/types/orders.types'

export function OrderDetailsPage() {
  const { orderId } = useParams()
  const location = useLocation()
  const numericOrderId = Number(orderId)
  const orderQuery = useOrder(numericOrderId)
  const wasJustPlaced = (location.state as OrderDetailsLocationState | null)?.placed === true

  if (!Number.isInteger(numericOrderId) || numericOrderId <= 0) {
    return <Navigate to="/orders" replace />
  }

  return (
    <main className="order-details-page">
      <section className="order-details-content">
        <div className="orders-page__container">
          <Link className="order-details__back" to="/orders">← Înapoi la comenzi</Link>

          {wasJustPlaced && (
            <div className="order-success" role="status">
              <span>✓</span>
              <div>
                <strong>Comanda a fost plasată cu succes.</strong>
                <p>Cărțile au fost rezervate, iar vânzătorii au fost notificați.</p>
              </div>
            </div>
          )}

          {orderQuery.isLoading && <OrdersLoadingState />}
          {orderQuery.isError && <OrdersErrorState onRetry={() => void orderQuery.refetch()} />}
          {!orderQuery.isLoading && !orderQuery.isError && orderQuery.data && (
            <>
              <header className="order-details-header">
                <div>
                  <span className="home-kicker">Detalii comandă</span>
                  <h1>{orderQuery.data.orderNumber}</h1>
                </div>
                <OrderStatusBadge status={orderQuery.data.status} />
              </header>

              <div className="order-details-layout">
                <div className="order-details-main">
                  <OrderItemsList items={orderQuery.data.items} currency={orderQuery.data.currency} />

                  <section className="order-panel">
                    <div className="order-panel__heading">
                      <div>
                        <span className="home-kicker">Livrare</span>
                        <h2>Coletele tale</h2>
                      </div>
                    </div>
                    <div className="shipments-list">
                      {orderQuery.data.sellerOrders.map((sellerOrder) => (
                        <ShipmentCard sellerOrder={sellerOrder} currency={orderQuery.data.currency} key={sellerOrder.id} />
                      ))}
                    </div>
                  </section>
                </div>

                <OrderDetailsSummary order={orderQuery.data} />
              </div>

              <div className="order-details__footer-link">
                <Link to={{ pathname: '/', hash: '#catalog' }}>Descoperă alte cărți <ArrowUpRightIcon /></Link>
              </div>
            </>
          )}
        </div>
      </section>
    </main>
  )
}
