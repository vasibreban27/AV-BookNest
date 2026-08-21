import { api } from '../../../api/client'
import type {
  AdminAuditLog,
  AdminBook,
  AdminBookFilters,
  AdminCategory,
  AdminDashboard,
  AdminIntegrationEvent,
  AdminIssue,
  AdminIssueFilters,
  AdminOrderDetails,
  AdminOrderFilters,
  AdminOrderSummary,
  AdminPageRequest,
  AdminShipmentException,
  AdminTransfer,
  AdminUser,
  AdminUserDetails,
  AdminUserFilters,
  BookModerationPayload,
  CategoryPayload,
  CategoryUpdatePayload,
  IssueResolutionPayload,
  PageResponse,
} from '../types/admin.types'

const params = (values: Record<string, unknown>) =>
  Object.fromEntries(Object.entries(values).filter(([, value]) => value !== undefined && value !== ''))

export const adminApi = {
  async dashboard() {
    const { data } = await api.get<AdminDashboard>('/admin/dashboard')
    return data
  },
  async users(filters: AdminUserFilters) {
    const { data } = await api.get<PageResponse<AdminUser>>('/admin/users', { params: params(filters) })
    return data
  },
  async user(userId: number) {
    const { data } = await api.get<AdminUserDetails>(`/admin/users/${userId}`)
    return data
  },
  async suspendUser(userId: number, reason: string) {
    const { data } = await api.post<AdminUserDetails>(`/admin/users/${userId}/suspend`, { reason })
    return data
  },
  async reactivateUser(userId: number, reason: string) {
    const { data } = await api.post<AdminUserDetails>(`/admin/users/${userId}/reactivate`, { reason })
    return data
  },
  async revokeSessions(userId: number, reason: string) {
    await api.post(`/admin/users/${userId}/revoke-sessions`, { reason })
  },
  async resendVerification(userId: number) {
    await api.post(`/admin/users/${userId}/resend-verification`)
  },
  async books(filters: AdminBookFilters) {
    const { data } = await api.get<PageResponse<AdminBook>>('/admin/books', { params: params(filters) })
    return data
  },
  async book(bookId: number) {
    const { data } = await api.get<AdminBook>(`/admin/books/${bookId}`)
    return data
  },
  async hideBook(bookId: number, payload: BookModerationPayload) {
    const { data } = await api.post<AdminBook>(`/admin/books/${bookId}/hide`, payload)
    return data
  },
  async restoreBook(bookId: number, reason: string) {
    const { data } = await api.post<AdminBook>(`/admin/books/${bookId}/restore`, { reason })
    return data
  },
  async categories() {
    const { data } = await api.get<AdminCategory[]>('/admin/categories')
    return data
  },
  async createCategory(payload: CategoryPayload) {
    const { data } = await api.post<AdminCategory>('/admin/categories', payload)
    return data
  },
  async updateCategory(categoryId: number, payload: CategoryUpdatePayload) {
    const { data } = await api.put<AdminCategory>(`/admin/categories/${categoryId}`, payload)
    return data
  },
  async setCategoryActive(categoryId: number, active: boolean, reason: string) {
    const action = active ? 'activate' : 'deactivate'
    const { data } = await api.patch<AdminCategory>(`/admin/categories/${categoryId}/${action}`, { reason })
    return data
  },
  async orders(filters: AdminOrderFilters) {
    const { data } = await api.get<PageResponse<AdminOrderSummary>>('/admin/orders', { params: params(filters) })
    return data
  },
  async order(orderId: number) {
    const { data } = await api.get<AdminOrderDetails>(`/admin/orders/${orderId}`)
    return data
  },
  async issues(filters: AdminIssueFilters) {
    const { data } = await api.get<PageResponse<AdminIssue>>('/admin/issues', { params: params(filters) })
    return data
  },
  async resolveIssue(sellerOrderId: number, payload: IssueResolutionPayload) {
    const { data } = await api.post<AdminIssue>(`/admin/issues/${sellerOrderId}/resolve`, payload)
    return data
  },
  async failedIntegrations(filters: AdminPageRequest) {
    const { data } = await api.get<PageResponse<AdminIntegrationEvent>>('/admin/operations/integrations/failed', { params: params(filters) })
    return data
  },
  async retryIntegration(eventId: number, reason: string) {
    const { data } = await api.post<AdminIntegrationEvent>(`/admin/operations/integrations/${eventId}/retry`, { reason })
    return data
  },
  async failedTransfers(filters: AdminPageRequest) {
    const { data } = await api.get<PageResponse<AdminTransfer>>('/admin/operations/transfers/failed', { params: params(filters) })
    return data
  },
  async shipmentExceptions(filters: AdminPageRequest) {
    const { data } = await api.get<PageResponse<AdminShipmentException>>('/admin/operations/shipments/exceptions', { params: params(filters) })
    return data
  },
  async auditLogs(filters: AdminPageRequest) {
    const { data } = await api.get<PageResponse<AdminAuditLog>>('/admin/audit-logs', { params: params(filters) })
    return data
  },
}
