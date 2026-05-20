import { setup, assign, fromPromise } from 'xstate'
import type { UserDTO, AuthResponse } from '$lib/types/api'
import * as authApi from '$lib/api/auth.api'
import { clearTokens, saveAuthData, isTokenExpired } from '$lib/api/client'

interface AuthContext {
  user: UserDTO | null
  error: string | null
  email: string
  deviceId: string
}

type AuthEvent =
  | { type: 'REQUEST_OTP'; email: string }
  | { type: 'VERIFY_OTP'; otp: string }
  | { type: 'LOGIN_GUEST' }
  | { type: 'LOGOUT' }
  | { type: 'BACK' }

const restoreSessionActor = fromPromise(async (): Promise<{
  status: 'authenticated' | 'expired' | 'none'
  user: UserDTO | null
}> => {
  const user = authApi.getStoredUser()
  if (!user) return { status: 'none', user: null }
  if (!isTokenExpired()) return { status: 'authenticated', user }
  return { status: 'expired', user }
})

export const authMachine = setup({
  types: {
    context: {} as AuthContext,
    events: {} as AuthEvent
  },
  actors: {
    restoreSession: restoreSessionActor,

    requestOtp: fromPromise(async ({ input }: { input: { email: string } }) => {
      await authApi.requestOtp(input.email)
    }),

    verifyOtp: fromPromise(async ({
      input
    }: {
      input: { email: string; otp: string; deviceId: string }
    }): Promise<AuthResponse> => {
      return authApi.verifyOtp(input.email, input.otp, input.deviceId)
    }),

    loginGuest: fromPromise(async ({
      input
    }: {
      input: { deviceId: string }
    }): Promise<AuthResponse> => {
      return authApi.loginGuest(input.deviceId)
    }),

    refreshToken: fromPromise(async (): Promise<{ accessToken: string; expiresIn: number }> => {
      const storedRefresh = localStorage.getItem('wakeve_refresh_token')
      if (!storedRefresh) throw new Error('No refresh token')
      const resp = await fetch('/api/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: storedRefresh })
      })
      if (!resp.ok) throw new Error('Refresh failed')
      const data = await resp.json()
      localStorage.setItem('wakeve_access_token', data.accessToken)
      localStorage.setItem(
        'wakeve_token_expiry',
        String(Date.now() + data.expiresIn * 1000)
      )
      return data
    })
  },

  actions: {
    assignUser: assign({
      user: ({ event }) =>
        (event as { output: AuthResponse }).output.user
    }),

    assignError: assign({
      error: ({ event }) =>
        String((event as { error: unknown }).error)
    }),

    clearError: assign({ error: null }),

    assignEmail: assign({
      email: ({ event }) =>
        (event as { type: 'REQUEST_OTP'; email: string }).email
    }),

    doLogout: () => {
      clearTokens()
    },

    saveAuth: ({ event }) => {
      saveAuthData((event as { output: AuthResponse }).output)
    },

    restoreUser: assign({
      user: () => authApi.getStoredUser()
    })
  }
}).createMachine({
  id: 'auth',
  initial: 'restoringSession',
  context: {
    user: null,
    error: null,
    email: '',
    deviceId:
      typeof crypto !== 'undefined'
        ? crypto.randomUUID()
        : Math.random().toString(36).slice(2)
  },
  states: {
    restoringSession: {
      invoke: {
        src: 'restoreSession',
        onDone: [
          {
            guard: ({ event }) => event.output.status === 'authenticated',
            target: 'authenticated',
            actions: assign({ user: ({ event }) => event.output.user })
          },
          {
            guard: ({ event }) => event.output.status === 'expired',
            target: 'refreshing'
          },
          {
            target: 'unauthenticated'
          }
        ],
        onError: {
          target: 'unauthenticated'
        }
      }
    },

    unauthenticated: {
      entry: 'clearError',
      on: {
        REQUEST_OTP: { target: 'requestingOtp', actions: 'assignEmail' },
        LOGIN_GUEST: { target: 'loggingInGuest' }
      }
    },

    requestingOtp: {
      invoke: {
        src: 'requestOtp',
        input: ({ context }) => ({ email: context.email }),
        onDone: { target: 'enteringOtp', actions: 'clearError' },
        onError: { target: 'unauthenticated', actions: 'assignError' }
      }
    },

    enteringOtp: {
      entry: 'clearError',
      on: {
        VERIFY_OTP: { target: 'verifyingOtp' },
        BACK: { target: 'unauthenticated' }
      }
    },

    verifyingOtp: {
      invoke: {
        src: 'verifyOtp',
        input: ({ context, event }) => ({
          email: context.email,
          otp: (event as { type: 'VERIFY_OTP'; otp: string }).otp,
          deviceId: context.deviceId
        }),
        onDone: {
          target: 'authenticated',
          actions: ['saveAuth', 'assignUser']
        },
        onError: {
          target: 'enteringOtp',
          actions: 'assignError'
        }
      }
    },

    loggingInGuest: {
      invoke: {
        src: 'loginGuest',
        input: ({ context }) => ({ deviceId: context.deviceId }),
        onDone: {
          target: 'authenticated',
          actions: ['saveAuth', 'assignUser']
        },
        onError: {
          target: 'unauthenticated',
          actions: 'assignError'
        }
      }
    },

    refreshing: {
      invoke: {
        src: 'refreshToken',
        onDone: {
          target: 'authenticated',
          actions: 'restoreUser'
        },
        onError: {
          target: 'unauthenticated',
          actions: 'doLogout'
        }
      }
    },

    authenticated: {
      on: {
        LOGOUT: { target: 'unauthenticated', actions: 'doLogout' }
      }
    }
  }
})
