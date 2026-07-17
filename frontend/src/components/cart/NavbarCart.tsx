import { Link } from 'react-router-dom'
import { useCart } from '../../features/cart/hooks/useCart'
import { formatBookPrice } from '../../features/catalog/utils/catalogFormatters'
import { CartIcon } from '../common/icons/AppIcons'
import type { NavbarCartProps } from './types/cart-component.types'

export function NavbarCart({ onNavigate }: NavbarCartProps) {
  const { data: cart, isLoading, isError } = useCart()
  const itemCount = cart?.items.length ?? 0
  const accessibleLabel = isLoading
    ? 'Se încarcă coșul'
    : `Coșul meu, ${itemCount} ${itemCount === 1 ? 'carte' : 'cărți'}`

  return (
    <div className="navbar-cart">
      <Link className="navbar-cart__trigger" to="/cart" onClick={onNavigate} aria-label={accessibleLabel}>
        <CartIcon />
        {itemCount > 0 && <span className="navbar-cart__badge">{itemCount > 9 ? '9+' : itemCount}</span>}
      </Link>

      <div className="navbar-cart__preview">
        <div className="navbar-cart__heading">
          <strong>Coșul tău</strong>
          <span>{itemCount} {itemCount === 1 ? 'carte' : 'cărți'}</span>
        </div>

        {isLoading && <p>Încărcăm selecția ta...</p>}
        {isError && <p>Coșul nu poate fi încărcat momentan.</p>}
        {!isLoading && !isError && itemCount === 0 && (
          <p>Coșul este gol. Descoperă o poveste nouă.</p>
        )}
        {!isLoading && !isError && cart && itemCount > 0 && (
          <>
            <ul>
              {cart.items.slice(0, 3).map((item) => (
                <li key={item.id}>
                  <span>{item.book.title}</span>
                  <strong>{formatBookPrice(item.book.price)}</strong>
                </li>
              ))}
            </ul>
            {itemCount > 3 && <small>+ încă {itemCount - 3} în coș</small>}
            <div className="navbar-cart__total">
              <span>Total</span>
              <strong>{formatBookPrice(cart.total)}</strong>
            </div>
          </>
        )}

        <Link className="navbar-cart__view" to="/cart" onClick={onNavigate}>Vezi coșul</Link>
      </div>
    </div>
  )
}
