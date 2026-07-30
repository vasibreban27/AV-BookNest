import { Elements } from '@stripe/react-stripe-js'
import { loadStripe } from '@stripe/stripe-js'
import { useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { CheckoutForm } from '../../components/checkout/CheckoutForm'
import { CheckoutErrorState, CheckoutEmptyState, CheckoutLoadingState } from '../../components/checkout/CheckoutStates'
import { CheckoutSummary } from '../../components/checkout/CheckoutSummary'
import { StripePaymentForm } from '../../components/checkout/StripePaymentForm'
import { PackageIcon } from '../../components/common/icons/AppIcons'
import { useCart } from '../../features/cart/hooks/useCart'
import type { Cart } from '../../features/cart/types/cart.types'
import type {
  ShippingQuote,
  StripeCheckoutSession,
} from '../../features/orders/types/orders.types'

export function CheckoutPage() {
  const cartQuery = useCart()
  const navigate = useNavigate()
  const [shippingQuote, setShippingQuote] = useState<ShippingQuote | null>(null)
  const [payment, setPayment] = useState<{
    session: StripeCheckoutSession
    cart: Cart
  } | null>(null)

  const handlePrepared = (session: StripeCheckoutSession, cart: Cart) => {
    setPayment({ session, cart })
  }

  const handleCompleted = () => {
    if (!payment) return
    navigate(`/orders/${payment.session.order.id}`, {
      replace: true,
      state: { placed: true },
    })
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
          {!payment && cartQuery.isLoading && <CheckoutLoadingState />}
          {!payment && cartQuery.isError && <CheckoutErrorState onRetry={() => void cartQuery.refetch()} />}
          {!payment && !cartQuery.isLoading && !cartQuery.isError && cartQuery.data?.items.length === 0 && (
            <CheckoutEmptyState />
          )}
          {!payment && !cartQuery.isLoading && !cartQuery.isError && cartQuery.data && cartQuery.data.items.length > 0 && (
            <div className="checkout-layout">
              <CheckoutForm
                onPrepared={(session) => handlePrepared(session, cartQuery.data)}
                onQuoteChange={setShippingQuote}
              />
              <CheckoutSummary cart={cartQuery.data} quote={shippingQuote} />
            </div>
          )}
          {payment && (
            <div className="checkout-layout">
              <Elements
                stripe={loadStripe(payment.session.publishableKey)}
                options={{
                  clientSecret: payment.session.clientSecret,
                  appearance: {
                    theme: 'stripe',
                    variables: {
                      colorPrimary: '#1f5c4e',
                      colorText: '#172b2a',
                      borderRadius: '11px',
                    },
                  },
                }}
              >
                <StripePaymentForm session={payment.session} onCompleted={handleCompleted} />
              </Elements>
              <CheckoutSummary
                cart={payment.cart}
                quote={{
                  shippingCost: payment.session.order.shippingCost,
                  currency: payment.session.order.currency,
                  packages: [],
                }}
              />
            </div>
          )}
        </div>
      </section>
    </main>
  )
}
