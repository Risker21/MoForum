import { http } from './http'

export type AdminStats = {
  success: boolean
  userCount: number
  postCount: number
  replyCount: number
  boardCount: number
  postsPerBoard: Array<{ name: string; count: number }>
  dailyPosts: Array<{ date: string; count: number }>
  topUsers: Array<{ username: string; count: number }>
}

export function getAdminStats() {
  return http.get<AdminStats>('/admin/stats')
}
