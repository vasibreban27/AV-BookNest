import { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { authApi } from '../../../features/auth/api/authApi'
import { getFormErrorMessage } from '../../../features/auth/utils/authFormErrors'

export function VerifyEmailSentPage() {
  const location = useLocation()
  const initialEmail = (location.state as { email?: string } | null)?.email ?? ''
  const [email, setEmail] = useState(initialEmail)
  const [message, setMessage] = useState(
    initialEmail ? `Am trimis un link de verificare la ${initialEmail}.` : '',
  )
  const [error, setError] = useState<string | null>(null)
  const [pending, setPending] = useState(false)

  const resend = async () => {
    if (!email.trim()) return
    setPending(true)
    setError(null)
    try {
      await authApi.resendVerification(email.trim())
      setMessage('Dacă adresa aparține unui cont neverificat, am trimis un link nou.')
    } catch (requestError) {
      setError(getFormErrorMessage(requestError))
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="auth-form-page auth-status-page">
      <div className="auth-heading">
        <p className="eyebrow">Încă un pas</p>
        <h2>Verifică adresa de email</h2>
        <p>Deschide linkul primit pe email. Acesta este valabil 24 de ore.</p>
      </div>
      {message && <div className="form-success" role="status">{message}</div>}
      {error && <div className="form-alert" role="alert"><span>!</span><p>{error}</p></div>}
      <div className="auth-form">
        <div className="field-group">
          <label htmlFor="verification-email">Adresă de email</label>
          <div className="input-wrap">
            <input
              id="verification-email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="nume@exemplu.ro"
            />
          </div>
        </div>
        <button className="primary-button" type="button" onClick={resend} disabled={pending || !email.trim()}>
          {pending ? 'Trimitem...' : 'Retrimite linkul'}
        </button>
      </div>
      <p className="auth-switch"><Link to="/login">Înapoi la autentificare</Link></p>
    </div>
  )
}
