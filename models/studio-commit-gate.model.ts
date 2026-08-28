export interface StudioCommitIdentity {
  operationId: string
  draftRevision: number
}

export type StudioCommitSubject =
  | { kind: 'NEW'; eventId: string }
  | { kind: 'EDIT_EXISTING'; eventId: string; baseRevision: number }

export type StudioArtworkCommitPayload =
  | { kind: 'NONE' }
  | { kind: 'KEEP_EXISTING' }
  | { kind: 'PRESET'; presetId: string }
  | { kind: 'EXISTING_SERVER_ASSET'; assetId: string; assetRevision: number }

export type StudioResultingArtwork = Exclude<StudioArtworkCommitPayload, { kind: 'KEEP_EXISTING' }>

export interface StudioCommitRequestPayload {
  schemaVersion: 1
  subject: StudioCommitSubject
  actorId: string
  draftRevision: number
  canonicalDraftJson: string
  artwork: StudioArtworkCommitPayload
  expectedResultingArtwork: StudioResultingArtwork
}

export interface StudioCommitEnvelope {
  identity: StudioCommitIdentity
  requestPayload: StudioCommitRequestPayload
  durableOperationRef: string
  requestFingerprint: string
  maxResolutionAttempts: number
  expectedResultingArtwork: StudioResultingArtwork
}

export interface StudioPendingSyncSubject {
  schemaVersion: 1
  eventId: string
  committedRevision: number
  localReceiptId: string
  envelope: StudioCommitEnvelope
  expectedResultingArtwork: StudioResultingArtwork
}

export interface StudioLocalCommitReceipt {
  receiptId: string
  eventId: string
  committedRevision: number
  commitEnvelope: StudioCommitEnvelope
  syncStatus: 'PENDING_SYNC'
  serverReceiptId: null
  pendingSubject: StudioPendingSyncSubject
}

export type StudioSyncError =
  | { code: 'NETWORK_UNAVAILABLE' | 'SERVER_UNAVAILABLE'; retryable: true }
  | {
      code: 'FORBIDDEN' | 'EVENT_NOT_DRAFT' | 'STALE_BASE_REVISION' |
        'IDEMPOTENCY_CONFLICT' | 'REPOSITORY_INCONSISTENT' | 'PERMANENT_FAILURE'
      retryable: false
    }

export interface StudioSyncFailureMetadata {
  localReceiptId: string
  eventId: string
  committedRevision: number
  durableOperationRef: string
  requestFingerprint: string
  status: 'RETRYABLE_FAILURE' | 'TERMINAL_FAILURE'
  error: StudioSyncError
}

export interface StudioSyncAck {
  localReceiptId: string
  serverReceiptId: string
  eventId: string
  committedRevision: number
  durableOperationRef: string
  requestFingerprint: string
  outcome: 'APPLIED' | 'ALREADY_APPLIED'
  disposition: 'CREATED' | 'UPDATED'
  artwork: StudioArtworkCommitPayload
}

export interface StudioServerAggregate {
  eventId: string
  organizerId: string
  status: 'DRAFT' | 'POLLING' | 'COMPARING' | 'CONFIRMED' | 'ORGANIZING' | 'FINALIZED'
  revision: number
  canonicalDraftJson: string
  artwork: StudioArtworkCommitPayload
}

export interface StudioServerCommitReceipt {
  serverReceiptId: string
  idempotencyKey: {
    durableOperationRef: string
    requestFingerprint: string
  }
  commitEnvelope: StudioCommitEnvelope
  ack: StudioSyncAck
}

export type StudioServerCommitResult =
  | {
      kind: 'APPLY_ATOMIC'
      writes: { aggregate: StudioServerAggregate; receipt: StudioServerCommitReceipt }
      ack: StudioSyncAck
    }
  | { kind: 'RETURN_EXISTING_ACK'; ack: StudioSyncAck; aggregateMutation: null }
  | {
      kind: 'REJECT'
      code: 'INVALID_COMMAND' | 'IDEMPOTENCY_CONFLICT' | 'STALE_BASE_REVISION' |
        'EVENT_ALREADY_EXISTS' | 'EVENT_NOT_FOUND' | 'EVENT_NOT_DRAFT' | 'FORBIDDEN' |
        'REPOSITORY_INCONSISTENT'
      retryable: false
    }

export type StudioServerReceiptRaceFinalization =
  | { kind: 'RETURN_EXISTING_ACK'; ack: StudioSyncAck; aggregateMutation: null }
  | {
      kind: 'REJECT'
      code: 'FORBIDDEN' | 'REPOSITORY_INCONSISTENT'
      retryable: false
    }

export interface StudioPreCommitFailure {
  code: 'LOCAL_PERSISTENCE_FAILED' | 'REPOSITORY_UNAVAILABLE' | 'PERMANENT_FAILURE'
  retryable: boolean
}

export interface StudioCommitOutcomeUnknownFailure {
  code: 'COMMIT_OUTCOME_UNKNOWN'
  retryable: boolean
}

export interface StudioResolutionOutcomeUnknownFailure {
  code: 'RESOLUTION_OUTCOME_UNKNOWN'
  retryable: boolean
}

export type StudioResolutionFailure =
  | {
      code: 'COMMIT_OUTCOME_UNKNOWN' | 'RESOLUTION_OUTCOME_UNKNOWN' |
        'RESOLUTION_EXHAUSTED' | 'PERMANENT_FAILURE'
      retryable: boolean
      commitOutcome?: 'UNKNOWN'
    }
  | {
      code: 'REPOSITORY_INCONSISTENT'
      retryable: false
      commitOutcome: 'UNKNOWN'
    }

export interface StudioResolutionAttemptIdentity {
  attemptId: string
  fence: number
}

export interface StudioRehydrateSession {
  attachment: 'ATTACHED' | 'DETACHED'
  eventId: string
  actorId: string
}

export type StudioCommitGateState =
  | { kind: 'IDLE' }
  | { kind: 'COMMITTING'; envelope: StudioCommitEnvelope }
  | { kind: 'FAILED_BEFORE_COMMIT'; envelope: StudioCommitEnvelope; failure: StudioPreCommitFailure }
  | {
      kind: 'DETACHED_COMMITTING'
      envelope: StudioCommitEnvelope
      resolutionAttempt: number
      lastFailure: StudioResolutionFailure | null
      lastAttempt: StudioResolutionAttemptIdentity | null
    }
  | {
      kind: 'DETACHED_RESOLVING'
      envelope: StudioCommitEnvelope
      resolutionAttempt: number
      lastFailure: StudioResolutionFailure
      attempt: StudioResolutionAttemptIdentity
    }
  | { kind: 'PENDING_SYNC'; envelope: StudioCommitEnvelope; receipt: StudioLocalCommitReceipt;
      navigationConsumed: boolean }
  | { kind: 'SYNC_FAILED'; envelope: StudioCommitEnvelope; receipt: StudioLocalCommitReceipt;
      metadata: StudioSyncFailureMetadata; navigationConsumed: boolean }
  | { kind: 'SYNC_TERMINAL_FAILURE'; envelope: StudioCommitEnvelope;
      receipt: StudioLocalCommitReceipt; metadata: StudioSyncFailureMetadata;
      navigationConsumed: boolean }
  | { kind: 'DETACHED_PENDING_SYNC'; envelope: StudioCommitEnvelope;
      receipt: StudioLocalCommitReceipt; navigationConsumed: boolean }
  | { kind: 'SYNCED'; envelope: StudioCommitEnvelope; receipt: StudioLocalCommitReceipt;
      serverReceiptId: string; navigationConsumed: boolean }
  | {
      kind: 'DETACHED_RESOLUTION_FAILED'
      envelope: StudioCommitEnvelope
      failure: StudioResolutionFailure
    }
  | {
      kind: 'REHYDRATION_FAILED'
      failure: Extract<StudioResolutionFailure, { code: 'REPOSITORY_INCONSISTENT' }>
    }
  | { kind: 'CLOSED' }

export type StudioCommitGateEvent =
  | { type: 'CONFIRM_COMMIT'; envelope: StudioCommitEnvelope }
  | { type: 'REHYDRATE_STUDIO_RECEIPT'; session: StudioRehydrateSession; record: unknown }
  | { type: 'LOCAL_COMMIT'; envelope: StudioCommitEnvelope; receipt: unknown }
  | { type: 'FAIL_BEFORE_COMMIT'; envelope: StudioCommitEnvelope; failure: StudioPreCommitFailure }
  | { type: 'RETRY_BEFORE_COMMIT'; envelope: StudioCommitEnvelope }
  | { type: 'CLOSE' }
  | { type: 'LATE_LOCAL_COMMIT'; envelope: StudioCommitEnvelope; receipt: unknown }
  | { type: 'PROVEN_NON_COMMIT'; envelope: StudioCommitEnvelope; proofRef: string }
  | {
      type: 'OUTCOME_UNKNOWN'
      envelope: StudioCommitEnvelope
      failure: StudioCommitOutcomeUnknownFailure
    }
  | {
      type: 'RETRY_RESOLUTION'
      envelope: StudioCommitEnvelope
      attempt: StudioResolutionAttemptIdentity
    }
  | {
      type: 'RESOLUTION_RESULT'
      envelope: StudioCommitEnvelope
      attempt: StudioResolutionAttemptIdentity
      outcome:
        | { kind: 'LOCAL_COMMIT'; receipt: unknown }
        | { kind: 'PROVEN_NON_COMMIT'; proofRef: string }
        | { kind: 'UNKNOWN'; failure: StudioResolutionOutcomeUnknownFailure }
    }
  | {
      type: 'RESOLUTION_TERMINAL_FAILURE'
      envelope: StudioCommitEnvelope
      attempt: StudioResolutionAttemptIdentity
      failure: StudioResolutionFailure
    }
  | { type: 'SYNC_ACK'; ack: StudioSyncAck }
  | { type: 'SYNC_FAILED'; metadata: StudioSyncFailureMetadata }
  | { type: 'RETRY_SYNC' }
  | { type: 'OPEN_COMMITTED_EVENT' }

export type StudioCommitGateEffect =
  | 'DELIVER_COMPLETION_CALLBACK'
  | 'NAVIGATE_TO_COMMITTED_EVENT'
  | 'RESOLVE_OUTCOME_IDEMPOTENTLY'
  | 'REJECT_COMMIT_ALREADY_IN_FLIGHT'
  | 'CONSUME_CLOSE_WITHOUT_CANCELLATION'
  | {
      type: 'PERSIST_DURABLE_ENVELOPE_THEN_INVOKE'
      durableEnvelope: StudioCommitEnvelope
    }
  | { type: 'TRIGGER_SYNC_IMMEDIATELY'; subject: StudioPendingSyncSubject; idempotent: true }
  | { type: 'RETRY_PENDING_SYNC_SUBJECT'; subject: StudioPendingSyncSubject }

export interface StudioCommitGateResult {
  state: StudioCommitGateState
  effects: StudioCommitGateEffect[]
}

const sameIdentity = (left: StudioCommitIdentity, right: StudioCommitIdentity): boolean =>
  left.operationId === right.operationId && left.draftRevision === right.draftRevision

const isNonBlankStableScalar = (value: unknown): value is string => {
  if (typeof value !== 'string' || value.length === 0 || value.trim() !== value) return false
  for (let index = 0; index < value.length; index += 1) {
    const unit = value.charCodeAt(index)
    if (unit >= 0xd800 && unit <= 0xdbff) {
      const next = value.charCodeAt(index + 1)
      if (!(next >= 0xdc00 && next <= 0xdfff)) return false
      index += 1
    } else if (unit >= 0xdc00 && unit <= 0xdfff) return false
  }
  return true
}

const utf8Hex = (value: string): string => Array.from(new TextEncoder().encode(value))
  .map((byte) => byte.toString(16).padStart(2, '0')).join('')

const canonicalStudioRequest = (payload: StudioCommitRequestPayload): string => JSON.stringify([
  payload.schemaVersion,
  payload.subject.kind,
  payload.subject.eventId,
  payload.subject.kind === 'EDIT_EXISTING' ? payload.subject.baseRevision : 'NOT_APPLICABLE',
  payload.actorId,
  payload.draftRevision,
  payload.canonicalDraftJson,
  payload.artwork.kind,
  payload.artwork.kind === 'PRESET' ? payload.artwork.presetId : null,
  payload.artwork.kind === 'EXISTING_SERVER_ASSET' ? payload.artwork.assetId : null,
  payload.artwork.kind === 'EXISTING_SERVER_ASSET' ? payload.artwork.assetRevision : null,
  payload.expectedResultingArtwork.kind,
  payload.expectedResultingArtwork.kind === 'PRESET'
    ? payload.expectedResultingArtwork.presetId : null,
  payload.expectedResultingArtwork.kind === 'EXISTING_SERVER_ASSET'
    ? payload.expectedResultingArtwork.assetId : null,
  payload.expectedResultingArtwork.kind === 'EXISTING_SERVER_ASSET'
    ? payload.expectedResultingArtwork.assetRevision : null,
])

export const studioRequestFingerprint = (payload: StudioCommitRequestPayload): string =>
  `studio-request:v1:${utf8Hex(canonicalStudioRequest(payload))}`

export const studioDurableOperationRef = (
  identity: StudioCommitIdentity,
  payload: StudioCommitRequestPayload,
): string => `studio-operation:v1:${utf8Hex(identity.operationId)}:${studioRequestFingerprint(payload)}`

export const buildStudioCommitEnvelope = (
  identity: StudioCommitIdentity,
  requestPayload: StudioCommitRequestPayload,
  maxResolutionAttempts: number,
): StudioCommitEnvelope => ({
  identity,
  requestPayload,
  durableOperationRef: studioDurableOperationRef(identity, requestPayload),
  requestFingerprint: studioRequestFingerprint(requestPayload),
  maxResolutionAttempts,
  expectedResultingArtwork: requestPayload.expectedResultingArtwork,
})

const validArtwork = (artwork: unknown): artwork is StudioArtworkCommitPayload => {
  if (typeof artwork !== 'object' || artwork === null || !('kind' in artwork)) return false
  const candidate = artwork as Partial<StudioArtworkCommitPayload>
  return candidate.kind === 'NONE' || candidate.kind === 'KEEP_EXISTING' ||
    (candidate.kind === 'PRESET' && 'presetId' in candidate &&
      isNonBlankStableScalar(candidate.presetId)) ||
    (candidate.kind === 'EXISTING_SERVER_ASSET' && 'assetId' in candidate &&
      isNonBlankStableScalar(candidate.assetId) && 'assetRevision' in candidate &&
      Number.isSafeInteger(candidate.assetRevision) && (candidate.assetRevision ?? -1) >= 0)
}

const validResultingArtwork = (artwork: unknown): artwork is StudioResultingArtwork =>
  validArtwork(artwork) && artwork.kind !== 'KEEP_EXISTING'

export const deriveStudioExpectedResultingArtwork = (
  choice: StudioArtworkCommitPayload,
  existingAggregateArtworkSnapshot: StudioResultingArtwork | null,
): StudioResultingArtwork | null => choice.kind === 'KEEP_EXISTING'
  ? existingAggregateArtworkSnapshot
  : choice

const sameArtwork = (
  left: StudioArtworkCommitPayload,
  right: StudioArtworkCommitPayload,
): boolean => left.kind === right.kind &&
  (left.kind !== 'PRESET' || (right.kind === 'PRESET' && left.presetId === right.presetId)) &&
  (left.kind !== 'EXISTING_SERVER_ASSET' || (right.kind === 'EXISTING_SERVER_ASSET' &&
    left.assetId === right.assetId && left.assetRevision === right.assetRevision))

const validEnvelope = (envelope: StudioCommitEnvelope): boolean => {
  try {
    return isNonBlankStableScalar(envelope.identity.operationId) &&
      Number.isSafeInteger(envelope.identity.draftRevision) && envelope.identity.draftRevision >= 0 &&
      envelope.requestPayload.schemaVersion === 1 &&
      isNonBlankStableScalar(envelope.requestPayload.subject.eventId) &&
      isNonBlankStableScalar(envelope.requestPayload.actorId) &&
      isNonBlankStableScalar(envelope.requestPayload.canonicalDraftJson) &&
      validArtwork(envelope.requestPayload.artwork) &&
      validResultingArtwork(envelope.requestPayload.expectedResultingArtwork) &&
      validResultingArtwork(envelope.expectedResultingArtwork) &&
      sameArtwork(envelope.expectedResultingArtwork,
        envelope.requestPayload.expectedResultingArtwork) &&
      (envelope.requestPayload.artwork.kind === 'KEEP_EXISTING' ||
        sameArtwork(envelope.requestPayload.artwork,
          envelope.requestPayload.expectedResultingArtwork)) &&
      Number.isSafeInteger(envelope.requestPayload.draftRevision) &&
      envelope.requestPayload.draftRevision === envelope.identity.draftRevision &&
      (envelope.requestPayload.subject.kind === 'NEW' ||
        (envelope.requestPayload.subject.kind === 'EDIT_EXISTING' &&
          Number.isSafeInteger(envelope.requestPayload.subject.baseRevision) &&
          envelope.requestPayload.subject.baseRevision >= 0)) &&
      !(envelope.requestPayload.subject.kind === 'NEW' &&
        envelope.requestPayload.artwork.kind === 'KEEP_EXISTING') &&
      envelope.requestFingerprint === studioRequestFingerprint(envelope.requestPayload) &&
      envelope.durableOperationRef === studioDurableOperationRef(
        envelope.identity, envelope.requestPayload) &&
      Number.isSafeInteger(envelope.maxResolutionAttempts) && envelope.maxResolutionAttempts > 0
  } catch {
    return false
  }
}

const sameEnvelope = (left: StudioCommitEnvelope, right: StudioCommitEnvelope): boolean =>
  validEnvelope(left) && validEnvelope(right) &&
  sameIdentity(left.identity, right.identity) &&
  left.durableOperationRef === right.durableOperationRef &&
  left.requestFingerprint === right.requestFingerprint &&
  canonicalStudioRequest(left.requestPayload) === canonicalStudioRequest(right.requestPayload) &&
  sameArtwork(left.expectedResultingArtwork, right.expectedResultingArtwork) &&
  left.maxResolutionAttempts === right.maxResolutionAttempts

export type StudioDurableEnvelopeProjection =
  | { kind: 'VALID'; envelope: StudioCommitEnvelope }
  | { kind: 'INCONSISTENT'; code: 'REPOSITORY_INCONSISTENT'; retryable: false;
      commitOutcome: 'UNKNOWN'; record: unknown }

export const projectStudioDurableEnvelope = (record: unknown): StudioDurableEnvelopeProjection => {
  try {
    if (typeof record !== 'object' || record === null ||
        !validEnvelope(record as StudioCommitEnvelope)) {
      return {
        kind: 'INCONSISTENT', code: 'REPOSITORY_INCONSISTENT', retryable: false,
        commitOutcome: 'UNKNOWN', record,
      }
    }
    return { kind: 'VALID', envelope: record as StudioCommitEnvelope }
  } catch {
    return {
      kind: 'INCONSISTENT', code: 'REPOSITORY_INCONSISTENT', retryable: false,
      commitOutcome: 'UNKNOWN', record,
    }
  }
}

const validLocalCommitReceipt = (
  envelope: StudioCommitEnvelope,
  receipt: unknown,
): receipt is StudioLocalCommitReceipt => {
  if (typeof receipt !== 'object' || receipt === null) return false
  const candidate = receipt as Partial<StudioLocalCommitReceipt>
  const subject = candidate.pendingSubject
  return isNonBlankStableScalar(candidate.receiptId) &&
    isNonBlankStableScalar(candidate.eventId) &&
    Number.isSafeInteger(candidate.committedRevision) && (candidate.committedRevision ?? -1) >= 0 &&
    candidate.syncStatus === 'PENDING_SYNC' && candidate.serverReceiptId === null &&
    typeof candidate.commitEnvelope === 'object' && candidate.commitEnvelope !== null &&
    sameEnvelope(envelope, candidate.commitEnvelope) && subject?.schemaVersion === 1 &&
    subject.eventId === candidate.eventId &&
    candidate.eventId === envelope.requestPayload.subject.eventId &&
    subject.committedRevision === candidate.committedRevision &&
    subject.localReceiptId === candidate.receiptId &&
    typeof subject.envelope === 'object' && subject.envelope !== null &&
    sameEnvelope(envelope, subject.envelope) &&
    validResultingArtwork(subject.expectedResultingArtwork) &&
    sameArtwork(subject.expectedResultingArtwork, envelope.expectedResultingArtwork)
}

const validRehydratedStudioReceipt = (
  session: StudioRehydrateSession,
  record: unknown,
): record is StudioLocalCommitReceipt => {
  if ((session.attachment !== 'ATTACHED' && session.attachment !== 'DETACHED') ||
      !isNonBlankStableScalar(session.eventId) ||
      !isNonBlankStableScalar(session.actorId) ||
      typeof record !== 'object' || record === null) return false
  const candidate = record as Partial<StudioLocalCommitReceipt>
  if (typeof candidate.commitEnvelope !== 'object' || candidate.commitEnvelope === null ||
      !validEnvelope(candidate.commitEnvelope)) return false
  return validLocalCommitReceipt(candidate.commitEnvelope, record) &&
    candidate.eventId === session.eventId &&
    candidate.commitEnvelope.requestPayload.subject.eventId === session.eventId &&
    candidate.commitEnvelope.requestPayload.actorId === session.actorId
}

const syncAckMatches = (
  receipt: StudioLocalCommitReceipt,
  ack: StudioSyncAck,
): boolean => isNonBlankStableScalar(ack.localReceiptId) &&
  isNonBlankStableScalar(ack.serverReceiptId) && ack.localReceiptId === receipt.receiptId &&
  ack.eventId === receipt.eventId && ack.committedRevision === receipt.committedRevision &&
  ack.durableOperationRef === receipt.commitEnvelope.durableOperationRef &&
  ack.requestFingerprint === receipt.commitEnvelope.requestFingerprint &&
  ack.disposition === (receipt.commitEnvelope.requestPayload.subject.kind === 'NEW'
    ? 'CREATED' : 'UPDATED') && validArtwork(ack.artwork) && ack.artwork.kind !== 'KEEP_EXISTING' &&
  sameArtwork(ack.artwork, receipt.commitEnvelope.expectedResultingArtwork) &&
  (ack.outcome === 'APPLIED' || ack.outcome === 'ALREADY_APPLIED')

const validStudioSyncError = (error: StudioSyncError): boolean =>
  error.retryable
    ? error.code === 'NETWORK_UNAVAILABLE' || error.code === 'SERVER_UNAVAILABLE'
    : error.code === 'FORBIDDEN' || error.code === 'EVENT_NOT_DRAFT' ||
      error.code === 'STALE_BASE_REVISION' || error.code === 'IDEMPOTENCY_CONFLICT' ||
      error.code === 'REPOSITORY_INCONSISTENT' || error.code === 'PERMANENT_FAILURE'

const syncFailureMatches = (
  receipt: StudioLocalCommitReceipt,
  metadata: StudioSyncFailureMetadata,
): boolean => isNonBlankStableScalar(metadata.localReceiptId) &&
  metadata.localReceiptId === receipt.receiptId && metadata.eventId === receipt.eventId &&
  metadata.committedRevision === receipt.committedRevision &&
  metadata.durableOperationRef === receipt.commitEnvelope.durableOperationRef &&
  metadata.requestFingerprint === receipt.commitEnvelope.requestFingerprint &&
  validStudioSyncError(metadata.error) &&
  ((metadata.status === 'RETRYABLE_FAILURE' && metadata.error.retryable) ||
    (metadata.status === 'TERMINAL_FAILURE' && !metadata.error.retryable))

export const studioSyncFailureFromServerReject = (
  receipt: StudioLocalCommitReceipt,
  rejection: Extract<StudioServerCommitResult, { kind: 'REJECT' }>,
): StudioSyncFailureMetadata => {
  const code: Extract<StudioSyncError, { retryable: false }>['code'] =
    rejection.code === 'FORBIDDEN' || rejection.code === 'EVENT_NOT_DRAFT' ||
    rejection.code === 'STALE_BASE_REVISION' || rejection.code === 'IDEMPOTENCY_CONFLICT' ||
    rejection.code === 'REPOSITORY_INCONSISTENT'
      ? rejection.code
      : 'PERMANENT_FAILURE'
  return {
    localReceiptId: receipt.receiptId,
    eventId: receipt.eventId,
    committedRevision: receipt.committedRevision,
    durableOperationRef: receipt.commitEnvelope.durableOperationRef,
    requestFingerprint: receipt.commitEnvelope.requestFingerprint,
    status: 'TERMINAL_FAILURE',
    error: { code, retryable: false },
  }
}

const studioServerReceiptMatchesSubject = (
  receipt: StudioServerCommitReceipt,
  subject: StudioPendingSyncSubject,
): boolean => isNonBlankStableScalar(receipt.serverReceiptId) &&
  receipt.idempotencyKey.durableOperationRef === subject.envelope.durableOperationRef &&
  receipt.idempotencyKey.requestFingerprint === subject.envelope.requestFingerprint &&
  sameEnvelope(receipt.commitEnvelope, subject.envelope) &&
  receipt.ack.localReceiptId === subject.localReceiptId &&
  receipt.ack.serverReceiptId === receipt.serverReceiptId &&
  receipt.ack.eventId === subject.eventId &&
  receipt.ack.committedRevision === subject.committedRevision &&
  receipt.ack.durableOperationRef === subject.envelope.durableOperationRef &&
  receipt.ack.requestFingerprint === subject.envelope.requestFingerprint &&
  receipt.ack.disposition === (subject.envelope.requestPayload.subject.kind === 'NEW'
    ? 'CREATED' : 'UPDATED') && validArtwork(receipt.ack.artwork) &&
  receipt.ack.artwork.kind !== 'KEEP_EXISTING' &&
  validResultingArtwork(subject.expectedResultingArtwork) &&
  sameArtwork(subject.expectedResultingArtwork, subject.envelope.expectedResultingArtwork) &&
  sameArtwork(receipt.ack.artwork, subject.expectedResultingArtwork) &&
  (receipt.ack.outcome === 'APPLIED' || receipt.ack.outcome === 'ALREADY_APPLIED')

/**
 * Server-side decision for the typed Studio sync owner. APPLY_ATOMIC means the aggregate
 * (including artwork) and operation receipt are one transaction; replay never remutates it.
 */
export const applyStudioServerCommit = (
  authenticatedActorId: string,
  subject: StudioPendingSyncSubject,
  currentAggregate: StudioServerAggregate | null,
  existingReceiptForKey: StudioServerCommitReceipt | null,
): StudioServerCommitResult => {
  const envelope = subject.envelope
  const incomingKey = {
    durableOperationRef: envelope.durableOperationRef,
    requestFingerprint: envelope.requestFingerprint,
  }
  if (!isNonBlankStableScalar(authenticatedActorId) ||
      !isNonBlankStableScalar(envelope.requestPayload.actorId)) {
    return { kind: 'REJECT', code: 'INVALID_COMMAND', retryable: false }
  }
  if (authenticatedActorId !== envelope.requestPayload.actorId) {
    return { kind: 'REJECT', code: 'FORBIDDEN', retryable: false }
  }
  if (existingReceiptForKey !== null) {
    const keyMatches = existingReceiptForKey.idempotencyKey.durableOperationRef ===
        incomingKey.durableOperationRef &&
      existingReceiptForKey.idempotencyKey.requestFingerprint === incomingKey.requestFingerprint
    if (!keyMatches) {
      return { kind: 'REJECT', code: 'REPOSITORY_INCONSISTENT', retryable: false }
    }
    if (!sameEnvelope(existingReceiptForKey.commitEnvelope, envelope)) {
      return { kind: 'REJECT', code: 'IDEMPOTENCY_CONFLICT', retryable: false }
    }
    if (!studioServerReceiptMatchesSubject(existingReceiptForKey, subject)) {
      return { kind: 'REJECT', code: 'REPOSITORY_INCONSISTENT', retryable: false }
    }
    return {
      kind: 'RETURN_EXISTING_ACK', ack: existingReceiptForKey.ack, aggregateMutation: null,
    }
  }

  if (!validEnvelope(envelope) || subject.schemaVersion !== 1 ||
      !isNonBlankStableScalar(subject.eventId) ||
      !isNonBlankStableScalar(subject.localReceiptId) ||
      !Number.isSafeInteger(subject.committedRevision) || subject.committedRevision < 0 ||
      subject.eventId !== envelope.requestPayload.subject.eventId ||
      !validResultingArtwork(subject.expectedResultingArtwork) ||
      !sameArtwork(subject.expectedResultingArtwork, envelope.expectedResultingArtwork)) {
    return { kind: 'REJECT', code: 'INVALID_COMMAND', retryable: false }
  }

  const request = envelope.requestPayload
  let resultingRevision: number
  let resultingArtwork: StudioArtworkCommitPayload
  if (request.subject.kind === 'NEW') {
    if (currentAggregate !== null) {
      return { kind: 'REJECT', code: 'EVENT_ALREADY_EXISTS', retryable: false }
    }
    if (request.artwork.kind === 'KEEP_EXISTING') {
      return { kind: 'REJECT', code: 'INVALID_COMMAND', retryable: false }
    }
    resultingRevision = 1
    resultingArtwork = request.artwork
  } else {
    if (currentAggregate === null || currentAggregate.eventId !== request.subject.eventId) {
      return { kind: 'REJECT', code: 'EVENT_NOT_FOUND', retryable: false }
    }
    if (!isNonBlankStableScalar(currentAggregate.eventId) ||
        !isNonBlankStableScalar(currentAggregate.organizerId) ||
        !Number.isSafeInteger(currentAggregate.revision) || currentAggregate.revision < 0 ||
        currentAggregate.revision === Number.MAX_SAFE_INTEGER ||
        !isNonBlankStableScalar(currentAggregate.canonicalDraftJson) ||
        !validArtwork(currentAggregate.artwork) || currentAggregate.artwork.kind === 'KEEP_EXISTING') {
      return { kind: 'REJECT', code: 'REPOSITORY_INCONSISTENT', retryable: false }
    }
    if (currentAggregate.organizerId !== authenticatedActorId) {
      return { kind: 'REJECT', code: 'FORBIDDEN', retryable: false }
    }
    if (currentAggregate.status !== 'DRAFT') {
      return { kind: 'REJECT', code: 'EVENT_NOT_DRAFT', retryable: false }
    }
    if (request.subject.baseRevision !== currentAggregate.revision) {
      return { kind: 'REJECT', code: 'STALE_BASE_REVISION', retryable: false }
    }
    resultingRevision = currentAggregate.revision + 1
    resultingArtwork = request.artwork.kind === 'KEEP_EXISTING'
      ? currentAggregate.artwork
      : request.artwork
  }
  if (!sameArtwork(resultingArtwork, envelope.expectedResultingArtwork)) {
    return { kind: 'REJECT', code: 'REPOSITORY_INCONSISTENT', retryable: false }
  }
  if (subject.committedRevision !== resultingRevision) {
    return { kind: 'REJECT', code: 'INVALID_COMMAND', retryable: false }
  }

  const aggregate: StudioServerAggregate = {
    eventId: subject.eventId,
    organizerId: authenticatedActorId,
    status: 'DRAFT',
    revision: resultingRevision,
    canonicalDraftJson: request.canonicalDraftJson,
    artwork: resultingArtwork,
  }
  const serverReceiptId = `studio-server-receipt:v1:${utf8Hex(
    `${incomingKey.durableOperationRef}|${incomingKey.requestFingerprint}`)}`
  const ack: StudioSyncAck = {
    localReceiptId: subject.localReceiptId,
    serverReceiptId,
    eventId: subject.eventId,
    committedRevision: resultingRevision,
    durableOperationRef: incomingKey.durableOperationRef,
    requestFingerprint: incomingKey.requestFingerprint,
    outcome: 'APPLIED',
    disposition: request.subject.kind === 'NEW' ? 'CREATED' : 'UPDATED',
    artwork: resultingArtwork,
  }
  return {
    kind: 'APPLY_ATOMIC',
    writes: {
      aggregate,
      receipt: { serverReceiptId, idempotencyKey: incomingKey, commitEnvelope: envelope, ack },
    },
    ack,
  }
}

/**
 * Finalizes a unique-receipt collision inside the repository transaction. The losing
 * instance must re-read the non-null winner there; no aggregate state or generic result
 * participates after a winner has committed.
 */
export const finalizeStudioServerReceiptRace = (
  authenticatedActorId: string,
  subject: StudioPendingSyncSubject,
  winnerReceiptReadInsideTransaction: StudioServerCommitReceipt,
): StudioServerReceiptRaceFinalization => {
  if (!isNonBlankStableScalar(authenticatedActorId) ||
      authenticatedActorId !== subject.envelope.requestPayload.actorId) {
    return { kind: 'REJECT', code: 'FORBIDDEN', retryable: false }
  }
  if (!studioServerReceiptMatchesSubject(winnerReceiptReadInsideTransaction, subject)) {
    return { kind: 'REJECT', code: 'REPOSITORY_INCONSISTENT', retryable: false }
  }
  return {
    kind: 'RETURN_EXISTING_ACK',
    ack: winnerReceiptReadInsideTransaction.ack,
    aggregateMutation: null,
  }
}

const validResolutionAttempt = (attempt: StudioResolutionAttemptIdentity): boolean =>
  attempt.attemptId.trim().length > 0 && Number.isSafeInteger(attempt.fence) && attempt.fence >= 0

const sameResolutionAttempt = (
  left: StudioResolutionAttemptIdentity,
  right: StudioResolutionAttemptIdentity,
): boolean => left.attemptId === right.attemptId && left.fence === right.fence

const terminalResolutionFailure = (
  state: Extract<StudioCommitGateState, { kind: 'DETACHED_COMMITTING' | 'DETACHED_RESOLVING' }>,
  failure: StudioResolutionFailure,
): StudioCommitGateResult => ({
  state: { kind: 'DETACHED_RESOLUTION_FAILED', envelope: state.envelope, failure },
  effects: [],
})

export const reduceStudioCommitGate = (
  state: StudioCommitGateState,
  event: StudioCommitGateEvent,
): StudioCommitGateResult => {
  if (event.type === 'REHYDRATE_STUDIO_RECEIPT') {
    if (state.kind !== 'IDLE') return { state, effects: [] }
    if (!validRehydratedStudioReceipt(event.session, event.record)) {
      return {
        state: {
          kind: 'REHYDRATION_FAILED',
          failure: {
            code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
          },
        },
        effects: [],
      }
    }
    const receipt = event.record
    return {
      state: {
        kind: event.session.attachment === 'ATTACHED'
          ? 'PENDING_SYNC'
          : 'DETACHED_PENDING_SYNC',
        envelope: receipt.commitEnvelope,
        receipt,
        navigationConsumed: false,
      },
      effects: [{
        type: 'TRIGGER_SYNC_IMMEDIATELY', subject: receipt.pendingSubject, idempotent: true,
      }],
    }
  }

  if (state.kind === 'IDLE') {
    return event.type === 'CONFIRM_COMMIT' && validEnvelope(event.envelope)
      ? {
          state: { kind: 'COMMITTING', envelope: event.envelope },
          effects: [{
            type: 'PERSIST_DURABLE_ENVELOPE_THEN_INVOKE',
            durableEnvelope: event.envelope,
          }],
        }
      : { state, effects: [] }
  }

  if (state.kind === 'COMMITTING') {
    if (event.type === 'CONFIRM_COMMIT') {
      return sameEnvelope(state.envelope, event.envelope)
        ? { state, effects: [] }
        : { state, effects: ['REJECT_COMMIT_ALREADY_IN_FLIGHT'] }
    }
    if (event.type === 'LOCAL_COMMIT' && sameEnvelope(state.envelope, event.envelope) &&
        validLocalCommitReceipt(state.envelope, event.receipt)) {
      return {
        state: {
          kind: 'PENDING_SYNC', envelope: state.envelope, receipt: event.receipt,
          navigationConsumed: true,
        },
        effects: [
          'DELIVER_COMPLETION_CALLBACK', 'NAVIGATE_TO_COMMITTED_EVENT',
          {
            type: 'TRIGGER_SYNC_IMMEDIATELY', subject: event.receipt.pendingSubject,
            idempotent: true,
          },
        ],
      }
    }
    if (event.type === 'LOCAL_COMMIT' && sameEnvelope(state.envelope, event.envelope)) {
      return {
        state: {
          kind: 'DETACHED_RESOLUTION_FAILED', envelope: state.envelope,
          failure: {
            code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
          },
        },
        effects: [],
      }
    }
    if (event.type === 'FAIL_BEFORE_COMMIT' && sameEnvelope(state.envelope, event.envelope)) {
      return {
        state: { kind: 'FAILED_BEFORE_COMMIT', envelope: state.envelope, failure: event.failure },
        effects: [],
      }
    }
    if (event.type === 'OUTCOME_UNKNOWN' &&
        sameEnvelope(state.envelope, event.envelope)) {
      return event.failure.retryable
        ? {
            state: {
              kind: 'DETACHED_COMMITTING', envelope: state.envelope,
              resolutionAttempt: 0, lastFailure: event.failure, lastAttempt: null,
            },
            effects: [],
          }
        : {
            state: {
              kind: 'DETACHED_RESOLUTION_FAILED', envelope: state.envelope,
              failure: event.failure,
            },
            effects: [],
          }
    }
    if (event.type === 'CLOSE') {
      return {
        state: {
          kind: 'DETACHED_COMMITTING', envelope: state.envelope,
          resolutionAttempt: 0, lastFailure: null, lastAttempt: null,
        },
        effects: ['CONSUME_CLOSE_WITHOUT_CANCELLATION'],
      }
    }
    return { state, effects: [] }
  }

  if (state.kind === 'FAILED_BEFORE_COMMIT') {
    if (event.type === 'RETRY_BEFORE_COMMIT' &&
        sameEnvelope(state.envelope, event.envelope) && state.failure.retryable) {
      return {
        state: { kind: 'COMMITTING', envelope: state.envelope },
        effects: [{
          type: 'PERSIST_DURABLE_ENVELOPE_THEN_INVOKE',
          durableEnvelope: state.envelope,
        }],
      }
    }
    if (event.type === 'CLOSE') return { state: { kind: 'CLOSED' }, effects: [] }
    return { state, effects: [] }
  }

  if (state.kind === 'DETACHED_COMMITTING') {
    if (event.type === 'CLOSE') {
      return { state, effects: ['CONSUME_CLOSE_WITHOUT_CANCELLATION'] }
    }
    if (event.type === 'LATE_LOCAL_COMMIT' && sameEnvelope(state.envelope, event.envelope) &&
        validLocalCommitReceipt(state.envelope, event.receipt)) {
      return {
        state: {
          kind: 'DETACHED_PENDING_SYNC', envelope: state.envelope,
          receipt: event.receipt, navigationConsumed: false,
        },
        effects: [{
          type: 'TRIGGER_SYNC_IMMEDIATELY', subject: event.receipt.pendingSubject,
          idempotent: true,
        }],
      }
    }
    if (event.type === 'LATE_LOCAL_COMMIT' && sameEnvelope(state.envelope, event.envelope)) {
      return terminalResolutionFailure(state, {
        code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
      })
    }
    if (event.type === 'PROVEN_NON_COMMIT' &&
        sameEnvelope(state.envelope, event.envelope) &&
        event.proofRef.trim().length > 0) {
      return { state: { kind: 'CLOSED' }, effects: [] }
    }
    if (event.type === 'OUTCOME_UNKNOWN' && state.resolutionAttempt === 0 &&
        event.failure.code === 'COMMIT_OUTCOME_UNKNOWN' &&
        sameEnvelope(state.envelope, event.envelope)) {
      if (!event.failure.retryable || state.resolutionAttempt >= state.envelope.maxResolutionAttempts) {
        return terminalResolutionFailure(state, event.failure)
      }
      return { state: { ...state, lastFailure: event.failure }, effects: [] }
    }
    if (event.type === 'RETRY_RESOLUTION' &&
        sameEnvelope(state.envelope, event.envelope) && state.lastFailure?.retryable &&
        validResolutionAttempt(event.attempt) &&
        event.attempt.fence === state.resolutionAttempt + 1 &&
        event.attempt.attemptId !== state.lastAttempt?.attemptId) {
      if (state.resolutionAttempt >= state.envelope.maxResolutionAttempts) {
        return terminalResolutionFailure(state, {
          code: 'RESOLUTION_EXHAUSTED', retryable: false,
        })
      }
      return {
        state: {
          kind: 'DETACHED_RESOLVING', envelope: state.envelope,
          resolutionAttempt: state.resolutionAttempt + 1,
          lastFailure: state.lastFailure,
          attempt: event.attempt,
        },
        effects: ['RESOLVE_OUTCOME_IDEMPOTENTLY'],
      }
    }
    return { state, effects: [] }
  }

  if (state.kind === 'DETACHED_RESOLVING') {
    if (event.type === 'CLOSE') {
      return { state, effects: ['CONSUME_CLOSE_WITHOUT_CANCELLATION'] }
    }
    if (event.type === 'LATE_LOCAL_COMMIT' && sameEnvelope(state.envelope, event.envelope) &&
        validLocalCommitReceipt(state.envelope, event.receipt)) {
      return {
        state: {
          kind: 'DETACHED_PENDING_SYNC', envelope: state.envelope,
          receipt: event.receipt, navigationConsumed: false,
        },
        effects: [{
          type: 'TRIGGER_SYNC_IMMEDIATELY', subject: event.receipt.pendingSubject,
          idempotent: true,
        }],
      }
    }
    if (event.type === 'LATE_LOCAL_COMMIT' && sameEnvelope(state.envelope, event.envelope)) {
      return terminalResolutionFailure(state, {
        code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
      })
    }
    if (event.type === 'RESOLUTION_RESULT' &&
        sameEnvelope(state.envelope, event.envelope) &&
        sameResolutionAttempt(state.attempt, event.attempt)) {
      if (event.outcome.kind === 'LOCAL_COMMIT' &&
          validLocalCommitReceipt(state.envelope, event.outcome.receipt)) {
        return {
          state: {
            kind: 'DETACHED_PENDING_SYNC', envelope: state.envelope,
            receipt: event.outcome.receipt, navigationConsumed: false,
          },
          effects: [{
            type: 'TRIGGER_SYNC_IMMEDIATELY', subject: event.outcome.receipt.pendingSubject,
            idempotent: true,
          }],
        }
      }
      if (event.outcome.kind === 'LOCAL_COMMIT') {
        return terminalResolutionFailure(state, {
          code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
        })
      }
      if (event.outcome.kind === 'PROVEN_NON_COMMIT' && event.outcome.proofRef.trim().length > 0) {
        return { state: { kind: 'CLOSED' }, effects: [] }
      }
      if (event.outcome.kind === 'UNKNOWN' &&
          event.outcome.failure.code === 'RESOLUTION_OUTCOME_UNKNOWN') {
        if (!event.outcome.failure.retryable ||
            state.resolutionAttempt >= state.envelope.maxResolutionAttempts) {
          return terminalResolutionFailure(state, { ...event.outcome.failure, retryable: false })
        }
        return {
          state: {
            kind: 'DETACHED_COMMITTING', envelope: state.envelope,
            resolutionAttempt: state.resolutionAttempt, lastFailure: event.outcome.failure,
            lastAttempt: state.attempt,
          },
          effects: [],
        }
      }
    }
    if (event.type === 'RESOLUTION_TERMINAL_FAILURE' &&
        sameEnvelope(state.envelope, event.envelope) &&
        sameResolutionAttempt(state.attempt, event.attempt)) {
      return terminalResolutionFailure(state, { ...event.failure, retryable: false })
    }
    return { state, effects: [] }
  }

  if ((state.kind === 'PENDING_SYNC' || state.kind === 'SYNC_FAILED' ||
      state.kind === 'SYNC_TERMINAL_FAILURE' || state.kind === 'DETACHED_PENDING_SYNC' ||
      state.kind === 'SYNCED') &&
      event.type === 'OPEN_COMMITTED_EVENT' && !state.navigationConsumed) {
    return {
      state: { ...state, navigationConsumed: true },
      effects: ['NAVIGATE_TO_COMMITTED_EVENT'],
    }
  }

  if ((state.kind === 'PENDING_SYNC' || state.kind === 'SYNC_FAILED' ||
      state.kind === 'DETACHED_PENDING_SYNC') && event.type === 'SYNC_ACK' &&
      syncAckMatches(state.receipt, event.ack)) {
    return {
      state: {
        kind: 'SYNCED', envelope: state.envelope, receipt: state.receipt,
        serverReceiptId: event.ack.serverReceiptId,
        navigationConsumed: state.navigationConsumed,
      },
      effects: [],
    }
  }

  if ((state.kind === 'PENDING_SYNC' || state.kind === 'DETACHED_PENDING_SYNC') &&
      event.type === 'SYNC_FAILED' && syncFailureMatches(state.receipt, event.metadata)) {
    if (event.metadata.status === 'TERMINAL_FAILURE') {
      return {
        state: {
          kind: 'SYNC_TERMINAL_FAILURE', envelope: state.envelope, receipt: state.receipt,
          metadata: event.metadata, navigationConsumed: state.navigationConsumed,
        },
        effects: [],
      }
    }
    return {
      state: {
        kind: 'SYNC_FAILED', envelope: state.envelope, receipt: state.receipt,
        metadata: event.metadata, navigationConsumed: state.navigationConsumed,
      },
      effects: [],
    }
  }

  if (state.kind === 'SYNC_FAILED' && event.type === 'RETRY_SYNC' &&
      state.metadata.error.retryable) {
    return {
      state: {
        kind: 'PENDING_SYNC', envelope: state.envelope, receipt: state.receipt,
        navigationConsumed: state.navigationConsumed,
      },
      effects: [{ type: 'RETRY_PENDING_SYNC_SUBJECT', subject: state.receipt.pendingSubject }],
    }
  }

  if (state.kind === 'PENDING_SYNC' && event.type === 'CLOSE') {
    return {
      state: {
        kind: 'DETACHED_PENDING_SYNC', envelope: state.envelope, receipt: state.receipt,
        navigationConsumed: state.navigationConsumed,
      },
      effects: [],
    }
  }

  return { state, effects: [] }
}

export interface StudioPendingSyncBinding {
  localReceiptId: string
  subject: StudioPendingSyncSubject
}

export const isStudioPendingSync = (state: StudioCommitGateState): boolean =>
  state.kind === 'PENDING_SYNC' || state.kind === 'DETACHED_PENDING_SYNC' ||
  state.kind === 'SYNC_FAILED'

export const studioPendingSyncBinding = (
  state: StudioCommitGateState,
): StudioPendingSyncBinding | null => isStudioPendingSync(state) && 'receipt' in state
  ? { localReceiptId: state.receipt.receiptId, subject: state.receipt.pendingSubject }
  : null

export const studioCommitGateInvariants = [
  'A commit starts only when durableOperationRef and requestFingerprint recompute from the same complete requestPayload; the full envelope is persisted and read as one record.',
  'FAILED_BEFORE_COMMIT, DETACHED_COMMITTING, and DETACHED_RESOLVING retain the exact durable operation envelope.',
  'The first valid CONFIRM_COMMIT emits one typed owner effect that durably persists the complete envelope before invoking that same request; a duplicate is coalesced or rejected.',
  'Retry before commit and outcome resolution require the same identity, durable operation reference, request fingerprint, request payload, and attempt bound.',
  'Close during commit detaches presentation without cancelling or losing the durable operation.',
  'Late local commit preserves durable truth as DETACHED_PENDING_SYNC without automatic navigation; proven non-commit alone may close.',
  'DETACHED_RESOLVING carries one attemptId/fence; each accepted fence equals the previous attempt count plus one and reusing the last attempt id is forbidden.',
  'A second retry is inert until one matching result returns; stale results cannot unlock the next strictly monotone attempt, and exhaustion is terminal.',
  'Close is explicitly consumed in COMMITTING and both detached in-flight states without cancelling or dropping the durable envelope.',
  'Initial commit-outcome unknown and resolution-attempt unknown are distinct closed failure codes.',
  'A pre-commit failure has a disjoint type from every unknown/resolution failure; attached commit uncertainty transfers to durable detached resolution and is never reported as a pre-commit failure.',
  'Only the first matching attached completion delivers the callback, and navigation is consumed once.',
  'PENDING_SYNC is proof of a receipt with non-null exact commitEnvelope and pendingSubject, never an UNKNOWN outcome; entering it emits idempotent TRIGGER_SYNC_IMMEDIATELY, so observation alone cannot strand sync.',
  'Only an ACK correlated by local receipt, server receipt, event, committed revision, durable operation reference, request fingerprint, disposition, and artwork reaches SYNCED; generic batch success or conflict is inert.',
  'expectedResultingArtwork is fingerprinted in the durable envelope and repeated exactly in the pending subject; ACK, replay receipt, and race winner artwork must equal it for every artwork mode, including KEEP_EXISTING.',
  'A fully correlated retryable sync failure records RETRYABLE_FAILURE; a correlated forbidden, non-draft, stale-revision, or permanent rejection records terminal metadata and exits pending sync without authorizing retry.',
  'Studio sync is delegated to the typed sync owner with the pending subject; no model effect authorizes manual SQL or re-running event creation.',
  'Outer-current resolution decodes and correlates the complete inner envelope and subject; malformed or divergent inner data becomes non-retryable REPOSITORY_INCONSISTENT with UNKNOWN outcome.',
  'Repository-corrupt resolution enters terminal DETACHED_RESOLUTION_FAILED directly and can never fall back to retryable DETACHED_COMMITTING.',
  'IDLE rehydration accepts only a fully correlated pending receipt for the session event and actor; it restores attached or detached pending sync without navigation and emits one idempotent immediate sync trigger.',
  'A detached rehydrated PENDING_SYNC receipt remains DETACHED_PENDING_SYNC with its loaded binding and isStudioPendingSync true; it is not a committed terminal projection.',
  'Malformed, foreign, or divergent Studio rehydration data is terminal REPOSITORY_INCONSISTENT/UNKNOWN; replay after successful restoration is inert.',
  'The server idempotency key is durableOperationRef plus requestFingerprint; exact replay reads the final receipt before mutable aggregate guards and returns its stored ACK byte-for-byte without mutation.',
  'Concurrent server instances insert the receipt under one transactionally unique key; a loser re-reads the non-null winner inside the transaction finalizer and returns only its exact ACK or repository inconsistency.',
  'Server EDIT compares baseRevision to current revision in the atomic transaction; aggregate scalars, artwork, final receipt, and ACK commit together with no aggregate-only crash state, and ACK carries the actual resulting revision.',
  'Server authorization uses the trusted authenticated actor: creation assigns that exact organizer, while edit requires the same envelope actor, aggregate organizer, and DRAFT status before any write.',
] as const

export interface SemanticControlAffordance {
  visible: true
  isEnabled: boolean
  semanticState: 'ENABLED' | 'DISABLED'
  voiceOverState: 'ENABLED' | 'DISABLED'
  disabledReason: 'IN_FLIGHT' | 'CLOSED_CHOICE' | 'NOT_AVAILABLE' | null
}

export const projectCreationSubmitAffordance = (
  state: StudioCommitGateState,
): SemanticControlAffordance => {
  if (state.kind === 'COMMITTING') return {
      visible: true, isEnabled: false, semanticState: 'DISABLED',
      voiceOverState: 'DISABLED', disabledReason: 'IN_FLIGHT',
    }
  if (state.kind === 'IDLE') return {
      visible: true, isEnabled: true, semanticState: 'ENABLED',
      voiceOverState: 'ENABLED', disabledReason: null,
    }
  return {
    visible: true, isEnabled: false, semanticState: 'DISABLED',
    voiceOverState: 'DISABLED', disabledReason: 'NOT_AVAILABLE',
  }
}

export const projectStudioChoiceAffordance = (
  availability: 'OPEN' | 'CLOSED',
): SemanticControlAffordance => availability === 'CLOSED'
  ? {
      visible: true, isEnabled: false, semanticState: 'DISABLED',
      voiceOverState: 'DISABLED', disabledReason: 'CLOSED_CHOICE',
    }
  : {
      visible: true, isEnabled: true, semanticState: 'ENABLED',
      voiceOverState: 'ENABLED', disabledReason: null,
    }
