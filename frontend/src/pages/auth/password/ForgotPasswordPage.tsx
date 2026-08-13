import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router-dom'
import { authApi } from '../../../features/auth/api/authApi'
import { forgotPasswordSchema } from '../../../features/auth/schemas/auth.schemas'
import type { ForgotPasswordFormValues } from '../../../features/auth/types/auth-form.types'
import { getFormErrorMessage } from '../../../features/auth/utils/authFormErrors'

export function ForgotPasswordPage() {
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<ForgotPasswordFormValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: '' },
  })

  const submit = handleSubmit(async ({ email }) => {
    setError(null)
    try {
      await authApi.forgotPassword(email)
      setMessage('Dacă există un cont pentru această adresă, vei primi un link valabil 30 de minute.')
    } catch (requestError) {
      setError(getFormErrorMessage(requestError))
    }
  })

  return (
    <div className="auth-form-page">
      <div className="auth-heading">
        <p className="eyebrow">Recuperează accesul</p>
        <h2>Ai uitat parola?</h2>
        <p>Introdu emailul contului și îți trimitem un link temporar.</p>
      </div>
      {message && <div className="form-success" role="status">{message}</div>}
      {error && <div className="form-alert" role="alert"><span>!</span><p>{error}</p></div>}
      <form className="auth-form" onSubmit={submit} noValidate>
        <div className="field-group">
          <label htmlFor="forgot-email">Adresă de email</label>
          <div className={`input-wrap${errors.email ? ' input-wrap--error' : ''}`}>
            <input id="forgot-email" type="email" autoFocus {...register('email')} />
          </div>
          {errors.email && <span className="field-error">{errors.email.message}</span>}
        </div>
        <button className="primary-button" type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Trimitem...' : 'Trimite linkul de resetare'}
        </button>
      </form>
      <p className="auth-switch"><Link to="/login">Înapoi la autentificare</Link></p>
    </div>
  )
}
