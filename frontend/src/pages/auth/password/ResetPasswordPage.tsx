import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useSearchParams } from 'react-router-dom'
import { PasswordField } from '../../../components/auth/PasswordField'
import { authApi } from '../../../features/auth/api/authApi'
import { notifyAuthSessionExpired } from '../../../features/auth/events/authEvents'
import { resetPasswordSchema } from '../../../features/auth/schemas/auth.schemas'
import type { ResetPasswordFormValues } from '../../../features/auth/types/auth-form.types'
import { getFormErrorMessage } from '../../../features/auth/utils/authFormErrors'

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const [success, setSuccess] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { password: '', confirmPassword: '' },
  })

  const submit = handleSubmit(async ({ password }) => {
    setError(null)
    if (!token) {
      setError('Linkul nu conține tokenul de resetare.')
      return
    }
    try {
      await authApi.resetPassword(token, password)
      notifyAuthSessionExpired()
      window.history.replaceState(window.history.state, '', '/reset-password')
      setSuccess(true)
    } catch (requestError) {
      setError(getFormErrorMessage(requestError))
    }
  })

  if (success) {
    return (
      <div className="auth-form-page auth-status-page">
        <div className="auth-heading"><p className="eyebrow">Gata</p><h2>Parola a fost schimbată</h2><p>Sesiunile vechi nu vor mai putea fi reînnoite.</p></div>
        <p className="auth-switch"><Link to="/login">Intră în cont</Link></p>
      </div>
    )
  }

  return (
    <div className="auth-form-page">
      <div className="auth-heading"><p className="eyebrow">Securitatea contului</p><h2>Setează o parolă nouă</h2><p>Linkul poate fi folosit o singură dată.</p></div>
      {error && <div className="form-alert" role="alert"><span>!</span><p>{error}</p></div>}
      <form className="auth-form" onSubmit={submit} noValidate>
        <PasswordField label="Parolă nouă" autoComplete="new-password" error={errors.password?.message} registration={register('password')} />
        <PasswordField label="Confirmă parola" autoComplete="new-password" error={errors.confirmPassword?.message} registration={register('confirmPassword')} />
        <button className="primary-button" type="submit" disabled={isSubmitting}>{isSubmitting ? 'Salvăm...' : 'Schimbă parola'}</button>
      </form>
    </div>
  )
}
