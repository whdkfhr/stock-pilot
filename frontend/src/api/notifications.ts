import { api } from './client'
import type { Alert, AlertCreateBody, NotificationItem } from '@/types'

export const alertsApi = {
  create(body: AlertCreateBody) {
    return api.post<Alert>('/alerts', body)
  },
  list() {
    return api.get<Alert[]>('/alerts')
  },
  remove(id: number) {
    return api.delete(`/alerts/${id}`)
  },
}

export const notificationsApi = {
  list() {
    return api.get<NotificationItem[]>('/notifications')
  },
  markRead(id: number) {
    return api.patch<NotificationItem>(`/notifications/${id}/read`)
  },
}
