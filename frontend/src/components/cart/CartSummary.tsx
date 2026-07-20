import { useState } from 'react'
import { Link } from 'react-router-dom'
import { formatBookPrice } from '../../features/catalog/utils/catalogFormatters'
import { ArrowUpRightIcon, TrashIcon } from '../common/icons/AppIcons'
import type { CartSummaryProps } from './types/cart-component.types'

export function CartSummary({ itemCount, total, isClearing, onClear }: CartSummaryProps) {
  const [isConfirmingClear, setIsConfirmingClear] = useState(false)

  const handleClear = () => {
    if (!isConfirmingClear) {
      setIsConfirmingClear(true)
      return
    }
    onClear()
    setIsConfirmingClear(false)
  }

  return (
    <aside className="cart-summary" aria-label="Sumarul coșului">
      <span className="cart-summary__eyebrow">Sumar</span>
      <h2>Selecția ta</h2>

      <div className="cart-summary__line">
        <span>{itemCount} {itemCount === 1 ? 'carte' : 'cărți'}</span>
        <strong>{formatBookPrice(total)}</strong>
      </div>
      <div className="cart-summary__line">
        <span>Livrare</span>
        <small>Se stabilește la comandă</small>
      </div>
      <div className="cart-summary__total">
        <span>Total cărți</span>
        <strong>{formatBookPrice(total)}</strong>
      </div>

      <Link className="cart-summary__checkout" to="/checkout">
        Continuă către checkout <ArrowUpRightIcon />
      </Link>
      <Link className="cart-summary__continue" to={{ pathname: '/', hash: '#catalog' }}>Continuă cumpărăturile</Link>

      <div className="cart-summary__clear">
        <button type="button" onClick={handleClear} disabled={isClearing}>
          <TrashIcon />
          {isClearing ? 'Se golește...' : isConfirmingClear ? 'Confirmă golirea' : 'Golește coșul'}
        </button>
        {isConfirmingClear && (
          <button type="button" onClick={() => setIsConfirmingClear(false)}>Anulează</button>
        )}
      </div>
    </aside>
  )
}
