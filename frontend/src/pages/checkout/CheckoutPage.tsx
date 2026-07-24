import { useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { CheckoutForm } from '../../components/checkout/CheckoutForm'
import { CheckoutErrorState, CheckoutEmptyState, CheckoutLoadingState } from '../../components/checkout/CheckoutStates'
import { CheckoutSummary } from '../../components/checkout/CheckoutSummary'
import { PackageIcon } from '../../components/common/icons/AppIcons'
import { useCart } from '../../features/cart/hooks/useCart'
import type { Order, ShippingQuote } from '../../features/orders/types/orders.types'

export function CheckoutPage() {
  const cartQuery = useCart()
  const navigate = useNavigate()
  const [shippingQuote, setShippingQuote] = useState<ShippingQuote | null>(null)

  const handleCompleted = (order: Order) => {
    navigate(`/orders/${order.id}`, { replace: true, state: { placed: true } })
  }

  return (
    <main className="checkout-page">
      <section className="checkout-hero">
        <div className="checkout-page__container checkout-hero__inner">
          <div>
            <span className="home-kicker">Ultimul pas</span>
            <h1>Finalizează comanda</h1>
            <p>Confirmă destinația Easybox și pregătește-te pentru următoarea lectură.</p>
          </div>
          <span className="checkout-hero__icon" aria-hidden="true"><PackageIcon /></span>
        </div>
      </section>

      <section className="checkout-content">
        <div className="checkout-page__container">
          {cartQuery.isLoading && <CheckoutLoadingState />}
          {cartQuery.isError && <CheckoutErrorState onRetry={() => void cartQuery.refetch()} />}
          {!cartQuery.isLoading && !cartQuery.isError && cartQuery.data?.items.length === 0 && (
            <CheckoutEmptyState />
          )}
          {!cartQuery.isLoading && !cartQuery.isError && cartQuery.data && cartQuery.data.items.length > 0 && (
            <div className="checkout-layout">
              <CheckoutForm onCompleted={handleCompleted} onQuoteChange={setShippingQuote} />
              <CheckoutSummary cart={cartQuery.data} quote={shippingQuote} />
            </div>
          )}
        </div>
      </section>
    </main>
  )
}
