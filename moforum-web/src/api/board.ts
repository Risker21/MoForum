import { http } from './http'

export type BoardRow = {
  id: number
  name: string
  description?: string
  avatar?: string
  sortOrder?: number
  postCount?: number
  createTime?: string
  updateTime?: string
}

export function listBoards() {
  return http.get<BoardRow[]>('/board/list')
}

export function getBoard(id: number) {
  return http.get<BoardRow>('/board/detail', { params: { id } })
}
