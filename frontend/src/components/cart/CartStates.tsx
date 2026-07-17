import { Link } from 'react-router-dom'
import { BookOutlineIcon, CartIcon } from '../common/icons/AppIcons'
import type { CartErrorStateProps } from './types/cart-component.types'

export function CartLoadingState() {
  return (
    <div className="cart-loading" aria-label="Se încarcă coșul">
      {Array.from({ length: 2 }, (_, index) => (
        <span key={index}><i /><i /><i /></span>
      ))}
    </div>
  )
}

export function CartEmptyState() {
  return (
    <div className="cart-state">
      <span className="cart-state__icon"><CartIcon /></span>
      <p className="home-kicker">Un raft nou te așteaptă</p>
      <h2>Coșul tău este încă gol.</h2>
      <p>Explorează volumele comunității și păstrează aici cărțile pe care vrei să le citești.</p>
      <Link to={{ pathname: '/', hash: '#catalog' }}>Descoperă cărți</Link>
    </div>
  )
}

export function CartErrorState({ onRetry }: CartErrorStateProps) {
  return (
    <div className="cart-state cart-state--error" role="alert">
      <span className="cart-state__icon"><BookOutlineIcon /></span>
      <h2>Nu am putut deschide coșul.</h2>
      <p>Verifică backend-ul și încearcă din nou.</p>
      <button type="button" onClick={onRetry}>Reîncearcă</button>
    </div>
  )
}
