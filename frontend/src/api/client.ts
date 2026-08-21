import axios, {
  AxiosError,
  type AxiosRequestConfig,
} from 'axios'
import { notifyAuthSessionExpired } from '../features/auth/events/authEvents'
import type {
  ApiErrorResponse,
  AuthResponse,
  RetryableRequest,
} from '../features/auth/types/auth.types'

const baseURL = import.meta.env.VITE_API_URL ?? '/api'

export const api = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
})

const refreshClient = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
})

const SAFE_METHODS = new Set(['get', 'head', 'options'])
let csrfRequest: Promise<void> | null = null
let refreshRequest: Promise<AuthResponse> | null = null

function hasCsrfCookie() {
  if (typeof document === 'undefined') return false
  return document.cookie
    .split('; ')
    .some((cookie) => cookie.startsWith('XSRF-TOKEN='))
}

export async function initializeCsrf() {
  if (hasCsrfCookie()) return

  csrfRequest ??= refreshClient
    .get('/auth/csrf')
    .then(() => undefined)
    .finally(() => {
      csrfRequest = null
    })

  await csrfRequest
}

api.interceptors.request.use(async (config) => {
  const method = config.method?.toLowerCase() ?? 'get'
  if (!SAFE_METHODS.has(method)) await initializeCsrf()
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiErrorResponse>) => {
    const request = error.config as RetryableRequest | undefined
    const isCredentialRequest = [
      '/auth/login',
      '/auth/register',
      '/auth/refresh',
      '/auth/verification-email',
      '/auth/verify-email',
      '/auth/forgot-password',
      '/auth/reset-password',
    ].includes(request?.url ?? '')
    if (
      error.response?.status !== 401 ||
      !request ||
      request._retry ||
      isCredentialRequest
    ) {
      return Promise.reject(error)
    }

    request._retry = true

    try {
      await initializeCsrf()
      refreshRequest ??= refreshClient
        .post<AuthResponse>('/auth/refresh')
        .then((response) => response.data)
        .finally(() => {
          refreshRequest = null
        })

      await refreshRequest
      return api.request(request)
    } catch (refreshError) {
      notifyAuthSessionExpired()
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
