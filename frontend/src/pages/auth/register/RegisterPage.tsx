import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { AuthTextField } from '../../../components/auth/AuthTextField'
import { PasswordField } from '../../../components/auth/PasswordField'
import { ArrowIcon, MailIcon } from '../../../components/common/Icons'
import { useAuth } from '../../../features/auth/hooks/useAuth'
import { registerSchema } from '../../../features/auth/schemas/auth.schemas'
import type { RegisterFormValues } from '../../../features/auth/types/auth-form.types'
import {
  applyApiFieldErrors,
  getFormErrorMessage,
} from '../../../features/auth/utils/authFormErrors'

export function RegisterPage() {
  const { register: createAccount } = useAuth()
  const navigate = useNavigate()
  const [submitError, setSubmitError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      confirmPassword: '',
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    setSubmitError(null)
    try {
      await createAccount({
        firstName: values.firstName,
        lastName: values.lastName,
        email: values.email,
        password: values.password,
      })
      navigate('/', { replace: true })
    } catch (error) {
      applyApiFieldErrors(error, setError)
      setSubmitError(getFormErrorMessage(error))
    }
  })

  return (
    <div className="auth-form-page auth-form-page--register">
      <div className="auth-heading">
        <p className="eyebrow">Alătură-te comunității</p>
        <h2>Creează-ți contul</h2>
        <p>Primul capitol începe cu doar câteva detalii.</p>
      </div>

      {submitError && (
        <div className="form-alert" role="alert">
          <span>!</span>
          <p>{submitError}</p>
        </div>
      )}

      <form className="auth-form" onSubmit={onSubmit} noValidate>
        <div className="form-row">
          <AuthTextField
            id="firstName"
            label="Prenume"
            placeholder="Ana"
            error={errors.firstName?.message}
            registration={register('firstName')}
          />
          <AuthTextField
            id="lastName"
            label="Nume"
            placeholder="Popescu"
            error={errors.lastName?.message}
            registration={register('lastName')}
          />
        </div>

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
          placeholder="Minimum 8 caractere"
          autoComplete="new-password"
          error={errors.password?.message}
          registration={register('password')}
        />
        <PasswordField
          label="Confirmă parola"
          placeholder="Reintrodu parola"
          autoComplete="new-password"
          error={errors.confirmPassword?.message}
          registration={register('confirmPassword')}
        />

        <p className="password-hint">Folosește cel puțin 8 caractere, o literă și o cifră.</p>

        <button className="primary-button" type="submit" disabled={isSubmitting}>
          <span>{isSubmitting ? 'Se creează contul...' : 'Creează cont'}</span>
          {isSubmitting ? <span className="button-spinner" /> : <ArrowIcon />}
        </button>
      </form>

      <p className="auth-switch">
        Ai deja un cont? <Link to="/login">Intră în cont</Link>
      </p>
    </div>
  )
}
