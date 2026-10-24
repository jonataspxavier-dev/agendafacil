import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'

const ACCESS_TOKEN_KEY = 'agendafacil.accessToken'
const REFRESH_TOKEN_KEY = 'agendafacil.refreshToken'

export interface AuthTokens {
  accessToken: string
  refreshToken: string
}

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function storeTokens(tokens: AuthTokens): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken)
  localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken)
}

export function clearTokens(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export const api = axios.create({ baseURL: '/api' })

api.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export async function refreshTokens(): Promise<AuthTokens> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new Error('Nenhum refresh token disponível')
  }

  const { data } = await axios.post<AuthTokens & { expiresInSeconds: number }>('/api/auth/refresh', {
    refreshToken,
  })
  storeTokens(data)
  return data
}

let onAuthFailure: (() => void) | undefined

export function setOnAuthFailure(handler: () => void): void {
  onAuthFailure = handler
}

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean
}

export function extractErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { message?: string } | undefined
    if (data?.message) {
      return data.message
    }
  }
  return 'Ocorreu um erro inesperado. Tente novamente.'
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryableRequestConfig | undefined
    const status = error.response?.status

    if (status === 401 && originalRequest && !originalRequest._retry && getRefreshToken()) {
      originalRequest._retry = true
      try {
        await refreshTokens()
        return api(originalRequest)
      } catch {
        clearTokens()
        onAuthFailure?.()
        return Promise.reject(error)
      }
    }

    if (status === 401) {
      clearTokens()
      onAuthFailure?.()
    }

    return Promise.reject(error)
  },
)
