import { Link } from 'react-router-dom'
import { CartIcon, PackageIcon } from '../common/icons/AppIcons'
import type { CheckoutErrorStateProps } from './types/checkout-component.types'

export function CheckoutLoadingState() {
  return (
    <div className="checkout-loading" aria-label="Se pregătește checkout-ul">
      <span /><span />
    </div>
  )
}

export function CheckoutEmptyState() {
  return (
    <div className="checkout-state">
      <span><CartIcon /></span>
      <h2>Coșul tău este gol.</h2>
      <p>Ai nevoie de cel puțin o carte pentru a continua cu checkout-ul.</p>
      <Link to={{ pathname: '/', hash: '#catalog' }}>Descoperă cărți</Link>
    </div>
  )
}

export function CheckoutErrorState({ onRetry }: CheckoutErrorStateProps) {
  return (
    <div className="checkout-state checkout-state--error" role="alert">
      <span><PackageIcon /></span>
      <h2>Nu am putut pregăti comanda.</h2>
      <p>Verifică backend-ul și încearcă din nou.</p>
      <button type="button" onClick={onRetry}>Reîncearcă</button>
    </div>
  )
}
