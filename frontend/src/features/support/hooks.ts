import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../auth/hooks/useAuth'
import { supportApi } from './api'
import type { SupportAction, SupportFilters } from './types'

export function useSupportList(admin: boolean, filters: SupportFilters) {
  const { user } = useAuth()
  return useQuery({ queryKey: ['support', user?.id, admin, 'list', filters], queryFn: () => supportApi.list(admin, filters),
    enabled: Boolean(user) && (!admin || user?.role === 'ADMIN'), refetchInterval: 30_000 })
}
export function useSupportTicket(id: number, admin: boolean, page: number) {
  const { user } = useAuth()
  const enabled = Boolean(user) && Number.isSafeInteger(id) && id > 0 && (!admin || user?.role === 'ADMIN')
  const ticket = useQuery({ queryKey: ['support', user?.id, admin, id], queryFn: () => supportApi.ticket(id, admin), enabled, refetchInterval: 30_000 })
  const messages = useQuery({ queryKey: ['support', user?.id, admin, id, 'messages', page], queryFn: () => supportApi.messages(id, admin, page), enabled: enabled && ticket.isSuccess, refetchInterval: 30_000 })
  return { ticket, messages }
}
export function useSupportAdministrators(enabled: boolean) {
  const { user } = useAuth()
  return useQuery({ queryKey: ['support', user?.id, 'administrators'], queryFn: supportApi.administrators, enabled: enabled && user?.role === 'ADMIN' })
}
export function useSupportAction(id: number, admin: boolean) {
  const client = useQueryClient()
  return useMutation({ mutationFn: (action: SupportAction) => supportApi.action(id, admin, action), onSuccess: async () => {
    await Promise.all([client.invalidateQueries({ queryKey: ['support'] }), client.invalidateQueries({ queryKey: ['notifications'] }), client.invalidateQueries({ queryKey: ['admin', 'audit'] })])
  }, onError: () => { void client.invalidateQueries({ queryKey: ['support'] }) } })
}
