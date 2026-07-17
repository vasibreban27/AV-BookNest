import { Link } from 'react-router-dom'
import { useWishlist } from '../../features/wishlist/hooks/useWishlist'
import { HeartIcon } from '../common/icons/AppIcons'
import type { NavbarWishlistProps } from './types/wishlist-component.types'

export function NavbarWishlist({ onNavigate }: NavbarWishlistProps) {
  const { data: wishlist, isLoading, isError } = useWishlist()
  const itemCount = wishlist?.length ?? 0
  const accessibleLabel = isLoading
    ? 'Se încarcă favoritele'
    : `Favorite, ${itemCount} ${itemCount === 1 ? 'carte' : 'cărți'}`

  return (
    <div className="navbar-collection navbar-wishlist">
      <Link className="navbar-collection__trigger" to="/wishlist" onClick={onNavigate} aria-label={accessibleLabel}>
        <HeartIcon />
        {itemCount > 0 && <span className="navbar-collection__badge">{itemCount > 9 ? '9+' : itemCount}</span>}
      </Link>

      <div className="navbar-collection__preview">
        <div className="navbar-collection__heading">
          <strong>Favoritele tale</strong>
          <span>{itemCount} {itemCount === 1 ? 'carte' : 'cărți'}</span>
        </div>

        {isLoading && <p>Încărcăm cărțile preferate...</p>}
        {isError && <p>Favoritele nu pot fi încărcate momentan.</p>}
        {!isLoading && !isError && itemCount === 0 && (
          <p>Nu ai salvat încă nicio carte.</p>
        )}
        {!isLoading && !isError && wishlist && itemCount > 0 && (
          <>
            <ul>
              {wishlist.slice(0, 3).map((item) => (
                <li key={item.book.id}>
                  <span>
                    <strong>{item.book.title}</strong>
                    <small>{item.book.author}</small>
                  </span>
                </li>
              ))}
            </ul>
            {itemCount > 3 && <small>+ încă {itemCount - 3} salvate</small>}
          </>
        )}

        <Link className="navbar-collection__view" to="/wishlist" onClick={onNavigate}>Vezi favoritele</Link>
      </div>
    </div>
  )
}
