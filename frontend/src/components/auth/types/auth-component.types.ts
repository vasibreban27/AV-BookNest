import type { InputHTMLAttributes } from 'react'
import type { UseFormRegisterReturn } from 'react-hook-form'

export type PasswordFieldProps = Omit<
  InputHTMLAttributes<HTMLInputElement>,
  'type'
> & {
  label: string
  error?: string
  registration: UseFormRegisterReturn
}

export type AuthTextFieldProps = {
  id: 'firstName' | 'lastName'
  label: string
  placeholder: string
  error?: string
  registration: UseFormRegisterReturn<'firstName' | 'lastName'>
}
