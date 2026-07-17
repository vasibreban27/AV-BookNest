import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthLayout } from '../components/auth/AuthLayout'
import { AppLayout } from '../layout/AppLayout'
import { AccountPage } from '../pages/account/AccountPage'
import { LoginPage } from '../pages/auth/login/LoginPage'
import { RegisterPage } from '../pages/auth/register/RegisterPage'
import { CartPage } from '../pages/cart/CartPage'
import { HomePage } from '../pages/home/HomePage'
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
        <Route element={<AppLayout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/home" element={<Navigate to="/" replace />} />
          <Route path="/account" element={<AccountPage />} />
          <Route path="/cart" element={<CartPage />} />
        </Route>
      </Route>

      <Route path="/autentificare" element={<Navigate to="/login" replace />} />
      <Route path="/inregistrare" element={<Navigate to="/register" replace />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
