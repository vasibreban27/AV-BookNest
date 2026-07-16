import { useState } from 'react'
import { EyeIcon, LockIcon } from '../common/Icons'
import type { PasswordFieldProps } from './types/auth-component.types'

export function PasswordField({
  label,
  error,
  registration,
  ...inputProps
}: PasswordFieldProps) {
  const [visible, setVisible] = useState(false)
  const errorId = `${registration.name}-error`

  return (
    <div className="field-group">
      <label htmlFor={registration.name}>{label}</label>
      <div className={`input-wrap${error ? ' input-wrap--error' : ''}`}>
        <LockIcon className="input-icon" />
        <input
          id={registration.name}
          type={visible ? 'text' : 'password'}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? errorId : undefined}
          {...registration}
          {...inputProps}
        />
        <button
          className="password-toggle"
          type="button"
          onClick={() => setVisible((value) => !value)}
          aria-label={visible ? 'Ascunde parola' : 'Afișează parola'}
        >
          <EyeIcon open={visible} />
        </button>
      </div>
      {error && (
        <span className="field-error" id={errorId}>
          {error}
        </span>
      )}
    </div>
  )
}
