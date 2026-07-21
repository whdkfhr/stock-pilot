import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { notificationsApi } from '@/api/notifications'
import type { NotificationItem } from '@/types'

export const useNotificationStore = defineStore('notifications', () => {
  const items = ref<NotificationItem[]>([])
  const loaded = ref(false)

  const unreadCount = computed(() => items.value.filter((n) => !n.read).length)

  async function fetch() {
    const { data } = await notificationsApi.list()
    items.value = data
    loaded.value = true
  }

  async function markRead(id: number) {
    await notificationsApi.markRead(id)
    const n = items.value.find((x) => x.id === id)
    if (n) n.read = true
  }

  return { items, loaded, unreadCount, fetch, markRead }
})
