import { http } from './http'
import type { PostRow } from './post'
import type { BoardRow } from './board'
import type { UserRow } from './user'

export type SearchResult = {
  posts?: PostRow[]
  boards?: BoardRow[]
  users?: UserRow[]
}

export function search(q: string, type = 'all') {
  return http.get<SearchResult>('/search', { params: { q, type } })
}
