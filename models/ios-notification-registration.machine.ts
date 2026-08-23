import { assign, createActor, setup } from 'xstate'

export type AuthorizationStatus = 'notDetermined' | 'denied' | 'authorized' | 'provisional' | 'ephemeral'
export type RegistrationFailure = 'transient' | 'authentication' | 'configuration'
export type RegistrationResumeState = 'checkingPermission' | 'requestingPermission' | 'registeringApns' | 'registeringBackend' | 'unregistering'
export type ApnsEnvironment = 'sandbox' | 'production'
export type RawApnsTokenClearReason = 'backendAcknowledged' | 'cancelled' | 'unregistered' | 'misconfigured'

export interface RawApnsTokenCustodyPort {
  replace(correlationId: string): void
  withToken(operation: 'registerInstallationWithBackend', correlationId: string): boolean
  clear(reason: RawApnsTokenClearReason): void
}

export interface RegistrationContext {
  installationId: string
  topic: string
  environment: ApnsEnvironment
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

export type BackendUnregistrationTarget =
  | { kind: 'registration'; registrationId: string; installationId: string }
  | { kind: 'installation'; installationId: string }

export const backendUnregistrationTarget = (context: RegistrationContext): BackendUnregistrationTarget =>
  context.backendRegistrationId !== null
    ? { kind: 'registration', registrationId: context.backendRegistrationId, installationId: context.installationId }
    : { kind: 'installation', installationId: context.installationId }

export const classifyRegistrationBackendFailure = (
  statusCode: number | null,
  networkFailure = false,
): RegistrationFailure => {
  if (networkFailure) return 'transient'
  if (statusCode === 401 || statusCode === 403) return 'authentication'
  if (statusCode === 408 || statusCode === 429 || (statusCode !== null && statusCode >= 500 && statusCode <= 599)) return 'transient'
  return 'configuration'
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
  | { type: 'RAW_APNS_TOKEN_UNAVAILABLE'; correlationId: string }
  | { type: 'BACKEND_REGISTER_SUCCEEDED'; backendRegistrationId: string; correlationId: string }
  | { type: 'BACKEND_REGISTER_FAILED'; failure: RegistrationFailure; correlationId: string; nextRetryAt?: number }
  | { type: 'RETRY_DUE' }
  | { type: 'LOGOUT_REQUESTED' }
  | { type: 'BACKEND_UNREGISTER_SUCCEEDED'; correlationId: string }
  | { type: 'BACKEND_UNREGISTER_FAILED'; failure: RegistrationFailure; correlationId: string; nextRetryAt?: number }
  | { type: 'CONFIGURATION_INVALID' }

export const validateRegistrationScope = (
  topic: string,
  environment: string,
): { topic: string; environment: ApnsEnvironment } | null => {
  const normalizedTopic = topic.trim()
  if (normalizedTopic.length === 0) return null
  if (environment !== 'sandbox' && environment !== 'production') return null
  return { topic: normalizedTopic, environment }
}

export const iosNotificationRegistrationPortContract = {
  correlatedEffects: [
    'readPermissionStatus',
    'requestAuthorization',
    'registerForRemoteNotifications',
    'registerInstallationWithBackend',
    'unregisterInstallationWithBackend',
  ],
  correlationField: 'correlationId',
  rawTokenCustody: 'private-port-outside-observable-context-and-snapshots',
  observableTokenValue: 'tokenFingerprint-only',
  registeredAcknowledgement: 'BACKEND_REGISTER_SUCCEEDED',
  backendFailureClassifier: 'classifyRegistrationBackendFailure(statusCode, networkFailure)',
  unregisterTarget: {
    resolver: 'backendUnregistrationTarget(context)',
    correlationField: 'correlationId',
  },
  logoutFlight: {
    transitionAuthority: 'iosNotificationRegistration-actor-only',
    observerCardinality: 'zero-or-more-read-only-observers',
    observerTransitionAuthority: false,
    successTerminal: 'unregistered',
    successSignal: 'PUSH_UNREGISTERED',
    blockedTerminal: 'misconfigured',
    blockedResult: 'return-to-all-observers-without-clearing-credentials',
  },
} as const

export const rawApnsTokenCustodyLifecycle = {
  replaceOn: 'APNS_DID_REGISTER',
  withTokenOnlyFor: 'registerInstallationWithBackend',
  clearOn: ['BACKEND_REGISTER_SUCCEEDED', 'cancelled', 'unregistered', 'misconfigured'],
  retainIn: ['awaitingAuthentication', 'retry'],
  observableContext: 'tokenFingerprint-only',
} as const

export const permissionAllowsRemoteRegistration = (status: AuthorizationStatus) =>
  status === 'authorized' || status === 'provisional' || status === 'ephemeral'

export const hasUsableAuthentication = (context: RegistrationContext) =>
  context.authSessionId !== null && context.hasUsableJwt

export const isCorrelated = (context: RegistrationContext, event: RegistrationEvent) =>
  !('correlationId' in event) || context.correlationId === null || event.correlationId === context.correlationId

export const registrationBackoff = (attempt: number, random = 0.5, baseMs = 1_000, capMs = 60_000) =>
  Math.floor(Math.min(capMs, baseMs * 2 ** Math.max(0, attempt - 1)) * Math.max(0, Math.min(1, random)))

const failureActions = ['captureFailure', 'incrementAttempt'] as const

type ValidatedRegistrationInput = Pick<RegistrationContext, 'installationId' | 'topic' | 'environment'> &
  Partial<Omit<RegistrationContext, 'installationId' | 'topic' | 'environment'>>

export type IosNotificationRegistrationInput = Pick<RegistrationContext, 'installationId'> &
  { topic: string; environment: string } &
  Partial<Omit<RegistrationContext, 'installationId' | 'topic' | 'environment'>>

const iosNotificationRegistrationMachine = setup({
  types: {
    context: {} as RegistrationContext,
    events: {} as RegistrationEvent,
    input: {} as ValidatedRegistrationInput,
  },
  guards: {
    correlated: ({ context, event }) => isCorrelated(context, event),
    permissionAllowed: ({ event }) => event.type === 'PERMISSION_STATUS_RESOLVED' && permissionAllowsRemoteRegistration(event.status),
    permissionNotDetermined: ({ event }) => event.type === 'PERMISSION_STATUS_RESOLVED' && event.status === 'notDetermined',
    permissionDenied: ({ event }) => event.type === 'PERMISSION_STATUS_RESOLVED' && event.status === 'denied',
    hasAuth: ({ context }) => hasUsableAuthentication(context),
    hasAssociation: ({ context }) => context.backendRegistrationId !== null,
    logoutAlreadyRequested: ({ context }) => context.logoutRequested,
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
    replaceRawApnsToken: () => {},
    withRawApnsTokenForBackendRegistration: () => {},
    clearRawApnsTokenAfterBackendAcknowledgement: () => {},
    clearRawApnsTokenOnCancelled: () => {},
    clearRawApnsTokenOnUnregistered: () => {},
    clearRawApnsTokenOnMisconfigured: () => {},
    scheduleRetry: () => {},
    emitPushUnregistered: () => {},
    reportPushUnregistrationBlocked: () => {},
    auditStaleCallback: () => {},
    beginInvocation: assign({ invocationSequence: ({ context }) => context.invocationSequence + 1, correlationId: ({ context }) => `registration-${context.installationId}-${context.invocationSequence + 1}` }),
    setPermission: assign({ authorizationStatus: ({ event }) => event.type === 'PERMISSION_STATUS_RESOLVED' ? event.status : event.type === 'PERMISSION_GRANTED' ? event.status : event.type === 'PERMISSION_DENIED' ? 'denied' : 'notDetermined', correlationId: ({ event }) => 'correlationId' in event ? event.correlationId : null }),
    setToken: assign({ tokenFingerprint: ({ event }) => event.type === 'APNS_DID_REGISTER' ? event.tokenFingerprint : null }),
    setAuth: assign({ authSessionId: ({ event }) => event.type === 'AUTH_BECAME_AVAILABLE' ? event.authSessionId : null, hasUsableJwt: ({ event }) => event.type === 'AUTH_BECAME_AVAILABLE' }),
    clearAuth: assign({ authSessionId: null, hasUsableJwt: false }),
    setRegistration: assign({ backendRegistrationId: ({ event }) => event.type === 'BACKEND_REGISTER_SUCCEEDED' ? event.backendRegistrationId : null, attempt: 0, lastErrorClass: null, nextRetryAt: null }),
    clearRegistration: assign({ backendRegistrationId: null, tokenFingerprint: null, attempt: 0, lastErrorClass: null, nextRetryAt: null }),
    markLogout: assign({ logoutRequested: true }),
    startLogoutFlight: assign({ logoutRequested: true, attempt: 0, lastErrorClass: null, nextRetryAt: null, resumeState: 'unregistering' }),
    startRecoverableLogoutFlight: assign({ logoutRequested: true, attempt: 0, lastErrorClass: 'authentication', nextRetryAt: null, resumeState: 'unregistering' }),
    captureAuthenticationFailure: assign({ lastErrorClass: 'authentication', nextRetryAt: null }),
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
  context: ({ input }) => ({ installationId: input.installationId, topic: input.topic.trim(), environment: input.environment, authorizationStatus: input.authorizationStatus ?? 'notDetermined', authSessionId: input.authSessionId ?? null, hasUsableJwt: input.hasUsableJwt ?? false, tokenFingerprint: null, backendRegistrationId: input.backendRegistrationId ?? null, attempt: 0, maxAttempts: input.maxAttempts ?? 3, nextRetryAt: null, lastErrorClass: null, resumeState: 'checkingPermission', correlationId: null, invocationSequence: 0, logoutRequested: false }),
  states: {
    checkingPermission: { entry: ['beginInvocation', 'readPermissionStatus'], on: {
      PERMISSION_STATUS_RESOLVED: [
        { guard: ({ context, event }) => isCorrelated(context, event) && permissionAllowsRemoteRegistration(event.status), target: 'registeringApns', actions: 'setPermission' },
        { guard: ({ context, event }) => isCorrelated(context, event) && event.status === 'notDetermined', target: 'notDetermined', actions: 'setPermission' },
        { guard: ({ context, event }) => isCorrelated(context, event) && event.status === 'denied', target: 'denied', actions: 'setPermission' },
        { actions: 'auditStaleCallback' },
      ],
      PERMISSION_STATUS_FAILED: [{ guard: 'correlatedTransientFailure', target: 'retry', actions: [...failureActions, 'resumeCheckingPermission'] }, { guard: 'correlatedConfigurationFailure', target: 'misconfigured', actions: 'captureFailure' }, { actions: 'auditStaleCallback' }],
      LOGOUT_REQUESTED: [{ guard: 'hasAuth', target: 'unregistering', actions: 'startLogoutFlight' }, { target: 'retry', actions: 'startRecoverableLogoutFlight' }],
      CONFIGURATION_INVALID: { target: 'misconfigured', actions: 'captureFailure' },
    } },
    notDetermined: { on: {
      USER_REQUESTED_ENABLE: 'requestingPermission', USER_CANCELLED: 'cancelled', APP_BECAME_ACTIVE: 'checkingPermission',
      LOGOUT_REQUESTED: [{ guard: 'hasAuth', target: 'unregistering', actions: 'startLogoutFlight' }, { target: 'retry', actions: 'startRecoverableLogoutFlight' }],
    } },
    requestingPermission: { entry: ['beginInvocation', 'requestAuthorization'], on: {
      PERMISSION_GRANTED: { guard: 'correlated', target: 'registeringApns', actions: 'setPermission' },
      PERMISSION_DENIED: { guard: 'correlated', target: 'denied', actions: 'setPermission' },
      PERMISSION_REQUEST_FAILED: [{ guard: 'correlatedTransientFailure', target: 'retry', actions: [...failureActions, 'resumeRequestingPermission'] }, { guard: 'correlatedConfigurationFailure', target: 'misconfigured', actions: 'captureFailure' }, { actions: 'auditStaleCallback' }],
      LOGOUT_REQUESTED: [{ guard: 'hasAuth', target: 'unregistering', actions: 'startLogoutFlight' }, { target: 'retry', actions: 'startRecoverableLogoutFlight' }],
      CONFIGURATION_INVALID: { target: 'misconfigured', actions: 'captureFailure' },
    } },
    denied: { on: {
      USER_OPENED_SETTINGS: { actions: 'openSystemSettings' }, APP_BECAME_ACTIVE: 'checkingPermission', USER_CANCELLED: 'cancelled',
      LOGOUT_REQUESTED: [{ guard: 'hasAuth', target: 'unregistering', actions: 'startLogoutFlight' }, { target: 'retry', actions: 'startRecoverableLogoutFlight' }],
    } },
    registeringApns: { entry: ['beginInvocation', 'registerForRemoteNotifications'], on: {
      APNS_DID_REGISTER: [{ guard: ({ context, event }) => isCorrelated(context, event) && hasUsableAuthentication(context), target: 'registeringBackend', actions: ['replaceRawApnsToken', 'setToken'] }, { guard: 'correlated', target: 'awaitingAuthentication', actions: ['replaceRawApnsToken', 'setToken'] }, { actions: 'auditStaleCallback' }],
      APNS_DID_FAIL: [{ guard: 'correlatedTransientFailure', target: 'retry', actions: [...failureActions, 'resumeRegisteringApns'] }, { guard: 'correlatedConfigurationFailure', target: 'misconfigured', actions: 'captureFailure' }, { actions: 'auditStaleCallback' }],
      LOGOUT_REQUESTED: [{ guard: 'hasAuth', target: 'unregistering', actions: 'startLogoutFlight' }, { target: 'retry', actions: 'startRecoverableLogoutFlight' }],
      CONFIGURATION_INVALID: { target: 'misconfigured', actions: 'captureFailure' },
    } },
    awaitingAuthentication: { on: {
      AUTH_BECAME_AVAILABLE: { target: 'registeringBackend', actions: 'setAuth' },
      APNS_DID_REGISTER: { guard: 'correlated', actions: ['replaceRawApnsToken', 'setToken'] }, USER_CANCELLED: 'cancelled',
      LOGOUT_REQUESTED: [{ guard: 'hasAuth', target: 'unregistering', actions: 'startLogoutFlight' }, { target: 'retry', actions: 'startRecoverableLogoutFlight' }],
    } },
    registeringBackend: { entry: ['beginInvocation', 'withRawApnsTokenForBackendRegistration', 'registerInstallationWithBackend'], on: {
      RAW_APNS_TOKEN_UNAVAILABLE: [{ guard: 'correlated', target: 'misconfigured', actions: 'captureFailure' }, { actions: 'auditStaleCallback' }],
      BACKEND_REGISTER_SUCCEEDED: [{ guard: 'correlated', target: 'registered', actions: ['setRegistration', 'clearRawApnsTokenAfterBackendAcknowledgement'] }, { actions: 'auditStaleCallback' }],
      BACKEND_REGISTER_FAILED: [{ guard: 'correlatedAuthenticationFailure', target: 'awaitingAuthentication', actions: 'captureFailure' }, { guard: 'correlatedTransientFailure', target: 'retry', actions: [...failureActions, 'resumeRegisteringBackend'] }, { guard: 'correlatedConfigurationFailure', target: 'misconfigured', actions: 'captureFailure' }, { actions: 'auditStaleCallback' }],
      AUTH_BECAME_UNAVAILABLE: { target: 'awaitingAuthentication', actions: ['clearAuth', 'captureAuthenticationFailure'] },
      LOGOUT_REQUESTED: [{ guard: 'hasAuth', target: 'unregistering', actions: 'startLogoutFlight' }, { target: 'retry', actions: 'startRecoverableLogoutFlight' }],
      CONFIGURATION_INVALID: { target: 'misconfigured', actions: 'captureFailure' },
    } },
    retry: { entry: 'scheduleRetry', on: {
      RETRY_DUE: [
        { guard: ({ context }) => context.logoutRequested && context.resumeState === 'unregistering' && !hasUsableAuthentication(context), actions: 'captureAuthenticationFailure' },
        { guard: ({ context }) => context.attempt < context.maxAttempts && context.resumeState === 'unregistering' && hasUsableAuthentication(context), target: 'unregistering' },
        { guard: ({ context }) => context.attempt < context.maxAttempts && context.resumeState === 'registeringBackend' && hasUsableAuthentication(context), target: 'registeringBackend' },
        { guard: ({ context }) => context.attempt < context.maxAttempts && context.resumeState === 'registeringApns', target: 'registeringApns' },
        { guard: ({ context }) => context.attempt < context.maxAttempts && context.resumeState === 'requestingPermission', target: 'requestingPermission' },
        { guard: 'retryBudgetAvailable', target: 'checkingPermission' },
        { target: 'misconfigured' },
      ],
      AUTH_BECAME_AVAILABLE: [{ guard: ({ context }) => context.logoutRequested && context.resumeState === 'unregistering', target: 'unregistering', actions: 'setAuth' }, { actions: 'setAuth' }],
      AUTH_BECAME_UNAVAILABLE: { actions: ['clearAuth', 'captureAuthenticationFailure'] },
      APP_BECAME_ACTIVE: { guard: ({ context }) => !context.logoutRequested, target: 'checkingPermission' },
      USER_CANCELLED: { guard: ({ context }) => !context.logoutRequested, target: 'cancelled' },
      LOGOUT_REQUESTED: [{ guard: 'logoutAlreadyRequested' }, { guard: 'hasAuth', target: 'unregistering', actions: 'startLogoutFlight' }, { actions: 'startRecoverableLogoutFlight' }],
      CONFIGURATION_INVALID: { target: 'misconfigured', actions: 'captureFailure' },
    } },
    registered: { on: {
      APNS_DID_REGISTER: { guard: 'correlated', target: 'registeringBackend', actions: ['replaceRawApnsToken', 'setToken'] },
      AUTH_BECAME_UNAVAILABLE: { actions: ['clearAuth', 'captureAuthenticationFailure'] },
      LOGOUT_REQUESTED: [{ guard: 'hasAuth', target: 'unregistering', actions: 'startLogoutFlight' }, { target: 'retry', actions: 'startRecoverableLogoutFlight' }],
      APP_BECAME_ACTIVE: 'checkingPermission',
    } },
    unregistering: { entry: ['beginInvocation', 'unregisterInstallationWithBackend'], on: {
      BACKEND_UNREGISTER_SUCCEEDED: [{ guard: 'correlated', target: 'unregistered', actions: 'clearRegistration' }, { actions: 'auditStaleCallback' }],
      BACKEND_UNREGISTER_FAILED: [
        { guard: 'correlatedTransientFailure', target: 'retry', actions: [...failureActions, 'resumeUnregistering'] },
        { guard: 'correlatedAuthenticationFailure', target: 'retry', actions: [...failureActions, 'resumeUnregistering'] },
        { guard: 'correlatedConfigurationFailure', target: 'misconfigured', actions: 'captureFailure' },
        { actions: 'auditStaleCallback' },
      ],
      LOGOUT_REQUESTED: { guard: 'logoutAlreadyRequested' },
      CONFIGURATION_INVALID: { target: 'misconfigured', actions: 'captureFailure' },
    } },
    unregistered: { type: 'final', entry: ['clearRawApnsTokenOnUnregistered', 'emitPushUnregistered'] },
    cancelled: { type: 'final', entry: 'clearRawApnsTokenOnCancelled' },
    misconfigured: { type: 'final', entry: ['clearRawApnsTokenOnMisconfigured', 'reportPushUnregistrationBlocked'] },
  },
})

export const createIosNotificationRegistrationActor = (input: IosNotificationRegistrationInput) => {
  const scope = validateRegistrationScope(input.topic, input.environment)
  if (scope === null) return null

  return createActor(iosNotificationRegistrationMachine, {
    input: { ...input, ...scope },
  }).start()
}

export const registrationInvariants = [
  'registration scope requires a non-empty trimmed topic and an explicit sandbox or production environment',
  'permission prompt only follows USER_REQUESTED_ENABLE from notDetermined',
  'permission APNs and backend effects and callbacks use the active correlationId',
  'backend calls require usable authentication',
  'registered requires correlated BACKEND_REGISTER_SUCCEEDED acknowledgement',
  'logout from every non-terminal registration state targets the known registration or stable installation fallback',
  'logout without usable authentication remains explicitly recoverable and never emits PUSH_UNREGISTERED',
  'logout retains credentials through transient authentication and configuration failures until unregistered',
  'only unregistered emits PUSH_UNREGISTERED; misconfigured returns a blocked result without clearing credentials',
  'one registration actor owns each logout transition while zero or more observers receive the same terminal result',
  'raw APNs tokens remain in private port custody and raw tokens and JWTs never enter observable context or snapshots',
  'stale callbacks never transition state',
  'unregistration targets one stable installation',
  'no state transition depends on free text or an LLM',
] as const
