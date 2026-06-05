import axios from 'axios'
import { syncUnityAccessToken } from '../utils/unityAuthBridge'

const baseURL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'
const GUEST_PATHS = new Set(['/', '/login', '/signup', '/terms', '/privacy', '/service-intro'])

declare module 'axios' {
  interface AxiosRequestConfig {
    skipAuth?: boolean
  }

  interface InternalAxiosRequestConfig {
    skipAuth?: boolean
  }
}

const api = axios.create({
  baseURL,
  timeout: 10000,
  withCredentials: true,
})

api.interceptors.request.use((config) => {
  if (config.skipAuth) {
    return config
  }

  const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    const isGuestRequest = Boolean(originalRequest?.skipAuth)
    const isRefreshRequest = typeof originalRequest?.url === 'string' && originalRequest.url.includes('/auth/refresh')

    if (error.response?.status === 401 && !isGuestRequest && !isRefreshRequest && !originalRequest?._retry) {
      originalRequest._retry = true

      try {
        const refreshResponse = await axios.post<{ accessToken: string }>(
          `${baseURL}/auth/refresh`,
          {},
          { withCredentials: true }
        )

        const nextToken = refreshResponse.data.accessToken
        localStorage.setItem('accessToken', nextToken)
        syncUnityAccessToken(nextToken)
        originalRequest.headers.Authorization = `Bearer ${nextToken}`
        return api(originalRequest)
      } catch (refreshError) {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('authUserUuid')
        localStorage.removeItem('authUserId')
        syncUnityAccessToken(null)

        const currentPath = typeof window !== 'undefined' ? window.location.pathname : null
        if (currentPath && !GUEST_PATHS.has(currentPath)) {
          window.location.href = '/login'
        }

        return Promise.reject(refreshError)
      }
    }

    return Promise.reject(error)
  }
)

export default api
