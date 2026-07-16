import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { PasswordField } from '../../../components/auth/PasswordField'
import { ArrowIcon, MailIcon } from '../../../components/common/Icons'
import { useAuth } from '../../../features/auth/hooks/useAuth'
import { loginSchema } from '../../../features/auth/schemas/auth.schemas'
import type { LoginFormValues } from '../../../features/auth/types/auth-form.types'
import {
  applyApiFieldErrors,
  getFormErrorMessage,
} from '../../../features/auth/utils/authFormErrors'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [submitError, setSubmitError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })

  const destination =
    (location.state as { from?: string } | null)?.from ?? '/'

  const onSubmit = handleSubmit(async (values) => {
    setSubmitError(null)
    try {
      await login(values)
      navigate(destination, { replace: true })
    } catch (error) {
      applyApiFieldErrors(error, setError)
      setSubmitError(getFormErrorMessage(error))
    }
  })

  return (
    <div className="auth-form-page">
      <div className="auth-heading">
        <p className="eyebrow">Bine ai revenit</p>
        <h2>Intră în contul tău</h2>
        <p>Continuă de unde ai rămas în comunitatea BookNest.</p>
      </div>

      {submitError && (
        <div className="form-alert" role="alert">
          <span>!</span>
          <p>{submitError}</p>
        </div>
      )}

      <form className="auth-form" onSubmit={onSubmit} noValidate>
        <div className="field-group">
          <label htmlFor="email">Adresă de email</label>
          <div className={`input-wrap${errors.email ? ' input-wrap--error' : ''}`}>
            <MailIcon className="input-icon" />
            <input
              id="email"
              type="email"
              autoComplete="email"
              placeholder="nume@exemplu.ro"
              aria-invalid={Boolean(errors.email)}
              aria-describedby={errors.email ? 'email-error' : undefined}
              autoFocus
              {...register('email')}
            />
          </div>
          {errors.email && (
            <span className="field-error" id="email-error">
              {errors.email.message}
            </span>
          )}
        </div>

        <PasswordField
          label="Parolă"
          placeholder="Introdu parola"
          autoComplete="current-password"
          error={errors.password?.message}
          registration={register('password')}
        />

        <button className="primary-button" type="submit" disabled={isSubmitting}>
          <span>{isSubmitting ? 'Se autentifică...' : 'Intră în cont'}</span>
          {isSubmitting ? <span className="button-spinner" /> : <ArrowIcon />}
        </button>
      </form>

      <p className="auth-switch">
        Nu ai încă un cont? <Link to="/register">Creează unul</Link>
      </p>
    </div>
  )
}
