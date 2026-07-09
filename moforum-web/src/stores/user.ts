import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

type Stored = { userId: number; username: string; userNo?: number; avatarUrl?: string }

function load(): Stored | null {
  try {
    const raw = localStorage.getItem('moforum_session')
    if (!raw) return null
    const v = JSON.parse(raw)
    if (typeof v.userId === 'number' && typeof v.username === 'string') return v
    return null
  } catch {
    return null
  }
}

function save(v: Stored) {
  localStorage.setItem('moforum_session', JSON.stringify(v))
}

function clear() {
  localStorage.removeItem('moforum_session')
}

export const useUserStore = defineStore('user', () => {
  const stored = load()

  const userId = ref<number | null>(stored?.userId ?? null)
  const username = ref(stored?.username ?? '')
  const userNo = ref<number | null>(stored?.userNo ?? null)
  const avatarUrl = ref(stored?.avatarUrl ?? '')

  const isLoggedIn = computed(() => userId.value != null)

  function setSession(id: number, name: string, moNo?: number, avatar?: string) {
    userId.value = id
    username.value = name
    userNo.value = moNo ?? null
    avatarUrl.value = avatar ?? ''
    const payload: Stored = { userId: id, username: name }
    if (moNo != null) payload.userNo = moNo
    if (avatar) payload.avatarUrl = avatar
    save(payload)
  }

  function updateAvatar(url: string) {
    avatarUrl.value = url
    const stored = load()
    if (stored) {
      stored.avatarUrl = url
      save(stored)
    }
  }

  function logout() {
    userId.value = null
    username.value = ''
    userNo.value = null
    avatarUrl.value = ''
    clear()
  }

  return { userId, username, userNo, avatarUrl, isLoggedIn, setSession, updateAvatar, logout }
})
