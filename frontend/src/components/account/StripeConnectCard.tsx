import {
  useStripeConnectStatus,
  useStripeDashboard,
  useStripeOnboarding,
} from '../../features/payments/hooks/useStripeConnect'
import { LockIcon } from '../common/Icons'

export function StripeConnectCard() {
  const status = useStripeConnectStatus()
  const onboarding = useStripeOnboarding()
  const dashboard = useStripeDashboard()

  const content = () => {
    if (status.isLoading) {
      return <p>Verificăm contul Stripe sandbox...</p>
    }
    if (status.isError || !status.data?.sandboxConfigured) {
      return (
        <>
          <strong>Stripe sandbox nu este configurat</strong>
          <p>Adaugă cheile pk_test_ și sk_test_ în backend pentru a activa onboardingul.</p>
        </>
      )
    }
    if (status.data.payoutsEnabled) {
      return (
        <>
          <strong>Cont Stripe sandbox pregătit</strong>
          <p>Poți primi transferurile de test pentru cărțile vândute.</p>
          <span className="stripe-connect-card__status">Payouts active · fără bani reali</span>
          <button
            type="button"
            onClick={() => dashboard.mutate()}
            disabled={dashboard.isPending}
          >
            {dashboard.isPending ? 'Deschidem Stripe...' : 'Deschide Express Dashboard'}
          </button>
          {dashboard.isError && <small>Dashboardul Stripe nu a putut fi deschis.</small>}
        </>
      )
    }
    return (
      <>
        <strong>
          {status.data.connected
            ? 'Finalizează verificarea Stripe'
            : 'Conectează Stripe pentru vânzări'}
        </strong>
        <p>
          Stripe colectează datele de identitate și contul bancar de test. BookNest nu vede
          aceste date.
        </p>
        <button
          type="button"
          onClick={() => onboarding.mutate()}
          disabled={onboarding.isPending}
        >
          {onboarding.isPending
            ? 'Deschidem Stripe...'
            : status.data.connected
              ? 'Continuă onboardingul'
              : 'Conectează Stripe sandbox'}
        </button>
        {onboarding.isError && (
          <small>Linkul Stripe nu a putut fi creat. Încearcă din nou.</small>
        )}
      </>
    )
  }

  return (
    <section className="stripe-connect-card">
      <span><LockIcon /></span>
      <div>{content()}</div>
    </section>
  )
}
