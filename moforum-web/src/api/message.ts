import { http } from './http'

export type MessageRow = {
  id: number
  fromId: number
  toId: number
  content: string
  read: number
  createTime?: string
}

export type Conversation = {
  userId: number
  username: string
  avatarUrl?: string
  lastContent: string
  lastTime: string
  unread: number
}

export type MessagePage = {
  list: MessageRow[]
  total: number
  page: number
  pageSize: number
}

export function sendMessage(toUserId: number, content: string) {
  return http.post<{ success: boolean; message: string; id?: number }>('/message/send', { toUserId, content })
}

export function getConversations() {
  return http.get<{ success: boolean; list: Conversation[] }>('/message/conversations')
}

export function getMessages(userId: number, page: number, pageSize: number) {
  return http.get<MessagePage>('/message/list', { params: { userId, page, pageSize } })
}

export function markRead(userId: number) {
  return http.post<{ success: boolean }>('/message/read', { userId })
}

export function getUnreadCount() {
  return http.get<{ count: number }>('/message/unread-count')
}
