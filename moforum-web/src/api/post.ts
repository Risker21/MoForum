import { http } from './http'

export type PostRow = {
  id: number
  userId: number
  boardId?: number
  title: string
  content: string
  viewCount?: number
  replyCount?: number
  createTime?: string
  updateTime?: string
  authorName?: string
  avatarUrl?: string
  boardName?: string
}

export type PostPage = {
  list: PostRow[]
  total: number
  page: number
  pageSize: number
}

export type CreatePostRes = { success: boolean; message: string }

export type DeletePostRes = { success: boolean; message: string }

export function createPost(payload: {
  boardId: number
  title: string
  content: string
}) {
  return http.post<CreatePostRes>('/post/create', payload)
}

export function listPostsByBoard(boardId: number, page: number, pageSize: number) {
  return http.get<PostPage>('/post/list', { params: { boardId, page, pageSize } })
}

export function listPostsByUser(userId: number, page: number, pageSize: number) {
  return http.get<PostPage>('/post/list', { params: { userId, page, pageSize } })
}

export function latestPosts(limit = 8) {
  return http.get<PostRow[]>('/post/latest', { params: { limit } })
}

export function getPostDetail(id: number) {
  return http.get<PostRow | null>('/post/detail', { params: { id } })
}

export function deletePost(postId: number) {
  return http.post<DeletePostRes>('/post/delete', { postId })
}
