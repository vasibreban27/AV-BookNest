import { UserIcon } from '../common/Icons'
import type { AuthTextFieldProps } from './types/auth-component.types'

export function AuthTextField({
  id,
  label,
  placeholder,
  error,
  registration,
}: AuthTextFieldProps) {
  return (
    <div className="field-group">
      <label htmlFor={id}>{label}</label>
      <div className={`input-wrap${error ? ' input-wrap--error' : ''}`}>
        <UserIcon className="input-icon" />
        <input
          id={id}
          type="text"
          autoComplete={id === 'firstName' ? 'given-name' : 'family-name'}
          placeholder={placeholder}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${id}-error` : undefined}
          {...registration}
        />
      </div>
      {error && (
        <span className="field-error" id={`${id}-error`}>
          {error}
        </span>
      )}
    </div>
  )
}
