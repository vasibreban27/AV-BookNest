import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { authApi } from '../../../features/auth/api/authApi'
import { getFormErrorMessage } from '../../../features/auth/utils/authFormErrors'

export function VerifyEmailPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const started = useRef(false)
  const [status, setStatus] = useState<'pending' | 'success' | 'error'>(
    token ? 'pending' : 'error',
  )
  const [message, setMessage] = useState(
    token ? 'Verificăm linkul...' : 'Linkul nu conține tokenul de verificare.',
  )

  useEffect(() => {
    if (started.current) return
    started.current = true
    if (!token) return
    window.history.replaceState(window.history.state, '', '/verify-email')
    authApi.verifyEmail(token)
      .then(() => {
        setStatus('success')
        setMessage('Adresa a fost verificată. Acum te poți autentifica.')
      })
      .catch((error) => {
        setStatus('error')
        setMessage(getFormErrorMessage(error))
      })
  }, [token])

  return (
    <div className="auth-form-page auth-status-page">
      <div className="auth-heading">
        <p className="eyebrow">Securitatea contului</p>
        <h2>{status === 'success' ? 'Email verificat' : 'Confirmare email'}</h2>
        <p>{message}</p>
      </div>
      {status !== 'pending' && (
        <p className="auth-switch">
          <Link to={status === 'success' ? '/login' : '/verify-email-sent'}>
            {status === 'success' ? 'Intră în cont' : 'Cere un link nou'}
          </Link>
        </p>
      )}
    </div>
  )
}
