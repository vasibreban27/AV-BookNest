import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { StripeConnectCard } from '../../components/account/StripeConnectCard'
import { PasswordField } from '../../components/auth/PasswordField'
import { CheckIcon, MailIcon, UserIcon } from '../../components/common/Icons'
import { authApi } from '../../features/auth/api/authApi'
import { useAuth } from '../../features/auth/hooks/useAuth'
import {
  changePasswordSchema,
  profileSchema,
} from '../../features/auth/schemas/auth.schemas'
import type {
  ChangePasswordFormValues,
  ProfileFormValues,
} from '../../features/auth/types/auth-form.types'
import {
  applyApiFieldErrors,
  getFormErrorMessage,
} from '../../features/auth/utils/authFormErrors'

export function AccountPage() {
  const { user, updateUser, logout } = useAuth()
  const navigate = useNavigate()
  const [profileMessage, setProfileMessage] = useState<string | null>(null)
  const [profileError, setProfileError] = useState<string | null>(null)
  const [passwordError, setPasswordError] = useState<string | null>(null)

  const profileForm = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    values: {
      firstName: user?.firstName ?? '',
      lastName: user?.lastName ?? '',
      phoneNumber: user?.phoneNumber ?? '',
    },
  })
  const passwordForm = useForm<ChangePasswordFormValues>({
    resolver: zodResolver(changePasswordSchema),
    defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
  })

  if (!user) return null

  const initials = `${user.firstName[0] ?? ''}${user.lastName[0] ?? ''}`.toUpperCase()

  const saveProfile = profileForm.handleSubmit(async (values) => {
    setProfileMessage(null)
    setProfileError(null)
    try {
      const updated = await authApi.updateProfile(values)
      updateUser(updated)
      setProfileMessage('Datele profilului au fost actualizate.')
    } catch (error) {
      applyApiFieldErrors(error, profileForm.setError)
      setProfileError(getFormErrorMessage(error))
    }
  })

  const savePassword = passwordForm.handleSubmit(async (values) => {
    setPasswordError(null)
    try {
      await authApi.changePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      })
      await logout()
      navigate('/login', { replace: true, state: { passwordChanged: true } })
    } catch (error) {
      applyApiFieldErrors(error, passwordForm.setError)
      setPasswordError(getFormErrorMessage(error))
    }
  })

  return (
    <main className="account-page">
      <section className="account-content">
        <div className="profile-card">
          <div className="profile-card__header">
            <div className="avatar">{initials}</div>
            <div>
              <p className="eyebrow">Contul meu</p>
              <h1>{user.firstName} {user.lastName}</h1>
              <span className="role-badge">
                {user.role === 'ADMIN' ? 'Administrator' : 'Cititor BookNest'}
              </span>
            </div>
          </div>

          <div className="profile-details">
            <div>
              <MailIcon />
              <span><small>Adresă de email</small>{user.email}</span>
            </div>
            <div>
              <UserIcon />
              <span>
                <small>Starea contului</small>
                {user.emailVerified ? 'Email verificat' : 'Email neverificat'}
              </span>
            </div>
          </div>
        </div>

        <section className="account-settings-card">
          <header>
            <div>
              <p className="eyebrow">Date personale</p>
              <h2>Editează profilul</h2>
            </div>
            <p>Emailul nu se modifică aici deoarece schimbarea lui necesită o nouă verificare.</p>
          </header>

          {profileMessage && (
            <div className="form-success" role="status"><CheckIcon />{profileMessage}</div>
          )}
          {profileError && <div className="form-alert" role="alert"><span>!</span><p>{profileError}</p></div>}

          <form className="account-settings-form" onSubmit={saveProfile} noValidate>
            <div className="account-settings-form__row">
              <label>
                <span>Prenume</span>
                <input type="text" autoComplete="given-name" {...profileForm.register('firstName')} />
                {profileForm.formState.errors.firstName && <small>{profileForm.formState.errors.firstName.message}</small>}
              </label>
              <label>
                <span>Nume</span>
                <input type="text" autoComplete="family-name" {...profileForm.register('lastName')} />
                {profileForm.formState.errors.lastName && <small>{profileForm.formState.errors.lastName.message}</small>}
              </label>
            </div>
            <label>
              <span>Număr de telefon</span>
              <input type="tel" autoComplete="tel" placeholder="+40 7..." {...profileForm.register('phoneNumber')} />
              {profileForm.formState.errors.phoneNumber && <small>{profileForm.formState.errors.phoneNumber.message}</small>}
            </label>
            <button type="submit" disabled={profileForm.formState.isSubmitting || !profileForm.formState.isDirty}>
              {profileForm.formState.isSubmitting ? 'Salvăm...' : 'Salvează modificările'}
            </button>
          </form>
        </section>

        <section className="account-settings-card account-settings-card--security">
          <header>
            <div><p className="eyebrow">Securitate</p><h2>Schimbă parola</h2></div>
            <p>După schimbare vei fi delogat, iar sesiunile existente nu vor mai putea fi reînnoite.</p>
          </header>
          {passwordError && <div className="form-alert" role="alert"><span>!</span><p>{passwordError}</p></div>}
          <form className="account-settings-form" onSubmit={savePassword} noValidate>
            <PasswordField
              label="Parola curentă"
              autoComplete="current-password"
              error={passwordForm.formState.errors.currentPassword?.message}
              registration={passwordForm.register('currentPassword')}
            />
            <div className="account-settings-form__row">
              <PasswordField
                label="Parola nouă"
                autoComplete="new-password"
                error={passwordForm.formState.errors.newPassword?.message}
                registration={passwordForm.register('newPassword')}
              />
              <PasswordField
                label="Confirmă parola nouă"
                autoComplete="new-password"
                error={passwordForm.formState.errors.confirmPassword?.message}
                registration={passwordForm.register('confirmPassword')}
              />
            </div>
            <button type="submit" disabled={passwordForm.formState.isSubmitting}>
              {passwordForm.formState.isSubmitting ? 'Schimbăm...' : 'Schimbă parola'}
            </button>
          </form>
        </section>

        <StripeConnectCard />
      </section>
    </main>
  )
}
