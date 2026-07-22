import { useEffect, useState } from 'react'
import { Link, NavLink } from 'react-router-dom'
import { NavbarCart } from '../cart/NavbarCart'
import { CloseIcon, MenuIcon } from '../common/icons/AppIcons'
import { Logo } from '../common/Logo'
import { NavbarWishlist } from '../wishlist/NavbarWishlist'
import { UserMenu } from './UserMenu'

const navigationItems = [
  { label: 'Descoperă', to: '/#catalog' },
  { label: 'Vinde o carte', to: '/sell' },
] as const

export function Navbar() {
  const [menuOpen, setMenuOpen] = useState(false)

  const closeMenu = () => setMenuOpen(false)

  useEffect(() => {
    if (!menuOpen) return

    const mobileQuery = window.matchMedia('(max-width: 1040px)')
    const previousOverflow = document.body.style.overflow
    if (mobileQuery.matches) document.body.style.overflow = 'hidden'

    const handleBreakpointChange = (event: MediaQueryListEvent) => {
      if (!event.matches) setMenuOpen(false)
    }
    mobileQuery.addEventListener('change', handleBreakpointChange)

    return () => {
      document.body.style.overflow = previousOverflow
      mobileQuery.removeEventListener('change', handleBreakpointChange)
    }
  }, [menuOpen])

  return (
    <header className="app-navbar">
      <nav className="app-navbar__inner" aria-label="Navigație principală">
        <Link to="/" onClick={closeMenu} aria-label="BookNest — pagina principală">
          <Logo />
        </Link>

        <button
          className="app-navbar__menu-button"
          type="button"
          onClick={() => setMenuOpen((value) => !value)}
          aria-expanded={menuOpen}
          aria-controls="app-navigation-menu"
          aria-label={menuOpen ? 'Închide meniul' : 'Deschide meniul'}
        >
          {menuOpen ? <CloseIcon /> : <MenuIcon />}
        </button>

        <div
          className={`app-navbar__menu${menuOpen ? ' app-navbar__menu--open' : ''}`}
          id="app-navigation-menu"
        >
          <div className="app-navbar__links">
            <span className="app-navbar__section-label">Explorează</span>
            {navigationItems.map((item) => (
              <NavLink
                to={item.to}
                onClick={closeMenu}
                key={item.label}
                className={({ isActive }) => isActive ? 'app-navbar__link--active' : undefined}
              >
                {item.label}
              </NavLink>
            ))}
          </div>

          <div className="app-navbar__account">
            <span className="app-navbar__section-label">Colecția ta</span>
            <div className="app-navbar__quick-actions">
              <NavbarWishlist onNavigate={closeMenu} />
              <NavbarCart onNavigate={closeMenu} />
            </div>
            <UserMenu onNavigate={closeMenu} />
          </div>
        </div>
      </nav>
    </header>
  )
}
