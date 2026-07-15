import { useState } from 'react'
import { CheckIcon, MailIcon, UserIcon } from '../../components/common/Icons'
import { Logo } from '../../components/common/Logo'
import { useAuth } from '../../features/auth/hooks/useAuth'

export function AccountPage() {
  const { user, logout } = useAuth()
  const [isLoggingOut, setIsLoggingOut] = useState(false)

  if (!user) return null

  const initials = `${user.firstName[0] ?? ''}${user.lastName[0] ?? ''}`.toUpperCase()

  const handleLogout = async () => {
    setIsLoggingOut(true)
    await logout()
  }

  return (
    <main className="account-page">
      <header className="account-header">
        <Logo />
        <button className="secondary-button" type="button" onClick={handleLogout} disabled={isLoggingOut}>
          {isLoggingOut ? 'Se închide sesiunea...' : 'Ieși din cont'}
        </button>
      </header>

      <section className="account-content">
        <div className="success-banner">
          <span><CheckIcon /></span>
          <div>
            <strong>Autentificare reușită</strong>
            <p>Contul tău este conectat la API-ul BookNest.</p>
          </div>
        </div>

        <div className="profile-card">
          <div className="profile-card__header">
            <div className="avatar">{initials}</div>
            <div>
              <p className="eyebrow">Contul meu</p>
              <h1>{user.firstName} {user.lastName}</h1>
              <span className="role-badge">{user.role === 'ADMIN' ? 'Administrator' : 'Cititor BookNest'}</span>
            </div>
          </div>

          <div className="profile-details">
            <div>
              <MailIcon />
              <span><small>Adresă de email</small>{user.email}</span>
            </div>
            <div>
              <UserIcon />
              <span><small>Starea contului</small>{user.emailVerified ? 'Email verificat' : 'Email neverificat'}</span>
            </div>
          </div>
        </div>
      </section>
    </main>
  )
}
