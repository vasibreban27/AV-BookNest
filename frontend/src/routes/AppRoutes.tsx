import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthLayout } from '../components/auth/AuthLayout'
import { AppLayout } from '../layout/AppLayout'
import { AccountPage } from '../pages/account/AccountPage'
import { LoginPage } from '../pages/auth/login/LoginPage'
import { RegisterPage } from '../pages/auth/register/RegisterPage'
import { ForgotPasswordPage } from '../pages/auth/password/ForgotPasswordPage'
import { ResetPasswordPage } from '../pages/auth/password/ResetPasswordPage'
import { VerifyEmailPage } from '../pages/auth/verify/VerifyEmailPage'
import { VerifyEmailSentPage } from '../pages/auth/verify/VerifyEmailSentPage'
import { CartPage } from '../pages/cart/CartPage'
import { BookDetailsPage } from '../pages/books/details/BookDetailsPage'
import { CheckoutPage } from '../pages/checkout/CheckoutPage'
import { HomePage } from '../pages/home/HomePage'
import { OrderDetailsPage } from '../pages/orders/details/OrderDetailsPage'
import { OrdersPage } from '../pages/orders/OrdersPage'
import { CreateListingPage } from '../pages/listings/CreateListingPage'
import { EditListingPage } from '../pages/listings/EditListingPage'
import { MyListingsPage } from '../pages/listings/MyListingsPage'
import { WishlistPage } from '../pages/wishlist/WishlistPage'
import { SalesPage } from '../pages/sales/SalesPage'
import { NotificationsPage } from '../pages/notifications/NotificationsPage'
import { ProtectedRoute, PublicOnlyRoute } from './guards/AuthRouteGuards'

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<AuthLayout />}>
        <Route element={<PublicOnlyRoute />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
        </Route>
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />
        <Route path="/verify-email-sent" element={<VerifyEmailSentPage />} />
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/home" element={<Navigate to="/" replace />} />
          <Route path="/account" element={<AccountPage />} />
          <Route path="/cart" element={<CartPage />} />
          <Route path="/books/:bookId" element={<BookDetailsPage />} />
          <Route path="/wishlist" element={<WishlistPage />} />
          <Route path="/checkout" element={<CheckoutPage />} />
          <Route path="/orders" element={<OrdersPage />} />
          <Route path="/orders/:orderId" element={<OrderDetailsPage />} />
          <Route path="/sales" element={<SalesPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/sell" element={<CreateListingPage />} />
          <Route path="/my-books" element={<MyListingsPage />} />
          <Route path="/my-books/:bookId/edit" element={<EditListingPage />} />
        </Route>
      </Route>

      <Route path="/autentificare" element={<Navigate to="/login" replace />} />
      <Route path="/inregistrare" element={<Navigate to="/register" replace />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
