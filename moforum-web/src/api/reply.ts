import { http } from './http'

export type ReplyRow = {
  id: number
  postId: number
  userId: number
  content: string
  createTime?: string
  authorName?: string
  avatarUrl?: string
}

export type CreateReplyRes = { success: boolean; message: string }

export function listReplies(postId: number) {
  return http.get<ReplyRow[]>('/reply/list', { params: { postId } })
}

export function createReply(payload: {
  postId: number
  content: string
}) {
  return http.post<CreateReplyRes>('/reply/create', payload)
}
