import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../../auth/hooks/useAuth'
import { notificationsApi } from '../api/notificationsApi'
import type { Notification } from '../types/notifications.types'

export const notificationQueryKey = (userId: number | undefined) =>
  ['notifications', userId] as const

export function useNotifications() {
  const { user } = useAuth()
  return useQuery({
    queryKey: notificationQueryKey(user?.id),
    queryFn: notificationsApi.list,
    enabled: Boolean(user),
    refetchInterval: 15_000,
  })
}

export function useMarkNotificationRead() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: notificationsApi.markRead,
    onSuccess: (notification) => {
      queryClient.setQueryData<Notification[]>(notificationQueryKey(user?.id), (items = []) =>
        items.map((item) => item.id === notification.id ? notification : item),
      )
    },
  })
}

export function useMarkAllNotificationsRead() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: notificationsApi.markAllRead,
    onSuccess: () => {
      const readAt = new Date().toISOString()
      queryClient.setQueryData<Notification[]>(notificationQueryKey(user?.id), (items = []) =>
        items.map((item) => ({ ...item, readAt: item.readAt ?? readAt })),
      )
    },
  })
}
