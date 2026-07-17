import { Link } from 'react-router-dom'
import { HeartIcon } from '../common/icons/AppIcons'
import type { WishlistErrorStateProps } from './types/wishlist-component.types'

export function WishlistLoadingState() {
  return (
    <div className="wishlist-loading" aria-label="Se încarcă favoritele">
      {Array.from({ length: 4 }, (_, index) => (
        <span key={index}><i /><i /><i /></span>
      ))}
    </div>
  )
}

export function WishlistEmptyState() {
  return (
    <div className="wishlist-state">
      <span className="wishlist-state__icon"><HeartIcon /></span>
      <p className="home-kicker">Colecția ta personală</p>
      <h2>Păstrează aproape poveștile preferate.</h2>
      <p>Apasă pe inimioara unei cărți pentru a o salva aici și a reveni la ea oricând.</p>
      <Link to={{ pathname: '/', hash: '#catalog' }}>Explorează catalogul</Link>
    </div>
  )
}

export function WishlistErrorState({ onRetry }: WishlistErrorStateProps) {
  return (
    <div className="wishlist-state wishlist-state--error" role="alert">
      <span className="wishlist-state__icon"><HeartIcon /></span>
      <h2>Nu am putut încărca favoritele.</h2>
      <p>Verifică backend-ul și încearcă din nou.</p>
      <button type="button" onClick={onRetry}>Reîncearcă</button>
    </div>
  )
}
