import { http } from './http'

export type RegisterRes = {
  success: boolean
  message: string
  token?: string
  userId?: number
  username?: string
  userNo?: number
}

export type LoginRes = {
  success: boolean
  message: string
  token?: string
  userId?: number
  username?: string
  userNo?: number
}

export type UserRow = {
  id: number
  username: string
  userNo?: number
  bio?: string
  avatarUrl?: string
  createTime?: string
  updateTime?: string
}

export function register(username: string, password: string) {
  return http.post<RegisterRes>('/user/register', { username, password })
}

export function login(username: string, password: string) {
  return http.post<LoginRes>('/user/login', { username, password })
}

export function logout() {
  return http.post<{ success: boolean; message: string }>('/user/logout')
}

export function getUserById(id: number) {
  return http.get<UserRow | null>('/user/getById', { params: { id } })
}

export function getUserByUserNo(userNo: number) {
  return http.get<UserRow>('/user/getByUserNo', { params: { userNo } })
}

export function updateProfile(data: { bio?: string; avatarUrl?: string }) {
  return http.put<{ success: boolean; message: string }>('/user/profile', data)
}
