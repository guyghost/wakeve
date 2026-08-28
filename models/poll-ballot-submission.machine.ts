import { assign, setup } from 'xstate'

export type VoteChoice = 'YES' | 'MAYBE' | 'NO'

export interface BallotEntry {
  slotId: string
  choice: VoteChoice
}

export type PollVoteAccessDecision =
  | { kind: 'ALLOWED'; basis: 'ORGANIZER' | 'ACCEPTED_ACTIVE_MEMBER' }
  | {
      kind: 'DENIED'
      reason: 'IDENTITY_MISMATCH' | 'NON_MEMBER' | 'INACTIVE_MEMBERSHIP' |
        'RSVP_NOT_ACCEPTED' | 'ACCESS_UNAVAILABLE'
    }

export interface PollVoteAccessInput {
  actorId: string
  eventOrganizerId: string
  accessUserId: string
  role: 'ORGANIZER' | 'MEMBER' | 'NON_MEMBER'
  membership: 'ACTIVE_MEMBER' | 'NON_MEMBER' | 'LEFT' | 'REMOVED' | 'UNAVAILABLE'
  rsvp: 'ACCEPTED' | 'PENDING' | 'DECLINED' | 'NOT_APPLICABLE' | 'UNAVAILABLE'
}

export const derivePollVoteAccess = (input: PollVoteAccessInput): PollVoteAccessDecision => {
  if ([input.actorId, input.eventOrganizerId, input.accessUserId].some((identifier) =>
    identifier.trim().length === 0 || identifier.trim() !== identifier)) {
    return { kind: 'DENIED', reason: 'IDENTITY_MISMATCH' }
  }
  if (input.actorId !== input.accessUserId) {
    return { kind: 'DENIED', reason: 'IDENTITY_MISMATCH' }
  }
  if (input.role === 'ORGANIZER') {
    return input.actorId === input.eventOrganizerId
      ? { kind: 'ALLOWED', basis: 'ORGANIZER' }
      : { kind: 'DENIED', reason: 'IDENTITY_MISMATCH' }
  }
  if (input.role !== 'MEMBER') return { kind: 'DENIED', reason: 'NON_MEMBER' }
  if (input.membership === 'UNAVAILABLE') return { kind: 'DENIED', reason: 'ACCESS_UNAVAILABLE' }
  if (input.membership !== 'ACTIVE_MEMBER') return { kind: 'DENIED', reason: 'INACTIVE_MEMBERSHIP' }
  if (input.rsvp === 'UNAVAILABLE') return { kind: 'DENIED', reason: 'ACCESS_UNAVAILABLE' }
  return input.rsvp === 'ACCEPTED'
    ? { kind: 'ALLOWED', basis: 'ACCEPTED_ACTIVE_MEMBER' }
    : { kind: 'DENIED', reason: 'RSVP_NOT_ACCEPTED' }
}

export const actorCanVote = (decision: PollVoteAccessDecision): boolean =>
  decision.kind === 'ALLOWED'

export interface VotingEligibility {
  repositoryAvailable: boolean
  eventExists: boolean
  voteAccess: PollVoteAccessDecision
  eventStatus: 'POLLING' | 'DRAFT' | 'COMPARING' | 'CONFIRMED' | 'ORGANIZING' | 'FINALIZED'
}

export type BallotFailureCode =
  | 'REPOSITORY_UNAVAILABLE'
  | 'EVENT_NOT_FOUND'
  | 'FORBIDDEN'
  | 'INVALID_EVENT_STATUS'
  | 'INVALID_POLL_REVISION'
  | 'INVALID_DEADLINE_ISO'
  | 'INVALID_NOW_ISO'
  | 'DEADLINE_REACHED'
  | 'INCOMPLETE_BALLOT'
  | 'DUPLICATE_SLOT'
  | 'UNKNOWN_SLOT'
  | 'INVALID_SLOT_ID'
  | 'INVALID_CHOICE'
  | 'LOCAL_TRANSACTION_FAILED'
  | 'IDEMPOTENCY_CONFLICT'
  | 'CLOCK_UNAVAILABLE'
  | 'COMMAND_JOURNAL_FAILED'
  | 'COMMAND_DISPATCH_MARK_FAILED'
  | 'COMMAND_CANCEL_FAILED'
  | 'COMMAND_JOURNAL_READ_FAILED'
  | 'COMMAND_CANCELLATION_TOMBSTONE_FAILED'
  | 'REPOSITORY_INCONSISTENT'

export interface BallotFailure {
  code: BallotFailureCode
  retryable: boolean
  commitOutcome: 'NOT_COMMITTED' | 'UNKNOWN'
}

export interface BallotReceipt {
  receiptId: string
  operationKey: string
  operationId: string
  eventId: string
  actorId: string
  pollRevision: number
  ballotFingerprint: string
  acceptedAtIso: string
  syncStatus: 'LOCAL_PENDING' | 'SYNCED'
  syncPayload: BallotSyncPayload
  serverReceiptId: string | null
}

export interface BallotSyncError {
  code: 'NETWORK_UNAVAILABLE' | 'SERVER_UNAVAILABLE' | 'FORBIDDEN' | 'PERMANENT_FAILURE'
  retryable: boolean
}

export interface BallotServerAck {
  localReceiptId: string
  serverReceiptId: string
  identity: BallotOperationIdentity
  ballotFingerprint: string
  outcome: 'APPLIED' | 'ALREADY_APPLIED'
}

export interface BallotOperationIdentity {
  eventId: string
  actorId: string
  pollRevision: number
  operationId: string
}

export interface BallotCommandEnvelope {
  schemaVersion: 1
  identity: BallotOperationIdentity
  authoritativeDeadlineIso: string
  entries: BallotEntry[]
  ballotFingerprint: string
}

export type BallotJournalStatus =
  | 'STAGED_NOT_DISPATCHED'
  | 'DISPATCHED'
  | 'CANCELLED'
  | 'DISPATCH_CANCELLATION_TOMBSTONED'

export type BallotTombstoneTerminalDestination =
  | { kind: 'CANCELLED' }
  | {
      kind: 'TERMINAL_FAILURE'
      code: BallotFailureCode
      commitOutcome: BallotFailure['commitOutcome']
    }
  | { kind: 'REVISED' }

export type BallotJournalSnapshot =
  | {
      journalStatus: 'STAGED_NOT_DISPATCHED' | 'DISPATCHED' | 'CANCELLED'
      terminalDestination: null
    }
  | {
      journalStatus: 'DISPATCH_CANCELLATION_TOMBSTONED'
      terminalDestination: BallotTombstoneTerminalDestination
    }

export interface BallotSyncPayload {
  schemaVersion: 1
  localReceiptId: string
  command: BallotCommandEnvelope
}

export type BallotServerSyncMutation = {
  table: 'poll_ballot' | 'votes'
  operation: 'CREATE' | 'UPDATE' | 'DELETE'
  payload: BallotSyncPayload | null
}

export type BallotServerMutationDecision =
  | { kind: 'APPLY_ATOMIC_POLL_BALLOT'; authority: 'poll_ballot' }
  | {
      kind: 'REJECT'
      code: 'LEGACY_VOTES_MUTATION_FORBIDDEN' | 'INVALID_POLL_BALLOT_MUTATION'
      retryable: false
    }

export interface PendingBallotMetadata {
  operationKey: string
  eventId: string
  actorId: string
  pollRevision: number
  operationId: string
  ballotFingerprint: string
  journalStatus: BallotJournalStatus
  terminalDestination: BallotTombstoneTerminalDestination | null
}

export type PendingBallotMetadataProjection =
  | { kind: 'VALID'; metadata: PendingBallotMetadata }
  | {
      kind: 'INCONSISTENT'
      code: 'REPOSITORY_INCONSISTENT'
      retryable: false
      commitOutcome: 'UNKNOWN'
      metadata: unknown
    }

export type PendingBallotSyncPayloadSnapshot =
  | { kind: 'EMPTY' }
  | { kind: 'MALFORMED'; diagnosticId: string }
  | { kind: 'PARSED'; payload: BallotSyncPayload }

export interface PendingBallotSyncMetadataRow {
  metadataId: string
  localReceiptId: string
  payloadSnapshot: PendingBallotSyncPayloadSnapshot
}

export type PendingBallotSyncJoinProjection =
  | {
      kind: 'VALID'
      metadata: PendingBallotSyncMetadataRow
      receipt: BallotReceipt
      payload: BallotSyncPayload
    }
  | {
      kind: 'INCONSISTENT'
      code: 'RECEIPT_MISSING' | 'PAYLOAD_EMPTY' | 'PAYLOAD_MALFORMED' |
        'RECEIPT_ID_DIVERGENT' | 'TUPLE_DIVERGENT' |
        'FINGERPRINT_DIVERGENT' | 'OPERATION_KEY_DIVERGENT' |
        'RECEIPT_NOT_LOCAL_PENDING' | 'RECEIPT_ALREADY_ACKNOWLEDGED'
      retryable: false
      commitOutcome: 'UNKNOWN'
      metadata: PendingBallotSyncMetadataRow
      receipt: BallotReceipt | null
    }

export interface BallotChoiceAffordance {
  visible: true
  isEnabled: boolean
  semanticState: 'ENABLED' | 'DISABLED'
  voiceOverState: 'ENABLED' | 'DISABLED'
  disabledReason: 'POLL_CLOSED' | null
}

export const projectBallotChoiceAffordance = (voteMutable: boolean): BallotChoiceAffordance =>
  voteMutable
    ? {
        visible: true, isEnabled: true, semanticState: 'ENABLED',
        voiceOverState: 'ENABLED', disabledReason: null,
      }
    : {
        visible: true, isEnabled: false, semanticState: 'DISABLED',
        voiceOverState: 'DISABLED', disabledReason: 'POLL_CLOSED',
      }

export interface ExistingBallotOperation {
  command: BallotCommandEnvelope
  receipt: BallotReceipt
}

export type IdempotencyDecision =
  | { kind: 'NEW_TUPLE' }
  | { kind: 'RETURN_EXISTING_RECEIPT'; receipt: BallotReceipt }
  | { kind: 'IDEMPOTENCY_CONFLICT' }
  | { kind: 'REPOSITORY_INCONSISTENT' }

interface PendingVote {
  clockRequestId: string
  slotId: string
  choice: VoteChoice
}

export interface PollBallotInput {
  eventId: string
  actorId: string
  pollRevision: number
  validSlotIds: readonly string[]
  votingDeadlineIso: string
  maxJournalResolutionAttempts: number
  eligibility: VotingEligibility
  initialEntries?: readonly BallotEntry[]
}

export type BallotEffect =
  | 'requestClockSnapshot'
  | 'persistPendingCommand'
  | 'markPendingCommandDispatched'
  | 'cancelPendingCommandJournal'
  | 'resolvePendingCommandJournal'
  | 'tombstoneDispatchedCancellation'
  | 'dispatchAtomicBallotCommand'
  | 'requestOperationCancellation'
  | 'navigateAfterLocalCommit'
  | 'returnToEventOnBack'
  | 'retryReceiptSync'

export interface PollBallotContext extends PollBallotInput {
  entries: BallotEntry[]
  submittedEntries: BallotEntry[] | null
  pendingCommand: BallotCommandEnvelope | null
  journalStatus: BallotJournalStatus | null
  journalIntent: 'AUTHORIZE_DISPATCH' | 'CANCEL' | 'TOMBSTONE_DISPATCHED_CANCELLATION' | null
  journalResolutionAttempt: number
  tombstoneTerminalDestination: BallotTombstoneTerminalDestination | null
  tombstoneFailure: BallotFailure | null
  pendingVote: PendingVote | null
  operationId: string | null
  lastClockNowIso: string | null
  receipt: BallotReceipt | null
  failure: BallotFailure | null
  syncError: BallotSyncError | null
  effects: BallotEffect[]
}

export type PollBallotEvent =
  | { type: 'ELIGIBILITY_UPDATED'; eligibility: VotingEligibility }
  | { type: 'REQUEST_SET_VOTE'; clockRequestId: string; slotId: string; choice: VoteChoice }
  | { type: 'VOTE_CLOCK_SNAPSHOT'; clockRequestId: string; nowIso: string }
  | { type: 'VOTE_CLOCK_FAILED'; clockRequestId: string }
  | { type: 'REQUEST_SUBMIT'; operationId: string }
  | { type: 'SUBMISSION_CLOCK_SNAPSHOT'; operationId: string; nowIso: string }
  | { type: 'SUBMISSION_CLOCK_FAILED'; operationId: string }
  | {
      type: 'COMMAND_JOURNALED'
      operationKey: string
      ballotFingerprint: string
    }
  | {
      type: 'COMMAND_JOURNAL_FAILED'
      operationKey: string
      ballotFingerprint: string
      error: BallotFailure
    }
  | { type: 'COMMAND_DISPATCH_MARKED'; operationKey: string; ballotFingerprint: string }
  | {
      type: 'COMMAND_DISPATCH_MARK_FAILED' | 'COMMAND_DISPATCH_MARK_UNKNOWN'
      operationKey: string
      ballotFingerprint: string
      error: BallotFailure
    }
  | { type: 'COMMAND_CANCELLED'; operationKey: string; ballotFingerprint: string }
  | {
      type: 'COMMAND_CANCEL_FAILED' | 'COMMAND_CANCEL_UNKNOWN'
      operationKey: string
      ballotFingerprint: string
      error: BallotFailure
    }
  | {
      type: 'COMMAND_JOURNAL_STATUS_RESOLVED'
      operationKey: string
      ballotFingerprint: string
      status: BallotJournalStatus
      terminalDestination: BallotTombstoneTerminalDestination | null
    }
  | {
      type: 'COMMAND_JOURNAL_STATUS_READ_FAILED'
      operationKey: string
      ballotFingerprint: string
      error: BallotFailure
    }
  | {
      type: 'COMMAND_JOURNAL_STATUS_MISSING' | 'COMMAND_JOURNAL_STATUS_MALFORMED'
      operationKey: string
      ballotFingerprint: string
    }
  | { type: 'LOCAL_COMMIT'; receipt: BallotReceipt }
  | { type: 'SUBMISSION_FAILED'; operationId: string; failure: BallotFailure }
  | {
      type: 'REHYDRATE_UNKNOWN_OUTCOME'
      command: BallotCommandEnvelope
      journalStatus: BallotJournalStatus
      terminalDestination: BallotTombstoneTerminalDestination | null
      failure: BallotFailure
    }
  | { type: 'RETRY_SUBMISSION' }
  | { type: 'RETRY_JOURNAL_CANCELLATION' }
  | { type: 'RETRY_JOURNAL_RESOLUTION' }
  | { type: 'REVISE_BALLOT' }
  | { type: 'RETRY_RESOLUTION' }
  | { type: 'CANCEL' }
  | { type: 'CANCELLATION_CONFIRMED'; operationId: string }
  | { type: 'CANCELLATION_UNKNOWN'; operationId: string }
  | {
      type: 'DISPATCH_CANCELLATION_TOMBSTONED'
      operationKey: string
      ballotFingerprint: string
      terminalDestination: BallotTombstoneTerminalDestination
    }
  | {
      type: 'DISPATCH_CANCELLATION_TOMBSTONE_FAILED' |
        'DISPATCH_CANCELLATION_TOMBSTONE_UNKNOWN'
      operationKey: string
      ballotFingerprint: string
      error: BallotFailure
    }
  | { type: 'SYNC_COMPLETED'; ack: BallotServerAck }
  | { type: 'SYNC_FAILED'; receiptId: string; error: BallotSyncError }
  | { type: 'RETRY_SYNC' }
  | { type: 'CLOSE' | 'BACK' }

const voteChoices: readonly string[] = ['YES', 'MAYBE', 'NO']
const ballotFailureCodes = new Set<BallotFailureCode>([
  'REPOSITORY_UNAVAILABLE', 'EVENT_NOT_FOUND', 'FORBIDDEN', 'INVALID_EVENT_STATUS',
  'INVALID_POLL_REVISION', 'INVALID_DEADLINE_ISO', 'INVALID_NOW_ISO', 'DEADLINE_REACHED',
  'INCOMPLETE_BALLOT', 'DUPLICATE_SLOT', 'UNKNOWN_SLOT', 'INVALID_SLOT_ID', 'INVALID_CHOICE',
  'LOCAL_TRANSACTION_FAILED', 'IDEMPOTENCY_CONFLICT', 'CLOCK_UNAVAILABLE',
  'COMMAND_JOURNAL_FAILED', 'COMMAND_DISPATCH_MARK_FAILED', 'COMMAND_CANCEL_FAILED',
  'COMMAND_JOURNAL_READ_FAILED', 'COMMAND_CANCELLATION_TOMBSTONE_FAILED',
  'REPOSITORY_INCONSISTENT',
])

export const parseIsoInstant = (value: string): number | null => {
  if (typeof value !== 'string' || value.trim() !== value) return null
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.(\d{1,9}))?(Z|([+-])(\d{2}):(\d{2}))$/.exec(value)
  if (!match) return null

  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  const hour = Number(match[4])
  const minute = Number(match[5])
  const second = Number(match[6])
  const offsetHour = match[10] === undefined ? 0 : Number(match[10])
  const offsetMinute = match[11] === undefined ? 0 : Number(match[11])
  const leapYear = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0)
  const daysByMonth = [31, leapYear ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]

  if (year === 0 || month < 1 || month > 12 || day < 1 ||
      day > (daysByMonth[month - 1] ?? 0) || hour > 23 || minute > 59 || second > 59 ||
      offsetHour > 14 || offsetMinute > 59 || (offsetHour === 14 && offsetMinute !== 0)) return null

  const millis = Date.parse(value)
  return Number.isFinite(millis) ? millis : null
}

export const canMutateVote = (deadlineIso: string, nowIso: string): boolean => {
  const deadline = parseIsoInstant(deadlineIso)
  const now = parseIsoInstant(nowIso)
  return deadline !== null && now !== null && now < deadline
}

const utf8 = (value: string): Uint8Array => new TextEncoder().encode(value)

const isUnicodeScalarString = (value: string): boolean => {
  if (value.length === 0) return false
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

const isNonBlankTrimmedUnicodeScalar = (value: unknown): value is string =>
  typeof value === 'string' && value.trim().length > 0 && value.trim() === value &&
  isUnicodeScalarString(value)

const compareUtf8 = (left: string, right: string): number => {
  const leftBytes = utf8(left)
  const rightBytes = utf8(right)
  const length = Math.min(leftBytes.length, rightBytes.length)
  for (let index = 0; index < length; index += 1) {
    if (leftBytes[index] !== rightBytes[index]) return (leftBytes[index] ?? 0) - (rightBytes[index] ?? 0)
  }
  return leftBytes.length - rightBytes.length
}

export const utf8Hex = (value: string): string =>
  [...utf8(value)].map((byte) => byte.toString(16).padStart(2, '0')).join('')

/**
 * Cross-platform v1 format, mirrored byte-for-byte in Kotlin:
 * sort slot ids by unsigned UTF-8 bytes, encode each id as lowercase UTF-8 hex,
 * then emit `v1|<hex>=<choice>` segments. No locale or URL encoder participates.
 */
export const canonicalizeBallot = (entries: readonly BallotEntry[]): BallotEntry[] =>
  [...entries].sort((left, right) => compareUtf8(left.slotId, right.slotId))

export const ballotFingerprint = (entries: readonly BallotEntry[]): string => {
  const segments = canonicalizeBallot(entries)
    .map(({ slotId, choice }) => `${utf8Hex(slotId)}=${choice}`)
  return ['v1', ...segments].join('|')
}

const identityField = (value: string): string => `${utf8(value).length}:${utf8Hex(value)}`

export const isValidPollRevision = (value: number): boolean =>
  Number.isSafeInteger(value) && value >= 0

export const isValidBallotOperationIdentity = (identity: BallotOperationIdentity): boolean =>
  isValidPollRevision(identity.pollRevision) &&
  [identity.eventId, identity.actorId, identity.operationId].every(isNonBlankTrimmedUnicodeScalar)

export const ballotOperationKey = (identity: BallotOperationIdentity): string => {
  if (!isValidBallotOperationIdentity(identity)) throw new TypeError('INVALID_OPERATION_IDENTITY')
  return `v1|${identityField(identity.eventId)}|${identityField(identity.actorId)}|` +
    `${identity.pollRevision}|${identityField(identity.operationId)}`
}

export const sameOperationIdentity = (
  left: BallotOperationIdentity,
  right: BallotOperationIdentity,
): boolean => isValidBallotOperationIdentity(left) && isValidBallotOperationIdentity(right) &&
  ballotOperationKey(left) === ballotOperationKey(right)

export const buildBallotCommand = (
  identity: BallotOperationIdentity,
  entries: readonly BallotEntry[],
  authoritativeDeadlineIso: string,
): BallotCommandEnvelope => {
  if (!isValidBallotOperationIdentity(identity)) throw new TypeError('INVALID_OPERATION_IDENTITY')
  if (parseIsoInstant(authoritativeDeadlineIso) === null) throw new TypeError('INVALID_DEADLINE_ISO')
  const canonicalEntries = canonicalizeBallot(entries)
  return {
    schemaVersion: 1,
    identity,
    authoritativeDeadlineIso,
    entries: canonicalEntries,
    ballotFingerprint: ballotFingerprint(canonicalEntries),
  }
}

export const commandsEqual = (
  left: BallotCommandEnvelope,
  right: BallotCommandEnvelope,
): boolean => {
  const leftEntries = canonicalizeBallot(left.entries)
  const rightEntries = canonicalizeBallot(right.entries)
  return sameOperationIdentity(left.identity, right.identity) &&
    left.schemaVersion === right.schemaVersion &&
    left.authoritativeDeadlineIso === right.authoritativeDeadlineIso &&
    left.ballotFingerprint === right.ballotFingerprint &&
    leftEntries.length === rightEntries.length &&
    leftEntries.every((entry, index) => entry.slotId === rightEntries[index]?.slotId &&
      entry.choice === rightEntries[index]?.choice)
}

export const classifyIdempotency = (
  existing: ExistingBallotOperation,
  incoming: BallotCommandEnvelope,
): IdempotencyDecision => {
  if (!sameOperationIdentity(existing.command.identity, incoming.identity)) {
    return { kind: 'NEW_TUPLE' }
  }
  return commandsEqual(existing.command, incoming)
    ? { kind: 'RETURN_EXISTING_RECEIPT', receipt: existing.receipt }
    : { kind: 'IDEMPOTENCY_CONFLICT' }
}

/** Resolution after a transactional unique-key collision: re-read in the same transaction. */
export const resolveConcurrentIdempotencyCollision = (
  reread: ExistingBallotOperation | null,
  incoming: BallotCommandEnvelope,
): IdempotencyDecision => reread === null
  ? { kind: 'REPOSITORY_INCONSISTENT' }
  : classifyIdempotency(reread, incoming)

export const validateBallotEntries = (
  validSlotIds: readonly string[],
  entries: readonly BallotEntry[],
): BallotFailureCode | null => {
  const required = new Set(validSlotIds)
  const seen = new Set<string>()

  for (const entry of entries) {
    if (!isUnicodeScalarString(entry.slotId)) return 'INVALID_SLOT_ID'
    if (seen.has(entry.slotId)) return 'DUPLICATE_SLOT'
    if (!required.has(entry.slotId)) return 'UNKNOWN_SLOT'
    if (!voteChoices.includes(entry.choice)) return 'INVALID_CHOICE'
    seen.add(entry.slotId)
  }

  return seen.size === required.size ? null : 'INCOMPLETE_BALLOT'
}

export const submissionFailure = (
  context: Pick<PollBallotContext,
    'eligibility' | 'votingDeadlineIso' | 'validSlotIds' | 'submittedEntries' | 'pollRevision'>,
  nowIso: string,
): BallotFailure | null => {
  const notCommitted = (code: BallotFailureCode, retryable = false): BallotFailure => ({
    code,
    retryable,
    commitOutcome: 'NOT_COMMITTED',
  })

  const mutationFailure = voteMutationFailure(context, nowIso)
  if (mutationFailure !== null) return mutationFailure
  if (!isValidPollRevision(context.pollRevision)) return notCommitted('INVALID_POLL_REVISION')

  const ballotError = validateBallotEntries(
    context.validSlotIds,
    context.submittedEntries ?? [],
  )
  return ballotError === null ? null : notCommitted(ballotError)
}

export const voteMutationFailure = (
  context: Pick<PollBallotContext, 'eligibility' | 'votingDeadlineIso'>,
  nowIso: string,
): BallotFailure | null => {
  const failure = (code: BallotFailureCode, retryable = false): BallotFailure => ({
    code,
    retryable,
    commitOutcome: 'NOT_COMMITTED',
  })
  if (!context.eligibility.repositoryAvailable) return failure('REPOSITORY_UNAVAILABLE', true)
  if (!context.eligibility.eventExists) return failure('EVENT_NOT_FOUND')
  if (!actorCanVote(context.eligibility.voteAccess)) return failure('FORBIDDEN')
  if (context.eligibility.eventStatus !== 'POLLING') return failure('INVALID_EVENT_STATUS')
  if (parseIsoInstant(context.votingDeadlineIso) === null) return failure('INVALID_DEADLINE_ISO')
  if (parseIsoInstant(nowIso) === null) return failure('INVALID_NOW_ISO')
  if (!canMutateVote(context.votingDeadlineIso, nowIso)) return failure('DEADLINE_REACHED')
  return null
}

type BallotReceiptWithUnknownPayload = Omit<BallotReceipt, 'syncPayload'> & { syncPayload: unknown }

const isBallotReceiptOuterShapeValid = (
  receipt: unknown,
): receipt is BallotReceiptWithUnknownPayload => {
  if (typeof receipt !== 'object' || receipt === null) return false
  const candidate = receipt as Record<string, unknown>
  return isNonBlankTrimmedUnicodeScalar(candidate.receiptId) &&
    isNonBlankTrimmedUnicodeScalar(candidate.operationKey) &&
    isNonBlankTrimmedUnicodeScalar(candidate.operationId) &&
    isNonBlankTrimmedUnicodeScalar(candidate.eventId) &&
    isNonBlankTrimmedUnicodeScalar(candidate.actorId) &&
    isValidPollRevision(candidate.pollRevision as number) &&
    isNonBlankTrimmedUnicodeScalar(candidate.ballotFingerprint) &&
    typeof candidate.acceptedAtIso === 'string' && parseIsoInstant(candidate.acceptedAtIso) !== null &&
    candidate.syncStatus === 'LOCAL_PENDING' && candidate.serverReceiptId === null
}

const isBallotReceiptPayloadValid = (
  receipt: BallotReceiptWithUnknownPayload,
): receipt is BallotReceipt => {
  if (typeof receipt.syncPayload !== 'object' || receipt.syncPayload === null) return false
  const syncPayload = receipt.syncPayload as Record<string, unknown>
  return syncPayload.schemaVersion === 1 &&
    syncPayload.localReceiptId === receipt.receiptId &&
    typeof syncPayload.command === 'object' && syncPayload.command !== null &&
    validateDurableCommandEnvelope(syncPayload.command as BallotCommandEnvelope)
}

export type BallotReceiptObservation = 'MATCH' | 'STALE' | 'MALFORMED'

export const classifyBallotReceiptObservation = (
  context: PollBallotContext,
  receipt: unknown,
): BallotReceiptObservation => {
  const expectedCommand = context.pendingCommand
  if (!isBallotReceiptOuterShapeValid(receipt)) return 'MALFORMED'
  const outerMatchesCurrent = expectedCommand !== null && context.journalStatus === 'DISPATCHED' &&
    receipt.operationKey === ballotOperationKey(expectedCommand.identity) &&
    receipt.operationId === expectedCommand.identity.operationId &&
    receipt.eventId === expectedCommand.identity.eventId &&
    receipt.actorId === expectedCommand.identity.actorId &&
    receipt.pollRevision === expectedCommand.identity.pollRevision &&
    receipt.ballotFingerprint === expectedCommand.ballotFingerprint

  if (!isBallotReceiptPayloadValid(receipt)) return 'MALFORMED'
  const innerIdentity = receipt.syncPayload.command.identity
  const receiptIsInternallyCoherent =
    receipt.operationKey === ballotOperationKey(innerIdentity) &&
    receipt.operationId === innerIdentity.operationId &&
    receipt.eventId === innerIdentity.eventId && receipt.actorId === innerIdentity.actorId &&
    receipt.pollRevision === innerIdentity.pollRevision &&
    receipt.ballotFingerprint === receipt.syncPayload.command.ballotFingerprint
  if (!receiptIsInternallyCoherent) return 'MALFORMED'
  if (!outerMatchesCurrent || expectedCommand === null) return 'STALE'

  const acceptedAt = parseIsoInstant(receipt.acceptedAtIso)
  const deadline = parseIsoInstant(expectedCommand.authoritativeDeadlineIso)
  return (
    receipt.syncPayload.localReceiptId === receipt.receiptId &&
    receipt.syncPayload.schemaVersion === 1 &&
    commandsEqual(receipt.syncPayload.command, expectedCommand) &&
    acceptedAt !== null && deadline !== null && acceptedAt < deadline
  ) ? 'MATCH' : 'MALFORMED'
}

const receiptMatches = (context: PollBallotContext, receipt: unknown): receipt is BallotReceipt =>
  classifyBallotReceiptObservation(context, receipt) === 'MATCH'

export const validateDurableCommandEnvelope = (command: BallotCommandEnvelope): boolean => {
  if (command.schemaVersion !== 1 || !isValidBallotOperationIdentity(command.identity) ||
      parseIsoInstant(command.authoritativeDeadlineIso) === null || command.entries.length === 0) return false
  const slotIds = command.entries.map((entry) => entry.slotId)
  if (validateBallotEntries(slotIds, command.entries) !== null) return false
  const canonicalEntries = canonicalizeBallot(command.entries)
  const alreadyCanonical = command.entries.every((entry, index) =>
    entry.slotId === canonicalEntries[index]?.slotId && entry.choice === canonicalEntries[index]?.choice)
  return alreadyCanonical && command.ballotFingerprint === ballotFingerprint(command.entries)
}

export const authorizeBallotServerMutation = (
  mutation: BallotServerSyncMutation,
): BallotServerMutationDecision => {
  if (mutation.table === 'votes') {
    return { kind: 'REJECT', code: 'LEGACY_VOTES_MUTATION_FORBIDDEN', retryable: false }
  }
  if (mutation.operation !== 'UPDATE' || mutation.payload === null ||
      mutation.payload.schemaVersion !== 1 || mutation.payload.localReceiptId.trim().length === 0 ||
      !validateDurableCommandEnvelope(mutation.payload.command)) {
    return { kind: 'REJECT', code: 'INVALID_POLL_BALLOT_MUTATION', retryable: false }
  }
  return { kind: 'APPLY_ATOMIC_POLL_BALLOT', authority: 'poll_ballot' }
}

export const projectPendingBallotMetadata = (
  session: Pick<PollBallotInput, 'eventId' | 'actorId'>,
  metadata: unknown,
): PendingBallotMetadataProjection => {
  const inconsistent = (): PendingBallotMetadataProjection => ({
    kind: 'INCONSISTENT', code: 'REPOSITORY_INCONSISTENT', retryable: false,
    commitOutcome: 'UNKNOWN', metadata,
  })
  if (typeof metadata !== 'object' || metadata === null) return inconsistent()
  const candidate = metadata as Record<string, unknown>
  const precheck = isNonBlankTrimmedUnicodeScalar(candidate.eventId) &&
    isNonBlankTrimmedUnicodeScalar(candidate.actorId) &&
    isNonBlankTrimmedUnicodeScalar(candidate.operationId) &&
    isNonBlankTrimmedUnicodeScalar(candidate.operationKey) &&
    isNonBlankTrimmedUnicodeScalar(candidate.ballotFingerprint) &&
    isValidPollRevision(candidate.pollRevision as number) &&
    isValidBallotJournalSnapshot({
      journalStatus: candidate.journalStatus,
      terminalDestination: candidate.terminalDestination,
    })
  if (!precheck) return inconsistent()
  const validMetadata = candidate as unknown as PendingBallotMetadata
  const identity: BallotOperationIdentity = {
    eventId: validMetadata.eventId,
    actorId: validMetadata.actorId,
    pollRevision: validMetadata.pollRevision,
    operationId: validMetadata.operationId,
  }
  let expectedOperationKey: string | null = null
  if (precheck) {
    try {
      expectedOperationKey = ballotOperationKey(identity)
    } catch {
      expectedOperationKey = null
    }
  }
  const consistent = validMetadata.eventId === session.eventId &&
    validMetadata.actorId === session.actorId && validMetadata.operationKey === expectedOperationKey
  return consistent
    ? { kind: 'VALID', metadata: validMetadata }
    : inconsistent()
}

export const projectPendingBallotMetadataList = (
  session: Pick<PollBallotInput, 'eventId' | 'actorId'>,
  rows: readonly unknown[],
): PendingBallotMetadataProjection[] => rows.map((row) => projectPendingBallotMetadata(session, row))

/**
 * Lossless repository join: every pending metadata row produces exactly one visible projection.
 * Empty/malformed payloads and broken receipt correlations are data inconsistencies, never
 * silently filtered pending work.
 */
export const projectPendingBallotSyncJoins = (
  metadataRows: readonly PendingBallotSyncMetadataRow[],
  receipts: readonly BallotReceipt[],
): PendingBallotSyncJoinProjection[] => {
  const receiptById = new Map(receipts.map((receipt) => [receipt.receiptId, receipt]))
  const inconsistent = (
    metadata: PendingBallotSyncMetadataRow,
    receipt: BallotReceipt | null,
    code: Extract<PendingBallotSyncJoinProjection, { kind: 'INCONSISTENT' }>['code'],
  ): PendingBallotSyncJoinProjection => ({
    kind: 'INCONSISTENT', code, retryable: false, commitOutcome: 'UNKNOWN', metadata, receipt,
  })

  return metadataRows.map((metadata) => {
    const receipt = receiptById.get(metadata.localReceiptId) ?? null
    if (receipt === null) return inconsistent(metadata, null, 'RECEIPT_MISSING')
    if (receipt.syncStatus !== 'LOCAL_PENDING') {
      return inconsistent(metadata, receipt, 'RECEIPT_NOT_LOCAL_PENDING')
    }
    if (receipt.serverReceiptId !== null) {
      return inconsistent(metadata, receipt, 'RECEIPT_ALREADY_ACKNOWLEDGED')
    }
    if (metadata.payloadSnapshot.kind === 'EMPTY') {
      return inconsistent(metadata, receipt, 'PAYLOAD_EMPTY')
    }
    if (metadata.payloadSnapshot.kind === 'MALFORMED') {
      return inconsistent(metadata, receipt, 'PAYLOAD_MALFORMED')
    }

    const payload = metadata.payloadSnapshot.payload
    if (metadata.localReceiptId.trim().length === 0 ||
        payload.localReceiptId !== metadata.localReceiptId ||
        receipt.receiptId !== metadata.localReceiptId ||
        receipt.syncPayload.localReceiptId !== receipt.receiptId) {
      return inconsistent(metadata, receipt, 'RECEIPT_ID_DIVERGENT')
    }
    if (!validateDurableCommandEnvelope(payload.command) ||
        !sameOperationIdentity(payload.command.identity, receipt.syncPayload.command.identity) ||
        !sameOperationIdentity(payload.command.identity, {
          eventId: receipt.eventId,
          actorId: receipt.actorId,
          pollRevision: receipt.pollRevision,
          operationId: receipt.operationId,
        })) {
      return inconsistent(metadata, receipt, 'TUPLE_DIVERGENT')
    }
    if (payload.command.ballotFingerprint !== receipt.ballotFingerprint ||
        payload.command.ballotFingerprint !== receipt.syncPayload.command.ballotFingerprint) {
      return inconsistent(metadata, receipt, 'FINGERPRINT_DIVERGENT')
    }
    const operationKey = ballotOperationKey(payload.command.identity)
    if (receipt.operationKey !== operationKey) {
      return inconsistent(metadata, receipt, 'OPERATION_KEY_DIVERGENT')
    }
    return { kind: 'VALID', metadata, receipt, payload }
  })
}

/** Only fully correlated LOCAL_PENDING rows can reach the sync dispatcher. */
export const pendingSyncPayloadsForDispatch = (
  projections: readonly PendingBallotSyncJoinProjection[],
): BallotSyncPayload[] => projections.flatMap((projection) =>
  projection.kind === 'VALID' ? [projection.payload] : [])

const commandBelongsToSession = (
  context: Pick<PollBallotContext, 'eventId' | 'actorId'>,
  command: BallotCommandEnvelope,
): boolean => command.identity.eventId === context.eventId && command.identity.actorId === context.actorId

type JournalCorrelationEvent = Extract<
  PollBallotEvent,
  { operationKey: string; ballotFingerprint: string }
>

const journalCorrelationMatches = (
  context: PollBallotContext,
  event: JournalCorrelationEvent,
): boolean => context.pendingCommand !== null &&
  event.operationKey === ballotOperationKey(context.pendingCommand.identity) &&
  event.ballotFingerprint === context.pendingCommand.ballotFingerprint

const tombstoneDestinationsEqual = (
  left: BallotTombstoneTerminalDestination | null,
  right: BallotTombstoneTerminalDestination | null,
): boolean => {
  if (left === null || right === null) return left === right
  if (left.kind !== right.kind) return false
  return left.kind !== 'TERMINAL_FAILURE' || right.kind !== 'TERMINAL_FAILURE'
    ? true
    : left.code === right.code && left.commitOutcome === right.commitOutcome
}

const isValidTombstoneDestination = (
  destination: unknown,
): destination is BallotTombstoneTerminalDestination => {
  if (typeof destination !== 'object' || destination === null) return false
  const candidate = destination as Record<string, unknown>
  if (candidate.kind === 'CANCELLED' || candidate.kind === 'REVISED') return true
  return candidate.kind === 'TERMINAL_FAILURE' &&
    isNonBlankTrimmedUnicodeScalar(candidate.code) &&
    ballotFailureCodes.has(candidate.code as BallotFailureCode) &&
    (candidate.commitOutcome === 'NOT_COMMITTED' || candidate.commitOutcome === 'UNKNOWN')
}

export const isValidBallotJournalSnapshot = (
  snapshot: unknown,
): snapshot is BallotJournalSnapshot => {
  if (typeof snapshot !== 'object' || snapshot === null) return false
  const candidate = snapshot as Record<string, unknown>
  if (candidate.journalStatus === 'DISPATCH_CANCELLATION_TOMBSTONED') {
    return isValidTombstoneDestination(candidate.terminalDestination)
  }
  return (candidate.journalStatus === 'STAGED_NOT_DISPATCHED' ||
    candidate.journalStatus === 'DISPATCHED' || candidate.journalStatus === 'CANCELLED') &&
    candidate.terminalDestination === null
}

export type BallotTombstoneProjection =
  | { state: 'CANCELLED'; navigation: 'RETURN_TO_EVENT'; failure: null }
  | { state: 'EDITING'; navigation: 'NONE'; failure: null }
  | { state: 'TERMINAL_FAILURE'; navigation: 'NONE'; failure: BallotFailure }

export const projectBallotTombstoneDestination = (
  destination: BallotTombstoneTerminalDestination,
): BallotTombstoneProjection => {
  if (destination.kind === 'CANCELLED') {
    return { state: 'CANCELLED', navigation: 'RETURN_TO_EVENT', failure: null }
  }
  if (destination.kind === 'REVISED') {
    return { state: 'EDITING', navigation: 'NONE', failure: null }
  }
  return {
    state: 'TERMINAL_FAILURE', navigation: 'NONE',
    failure: {
      code: destination.code, retryable: false, commitOutcome: destination.commitOutcome,
    },
  }
}

export const pollBallotInvariants = [
  'No production clock read occurs in the model; every edit/submit time comes from the injected Clock port.',
  'A vote is mutable if and only if both instants are valid offset-qualified ISO values and now is strictly before deadline.',
  'The submitted ballot covers exactly the repository slot set once, with no missing, duplicate, or unknown slot.',
  'One repository transaction writes every ballot entry and its operation receipt, or writes nothing.',
  'The repository transaction rechecks permission, POLLING status, poll revision, slot set, and deadline using its injected authoritative clock.',
  'An operation key is unique by event, actor, poll revision, and operation id; replay with the same payload returns the same receipt.',
  'The same operation id under a different event, actor, or poll revision is an independent tuple, not a conflict.',
  'The v1 fingerprint is locale-free UTF-8 byte order plus lowercase UTF-8 hex and is identical in TypeScript and Kotlin.',
  'A retry after a known non-commit requests a fresh clock; an unknown-outcome retry replays the exact captured command without becoming a new mutation.',
  'While a DISPATCHED command has unknown outcome, REQUEST_SUBMIT for any new operation is forbidden; only resolution retry may replay the same operation id and payload, so at most one command is active.',
  'The journal is monotone: STAGED_NOT_DISPATCHED to DISPATCHED or CANCELLED, then DISPATCHED to DISPATCH_CANCELLATION_TOMBSTONED only after proven non-commit; dispatch requires a correlated durable DISPATCHED acknowledgement.',
  'Journal-status read failure, missing row, and malformed row are correlated and explicitly retried within a safe positive bound; exhaustion is terminal REPOSITORY_INCONSISTENT.',
  'A correlated non-retryable journal read failure outside tombstone persistence enters terminalFailure with REPOSITORY_INCONSISTENT immediately.',
  'Every retry that emits a ballot dispatch retains durable DISPATCHED status; non-retryable persistence failures are terminal.',
  'Only a DISPATCHED journal can rehydrate unknown-outcome resolution; event and actor must match the session while current revision and slots are ignored.',
  'A concurrent four-field uniqueness collision is resolved by transactional re-read, never blind insertion retry.',
  'Cancellation after dispatch is terminal only after the repository proves that no commit occurred; a racing receipt wins.',
  'PollVoting CLOSE and BACK after DISPATCHED are cancellation requests, not presentation exits: a valid racing receipt wins, otherwise only correlated proof plus its durable typed tombstone may exit.',
  'After proof of non-commit for a DISPATCHED command, cancellation becomes terminal only after its correlated durable cancellation tombstone; tombstone rehydration never redispatches.',
  'A known terminal NOT_COMMITTED result for a DISPATCHED command also persists the same no-redispatch tombstone before terminalFailure; tombstone failure remains nonterminal.',
  'Every dispatched tombstone durably carries exactly one destination: CANCELLED, TERMINAL_FAILURE(code, outcome), or REVISED; rehydration follows that destination exactly.',
  'Tombstone projection is discriminated and total: CANCELLED emits returnToEventOnBack exactly once then reaches terminal cancelled, REVISED restores editing without navigation, and TERMINAL_FAILURE restores its typed failure without navigation.',
  'The journal snapshot is a total discriminated union: STAGED_NOT_DISPATCHED, DISPATCHED, and CANCELLED require a null destination; only DISPATCH_CANCELLATION_TOMBSTONED requires a valid typed destination. Any divergent rehydrated pair is terminal REPOSITORY_INCONSISTENT.',
  'REVISE_BALLOT cannot clear a DISPATCHED command from failedBeforeCommit until a correlated REVISED tombstone is durable; restoring that tombstone never redispatches the old command.',
  'Resolved cancellation is accepted only under its exact intent: pre-dispatch CANCEL expects CANCELLED, while dispatched cancellation expects TOMBSTONE_DISPATCHED_CANCELLATION plus DISPATCH_CANCELLATION_TOMBSTONED.',
  'Local commit makes navigation eligible exactly once; sync failure is a separate post-commit state and never rolls back or resubmits the ballot.',
  'A local receipt says LOCAL_PENDING and carries its localReceiptId plus the complete command; only a correlated backend ACK with non-blank serverReceiptId changes it to SYNCED.',
  'Server sync mutates ballots only through atomic poll_ballot UPDATE and rejects every legacy votes mutation.',
  'Pending metadata is projected one-for-one; inconsistency is visible, non-retryable, UNKNOWN, and never filtered away.',
  'Receipt classification correlates outer operation key/id/event/actor/revision/fingerprint first: outer-current plus malformed/divergent inner payload is terminal REPOSITORY_INCONSISTENT with retryable false and UNKNOWN; STALE is reserved for an outer-foreign receipt that is structurally valid and internally coherent.',
  'The pending-sync metadata/receipt join is lossless; absent receipts, empty or malformed payloads, divergent receipt id, tuple, fingerprint, or operation key, and any non-LOCAL_PENDING/already-acknowledged receipt remain visible and cannot be dispatched.',
] as const

export const pollBallotSubmissionMachine = setup({
  types: {
    context: {} as PollBallotContext,
    events: {} as PollBallotEvent,
    input: {} as PollBallotInput,
  },
  guards: {
    canRequestVoteClock: ({ context, event }) => event.type === 'REQUEST_SET_VOTE' &&
      event.clockRequestId.length > 0 && context.validSlotIds.includes(event.slotId) &&
      context.pendingVote === null,
    voteClockMatches: ({ context, event }) => event.type === 'VOTE_CLOCK_SNAPSHOT' &&
      event.clockRequestId === context.pendingVote?.clockRequestId,
    voteClockMatchesAndMutationIsValid: ({ context, event }) =>
      event.type === 'VOTE_CLOCK_SNAPSHOT' &&
      event.clockRequestId === context.pendingVote?.clockRequestId &&
      voteMutationFailure(context, event.nowIso) === null,
    voteClockFailureMatches: ({ context, event }) => event.type === 'VOTE_CLOCK_FAILED' &&
      event.clockRequestId === context.pendingVote?.clockRequestId,
    canBeginSubmission: ({ context, event }) => event.type === 'REQUEST_SUBMIT' &&
      context.operationId === null && event.operationId.length > 0,
    clockMatches: ({ context, event }) => event.type === 'SUBMISSION_CLOCK_SNAPSHOT' &&
      event.operationId === context.operationId,
    clockMatchesAndSubmissionIsValid: ({ context, event }) => event.type === 'SUBMISSION_CLOCK_SNAPSHOT' &&
      event.operationId === context.operationId && submissionFailure(context, event.nowIso) === null,
    clockFailureMatches: ({ context, event }) => event.type === 'SUBMISSION_CLOCK_FAILED' &&
      event.operationId === context.operationId,
    commandJournalMatches: ({ context, event }) => event.type === 'COMMAND_JOURNALED' &&
      context.pendingCommand !== null &&
      event.operationKey === ballotOperationKey(context.pendingCommand.identity) &&
      event.ballotFingerprint === context.pendingCommand.ballotFingerprint,
    retryableCommandJournalFailureMatches: ({ context, event }) =>
      event.type === 'COMMAND_JOURNAL_FAILED' &&
      context.pendingCommand !== null &&
      event.operationKey === ballotOperationKey(context.pendingCommand.identity) &&
      event.ballotFingerprint === context.pendingCommand.ballotFingerprint &&
      event.error.code === 'COMMAND_JOURNAL_FAILED' &&
      event.error.commitOutcome === 'NOT_COMMITTED' && event.error.retryable,
    terminalCommandJournalFailureMatches: ({ context, event }) =>
      event.type === 'COMMAND_JOURNAL_FAILED' &&
      context.pendingCommand !== null &&
      event.operationKey === ballotOperationKey(context.pendingCommand.identity) &&
      event.ballotFingerprint === context.pendingCommand.ballotFingerprint &&
      event.error.code === 'COMMAND_JOURNAL_FAILED' &&
      event.error.commitOutcome === 'NOT_COMMITTED' && !event.error.retryable,
    commandStatusMatches: ({ context, event }) =>
      (event.type === 'COMMAND_DISPATCH_MARKED' || event.type === 'COMMAND_CANCELLED') &&
      journalCorrelationMatches(context, event),
    retryableDispatchMarkFailureMatches: ({ context, event }) =>
      event.type === 'COMMAND_DISPATCH_MARK_FAILED' && journalCorrelationMatches(context, event) &&
      event.error.code === 'COMMAND_DISPATCH_MARK_FAILED' &&
      event.error.commitOutcome === 'NOT_COMMITTED' && event.error.retryable,
    terminalDispatchMarkFailureMatches: ({ context, event }) =>
      event.type === 'COMMAND_DISPATCH_MARK_FAILED' && journalCorrelationMatches(context, event) &&
      event.error.code === 'COMMAND_DISPATCH_MARK_FAILED' &&
      event.error.commitOutcome === 'NOT_COMMITTED' && !event.error.retryable,
    unknownDispatchMarkMatches: ({ context, event }) =>
      event.type === 'COMMAND_DISPATCH_MARK_UNKNOWN' && journalCorrelationMatches(context, event) &&
      event.error.code === 'COMMAND_DISPATCH_MARK_FAILED' &&
      event.error.commitOutcome === 'UNKNOWN',
    retryableCancelFailureMatches: ({ context, event }) =>
      event.type === 'COMMAND_CANCEL_FAILED' && journalCorrelationMatches(context, event) &&
      event.error.code === 'COMMAND_CANCEL_FAILED' &&
      event.error.commitOutcome === 'NOT_COMMITTED' && event.error.retryable,
    terminalCancelFailureMatches: ({ context, event }) =>
      event.type === 'COMMAND_CANCEL_FAILED' && journalCorrelationMatches(context, event) &&
      event.error.code === 'COMMAND_CANCEL_FAILED' &&
      event.error.commitOutcome === 'NOT_COMMITTED' && !event.error.retryable,
    unknownCancelMatches: ({ context, event }) => event.type === 'COMMAND_CANCEL_UNKNOWN' &&
      journalCorrelationMatches(context, event) && event.error.code === 'COMMAND_CANCEL_FAILED' &&
      event.error.commitOutcome === 'UNKNOWN',
    resolvedJournalStatusMatches: ({ context, event }) =>
      event.type === 'COMMAND_JOURNAL_STATUS_RESOLVED' && journalCorrelationMatches(context, event),
    resolvedDispatchedForAuthorize: ({ context, event }) =>
      event.type === 'COMMAND_JOURNAL_STATUS_RESOLVED' &&
      journalCorrelationMatches(context, event) && context.journalIntent === 'AUTHORIZE_DISPATCH' &&
      event.status === 'DISPATCHED' && event.terminalDestination === null,
    resolvedStagedForAuthorize: ({ context, event }) =>
      event.type === 'COMMAND_JOURNAL_STATUS_RESOLVED' &&
      journalCorrelationMatches(context, event) && context.journalIntent === 'AUTHORIZE_DISPATCH' &&
      event.status === 'STAGED_NOT_DISPATCHED' && event.terminalDestination === null,
    resolvedPreDispatchCancelled: ({ context, event }) =>
      event.type === 'COMMAND_JOURNAL_STATUS_RESOLVED' &&
      journalCorrelationMatches(context, event) && context.journalIntent === 'CANCEL' &&
      event.status === 'CANCELLED' && event.terminalDestination === null,
    resolvedDispatchedForCancel: ({ context, event }) =>
      event.type === 'COMMAND_JOURNAL_STATUS_RESOLVED' &&
      journalCorrelationMatches(context, event) && context.journalIntent === 'CANCEL' &&
      event.status === 'DISPATCHED' && event.terminalDestination === null,
    resolvedStagedForCancel: ({ context, event }) =>
      event.type === 'COMMAND_JOURNAL_STATUS_RESOLVED' &&
      journalCorrelationMatches(context, event) && context.journalIntent === 'CANCEL' &&
      event.status === 'STAGED_NOT_DISPATCHED' && event.terminalDestination === null,
    resolvedCancelled: ({ context, event }) =>
      event.type === 'COMMAND_JOURNAL_STATUS_RESOLVED' &&
      journalCorrelationMatches(context, event) &&
      context.journalIntent === 'TOMBSTONE_DISPATCHED_CANCELLATION' &&
      context.tombstoneTerminalDestination?.kind === 'CANCELLED' &&
      event.status === 'DISPATCH_CANCELLATION_TOMBSTONED' &&
      tombstoneDestinationsEqual(event.terminalDestination, context.tombstoneTerminalDestination),
    resolvedTerminalFailureAfterTombstone: ({ context, event }) =>
      event.type === 'COMMAND_JOURNAL_STATUS_RESOLVED' &&
      journalCorrelationMatches(context, event) &&
      context.journalIntent === 'TOMBSTONE_DISPATCHED_CANCELLATION' &&
      context.tombstoneTerminalDestination?.kind === 'TERMINAL_FAILURE' &&
      event.status === 'DISPATCH_CANCELLATION_TOMBSTONED' &&
      tombstoneDestinationsEqual(event.terminalDestination, context.tombstoneTerminalDestination),
    resolvedRevisionAfterTombstone: ({ context, event }) =>
      event.type === 'COMMAND_JOURNAL_STATUS_RESOLVED' &&
      journalCorrelationMatches(context, event) &&
      context.journalIntent === 'TOMBSTONE_DISPATCHED_CANCELLATION' &&
      context.tombstoneTerminalDestination?.kind === 'REVISED' &&
      event.status === 'DISPATCH_CANCELLATION_TOMBSTONED' &&
      tombstoneDestinationsEqual(event.terminalDestination, context.tombstoneTerminalDestination),
    resolvedDispatchedForTombstoneRetry: ({ context, event }) =>
      event.type === 'COMMAND_JOURNAL_STATUS_RESOLVED' &&
      journalCorrelationMatches(context, event) &&
      context.journalIntent === 'TOMBSTONE_DISPATCHED_CANCELLATION' &&
      event.status === 'DISPATCHED' && event.terminalDestination === null,
    nonRetryableJournalReadFailureMatches: ({ context, event }) =>
      event.type === 'COMMAND_JOURNAL_STATUS_READ_FAILED' &&
      journalCorrelationMatches(context, event) &&
      event.error.code === 'COMMAND_JOURNAL_READ_FAILED' && !event.error.retryable &&
      context.journalIntent !== 'TOMBSTONE_DISPATCHED_CANCELLATION',
    nonRetryableTombstoneJournalReadFailureMatches: ({ context, event }) =>
      event.type === 'COMMAND_JOURNAL_STATUS_READ_FAILED' &&
      journalCorrelationMatches(context, event) &&
      event.error.code === 'COMMAND_JOURNAL_READ_FAILED' && !event.error.retryable &&
      context.journalIntent === 'TOMBSTONE_DISPATCHED_CANCELLATION',
    journalResolutionProblemMatchesAndCanRetry: ({ context, event }) =>
      (event.type === 'COMMAND_JOURNAL_STATUS_READ_FAILED' ||
        event.type === 'COMMAND_JOURNAL_STATUS_MISSING' ||
        event.type === 'COMMAND_JOURNAL_STATUS_MALFORMED') &&
      journalCorrelationMatches(context, event) &&
      context.journalIntent !== 'TOMBSTONE_DISPATCHED_CANCELLATION' &&
      (event.type !== 'COMMAND_JOURNAL_STATUS_READ_FAILED' || event.error.retryable) &&
      Number.isSafeInteger(context.maxJournalResolutionAttempts) &&
      context.maxJournalResolutionAttempts > 0 &&
      context.journalResolutionAttempt < context.maxJournalResolutionAttempts,
    tombstoneJournalResolutionProblemMatchesAndCanRetry: ({ context, event }) =>
      (event.type === 'COMMAND_JOURNAL_STATUS_READ_FAILED' ||
        event.type === 'COMMAND_JOURNAL_STATUS_MISSING' ||
        event.type === 'COMMAND_JOURNAL_STATUS_MALFORMED') &&
      journalCorrelationMatches(context, event) &&
      context.journalIntent === 'TOMBSTONE_DISPATCHED_CANCELLATION' &&
      (event.type !== 'COMMAND_JOURNAL_STATUS_READ_FAILED' || event.error.retryable) &&
      Number.isSafeInteger(context.maxJournalResolutionAttempts) &&
      context.maxJournalResolutionAttempts > 0 &&
      context.journalResolutionAttempt < context.maxJournalResolutionAttempts,
    journalResolutionProblemMatchesAndExhausted: ({ context, event }) =>
      (event.type === 'COMMAND_JOURNAL_STATUS_READ_FAILED' ||
        event.type === 'COMMAND_JOURNAL_STATUS_MISSING' ||
        event.type === 'COMMAND_JOURNAL_STATUS_MALFORMED') &&
      journalCorrelationMatches(context, event) &&
      context.journalIntent !== 'TOMBSTONE_DISPATCHED_CANCELLATION' &&
      (!Number.isSafeInteger(context.maxJournalResolutionAttempts) ||
        context.maxJournalResolutionAttempts <= 0 ||
        context.journalResolutionAttempt >= context.maxJournalResolutionAttempts),
    tombstoneJournalResolutionProblemMatchesAndExhausted: ({ context, event }) =>
      (event.type === 'COMMAND_JOURNAL_STATUS_READ_FAILED' ||
        event.type === 'COMMAND_JOURNAL_STATUS_MISSING' ||
        event.type === 'COMMAND_JOURNAL_STATUS_MALFORMED') &&
      journalCorrelationMatches(context, event) &&
      context.journalIntent === 'TOMBSTONE_DISPATCHED_CANCELLATION' &&
      (!Number.isSafeInteger(context.maxJournalResolutionAttempts) ||
        context.maxJournalResolutionAttempts <= 0 ||
        context.journalResolutionAttempt >= context.maxJournalResolutionAttempts),
    canRetryJournalResolution: ({ context }) => context.failure?.retryable === true &&
      Number.isSafeInteger(context.maxJournalResolutionAttempts) &&
      context.maxJournalResolutionAttempts > 0 &&
      context.journalResolutionAttempt < context.maxJournalResolutionAttempts,
    receiptMatchesPendingOperation: ({ context, event }) => event.type === 'LOCAL_COMMIT' &&
      receiptMatches(context, event.receipt),
    malformedReceiptForPendingOperation: ({ context, event }) => event.type === 'LOCAL_COMMIT' &&
      classifyBallotReceiptObservation(context, event.receipt) === 'MALFORMED',
    rehydratedRetryableCommandIsValid: ({ context, event }) =>
      event.type === 'REHYDRATE_UNKNOWN_OUTCOME' && event.journalStatus === 'DISPATCHED' &&
      event.terminalDestination === null &&
      event.failure.commitOutcome === 'UNKNOWN' && event.failure.retryable &&
      validateDurableCommandEnvelope(event.command) && commandBelongsToSession(context, event.command),
    rehydratedTerminalCommandIsValid: ({ context, event }) =>
      event.type === 'REHYDRATE_UNKNOWN_OUTCOME' && event.journalStatus === 'DISPATCHED' &&
      event.terminalDestination === null &&
      event.failure.commitOutcome === 'UNKNOWN' && !event.failure.retryable &&
      validateDurableCommandEnvelope(event.command) && commandBelongsToSession(context, event.command),
    rehydratedStagedCommandIsValid: ({ context, event }) =>
      event.type === 'REHYDRATE_UNKNOWN_OUTCOME' && event.journalStatus === 'STAGED_NOT_DISPATCHED' &&
      event.terminalDestination === null &&
      validateDurableCommandEnvelope(event.command) && commandBelongsToSession(context, event.command),
    rehydratedPreDispatchCancelledCommandIsValid: ({ context, event }) =>
      event.type === 'REHYDRATE_UNKNOWN_OUTCOME' &&
      event.journalStatus === 'CANCELLED' && event.terminalDestination === null &&
      validateDurableCommandEnvelope(event.command) && commandBelongsToSession(context, event.command),
    rehydratedCancelledTombstoneIsValid: ({ context, event }) =>
      event.type === 'REHYDRATE_UNKNOWN_OUTCOME' &&
      event.journalStatus === 'DISPATCH_CANCELLATION_TOMBSTONED' &&
      isValidTombstoneDestination(event.terminalDestination) &&
      event.terminalDestination.kind === 'CANCELLED' &&
      validateDurableCommandEnvelope(event.command) && commandBelongsToSession(context, event.command),
    rehydratedTerminalFailureTombstoneIsValid: ({ context, event }) =>
      event.type === 'REHYDRATE_UNKNOWN_OUTCOME' &&
      event.journalStatus === 'DISPATCH_CANCELLATION_TOMBSTONED' &&
      isValidTombstoneDestination(event.terminalDestination) &&
      event.terminalDestination.kind === 'TERMINAL_FAILURE' &&
      validateDurableCommandEnvelope(event.command) && commandBelongsToSession(context, event.command),
    rehydratedRevisionTombstoneIsValid: ({ context, event }) =>
      event.type === 'REHYDRATE_UNKNOWN_OUTCOME' &&
      event.journalStatus === 'DISPATCH_CANCELLATION_TOMBSTONED' &&
      isValidTombstoneDestination(event.terminalDestination) &&
      event.terminalDestination.kind === 'REVISED' &&
      validateDurableCommandEnvelope(event.command) && commandBelongsToSession(context, event.command),
    rehydratedSnapshotOrCommandIsInconsistent: ({ context, event }) =>
      event.type === 'REHYDRATE_UNKNOWN_OUTCOME' &&
      (!isValidBallotJournalSnapshot({
        journalStatus: event.journalStatus,
        terminalDestination: event.terminalDestination,
      }) || !validateDurableCommandEnvelope(event.command) ||
        !commandBelongsToSession(context, event.command)),
    retryableKnownFailureMatches: ({ context, event }) => event.type === 'SUBMISSION_FAILED' &&
      event.operationId === context.operationId && event.failure.commitOutcome === 'NOT_COMMITTED' &&
      event.failure.retryable,
    terminalKnownFailureMatches: ({ context, event }) => event.type === 'SUBMISSION_FAILED' &&
      event.operationId === context.operationId && event.failure.commitOutcome === 'NOT_COMMITTED' &&
      !event.failure.retryable,
    unknownFailureMatches: ({ context, event }) => event.type === 'SUBMISSION_FAILED' &&
      event.operationId === context.operationId && event.failure.commitOutcome === 'UNKNOWN' &&
      event.failure.retryable,
    terminalUnknownFailureMatches: ({ context, event }) => event.type === 'SUBMISSION_FAILED' &&
      event.operationId === context.operationId && event.failure.commitOutcome === 'UNKNOWN' &&
      !event.failure.retryable,
    retryableKnownFailure: ({ context }) => context.failure?.retryable === true &&
      context.failure.commitOutcome === 'NOT_COMMITTED' && context.operationId !== null,
    retryableKnownFailureWithDispatchedJournal: ({ context }) =>
      context.failure?.retryable === true && context.failure.commitOutcome === 'NOT_COMMITTED' &&
      context.operationId !== null && context.journalStatus === 'DISPATCHED',
    retryableKnownFailureWithoutDispatchedJournal: ({ context }) =>
      context.failure?.retryable === true && context.failure.commitOutcome === 'NOT_COMMITTED' &&
      context.operationId !== null && context.journalStatus !== 'DISPATCHED',
    operationMatchesCancellation: ({ context, event }) =>
      (event.type === 'CANCELLATION_CONFIRMED' || event.type === 'CANCELLATION_UNKNOWN') &&
      event.operationId === context.operationId,
    cancellationTombstoneMatches: ({ context, event }) =>
      event.type === 'DISPATCH_CANCELLATION_TOMBSTONED' &&
      journalCorrelationMatches(context, event) &&
      context.journalIntent === 'TOMBSTONE_DISPATCHED_CANCELLATION' &&
      context.tombstoneTerminalDestination?.kind === 'CANCELLED' &&
      tombstoneDestinationsEqual(event.terminalDestination, context.tombstoneTerminalDestination),
    terminalFailureTombstoneMatches: ({ context, event }) =>
      event.type === 'DISPATCH_CANCELLATION_TOMBSTONED' &&
      journalCorrelationMatches(context, event) &&
      context.journalIntent === 'TOMBSTONE_DISPATCHED_CANCELLATION' &&
      context.tombstoneTerminalDestination?.kind === 'TERMINAL_FAILURE' &&
      tombstoneDestinationsEqual(event.terminalDestination, context.tombstoneTerminalDestination),
    revisionTombstoneMatches: ({ context, event }) =>
      event.type === 'DISPATCH_CANCELLATION_TOMBSTONED' &&
      journalCorrelationMatches(context, event) &&
      context.journalIntent === 'TOMBSTONE_DISPATCHED_CANCELLATION' &&
      context.tombstoneTerminalDestination?.kind === 'REVISED' &&
      tombstoneDestinationsEqual(event.terminalDestination, context.tombstoneTerminalDestination),
    knownNonCommitHasDispatchedJournal: ({ context }) =>
      context.journalStatus === 'DISPATCHED' &&
      context.failure?.commitOutcome === 'NOT_COMMITTED',
    canRetryDispatchedTombstone: ({ context }) => context.tombstoneFailure?.retryable === true,
    retryableCancellationTombstoneFailureMatches: ({ context, event }) =>
      event.type === 'DISPATCH_CANCELLATION_TOMBSTONE_FAILED' &&
      journalCorrelationMatches(context, event) &&
      event.error.code === 'COMMAND_CANCELLATION_TOMBSTONE_FAILED' &&
      event.error.commitOutcome === 'NOT_COMMITTED' && event.error.retryable,
    terminalCancellationTombstoneFailureMatches: ({ context, event }) =>
      event.type === 'DISPATCH_CANCELLATION_TOMBSTONE_FAILED' &&
      journalCorrelationMatches(context, event) &&
      event.error.code === 'COMMAND_CANCELLATION_TOMBSTONE_FAILED' &&
      event.error.commitOutcome === 'NOT_COMMITTED' && !event.error.retryable,
    unknownCancellationTombstoneFailureMatches: ({ context, event }) =>
      event.type === 'DISPATCH_CANCELLATION_TOMBSTONE_UNKNOWN' &&
      journalCorrelationMatches(context, event) &&
      event.error.code === 'COMMAND_CANCELLATION_TOMBSTONE_FAILED' &&
      event.error.commitOutcome === 'UNKNOWN',
    syncAckMatches: ({ context, event }) => event.type === 'SYNC_COMPLETED' &&
      event.ack.localReceiptId === context.receipt?.receiptId &&
      event.ack.serverReceiptId.trim().length > 0 &&
      context.pendingCommand !== null &&
      sameOperationIdentity(event.ack.identity, context.pendingCommand.identity) &&
      event.ack.ballotFingerprint === context.pendingCommand.ballotFingerprint,
    syncFailureMatches: ({ context, event }) => event.type === 'SYNC_FAILED' &&
      event.receiptId === context.receipt?.receiptId,
    retryableSyncFailure: ({ context }) => context.syncError?.retryable === true,
    hasRetryablePendingCommand: ({ context }) => context.pendingCommand !== null &&
      context.journalStatus === 'DISPATCHED' &&
      context.failure?.commitOutcome === 'UNKNOWN' && context.failure.retryable,
  },
  actions: {
    updateEligibility: assign({
      eligibility: ({ event, context }) => event.type === 'ELIGIBILITY_UPDATED'
        ? event.eligibility
        : context.eligibility,
    }),
    stagePendingVote: assign({
      pendingVote: ({ event }) => event.type === 'REQUEST_SET_VOTE'
        ? { clockRequestId: event.clockRequestId, slotId: event.slotId, choice: event.choice }
        : null,
      failure: null,
    }),
    applyPendingVote: assign({
      entries: ({ context }) => {
        if (context.pendingVote === null) return context.entries
        const event = context.pendingVote
        const remaining = context.entries.filter((entry) => entry.slotId !== event.slotId)
        return canonicalizeBallot([...remaining, { slotId: event.slotId, choice: event.choice }])
      },
      pendingVote: null,
      failure: null,
    }),
    captureVoteClock: assign({
      lastClockNowIso: ({ event }) => event.type === 'VOTE_CLOCK_SNAPSHOT' ? event.nowIso : null,
    }),
    captureVoteValidationFailure: assign({
      failure: ({ context, event }) => event.type === 'VOTE_CLOCK_SNAPSHOT'
        ? voteMutationFailure(context, event.nowIso)
        : context.failure,
      pendingVote: null,
    }),
    clearPendingVote: assign({ pendingVote: null }),
    captureAttempt: assign({
      operationId: ({ event }) => event.type === 'REQUEST_SUBMIT' ? event.operationId : null,
      submittedEntries: ({ context }) => canonicalizeBallot(context.entries),
      failure: null,
      lastClockNowIso: null,
    }),
    capturePendingCommand: assign({
      pendingCommand: ({ context }) => context.operationId === null || context.submittedEntries === null
        ? null
        : buildBallotCommand({
            eventId: context.eventId,
            actorId: context.actorId,
            pollRevision: context.pollRevision,
            operationId: context.operationId,
          }, context.submittedEntries, context.votingDeadlineIso),
      journalStatus: null,
      journalIntent: null,
      journalResolutionAttempt: 0,
      tombstoneTerminalDestination: null,
      tombstoneFailure: null,
    }),
    restorePendingCommand: assign({
      pendingCommand: ({ event }) => event.type === 'REHYDRATE_UNKNOWN_OUTCOME'
        ? event.command
        : null,
      operationId: ({ event }) => event.type === 'REHYDRATE_UNKNOWN_OUTCOME'
        ? event.command.identity.operationId
        : null,
      submittedEntries: ({ event }) => event.type === 'REHYDRATE_UNKNOWN_OUTCOME'
        ? canonicalizeBallot(event.command.entries)
        : null,
      failure: ({ event }) => event.type === 'REHYDRATE_UNKNOWN_OUTCOME'
        ? event.failure
        : null,
      journalStatus: ({ event }) => event.type === 'REHYDRATE_UNKNOWN_OUTCOME'
        ? event.journalStatus
        : null,
      journalIntent: null,
      journalResolutionAttempt: 0,
      tombstoneTerminalDestination: ({ event }) => event.type === 'REHYDRATE_UNKNOWN_OUTCOME'
        ? event.terminalDestination
        : null,
      tombstoneFailure: null,
    }),
    captureRehydrationInconsistency: assign({
      failure: {
        code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
      },
    }),
    captureRehydratedTerminalDestinationFailure: assign({
      failure: ({ event }) => event.type === 'REHYDRATE_UNKNOWN_OUTCOME' &&
        event.terminalDestination?.kind === 'TERMINAL_FAILURE'
        ? {
            code: event.terminalDestination.code,
            retryable: false,
            commitOutcome: event.terminalDestination.commitOutcome,
          }
        : {
            code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
          },
    }),
    captureClock: assign({
      lastClockNowIso: ({ event }) => event.type === 'SUBMISSION_CLOCK_SNAPSHOT' ? event.nowIso : null,
    }),
    captureValidationFailure: assign({
      failure: ({ context, event }) => event.type === 'SUBMISSION_CLOCK_SNAPSHOT'
        ? submissionFailure(context, event.nowIso)
        : context.failure,
    }),
    captureClockFailure: assign({
      failure: {
        code: 'CLOCK_UNAVAILABLE',
        retryable: true,
        commitOutcome: 'NOT_COMMITTED',
      },
    }),
    captureSubmissionFailure: assign({
      failure: ({ event, context }) => event.type === 'SUBMISSION_FAILED'
        ? event.failure
        : context.failure,
    }),
    captureCommandJournalFailure: assign({
      failure: ({ event, context }) => event.type === 'COMMAND_JOURNAL_FAILED'
        ? event.error
        : context.failure,
    }),
    captureJournalTransitionFailure: assign({
      failure: ({ event, context }) =>
        (event.type === 'COMMAND_DISPATCH_MARK_FAILED' ||
          event.type === 'COMMAND_DISPATCH_MARK_UNKNOWN' ||
          event.type === 'COMMAND_CANCEL_FAILED' || event.type === 'COMMAND_CANCEL_UNKNOWN')
          ? event.error
          : context.failure,
    }),
    captureResolvedAuthorizeNotCommitted: assign({
      journalStatus: 'STAGED_NOT_DISPATCHED',
      failure: {
        code: 'COMMAND_DISPATCH_MARK_FAILED', retryable: true, commitOutcome: 'NOT_COMMITTED',
      },
    }),
    captureResolvedCancelNotCommitted: assign({
      journalStatus: 'STAGED_NOT_DISPATCHED',
      failure: {
        code: 'COMMAND_CANCEL_FAILED', retryable: true, commitOutcome: 'NOT_COMMITTED',
      },
    }),
    captureJournalResolutionProblem: assign({
      failure: ({ context, event }) => event.type === 'COMMAND_JOURNAL_STATUS_READ_FAILED'
        ? event.error
        : {
            code: 'REPOSITORY_INCONSISTENT', retryable: true,
            commitOutcome: 'UNKNOWN' as const,
          },
    }),
    captureTerminalJournalResolutionInconsistency: assign({
      failure: {
        code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
      },
    }),
    captureCancellationTombstoneFailure: assign({
      tombstoneFailure: ({ event, context }) =>
        (event.type === 'DISPATCH_CANCELLATION_TOMBSTONE_FAILED' ||
          event.type === 'DISPATCH_CANCELLATION_TOMBSTONE_UNKNOWN')
          ? event.error
          : context.tombstoneFailure,
    }),
    captureCancellationTombstoneRetryableFailure: assign({
      journalStatus: 'DISPATCHED',
      tombstoneFailure: {
        code: 'COMMAND_CANCELLATION_TOMBSTONE_FAILED', retryable: true,
        commitOutcome: 'NOT_COMMITTED',
      },
    }),
    captureTerminalTombstoneInconsistency: assign({
      tombstoneFailure: {
        code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
      },
    }),
    captureRetryableTombstoneResolutionInconsistency: assign({
      tombstoneFailure: {
        code: 'REPOSITORY_INCONSISTENT', retryable: true, commitOutcome: 'UNKNOWN',
      },
    }),
    captureReceipt: assign({
      receipt: ({ event, context }) => event.type === 'LOCAL_COMMIT'
        ? event.receipt
        : context.receipt,
      failure: null,
      syncError: null,
    }),
    captureMalformedReceipt: assign({
      failure: {
        code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
      },
    }),
    captureSyncFailure: assign({
      syncError: ({ event, context }) => event.type === 'SYNC_FAILED'
        ? event.error
        : context.syncError,
    }),
    clearSyncFailure: assign({ syncError: null }),
    markReceiptServerAcknowledged: assign({
      receipt: ({ context, event }) => context.receipt === null || event.type !== 'SYNC_COMPLETED'
        ? null
        : {
            ...context.receipt,
            syncStatus: 'SYNCED' as const,
            serverReceiptId: event.ack.serverReceiptId,
          },
      syncError: null,
    }),
    resetForRevision: assign({
      submittedEntries: null,
      pendingCommand: null,
      journalStatus: null,
      journalIntent: null,
      journalResolutionAttempt: 0,
      tombstoneTerminalDestination: null,
      tombstoneFailure: null,
      operationId: null,
      lastClockNowIso: null,
      failure: null,
    }),
    requestClock: assign({
      effects: ({ context }) => [...context.effects, 'requestClockSnapshot'],
    }),
    persistCommand: assign({
      effects: ({ context }) => [...context.effects, 'persistPendingCommand'],
    }),
    markCommandDispatched: assign({
      effects: ({ context }) => [...context.effects, 'markPendingCommandDispatched'],
      journalIntent: 'AUTHORIZE_DISPATCH',
    }),
    captureStagedStatus: assign({ journalStatus: 'STAGED_NOT_DISPATCHED' }),
    captureDispatchedStatus: assign({ journalStatus: 'DISPATCHED' }),
    cancelCommandJournal: assign({
      effects: ({ context }) => [...context.effects, 'cancelPendingCommandJournal'],
      journalIntent: 'CANCEL',
    }),
    resolveCommandJournal: assign({
      effects: ({ context }) => [...context.effects, 'resolvePendingCommandJournal'],
      journalResolutionAttempt: ({ context }) => context.journalResolutionAttempt + 1,
    }),
    captureCancelledStatus: assign({ journalStatus: 'CANCELLED' }),
    tombstoneDispatchedCancellation: assign({
      effects: ({ context }) => [...context.effects, 'tombstoneDispatchedCancellation'],
      journalIntent: 'TOMBSTONE_DISPATCHED_CANCELLATION',
      tombstoneTerminalDestination: { kind: 'CANCELLED' },
      tombstoneFailure: null,
    }),
    tombstoneDispatchedTerminalFailure: assign({
      effects: ({ context }) => [...context.effects, 'tombstoneDispatchedCancellation'],
      journalIntent: 'TOMBSTONE_DISPATCHED_CANCELLATION',
      tombstoneTerminalDestination: ({ context }) => ({
        kind: 'TERMINAL_FAILURE' as const,
        code: context.failure?.code ?? 'REPOSITORY_INCONSISTENT',
        commitOutcome: context.failure?.commitOutcome ?? 'UNKNOWN',
      }),
      tombstoneFailure: null,
    }),
    tombstoneDispatchedRevision: assign({
      effects: ({ context }) => [...context.effects, 'tombstoneDispatchedCancellation'],
      journalIntent: 'TOMBSTONE_DISPATCHED_CANCELLATION',
      tombstoneTerminalDestination: { kind: 'REVISED' },
      tombstoneFailure: null,
    }),
    retryDispatchedTombstone: assign({
      effects: ({ context }) => [...context.effects, 'tombstoneDispatchedCancellation'],
      journalIntent: 'TOMBSTONE_DISPATCHED_CANCELLATION',
    }),
    captureDispatchedCancellationTombstone: assign({
      journalStatus: 'DISPATCH_CANCELLATION_TOMBSTONED',
      tombstoneFailure: null,
    }),
    returnToEventOnBack: assign({
      effects: ({ context }) => [...context.effects, 'returnToEventOnBack'],
    }),
    dispatchAtomicCommand: assign({
      effects: ({ context }) => [...context.effects, 'dispatchAtomicBallotCommand'],
    }),
    requestCancellation: assign({
      effects: ({ context }) => [...context.effects, 'requestOperationCancellation'],
    }),
    navigateAfterCommit: assign({
      effects: ({ context }) => [...context.effects, 'navigateAfterLocalCommit'],
    }),
    retrySync: assign({
      effects: ({ context }) => [...context.effects, 'retryReceiptSync'],
    }),
  },
}).createMachine({
  id: 'pollBallotSubmission',
  initial: 'editing',
  context: ({ input }) => ({
    ...input,
    entries: canonicalizeBallot(input.initialEntries ?? []),
    submittedEntries: null,
    pendingCommand: null,
    journalStatus: null,
    journalIntent: null,
    journalResolutionAttempt: 0,
    tombstoneTerminalDestination: null,
    tombstoneFailure: null,
    pendingVote: null,
    operationId: null,
    lastClockNowIso: null,
    receipt: null,
    failure: null,
    syncError: null,
    effects: [],
  }),
  on: {
    ELIGIBILITY_UPDATED: { actions: 'updateEligibility' },
  },
  states: {
    editing: {
      on: {
        REQUEST_SET_VOTE: {
          guard: 'canRequestVoteClock',
          target: 'checkingVoteClock',
          actions: ['stagePendingVote', 'requestClock'],
        },
        REQUEST_SUBMIT: {
          guard: 'canBeginSubmission',
          target: 'checkingClock',
          actions: ['captureAttempt', 'requestClock'],
        },
        REHYDRATE_UNKNOWN_OUTCOME: [
          {
            guard: 'rehydratedRetryableCommandIsValid',
            target: 'outcomeUnknown',
            actions: 'restorePendingCommand',
          },
          {
            guard: 'rehydratedTerminalCommandIsValid',
            target: 'resolutionFailed',
            actions: 'restorePendingCommand',
          },
          {
            guard: 'rehydratedStagedCommandIsValid',
            target: 'cancellingJournal',
            actions: ['restorePendingCommand', 'cancelCommandJournal'],
          },
          {
            guard: 'rehydratedPreDispatchCancelledCommandIsValid',
            target: 'cancelled',
            actions: 'restorePendingCommand',
          },
          {
            guard: 'rehydratedCancelledTombstoneIsValid',
            target: 'cancelled',
            actions: ['restorePendingCommand', 'returnToEventOnBack'],
          },
          {
            guard: 'rehydratedTerminalFailureTombstoneIsValid',
            target: 'terminalFailure',
            actions: ['restorePendingCommand', 'captureRehydratedTerminalDestinationFailure'],
          },
          {
            guard: 'rehydratedRevisionTombstoneIsValid',
            target: 'editing',
            reenter: true,
            actions: ['restorePendingCommand', 'resetForRevision'],
          },
          {
            guard: 'rehydratedSnapshotOrCommandIsInconsistent',
            target: 'resolutionFailed',
            actions: 'captureRehydrationInconsistency',
          },
        ],
        CANCEL: { target: 'cancelled' },
        CLOSE: { target: 'detached' },
        BACK: { target: 'detached' },
      },
    },
    checkingVoteClock: {
      on: {
        VOTE_CLOCK_SNAPSHOT: [
          {
            guard: 'voteClockMatchesAndMutationIsValid',
            target: 'editing',
            actions: ['captureVoteClock', 'applyPendingVote'],
          },
          {
            guard: 'voteClockMatches',
            target: 'editing',
            actions: ['captureVoteClock', 'captureVoteValidationFailure'],
          },
        ],
        VOTE_CLOCK_FAILED: {
          guard: 'voteClockFailureMatches',
          target: 'editing',
          actions: ['captureClockFailure', 'clearPendingVote'],
        },
        CANCEL: { target: 'cancelled' },
        CLOSE: { target: 'cancelled' },
        BACK: { target: 'cancelled' },
      },
    },
    checkingClock: {
      on: {
        SUBMISSION_CLOCK_SNAPSHOT: [
          {
            guard: 'clockMatchesAndSubmissionIsValid',
            target: 'persistingCommand',
            actions: ['captureClock', 'capturePendingCommand', 'persistCommand'],
          },
          {
            guard: 'clockMatches',
            target: 'failedBeforeCommit',
            actions: ['captureClock', 'captureValidationFailure'],
          },
        ],
        SUBMISSION_CLOCK_FAILED: {
          guard: 'clockFailureMatches',
          target: 'failedBeforeCommit',
          actions: 'captureClockFailure',
        },
        CANCEL: { target: 'cancelled' },
        CLOSE: { target: 'cancelled' },
        BACK: { target: 'cancelled' },
      },
    },
    persistingCommand: {
      on: {
        COMMAND_JOURNALED: {
          guard: 'commandJournalMatches',
          target: 'authorizingDispatch',
          actions: ['captureStagedStatus', 'markCommandDispatched'],
        },
        COMMAND_JOURNAL_FAILED: [
          {
            guard: 'retryableCommandJournalFailureMatches',
            target: 'failedBeforeCommit',
            actions: 'captureCommandJournalFailure',
          },
          {
            guard: 'terminalCommandJournalFailureMatches',
            target: 'terminalFailure',
            actions: 'captureCommandJournalFailure',
          },
        ],
        CANCEL: { target: 'cancellingJournal', actions: 'cancelCommandJournal' },
        CLOSE: { target: 'cancellingJournal', actions: 'cancelCommandJournal' },
        BACK: { target: 'cancellingJournal', actions: 'cancelCommandJournal' },
      },
    },
    authorizingDispatch: {
      on: {
        COMMAND_DISPATCH_MARKED: {
          guard: 'commandStatusMatches',
          target: 'submitting',
          actions: ['captureDispatchedStatus', 'dispatchAtomicCommand'],
        },
        COMMAND_DISPATCH_MARK_FAILED: [
          {
            guard: 'retryableDispatchMarkFailureMatches',
            target: 'failedBeforeCommit',
            actions: 'captureJournalTransitionFailure',
          },
          {
            guard: 'terminalDispatchMarkFailureMatches',
            target: 'terminalFailure',
            actions: 'captureJournalTransitionFailure',
          },
        ],
        COMMAND_DISPATCH_MARK_UNKNOWN: {
          guard: 'unknownDispatchMarkMatches',
          target: 'resolvingJournalStatus',
          actions: ['captureJournalTransitionFailure', 'resolveCommandJournal'],
        },
        CANCEL: { target: 'cancellingJournal', actions: 'cancelCommandJournal' },
        CLOSE: { target: 'cancellingJournal', actions: 'cancelCommandJournal' },
        BACK: { target: 'cancellingJournal', actions: 'cancelCommandJournal' },
      },
    },
    cancellingJournal: {
      on: {
        COMMAND_DISPATCH_MARKED: {
          guard: 'commandStatusMatches',
          target: 'cancelling',
          actions: ['captureDispatchedStatus', 'dispatchAtomicCommand', 'requestCancellation'],
        },
        COMMAND_CANCELLED: {
          guard: 'commandStatusMatches',
          target: 'cancelled',
          actions: 'captureCancelledStatus',
        },
        COMMAND_CANCEL_FAILED: [
          {
            guard: 'retryableCancelFailureMatches',
            target: 'journalCancellationFailed',
            actions: 'captureJournalTransitionFailure',
          },
          {
            guard: 'terminalCancelFailureMatches',
            target: 'terminalFailure',
            actions: 'captureJournalTransitionFailure',
          },
        ],
        COMMAND_CANCEL_UNKNOWN: {
          guard: 'unknownCancelMatches',
          target: 'resolvingJournalStatus',
          actions: ['captureJournalTransitionFailure', 'resolveCommandJournal'],
        },
      },
    },
    journalCancellationFailed: {
      on: {
        RETRY_JOURNAL_CANCELLATION: {
          target: 'cancellingJournal',
          actions: 'cancelCommandJournal',
        },
      },
    },
    resolvingJournalStatus: {
      on: {
        COMMAND_JOURNAL_STATUS_RESOLVED: [
          {
            guard: 'resolvedCancelled',
            target: 'cancelled',
            actions: [
              'captureDispatchedCancellationTombstone', 'returnToEventOnBack',
            ],
          },
          {
            guard: 'resolvedTerminalFailureAfterTombstone',
            target: 'terminalFailure',
            actions: 'captureDispatchedCancellationTombstone',
          },
          {
            guard: 'resolvedRevisionAfterTombstone',
            target: 'editing',
            actions: ['captureDispatchedCancellationTombstone', 'resetForRevision'],
          },
          {
            guard: 'resolvedDispatchedForTombstoneRetry',
            target: 'dispatchedCancellationTombstoneFailed',
            actions: 'captureCancellationTombstoneRetryableFailure',
          },
          {
            guard: 'resolvedDispatchedForAuthorize',
            target: 'submitting',
            actions: ['captureDispatchedStatus', 'dispatchAtomicCommand'],
          },
          {
            guard: 'resolvedStagedForAuthorize',
            target: 'failedBeforeCommit',
            actions: 'captureResolvedAuthorizeNotCommitted',
          },
          {
            guard: 'resolvedPreDispatchCancelled',
            target: 'cancelled',
            actions: 'captureCancelledStatus',
          },
          {
            guard: 'resolvedDispatchedForCancel',
            target: 'cancelling',
            actions: ['captureDispatchedStatus', 'dispatchAtomicCommand', 'requestCancellation'],
          },
          {
            guard: 'resolvedStagedForCancel',
            target: 'journalCancellationFailed',
            actions: 'captureResolvedCancelNotCommitted',
          },
        ],
        COMMAND_JOURNAL_STATUS_READ_FAILED: [
          {
            guard: 'nonRetryableTombstoneJournalReadFailureMatches',
            target: 'dispatchedCancellationTombstoneFailed',
            actions: 'captureTerminalTombstoneInconsistency',
          },
          {
            guard: 'nonRetryableJournalReadFailureMatches',
            target: 'terminalFailure',
            actions: 'captureTerminalJournalResolutionInconsistency',
          },
          {
            guard: 'tombstoneJournalResolutionProblemMatchesAndCanRetry',
            target: 'dispatchedCancellationTombstoneFailed',
            actions: 'captureRetryableTombstoneResolutionInconsistency',
          },
          {
            guard: 'journalResolutionProblemMatchesAndCanRetry',
            target: 'journalResolutionFailed',
            actions: 'captureJournalResolutionProblem',
          },
          {
            guard: 'tombstoneJournalResolutionProblemMatchesAndExhausted',
            target: 'dispatchedCancellationTombstoneFailed',
            actions: 'captureTerminalTombstoneInconsistency',
          },
          {
            guard: 'journalResolutionProblemMatchesAndExhausted',
            target: 'resolutionFailed',
            actions: 'captureTerminalJournalResolutionInconsistency',
          },
        ],
        COMMAND_JOURNAL_STATUS_MISSING: [
          {
            guard: 'tombstoneJournalResolutionProblemMatchesAndCanRetry',
            target: 'dispatchedCancellationTombstoneFailed',
            actions: 'captureRetryableTombstoneResolutionInconsistency',
          },
          {
            guard: 'journalResolutionProblemMatchesAndCanRetry',
            target: 'journalResolutionFailed',
            actions: 'captureJournalResolutionProblem',
          },
          {
            guard: 'tombstoneJournalResolutionProblemMatchesAndExhausted',
            target: 'dispatchedCancellationTombstoneFailed',
            actions: 'captureTerminalTombstoneInconsistency',
          },
          {
            guard: 'journalResolutionProblemMatchesAndExhausted',
            target: 'resolutionFailed',
            actions: 'captureTerminalJournalResolutionInconsistency',
          },
        ],
        COMMAND_JOURNAL_STATUS_MALFORMED: [
          {
            guard: 'tombstoneJournalResolutionProblemMatchesAndCanRetry',
            target: 'dispatchedCancellationTombstoneFailed',
            actions: 'captureRetryableTombstoneResolutionInconsistency',
          },
          {
            guard: 'journalResolutionProblemMatchesAndCanRetry',
            target: 'journalResolutionFailed',
            actions: 'captureJournalResolutionProblem',
          },
          {
            guard: 'tombstoneJournalResolutionProblemMatchesAndExhausted',
            target: 'dispatchedCancellationTombstoneFailed',
            actions: 'captureTerminalTombstoneInconsistency',
          },
          {
            guard: 'journalResolutionProblemMatchesAndExhausted',
            target: 'resolutionFailed',
            actions: 'captureTerminalJournalResolutionInconsistency',
          },
        ],
      },
    },
    journalResolutionFailed: {
      on: {
        RETRY_JOURNAL_RESOLUTION: {
          guard: 'canRetryJournalResolution',
          target: 'resolvingJournalStatus',
          actions: 'resolveCommandJournal',
        },
      },
    },
    submitting: {
      on: {
        LOCAL_COMMIT: [
          {
            guard: 'receiptMatchesPendingOperation',
            target: 'committed.pendingSync',
            actions: ['captureReceipt', 'navigateAfterCommit'],
          },
          {
            guard: 'malformedReceiptForPendingOperation',
            target: 'resolutionFailed',
            actions: 'captureMalformedReceipt',
          },
        ],
        SUBMISSION_FAILED: [
          {
            guard: 'retryableKnownFailureMatches',
            target: 'failedBeforeCommit',
            actions: 'captureSubmissionFailure',
          },
          {
            guard: 'terminalKnownFailureMatches',
            target: 'tombstoningDispatchedCancellation',
            actions: ['captureSubmissionFailure', 'tombstoneDispatchedTerminalFailure'],
          },
          {
            guard: 'unknownFailureMatches',
            target: 'outcomeUnknown',
            actions: 'captureSubmissionFailure',
          },
          {
            guard: 'terminalUnknownFailureMatches',
            target: 'resolutionFailed',
            actions: 'captureSubmissionFailure',
          },
        ],
        CANCEL: {
          target: 'cancelling',
          actions: 'requestCancellation',
        },
        CLOSE: {
          target: 'cancelling',
          actions: 'requestCancellation',
        },
        BACK: {
          target: 'cancelling',
          actions: 'requestCancellation',
        },
      },
    },
    failedBeforeCommit: {
      on: {
        RETRY_SUBMISSION: [
          {
            guard: 'retryableKnownFailureWithDispatchedJournal',
            target: 'checkingDispatchedRetryClock',
            actions: 'requestClock',
          },
          {
            guard: 'retryableKnownFailureWithoutDispatchedJournal',
            target: 'checkingClock',
            actions: 'requestClock',
          },
        ],
        REVISE_BALLOT: [
          {
            guard: 'knownNonCommitHasDispatchedJournal',
            target: 'tombstoningDispatchedCancellation',
            actions: 'tombstoneDispatchedRevision',
          },
          { target: 'editing', actions: 'resetForRevision' },
        ],
        CANCEL: [
          {
            guard: 'knownNonCommitHasDispatchedJournal',
            target: 'tombstoningDispatchedCancellation',
            actions: 'tombstoneDispatchedCancellation',
          },
          { target: 'cancelled' },
        ],
        CLOSE: [
          {
            guard: 'knownNonCommitHasDispatchedJournal',
            target: 'tombstoningDispatchedCancellation',
            actions: 'tombstoneDispatchedCancellation',
          },
          { target: 'detached' },
        ],
        BACK: [
          {
            guard: 'knownNonCommitHasDispatchedJournal',
            target: 'tombstoningDispatchedCancellation',
            actions: 'tombstoneDispatchedCancellation',
          },
          { target: 'detached' },
        ],
      },
    },
    checkingDispatchedRetryClock: {
      on: {
        SUBMISSION_CLOCK_SNAPSHOT: [
          {
            guard: 'clockMatchesAndSubmissionIsValid',
            target: 'submitting',
            actions: ['captureClock', 'dispatchAtomicCommand'],
          },
          {
            guard: 'clockMatches',
            target: 'failedBeforeCommit',
            actions: ['captureClock', 'captureValidationFailure'],
          },
        ],
        SUBMISSION_CLOCK_FAILED: {
          guard: 'clockFailureMatches',
          target: 'failedBeforeCommit',
          actions: 'captureClockFailure',
        },
        CANCEL: { target: 'cancelling', actions: 'requestCancellation' },
        CLOSE: { target: 'cancelling', actions: 'requestCancellation' },
        BACK: { target: 'cancelling', actions: 'requestCancellation' },
      },
    },
    outcomeUnknown: {
      on: {
        RETRY_RESOLUTION: {
          guard: 'hasRetryablePendingCommand',
          target: 'submitting',
          actions: 'dispatchAtomicCommand',
        },
        LOCAL_COMMIT: [
          {
            guard: 'receiptMatchesPendingOperation',
            target: 'committed.pendingSync',
            actions: ['captureReceipt', 'navigateAfterCommit'],
          },
          {
            guard: 'malformedReceiptForPendingOperation',
            target: 'resolutionFailed',
            actions: 'captureMalformedReceipt',
          },
        ],
        CANCEL: {
          target: 'cancelling',
          actions: 'requestCancellation',
        },
        CLOSE: {
          target: 'cancelling',
          actions: 'requestCancellation',
        },
        BACK: {
          target: 'cancelling',
          actions: 'requestCancellation',
        },
      },
    },
    cancelling: {
      on: {
        CANCELLATION_CONFIRMED: {
          guard: 'operationMatchesCancellation',
          target: 'tombstoningDispatchedCancellation',
          actions: 'tombstoneDispatchedCancellation',
        },
        CANCELLATION_UNKNOWN: {
          guard: 'operationMatchesCancellation',
          target: 'outcomeUnknown',
        },
        LOCAL_COMMIT: [
          {
            guard: 'receiptMatchesPendingOperation',
            target: 'committed.pendingSync',
            actions: ['captureReceipt', 'navigateAfterCommit'],
          },
          {
            guard: 'malformedReceiptForPendingOperation',
            target: 'resolutionFailed',
            actions: 'captureMalformedReceipt',
          },
        ],
        CLOSE: {},
        BACK: {},
      },
    },
    tombstoningDispatchedCancellation: {
      on: {
        DISPATCH_CANCELLATION_TOMBSTONED: [
          {
            guard: 'cancellationTombstoneMatches',
            target: 'cancelled',
            actions: [
              'captureDispatchedCancellationTombstone', 'returnToEventOnBack',
            ],
          },
          {
            guard: 'terminalFailureTombstoneMatches',
            target: 'terminalFailure',
            actions: 'captureDispatchedCancellationTombstone',
          },
          {
            guard: 'revisionTombstoneMatches',
            target: 'editing',
            actions: ['captureDispatchedCancellationTombstone', 'resetForRevision'],
          },
        ],
        DISPATCH_CANCELLATION_TOMBSTONE_FAILED: [
          {
            guard: 'retryableCancellationTombstoneFailureMatches',
            target: 'dispatchedCancellationTombstoneFailed',
            actions: 'captureCancellationTombstoneFailure',
          },
          {
            guard: 'terminalCancellationTombstoneFailureMatches',
            target: 'dispatchedCancellationTombstoneFailed',
            actions: 'captureCancellationTombstoneFailure',
          },
        ],
        DISPATCH_CANCELLATION_TOMBSTONE_UNKNOWN: {
          guard: 'unknownCancellationTombstoneFailureMatches',
          target: 'resolvingJournalStatus',
          actions: ['captureCancellationTombstoneFailure', 'resolveCommandJournal'],
        },
        CLOSE: {},
        BACK: {},
      },
    },
    dispatchedCancellationTombstoneFailed: {
      on: {
        RETRY_JOURNAL_CANCELLATION: {
          guard: 'canRetryDispatchedTombstone',
          target: 'tombstoningDispatchedCancellation',
          actions: 'retryDispatchedTombstone',
        },
        CLOSE: {},
        BACK: {},
      },
    },
    committed: {
      initial: 'pendingSync',
      states: {
        pendingSync: {
          on: {
            SYNC_COMPLETED: {
              guard: 'syncAckMatches',
              target: '#pollBallotSubmission.synced',
              actions: 'markReceiptServerAcknowledged',
            },
            SYNC_FAILED: {
              guard: 'syncFailureMatches',
              target: 'syncFailed',
              actions: 'captureSyncFailure',
            },
            CLOSE: { target: '#pollBallotSubmission.detached' },
            BACK: { target: '#pollBallotSubmission.detached' },
          },
        },
        syncFailed: {
          on: {
            RETRY_SYNC: {
              guard: 'retryableSyncFailure',
              target: 'pendingSync',
              actions: ['clearSyncFailure', 'retrySync'],
            },
            SYNC_COMPLETED: {
              guard: 'syncAckMatches',
              target: '#pollBallotSubmission.synced',
              actions: 'markReceiptServerAcknowledged',
            },
            CLOSE: { target: '#pollBallotSubmission.detached' },
            BACK: { target: '#pollBallotSubmission.detached' },
          },
        },
      },
    },
    synced: { type: 'final' },
    resolutionFailed: { type: 'final' },
    terminalFailure: { type: 'final' },
    cancelled: { type: 'final' },
    detached: { type: 'final' },
  },
})
