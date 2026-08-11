import { api } from '../../../api/client'
import type { Notification } from '../types/notifications.types'

export const notificationsApi = {
  async list() {
    const { data } = await api.get<Notification[]>('/notifications')
    return data
  },

  async markRead(notificationId: number) {
    const { data } = await api.patch<Notification>(`/notifications/${notificationId}/read`)
    return data
  },

  async markAllRead() {
    await api.patch('/notifications/read-all')
  },
}
