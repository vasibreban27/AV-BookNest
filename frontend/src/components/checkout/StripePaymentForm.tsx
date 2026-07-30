import { PaymentElement, useElements, useStripe } from '@stripe/react-stripe-js'
import { useState, type FormEvent } from 'react'
import { LockIcon } from '../common/Icons'
import type { StripePaymentFormProps } from './types/checkout-component.types'

export function StripePaymentForm({ session, onCompleted }: StripePaymentFormProps) {
  const stripe = useStripe()
  const elements = useElements()
  const [isPaying, setIsPaying] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!stripe || !elements) return
    setIsPaying(true)
    setError(null)

    const returnBase = session.returnUrl.replace(/\/$/, '')
    const result = await stripe.confirmPayment({
      elements,
      confirmParams: {
        return_url: `${returnBase}/${session.order.id}?payment=return`,
      },
      redirect: 'if_required',
    })

    if (result.error) {
      setError(result.error.message ?? 'Plata Stripe nu a putut fi confirmată.')
      setIsPaying(false)
      return
    }
    onCompleted()
  }

  return (
    <form className="checkout-form stripe-payment-form" onSubmit={submit}>
      <div className="checkout-form__heading">
        <span><LockIcon /></span>
        <div>
          <p className="home-kicker">Pasul 2</p>
          <h2>Plată Stripe sandbox</h2>
          <p>Nu se retrag bani reali. Folosește un card de test Stripe.</p>
        </div>
      </div>

      <div className="stripe-test-banner">
        <strong>Mod de test</strong>
        <span>Card: 4242 4242 4242 4242 · orice dată viitoare · orice CVC</span>
      </div>

      <PaymentElement options={{ layout: 'tabs' }} />

      {error && <div className="checkout-form__error" role="alert">{error}</div>}

      <button className="checkout-submit" type="submit" disabled={!stripe || isPaying}>
        {isPaying
          ? 'Confirmăm plata...'
          : `Plătește ${session.order.totalAmount.toFixed(2)} ${session.order.currency}`}
      </button>
      <small className="checkout-form__legal">
        Comanda devine activă numai după confirmarea webhookului Stripe.
      </small>
    </form>
  )
}
