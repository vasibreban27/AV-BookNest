import { lazy, Suspense } from 'react'
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
import { CatalogPage } from '../pages/catalog/CatalogPage'
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
import { ContactPage } from '../pages/contact/ContactPage'
import {
  ConsumerRightsPage,
  CookiesPage,
  PrivacyPage,
  TermsPage,
} from '../pages/legal/LegalPages'
import { AdminRoute, ProtectedRoute, PublicOnlyRoute } from './guards/AuthRouteGuards'

const AdminLayout = lazy(() => import('../components/admin/AdminLayout').then((module) => ({ default: module.AdminLayout })))
const AdminDashboardPage = lazy(() => import('../pages/admin/AdminDashboardPage').then((module) => ({ default: module.AdminDashboardPage })))
const AdminUsersPage = lazy(() => import('../pages/admin/AdminUsersPage').then((module) => ({ default: module.AdminUsersPage })))
const AdminBooksPage = lazy(() => import('../pages/admin/AdminBooksPage').then((module) => ({ default: module.AdminBooksPage })))
const AdminReviewsPage = lazy(() => import('../pages/admin/AdminReviewsPage').then((module) => ({ default: module.AdminReviewsPage })))
const AdminCategoriesPage = lazy(() => import('../pages/admin/AdminCategoriesPage').then((module) => ({ default: module.AdminCategoriesPage })))
const AdminOrdersPage = lazy(() => import('../pages/admin/AdminOrdersPage').then((module) => ({ default: module.AdminOrdersPage })))
const AdminIssuesPage = lazy(() => import('../pages/admin/AdminIssuesPage').then((module) => ({ default: module.AdminIssuesPage })))
const AdminOperationsPage = lazy(() => import('../pages/admin/AdminOperationsPage').then((module) => ({ default: module.AdminOperationsPage })))
const AdminAuditPage = lazy(() => import('../pages/admin/AdminAuditPage').then((module) => ({ default: module.AdminAuditPage })))
const SupportPage = lazy(() => import('../pages/support/SupportPage').then((module) => ({ default: module.SupportPage })))
const SupportTicketPage = lazy(() => import('../pages/support/SupportTicketPage').then((module) => ({ default: module.SupportTicketPage })))

function AdminRouteLoading() {
  return <main className="admin-page"><div className="admin-state" role="status"><span className="spinner" /><div><strong>Se încarcă secțiunea</strong><p>Pregătim datele administrative.</p></div></div></main>
}

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

      <Route element={<AdminRoute />}>
        <Route element={<Suspense fallback={<div className="loading-screen" role="status"><span className="spinner" /><span className="sr-only">Se încarcă administrarea...</span></div>}><AdminLayout /></Suspense>}>
          <Route path="/admin" element={<Suspense fallback={<AdminRouteLoading />}><AdminDashboardPage /></Suspense>} />
          <Route path="/admin/users" element={<Suspense fallback={<AdminRouteLoading />}><AdminUsersPage /></Suspense>} />
          <Route path="/admin/books" element={<Suspense fallback={<AdminRouteLoading />}><AdminBooksPage /></Suspense>} />
          <Route path="/admin/reviews" element={<Suspense fallback={<AdminRouteLoading />}><AdminReviewsPage /></Suspense>} />
          <Route path="/admin/support" element={<Suspense fallback={<AdminRouteLoading />}><SupportPage admin /></Suspense>} />
          <Route path="/admin/support/:ticketId" element={<Suspense fallback={<AdminRouteLoading />}><SupportTicketPage admin /></Suspense>} />
          <Route path="/admin/categories" element={<Suspense fallback={<AdminRouteLoading />}><AdminCategoriesPage /></Suspense>} />
          <Route path="/admin/orders" element={<Suspense fallback={<AdminRouteLoading />}><AdminOrdersPage /></Suspense>} />
          <Route path="/admin/issues" element={<Suspense fallback={<AdminRouteLoading />}><AdminIssuesPage /></Suspense>} />
          <Route path="/admin/operations" element={<Suspense fallback={<AdminRouteLoading />}><AdminOperationsPage /></Suspense>} />
          <Route path="/admin/audit" element={<Suspense fallback={<AdminRouteLoading />}><AdminAuditPage /></Suspense>} />
        </Route>
      </Route>

      <Route element={<AppLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/home" element={<Navigate to="/" replace />} />
        <Route path="/catalog" element={<CatalogPage />} />
        <Route path="/books/:bookId" element={<BookDetailsPage />} />
        <Route path="/contact" element={<ContactPage />} />
        <Route path="/terms" element={<TermsPage />} />
        <Route path="/privacy" element={<PrivacyPage />} />
        <Route path="/cookies" element={<CookiesPage />} />
        <Route path="/consumer-rights" element={<ConsumerRightsPage />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/account" element={<AccountPage />} />
          <Route path="/support" element={<Suspense fallback={<p role="status">Se încarcă suportul…</p>}><SupportPage /></Suspense>} />
          <Route path="/support/:ticketId" element={<Suspense fallback={<p role="status">Se încarcă suportul…</p>}><SupportTicketPage /></Suspense>} />
          <Route path="/cart" element={<CartPage />} />
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
