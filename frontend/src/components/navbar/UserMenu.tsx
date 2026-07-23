import { useEffect, useRef, useState } from 'react'
import { NavLink } from 'react-router-dom'
import { useAuth } from '../../features/auth/hooks/useAuth'
import { UserIcon } from '../common/Icons'
import {
  BookOutlineIcon,
  ChevronDownIcon,
  LogoutIcon,
  PackageIcon,
} from '../common/icons/AppIcons'
import type { UserMenuProps } from './types/navbar-component.types'

function AccountLinks({ onNavigate }: UserMenuProps) {
  return (
    <div className="user-menu__links">
      <NavLink to="/my-books" onClick={onNavigate}>
        <BookOutlineIcon />
        <span><strong>Cărțile mele</strong><small>Gestionează anunțurile</small></span>
      </NavLink>
      <NavLink to="/orders" onClick={onNavigate}>
        <PackageIcon />
        <span><strong>Comenzile mele</strong><small>Urmărește cumpărăturile</small></span>
      </NavLink>
      <NavLink to="/sales" onClick={onNavigate}>
        <PackageIcon />
        <span><strong>Vânzările mele</strong><small>Procesează comenzile primite</small></span>
      </NavLink>
      <NavLink to="/account" onClick={onNavigate}>
        <UserIcon />
        <span><strong>Contul meu</strong><small>Date personale și securitate</small></span>
      </NavLink>
    </div>
  )
}

export function UserMenu({ onNavigate }: UserMenuProps) {
  const { user, logout } = useAuth()
  const [isOpen, setIsOpen] = useState(false)
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)
  const triggerRef = useRef<HTMLButtonElement>(null)
  const initials = `${user?.firstName[0] ?? ''}${user?.lastName[0] ?? ''}`.toUpperCase()

  useEffect(() => {
    const handlePointerDown = (event: PointerEvent) => {
      if (!menuRef.current?.contains(event.target as Node)) setIsOpen(false)
    }
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key !== 'Escape' || !isOpen) return
      setIsOpen(false)
      triggerRef.current?.focus()
    }

    document.addEventListener('pointerdown', handlePointerDown)
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown)
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [isOpen])

  const handleNavigate = () => {
    setIsOpen(false)
    onNavigate()
  }

  const handleLogout = async () => {
    setIsLoggingOut(true)
    try {
      await logout()
    } finally {
      setIsLoggingOut(false)
    }
  }

  return (
    <div className="user-menu" ref={menuRef}>
      <button
        className="user-menu__trigger"
        type="button"
        onClick={() => setIsOpen((value) => !value)}
        aria-expanded={isOpen}
        aria-controls="user-navigation-menu"
        ref={triggerRef}
      >
        <span className="user-menu__avatar">{initials}</span>
        <span className="user-menu__identity">
          <small>Salut,</small>
          <strong>{user?.firstName}</strong>
        </span>
        <ChevronDownIcon className={isOpen ? 'user-menu__chevron--open' : undefined} />
      </button>

      <div
        className={`user-menu__dropdown${isOpen ? ' user-menu__dropdown--open' : ''}`}
        id="user-navigation-menu"
        aria-label="Meniul contului"
      >
        <div className="user-menu__welcome">
          <strong>{user?.firstName} {user?.lastName}</strong>
          <small>{user?.email}</small>
        </div>

        <AccountLinks onNavigate={handleNavigate} />

        <button
          className="user-menu__logout"
          type="button"
          onClick={handleLogout}
          disabled={isLoggingOut}
        >
          <LogoutIcon />
          {isLoggingOut ? 'Se închide sesiunea...' : 'Ieși din cont'}
        </button>
      </div>

      <div className="user-menu__mobile-panel">
        <div className="user-menu__mobile-profile">
          <span className="user-menu__avatar">{initials}</span>
          <span>
            <strong>{user?.firstName} {user?.lastName}</strong>
            <small>{user?.email}</small>
          </span>
        </div>
        <AccountLinks onNavigate={handleNavigate} />
        <button
          className="user-menu__logout"
          type="button"
          onClick={handleLogout}
          disabled={isLoggingOut}
        >
          <LogoutIcon />
          {isLoggingOut ? 'Se închide sesiunea...' : 'Ieși din cont'}
        </button>
      </div>
    </div>
  )
}
