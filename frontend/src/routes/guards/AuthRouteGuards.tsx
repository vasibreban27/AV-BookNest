import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { Logo } from '../../components/common/Logo'
import { useAuth } from '../../features/auth/hooks/useAuth'

function LoadingScreen() {
  return (
    <div className="loading-screen" role="status" aria-live="polite">
      <Logo />
      <span className="spinner" />
      <span className="sr-only">Se încarcă sesiunea...</span>
    </div>
  )
}

export function ProtectedRoute() {
  const { isAuthenticated, isInitializing } = useAuth()
  const location = useLocation()

  if (isInitializing) return <LoadingScreen />
  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return <Outlet />
}

export function PublicOnlyRoute() {
  const { isAuthenticated, isInitializing } = useAuth()

  if (isInitializing) return <LoadingScreen />
  if (isAuthenticated) return <Navigate to="/" replace />
  return <Outlet />
}
