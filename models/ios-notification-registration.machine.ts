import { assign, setup } from 'xstate'

export type AuthorizationStatus = 'notDetermined' | 'denied' | 'authorized' | 'provisional' | 'ephemeral'
export type RegistrationFailure = 'transient' | 'authentication' | 'configuration'
export type RegistrationResumeState = 'checkingPermission' | 'requestingPermission' | 'registeringApns' | 'registeringBackend' | 'unregistering'

export interface RegistrationContext {
  installationId: string
  authorizationStatus: AuthorizationStatus
  authSessionId: string | null
  hasUsableJwt: boolean
  tokenFingerprint: string | null
  backendRegistrationId: string | null
  attempt: number
  maxAttempts: number
  nextRetryAt: number | null
  lastErrorClass: RegistrationFailure | null
  resumeState: RegistrationResumeState
  correlationId: string | null
  invocationSequence: number
  logoutRequested: boolean
}

export type RegistrationEvent =
  | { type: 'APP_BECAME_ACTIVE' }
  | { type: 'PERMISSION_STATUS_RESOLVED'; status: AuthorizationStatus; correlationId: string }
  | { type: 'PERMISSION_STATUS_FAILED'; failure: RegistrationFailure; correlationId: string }
  | { type: 'USER_REQUESTED_ENABLE' }
  | { type: 'USER_OPENED_SETTINGS' }
  | { type: 'USER_CANCELLED' }
  | { type: 'PERMISSION_GRANTED'; status: Exclude<AuthorizationStatus, 'notDetermined' | 'denied'>; correlationId: string }
  | { type: 'PERMISSION_DENIED'; correlationId: string }
  | { type: 'PERMISSION_REQUEST_FAILED'; failure: RegistrationFailure; correlationId: string }
  | { type: 'APNS_DID_REGISTER'; tokenFingerprint: string; correlationId: string }
  | { type: 'APNS_DID_FAIL'; failure: RegistrationFailure; correlationId: string }
  | { type: 'AUTH_BECAME_AVAILABLE'; authSessionId: string }
  | { type: 'AUTH_BECAME_UNAVAILABLE' }
  | { type: 'BACKEND_REGISTER_SUCCEEDED'; backendRegistrationId: string; correlationId: string }
  | { type: 'BACKEND_REGISTER_FAILED'; failure: RegistrationFailure; correlationId: string; nextRetryAt?: number }
  | { type: 'RETRY_DUE' }
  | { type: 'LOGOUT_REQUESTED' }
  | { type: 'BACKEND_UNREGISTER_SUCCEEDED'; correlationId: string }
  | { type: 'BACKEND_UNREGISTER_FAILED'; failure: RegistrationFailure; correlationId: string; nextRetryAt?: number }
  | { type: 'CONFIGURATION_INVALID' }

export const permissionAllowsRemoteRegistration = (status: AuthorizationStatus) =>
  status === 'authorized' || status === 'provisional' || status === 'ephemeral'

export const hasUsableAuthentication = (context: RegistrationContext) =>
  context.authSessionId !== null && context.hasUsableJwt

export const isCorrelated = (context: RegistrationContext, event: RegistrationEvent) =>
  !('correlationId' in event) || context.correlationId === null || event.correlationId === context.correlationId

export const registrationBackoff = (attempt: number, random = 0.5, baseMs = 1_000, capMs = 60_000) =>
  Math.floor(Math.min(capMs, baseMs * 2 ** Math.max(0, attempt - 1)) * Math.max(0, Math.min(1, random)))

const failureActions = ['captureFailure', 'incrementAttempt'] as const

export const iosNotificationRegistrationMachine = setup({
  types: { context: {} as RegistrationContext, events: {} as RegistrationEvent, input: {} as Pick<RegistrationContext, 'installationId'> & Partial<RegistrationContext> },
  guards: {
    correlated: ({ context, event }) => isCorrelated(context, event),
    permissionAllowed: ({ event }) => event.type === 'PERMISSION_STATUS_RESOLVED' && permissionAllowsRemoteRegistration(event.status),
    permissionNotDetermined: ({ event }) => event.type === 'PERMISSION_STATUS_RESOLVED' && event.status === 'notDetermined',
    permissionDenied: ({ event }) => event.type === 'PERMISSION_STATUS_RESOLVED' && event.status === 'denied',
    hasAuth: ({ context }) => hasUsableAuthentication(context),
    hasAssociation: ({ context }) => context.backendRegistrationId !== null,
    retryBudgetAvailable: ({ context }) => context.attempt < context.maxAttempts,
    transientFailure: ({ event }) => 'failure' in event && event.failure === 'transient',
    correlatedTransientFailure: ({ context, event }) => isCorrelated(context, event) && 'failure' in event && event.failure === 'transient',
    correlatedAuthenticationFailure: ({ context, event }) => isCorrelated(context, event) && 'failure' in event && event.failure === 'authentication',
    correlatedConfigurationFailure: ({ context, event }) => isCorrelated(context, event) && 'failure' in event && event.failure === 'configuration',
    authenticationFailure: ({ event }) => 'failure' in event && event.failure === 'authentication',
  },
  actions: {
    readPermissionStatus: () => {},
    requestAuthorization: () => {},
    openSystemSettings: () => {},
    registerForRemoteNotifications: () => {},
    registerInstallationWithBackend: () => {},
    unregisterInstallationWithBackend: () => {},
    scheduleRetry: () => {},
    emitPushUnregistered: () => {},
    auditStaleCallback: () => {},
    beginInvocation: assign({ invocationSequence: ({ context }) => context.invocationSequence + 1, correlationId: ({ context }) => `registration-${context.installationId}-${context.invocationSequence + 1}` }),
    setPermission: assign({ authorizationStatus: ({ event }) => event.type === 'PERMISSION_STATUS_RESOLVED' ? event.status : event.type === 'PERMISSION_GRANTED' ? event.status : event.type === 'PERMISSION_DENIED' ? 'denied' : 'notDetermined', correlationId: ({ event }) => 'correlationId' in event ? event.correlationId : null }),
    setToken: assign({ tokenFingerprint: ({ event }) => event.type === 'APNS_DID_REGISTER' ? event.tokenFingerprint : null }),
    setAuth: assign({ authSessionId: ({ event }) => event.type === 'AUTH_BECAME_AVAILABLE' ? event.authSessionId : null, hasUsableJwt: ({ event }) => event.type === 'AUTH_BECAME_AVAILABLE' }),
    clearAuth: assign({ authSessionId: null, hasUsableJwt: false }),
    setRegistration: assign({ backendRegistrationId: ({ event }) => event.type === 'BACKEND_REGISTER_SUCCEEDED' ? event.backendRegistrationId : null, attempt: 0, lastErrorClass: null, nextRetryAt: null }),
    clearRegistration: assign({ backendRegistrationId: null, tokenFingerprint: null, attempt: 0, lastErrorClass: null, nextRetryAt: null }),
    markLogout: assign({ logoutRequested: true }),
    captureFailure: assign({ lastErrorClass: ({ event }) => 'failure' in event ? event.failure : 'configuration', nextRetryAt: ({ event }) => 'nextRetryAt' in event ? event.nextRetryAt ?? null : null }),
    incrementAttempt: assign({ attempt: ({ context }) => context.attempt + 1 }),
    resumeCheckingPermission: assign({ resumeState: 'checkingPermission' }),
    resumeRequestingPermission: assign({ resumeState: 'requestingPermission' }),
    resumeRegisteringApns: assign({ resumeState: 'registeringApns' }),
    resumeRegisteringBackend: assign({ resumeState: 'registeringBackend' }),
    resumeUnregistering: assign({ resumeState: 'unregistering' }),
  },
}).createMachine({
  id: 'iosNotificationRegistration',
  initial: 'checkingPermission',
  context: ({ input }) => ({ installationId: input.installationId, authorizationStatus: input.authorizationStatus ?? 'notDetermined', authSessionId: input.authSessionId ?? null, hasUsableJwt: input.hasUsableJwt ?? false, tokenFingerprint: null, backendRegistrationId: input.backendRegistrationId ?? null, attempt: 0, maxAttempts: input.maxAttempts ?? 3, nextRetryAt: null, lastErrorClass: null, resumeState: 'checkingPermission', correlationId: null, invocationSequence: 0, logoutRequested: false }),
  states: {
    checkingPermission: { entry: ['beginInvocation', 'readPermissionStatus'], on: {
      PERMISSION_STATUS_RESOLVED: [
        { guard: ({ context, event }) => isCorrelated(context, event) && permissionAllowsRemoteRegistration(event.status), target: 'registeringApns', actions: 'setPermission' },
        { guard: ({ context, event }) => isCorrelated(context, event) && event.status === 'notDetermined', target: 'notDetermined', actions: 'setPermission' },
        { guard: ({ context, event }) => isCorrelated(context, event) && event.status === 'denied', target: 'denied', actions: 'setPermission' },
        { actions: 'auditStaleCallback' },
      ],
      PERMISSION_STATUS_FAILED: [{ guard: 'correlatedTransientFailure', target: 'retry', actions: [...failureActions, 'resumeCheckingPermission'] }, { guard: 'correlatedConfigurationFailure', target: 'misconfigured', actions: 'captureFailure' }, { actions: 'auditStaleCallback' }], CONFIGURATION_INVALID: 'misconfigured' } },
    notDetermined: { on: { USER_REQUESTED_ENABLE: 'requestingPermission', USER_CANCELLED: 'cancelled', LOGOUT_REQUESTED: { target: 'unregistered', actions: 'markLogout' }, APP_BECAME_ACTIVE: 'checkingPermission' } },
    requestingPermission: { entry: ['beginInvocation', 'requestAuthorization'], on: { PERMISSION_GRANTED: { guard: 'correlated', target: 'registeringApns', actions: 'setPermission' }, PERMISSION_DENIED: { guard: 'correlated', target: 'denied', actions: 'setPermission' }, PERMISSION_REQUEST_FAILED: [{ guard: 'correlatedTransientFailure', target: 'retry', actions: [...failureActions, 'resumeRequestingPermission'] }, { guard: 'correlatedConfigurationFailure', target: 'misconfigured', actions: 'captureFailure' }, { actions: 'auditStaleCallback' }], CONFIGURATION_INVALID: 'misconfigured' } },
    denied: { on: { USER_OPENED_SETTINGS: { actions: 'openSystemSettings' }, APP_BECAME_ACTIVE: 'checkingPermission', USER_CANCELLED: 'cancelled', LOGOUT_REQUESTED: { target: 'unregistered', actions: 'markLogout' } } },
    registeringApns: { entry: ['beginInvocation', 'registerForRemoteNotifications'], on: { APNS_DID_REGISTER: [{ guard: ({ context, event }) => isCorrelated(context, event) && hasUsableAuthentication(context), target: 'registeringBackend', actions: 'setToken' }, { guard: 'correlated', target: 'awaitingAuthentication', actions: 'setToken' }, { actions: 'auditStaleCallback' }], APNS_DID_FAIL: [{ guard: 'correlatedTransientFailure', target: 'retry', actions: [...failureActions, 'resumeRegisteringApns'] }, { guard: 'correlatedConfigurationFailure', target: 'misconfigured', actions: 'captureFailure' }, { actions: 'auditStaleCallback' }], LOGOUT_REQUESTED: [{ guard: 'hasAssociation', target: 'unregistering', actions: 'markLogout' }, { target: 'unregistered', actions: 'markLogout' }], CONFIGURATION_INVALID: 'misconfigured' } },
    awaitingAuthentication: { on: { AUTH_BECAME_AVAILABLE: { target: 'registeringBackend', actions: 'setAuth' }, APNS_DID_REGISTER: { guard: 'correlated', actions: 'setToken' }, USER_CANCELLED: 'cancelled', LOGOUT_REQUESTED: { target: 'unregistered', actions: 'markLogout' } } },
    registeringBackend: { entry: ['beginInvocation', 'registerInstallationWithBackend'], on: { BACKEND_REGISTER_SUCCEEDED: [{ guard: 'correlated', target: 'registered', actions: 'setRegistration' }, { actions: 'auditStaleCallback' }], BACKEND_REGISTER_FAILED: [{ guard: 'correlatedAuthenticationFailure', target: 'awaitingAuthentication', actions: 'captureFailure' }, { guard: 'correlatedTransientFailure', target: 'retry', actions: [...failureActions, 'resumeRegisteringBackend'] }, { guard: 'correlatedConfigurationFailure', target: 'misconfigured', actions: 'captureFailure' }, { actions: 'auditStaleCallback' }], LOGOUT_REQUESTED: { target: 'unregistering', actions: 'markLogout' }, CONFIGURATION_INVALID: 'misconfigured' } },
    retry: { entry: 'scheduleRetry', on: { RETRY_DUE: [{ guard: ({ context }) => context.attempt < context.maxAttempts && context.resumeState === 'unregistering' && hasUsableAuthentication(context), target: 'unregistering' }, { guard: ({ context }) => context.attempt < context.maxAttempts && context.resumeState === 'registeringBackend' && hasUsableAuthentication(context), target: 'registeringBackend' }, { guard: ({ context }) => context.attempt < context.maxAttempts && context.resumeState === 'registeringApns', target: 'registeringApns' }, { guard: ({ context }) => context.attempt < context.maxAttempts && context.resumeState === 'requestingPermission', target: 'requestingPermission' }, { guard: 'retryBudgetAvailable', target: 'checkingPermission' }, { target: 'misconfigured' }], AUTH_BECAME_UNAVAILABLE: { actions: 'captureFailure' }, APP_BECAME_ACTIVE: { guard: ({ context }) => !context.logoutRequested, target: 'checkingPermission' }, USER_CANCELLED: { guard: ({ context }) => !context.logoutRequested, target: 'cancelled' }, LOGOUT_REQUESTED: [{ guard: ({ context }) => context.backendRegistrationId !== null && hasUsableAuthentication(context), target: 'unregistering', actions: 'markLogout' }, { actions: 'markLogout' }], CONFIGURATION_INVALID: 'misconfigured' } },
    registered: { on: { APNS_DID_REGISTER: { guard: 'correlated', target: 'registeringBackend', actions: 'setToken' }, AUTH_BECAME_UNAVAILABLE: { actions: 'captureFailure' }, LOGOUT_REQUESTED: [{ guard: 'hasAuth', target: 'unregistering', actions: 'markLogout' }, { target: 'retry', actions: ['markLogout', 'resumeUnregistering'] }], APP_BECAME_ACTIVE: 'checkingPermission' } },
    unregistering: { entry: ['beginInvocation', 'unregisterInstallationWithBackend'], on: { BACKEND_UNREGISTER_SUCCEEDED: [{ guard: 'correlated', target: 'unregistered', actions: 'clearRegistration' }, { actions: 'auditStaleCallback' }], BACKEND_UNREGISTER_FAILED: [{ guard: 'correlatedTransientFailure', target: 'retry', actions: [...failureActions, 'resumeUnregistering'] }, { guard: 'correlatedConfigurationFailure', target: 'misconfigured', actions: 'captureFailure' }, { actions: 'auditStaleCallback' }], CONFIGURATION_INVALID: 'misconfigured' } },
    unregistered: { type: 'final', entry: 'emitPushUnregistered' }, cancelled: { type: 'final' }, misconfigured: { type: 'final' },
  },
})

export const registrationInvariants = [
  'permission prompt only follows USER_REQUESTED_ENABLE from notDetermined', 'backend calls require usable authentication', 'registered requires backend acknowledgement', 'logout retains credentials until unregistered', 'raw tokens and JWTs never enter observable context', 'stale callbacks never transition state', 'unregistration targets one installation', 'no state transition depends on free text or an LLM',
] as const
