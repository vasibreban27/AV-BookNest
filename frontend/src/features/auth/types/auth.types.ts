import type { InternalAxiosRequestConfig } from 'axios'
import type { ReactNode } from 'react'

export type UserRole = 'USER' | 'ADMIN'

export type User = {
  id: number
  firstName: string
  lastName: string
  email: string
  role: UserRole
  emailVerified: boolean
}

export type LoginPayload = {
  email: string
  password: string
}

export type RegisterPayload = LoginPayload & {
  firstName: string
  lastName: string
}

export type AuthResponse = {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export type ApiErrorResponse = {
  status: number
  error: string
  message: string
  path: string
  fieldErrors?: Record<string, string>
}

export type RetryableRequest = InternalAxiosRequestConfig & {
  _retry?: boolean
}

export type AuthContextValue = {
  user: User | null
  isAuthenticated: boolean
  isInitializing: boolean
  login: (payload: LoginPayload) => Promise<void>
  register: (payload: RegisterPayload) => Promise<void>
  logout: () => Promise<void>
}

export type AuthProviderProps = {
  children: ReactNode
}
