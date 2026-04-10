import type { AuthResponse } from '$lib/types/api'

const TOKEN_KEY = 'wakeve_access_token'
const REFRESH_KEY = 'wakeve_refresh_token'
const EXPIRY_KEY = 'wakeve_token_expiry'
const USER_KEY = 'wakeve_user'

export class ApiError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message)
    this.name = 'ApiError'
  }
}

export function clearTokens(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(REFRESH_KEY)
  localStorage.removeItem(EXPIRY_KEY)
  localStorage.removeItem(USER_KEY)
}

export function saveAuthData(response: AuthResponse): void {
  localStorage.setItem(TOKEN_KEY, response.accessToken)
  localStorage.setItem(REFRESH_KEY, response.refreshToken)
  localStorage.setItem(EXPIRY_KEY, String(Date.now() + response.expiresIn * 1000))
  localStorage.setItem(USER_KEY, JSON.stringify(response.user))
}

export function isTokenExpired(): boolean {
  const expiry = localStorage.getItem(EXPIRY_KEY)
  if (!expiry) return true
  return Date.now() > parseInt(expiry, 10) - 30_000
}

function getAccessToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_KEY)
}

let isRefreshing = false
let refreshPromise: Promise<void> | null = null

async function tryRefreshToken(): Promise<void> {
  if (isRefreshing && refreshPromise) return refreshPromise
  isRefreshing = true
  refreshPromise = (async () => {
    const refreshToken = getRefreshToken()
    if (!refreshToken) throw new ApiError(401, 'No refresh token')
    const resp = await fetch('/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    })
    if (!resp.ok) {
      clearTokens()
      throw new ApiError(resp.status, 'Token refresh failed')
    }
    const data = await resp.json()
    localStorage.setItem(TOKEN_KEY, data.accessToken)
    localStorage.setItem(EXPIRY_KEY, String(Date.now() + data.expiresIn * 1000))
  })()
  try {
    await refreshPromise
  } finally {
    isRefreshing = false
    refreshPromise = null
  }
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
  requiresAuth = true
): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> ?? {})
  }

  if (requiresAuth) {
    if (isTokenExpired()) {
      try {
        await tryRefreshToken()
      } catch {
        clearTokens()
        window.dispatchEvent(new CustomEvent('wakeve:auth-expired'))
        throw new ApiError(401, 'Session expired')
      }
    }
    const token = getAccessToken()
    if (token) headers['Authorization'] = `Bearer ${token}`
  }

  const response = await fetch(`/api${path}`, { ...options, headers })

  if (response.status === 204) return null as T

  if (response.status === 401 && requiresAuth) {
    try {
      await tryRefreshToken()
      const token = getAccessToken()
      if (token) headers['Authorization'] = `Bearer ${token}`
      const retryResponse = await fetch(`/api${path}`, { ...options, headers })
      if (retryResponse.status === 204) return null as T
      if (!retryResponse.ok) {
        const err = await retryResponse.json().catch(() => ({}))
        throw new ApiError(retryResponse.status, err.message ?? 'Request failed')
      }
      return retryResponse.json()
    } catch (e) {
      if (e instanceof ApiError && e.status !== 401) throw e
      clearTokens()
      window.dispatchEvent(new CustomEvent('wakeve:auth-expired'))
      throw new ApiError(401, 'Session expired')
    }
  }

  if (!response.ok) {
    const err = await response.json().catch(() => ({}))
    throw new ApiError(response.status, err.message ?? 'Request failed')
  }

  return response.json()
}
