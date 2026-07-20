import { Link } from 'react-router-dom'
import { PackageIcon, ReceiptIcon } from '../common/icons/AppIcons'
import type { OrdersErrorStateProps } from './types/order-component.types'

export function OrdersLoadingState() {
  return (
    <div className="orders-loading" aria-label="Se încarcă comenzile">
      {Array.from({ length: 3 }, (_, index) => <span key={index}><i /><i /><i /></span>)}
    </div>
  )
}

export function OrdersEmptyState() {
  return (
    <div className="orders-state">
      <span><ReceiptIcon /></span>
      <h2>Nu ai plasat încă nicio comandă.</h2>
      <p>Când alegi următoarea poveste, comanda și progresul livrării vor apărea aici.</p>
      <Link to={{ pathname: '/', hash: '#catalog' }}>Descoperă cărți</Link>
    </div>
  )
}

export function OrdersErrorState({ onRetry }: OrdersErrorStateProps) {
  return (
    <div className="orders-state orders-state--error" role="alert">
      <span><PackageIcon /></span>
      <h2>Nu am putut încărca comenzile.</h2>
      <p>Verifică backend-ul și încearcă din nou.</p>
      <button type="button" onClick={onRetry}>Reîncearcă</button>
    </div>
  )
}
