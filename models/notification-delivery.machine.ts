import { assign, setup } from 'xstate'

export type DeliveryFailure = 'transient' | 'providerAuth' | 'invalidToken' | 'rejectedPayload' | 'unknown'
export type ApnsReason = 'Success' | 'BadDeviceToken' | 'DeviceTokenNotForTopic' | 'ExpiredToken' | 'Unregistered' | 'BadCollapseId' | 'BadMessageId' | 'BadTopic' | 'BadPath' | 'MethodNotAllowed' | 'PayloadEmpty' | 'PayloadTooLarge' | 'BadPriority' | 'BadExpirationDate' | 'MissingTopic' | 'IdleTimeout' | 'ExpiredProviderToken' | 'InvalidProviderToken' | 'TooManyRequests' | 'TooManyProviderTokenUpdates' | 'InternalServerError' | 'ServiceUnavailable'
export type EpochSeconds = number & { readonly __epochSeconds: unique symbol }
export const epochSeconds = (value: number): EpochSeconds => value as EpochSeconds
export type ApnsClassification = 'accepted' | 'invalidToken' | 'rejectedPayload' | 'retry' | 'refreshAuth' | 'providerAuthBlocked' | 'unknownTerminal'
const invalid400Reasons = new Set(['BadDeviceToken', 'DeviceTokenNotForTopic'])
const invalid410Reasons = new Set(['ExpiredToken', 'Unregistered'])
const rejectedReasons = new Set(['BadCollapseId', 'BadMessageId', 'BadTopic', 'BadPath', 'MethodNotAllowed', 'PayloadEmpty', 'PayloadTooLarge', 'BadPriority', 'BadExpirationDate', 'MissingTopic'])
export const classifyApnsResponse = (status: number, reason = ''): ApnsClassification => {
  if (status === 200) return 'accepted'
  if (status === 400) {
    if (invalid400Reasons.has(reason)) return 'invalidToken'
    if (reason === 'IdleTimeout') return 'retry'
    if (rejectedReasons.has(reason)) return 'rejectedPayload'
    return 'unknownTerminal'
  }
  if (status === 410) return invalid410Reasons.has(reason) ? 'invalidToken' : 'unknownTerminal'
  if (status === 403) return reason === 'ExpiredProviderToken' ? 'refreshAuth' : 'providerAuthBlocked'
  if (status === 429) return reason === 'TooManyProviderTokenUpdates' ? 'providerAuthBlocked' : 'retry'
  if (status === 500 || status === 503) return 'retry'
  if (status === 404 || status === 405 || status === 413) return 'rejectedPayload'
  return 'unknownTerminal'
}
/** Process-scoped provider circuit; a new delivery cannot silently clear an auth block. */
export type ProviderCircuit = { blockedCredentialVersion: string | null }
export const nextProviderCircuit = (current: ProviderCircuit, event: { type: 'AUTH_BLOCKED'; credentialVersion: string } | { type: 'VALIDATED_CREDENTIALS_CHANGED'; credentialVersion: string }): ProviderCircuit =>
  event.type === 'AUTH_BLOCKED' ? { blockedCredentialVersion: event.credentialVersion } :
    current.blockedCredentialVersion === event.credentialVersion ? current : { blockedCredentialVersion: null }
/** Per-send only: an interleaved delivery cannot consume or reset this send's one refresh. */
export type ProviderAuthSendCoordinator = { refreshUsed: boolean }
export const nextProviderAuthSendCoordinator = (
  current: ProviderAuthSendCoordinator,
  outcome: Extract<ApnsClassification, 'refreshAuth' | 'accepted' | 'providerAuthBlocked'>
): { coordinator: ProviderAuthSendCoordinator; action: 'refresh' | 'block' | 'none' } => {
  if (outcome === 'refreshAuth') return current.refreshUsed
    ? { coordinator: current, action: 'block' }
    : { coordinator: { refreshUsed: true }, action: 'refresh' }
  return { coordinator: current, action: outcome === 'providerAuthBlocked' ? 'block' : 'none' }
}
const requireIdentityComponent = (label: string, value: string): string => {
  if (value.trim() !== value || value.length === 0) throw new RangeError(`${label} is not a canonical identity component`)
  return value
}
const tupleIdentity = (prefix: string, components: readonly string[]) => `${prefix}.${components.map((value) => `${value.length}:${value}`).join('')}`
export const effectIdentity = (domainEventId: string, effectType: string, schemaVersion: number) => {
  if (!Number.isSafeInteger(schemaVersion) || schemaVersion < 1) throw new RangeError('schemaVersion must be a positive safe integer')
  return `${tupleIdentity('ek2', [requireIdentityComponent('domainEventId', domainEventId), requireIdentityComponent('effectType', effectType)])}.v${schemaVersion}`
}
export const recipientIdentity = (effectKey: string, participantId: string, channel: string) => tupleIdentity('rk2', [effectKey, requireIdentityComponent('participantId', participantId), requireIdentityComponent('channel', channel)])
export const deliveryIdentity = (recipientKey: string, registrationId: string, provider = 'apns') => tupleIdentity('dk2', [recipientKey, requireIdentityComponent('registrationId', registrationId), requireIdentityComponent('provider', provider)])
export const calendarArtifactIdentity = (effectKey: string, participantId: string, calendarProvider: string) => tupleIdentity('ck2', [effectKey, requireIdentityComponent('participantId', participantId), requireIdentityComponent('calendarProvider', calendarProvider)])
export type LegacyPollDateConfirmationSource = {
  format: 'poll-date-confirmed'
  legacyVersion: number
  eventId: string
  slotId: string
  legacyDomainEventId: string
  legacyEffectKey: string
}
export type LegacyEffectMigrationRecord = {
  format: 'poll-date-confirmed-v1'
  sourceTupleId: string
  legacyDomainEventId: string
  legacyEffectKey: string
  canonicalDomainEventId: string
  canonicalEffectKey: string
}
export type LegacyEffectMigrationResult =
  | { status: 'mapped' | 'replayed'; record: LegacyEffectMigrationRecord }
  | { status: 'quarantined'; reason: 'unsupportedLegacyFormat' | 'unsupportedLegacyVersion' | 'invalidAuthoritativeTuple' | 'legacyKeyMismatch' | 'legacyKeyCollision' | 'migrationRecordMismatch' }
export const effectIdentityMigration = {
  readVersions: ['poll-date-confirmed-v1', 'canonical-v2'],
  writeVersion: 'canonical-v2',
  authoritativeLegacyFormats: ['poll-date-confirmed-v1'],
  ambiguousLegacyKey: 'quarantine',
  collisionPolicy: 'quarantine',
} as const
const sameLegacyMigrationRecord = (left: LegacyEffectMigrationRecord, right: LegacyEffectMigrationRecord) =>
  left.format === right.format && left.sourceTupleId === right.sourceTupleId && left.legacyDomainEventId === right.legacyDomainEventId && left.legacyEffectKey === right.legacyEffectKey && left.canonicalDomainEventId === right.canonicalDomainEventId && left.canonicalEffectKey === right.canonicalEffectKey
export const migrateLegacyEffectIdentity = (input: LegacyPollDateConfirmationSource, existingRecord: LegacyEffectMigrationRecord | null): LegacyEffectMigrationResult => {
  if (input.format !== 'poll-date-confirmed') return { status: 'quarantined', reason: 'unsupportedLegacyFormat' }
  if (input.legacyVersion !== 1) return { status: 'quarantined', reason: 'unsupportedLegacyVersion' }
  let eventId: string
  let slotId: string
  try {
    eventId = requireIdentityComponent('eventId', input.eventId)
    slotId = requireIdentityComponent('slotId', input.slotId)
  } catch {
    return { status: 'quarantined', reason: 'invalidAuthoritativeTuple' }
  }
  const expectedLegacyDomainEventId = `poll-date-confirmed:${eventId}:${slotId}:v1`
  const expectedLegacyEffectKey = `${expectedLegacyDomainEventId}:confirmation`
  if (input.legacyDomainEventId !== expectedLegacyDomainEventId || input.legacyEffectKey !== expectedLegacyEffectKey) return { status: 'quarantined', reason: 'legacyKeyMismatch' }
  const sourceTupleId = tupleIdentity('lpdc1', [eventId, slotId])
  const canonicalDomainEventId = tupleIdentity('pdc2', [eventId, slotId])
  const record: LegacyEffectMigrationRecord = {
    format: 'poll-date-confirmed-v1',
    sourceTupleId,
    legacyDomainEventId: expectedLegacyDomainEventId,
    legacyEffectKey: expectedLegacyEffectKey,
    canonicalDomainEventId,
    canonicalEffectKey: effectIdentity(canonicalDomainEventId, 'confirmation', 1),
  }
  if (existingRecord === null) return { status: 'mapped', record }
  if (sameLegacyMigrationRecord(existingRecord, record)) return { status: 'replayed', record: existingRecord }
  if (existingRecord.legacyEffectKey === record.legacyEffectKey && existingRecord.sourceTupleId !== record.sourceTupleId) return { status: 'quarantined', reason: 'legacyKeyCollision' }
  return { status: 'quarantined', reason: 'migrationRecordMismatch' }
}
export const canonicalNotificationIdentities = (input: { domainEventId: string; effectType: string; schemaVersion: number; participantId: string; channel: string; registrationId: string; provider: string; calendarProvider: string }) => {
  const effectKey = effectIdentity(input.domainEventId, input.effectType, input.schemaVersion)
  const recipientKey = recipientIdentity(effectKey, input.participantId, input.channel)
  return {
    effectKey,
    recipientKey,
    deliveryKey: deliveryIdentity(recipientKey, input.registrationId, input.provider),
    calendarArtifactKey: calendarArtifactIdentity(effectKey, input.participantId, input.calendarProvider),
  }
}
export type DeliveryAuthority = 'legacy' | 'outbox-v2'
export type ProviderRetryFailureReason = 'invalidRetryAfter' | 'retryWouldReachExpiry'
export type PersistedProviderReason = 'http200' | 'http5xx' | 'tooManyRequests' | 'idleTimeout' | 'tokenInvalid' | 'payloadRejected' | 'providerAuthRejected' | 'unknownProviderReason' | 'transportBeforeWrite' | 'transportOutcomeUnknown' | 'invalidRetryAfter' | 'retryWouldReachExpiry' | 'retryBudgetExhausted'
export type DurableProviderOutcome = 'accepted' | 'retry' | 'refreshAuth' | 'invalidToken' | 'rejectedPayload' | 'providerAuthBlocked' | 'unknownOutcome' | 'expired' | 'retryExhausted'

export const fullJitterBackoffSeconds = (attempt: number, sample: number): EpochSeconds => {
  const safeAttempt = Number.isSafeInteger(attempt) ? Math.max(1, Math.min(1_024, attempt)) : 1
  const cap = Math.min(300, 2 ** Math.min(20, safeAttempt - 1))
  const normalized = Number.isFinite(sample) ? Math.max(0, Math.min(1, sample)) : 0
  return epochSeconds(Math.max(1, Math.floor(cap * normalized)))
}
export const deliveryBackoffSeconds = fullJitterBackoffSeconds

const uniqueCanonicalRegistrations = (registrationIds: readonly string[]) => [...new Set(registrationIds.filter((id) => {
  try { requireIdentityComponent('registrationId', id); return true } catch { return false }
}))].sort()

export type BackendIngestionSource = { domainEventId: string; effectType: string; schemaVersion: number; participantId: string; channel: string; provider: string; registrationIds: readonly string[] }
export const buildBackendIngestionTransaction = (source: BackendIngestionSource) => {
  const effectKey = effectIdentity(source.domainEventId, source.effectType, source.schemaVersion)
  const recipientKey = recipientIdentity(effectKey, source.participantId, source.channel)
  const registrations = uniqueCanonicalRegistrations(source.registrationIds)
  const deliveries = registrations.map((registrationId) => ({ registrationId, deliveryKey: deliveryIdentity(recipientKey, registrationId, source.provider) }))
  const transactionalTables = registrations.length > 0
    ? ['domain_event_ingestion', 'notification_logical', 'notification_recipient', 'notification_delivery'] as const
    : ['domain_event_ingestion', 'notification_logical', 'notification_recipient'] as const
  const deliveryKeys = deliveries.map((item) => item.deliveryKey)
  return {
    source, effectKey, recipientKey, deliveries, deliveryKeys,
    recipientStatus: registrations.length > 0 ? 'targeted' as const : 'pendingTarget' as const,
    transactionalTables, providerIoBeforeCommit: false as const, localOutboxWrites: 0 as const,
    transactionPlan: { deliveryKeys },
    transactionDigest: tupleIdentity('ingest2', [effectKey, recipientKey, ...deliveryKeys]),
  }
}
export const notificationPersistenceBoundary = {
  local: { table: 'confirmation_effect_outbox', owner: 'DatabaseEventRepository', transaction: 'local-confirmation' },
  backend: { owner: 'notification-backend', transaction: 'backend-ingestion', transactionalWrites: ['domain_event_ingestion', 'notification_logical', 'notification_recipient', 'notification_delivery'], postCommitOnly: ['provider-io', 'calendar-provider-io'] },
  crossDatastoreAtomicity: false,
} as const

type IngestionContext = ReturnType<typeof buildBackendIngestionTransaction> & { backendTransactionId: string | null }
type IngestionEvent = { type: 'BACKEND_INGESTION_TRANSACTION_COMMITTED'; transactionId: string; effectKey: string; transactionDigest: string }
export const notificationIngestionMachine = setup({
  types: { context: {} as IngestionContext, events: {} as IngestionEvent, input: {} as BackendIngestionSource },
  guards: { exactCommit: ({ context, event }) => event.transactionId.trim().length > 0 && event.effectKey === context.effectKey && event.transactionDigest === context.transactionDigest },
  actions: { recordCommit: assign({ backendTransactionId: ({ event }) => event.transactionId }) },
}).createMachine({
  id: 'notificationDomainEventIngestion', initial: 'awaitingBackendIngestionCommit',
  context: ({ input }) => ({ ...buildBackendIngestionTransaction(input), backendTransactionId: null }),
  states: { awaitingBackendIngestionCommit: { on: { BACKEND_INGESTION_TRANSACTION_COMMITTED: { guard: 'exactCommit', target: 'backendIngestionCommitted', actions: 'recordCommit' } } }, backendIngestionCommitted: { type: 'final' } },
})

export type TargetDelivery = { registrationId: string; deliveryKey: string }
type TargetLease = { holderId: string; version: number; fencingToken: number; expiresAtEpochSeconds: EpochSeconds }
type TargetCheckpoint = { kind: 'retry' | 'fanout' | 'expiry' | 'exhausted'; effectId: string; revision: number; effectEmitted: boolean; nextAttemptAtEpochSeconds: EpochSeconds | null; nextAttempt: number; deliveries: readonly TargetDelivery[]; transactionReceiptId: string; holderId: string | null; leaseVersion: number | null; fencingToken: number | null }
type RecipientTargetContext = { recipientKey: string; provider: string; expiresAtEpochSeconds: EpochSeconds; nowEpochSeconds: EpochSeconds; clockRevision: number; attempt: number; maxAttempts: number; nextAttemptAtEpochSeconds: EpochSeconds; checkpointRevision: number; pendingCheckpoint: TargetCheckpoint | null; deliveries: readonly TargetDelivery[]; lease: TargetLease | null; lastLeaseVersion: number; lastFencingToken: number }
type RecipientTargetInput = Pick<RecipientTargetContext, 'recipientKey' | 'provider' | 'expiresAtEpochSeconds' | 'nowEpochSeconds' | 'maxAttempts' | 'nextAttemptAtEpochSeconds'>
type TargetLeaseReference = { holderId: string; leaseVersion: number; fencingToken: number }
type TargetCheckpointReference = { effectId: string; checkpointRevision: number; transactionReceiptId: string; holderId: string | null; leaseVersion: number | null; fencingToken: number | null }
type RecipientTargetEvent =
  | { type: 'CLOCK_DURABLY_ADVANCED'; expectedClockRevision: number; newEpochSeconds: EpochSeconds }
  | { type: 'TARGET_RESOLUTION_DUE' }
  | ({ type: 'TARGET_LEASE_DURABLY_ACQUIRED'; expiresAtEpochSeconds: EpochSeconds; expectedCheckpointRevision: number } & TargetLeaseReference)
  | ({ type: 'REGISTRATIONS_RESOLVED'; registrationIds: readonly string[] } & TargetLeaseReference)
  | ({ type: 'NO_REGISTRATIONS_RESOLVED'; jitterSample: number } & TargetLeaseReference)
  | { type: 'TARGET_EXPIRY_DETECTED' }
  | ({ type: 'TARGET_CHECKPOINT_DURABLE_EFFECT_REQUESTED' } & TargetCheckpointReference)
  | ({ type: 'TARGET_CHECKPOINT_DURABLY_RECORDED' } & TargetCheckpointReference)
const targetCheckpoint = (context: RecipientTargetContext, kind: TargetCheckpoint['kind'], values: Partial<TargetCheckpoint> = {}): TargetCheckpoint => {
  const revision = context.checkpointRevision + 1
  const holderId = context.lease?.holderId ?? null
  const leaseVersion = context.lease?.version ?? null
  const fencingToken = context.lease?.fencingToken ?? null
  return { kind, revision, effectId: `${context.recipientKey}:${kind}:r${revision}:f${fencingToken ?? 0}`, effectEmitted: false, nextAttemptAtEpochSeconds: null, nextAttempt: context.attempt, deliveries: [], transactionReceiptId: `${context.recipientKey}:${kind}:receipt`, holderId, leaseVersion, fencingToken, ...values }
}
const exactTargetLease = (context: RecipientTargetContext, reference: TargetLeaseReference) => context.lease !== null && context.lease.expiresAtEpochSeconds > context.nowEpochSeconds && reference.holderId === context.lease.holderId && reference.leaseVersion === context.lease.version && reference.fencingToken === context.lease.fencingToken
const exactTargetCheckpoint = (context: RecipientTargetContext, event: TargetCheckpointReference, requireEmission: boolean) => context.pendingCheckpoint !== null && (context.pendingCheckpoint.holderId === null || (event.holderId !== null && event.leaseVersion !== null && event.fencingToken !== null && exactTargetLease(context, { holderId: event.holderId, leaseVersion: event.leaseVersion, fencingToken: event.fencingToken }))) && event.effectId === context.pendingCheckpoint.effectId && event.checkpointRevision === context.pendingCheckpoint.revision && event.transactionReceiptId === context.pendingCheckpoint.transactionReceiptId && event.holderId === context.pendingCheckpoint.holderId && event.leaseVersion === context.pendingCheckpoint.leaseVersion && event.fencingToken === context.pendingCheckpoint.fencingToken && (!requireEmission || context.pendingCheckpoint.effectEmitted)
export const notificationRecipientTargetMachine = setup({
  types: { context: {} as RecipientTargetContext, events: {} as RecipientTargetEvent, input: {} as RecipientTargetInput },
  guards: {
    clockAdvanceValid: ({ context, event }) => event.type === 'CLOCK_DURABLY_ADVANCED' && event.expectedClockRevision === context.clockRevision && event.newEpochSeconds >= context.nowEpochSeconds,
    resolutionDue: ({ context }) => context.nowEpochSeconds >= context.nextAttemptAtEpochSeconds && context.nowEpochSeconds < context.expiresAtEpochSeconds && context.attempt < context.maxAttempts,
    targetLeaseAcquisitionValid: ({ context, event }) => event.type === 'TARGET_LEASE_DURABLY_ACQUIRED' && event.expectedCheckpointRevision === context.checkpointRevision && event.holderId.trim().length > 0 && event.leaseVersion > context.lastLeaseVersion && event.fencingToken > context.lastFencingToken && event.expiresAtEpochSeconds > context.nowEpochSeconds && context.nowEpochSeconds < context.expiresAtEpochSeconds && (context.lease === null || context.lease.expiresAtEpochSeconds <= context.nowEpochSeconds),
    targetCheckpointRecoveryLeaseValid: ({ context, event }) => event.type === 'TARGET_LEASE_DURABLY_ACQUIRED' && context.pendingCheckpoint !== null && context.lease !== null && context.lease.expiresAtEpochSeconds <= context.nowEpochSeconds && event.expectedCheckpointRevision === context.checkpointRevision && event.holderId.trim().length > 0 && event.leaseVersion > context.lastLeaseVersion && event.fencingToken > context.lastFencingToken && event.expiresAtEpochSeconds > context.nowEpochSeconds && context.nowEpochSeconds < context.expiresAtEpochSeconds,
    hasRegistrations: ({ context, event }) => event.type === 'REGISTRATIONS_RESOLVED' && exactTargetLease(context, event) && uniqueCanonicalRegistrations(event.registrationIds).length > 0,
    exactNoRegistrations: ({ context, event }) => event.type === 'NO_REGISTRATIONS_RESOLVED' && exactTargetLease(context, event),
    expiryReached: ({ context }) => context.nowEpochSeconds >= context.expiresAtEpochSeconds,
    retryAvailable: ({ context, event }) => event.type === 'NO_REGISTRATIONS_RESOLVED' && exactTargetLease(context, event) && context.attempt + 1 < context.maxAttempts && context.nowEpochSeconds + fullJitterBackoffSeconds(context.attempt + 1, event.jitterSample) < context.expiresAtEpochSeconds,
    retryWouldReachExpiry: ({ context, event }) => event.type === 'NO_REGISTRATIONS_RESOLVED' && exactTargetLease(context, event) && context.attempt + 1 < context.maxAttempts && context.nowEpochSeconds + fullJitterBackoffSeconds(context.attempt + 1, event.jitterSample) >= context.expiresAtEpochSeconds,
    exactRequest: ({ context, event }) => event.type === 'TARGET_CHECKPOINT_DURABLE_EFFECT_REQUESTED' && exactTargetCheckpoint(context, event, false),
    exactAckAfterEmission: ({ context, event }) => event.type === 'TARGET_CHECKPOINT_DURABLY_RECORDED' && exactTargetCheckpoint(context, event, true),
  },
  actions: {
    advanceClock: assign({ nowEpochSeconds: ({ event }) => event.type === 'CLOCK_DURABLY_ADVANCED' ? event.newEpochSeconds : epochSeconds(0), clockRevision: ({ context }) => context.clockRevision + 1 }),
    recordTargetLease: assign(({ event }) => event.type !== 'TARGET_LEASE_DURABLY_ACQUIRED' ? {} : ({ lease: { holderId: event.holderId, version: event.leaseVersion, fencingToken: event.fencingToken, expiresAtEpochSeconds: event.expiresAtEpochSeconds }, lastLeaseVersion: event.leaseVersion, lastFencingToken: event.fencingToken })),
    recoverTargetCheckpoint: assign(({ context, event }) => {
      if (event.type !== 'TARGET_LEASE_DURABLY_ACQUIRED' || context.pendingCheckpoint === null) return {}
      const revision = context.checkpointRevision + 1
      const lease = { holderId: event.holderId, version: event.leaseVersion, fencingToken: event.fencingToken, expiresAtEpochSeconds: event.expiresAtEpochSeconds }
      return { lease, lastLeaseVersion: event.leaseVersion, lastFencingToken: event.fencingToken, checkpointRevision: revision, pendingCheckpoint: { ...context.pendingCheckpoint, revision, effectId: `${context.recipientKey}:${context.pendingCheckpoint.kind}:r${revision}:f${event.fencingToken}`, effectEmitted: false, holderId: event.holderId, leaseVersion: event.leaseVersion, fencingToken: event.fencingToken } }
    }),
    stageFanout: assign(({ context, event }) => {
      if (event.type !== 'REGISTRATIONS_RESOLVED') return {}
      const deliveries = uniqueCanonicalRegistrations(event.registrationIds).map((registrationId) => ({ registrationId, deliveryKey: deliveryIdentity(context.recipientKey, registrationId, context.provider) }))
      const checkpoint = targetCheckpoint(context, 'fanout', { deliveries })
      return { checkpointRevision: checkpoint.revision, pendingCheckpoint: checkpoint }
    }),
    stageRetry: assign(({ context, event }) => {
      if (event.type !== 'NO_REGISTRATIONS_RESOLVED') return {}
      const nextAttempt = context.attempt + 1
      const nextAttemptAtEpochSeconds = epochSeconds(context.nowEpochSeconds + fullJitterBackoffSeconds(nextAttempt, event.jitterSample))
      const checkpoint = targetCheckpoint(context, 'retry', { nextAttempt, nextAttemptAtEpochSeconds })
      return { checkpointRevision: checkpoint.revision, pendingCheckpoint: checkpoint }
    }),
    stageExpiry: assign(({ context }) => { const checkpoint = targetCheckpoint(context, 'expiry', { holderId: null, leaseVersion: null, fencingToken: null }); return { checkpointRevision: checkpoint.revision, pendingCheckpoint: checkpoint } }),
    stageExhausted: assign(({ context }) => { const checkpoint = targetCheckpoint(context, 'exhausted'); return { checkpointRevision: checkpoint.revision, pendingCheckpoint: checkpoint } }),
    emitTargetEffect: assign({ pendingCheckpoint: ({ context }) => context.pendingCheckpoint === null ? null : { ...context.pendingCheckpoint, effectEmitted: true } }),
    commitRetry: assign(({ context }) => ({ attempt: context.pendingCheckpoint?.nextAttempt ?? context.attempt, nextAttemptAtEpochSeconds: context.pendingCheckpoint?.nextAttemptAtEpochSeconds ?? context.nextAttemptAtEpochSeconds, pendingCheckpoint: null, lease: null })),
    commitFanout: assign(({ context }) => ({ deliveries: context.pendingCheckpoint?.deliveries ?? [], pendingCheckpoint: null, lease: null })),
    clearTargetCheckpoint: assign({ pendingCheckpoint: null }),
  },
}).createMachine({
  id: 'notificationRecipientTarget', initial: 'pendingTarget',
  context: ({ input }) => ({ ...input, clockRevision: 0, attempt: 0, checkpointRevision: 0, pendingCheckpoint: null, deliveries: [], lease: null, lastLeaseVersion: 0, lastFencingToken: 0 }),
  on: { CLOCK_DURABLY_ADVANCED: { guard: 'clockAdvanceValid', actions: 'advanceClock' } },
  states: {
    pendingTarget: { on: { TARGET_RESOLUTION_DUE: { guard: 'resolutionDue', target: 'awaitingTargetLease' }, TARGET_EXPIRY_DETECTED: { guard: 'expiryReached', target: 'awaitingTargetExpiryPersistence', actions: 'stageExpiry' } } },
    awaitingTargetLease: { on: { TARGET_LEASE_DURABLY_ACQUIRED: { guard: 'targetLeaseAcquisitionValid', target: 'resolvingTarget', actions: 'recordTargetLease' }, TARGET_EXPIRY_DETECTED: { guard: 'expiryReached', target: 'awaitingTargetExpiryPersistence', actions: 'stageExpiry' } } },
    resolvingTarget: { on: { REGISTRATIONS_RESOLVED: { guard: 'hasRegistrations', target: 'awaitingTargetFanoutPersistence', actions: 'stageFanout' }, NO_REGISTRATIONS_RESOLVED: [{ guard: 'retryAvailable', target: 'awaitingTargetRetryPersistence', actions: 'stageRetry' }, { guard: 'retryWouldReachExpiry', target: 'awaitingTargetExpiryPersistence', actions: 'stageExpiry' }, { target: 'awaitingTargetExhaustionPersistence', actions: 'stageExhausted' }], TARGET_EXPIRY_DETECTED: { guard: 'expiryReached', target: 'awaitingTargetExpiryPersistence', actions: 'stageExpiry' } } },
    awaitingTargetRetryPersistence: { on: { TARGET_LEASE_DURABLY_ACQUIRED: { guard: 'targetCheckpointRecoveryLeaseValid', actions: 'recoverTargetCheckpoint' }, TARGET_CHECKPOINT_DURABLE_EFFECT_REQUESTED: { guard: 'exactRequest', actions: 'emitTargetEffect' }, TARGET_CHECKPOINT_DURABLY_RECORDED: { guard: 'exactAckAfterEmission', target: 'pendingTarget', actions: 'commitRetry' } } },
    awaitingTargetFanoutPersistence: { on: { TARGET_LEASE_DURABLY_ACQUIRED: { guard: 'targetCheckpointRecoveryLeaseValid', actions: 'recoverTargetCheckpoint' }, TARGET_CHECKPOINT_DURABLE_EFFECT_REQUESTED: { guard: 'exactRequest', actions: 'emitTargetEffect' }, TARGET_CHECKPOINT_DURABLY_RECORDED: { guard: 'exactAckAfterEmission', target: 'targeted', actions: 'commitFanout' } } },
    awaitingTargetExpiryPersistence: { on: { TARGET_CHECKPOINT_DURABLE_EFFECT_REQUESTED: { guard: 'exactRequest', actions: 'emitTargetEffect' }, TARGET_CHECKPOINT_DURABLY_RECORDED: { guard: 'exactAckAfterEmission', target: 'targetExpired', actions: 'clearTargetCheckpoint' } } },
    awaitingTargetExhaustionPersistence: { on: { TARGET_CHECKPOINT_DURABLE_EFFECT_REQUESTED: { guard: 'exactRequest', actions: 'emitTargetEffect' }, TARGET_CHECKPOINT_DURABLY_RECORDED: { guard: 'exactAckAfterEmission', target: 'targetExhausted', actions: 'clearTargetCheckpoint' } } },
    targeted: { type: 'final' }, targetExpired: { type: 'final' }, targetExhausted: { type: 'final' },
  },
})

type DeliveryLease = { holderId: string; version: number; fencingToken: number; expiresAtEpochSeconds: EpochSeconds }
type DeliveryStatus = 'policyCheck' | 'suppressed' | 'deferredQuietHours' | 'awaitingToken' | 'queued' | 'auth' | 'sending' | 'retryScheduled' | 'unknownOutcome' | 'providerAuthBlocked' | 'acceptedByAPNs' | 'invalidToken' | 'rejectedPayload' | 'expired' | 'retryExhausted' | 'cancelled' | 'identityMismatch'
export type ProviderResultCheckpoint = { effectId: string; revision: number; outcome: DurableProviderOutcome; httpStatus: number | null; reason: PersistedProviderReason; acceptedAtEpochSeconds: EpochSeconds | null; nextAttemptAtEpochSeconds: EpochSeconds | null; nextAttempt: number; leaseHolderId: string | null; leaseVersion: number | null; leaseFencingToken: number | null }
export type NotificationDeliveryContext = { deliveryKey: string; canonicalDeliveryKey: string; registrationId: string; authority: DeliveryAuthority; authorityFencingToken: number; expiresAtEpochSeconds: EpochSeconds; nowEpochSeconds: EpochSeconds; clockRevision: number; attempt: number; maxAttempts: number; checkpointRevision: number; lease: DeliveryLease | null; lastLeaseVersion: number; lastLeaseFencingToken: number; correlationId: string | null; pendingCheckpoint: ProviderResultCheckpoint | null; persistenceEffectEmitted: boolean; deliveryStatus: DeliveryStatus; acceptedAtEpochSeconds: EpochSeconds | null; sentAtEpochSeconds: EpochSeconds | null; nextAttemptAtEpochSeconds: EpochSeconds | null; quietHoursNextEligibleAtEpochSeconds: EpochSeconds | null; credentialVersion: string; apnsId: string; providerCircuit: ProviderCircuit; providerAuthSendCoordinator: ProviderAuthSendCoordinator; decisionSyncStatus: 'acknowledged'; effectDispatchStatus: 'notDispatched' | 'queued' | 'sending' | 'retryScheduled' | 'unknownOutcome' | 'providerAuthBlocked' | 'dispatched' | 'terminalFailure' }
type NotificationDeliveryInput = Pick<NotificationDeliveryContext, 'deliveryKey' | 'registrationId' | 'authority' | 'authorityFencingToken' | 'expiresAtEpochSeconds' | 'nowEpochSeconds' | 'maxAttempts'> & Partial<Pick<NotificationDeliveryContext, 'canonicalDeliveryKey' | 'credentialVersion' | 'apnsId' | 'providerCircuit'>>
type ObservationReference = { deliveryKey: string; correlationId: string; attempt: number; leaseHolderId: string; leaseVersion: number; fencingToken: number }
type ProviderHttpObservation = { type: 'PROVIDER_HTTP_OBSERVED'; status: number; reason?: unknown; retryAfterEpochSeconds?: EpochSeconds; acceptedAtEpochSeconds?: EpochSeconds; jitterSample?: number } & ObservationReference
type TransportObservation = { type: 'PROVIDER_TRANSPORT_OBSERVED'; phase: 'beforeWrite' | 'mayHaveWritten'; jitterSample?: number } & ObservationReference
type DeliveryCheckpointReference = { effectId: string; checkpointRevision: number; authority: DeliveryAuthority; authorityFencingToken: number; leaseHolderId: string | null; leaseVersion: number | null; leaseFencingToken: number | null }
export type NotificationDeliveryEvent =
  | { type: 'POLICY_ALLOWED' } | { type: 'POLICY_SUPPRESSED' }
  | { type: 'QUIET_HOURS_ACTIVE'; nextEligibleAtEpochSeconds: EpochSeconds } | { type: 'QUIET_HOURS_ENDED' }
  | { type: 'NO_ACTIVE_TOKEN' } | { type: 'TOKEN_REGISTERED'; registrationId: string } | { type: 'CANCEL_REQUESTED' }
  | ({ type: 'DELIVERY_LEASE_DURABLY_ACQUIRED'; holderId: string; leaseVersion: number; fencingToken: number; leaseExpiresAtEpochSeconds: EpochSeconds; expectedCheckpointRevision: number; deliveryKey: string })
  | ({ type: 'DELIVERY_LEASE_LOST' } & ObservationReference)
  | ({ type: 'PROVIDER_AUTH_READY' } & ObservationReference)
  | ({ type: 'PROVIDER_AUTH_CONFIGURATION_REJECTED' } & ObservationReference)
  | ProviderHttpObservation | TransportObservation
  | ({ type: 'UNKNOWN_OUTCOME_RETRY_REQUESTED'; jitterSample: number } & ObservationReference)
  | ({ type: 'PROVIDER_RESULT_PERSISTENCE_REQUESTED' } & DeliveryCheckpointReference)
  | ({ type: 'PROVIDER_RESULT_DURABLY_RECORDED' } & DeliveryCheckpointReference)
  | { type: 'CLOCK_TICK'; expectedClockRevision: number; nowEpochSeconds: EpochSeconds }
  | { type: 'DELIVERY_CLOCK_DURABLY_ADVANCED'; expectedClockRevision: number; newEpochSeconds: EpochSeconds }
  | { type: 'RETRY_DUE' } | { type: 'CREDENTIALS_ROTATED'; credentialVersion: string }

type ProviderCheckpointValues = Omit<ProviderResultCheckpoint, 'effectId' | 'revision' | 'leaseHolderId' | 'leaseVersion' | 'leaseFencingToken'>
const retryCheckpointValues = (context: NotificationDeliveryContext, event: { jitterSample?: number; retryAfterEpochSeconds?: EpochSeconds; status?: number }, reason: PersistedProviderReason): ProviderCheckpointValues => {
  const nextAttempt = context.attempt + 1
  if (nextAttempt >= context.maxAttempts) return { outcome: 'retryExhausted', httpStatus: event.status ?? null, reason: 'retryBudgetExhausted', acceptedAtEpochSeconds: null, nextAttemptAtEpochSeconds: null, nextAttempt }
  if (event.retryAfterEpochSeconds !== undefined) {
    const candidate = event.retryAfterEpochSeconds
    if (!Number.isSafeInteger(candidate) || candidate <= context.nowEpochSeconds) return { outcome: 'unknownOutcome', httpStatus: event.status ?? null, reason: 'invalidRetryAfter', acceptedAtEpochSeconds: null, nextAttemptAtEpochSeconds: null, nextAttempt }
    if (candidate >= context.expiresAtEpochSeconds) return { outcome: 'expired', httpStatus: event.status ?? null, reason: 'retryWouldReachExpiry', acceptedAtEpochSeconds: null, nextAttemptAtEpochSeconds: null, nextAttempt }
    if (candidate - context.nowEpochSeconds > 300) return { outcome: 'unknownOutcome', httpStatus: event.status ?? null, reason: 'invalidRetryAfter', acceptedAtEpochSeconds: null, nextAttemptAtEpochSeconds: null, nextAttempt }
    return { outcome: 'retry', httpStatus: event.status ?? null, reason, acceptedAtEpochSeconds: null, nextAttemptAtEpochSeconds: candidate, nextAttempt }
  }
  const candidate = epochSeconds(context.nowEpochSeconds + fullJitterBackoffSeconds(nextAttempt, event.jitterSample ?? 0))
  return candidate < context.expiresAtEpochSeconds
    ? { outcome: 'retry', httpStatus: event.status ?? null, reason, acceptedAtEpochSeconds: null, nextAttemptAtEpochSeconds: candidate, nextAttempt }
    : { outcome: 'expired', httpStatus: event.status ?? null, reason: 'retryWouldReachExpiry', acceptedAtEpochSeconds: null, nextAttemptAtEpochSeconds: null, nextAttempt }
}
const classifyProviderObservation = (context: NotificationDeliveryContext, event: ProviderHttpObservation | TransportObservation): ProviderCheckpointValues => {
  if (event.type === 'PROVIDER_TRANSPORT_OBSERVED') return event.phase === 'mayHaveWritten'
    ? { outcome: 'unknownOutcome', httpStatus: null, reason: 'transportOutcomeUnknown', acceptedAtEpochSeconds: null, nextAttemptAtEpochSeconds: null, nextAttempt: context.attempt }
    : retryCheckpointValues(context, event, 'transportBeforeWrite')
  const classification = classifyApnsResponse(event.status, typeof event.reason === 'string' ? event.reason : '')
  if (classification === 'accepted') return { outcome: 'accepted', httpStatus: event.status, reason: 'http200', acceptedAtEpochSeconds: event.acceptedAtEpochSeconds ?? context.nowEpochSeconds, nextAttemptAtEpochSeconds: null, nextAttempt: context.attempt }
  if (classification === 'invalidToken') return { outcome: 'invalidToken', httpStatus: event.status, reason: 'tokenInvalid', acceptedAtEpochSeconds: null, nextAttemptAtEpochSeconds: null, nextAttempt: context.attempt }
  if (classification === 'rejectedPayload') return { outcome: 'rejectedPayload', httpStatus: event.status, reason: 'payloadRejected', acceptedAtEpochSeconds: null, nextAttemptAtEpochSeconds: null, nextAttempt: context.attempt }
  if (classification === 'refreshAuth') return context.providerAuthSendCoordinator.refreshUsed
    ? { outcome: 'providerAuthBlocked', httpStatus: event.status, reason: 'providerAuthRejected', acceptedAtEpochSeconds: null, nextAttemptAtEpochSeconds: null, nextAttempt: context.attempt }
    : { outcome: 'refreshAuth', httpStatus: event.status, reason: 'providerAuthRejected', acceptedAtEpochSeconds: null, nextAttemptAtEpochSeconds: null, nextAttempt: context.attempt }
  if (classification === 'providerAuthBlocked') return { outcome: 'providerAuthBlocked', httpStatus: event.status, reason: 'providerAuthRejected', acceptedAtEpochSeconds: null, nextAttemptAtEpochSeconds: null, nextAttempt: context.attempt }
  if (classification === 'retry') return retryCheckpointValues(context, event, event.status >= 500 ? 'http5xx' : event.reason === 'IdleTimeout' ? 'idleTimeout' : 'tooManyRequests')
  return { outcome: 'unknownOutcome', httpStatus: event.status, reason: 'unknownProviderReason', acceptedAtEpochSeconds: null, nextAttemptAtEpochSeconds: null, nextAttempt: context.attempt }
}
const exactDeliveryAttempt = (context: NotificationDeliveryContext, event: ObservationReference) => context.lease !== null && context.nowEpochSeconds < context.expiresAtEpochSeconds && context.lease.expiresAtEpochSeconds > context.nowEpochSeconds && event.deliveryKey === context.deliveryKey && event.correlationId === context.correlationId && event.attempt === context.attempt && event.leaseHolderId === context.lease.holderId && event.leaseVersion === context.lease.version && event.fencingToken === context.lease.fencingToken
const checkpointLeaseMatches = (context: NotificationDeliveryContext, event: DeliveryCheckpointReference) => context.pendingCheckpoint !== null && (context.pendingCheckpoint.leaseHolderId === null
  ? event.leaseHolderId === null && event.leaseVersion === null && event.leaseFencingToken === null
  : context.lease !== null && context.lease.expiresAtEpochSeconds > context.nowEpochSeconds && event.leaseHolderId === context.lease.holderId && event.leaseVersion === context.lease.version && event.leaseFencingToken === context.lease.fencingToken)
const exactDeliveryCheckpoint = (context: NotificationDeliveryContext, event: DeliveryCheckpointReference, requireEmission: boolean) => context.pendingCheckpoint !== null && checkpointLeaseMatches(context, event) && event.effectId === context.pendingCheckpoint.effectId && event.checkpointRevision === context.pendingCheckpoint.revision && event.authority === context.authority && event.authorityFencingToken === context.authorityFencingToken && event.leaseHolderId === context.pendingCheckpoint.leaseHolderId && event.leaseVersion === context.pendingCheckpoint.leaseVersion && event.leaseFencingToken === context.pendingCheckpoint.leaseFencingToken && (!requireEmission || context.persistenceEffectEmitted)
const expirableClock = { CLOCK_TICK: [{ guard: 'clockReachesExpiry', target: '#notificationDelivery.awaitingProviderResultPersistence', actions: ['advanceDeliveryClock', 'stageExpiry'] }, { guard: 'clockAdvanceValid', actions: 'advanceDeliveryClock' }] } as const
export const notificationDeliveryMachine = setup({
  types: { context: {} as NotificationDeliveryContext, events: {} as NotificationDeliveryEvent, input: {} as NotificationDeliveryInput },
  guards: {
    identityMatches: ({ context }) => context.deliveryKey === context.canonicalDeliveryKey,
    validLease: ({ context, event }) => event.type === 'DELIVERY_LEASE_DURABLY_ACQUIRED' && (context.authority === 'legacy' || context.authority === 'outbox-v2') && event.deliveryKey === context.deliveryKey && event.expectedCheckpointRevision === context.checkpointRevision && event.holderId.trim().length > 0 && event.leaseVersion > context.lastLeaseVersion && event.fencingToken > context.lastLeaseFencingToken && event.leaseExpiresAtEpochSeconds > context.nowEpochSeconds && context.nowEpochSeconds < context.expiresAtEpochSeconds && (context.lease === null || context.lease.expiresAtEpochSeconds <= context.nowEpochSeconds),
    validCheckpointRecoveryLease: ({ context, event }) => event.type === 'DELIVERY_LEASE_DURABLY_ACQUIRED' && context.pendingCheckpoint !== null && context.pendingCheckpoint.leaseHolderId !== null && context.lease !== null && context.lease.expiresAtEpochSeconds <= context.nowEpochSeconds && event.deliveryKey === context.deliveryKey && event.expectedCheckpointRevision === context.checkpointRevision && event.holderId.trim().length > 0 && event.leaseVersion > context.lastLeaseVersion && event.fencingToken > context.lastLeaseFencingToken && event.leaseExpiresAtEpochSeconds > context.nowEpochSeconds && context.nowEpochSeconds < context.expiresAtEpochSeconds,
    exactAttempt: ({ context, event }) => 'deliveryKey' in event && 'correlationId' in event && exactDeliveryAttempt(context, event),
    exactObservation: ({ context, event }) => (event.type === 'PROVIDER_HTTP_OBSERVED' || event.type === 'PROVIDER_TRANSPORT_OBSERVED') && exactDeliveryAttempt(context, event),
    exactPersistenceRequest: ({ context, event }) => event.type === 'PROVIDER_RESULT_PERSISTENCE_REQUESTED' && exactDeliveryCheckpoint(context, event, false),
    persistedOutcome: ({ context, event }, params: { outcome: DurableProviderOutcome }) => event.type === 'PROVIDER_RESULT_DURABLY_RECORDED' && exactDeliveryCheckpoint(context, event, true) && context.pendingCheckpoint?.outcome === params.outcome,
    clockAdvanceValid: ({ context, event }) => event.type === 'CLOCK_TICK' && event.expectedClockRevision === context.clockRevision && event.nowEpochSeconds >= context.nowEpochSeconds,
    clockReachesExpiry: ({ context, event }) => event.type === 'CLOCK_TICK' && event.expectedClockRevision === context.clockRevision && event.nowEpochSeconds >= context.expiresAtEpochSeconds && event.nowEpochSeconds >= context.nowEpochSeconds,
    legacyClockAdvanceValid: ({ context, event }) => event.type === 'DELIVERY_CLOCK_DURABLY_ADVANCED' && event.expectedClockRevision === context.clockRevision && event.newEpochSeconds >= context.nowEpochSeconds,
    quietHoursDue: ({ context }) => context.quietHoursNextEligibleAtEpochSeconds !== null && context.nowEpochSeconds >= context.quietHoursNextEligibleAtEpochSeconds,
    tokenMatchesRegistration: ({ context, event }) => event.type === 'TOKEN_REGISTERED' && event.registrationId === context.registrationId,
    retryDue: ({ context }) => context.nextAttemptAtEpochSeconds !== null && context.nowEpochSeconds >= context.nextAttemptAtEpochSeconds && context.nowEpochSeconds < context.expiresAtEpochSeconds,
    credentialsRotatedWithLiveLease: ({ context, event }) => event.type === 'CREDENTIALS_ROTATED' && event.credentialVersion !== context.credentialVersion && context.lease !== null && context.lease.expiresAtEpochSeconds > context.nowEpochSeconds && context.nowEpochSeconds < context.expiresAtEpochSeconds,
    credentialsRotated: ({ context, event }) => event.type === 'CREDENTIALS_ROTATED' && event.credentialVersion !== context.credentialVersion,
  },
  actions: {
    advanceDeliveryClock: assign({ nowEpochSeconds: ({ event }) => event.type === 'CLOCK_TICK' ? event.nowEpochSeconds : event.type === 'DELIVERY_CLOCK_DURABLY_ADVANCED' ? event.newEpochSeconds : epochSeconds(0), clockRevision: ({ context }) => context.clockRevision + 1 }),
    rejectIdentity: assign({ deliveryStatus: 'identityMismatch', effectDispatchStatus: 'terminalFailure' }),
    suppress: assign({ deliveryStatus: 'suppressed', effectDispatchStatus: 'terminalFailure' }),
    deferQuietHours: assign(({ event }) => event.type === 'QUIET_HOURS_ACTIVE' ? ({ quietHoursNextEligibleAtEpochSeconds: event.nextEligibleAtEpochSeconds, deliveryStatus: 'deferredQuietHours' as const }) : {}),
    enterPolicy: assign({ deliveryStatus: 'policyCheck', quietHoursNextEligibleAtEpochSeconds: null }),
    awaitToken: assign({ deliveryStatus: 'awaitingToken' }),
    queuePolicyDelivery: assign({ deliveryStatus: 'queued', effectDispatchStatus: 'queued' }),
    cancelBeforeWrite: assign({ deliveryStatus: 'cancelled', effectDispatchStatus: 'terminalFailure' }),
    recordLeaseAndBeginAuth: assign(({ context, event }) => event.type !== 'DELIVERY_LEASE_DURABLY_ACQUIRED' ? {} : ({ lease: { holderId: event.holderId, version: event.leaseVersion, fencingToken: event.fencingToken, expiresAtEpochSeconds: event.leaseExpiresAtEpochSeconds }, lastLeaseVersion: event.leaseVersion, lastLeaseFencingToken: event.fencingToken, correlationId: `${context.deliveryKey}:auth:a${context.attempt}:v${event.leaseVersion}:f${event.fencingToken}:refresh${context.providerAuthSendCoordinator.refreshUsed ? 1 : 0}`, deliveryStatus: 'auth' as const, effectDispatchStatus: 'queued' as const })),
    renewUnknownLease: assign(({ event }) => event.type !== 'DELIVERY_LEASE_DURABLY_ACQUIRED' ? {} : ({ lease: { holderId: event.holderId, version: event.leaseVersion, fencingToken: event.fencingToken, expiresAtEpochSeconds: event.leaseExpiresAtEpochSeconds }, lastLeaseVersion: event.leaseVersion, lastLeaseFencingToken: event.fencingToken })),
    beginSending: assign(({ context }) => ({ correlationId: `${context.deliveryKey}:send:a${context.attempt}:v${context.lease?.version ?? 0}:f${context.lease?.fencingToken ?? 0}:refresh${context.providerAuthSendCoordinator.refreshUsed ? 1 : 0}`, deliveryStatus: 'sending' as const, effectDispatchStatus: 'sending' as const })),
    loseLease: assign({ lease: null, correlationId: null, deliveryStatus: 'queued', effectDispatchStatus: 'queued' }),
    recoverProviderCheckpointLease: assign(({ context, event }) => {
      if (event.type !== 'DELIVERY_LEASE_DURABLY_ACQUIRED' || context.pendingCheckpoint === null) return {}
      const revision = context.checkpointRevision + 1
      return { lease: { holderId: event.holderId, version: event.leaseVersion, fencingToken: event.fencingToken, expiresAtEpochSeconds: event.leaseExpiresAtEpochSeconds }, lastLeaseVersion: event.leaseVersion, lastLeaseFencingToken: event.fencingToken, checkpointRevision: revision, pendingCheckpoint: { ...context.pendingCheckpoint, revision, effectId: `${context.deliveryKey}:provider-result:r${revision}:af${context.authorityFencingToken}:lf${event.fencingToken}`, leaseHolderId: event.holderId, leaseVersion: event.leaseVersion, leaseFencingToken: event.fencingToken }, persistenceEffectEmitted: false }
    }),
    stageObservation: assign(({ context, event }) => {
      if (event.type !== 'PROVIDER_HTTP_OBSERVED' && event.type !== 'PROVIDER_TRANSPORT_OBSERVED') return {}
      const revision = context.checkpointRevision + 1
      return { checkpointRevision: revision, pendingCheckpoint: { effectId: `${context.deliveryKey}:provider-result:r${revision}:af${context.authorityFencingToken}:lf${context.lease?.fencingToken ?? 0}`, revision, ...classifyProviderObservation(context, event), leaseHolderId: context.lease?.holderId ?? null, leaseVersion: context.lease?.version ?? null, leaseFencingToken: context.lease?.fencingToken ?? null }, persistenceEffectEmitted: false }
    }),
    stageAuthBlocked: assign(({ context }) => { const revision = context.checkpointRevision + 1; return { checkpointRevision: revision, pendingCheckpoint: { effectId: `${context.deliveryKey}:provider-result:r${revision}:af${context.authorityFencingToken}:lf${context.lease?.fencingToken ?? 0}`, revision, outcome: 'providerAuthBlocked' as const, httpStatus: null, reason: 'providerAuthRejected' as const, acceptedAtEpochSeconds: null, nextAttemptAtEpochSeconds: null, nextAttempt: context.attempt, leaseHolderId: context.lease?.holderId ?? null, leaseVersion: context.lease?.version ?? null, leaseFencingToken: context.lease?.fencingToken ?? null }, persistenceEffectEmitted: false } }),
    stageCancellationUnknown: assign(({ context }) => { const revision = context.checkpointRevision + 1; return { checkpointRevision: revision, pendingCheckpoint: { effectId: `${context.deliveryKey}:provider-result:r${revision}:af${context.authorityFencingToken}:lf${context.lease?.fencingToken ?? 0}`, revision, outcome: 'unknownOutcome' as const, httpStatus: null, reason: 'transportOutcomeUnknown' as const, acceptedAtEpochSeconds: null, nextAttemptAtEpochSeconds: null, nextAttempt: context.attempt, leaseHolderId: context.lease?.holderId ?? null, leaseVersion: context.lease?.version ?? null, leaseFencingToken: context.lease?.fencingToken ?? null }, persistenceEffectEmitted: false } }),
    stageUnknownRetry: assign(({ context, event }) => { if (event.type !== 'UNKNOWN_OUTCOME_RETRY_REQUESTED') return {}; const revision = context.checkpointRevision + 1; return { checkpointRevision: revision, pendingCheckpoint: { effectId: `${context.deliveryKey}:provider-result:r${revision}:af${context.authorityFencingToken}:lf${context.lease?.fencingToken ?? 0}`, revision, ...retryCheckpointValues(context, event, 'transportOutcomeUnknown'), leaseHolderId: context.lease?.holderId ?? null, leaseVersion: context.lease?.version ?? null, leaseFencingToken: context.lease?.fencingToken ?? null }, persistenceEffectEmitted: false } }),
    stageExpiry: assign(({ context }) => { const revision = context.checkpointRevision + 1; return { checkpointRevision: revision, pendingCheckpoint: { effectId: `${context.deliveryKey}:expired:r${revision}:af${context.authorityFencingToken}:lf${context.lease?.fencingToken ?? 0}`, revision, outcome: 'expired' as const, httpStatus: null, reason: 'retryWouldReachExpiry' as const, acceptedAtEpochSeconds: null, nextAttemptAtEpochSeconds: null, nextAttempt: context.attempt, leaseHolderId: context.lease?.holderId ?? null, leaseVersion: context.lease?.version ?? null, leaseFencingToken: context.lease?.fencingToken ?? null }, persistenceEffectEmitted: false } }),
    emitPersistenceEffect: assign({ persistenceEffectEmitted: true }),
    commitAccepted: assign(({ context }) => ({ deliveryStatus: 'acceptedByAPNs' as const, acceptedAtEpochSeconds: context.pendingCheckpoint?.acceptedAtEpochSeconds ?? null, sentAtEpochSeconds: context.pendingCheckpoint?.acceptedAtEpochSeconds ?? null, effectDispatchStatus: 'dispatched' as const, pendingCheckpoint: null, persistenceEffectEmitted: false })),
    commitRetry: assign(({ context }) => ({ deliveryStatus: 'retryScheduled' as const, effectDispatchStatus: 'retryScheduled' as const, attempt: context.pendingCheckpoint?.nextAttempt ?? context.attempt, nextAttemptAtEpochSeconds: context.pendingCheckpoint?.nextAttemptAtEpochSeconds ?? null, lease: null, correlationId: null, pendingCheckpoint: null, persistenceEffectEmitted: false })),
    commitRefreshAuth: assign(({ context }) => ({ deliveryStatus: 'auth' as const, correlationId: `${context.deliveryKey}:auth-refresh:a${context.attempt}:v${context.lease?.version ?? 0}:f${context.lease?.fencingToken ?? 0}`, providerAuthSendCoordinator: nextProviderAuthSendCoordinator(context.providerAuthSendCoordinator, 'refreshAuth').coordinator, pendingCheckpoint: null, persistenceEffectEmitted: false })),
    commitInvalid: assign({ deliveryStatus: 'invalidToken', effectDispatchStatus: 'terminalFailure', pendingCheckpoint: null, persistenceEffectEmitted: false }),
    commitRejected: assign({ deliveryStatus: 'rejectedPayload', effectDispatchStatus: 'terminalFailure', pendingCheckpoint: null, persistenceEffectEmitted: false }),
    commitBlocked: assign(({ context }) => ({ deliveryStatus: 'providerAuthBlocked' as const, effectDispatchStatus: 'providerAuthBlocked' as const, providerCircuit: nextProviderCircuit(context.providerCircuit, { type: 'AUTH_BLOCKED', credentialVersion: context.credentialVersion }), pendingCheckpoint: null, persistenceEffectEmitted: false })),
    commitUnknown: assign({ deliveryStatus: 'unknownOutcome', effectDispatchStatus: 'unknownOutcome', pendingCheckpoint: null, persistenceEffectEmitted: false }),
    commitExpired: assign({ deliveryStatus: 'expired', effectDispatchStatus: 'terminalFailure', pendingCheckpoint: null, persistenceEffectEmitted: false }),
    commitExhausted: assign({ deliveryStatus: 'retryExhausted', effectDispatchStatus: 'terminalFailure', pendingCheckpoint: null, persistenceEffectEmitted: false }),
    queueRetry: assign({ deliveryStatus: 'queued', effectDispatchStatus: 'queued', nextAttemptAtEpochSeconds: null, providerAuthSendCoordinator: { refreshUsed: false } }),
    rotateCredentials: assign(({ context, event }) => event.type !== 'CREDENTIALS_ROTATED' ? {} : ({ credentialVersion: event.credentialVersion, providerCircuit: nextProviderCircuit(context.providerCircuit, { type: 'VALIDATED_CREDENTIALS_CHANGED', credentialVersion: event.credentialVersion }), correlationId: `${context.deliveryKey}:auth:a${context.attempt}:v${context.lease?.version ?? 0}:f${context.lease?.fencingToken ?? 0}:refresh0`, providerAuthSendCoordinator: { refreshUsed: false }, deliveryStatus: 'auth' as const })),
    rotateCredentialsAndQueue: assign(({ context, event }) => event.type !== 'CREDENTIALS_ROTATED' ? {} : ({ credentialVersion: event.credentialVersion, providerCircuit: nextProviderCircuit(context.providerCircuit, { type: 'VALIDATED_CREDENTIALS_CHANGED', credentialVersion: event.credentialVersion }), correlationId: null, lease: null, providerAuthSendCoordinator: { refreshUsed: false }, deliveryStatus: 'queued' as const, effectDispatchStatus: 'queued' as const })),
  },
}).createMachine({
  id: 'notificationDelivery', initial: 'routeIdentity',
  context: ({ input }) => ({ ...input, canonicalDeliveryKey: input.canonicalDeliveryKey ?? input.deliveryKey, credentialVersion: input.credentialVersion ?? 'credential-1', apnsId: input.apnsId ?? `${input.deliveryKey}:apns`, providerCircuit: input.providerCircuit ?? { blockedCredentialVersion: null }, providerAuthSendCoordinator: { refreshUsed: false }, clockRevision: 0, attempt: 0, checkpointRevision: 0, lease: null, lastLeaseVersion: 0, lastLeaseFencingToken: 0, correlationId: null, pendingCheckpoint: null, persistenceEffectEmitted: false, deliveryStatus: 'policyCheck', acceptedAtEpochSeconds: null, sentAtEpochSeconds: null, nextAttemptAtEpochSeconds: null, quietHoursNextEligibleAtEpochSeconds: null, decisionSyncStatus: 'acknowledged', effectDispatchStatus: 'notDispatched' }),
  on: { DELIVERY_CLOCK_DURABLY_ADVANCED: { guard: 'legacyClockAdvanceValid', actions: 'advanceDeliveryClock' } },
  states: {
    routeIdentity: { always: [{ guard: 'identityMatches', target: 'policyCheck' }, { target: 'identityMismatch', actions: 'rejectIdentity' }] },
    policyCheck: { on: { ...expirableClock, POLICY_ALLOWED: { target: 'queued', actions: 'queuePolicyDelivery' }, POLICY_SUPPRESSED: { target: 'suppressed', actions: 'suppress' }, QUIET_HOURS_ACTIVE: { target: 'deferredQuietHours', actions: 'deferQuietHours' }, NO_ACTIVE_TOKEN: { target: 'awaitingToken', actions: 'awaitToken' }, CANCEL_REQUESTED: { target: 'cancelled', actions: 'cancelBeforeWrite' } } },
    deferredQuietHours: { on: { ...expirableClock, QUIET_HOURS_ENDED: { guard: 'quietHoursDue', target: 'policyCheck', actions: 'enterPolicy' }, CANCEL_REQUESTED: { target: 'cancelled', actions: 'cancelBeforeWrite' } } },
    awaitingToken: { on: { ...expirableClock, TOKEN_REGISTERED: { guard: 'tokenMatchesRegistration', target: 'policyCheck', actions: 'enterPolicy' }, CANCEL_REQUESTED: { target: 'cancelled', actions: 'cancelBeforeWrite' } } },
    queued: { on: { ...expirableClock, DELIVERY_LEASE_DURABLY_ACQUIRED: { guard: 'validLease', target: 'auth', actions: 'recordLeaseAndBeginAuth' }, CANCEL_REQUESTED: { target: 'cancelled', actions: 'cancelBeforeWrite' } } },
    auth: { on: { ...expirableClock, PROVIDER_AUTH_READY: { guard: 'exactAttempt', target: 'sending', actions: 'beginSending' }, PROVIDER_AUTH_CONFIGURATION_REJECTED: { guard: 'exactAttempt', target: 'awaitingProviderResultPersistence', actions: 'stageAuthBlocked' }, DELIVERY_LEASE_LOST: { guard: 'exactAttempt', target: 'queued', actions: 'loseLease' }, CANCEL_REQUESTED: { target: 'cancelled', actions: 'cancelBeforeWrite' } } },
    sending: { on: { ...expirableClock, PROVIDER_HTTP_OBSERVED: { guard: 'exactObservation', target: 'awaitingProviderResultPersistence', actions: 'stageObservation' }, PROVIDER_TRANSPORT_OBSERVED: { guard: 'exactObservation', target: 'awaitingProviderResultPersistence', actions: 'stageObservation' }, CANCEL_REQUESTED: { target: 'awaitingProviderResultPersistence', actions: 'stageCancellationUnknown' } } },
    awaitingProviderResultPersistence: { on: { DELIVERY_LEASE_DURABLY_ACQUIRED: { guard: 'validCheckpointRecoveryLease', actions: 'recoverProviderCheckpointLease' }, CLOCK_TICK: { guard: 'clockAdvanceValid', actions: 'advanceDeliveryClock' }, PROVIDER_RESULT_PERSISTENCE_REQUESTED: { guard: 'exactPersistenceRequest', actions: 'emitPersistenceEffect' }, PROVIDER_RESULT_DURABLY_RECORDED: [
      { guard: { type: 'persistedOutcome', params: { outcome: 'accepted' } }, target: 'accepted', actions: 'commitAccepted' },
      { guard: { type: 'persistedOutcome', params: { outcome: 'retry' } }, target: 'retry', actions: 'commitRetry' },
      { guard: { type: 'persistedOutcome', params: { outcome: 'refreshAuth' } }, target: 'auth', actions: 'commitRefreshAuth' },
      { guard: { type: 'persistedOutcome', params: { outcome: 'invalidToken' } }, target: 'invalidToken', actions: 'commitInvalid' },
      { guard: { type: 'persistedOutcome', params: { outcome: 'rejectedPayload' } }, target: 'rejectedPayload', actions: 'commitRejected' },
      { guard: { type: 'persistedOutcome', params: { outcome: 'providerAuthBlocked' } }, target: 'providerAuthBlocked', actions: 'commitBlocked' },
      { guard: { type: 'persistedOutcome', params: { outcome: 'expired' } }, target: 'expired', actions: 'commitExpired' },
      { guard: { type: 'persistedOutcome', params: { outcome: 'retryExhausted' } }, target: 'retryExhausted', actions: 'commitExhausted' },
      { guard: { type: 'persistedOutcome', params: { outcome: 'unknownOutcome' } }, target: 'unknownOutcome', actions: 'commitUnknown' },
    ] } },
    retry: { on: { ...expirableClock, RETRY_DUE: { guard: 'retryDue', target: 'queued', actions: 'queueRetry' }, CANCEL_REQUESTED: { target: 'cancelled', actions: 'cancelBeforeWrite' } } },
    unknownOutcome: { on: { ...expirableClock, DELIVERY_LEASE_DURABLY_ACQUIRED: { guard: 'validLease', actions: 'renewUnknownLease' }, UNKNOWN_OUTCOME_RETRY_REQUESTED: { guard: 'exactAttempt', target: 'awaitingProviderResultPersistence', actions: 'stageUnknownRetry' } } },
    providerAuthBlocked: { on: { ...expirableClock, CREDENTIALS_ROTATED: [{ guard: 'credentialsRotatedWithLiveLease', target: 'auth', actions: 'rotateCredentials' }, { guard: 'credentialsRotated', target: 'queued', actions: 'rotateCredentialsAndQueue' }] } },
    accepted: { type: 'final' }, invalidToken: { type: 'final' }, rejectedPayload: { type: 'final' }, expired: { type: 'final' }, retryExhausted: { type: 'final' }, cancelled: { type: 'final' }, suppressed: { type: 'final' }, identityMismatch: { type: 'final' },
  },
})

type AuthorityEffectKind = 'pauseLegacy' | 'reconcileLegacy' | 'commitOutboxV2' | 'pauseOutboxV2' | 'reconcileOutboxV2' | 'commitLegacy'
type AuthorityLease = { holderId: string; version: number; fencingToken: number; expiresAtEpochSeconds: EpochSeconds }
type AuthorityEffect = { kind: AuthorityEffectKind; effectId: string; checkpointRevision: number; fencingToken: number; effectEmitted: boolean }
type AuthorityContext = { authority: DeliveryAuthority; checkpointRevision: number; activeAttempts: number; nowEpochSeconds: EpochSeconds; clockRevision: number; recoveryLease: AuthorityLease | null; lastLeaseVersion: number; lastFencingToken: number; pendingEffect: AuthorityEffect | null }
type AuthorityReference = { holderId: string; leaseVersion: number; fencingToken: number }
type AuthorityEvent =
  | ({ type: 'AUTHORITY_RECOVERY_LEASE_DURABLY_ACQUIRED'; expiresAtEpochSeconds: EpochSeconds; expectedCheckpointRevision: number } & AuthorityReference)
  | ({ type: 'CUTOVER_REQUESTED' } & AuthorityReference) | ({ type: 'ROLLBACK_REQUESTED' } & AuthorityReference)
  | ({ type: 'AUTHORITY_EFFECT_REQUESTED'; effectId: string; checkpointRevision: number } & AuthorityReference)
  | ({ type: 'AUTHORITY_EFFECT_DURABLY_RECORDED'; effectId: string; checkpointRevision: number } & AuthorityReference)
  | { type: 'CLOCK_DURABLY_ADVANCED'; expectedClockRevision: number; newEpochSeconds: EpochSeconds }
const makeAuthorityEffect = (context: AuthorityContext, kind: AuthorityEffectKind, revision = context.checkpointRevision + 1, fence = context.recoveryLease?.fencingToken ?? context.lastFencingToken): AuthorityEffect => ({ kind, checkpointRevision: revision, fencingToken: fence, effectId: `authority:${kind}:r${revision}:f${fence}`, effectEmitted: false })
const exactAuthorityLease = (context: AuthorityContext, reference: AuthorityReference) => context.recoveryLease !== null && context.recoveryLease.expiresAtEpochSeconds > context.nowEpochSeconds && reference.holderId === context.recoveryLease.holderId && reference.leaseVersion === context.recoveryLease.version && reference.fencingToken === context.recoveryLease.fencingToken
const exactAuthorityEffect = (context: AuthorityContext, event: Extract<AuthorityEvent, { effectId: string }>, requireEmission: boolean) => exactAuthorityLease(context, event) && context.pendingEffect !== null && event.effectId === context.pendingEffect.effectId && event.checkpointRevision === context.pendingEffect.checkpointRevision && event.fencingToken === context.pendingEffect.fencingToken && (!requireEmission || context.pendingEffect.effectEmitted)
const nextAuthorityEffectKind = (kind: AuthorityEffectKind): AuthorityEffectKind | null => {
  const sequence: Record<AuthorityEffectKind, AuthorityEffectKind | null> = { pauseLegacy: 'reconcileLegacy', reconcileLegacy: 'commitOutboxV2', commitOutboxV2: null, pauseOutboxV2: 'reconcileOutboxV2', reconcileOutboxV2: 'commitLegacy', commitLegacy: null }
  return sequence[kind]
}
export const notificationDeliveryAuthorityMachine = setup({
  types: { context: {} as AuthorityContext, events: {} as AuthorityEvent, input: {} as Pick<AuthorityContext, 'authority' | 'checkpointRevision' | 'activeAttempts' | 'nowEpochSeconds'> },
  guards: {
    clockAdvanceValid: ({ context, event }) => event.type === 'CLOCK_DURABLY_ADVANCED' && event.expectedClockRevision === context.clockRevision && event.newEpochSeconds >= context.nowEpochSeconds,
    leaseAcquisitionValid: ({ context, event }) => event.type === 'AUTHORITY_RECOVERY_LEASE_DURABLY_ACQUIRED' && event.expectedCheckpointRevision === context.checkpointRevision && event.holderId.trim().length > 0 && event.leaseVersion > context.lastLeaseVersion && event.fencingToken > context.lastFencingToken && event.expiresAtEpochSeconds > context.nowEpochSeconds && (context.recoveryLease === null || context.recoveryLease.expiresAtEpochSeconds <= context.nowEpochSeconds),
    cutoverReady: ({ context, event }) => event.type === 'CUTOVER_REQUESTED' && context.authority === 'legacy' && context.activeAttempts === 0 && exactAuthorityLease(context, event),
    rollbackReady: ({ context, event }) => event.type === 'ROLLBACK_REQUESTED' && context.authority === 'outbox-v2' && context.activeAttempts === 0 && exactAuthorityLease(context, event),
    exactEffectRequest: ({ context, event }) => event.type === 'AUTHORITY_EFFECT_REQUESTED' && exactAuthorityEffect(context, event, false),
    exactEffectAck: ({ context, event }) => event.type === 'AUTHORITY_EFFECT_DURABLY_RECORDED' && exactAuthorityEffect(context, event, true),
    committingOutboxV2: ({ context, event }) => event.type === 'AUTHORITY_EFFECT_DURABLY_RECORDED' && exactAuthorityEffect(context, event, true) && context.pendingEffect?.kind === 'commitOutboxV2',
    committingLegacy: ({ context, event }) => event.type === 'AUTHORITY_EFFECT_DURABLY_RECORDED' && exactAuthorityEffect(context, event, true) && context.pendingEffect?.kind === 'commitLegacy',
  },
  actions: {
    advanceAuthorityClock: assign({ nowEpochSeconds: ({ event }) => event.type === 'CLOCK_DURABLY_ADVANCED' ? event.newEpochSeconds : epochSeconds(0), clockRevision: ({ context }) => context.clockRevision + 1 }),
    recordRecoveryLease: assign(({ context, event }) => {
      if (event.type !== 'AUTHORITY_RECOVERY_LEASE_DURABLY_ACQUIRED') return {}
      const revision = context.pendingEffect === null ? context.checkpointRevision : context.checkpointRevision + 1
      const recoveryLease = { holderId: event.holderId, version: event.leaseVersion, fencingToken: event.fencingToken, expiresAtEpochSeconds: event.expiresAtEpochSeconds }
      return { recoveryLease, lastLeaseVersion: event.leaseVersion, lastFencingToken: event.fencingToken, checkpointRevision: revision, pendingEffect: context.pendingEffect === null ? null : makeAuthorityEffect({ ...context, recoveryLease }, context.pendingEffect.kind, revision, event.fencingToken) }
    }),
    beginCutover: assign(({ context }) => { const effect = makeAuthorityEffect(context, 'pauseLegacy'); return { checkpointRevision: effect.checkpointRevision, pendingEffect: effect } }),
    beginRollback: assign(({ context }) => { const effect = makeAuthorityEffect(context, 'pauseOutboxV2'); return { checkpointRevision: effect.checkpointRevision, pendingEffect: effect } }),
    emitAuthorityEffect: assign({ pendingEffect: ({ context }) => context.pendingEffect === null ? null : { ...context.pendingEffect, effectEmitted: true } }),
    advanceAuthorityEffect: assign(({ context }) => {
      if (context.pendingEffect === null) return {}
      const nextKind = nextAuthorityEffectKind(context.pendingEffect.kind)
      if (nextKind === null) return {}
      const effect = makeAuthorityEffect(context, nextKind)
      return { checkpointRevision: effect.checkpointRevision, pendingEffect: effect }
    }),
    commitOutboxV2: assign({ authority: 'outbox-v2', pendingEffect: null, recoveryLease: null }),
    commitLegacy: assign({ authority: 'legacy', pendingEffect: null, recoveryLease: null }),
  },
}).createMachine({
  id: 'notificationDeliveryAuthority', initial: 'routeAuthority',
  context: ({ input }) => ({ ...input, clockRevision: 0, recoveryLease: null, lastLeaseVersion: 0, lastFencingToken: 0, pendingEffect: null }),
  on: { CLOCK_DURABLY_ADVANCED: { guard: 'clockAdvanceValid', actions: 'advanceAuthorityClock' }, AUTHORITY_RECOVERY_LEASE_DURABLY_ACQUIRED: { guard: 'leaseAcquisitionValid', actions: 'recordRecoveryLease' } },
  states: {
    routeAuthority: { always: [{ guard: ({ context }) => context.authority === 'legacy', target: 'legacyAuthoritative' }, { guard: ({ context }) => context.authority === 'outbox-v2', target: 'outboxV2Authoritative' }, { target: 'authorityRejected' }] },
    legacyAuthoritative: { on: { CUTOVER_REQUESTED: { guard: 'cutoverReady', target: 'cutoverRecovery', actions: 'beginCutover' } } },
    cutoverRecovery: { on: { AUTHORITY_EFFECT_REQUESTED: { guard: 'exactEffectRequest', actions: 'emitAuthorityEffect' }, AUTHORITY_EFFECT_DURABLY_RECORDED: [{ guard: 'committingOutboxV2', target: 'outboxV2Authoritative', actions: 'commitOutboxV2' }, { guard: 'exactEffectAck', actions: 'advanceAuthorityEffect' }] } },
    outboxV2Authoritative: { on: { ROLLBACK_REQUESTED: { guard: 'rollbackReady', target: 'rollbackRecovery', actions: 'beginRollback' } } },
    rollbackRecovery: { on: { AUTHORITY_EFFECT_REQUESTED: { guard: 'exactEffectRequest', actions: 'emitAuthorityEffect' }, AUTHORITY_EFFECT_DURABLY_RECORDED: [{ guard: 'committingLegacy', target: 'legacyAuthoritative', actions: 'commitLegacy' }, { guard: 'exactEffectAck', actions: 'advanceAuthorityEffect' }] } },
    authorityRejected: { type: 'final' },
  },
})

type CalendarLease = { holderId: string; version: number; fencingToken: number; expiresAtEpochSeconds: EpochSeconds }
type CalendarCheckpointOutcome = 'upserted' | 'removed' | 'retry' | 'expired' | 'retryExhausted'
type CalendarCheckpoint = { effectId: string; revision: number; outcome: CalendarCheckpointOutcome; effectEmitted: boolean; holderId: string | null; leaseVersion: number | null; fencingToken: number | null; nextAttempt: number; nextAttemptAtEpochSeconds: EpochSeconds | null }
type CalendarContext = { calendarArtifactKey: string; checkpointRevision: number; pendingCheckpoint: CalendarCheckpoint | null; nowEpochSeconds: EpochSeconds; expiresAtEpochSeconds: EpochSeconds; clockRevision: number; attempt: number; maxAttempts: number; nextAttemptAtEpochSeconds: EpochSeconds; lease: CalendarLease | null; lastLeaseVersion: number; lastFencingToken: number; correlationId: string | null }
type CalendarInput = Pick<CalendarContext, 'calendarArtifactKey' | 'checkpointRevision'> & Partial<Pick<CalendarContext, 'nowEpochSeconds' | 'expiresAtEpochSeconds' | 'maxAttempts' | 'nextAttemptAtEpochSeconds'>>
type CalendarAttemptReference = { calendarArtifactKey: string; correlationId: string; attempt: number; holderId: string; leaseVersion: number; fencingToken: number }
type CalendarCheckpointReference = { effectId: string; checkpointRevision: number; holderId: string | null; leaseVersion: number | null; fencingToken: number | null }
type CalendarEvent =
  | { type: 'CALENDAR_LEASE_DURABLY_ACQUIRED'; holderId: string; leaseVersion: number; fencingToken: number; expiresAtEpochSeconds: EpochSeconds; expectedCheckpointRevision: number }
  | ({ type: 'CALENDAR_APPLIED_OBSERVED' } & CalendarAttemptReference)
  | ({ type: 'CALENDAR_REMOVED_OBSERVED' } & CalendarAttemptReference)
  | ({ type: 'CALENDAR_RETRYABLE_FAILURE_OBSERVED'; jitterSample: number } & CalendarAttemptReference)
  | ({ type: 'CALENDAR_RESULT_PERSISTENCE_REQUESTED' } & CalendarCheckpointReference)
  | ({ type: 'CALENDAR_RESULT_DURABLY_RECORDED' } & CalendarCheckpointReference)
  | { type: 'CALENDAR_CLOCK_TICK'; expectedClockRevision: number; nowEpochSeconds: EpochSeconds }
  | { type: 'CALENDAR_RETRY_DUE' }
const exactCalendarAttempt = (context: CalendarContext, event: CalendarAttemptReference) => context.lease !== null && context.nowEpochSeconds < context.expiresAtEpochSeconds && context.lease.expiresAtEpochSeconds > context.nowEpochSeconds && event.calendarArtifactKey === context.calendarArtifactKey && event.correlationId === context.correlationId && event.attempt === context.attempt && event.holderId === context.lease.holderId && event.leaseVersion === context.lease.version && event.fencingToken === context.lease.fencingToken
const exactCalendarCheckpoint = (context: CalendarContext, event: CalendarCheckpointReference, requireEmission: boolean) => context.pendingCheckpoint !== null && event.effectId === context.pendingCheckpoint.effectId && event.checkpointRevision === context.pendingCheckpoint.revision && event.holderId === context.pendingCheckpoint.holderId && event.leaseVersion === context.pendingCheckpoint.leaseVersion && event.fencingToken === context.pendingCheckpoint.fencingToken && (context.pendingCheckpoint.holderId === null || (context.lease !== null && context.lease.expiresAtEpochSeconds > context.nowEpochSeconds && event.holderId === context.lease.holderId && event.leaseVersion === context.lease.version && event.fencingToken === context.lease.fencingToken)) && (!requireEmission || context.pendingCheckpoint.effectEmitted)
const calendarCheckpoint = (context: CalendarContext, outcome: CalendarCheckpointOutcome, values: Partial<CalendarCheckpoint> = {}): CalendarCheckpoint => {
  const revision = context.checkpointRevision + 1
  return { effectId: `${context.calendarArtifactKey}:${outcome}:r${revision}:f${context.lease?.fencingToken ?? 0}`, revision, outcome, effectEmitted: false, holderId: context.lease?.holderId ?? null, leaseVersion: context.lease?.version ?? null, fencingToken: context.lease?.fencingToken ?? null, nextAttempt: context.attempt, nextAttemptAtEpochSeconds: null, ...values }
}
export const notificationCalendarArtifactMachine = setup({
  types: { context: {} as CalendarContext, events: {} as CalendarEvent, input: {} as CalendarInput },
  guards: {
    calendarLeaseValid: ({ context, event }) => event.type === 'CALENDAR_LEASE_DURABLY_ACQUIRED' && event.holderId.trim().length > 0 && event.expectedCheckpointRevision === context.checkpointRevision && event.leaseVersion > context.lastLeaseVersion && event.fencingToken > context.lastFencingToken && event.expiresAtEpochSeconds > context.nowEpochSeconds && context.nowEpochSeconds < context.expiresAtEpochSeconds && (context.lease === null || context.lease.expiresAtEpochSeconds <= context.nowEpochSeconds),
    calendarRecoveryLeaseValid: ({ context, event }) => event.type === 'CALENDAR_LEASE_DURABLY_ACQUIRED' && context.pendingCheckpoint !== null && context.lease !== null && context.lease.expiresAtEpochSeconds <= context.nowEpochSeconds && event.holderId.trim().length > 0 && event.expectedCheckpointRevision === context.checkpointRevision && event.leaseVersion > context.lastLeaseVersion && event.fencingToken > context.lastFencingToken && event.expiresAtEpochSeconds > context.nowEpochSeconds && context.nowEpochSeconds < context.expiresAtEpochSeconds,
    exactCalendarObservation: ({ context, event }) => (event.type === 'CALENDAR_APPLIED_OBSERVED' || event.type === 'CALENDAR_REMOVED_OBSERVED' || event.type === 'CALENDAR_RETRYABLE_FAILURE_OBSERVED') && exactCalendarAttempt(context, event),
    exactCalendarRequest: ({ context, event }) => event.type === 'CALENDAR_RESULT_PERSISTENCE_REQUESTED' && exactCalendarCheckpoint(context, event, false),
    exactCalendarAck: ({ context, event }, params: { outcome: CalendarCheckpointOutcome }) => event.type === 'CALENDAR_RESULT_DURABLY_RECORDED' && exactCalendarCheckpoint(context, event, true) && context.pendingCheckpoint?.outcome === params.outcome,
    calendarClockValid: ({ context, event }) => event.type === 'CALENDAR_CLOCK_TICK' && event.expectedClockRevision === context.clockRevision && event.nowEpochSeconds >= context.nowEpochSeconds,
    calendarClockExpires: ({ context, event }) => event.type === 'CALENDAR_CLOCK_TICK' && event.expectedClockRevision === context.clockRevision && event.nowEpochSeconds >= context.expiresAtEpochSeconds && event.nowEpochSeconds >= context.nowEpochSeconds,
    calendarRetryDue: ({ context }) => context.nowEpochSeconds >= context.nextAttemptAtEpochSeconds && context.nowEpochSeconds < context.expiresAtEpochSeconds,
  },
  actions: {
    recordCalendarLease: assign(({ context, event }) => event.type !== 'CALENDAR_LEASE_DURABLY_ACQUIRED' ? {} : ({ lease: { holderId: event.holderId, version: event.leaseVersion, fencingToken: event.fencingToken, expiresAtEpochSeconds: event.expiresAtEpochSeconds }, lastLeaseVersion: event.leaseVersion, lastFencingToken: event.fencingToken, correlationId: `${context.calendarArtifactKey}:a${context.attempt}:v${event.leaseVersion}:f${event.fencingToken}` })),
    recoverCalendarCheckpoint: assign(({ context, event }) => {
      if (event.type !== 'CALENDAR_LEASE_DURABLY_ACQUIRED' || context.pendingCheckpoint === null) return {}
      const revision = context.checkpointRevision + 1
      return { lease: { holderId: event.holderId, version: event.leaseVersion, fencingToken: event.fencingToken, expiresAtEpochSeconds: event.expiresAtEpochSeconds }, lastLeaseVersion: event.leaseVersion, lastFencingToken: event.fencingToken, checkpointRevision: revision, pendingCheckpoint: { ...context.pendingCheckpoint, revision, effectId: `${context.calendarArtifactKey}:${context.pendingCheckpoint.outcome}:r${revision}:f${event.fencingToken}`, effectEmitted: false, holderId: event.holderId, leaseVersion: event.leaseVersion, fencingToken: event.fencingToken } }
    }),
    stageCalendarResult: assign(({ context, event }) => {
      if (event.type !== 'CALENDAR_APPLIED_OBSERVED' && event.type !== 'CALENDAR_REMOVED_OBSERVED' && event.type !== 'CALENDAR_RETRYABLE_FAILURE_OBSERVED') return {}
      if (event.type === 'CALENDAR_APPLIED_OBSERVED') { const checkpoint = calendarCheckpoint(context, 'upserted'); return { checkpointRevision: checkpoint.revision, pendingCheckpoint: checkpoint } }
      if (event.type === 'CALENDAR_REMOVED_OBSERVED') { const checkpoint = calendarCheckpoint(context, 'removed'); return { checkpointRevision: checkpoint.revision, pendingCheckpoint: checkpoint } }
      const nextAttempt = context.attempt + 1
      const nextAttemptAtEpochSeconds = epochSeconds(context.nowEpochSeconds + fullJitterBackoffSeconds(nextAttempt, event.jitterSample))
      const outcome: CalendarCheckpointOutcome = nextAttempt >= context.maxAttempts ? 'retryExhausted' : nextAttemptAtEpochSeconds >= context.expiresAtEpochSeconds ? 'expired' : 'retry'
      const checkpoint = calendarCheckpoint(context, outcome, { nextAttempt, nextAttemptAtEpochSeconds: outcome === 'retry' ? nextAttemptAtEpochSeconds : null })
      return { checkpointRevision: checkpoint.revision, pendingCheckpoint: checkpoint }
    }),
    stageCalendarExpiry: assign(({ context }) => { const checkpoint = calendarCheckpoint(context, 'expired'); return { checkpointRevision: checkpoint.revision, pendingCheckpoint: checkpoint } }),
    advanceCalendarClock: assign({ nowEpochSeconds: ({ event }) => event.type === 'CALENDAR_CLOCK_TICK' ? event.nowEpochSeconds : epochSeconds(0), clockRevision: ({ context }) => context.clockRevision + 1 }),
    emitCalendarEffect: assign({ pendingCheckpoint: ({ context }) => context.pendingCheckpoint === null ? null : { ...context.pendingCheckpoint, effectEmitted: true } }),
    clearCalendarCheckpoint: assign({ pendingCheckpoint: null }),
    commitCalendarRetry: assign(({ context }) => ({ attempt: context.pendingCheckpoint?.nextAttempt ?? context.attempt, nextAttemptAtEpochSeconds: context.pendingCheckpoint?.nextAttemptAtEpochSeconds ?? context.nextAttemptAtEpochSeconds, pendingCheckpoint: null, lease: null, correlationId: null })),
  },
}).createMachine({
  id: 'notificationCalendarArtifact', initial: 'queued',
  context: ({ input }) => ({ ...input, nowEpochSeconds: input.nowEpochSeconds ?? epochSeconds(0), expiresAtEpochSeconds: input.expiresAtEpochSeconds ?? epochSeconds(Number.MAX_SAFE_INTEGER), maxAttempts: input.maxAttempts ?? 3, nextAttemptAtEpochSeconds: input.nextAttemptAtEpochSeconds ?? epochSeconds(0), clockRevision: 0, attempt: 0, pendingCheckpoint: null, lease: null, lastLeaseVersion: 0, lastFencingToken: 0, correlationId: null }),
  states: {
    queued: { on: { CALENDAR_CLOCK_TICK: [{ guard: 'calendarClockExpires', target: 'awaitingCalendarResultPersistence', actions: ['advanceCalendarClock', 'stageCalendarExpiry'] }, { guard: 'calendarClockValid', actions: 'advanceCalendarClock' }], CALENDAR_LEASE_DURABLY_ACQUIRED: { guard: 'calendarLeaseValid', target: 'sending', actions: 'recordCalendarLease' } } },
    sending: { on: { CALENDAR_CLOCK_TICK: [{ guard: 'calendarClockExpires', target: 'awaitingCalendarResultPersistence', actions: ['advanceCalendarClock', 'stageCalendarExpiry'] }, { guard: 'calendarClockValid', actions: 'advanceCalendarClock' }], CALENDAR_APPLIED_OBSERVED: { guard: 'exactCalendarObservation', target: 'awaitingCalendarResultPersistence', actions: 'stageCalendarResult' }, CALENDAR_REMOVED_OBSERVED: { guard: 'exactCalendarObservation', target: 'awaitingCalendarResultPersistence', actions: 'stageCalendarResult' }, CALENDAR_RETRYABLE_FAILURE_OBSERVED: { guard: 'exactCalendarObservation', target: 'awaitingCalendarResultPersistence', actions: 'stageCalendarResult' } } },
    awaitingCalendarResultPersistence: { on: { CALENDAR_CLOCK_TICK: { guard: 'calendarClockValid', actions: 'advanceCalendarClock' }, CALENDAR_LEASE_DURABLY_ACQUIRED: { guard: 'calendarRecoveryLeaseValid', actions: 'recoverCalendarCheckpoint' }, CALENDAR_RESULT_PERSISTENCE_REQUESTED: { guard: 'exactCalendarRequest', actions: 'emitCalendarEffect' }, CALENDAR_RESULT_DURABLY_RECORDED: [{ guard: { type: 'exactCalendarAck', params: { outcome: 'upserted' } }, target: 'applied', actions: 'clearCalendarCheckpoint' }, { guard: { type: 'exactCalendarAck', params: { outcome: 'removed' } }, target: 'applied', actions: 'clearCalendarCheckpoint' }, { guard: { type: 'exactCalendarAck', params: { outcome: 'retry' } }, target: 'retry', actions: 'commitCalendarRetry' }, { guard: { type: 'exactCalendarAck', params: { outcome: 'expired' } }, target: 'expired', actions: 'clearCalendarCheckpoint' }, { guard: { type: 'exactCalendarAck', params: { outcome: 'retryExhausted' } }, target: 'retryExhausted', actions: 'clearCalendarCheckpoint' }] } },
    retry: { on: { CALENDAR_CLOCK_TICK: [{ guard: 'calendarClockExpires', target: 'awaitingCalendarResultPersistence', actions: ['advanceCalendarClock', 'stageCalendarExpiry'] }, { guard: 'calendarClockValid', actions: 'advanceCalendarClock' }], CALENDAR_RETRY_DUE: { guard: 'calendarRetryDue', target: 'queued' } } },
    applied: { type: 'final' }, expired: { type: 'final' }, retryExhausted: { type: 'final' },
  },
})

export const deliveryInvariants = [
  'notificationDeliveryMachine is the only provider-delivery transition authority',
  'transport and HTTP observations are correlated to delivery, attempt, correlation, lease holder, lease version and fence',
  'provider outcome is classified internally and never supplied by the caller',
  'no durable delivery result changes before explicit effect emission and exact durable acknowledgement',
  'fallback retry is full jitter in [1,300], overflow-safe, strictly future and before expiry',
  'policy, quiet-hours, token wait, queue, auth, send, retry and unknown outcome remain business-expirable',
  'may-have-written is durable recoverable unknownOutcome and preserves delivery and APNs identities',
  'pending target retry, fan-out and expiry are separate receipt-and-lease-fenced durable checkpoints',
  'one correlated provider credential refresh is permitted per send before the process credential circuit blocks',
  'cutover and rollback use a durable recovery lease plus pause, reconciliation and fenced commit effects',
  'calendar artifacts have an independent correlated leased retry and expiry lifecycle',
  'no persisted reason, state transition or authority depends on free text or an LLM',
] as const
export const notificationDeliveryDurabilityInvariants = deliveryInvariants
