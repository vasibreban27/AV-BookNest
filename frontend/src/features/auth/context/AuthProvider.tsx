import { useCallback, useEffect, useMemo, useState } from 'react'
import { authApi } from '../api/authApi'
import { AuthContext } from '../hooks/useAuth'
import {
  AUTH_SESSION_EVENT,
  clearStoredSession,
  getStoredSession,
  storeSession,
} from '../storage/authStorage'
import type {
  AuthProviderProps,
  LoginPayload,
  RegisterPayload,
  User,
} from '../types/auth.types'

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(
    () => getStoredSession()?.user ?? null,
  )
  const [isInitializing, setIsInitializing] = useState(
    () => Boolean(getStoredSession()),
  )

  useEffect(() => {
    const syncSession = () => setUser(getStoredSession()?.user ?? null)
    window.addEventListener(AUTH_SESSION_EVENT, syncSession)
    window.addEventListener('storage', syncSession)
    return () => {
      window.removeEventListener(AUTH_SESSION_EVENT, syncSession)
      window.removeEventListener('storage', syncSession)
    }
  }, [])

  useEffect(() => {
    let active = true
    const session = getStoredSession()

    if (!session) {
      return () => {
        active = false
      }
    }

    authApi
      .currentUser()
      .then((currentUser) => {
        if (!active) return
        const latestSession = getStoredSession()
        if (latestSession) storeSession({ ...latestSession, user: currentUser })
      })
      .catch(() => {
        if (active) clearStoredSession()
      })
      .finally(() => {
        if (active) setIsInitializing(false)
      })

    return () => {
      active = false
    }
  }, [])

  const login = useCallback(async (payload: LoginPayload) => {
    const session = await authApi.login(payload)
    storeSession(session)
  }, [])

  const register = useCallback(async (payload: RegisterPayload) => {
    const session = await authApi.register(payload)
    storeSession(session)
  }, [])

  const logout = useCallback(async () => {
    const refreshToken = getStoredSession()?.refreshToken
    clearStoredSession()

    if (refreshToken) {
      try {
        await authApi.logout(refreshToken)
      } catch {
        // Local logout remains valid when the API is temporarily unavailable.
      }
    }
  }, [])

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      isInitializing,
      login,
      register,
      logout,
    }),
    [isInitializing, login, logout, register, user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
