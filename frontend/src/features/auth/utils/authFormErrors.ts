import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import { getApiError, isNetworkError } from '../../../api/client'

const messageTranslations: Record<string, string> = {
  'Invalid email or password': 'Adresa de email sau parola este incorectă.',
  'An account already exists for this email address':
    'Există deja un cont asociat acestei adrese de email.',
  'Validation failed': 'Verifică datele introduse și încearcă din nou.',
}

const fieldTranslations: Record<string, string> = {
  'must not be blank': 'Câmpul este obligatoriu.',
  'must be a well-formed email address': 'Introdu o adresă de email validă.',
  'Password must contain between 8 and 72 characters':
    'Parola trebuie să conțină între 8 și 72 de caractere.',
  'Password must contain a letter': 'Parola trebuie să conțină o literă.',
  'Password must contain a digit': 'Parola trebuie să conțină o cifră.',
}

export function applyApiFieldErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
) {
  const apiError = getApiError(error)
  if (!apiError?.fieldErrors) return

  Object.entries(apiError.fieldErrors).forEach(([field, message]) => {
    setError(field as Path<T>, {
      type: 'server',
      message: fieldTranslations[message] ?? message,
    })
  })
}

export function getFormErrorMessage(error: unknown) {
  if (isNetworkError(error)) {
    return 'Nu am putut contacta serverul. Verifică dacă backend-ul rulează și încearcă din nou.'
  }

  const apiError = getApiError(error)
  if (!apiError) return 'A apărut o problemă neașteptată. Încearcă din nou.'
  return messageTranslations[apiError.message] ?? apiError.message
}
