import type { AuthResponse } from '../types/auth.types'

const SESSION_KEY = 'booknest.auth.session'
export const AUTH_SESSION_EVENT = 'booknest:auth-session-changed'

export function getStoredSession(): AuthResponse | null {
  try {
    const value = localStorage.getItem(SESSION_KEY)
    return value ? (JSON.parse(value) as AuthResponse) : null
  } catch {
    localStorage.removeItem(SESSION_KEY)
    return null
  }
}

export function storeSession(session: AuthResponse) {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  window.dispatchEvent(new Event(AUTH_SESSION_EVENT))
}

export function clearStoredSession() {
  localStorage.removeItem(SESSION_KEY)
  window.dispatchEvent(new Event(AUTH_SESSION_EVENT))
}
