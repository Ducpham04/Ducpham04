import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { ApiErrorBody, TokenResponse } from './types'

const REFRESH_KEY = 'bookeasy.refresh'
const USER_KEY = 'bookeasy.user'

let accessToken: string | null = null

export const tokens = {
  get access() {
    return accessToken
  },
  get refresh() {
    try {
      return localStorage.getItem(REFRESH_KEY)
    } catch {
      return null
    }
  },
  save(t: TokenResponse) {
    accessToken = t.accessToken
    try {
      localStorage.setItem(REFRESH_KEY, t.refreshToken)
      localStorage.setItem(USER_KEY, JSON.stringify(t.user))
    } catch {
      /* private mode */
    }
  },
  cachedUser() {
    try {
      const raw = localStorage.getItem(USER_KEY)
      return raw ? JSON.parse(raw) : null
    } catch {
      return null
    }
  },
  clear() {
    accessToken = null
    try {
      localStorage.removeItem(REFRESH_KEY)
      localStorage.removeItem(USER_KEY)
    } catch {
      /* ignore */
    }
  },
}

export const api = axios.create({ baseURL: import.meta.env.VITE_API_URL ?? '/api', timeout: 20000 })

api.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`
  return config
})

let refreshing: Promise<string | null> | null = null

export async function refreshAccessToken(): Promise<string | null> {
  const rt = tokens.refresh
  if (!rt) return null
  if (!refreshing) {
    refreshing = axios
      .post<TokenResponse>(`${api.defaults.baseURL}/auth/refresh`, { refreshToken: rt })
      .then((res) => {
        tokens.save(res.data)
        return res.data.accessToken
      })
      .catch(() => {
        tokens.clear()
        window.dispatchEvent(new Event('bookeasy:logout'))
        return null
      })
      .finally(() => {
        refreshing = null
      })
  }
  return refreshing
}

api.interceptors.response.use(undefined, async (error: AxiosError) => {
  const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined
  const url = original?.url ?? ''
  if (error.response?.status === 401 && original && !original._retry && !url.includes('/auth/')) {
    original._retry = true
    const token = await refreshAccessToken()
    if (token) {
      original.headers.Authorization = `Bearer ${token}`
      return api(original)
    }
  }
  return Promise.reject(error)
})

export function errorBody(e: unknown): ApiErrorBody | null {
  if (axios.isAxiosError(e) && e.response?.data && typeof e.response.data === 'object') {
    return e.response.data as ApiErrorBody
  }
  return null
}

export function errorMessage(e: unknown, fallback = 'Đã có lỗi xảy ra, vui lòng thử lại'): string {
  if (axios.isAxiosError(e) && !e.response) return 'Không kết nối được máy chủ. Kiểm tra mạng và thử lại.'
  return errorBody(e)?.message ?? fallback
}
