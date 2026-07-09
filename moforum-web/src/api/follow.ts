import { http } from './http'

export function toggleFollow(userId: number) {
  return http.post<{ success: boolean; message: string; followed?: boolean }>('/follow/toggle', { userId })
}

export function getFollowers(userId: number) {
  return http.get<{ success: boolean; list: Array<{ userId: number; followTime: string; isFollowed?: boolean }> }>('/follow/followers', { params: { userId } })
}

export function getFollowing(userId: number) {
  return http.get<{ success: boolean; list: Array<{ userId: number; followTime: string; isFollowed?: boolean }> }>('/follow/following', { params: { userId } })
}

export function getFollowStatus(userId: number) {
  return http.get<{ followed: boolean }>('/follow/status', { params: { userId } })
}

export function getFollowCounts(userId: number) {
  return http.get<{ followers: number; following: number }>('/follow/counts', { params: { userId } })
}
