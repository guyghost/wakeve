import { assign, createActor, emit, setup } from 'xstate'

export type LegacyCompatibilityOperation = 'register' | 'unregister'
export type LegacyCompatibilityClientGeneration = 'N' | 'N_MINUS_1'
export type LegacyCompatibilityFailure =
  | 'unavailable'
  | 'transient'
  | 'configuration'
  | 'identityConflict'
export type CompatibilityWriteStatus = 'notRequired' | 'pending' | 'applied'
export type CompatibilityResumeStep = 'legacy' | 'v2'
export type CompatibilityResponseDisposition =
  | 'none'
  | 'reconciliationAccepted'
  | 'convergedSuccess'
  | 'blockedFailure'
export type LegacyCompatibilityEffectCheckpoint =
  | 'persistPendingReconciliation'
  | 'writeLegacyStore'
  | 'writeV2RegistrationStore'
  | 'persistRetrySchedule'
  | 'persistConvergence'
  | 'persistBlockedTerminal'

export interface LegacyCompatibilityRecoveryLease {
  leaseId: string
  holderId: string
  version: number
  fencingToken: number
  expiresAtEpochSeconds: number
  effectId: string
  effectCheckpoint: LegacyCompatibilityEffectCheckpoint
  checkpointRevision: number
  effectEmitted: boolean
}

export interface LegacyCompatibilityEffectReference {
  expectedEffectId: string
  checkpointRevision: number
  fencingToken: number
}

export interface LegacyCompatibilityInput {
  sagaId: string
  operation: LegacyCompatibilityOperation
  clientGeneration: LegacyCompatibilityClientGeneration
  authenticatedUserId: string
  platform: 'ios'
  /** Sanitized digest of the immutable row key. The row key itself never enters the model. */
  legacyPrimaryKeyFingerprint: string | null
  /** Opaque HMAC-derived identity, never an unhashed legacy primary key. */
  legacyInstallationId: string | null
  /** Opaque HMAC-derived identity, never an unhashed legacy primary key. */
  legacyRegistrationId: string | null
  targetInstallationId: string
  targetRegistrationId: string | null
  /** One-way token fingerprint only. A raw token is forbidden from model context. */
  tokenFingerprint: string | null
  /** Durable monotonic generation allocated when the desired compatibility state changes. */
  compatibilityGeneration: number
  maxAttemptsPerStore: number
  /** Explicit deterministic model clock seed. Runtime wall-clock reads are forbidden. */
  initialNowEpochSeconds: number
}

export interface LegacyCompatibilityContext extends LegacyCompatibilityInput {
  requestKey: string
  reconciliationId: string | null
  reconciliationStatus: 'notPersisted' | 'pending' | 'converged' | 'blocked'
  legacyWriteStatus: CompatibilityWriteStatus
  v2WriteStatus: CompatibilityWriteStatus
  v2TargetKind:
    | 'legacyDeterministicInstallationOnly'
    | 'exactRegistration'
    | 'exactInstallation'
  legacyAttempt: number
  v2Attempt: number
  resumeStep: CompatibilityResumeStep | null
  nextRetryAtEpochSeconds: number | null
  lastFailure: LegacyCompatibilityFailure | null
  duplicateCount: number
  effectCheckpoint: LegacyCompatibilityEffectCheckpoint | null
  checkpointRevision: number
  authorityFencingToken: number
  lastRecoveryLeaseVersion: number
  lastRecoveryFencingToken: number
  recoveryLease: LegacyCompatibilityRecoveryLease | null
  logicalNowEpochSeconds: number
  clockRevision: number
  recoveryCount: number
  responseDisposition: CompatibilityResponseDisposition
}

export interface LegacyCompatibilityEffectRequested {
  type: 'LEGACY_COMPATIBILITY_EFFECT_REQUESTED'
  sagaId: string
  requestKey: string
  effectId: string
  effectCheckpoint: LegacyCompatibilityEffectCheckpoint
  checkpointRevision: number
  fencingToken: number
  reconciliationId: string | null
  recoveryLeaseId: string | null
  recoveryLeaseHolderId: string | null
  cause: 'stateTransition' | 'recovery'
}

export type LegacyCompatibilityEvent =
  | ({ type: 'INTENT_PERSISTED'; sagaId: string; reconciliationId: string } & LegacyCompatibilityEffectReference)
  | ({ type: 'LEGACY_WRITE_SUCCEEDED'; sagaId: string; outcome: 'applied' | 'alreadyApplied' } & LegacyCompatibilityEffectReference)
  | ({ type: 'V2_WRITE_SUCCEEDED'; sagaId: string; outcome: 'applied' | 'alreadyApplied' } & LegacyCompatibilityEffectReference)
  | ({ type: 'LEGACY_WRITE_FAILED'; sagaId: string; failure: LegacyCompatibilityFailure } & LegacyCompatibilityEffectReference)
  | ({ type: 'V2_WRITE_FAILED'; sagaId: string; failure: LegacyCompatibilityFailure } & LegacyCompatibilityEffectReference)
  | ({ type: 'RETRY_RECORDED'; sagaId: string; nextRetryAtEpochSeconds: number } & LegacyCompatibilityEffectReference)
  | { type: 'RETRY_DUE'; sagaId: string; retryScheduleRevision: number }
  | ({ type: 'CONVERGENCE_RECORDED'; sagaId: string } & LegacyCompatibilityEffectReference)
  | ({ type: 'BLOCK_RECORDED'; sagaId: string } & LegacyCompatibilityEffectReference)
  | { type: 'CLOCK_ADVANCED'; sagaId: string; clockRevision: number; nowEpochSeconds: number }
  | {
      type: 'RECOVERY_LEASE_ACQUIRED'
      sagaId: string
      expectedEffectId: string
      effectCheckpoint: LegacyCompatibilityEffectCheckpoint
      checkpointRevision: number
      leaseId: string
      holderId: string
      version: number
      fencingToken: number
      expiresAtEpochSeconds: number
    }
  | {
      type: 'RECOVERY_REQUESTED'
      sagaId: string
      expectedEffectId: string
      effectCheckpoint: LegacyCompatibilityEffectCheckpoint
      checkpointRevision: number
      leaseId: string
      holderId: string
      version: number
      fencingToken: number
    }
  | { type: 'DUPLICATE_REQUEST_RECEIVED'; requestKey: string }

export const legacyCompatibilityRequestKey = (
  operation: LegacyCompatibilityOperation,
  authenticatedUserId: string,
  stableTargetIdentity: string,
  tokenFingerprint: string | null,
  compatibilityGeneration: number,
): string => JSON.stringify([
  'legacy-notification-registration-compatibility',
  'v2',
  operation,
  authenticatedUserId,
  compatibilityGeneration,
  stableTargetIdentity,
  tokenFingerprint,
])

const legacyCompatibilityEffectId = (
  context: LegacyCompatibilityContext,
  effectCheckpoint: LegacyCompatibilityEffectCheckpoint,
): string => JSON.stringify([
  'legacy-notification-registration-compatibility-effect',
  'v1',
  context.requestKey,
  effectCheckpoint,
  context.checkpointRevision,
  context.resumeStep,
  context.legacyAttempt,
  context.v2Attempt,
])

export const legacyCompatibilityExpectedEffectReference = (
  context: LegacyCompatibilityContext,
): LegacyCompatibilityEffectReference | null => context.effectCheckpoint === null
  ? null
  : {
      expectedEffectId: legacyCompatibilityEffectId(context, context.effectCheckpoint),
      checkpointRevision: context.checkpointRevision,
      fencingToken: context.authorityFencingToken,
    }

export const legacyCompatibilityBackoffSeconds = (
  attempt: number,
  random = 0.5,
  baseSeconds = 1,
  capSeconds = 300,
): number => {
  const boundedCapSeconds = Number.isFinite(capSeconds)
    ? Math.min(300, Math.max(1, Math.floor(capSeconds)))
    : 300
  const boundedBaseSeconds = Number.isFinite(baseSeconds)
    ? Math.min(boundedCapSeconds, Math.max(1, Math.floor(baseSeconds)))
    : 1
  const boundedAttempt = Number.isNaN(attempt)
    ? 1
    : Math.max(1, Math.floor(attempt))
  const exponentAtCap = Math.max(
    0,
    Math.ceil(Math.log2(boundedCapSeconds / boundedBaseSeconds)),
  )
  const boundedExponent = Math.min(exponentAtCap, boundedAttempt - 1)
  const fullJitterWindowSeconds = Math.min(
    boundedCapSeconds,
    boundedBaseSeconds * 2 ** boundedExponent,
  )
  const boundedSample = Number.isNaN(random)
    ? 0
    : Math.max(0, Math.min(1, random))

  // A persisted retry deadline must be strictly greater than the logical clock. Full jitter's
  // mathematical zero is therefore represented by the smallest valid durable delay: one second.
  return Math.max(1, Math.floor(fullJitterWindowSeconds * boundedSample))
}

const validatedInput = (
  input: LegacyCompatibilityInput,
): LegacyCompatibilityInput | null => {
  if (input.sagaId.trim().length === 0 || input.authenticatedUserId.trim().length === 0) return null
  if (input.platform !== 'ios' || input.targetInstallationId.trim().length === 0) return null
  if (!Number.isInteger(input.maxAttemptsPerStore) || input.maxAttemptsPerStore < 1) return null
  if (!Number.isInteger(input.compatibilityGeneration) || input.compatibilityGeneration < 1) return null
  if (!Number.isInteger(input.initialNowEpochSeconds) || input.initialNowEpochSeconds < 0) return null
  if (input.operation === 'register' && input.tokenFingerprint?.trim().length === 0) return null
  if (input.operation === 'register' && input.tokenFingerprint === null) return null
  if (input.operation === 'register' && input.targetRegistrationId !== null) return null
  if (input.operation === 'unregister' && input.tokenFingerprint !== null) return null
  if (input.targetRegistrationId !== null && input.targetRegistrationId.trim().length === 0) return null

  if (input.clientGeneration === 'N_MINUS_1') {
    if (!input.legacyPrimaryKeyFingerprint?.trim()) return null
    if (!input.legacyInstallationId?.trim()) return null
    if (!input.legacyRegistrationId?.trim()) return null
    if (input.targetInstallationId !== input.legacyInstallationId) return null
    if (input.targetRegistrationId !== null) return null
  }

  if (input.clientGeneration === 'N') {
    if (input.legacyPrimaryKeyFingerprint !== null) return null
    if (input.legacyInstallationId !== null) return null
    if (input.legacyRegistrationId !== null) return null
  }
  return {
    ...input,
    sagaId: input.sagaId.trim(),
    authenticatedUserId: input.authenticatedUserId.trim(),
    tokenFingerprint: input.tokenFingerprint?.trim() ?? null,
  }
}

const eventMatchesSaga = (
  context: LegacyCompatibilityContext,
  event: LegacyCompatibilityEvent,
) => !('sagaId' in event) || event.sagaId === context.sagaId

const retryable = (failure: LegacyCompatibilityFailure) =>
  failure === 'unavailable' || failure === 'transient'

const eventAcknowledgesCurrentEffect = (
  context: LegacyCompatibilityContext,
  event: LegacyCompatibilityEvent,
): boolean => {
  if (!('expectedEffectId' in event) || !('fencingToken' in event)) return false
  const expected = legacyCompatibilityExpectedEffectReference(context)
  if (expected === null) return false
  if (
    event.expectedEffectId !== expected.expectedEffectId ||
    event.checkpointRevision !== expected.checkpointRevision ||
    event.fencingToken !== expected.fencingToken
  ) return false
  const lease = context.recoveryLease
  return lease === null || (
    lease.fencingToken === expected.fencingToken &&
    context.logicalNowEpochSeconds < lease.expiresAtEpochSeconds
  )
}

const recoveryLeaseCanBeRecorded = (
  context: LegacyCompatibilityContext,
  event: LegacyCompatibilityEvent,
): boolean => {
  if (event.type !== 'RECOVERY_LEASE_ACQUIRED') return false
  const expected = legacyCompatibilityExpectedEffectReference(context)
  if (expected === null) return false
  const previousLeaseExpired = context.recoveryLease === null ||
    context.logicalNowEpochSeconds >= context.recoveryLease.expiresAtEpochSeconds
  return eventMatchesSaga(context, event) &&
    event.effectCheckpoint === context.effectCheckpoint &&
    event.expectedEffectId === expected.expectedEffectId &&
    event.checkpointRevision === expected.checkpointRevision &&
    event.leaseId.trim().length > 0 &&
    event.holderId.trim().length > 0 &&
    Number.isInteger(event.version) &&
    event.version > context.lastRecoveryLeaseVersion &&
    Number.isInteger(event.fencingToken) &&
    event.fencingToken > context.lastRecoveryFencingToken &&
    event.fencingToken > context.authorityFencingToken &&
    Number.isInteger(event.expiresAtEpochSeconds) &&
    event.expiresAtEpochSeconds > context.logicalNowEpochSeconds &&
    previousLeaseExpired
}

const recoveryRequestOwnsCurrentLease = (
  context: LegacyCompatibilityContext,
  event: LegacyCompatibilityEvent,
): boolean => {
  if (event.type !== 'RECOVERY_REQUESTED') return false
  const lease = context.recoveryLease
  if (lease === null || lease.effectEmitted) return false
  return eventMatchesSaga(context, event) &&
    event.expectedEffectId === lease.effectId &&
    event.effectCheckpoint === lease.effectCheckpoint &&
    event.checkpointRevision === lease.checkpointRevision &&
    event.leaseId === lease.leaseId &&
    event.holderId === lease.holderId &&
    event.version === lease.version &&
    event.fencingToken === lease.fencingToken &&
    context.logicalNowEpochSeconds < lease.expiresAtEpochSeconds
}

const nextEffectAfterIntent = (
  context: LegacyCompatibilityContext,
): LegacyCompatibilityEffectCheckpoint =>
  context.operation === 'register' && context.clientGeneration === 'N_MINUS_1'
    ? 'writeLegacyStore'
    : 'writeV2RegistrationStore'

const nextEffectAfterLegacyWrite = (
  context: LegacyCompatibilityContext,
): LegacyCompatibilityEffectCheckpoint =>
  context.operation === 'register'
    ? 'writeV2RegistrationStore'
    : 'persistConvergence'

const nextEffectAfterV2Write = (
  context: LegacyCompatibilityContext,
): LegacyCompatibilityEffectCheckpoint =>
  context.operation === 'unregister' && context.clientGeneration === 'N_MINUS_1'
    ? 'writeLegacyStore'
    : 'persistConvergence'

const writeEffectForResumeStep = (
  context: LegacyCompatibilityContext,
): LegacyCompatibilityEffectCheckpoint | null =>
  context.resumeStep === 'legacy'
    ? 'writeLegacyStore'
    : context.resumeStep === 'v2'
      ? 'writeV2RegistrationStore'
      : null

const effectRequested = (
  context: LegacyCompatibilityContext,
  event: LegacyCompatibilityEvent,
): LegacyCompatibilityEffectRequested => {
  if (context.effectCheckpoint === null) {
    throw new Error('A compatibility effect cannot be emitted without a durable checkpoint')
  }
  return {
    type: 'LEGACY_COMPATIBILITY_EFFECT_REQUESTED',
    sagaId: context.sagaId,
    requestKey: context.requestKey,
    effectId: legacyCompatibilityEffectId(context, context.effectCheckpoint),
    effectCheckpoint: context.effectCheckpoint,
    checkpointRevision: context.checkpointRevision,
    fencingToken: context.authorityFencingToken,
    reconciliationId: context.reconciliationId,
    recoveryLeaseId: event.type === 'RECOVERY_REQUESTED' ? event.leaseId : null,
    recoveryLeaseHolderId: event.type === 'RECOVERY_REQUESTED' ? event.holderId : null,
    cause: event.type === 'RECOVERY_REQUESTED' ? 'recovery' : 'stateTransition',
  }
}

export const legacyCompatibilityPortContract = {
  transitionAuthority: 'legacyNotificationRegistrationCompatibility-actor-only',
  transactionBoundary: 'one-durable-store-commit-at-a-time',
  pendingReconciliation: {
    owner: 'backend-compatibility-saga-store',
    requiredBeforeStoreWrites: true,
    restartSourceOfTruth: 'persisted-machine-snapshot-and-saga-id',
  },
  idempotency: {
    requestKeyEncoding: 'canonical-json-typed-tuple-v2',
    fields: ['operation', 'authenticated-subject', 'compatibility-generation', 'stable-target', 'token-fingerprint-or-null'],
  },
  recoveryWorker: {
    restoredEntriesReplayAutomatically: false,
    leaseRequired: true,
    leaseAcquisition: 'durable-compare-and-set-before-RECOVERY_LEASE_ACQUIRED',
    leaseAuthority: 'opaque-lease-id-holder-version-fencing-token-checkpoint-and-expiry',
    clockAuthority: 'persisted-logicalNowEpochSeconds-advanced-only-by-correlated-monotone-CLOCK_ADVANCED-revision',
    handshake: 'start-restored-actor-record-durable-lease-then-send-fenced-RECOVERY_REQUESTED',
    checkpointSource: 'persisted-effectCheckpoint',
    result: 'machine-re-emits-the-same-idempotent-effectId',
    concurrentEmission: 'at-most-once-per-recorded-lease',
  },
  effectAcknowledgement: {
    requiredReference: ['expected-effect-id', 'checkpoint-revision', 'authority-fencing-token'],
    staleResult: 'audit-only-no-transition-no-attempt-or-schedule-mutation',
  },
  registerOrder: {
    N_MINUS_1: ['legacy-sqldelight', 'backend-device-registration'],
    N: ['backend-device-registration'],
  },
  unregisterOrder: {
    N_MINUS_1: ['backend-device-registration', 'legacy-sqldelight'],
    N: ['backend-device-registration'],
  },
  response: {
    successTrueOnlyAfter: 'durable-convergence',
    acceptedBeforeConvergenceOnlyAfter: 'durable-reconciliation-enqueue',
    acceptedResponse: '202-with-explicit-accepted-not-success',
  },
  compensationPolicy: 'forward-reconciliation-only',
  v2DeletionScope: 'exact-registration-or-legacy-deterministic-installation-only',
  legacyIdentity: {
    derivation: 'HMAC-SHA-256-with-configured-stable-migration-key',
    input: 'immutable-legacy-primary-key',
    rawIdentityInSnapshot: false,
  },
  retryDomains: ['legacy-store', 'v2-registration-store'],
  retryClock: {
    scheduleRule: 'nextRetryAtEpochSeconds-strictly-greater-than-logicalNowEpochSeconds',
    dueEventCarriesTime: false,
    eligibility: 'logicalNowEpochSeconds-at-or-after-durable-deadline-and-schedule-revision-matches',
  },
} as const

export const legacyCompatibilityInvariants = [
  'a pending reconciliation intent is durable before the first datastore write',
  'no cross-datastore transaction is claimed; each store commit is an independent durable step',
  'legacy identity is derived from the immutable legacy primary key by a configured stable HMAC and raw identity material never enters the saga snapshot',
  'N clients mutate only the exact v2 registration or installation and never the lossy legacy user-platform row',
  'N-1 registration writes legacy first then the HMAC-derived v2 compatibility installation',
  'N-1 unregistration closes the HMAC-derived v2 compatibility installation before deleting its legacy row',
  'legacy compatibility unregistration never enumerates or removes another v2 installation',
  'retries retain the saga id and request key and advance only the failed store attempt counter',
  'a duplicate request coalesces onto the same persisted saga and terminal result',
  'success true is emitted only after durable convergence; pre-convergence acknowledgement is explicit reconciliation acceptance',
  'crash recovery resumes from the persisted step and never reinterprets an earlier durable outcome',
  'restored XState entry actions are never assumed to replay; a worker first records a durable lease then requests recovery for the exact persisted effect checkpoint',
  'recovery authority requires the stored opaque lease id, holder, version, fencing token, checkpoint and deterministic non-expired logical time',
  'one recorded lease emits its checkpoint effect at most once; foreign, expired and stale-fencing recovery requests cannot cause an effect',
  'every effect acknowledgement matches the current effect id, checkpoint revision and authority fencing token before it may mutate state, attempts or retry schedule',
  'retry due events carry no caller time; only the persisted monotone logical clock can cross a strictly future durable retry deadline',
  'the request key is a canonical typed tuple, so delimiter-bearing subject and target values cannot collide',
  'compensation is forward-only reconciliation and never destructive rollback of unrelated v2 registrations',
  'cutover and rollback change read authority only at a drained recorded checkpoint and preserve all v2 rows',
  'no transition depends on free text or an LLM',
] as const

export const legacyNotificationCompatibilityMachine = setup({
  types: {
    context: {} as LegacyCompatibilityContext,
    events: {} as LegacyCompatibilityEvent,
    input: {} as LegacyCompatibilityInput,
    emitted: {} as LegacyCompatibilityEffectRequested,
  },
  guards: {
    effectAckIsCurrent: ({ context, event }) =>
      eventMatchesSaga(context, event) && eventAcknowledgesCurrentEffect(context, event),
    duplicateMatches: ({ context, event }) =>
      event.type === 'DUPLICATE_REQUEST_RECEIVED' && event.requestKey === context.requestKey,
    intentPersistenceIsValid: ({ context, event }) =>
      event.type === 'INTENT_PERSISTED' &&
      eventMatchesSaga(context, event) &&
      eventAcknowledgesCurrentEffect(context, event) &&
      event.reconciliationId.trim().length > 0,
    legacyFailureCanRetry: ({ context, event }) =>
      event.type === 'LEGACY_WRITE_FAILED' &&
      eventMatchesSaga(context, event) &&
      eventAcknowledgesCurrentEffect(context, event) &&
      retryable(event.failure) &&
      context.legacyAttempt + 1 < context.maxAttemptsPerStore,
    v2FailureCanRetry: ({ context, event }) =>
      event.type === 'V2_WRITE_FAILED' &&
      eventMatchesSaga(context, event) &&
      eventAcknowledgesCurrentEffect(context, event) &&
      retryable(event.failure) &&
      context.v2Attempt + 1 < context.maxAttemptsPerStore,
    retryScheduleIsValid: ({ context, event }) =>
      event.type === 'RETRY_RECORDED' &&
      eventMatchesSaga(context, event) &&
      eventAcknowledgesCurrentEffect(context, event) &&
      Number.isInteger(event.nextRetryAtEpochSeconds) &&
      event.nextRetryAtEpochSeconds > context.logicalNowEpochSeconds,
    requiredWritesAreApplied: ({ context, event }) =>
      event.type === 'CONVERGENCE_RECORDED' &&
      eventMatchesSaga(context, event) &&
      eventAcknowledgesCurrentEffect(context, event) &&
      context.v2WriteStatus === 'applied' &&
      (context.legacyWriteStatus === 'applied' || context.legacyWriteStatus === 'notRequired'),
    recoveryLeaseCanBeRecorded: ({ context, event }) =>
      recoveryLeaseCanBeRecorded(context, event),
    recoveryMatchesCheckpoint: ({ context, event }) =>
      recoveryRequestOwnsCurrentLease(context, event),
    clockIsMonotone: ({ context, event }) =>
      event.type === 'CLOCK_ADVANCED' &&
      eventMatchesSaga(context, event) &&
      event.clockRevision === context.clockRevision + 1 &&
      Number.isInteger(event.nowEpochSeconds) &&
      event.nowEpochSeconds >= context.logicalNowEpochSeconds,
  },
  actions: {
    emitCurrentEffect: emit(({ context, event }) => effectRequested(context, event)),
    persistPendingReconciliation: emit(({ context, event }) => effectRequested(context, event)),
    writeLegacyStoreIdempotently: emit(({ context, event }) => effectRequested(context, event)),
    writeV2RegistrationStoreIdempotently: emit(({ context, event }) => effectRequested(context, event)),
    persistRetrySchedule: emit(({ context, event }) => effectRequested(context, event)),
    persistConvergence: emit(({ context, event }) => effectRequested(context, event)),
    persistBlockedTerminal: emit(({ context, event }) => effectRequested(context, event)),
    auditStaleOrConflictingCallback: () => {},
    acceptIntent: assign({
      reconciliationId: ({ event }) => event.type === 'INTENT_PERSISTED' ? event.reconciliationId : null,
      reconciliationStatus: 'pending',
      responseDisposition: 'reconciliationAccepted',
      effectCheckpoint: ({ context }) => nextEffectAfterIntent(context),
      checkpointRevision: ({ context }) => context.checkpointRevision + 1,
      authorityFencingToken: ({ context }) => context.authorityFencingToken + 1,
      recoveryLease: null,
    }),
    markLegacyApplied: assign({
      legacyWriteStatus: 'applied',
      lastFailure: null,
      nextRetryAtEpochSeconds: null,
      resumeStep: null,
      effectCheckpoint: ({ context }) => nextEffectAfterLegacyWrite(context),
      checkpointRevision: ({ context }) => context.checkpointRevision + 1,
      authorityFencingToken: ({ context }) => context.authorityFencingToken + 1,
      recoveryLease: null,
    }),
    markV2Applied: assign({
      v2WriteStatus: 'applied',
      lastFailure: null,
      nextRetryAtEpochSeconds: null,
      resumeStep: null,
      effectCheckpoint: ({ context }) => nextEffectAfterV2Write(context),
      checkpointRevision: ({ context }) => context.checkpointRevision + 1,
      authorityFencingToken: ({ context }) => context.authorityFencingToken + 1,
      recoveryLease: null,
    }),
    captureLegacyRetry: assign({
      legacyAttempt: ({ context }) => context.legacyAttempt + 1,
      lastFailure: ({ event }) => event.type === 'LEGACY_WRITE_FAILED' ? event.failure : null,
      resumeStep: 'legacy',
      nextRetryAtEpochSeconds: null,
      effectCheckpoint: 'persistRetrySchedule',
      checkpointRevision: ({ context }) => context.checkpointRevision + 1,
      authorityFencingToken: ({ context }) => context.authorityFencingToken + 1,
      recoveryLease: null,
    }),
    captureV2Retry: assign({
      v2Attempt: ({ context }) => context.v2Attempt + 1,
      lastFailure: ({ event }) => event.type === 'V2_WRITE_FAILED' ? event.failure : null,
      resumeStep: 'v2',
      nextRetryAtEpochSeconds: null,
      effectCheckpoint: 'persistRetrySchedule',
      checkpointRevision: ({ context }) => context.checkpointRevision + 1,
      authorityFencingToken: ({ context }) => context.authorityFencingToken + 1,
      recoveryLease: null,
    }),
    captureLegacyTerminalFailure: assign({
      legacyAttempt: ({ context, event }) =>
        event.type === 'LEGACY_WRITE_FAILED' && retryable(event.failure)
          ? context.legacyAttempt + 1
          : context.legacyAttempt,
      lastFailure: ({ event }) => event.type === 'LEGACY_WRITE_FAILED' ? event.failure : null,
      resumeStep: 'legacy',
      effectCheckpoint: 'persistBlockedTerminal',
      checkpointRevision: ({ context }) => context.checkpointRevision + 1,
      authorityFencingToken: ({ context }) => context.authorityFencingToken + 1,
      recoveryLease: null,
    }),
    captureV2TerminalFailure: assign({
      v2Attempt: ({ context, event }) =>
        event.type === 'V2_WRITE_FAILED' && retryable(event.failure)
          ? context.v2Attempt + 1
          : context.v2Attempt,
      lastFailure: ({ event }) => event.type === 'V2_WRITE_FAILED' ? event.failure : null,
      resumeStep: 'v2',
      effectCheckpoint: 'persistBlockedTerminal',
      checkpointRevision: ({ context }) => context.checkpointRevision + 1,
      authorityFencingToken: ({ context }) => context.authorityFencingToken + 1,
      recoveryLease: null,
    }),
    captureRetrySchedule: assign({
      nextRetryAtEpochSeconds: ({ event }) => event.type === 'RETRY_RECORDED'
        ? event.nextRetryAtEpochSeconds
        : null,
      effectCheckpoint: null,
      checkpointRevision: ({ context }) => context.checkpointRevision + 1,
      authorityFencingToken: ({ context }) => context.authorityFencingToken + 1,
      recoveryLease: null,
    }),
    clearRetrySchedule: assign({
      nextRetryAtEpochSeconds: null,
      effectCheckpoint: ({ context }) => writeEffectForResumeStep(context),
      checkpointRevision: ({ context }) => context.checkpointRevision + 1,
      authorityFencingToken: ({ context }) => context.authorityFencingToken + 1,
      recoveryLease: null,
    }),
    markConverged: assign({
      reconciliationStatus: 'converged',
      responseDisposition: 'convergedSuccess',
      lastFailure: null,
      nextRetryAtEpochSeconds: null,
      resumeStep: null,
      effectCheckpoint: null,
      checkpointRevision: ({ context }) => context.checkpointRevision + 1,
      authorityFencingToken: ({ context }) => context.authorityFencingToken + 1,
      recoveryLease: null,
    }),
    markBlocked: assign({
      reconciliationStatus: 'blocked',
      responseDisposition: 'blockedFailure',
      nextRetryAtEpochSeconds: null,
      effectCheckpoint: null,
      checkpointRevision: ({ context }) => context.checkpointRevision + 1,
      authorityFencingToken: ({ context }) => context.authorityFencingToken + 1,
      recoveryLease: null,
    }),
    countDuplicate: assign({ duplicateCount: ({ context }) => context.duplicateCount + 1 }),
    recordRecoveryLease: assign({
      authorityFencingToken: ({ event }) => event.type === 'RECOVERY_LEASE_ACQUIRED'
        ? event.fencingToken
        : 0,
      lastRecoveryLeaseVersion: ({ event }) => event.type === 'RECOVERY_LEASE_ACQUIRED'
        ? event.version
        : 0,
      lastRecoveryFencingToken: ({ event }) => event.type === 'RECOVERY_LEASE_ACQUIRED'
        ? event.fencingToken
        : 0,
      recoveryLease: ({ event }) => event.type === 'RECOVERY_LEASE_ACQUIRED'
        ? {
            leaseId: event.leaseId,
            holderId: event.holderId,
            version: event.version,
            fencingToken: event.fencingToken,
            expiresAtEpochSeconds: event.expiresAtEpochSeconds,
            effectId: event.expectedEffectId,
            effectCheckpoint: event.effectCheckpoint,
            checkpointRevision: event.checkpointRevision,
            effectEmitted: false,
          }
        : null,
    }),
    markRecoveryEffectEmitted: assign({
      recoveryCount: ({ context }) => context.recoveryCount + 1,
      recoveryLease: ({ context }) => context.recoveryLease === null
        ? null
        : { ...context.recoveryLease, effectEmitted: true },
    }),
    advanceLogicalClock: assign({
      logicalNowEpochSeconds: ({ event }) => event.type === 'CLOCK_ADVANCED'
        ? event.nowEpochSeconds
        : 0,
      clockRevision: ({ event }) => event.type === 'CLOCK_ADVANCED'
        ? event.clockRevision
        : 0,
    }),
  },
}).createMachine({
  id: 'legacyNotificationRegistrationCompatibility',
  initial: 'acceptingIntent',
  context: ({ input }) => {
    const stableTargetIdentity = input.legacyRegistrationId ??
      input.targetRegistrationId ?? input.targetInstallationId
    return {
      ...input,
      requestKey: legacyCompatibilityRequestKey(
        input.operation,
        input.authenticatedUserId,
        stableTargetIdentity,
        input.tokenFingerprint,
        input.compatibilityGeneration,
      ),
      reconciliationId: null,
      reconciliationStatus: 'notPersisted',
      legacyWriteStatus: input.clientGeneration === 'N_MINUS_1' ? 'pending' : 'notRequired',
      v2WriteStatus: 'pending',
      v2TargetKind: input.clientGeneration === 'N_MINUS_1'
        ? 'legacyDeterministicInstallationOnly'
        : input.targetRegistrationId !== null
          ? 'exactRegistration'
          : 'exactInstallation',
      legacyAttempt: 0,
      v2Attempt: 0,
      resumeStep: null,
      nextRetryAtEpochSeconds: null,
      lastFailure: null,
      duplicateCount: 0,
      effectCheckpoint: 'persistPendingReconciliation',
      checkpointRevision: 1,
      authorityFencingToken: 1,
      lastRecoveryLeaseVersion: 0,
      lastRecoveryFencingToken: 0,
      recoveryLease: null,
      logicalNowEpochSeconds: input.initialNowEpochSeconds,
      clockRevision: 0,
      recoveryCount: 0,
      responseDisposition: 'none',
    }
  },
  entry: 'persistPendingReconciliation',
  on: {
    DUPLICATE_REQUEST_RECEIVED: [
      { guard: 'duplicateMatches', actions: 'countDuplicate' },
      { actions: 'auditStaleOrConflictingCallback' },
    ],
    CLOCK_ADVANCED: [
      { guard: 'clockIsMonotone', actions: 'advanceLogicalClock' },
      { actions: 'auditStaleOrConflictingCallback' },
    ],
    RECOVERY_LEASE_ACQUIRED: [
      { guard: 'recoveryLeaseCanBeRecorded', actions: 'recordRecoveryLease' },
      { actions: 'auditStaleOrConflictingCallback' },
    ],
  },
  states: {
    acceptingIntent: {
      on: {
        RECOVERY_REQUESTED: [
          { guard: 'recoveryMatchesCheckpoint', actions: ['markRecoveryEffectEmitted', 'emitCurrentEffect'] },
          { actions: 'auditStaleOrConflictingCallback' },
        ],
        INTENT_PERSISTED: [
          {
            guard: ({ context, event }) =>
              eventMatchesSaga(context, event) &&
              eventAcknowledgesCurrentEffect(context, event) &&
              event.type === 'INTENT_PERSISTED' &&
              event.reconciliationId.trim().length > 0 &&
              context.operation === 'register' &&
              context.clientGeneration === 'N_MINUS_1',
            target: 'writingLegacy',
            actions: 'acceptIntent',
          },
          { guard: 'intentPersistenceIsValid', target: 'writingV2', actions: 'acceptIntent' },
          { actions: 'auditStaleOrConflictingCallback' },
        ],
      },
    },
    writingLegacy: {
      entry: 'writeLegacyStoreIdempotently',
      on: {
        RECOVERY_REQUESTED: [
          { guard: 'recoveryMatchesCheckpoint', actions: ['markRecoveryEffectEmitted', 'emitCurrentEffect'] },
          { actions: 'auditStaleOrConflictingCallback' },
        ],
        LEGACY_WRITE_SUCCEEDED: [
          {
            guard: ({ context, event }) =>
              eventMatchesSaga(context, event) &&
              eventAcknowledgesCurrentEffect(context, event) &&
              context.operation === 'register',
            target: 'writingV2',
            actions: 'markLegacyApplied',
          },
          { guard: 'effectAckIsCurrent', target: 'recordingConvergence', actions: 'markLegacyApplied' },
          { actions: 'auditStaleOrConflictingCallback' },
        ],
        LEGACY_WRITE_FAILED: [
          { guard: 'legacyFailureCanRetry', target: 'recordingRetry', actions: 'captureLegacyRetry' },
          { guard: 'effectAckIsCurrent', target: 'recordingBlock', actions: 'captureLegacyTerminalFailure' },
          { actions: 'auditStaleOrConflictingCallback' },
        ],
      },
    },
    writingV2: {
      entry: 'writeV2RegistrationStoreIdempotently',
      on: {
        RECOVERY_REQUESTED: [
          { guard: 'recoveryMatchesCheckpoint', actions: ['markRecoveryEffectEmitted', 'emitCurrentEffect'] },
          { actions: 'auditStaleOrConflictingCallback' },
        ],
        V2_WRITE_SUCCEEDED: [
          {
            guard: ({ context, event }) =>
              eventMatchesSaga(context, event) &&
              eventAcknowledgesCurrentEffect(context, event) &&
              context.operation === 'unregister' &&
              context.clientGeneration === 'N_MINUS_1',
            target: 'writingLegacy',
            actions: 'markV2Applied',
          },
          { guard: 'effectAckIsCurrent', target: 'recordingConvergence', actions: 'markV2Applied' },
          { actions: 'auditStaleOrConflictingCallback' },
        ],
        V2_WRITE_FAILED: [
          { guard: 'v2FailureCanRetry', target: 'recordingRetry', actions: 'captureV2Retry' },
          { guard: 'effectAckIsCurrent', target: 'recordingBlock', actions: 'captureV2TerminalFailure' },
          { actions: 'auditStaleOrConflictingCallback' },
        ],
      },
    },
    recordingRetry: {
      entry: 'persistRetrySchedule',
      on: {
        RECOVERY_REQUESTED: [
          { guard: 'recoveryMatchesCheckpoint', actions: ['markRecoveryEffectEmitted', 'emitCurrentEffect'] },
          { actions: 'auditStaleOrConflictingCallback' },
        ],
        RETRY_RECORDED: [
          { guard: 'retryScheduleIsValid', target: 'retryWait', actions: 'captureRetrySchedule' },
          { actions: 'auditStaleOrConflictingCallback' },
        ],
      },
    },
    retryWait: {
      on: {
        RETRY_DUE: [
          { guard: ({ context, event }) => eventMatchesSaga(context, event) && event.retryScheduleRevision === context.checkpointRevision && context.nextRetryAtEpochSeconds !== null && context.logicalNowEpochSeconds >= context.nextRetryAtEpochSeconds && context.resumeStep === 'legacy', target: 'writingLegacy', actions: 'clearRetrySchedule' },
          { guard: ({ context, event }) => eventMatchesSaga(context, event) && event.retryScheduleRevision === context.checkpointRevision && context.nextRetryAtEpochSeconds !== null && context.logicalNowEpochSeconds >= context.nextRetryAtEpochSeconds && context.resumeStep === 'v2', target: 'writingV2', actions: 'clearRetrySchedule' },
          { actions: 'auditStaleOrConflictingCallback' },
        ],
      },
    },
    recordingConvergence: {
      entry: 'persistConvergence',
      on: {
        RECOVERY_REQUESTED: [
          { guard: 'recoveryMatchesCheckpoint', actions: ['markRecoveryEffectEmitted', 'emitCurrentEffect'] },
          { actions: 'auditStaleOrConflictingCallback' },
        ],
        CONVERGENCE_RECORDED: [
          { guard: 'requiredWritesAreApplied', target: 'converged', actions: 'markConverged' },
          { actions: 'auditStaleOrConflictingCallback' },
        ],
      },
    },
    recordingBlock: {
      entry: 'persistBlockedTerminal',
      on: {
        RECOVERY_REQUESTED: [
          { guard: 'recoveryMatchesCheckpoint', actions: ['markRecoveryEffectEmitted', 'emitCurrentEffect'] },
          { actions: 'auditStaleOrConflictingCallback' },
        ],
        BLOCK_RECORDED: [
          { guard: 'effectAckIsCurrent', target: 'blocked', actions: 'markBlocked' },
          { actions: 'auditStaleOrConflictingCallback' },
        ],
      },
    },
    converged: { type: 'final' },
    blocked: { type: 'final' },
  },
})

export const createLegacyNotificationCompatibilityActor = (
  rawInput: LegacyCompatibilityInput,
) => {
  const input = validatedInput(rawInput)
  if (input === null) return null
  return createActor(legacyNotificationCompatibilityMachine, { input }).start()
}

export type RegistrationReadAuthority = 'legacy' | 'v2'

interface LegacyRegistrationRolloutContext {
  readAuthority: RegistrationReadAuthority
  checkpointId: string | null
  pendingReconciliationCount: number
  lastCheckpointFailure: 'checkpointNotSafe' | 'configuration' | null
  v2RowsPreserved: boolean
}

type LegacyRegistrationRolloutEvent =
  | { type: 'CUTOVER_REQUESTED'; checkpointId: string }
  | {
      type: 'CUTOVER_CHECKPOINT_CONFIRMED'
      checkpointId: string
      pendingReconciliationCount: number
      legacyWriterPaused: boolean
      uniquenessConfirmed: boolean
    }
  | { type: 'ROLLBACK_REQUESTED'; checkpointId: string }
  | {
      type: 'ROLLBACK_CHECKPOINT_CONFIRMED'
      checkpointId: string
      pendingReconciliationCount: number
      v2WriterPaused: boolean
      legacyProjectionReady: boolean
      reconciliationCheckpointConfirmed: boolean
    }
  | { type: 'CONFIGURATION_INVALID' }

export const legacyRegistrationRolloutMachine = setup({
  types: {
    context: {} as LegacyRegistrationRolloutContext,
    events: {} as LegacyRegistrationRolloutEvent,
    input: {} as { initialReadAuthority: RegistrationReadAuthority },
  },
  guards: {
    startsWithV2Authority: ({ context }) => context.readAuthority === 'v2',
    cutoverCheckpointSafe: ({ context, event }) =>
      event.type === 'CUTOVER_CHECKPOINT_CONFIRMED' &&
      event.checkpointId === context.checkpointId &&
      event.pendingReconciliationCount === 0 &&
      event.legacyWriterPaused &&
      event.uniquenessConfirmed,
    rollbackCheckpointSafe: ({ context, event }) =>
      event.type === 'ROLLBACK_CHECKPOINT_CONFIRMED' &&
      event.checkpointId === context.checkpointId &&
      event.pendingReconciliationCount === 0 &&
      event.v2WriterPaused &&
      event.legacyProjectionReady &&
      event.reconciliationCheckpointConfirmed,
  },
  actions: {
    persistCutoverCheckpoint: () => {},
    persistRollbackCheckpoint: () => {},
    captureCheckpoint: assign({
      checkpointId: ({ event }) =>
        event.type === 'CUTOVER_REQUESTED' || event.type === 'ROLLBACK_REQUESTED'
          ? event.checkpointId
          : null,
      lastCheckpointFailure: null,
    }),
    selectV2Authority: assign({
      readAuthority: 'v2',
      checkpointId: null,
      pendingReconciliationCount: 0,
      lastCheckpointFailure: null,
    }),
    selectLegacyAuthority: assign({
      readAuthority: 'legacy',
      checkpointId: null,
      pendingReconciliationCount: 0,
      lastCheckpointFailure: null,
      v2RowsPreserved: true,
    }),
    rejectCheckpoint: assign({
      checkpointId: null,
      pendingReconciliationCount: ({ event }) =>
        'pendingReconciliationCount' in event ? event.pendingReconciliationCount : 0,
      lastCheckpointFailure: 'checkpointNotSafe',
    }),
    captureConfigurationFailure: assign({ lastCheckpointFailure: 'configuration' }),
  },
}).createMachine({
  id: 'legacyRegistrationRollout',
  initial: 'selectingInitialAuthority',
  context: ({ input }) => ({
    readAuthority: input.initialReadAuthority,
    checkpointId: null,
    pendingReconciliationCount: 0,
    lastCheckpointFailure: null,
    v2RowsPreserved: true,
  }),
  states: {
    selectingInitialAuthority: {
      always: [
        { guard: 'startsWithV2Authority', target: 'v2Authoritative' },
        { target: 'legacyAuthoritative' },
      ],
    },
    legacyAuthoritative: {
      on: {
        CUTOVER_REQUESTED: {
          target: 'cutoverCheckpointPending',
          actions: ['captureCheckpoint', 'persistCutoverCheckpoint'],
        },
        CONFIGURATION_INVALID: { target: 'blocked', actions: 'captureConfigurationFailure' },
      },
    },
    cutoverCheckpointPending: {
      on: {
        CUTOVER_CHECKPOINT_CONFIRMED: [
          { guard: 'cutoverCheckpointSafe', target: 'v2Authoritative', actions: 'selectV2Authority' },
          { target: 'legacyAuthoritative', actions: 'rejectCheckpoint' },
        ],
        CONFIGURATION_INVALID: { target: 'blocked', actions: 'captureConfigurationFailure' },
      },
    },
    v2Authoritative: {
      on: {
        ROLLBACK_REQUESTED: {
          target: 'rollbackCheckpointPending',
          actions: ['captureCheckpoint', 'persistRollbackCheckpoint'],
        },
        CONFIGURATION_INVALID: { target: 'blocked', actions: 'captureConfigurationFailure' },
      },
    },
    rollbackCheckpointPending: {
      on: {
        ROLLBACK_CHECKPOINT_CONFIRMED: [
          { guard: 'rollbackCheckpointSafe', target: 'legacyAuthoritative', actions: 'selectLegacyAuthority' },
          { target: 'v2Authoritative', actions: 'rejectCheckpoint' },
        ],
        CONFIGURATION_INVALID: { target: 'blocked', actions: 'captureConfigurationFailure' },
      },
    },
    blocked: { type: 'final' },
  },
})
