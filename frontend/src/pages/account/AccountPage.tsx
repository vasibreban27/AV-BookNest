import { CheckIcon, MailIcon, UserIcon } from '../../components/common/Icons'
import { useAuth } from '../../features/auth/hooks/useAuth'

export function AccountPage() {
  const { user } = useAuth()

  if (!user) return null

  const initials = `${user.firstName[0] ?? ''}${user.lastName[0] ?? ''}`.toUpperCase()

  return (
    <main className="account-page">
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
