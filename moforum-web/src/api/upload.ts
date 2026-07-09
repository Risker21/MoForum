import { http } from './http'

export type UploadRes = {
  success: boolean
  message: string
  url?: string
}

export function uploadImage(file: File) {
  const form = new FormData()
  form.append('file', file)
  return http.post<UploadRes>('/upload/image', form)
}

export function confirmImage(url: string) {
  return http.post<{ success: boolean }>('/upload/confirm', { url })
}
