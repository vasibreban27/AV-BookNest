import { useCallback, useEffect, useMemo, useState } from 'react'
import { authApi } from '../api/authApi'
import { AUTH_SESSION_EXPIRED_EVENT } from '../events/authEvents'
import { AuthContext } from '../hooks/useAuth'
import type {
  AuthProviderProps,
  LoginPayload,
  RegisterPayload,
  User,
} from '../types/auth.types'

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(null)
  const [isInitializing, setIsInitializing] = useState(true)

  useEffect(() => {
    const expireSession = () => setUser(null)
    window.addEventListener(AUTH_SESSION_EXPIRED_EVENT, expireSession)
    return () => {
      window.removeEventListener(AUTH_SESSION_EXPIRED_EVENT, expireSession)
    }
  }, [])

  useEffect(() => {
    let active = true
    try {
      localStorage.removeItem('booknest.auth.session')
    } catch {
      // Cleanup for sessions created before authentication moved to HttpOnly cookies.
    }
    authApi.initializeCsrf()
      .then(() => authApi.currentUser())
      .then((currentUser) => {
        if (active) setUser(currentUser)
      })
      .catch(() => {
        if (active) setUser(null)
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
    setUser(session.user)
    try {
      await authApi.initializeCsrf()
    } catch {
      // The login response already issued a usable CSRF cookie.
    }
  }, [])

  const register = useCallback(async (payload: RegisterPayload) => {
    return authApi.register(payload)
  }, [])

  const logout = useCallback(async () => {
    try {
      await authApi.logout()
    } finally {
      setUser(null)
      try {
        await authApi.initializeCsrf()
      } catch {
        // The local UI still closes the session if the API is unavailable.
      }
    }
  }, [])

  const updateUser = useCallback((nextUser: User) => {
    setUser(nextUser)
  }, [])

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      isInitializing,
      login,
      register,
      updateUser,
      logout,
    }),
    [isInitializing, login, logout, register, updateUser, user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
