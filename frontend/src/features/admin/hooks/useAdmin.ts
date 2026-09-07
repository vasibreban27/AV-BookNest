import { useMutation, useQuery, useQueryClient, type QueryKey } from '@tanstack/react-query'
import { adminApi } from '../api/adminApi'
import type {
  AdminBookFilters,
  AdminIssueFilters,
  AdminOrderFilters,
  AdminPageRequest,
  AdminUserFilters,
  BookModerationPayload,
  CategoryPayload,
  CategoryUpdatePayload,
  IssueResolutionPayload,
} from '../types/admin.types'

export const adminQueryKeys = {
  root: ['admin'] as const,
  dashboard: ['admin', 'dashboard'] as const,
  users: (filters: AdminUserFilters) => ['admin', 'users', filters] as const,
  user: (userId: number | null) => ['admin', 'user', userId] as const,
  books: (filters: AdminBookFilters) => ['admin', 'books', filters] as const,
  book: (bookId: number | null) => ['admin', 'book', bookId] as const,
  categories: ['admin', 'categories'] as const,
  orders: (filters: AdminOrderFilters) => ['admin', 'orders', filters] as const,
  order: (orderId: number | null) => ['admin', 'order', orderId] as const,
  issues: (filters: AdminIssueFilters) => ['admin', 'issues', filters] as const,
  integrations: (filters: AdminPageRequest) => ['admin', 'operations', 'integrations', filters] as const,
  transfers: (filters: AdminPageRequest) => ['admin', 'operations', 'transfers', filters] as const,
  shipments: (filters: AdminPageRequest) => ['admin', 'operations', 'shipments', filters] as const,
  audit: (filters: AdminPageRequest) => ['admin', 'audit', filters] as const,
}

function useRefreshAdmin() {
  const queryClient = useQueryClient()
  return (...keys: QueryKey[]) => {
    void queryClient.invalidateQueries({ queryKey: adminQueryKeys.dashboard })
    void queryClient.invalidateQueries({ queryKey: ['admin', 'audit'] })
    void queryClient.invalidateQueries({ queryKey: ['moderation-history'] })
    keys.forEach((key) => void queryClient.invalidateQueries({ queryKey: key }))
  }
}

export function useAdminDashboard() {
  return useQuery({ queryKey: adminQueryKeys.dashboard, queryFn: adminApi.dashboard })
}

export function useAdminUsers(filters: AdminUserFilters) {
  return useQuery({ queryKey: adminQueryKeys.users(filters), queryFn: () => adminApi.users(filters) })
}

export function useAdminUser(userId: number | null) {
  return useQuery({
    queryKey: adminQueryKeys.user(userId),
    queryFn: () => adminApi.user(userId!),
    enabled: userId !== null,
  })
}

export function useSuspendAdminUser() {
  const refresh = useRefreshAdmin()
  return useMutation({
    mutationFn: ({ userId, reason }: { userId: number; reason: string }) => adminApi.suspendUser(userId, reason),
    onSuccess: (_, variables) => refresh(['admin', 'users'], adminQueryKeys.user(variables.userId), ['admin', 'books']),
  })
}

export function useReactivateAdminUser() {
  const refresh = useRefreshAdmin()
  return useMutation({
    mutationFn: ({ userId, reason }: { userId: number; reason: string }) => adminApi.reactivateUser(userId, reason),
    onSuccess: (_, variables) => refresh(['admin', 'users'], adminQueryKeys.user(variables.userId), ['admin', 'books']),
  })
}

export function useRevokeAdminUserSessions() {
  const refresh = useRefreshAdmin()
  return useMutation({
    mutationFn: ({ userId, reason }: { userId: number; reason: string }) => adminApi.revokeSessions(userId, reason),
    onSuccess: (_, variables) => refresh(adminQueryKeys.user(variables.userId)),
  })
}

export function useResendAdminVerification() {
  const refresh = useRefreshAdmin()
  return useMutation({
    mutationFn: adminApi.resendVerification,
    onSuccess: (_, userId) => refresh(adminQueryKeys.user(userId)),
  })
}

export function useAdminBooks(filters: AdminBookFilters) {
  return useQuery({ queryKey: adminQueryKeys.books(filters), queryFn: () => adminApi.books(filters) })
}

export function useAdminBook(bookId: number | null) {
  return useQuery({
    queryKey: adminQueryKeys.book(bookId),
    queryFn: () => adminApi.book(bookId!),
    enabled: bookId !== null,
  })
}

export function useHideAdminBook() {
  const refresh = useRefreshAdmin()
  return useMutation({
    mutationFn: ({ bookId, payload }: { bookId: number; payload: BookModerationPayload }) => adminApi.hideBook(bookId, payload),
    onSuccess: (_, variables) => refresh(['admin', 'books'], adminQueryKeys.book(variables.bookId)),
  })
}

export function useRestoreAdminBook() {
  const refresh = useRefreshAdmin()
  return useMutation({
    mutationFn: ({ bookId, reason }: { bookId: number; reason: string }) => adminApi.restoreBook(bookId, reason),
    onSuccess: (_, variables) => refresh(['admin', 'books'], adminQueryKeys.book(variables.bookId)),
  })
}

export function useAdminCategories() {
  return useQuery({ queryKey: adminQueryKeys.categories, queryFn: adminApi.categories })
}

export function useCreateAdminCategory() {
  const refresh = useRefreshAdmin()
  return useMutation({ mutationFn: (payload: CategoryPayload) => adminApi.createCategory(payload), onSuccess: () => refresh(adminQueryKeys.categories) })
}

export function useUpdateAdminCategory() {
  const refresh = useRefreshAdmin()
  return useMutation({
    mutationFn: ({ categoryId, payload }: { categoryId: number; payload: CategoryUpdatePayload }) => adminApi.updateCategory(categoryId, payload),
    onSuccess: () => refresh(adminQueryKeys.categories),
  })
}

export function useSetAdminCategoryActive() {
  const refresh = useRefreshAdmin()
  return useMutation({
    mutationFn: ({ categoryId, active, reason }: { categoryId: number; active: boolean; reason: string }) => adminApi.setCategoryActive(categoryId, active, reason),
    onSuccess: () => refresh(adminQueryKeys.categories, ['admin', 'books']),
  })
}

export function useAdminOrders(filters: AdminOrderFilters) {
  return useQuery({ queryKey: adminQueryKeys.orders(filters), queryFn: () => adminApi.orders(filters) })
}

export function useAdminOrder(orderId: number | null) {
  return useQuery({
    queryKey: adminQueryKeys.order(orderId),
    queryFn: () => adminApi.order(orderId!),
    enabled: orderId !== null,
  })
}

export function useAdminIssues(filters: AdminIssueFilters) {
  return useQuery({ queryKey: adminQueryKeys.issues(filters), queryFn: () => adminApi.issues(filters) })
}

export function useResolveAdminIssue() {
  const refresh = useRefreshAdmin()
  return useMutation({
    mutationFn: ({ sellerOrderId, payload }: { sellerOrderId: number; payload: IssueResolutionPayload }) => adminApi.resolveIssue(sellerOrderId, payload),
    onSuccess: () => refresh(['admin', 'issues'], ['admin', 'orders'], ['admin', 'operations']),
  })
}

export function useFailedAdminIntegrations(filters: AdminPageRequest) {
  return useQuery({ queryKey: adminQueryKeys.integrations(filters), queryFn: () => adminApi.failedIntegrations(filters) })
}

export function useRetryAdminIntegration() {
  const refresh = useRefreshAdmin()
  return useMutation({
    mutationFn: ({ eventId, reason }: { eventId: number; reason: string }) => adminApi.retryIntegration(eventId, reason),
    onSuccess: () => refresh(['admin', 'operations']),
  })
}

export function useFailedAdminTransfers(filters: AdminPageRequest) {
  return useQuery({ queryKey: adminQueryKeys.transfers(filters), queryFn: () => adminApi.failedTransfers(filters) })
}

export function useAdminShipmentExceptions(filters: AdminPageRequest) {
  return useQuery({ queryKey: adminQueryKeys.shipments(filters), queryFn: () => adminApi.shipmentExceptions(filters) })
}

export function useAdminAuditLogs(filters: AdminPageRequest) {
  return useQuery({ queryKey: adminQueryKeys.audit(filters), queryFn: () => adminApi.auditLogs(filters) })
}
