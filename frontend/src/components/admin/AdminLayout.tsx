import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Logo } from '../common/Logo'
import { CloseIcon, LogoutIcon, MenuIcon } from '../common/icons/AppIcons'
import { useAuth } from '../../features/auth/hooks/useAuth'
import '../../styles/pages/admin/admin.css'

const adminNavigation = [
  { to: '/admin', end: true, mark: 'OV', label: 'Privire generală', description: 'Indicatori și cozi' },
  { to: '/admin/users', end: false, mark: 'UT', label: 'Utilizatori', description: 'Conturi și sesiuni' },
  { to: '/admin/books', end: false, mark: 'AN', label: 'Anunțuri', description: 'Catalog și moderare' },
  { to: '/admin/reviews', end: false, mark: 'RC', label: 'Recenzii', description: 'Reputație și moderare' },
  { to: '/admin/categories', end: false, mark: 'CT', label: 'Categorii', description: 'Structura catalogului' },
  { to: '/admin/orders', end: false, mark: 'CO', label: 'Comenzi', description: 'Plăți și livrări' },
  { to: '/admin/issues', end: false, mark: 'DS', label: 'Dispute', description: 'Decizii și rambursări' },
  { to: '/admin/operations', end: false, mark: 'OP', label: 'Operațiuni', description: 'Excepții și retry' },
  { to: '/admin/audit', end: false, mark: 'AU', label: 'Audit', description: 'Istoric administrativ' },
] as const

const pageTitles: Record<string, string> = {
  '/admin': 'Privire generală',
  '/admin/users': 'Utilizatori',
  '/admin/books': 'Anunțuri',
  '/admin/reviews': 'Recenzii și reputație',
  '/admin/categories': 'Categorii',
  '/admin/orders': 'Comenzi',
  '/admin/issues': 'Dispute',
  '/admin/operations': 'Operațiuni',
  '/admin/audit': 'Jurnal de audit',
}

export function AdminLayout() {
  const [menuOpen, setMenuOpen] = useState(false)
  const [loggingOut, setLoggingOut] = useState(false)
  const { user, logout } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'instant' })
  }, [location.pathname])

  useEffect(() => {
    if (!menuOpen) return
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => { document.body.style.overflow = previousOverflow }
  }, [menuOpen])

  const handleLogout = async () => {
    setLoggingOut(true)
    try {
      await logout()
      navigate('/login', { replace: true })
    } finally {
      setLoggingOut(false)
    }
  }

  const initials = `${user?.firstName[0] ?? ''}${user?.lastName[0] ?? ''}`.toUpperCase()

  return (
    <div className="admin-shell">
      <aside className={`admin-sidebar${menuOpen ? ' admin-sidebar--open' : ''}`} aria-label="Navigație administrare">
        <div className="admin-sidebar__brand">
          <Logo />
          <span>Admin</span>
          <button type="button" onClick={() => setMenuOpen(false)} aria-label="Închide meniul"><CloseIcon /></button>
        </div>
        <nav className="admin-sidebar__nav">
          <span className="admin-sidebar__label">Centru de control</span>
          {adminNavigation.map((item) => (
            <NavLink to={item.to} end={item.end} key={item.to} onClick={() => setMenuOpen(false)}>
              <span className="admin-sidebar__mark" aria-hidden="true">{item.mark}</span>
              <span><strong>{item.label}</strong><small>{item.description}</small></span>
            </NavLink>
          ))}
        </nav>
        <div className="admin-sidebar__footer">
          <NavLink to="/" className="admin-sidebar__store-link">← Înapoi în magazin</NavLink>
          <button type="button" onClick={() => void handleLogout()} disabled={loggingOut}>
            <LogoutIcon /> {loggingOut ? 'Se închide...' : 'Închide sesiunea'}
          </button>
        </div>
      </aside>
      {menuOpen && <button className="admin-sidebar__backdrop" type="button" aria-label="Închide meniul" onClick={() => setMenuOpen(false)} />}

      <div className="admin-workspace">
        <header className="admin-topbar">
          <button className="admin-topbar__menu" type="button" onClick={() => setMenuOpen(true)} aria-label="Deschide meniul"><MenuIcon /></button>
          <div><span>BookNest Admin</span><strong>{pageTitles[location.pathname] ?? 'Administrare'}</strong></div>
          <div className="admin-topbar__identity">
            <span className="admin-topbar__avatar">{initials}</span>
            <span><strong>{user?.firstName} {user?.lastName}</strong><small>{user?.email}</small></span>
          </div>
        </header>
        <Outlet />
      </div>
    </div>
  )
}
