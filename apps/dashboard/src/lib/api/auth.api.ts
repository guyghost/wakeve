import type {
  AuthResponse,
  OTPRequestResponse,
  TokenRefreshResponse,
  UserDTO
} from '$lib/types/api'
import { apiFetch, clearTokens, saveAuthData } from './client'

const USER_KEY = 'wakeve_user'

/**
 * Request an OTP to be sent to the given email address.
 */
export async function requestOtp(email: string): Promise<OTPRequestResponse> {
  return apiFetch<OTPRequestResponse>(
    '/auth/email/request-otp',
    {
      method: 'POST',
      body: JSON.stringify({ email })
    },
    false
  )
}

/**
 * Verify an OTP code and obtain auth tokens.
 */
export async function verifyOtp(
  email: string,
  otp: string,
  deviceId: string
): Promise<AuthResponse> {
  const response = await apiFetch<AuthResponse>(
    '/auth/email/verify-otp',
    {
      method: 'POST',
      body: JSON.stringify({ email, otp, deviceId })
    },
    false
  )
  saveAuthData(response)
  return response
}

/**
 * Create a guest session (no email required).
 */
export async function loginGuest(
  deviceId: string,
  displayName?: string
): Promise<AuthResponse> {
  const response = await apiFetch<AuthResponse>(
    '/auth/guest',
    {
      method: 'POST',
      body: JSON.stringify({ deviceId, ...(displayName ? { displayName } : {}) })
    },
    false
  )
  saveAuthData(response)
  return response
}

/**
 * Exchange a refresh token for a new access token.
 */
export async function refreshToken(token: string): Promise<TokenRefreshResponse> {
  return apiFetch<TokenRefreshResponse>(
    '/auth/refresh',
    {
      method: 'POST',
      body: JSON.stringify({ refreshToken: token })
    },
    false
  )
}

/**
 * Read the currently authenticated user from localStorage.
 * Returns null if no user is stored.
 */
export function getStoredUser(): UserDTO | null {
  if (typeof localStorage === 'undefined') return null
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserDTO
  } catch {
    return null
  }
}

/**
 * Return true when a valid (non-expired) access token exists in storage.
 */
export function isLoggedIn(): boolean {
  if (typeof localStorage === 'undefined') return false
  const token = localStorage.getItem('wakeve_access_token')
  if (!token) return false
  const expiry = localStorage.getItem('wakeve_token_expiry')
  if (!expiry) return false
  return Date.now() < parseInt(expiry, 10)
}

/**
 * Clear all auth tokens and user data from localStorage.
 */
export function logout(): void {
  clearTokens()
}
