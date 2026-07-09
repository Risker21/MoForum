import axios from 'axios'

const TOKEN_KEY = 'moforum_token'

export const http = axios.create({
  baseURL: '',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem('moforum_session')
      window.location.href = '/auth/login'
    }
    const msg =
      err.response?.data?.message ??
      err.response?.data?.error ??
      err.message ??
      '请求失败'
    return Promise.reject(new Error(typeof msg === 'string' ? msg : JSON.stringify(msg)))
  },
)
