import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getPendingCount } from '@/api/friend'
import { getUnreadCount } from '@/api/message'

export const useNotificationStore = defineStore('notification', () => {
  const friendRequestCount = ref(0)
  const unreadMessageCount = ref(0)

  async function refresh() {
    try {
      const [friendRes, msgRes] = await Promise.all([
        getPendingCount().catch(() => ({ data: { count: 0 } })),
        getUnreadCount().catch(() => ({ data: { count: 0 } })),
      ])
      friendRequestCount.value = friendRes.data?.count ?? 0
      unreadMessageCount.value = msgRes.data?.count ?? 0
    } catch {
      friendRequestCount.value = 0
      unreadMessageCount.value = 0
    }
  }

  return { friendRequestCount, unreadMessageCount, refresh }
})
