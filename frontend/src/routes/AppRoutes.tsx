import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthLayout } from '../components/auth/AuthLayout'
import { AccountPage } from '../pages/account/AccountPage'
import { LoginPage } from '../pages/auth/login/LoginPage'
import { RegisterPage } from '../pages/auth/register/RegisterPage'
import { ProtectedRoute, PublicOnlyRoute } from './guards/AuthRouteGuards'

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<PublicOnlyRoute />}>
        <Route element={<AuthLayout />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
        </Route>
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route path="/account" element={<AccountPage />} />
      </Route>

      <Route path="/autentificare" element={<Navigate to="/login" replace />} />
      <Route path="/inregistrare" element={<Navigate to="/register" replace />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  )
}
