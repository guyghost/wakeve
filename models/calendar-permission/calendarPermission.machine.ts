import { assign, emit, fromPromise, setup } from 'xstate'

export type CalendarAuthorizationStatus =
  | 'notDetermined'
  | 'restricted'
  | 'denied'
  | 'fullAccess'
  | 'writeOnly'

export interface CalendarPermissionPort {
  readAuthorizationStatus: () => Promise<CalendarAuthorizationStatus>
  requestFullAccessToEvents: () => Promise<CalendarAuthorizationStatus>
  openApplicationSettings: () => Promise<void>
}

export interface CalendarPermissionInput {
  port: CalendarPermissionPort
}

interface CalendarPermissionContext {
  port: CalendarPermissionPort
  lastError: string | null
}

export type CalendarPermissionEvent =
  | { type: 'START' }
  | { type: 'OPEN_SETTINGS' }
  | { type: 'APP_BECAME_ACTIVE' }
  | { type: 'RETRY' }
  | { type: 'CANCEL' }

export type CalendarPermissionSignal =
  | { type: 'CALENDAR_PERMISSION_GRANTED' }
  | { type: 'SHOW_DENIED_GUIDANCE' }
  | { type: 'SHOW_RESTRICTED_GUIDANCE' }
  | { type: 'SHOW_FULL_ACCESS_REQUIRED' }
  | { type: 'WAIT_FOR_SETTINGS_RETURN' }
  | { type: 'SHOW_RETRYABLE_ERROR' }
  | { type: 'CALENDAR_PERMISSION_FAILED' }

interface CalendarPermissionActorInput {
  port: CalendarPermissionPort
}

const actorOutput = (event: unknown): unknown => {
  if (typeof event !== 'object' || event === null || !('output' in event)) {
    return undefined
  }
  return (event as { output: unknown }).output
}

const actorOutputIs = (
  event: unknown,
  expected: CalendarAuthorizationStatus,
): boolean => actorOutput(event) === expected

const actorErrorMessage = (event: unknown): string => {
  if (typeof event !== 'object' || event === null || !('error' in event)) {
    return 'Unknown calendar permission error'
  }

  const error = (event as { error: unknown }).error
  return error instanceof Error ? error.message : String(error)
}

export const calendarPermissionMachine = setup({
  types: {
    context: {} as CalendarPermissionContext,
    events: {} as CalendarPermissionEvent,
    input: {} as CalendarPermissionInput,
    emitted: {} as CalendarPermissionSignal,
  },
  actors: {
    readAuthorizationStatus: fromPromise<
      CalendarAuthorizationStatus,
      CalendarPermissionActorInput
    >(async ({ input }) => input.port.readAuthorizationStatus()),
    requestFullAccessToEvents: fromPromise<
      CalendarAuthorizationStatus,
      CalendarPermissionActorInput
    >(async ({ input }) => input.port.requestFullAccessToEvents()),
    openApplicationSettings: fromPromise<void, CalendarPermissionActorInput>(
      async ({ input }) => input.port.openApplicationSettings(),
    ),
  },
  guards: {
    actorReturnedNotDetermined: ({ event }) =>
      actorOutputIs(event, 'notDetermined'),
    actorReturnedRestricted: ({ event }) => actorOutputIs(event, 'restricted'),
    actorReturnedDenied: ({ event }) => actorOutputIs(event, 'denied'),
    actorReturnedFullAccess: ({ event }) => actorOutputIs(event, 'fullAccess'),
    actorReturnedWriteOnly: ({ event }) => actorOutputIs(event, 'writeOnly'),
  },
  actions: {
    clearLastError: assign({ lastError: null }),
    captureActorError: assign({
      lastError: ({ event }) => actorErrorMessage(event),
    }),
    captureUnexpectedStatus: assign({
      lastError: ({ event }) =>
        `Unexpected calendar authorization status: ${String(actorOutput(event))}`,
    }),
    emitPermissionGranted: emit({ type: 'CALENDAR_PERMISSION_GRANTED' }),
    emitDeniedGuidance: emit({ type: 'SHOW_DENIED_GUIDANCE' }),
    emitRestrictedGuidance: emit({ type: 'SHOW_RESTRICTED_GUIDANCE' }),
    emitFullAccessRequired: emit({ type: 'SHOW_FULL_ACCESS_REQUIRED' }),
    emitWaitingForSettings: emit({ type: 'WAIT_FOR_SETTINGS_RETURN' }),
    emitRetryableError: emit({ type: 'SHOW_RETRYABLE_ERROR' }),
    emitTerminalFailure: emit({ type: 'CALENDAR_PERMISSION_FAILED' }),
  },
}).createMachine({
  id: 'iosCalendarPermission',
  initial: 'idle',
  context: ({ input }) => ({
    port: input.port,
    lastError: null,
  }),
  states: {
    idle: {
      on: {
        START: 'checkingAuthorization',
        CANCEL: 'cancelled',
      },
    },
    checkingAuthorization: {
      entry: 'clearLastError',
      invoke: {
        id: 'read-calendar-authorization-status',
        src: 'readAuthorizationStatus',
        input: ({ context }) => ({ port: context.port }),
        onDone: [
          {
            guard: 'actorReturnedFullAccess',
            target: 'fullAccess',
          },
          {
            guard: 'actorReturnedNotDetermined',
            target: 'requestingFullAccess',
          },
          {
            guard: 'actorReturnedDenied',
            target: 'denied',
          },
          {
            guard: 'actorReturnedRestricted',
            target: 'restricted',
          },
          {
            guard: 'actorReturnedWriteOnly',
            target: 'writeOnly',
          },
          {
            target: 'error',
            actions: 'captureUnexpectedStatus',
          },
        ],
        onError: {
          target: 'error',
          actions: 'captureActorError',
        },
      },
    },
    requestingFullAccess: {
      invoke: {
        id: 'request-full-calendar-access',
        src: 'requestFullAccessToEvents',
        input: ({ context }) => ({ port: context.port }),
        onDone: [
          {
            guard: 'actorReturnedFullAccess',
            target: 'fullAccess',
          },
          {
            guard: 'actorReturnedDenied',
            target: 'denied',
          },
          {
            guard: 'actorReturnedRestricted',
            target: 'restricted',
          },
          {
            guard: 'actorReturnedWriteOnly',
            target: 'writeOnly',
          },
          {
            target: 'error',
            actions: 'captureUnexpectedStatus',
          },
        ],
        onError: {
          target: 'error',
          actions: 'captureActorError',
        },
      },
      on: {
        CANCEL: 'cancelled',
      },
    },
    denied: {
      entry: 'emitDeniedGuidance',
      on: {
        OPEN_SETTINGS: 'openingSettings',
        CANCEL: 'cancelled',
      },
    },
    writeOnly: {
      entry: 'emitFullAccessRequired',
      on: {
        OPEN_SETTINGS: 'openingSettings',
        CANCEL: 'cancelled',
      },
    },
    openingSettings: {
      invoke: {
        id: 'open-calendar-settings',
        src: 'openApplicationSettings',
        input: ({ context }) => ({ port: context.port }),
        onDone: 'waitingForSettingsReturn',
        onError: {
          target: 'error',
          actions: 'captureActorError',
        },
      },
    },
    waitingForSettingsReturn: {
      entry: 'emitWaitingForSettings',
      on: {
        APP_BECAME_ACTIVE: 'checkingAuthorization',
        CANCEL: 'cancelled',
      },
    },
    error: {
      entry: 'emitRetryableError',
      on: {
        RETRY: 'checkingAuthorization',
        CANCEL: 'failed',
      },
    },
    fullAccess: {
      type: 'final',
      entry: 'emitPermissionGranted',
    },
    restricted: {
      type: 'final',
      entry: 'emitRestrictedGuidance',
    },
    cancelled: {
      type: 'final',
    },
    failed: {
      type: 'final',
      entry: 'emitTerminalFailure',
    },
  },
})
