import { http } from './http'

export type FriendRequestStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'

export type FriendUser = {
  userId: number
  username: string
  userNo?: number
  avatarUrl?: string
}

export type FriendRequest = {
  id: number
  fromId: number
  status: string
  createTime?: string
  username?: string
}

export function sendFriendRequest(userId: number) {
  return http.post<{ success: boolean; message: string }>('/friend/request', { userId })
}

export function respondFriendRequest(requestId: number, accept: boolean) {
  return http.post<{ success: boolean; message: string }>('/friend/respond', { requestId, accept })
}

export function getFriendList() {
  return http.get<{ success: boolean; list: FriendUser[] }>('/friend/list')
}

export function getReceivedRequests() {
  return http.get<{ success: boolean; list: FriendRequest[] }>('/friend/requests/received')
}

export function getSentRequests() {
  return http.get<{ success: boolean; list: FriendRequest[] }>('/friend/requests/sent')
}

export function getFriendStatus(userId: number) {
  return http.get<{ status: 'NONE' | 'PENDING' | 'FRIENDS' }>('/friend/status', { params: { userId } })
}

export function getPendingCount() {
  return http.get<{ count: number }>('/friend/pending-count')
}
