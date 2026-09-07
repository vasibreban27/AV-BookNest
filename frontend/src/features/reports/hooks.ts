import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../auth/hooks/useAuth'
import { reportsApi, type ReportResolution, type ReportStatus } from './api'

export function useReports(admin: boolean, page: number, status: ReportStatus | '', targetUser?: number) {
  const { user } = useAuth()
  return useQuery({ queryKey: ['reports', user?.id, admin, page, status, targetUser], queryFn: () => reportsApi.list(admin, page, status, targetUser), enabled: !!user && (!admin || user.role === 'ADMIN'), refetchInterval: 30_000 })
}
export function useReport(admin: boolean, id: number) {
  const { user } = useAuth()
  return useQuery({ queryKey: ['reports', user?.id, admin, 'detail', id], queryFn: () => reportsApi.details(admin, id), enabled: !!user && Number.isSafeInteger(id) && id > 0 && (!admin || user.role === 'ADMIN'), refetchInterval: 30_000 })
}
export function useReportAction(id: number) {
  const client = useQueryClient()
  return useMutation({ mutationFn: (body: ReportResolution | 'claim') => body === 'claim' ? reportsApi.claim(id) : reportsApi.resolve(id, body), onSuccess: async () => {
    await Promise.all(['reports', 'moderation-history', 'admin', 'notifications', 'catalog', 'book'].map(key => client.invalidateQueries({ queryKey: [key] })))
  }, onError: () => { void client.invalidateQueries({ queryKey: ['reports'] }) } })
}
export function useModerationHistory(userId: number) {
  const { user } = useAuth()
  return useQuery({ queryKey: ['moderation-history', user?.id, userId], queryFn: () => reportsApi.history(userId), enabled: user?.role === 'ADMIN' && userId > 0 })
}
