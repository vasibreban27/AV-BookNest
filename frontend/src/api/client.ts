import axios, {
  AxiosError,
  type AxiosRequestConfig,
} from 'axios'
import {
  clearStoredSession,
  getStoredSession,
  storeSession,
} from '../features/auth/storage/authStorage'
import type {
  ApiErrorResponse,
  AuthResponse,
  RetryableRequest,
} from '../features/auth/types/auth.types'

const baseURL = import.meta.env.VITE_API_URL ?? '/api'

export const api = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
})

const refreshClient = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
})

let refreshRequest: Promise<AuthResponse> | null = null

api.interceptors.request.use((config) => {
  const accessToken = getStoredSession()?.accessToken
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiErrorResponse>) => {
    const request = error.config as RetryableRequest | undefined
    const isCredentialRequest = ['/auth/login', '/auth/register', '/auth/refresh'].includes(
      request?.url ?? '',
    )
    const session = getStoredSession()

    if (
      error.response?.status !== 401 ||
      !request ||
      request._retry ||
      isCredentialRequest ||
      !session?.refreshToken
    ) {
      return Promise.reject(error)
    }

    request._retry = true

    try {
      refreshRequest ??= refreshClient
        .post<AuthResponse>('/auth/refresh', {
          refreshToken: session.refreshToken,
        })
        .then((response) => {
          storeSession(response.data)
          return response.data
        })
        .finally(() => {
          refreshRequest = null
        })

      const refreshedSession = await refreshRequest
      request.headers.Authorization = `Bearer ${refreshedSession.accessToken}`
      return api.request(request)
    } catch (refreshError) {
      clearStoredSession()
      return Promise.reject(refreshError)
    }
  },
)

export function getApiError(error: unknown): ApiErrorResponse | null {
  if (!axios.isAxiosError<ApiErrorResponse>(error)) return null
  return error.response?.data ?? null
}

export function isNetworkError(error: unknown) {
  return axios.isAxiosError(error) && !error.response
}

export function request<T>(config: AxiosRequestConfig) {
  return api.request<T>(config)
}
