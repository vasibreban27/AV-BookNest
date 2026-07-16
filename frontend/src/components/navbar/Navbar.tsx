import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../features/auth/hooks/useAuth'
import { CloseIcon, LogoutIcon, MenuIcon } from '../common/icons/AppIcons'
import { Logo } from '../common/Logo'

const navigationItems = [
  { label: 'Acasă', hash: '' },
  { label: 'Recomandări', hash: '#recomandari' },
  { label: 'Descoperă', hash: '#catalog' },
] as const

export function Navbar() {
  const { user, logout } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const initials = `${user?.firstName[0] ?? ''}${user?.lastName[0] ?? ''}`.toUpperCase()

  const closeMenu = () => setMenuOpen(false)

  const handleLogout = async () => {
    setIsLoggingOut(true)
    await logout()
  }

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
            {navigationItems.map((item) => (
              <Link
                to={{ pathname: '/', hash: item.hash }}
                onClick={closeMenu}
                key={item.label}
              >
                {item.label}
              </Link>
            ))}
          </div>

          <div className="app-navbar__account">
            <Link className="app-navbar__profile" to="/account" onClick={closeMenu}>
              <span>{initials}</span>
              <span>
                <small>Salut,</small>
                {user?.firstName}
              </span>
            </Link>
            <button
              className="app-navbar__logout"
              type="button"
              onClick={handleLogout}
              disabled={isLoggingOut}
              aria-label={isLoggingOut ? 'Se închide sesiunea' : 'Ieși din cont'}
            >
              <LogoutIcon />
            </button>
          </div>
        </div>
      </nav>
    </header>
  )
}
