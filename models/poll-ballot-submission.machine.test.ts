import assert from 'node:assert/strict'
import test from 'node:test'
import { createActor } from 'xstate'

import {
  authorizeBallotServerMutation,
  ballotFingerprint,
  ballotOperationKey,
  buildBallotCommand,
  canMutateVote,
  classifyIdempotency,
  derivePollVoteAccess,
  isValidBallotJournalSnapshot,
  isValidPollRevision,
  parseIsoInstant,
  pendingSyncPayloadsForDispatch,
  pollBallotSubmissionMachine,
  projectBallotChoiceAffordance,
  projectBallotTombstoneDestination,
  projectPendingBallotSyncJoins,
  resolveConcurrentIdempotencyCollision,
  projectPendingBallotMetadataList,
  validateBallotEntries,
  type BallotReceipt,
  type PendingBallotSyncMetadataRow,
  type PollBallotInput,
} from './poll-ballot-submission.machine.ts'

const initialEntries = [
  { slotId: 'slot-1', choice: 'YES' },
  { slotId: 'slot-2', choice: 'MAYBE' },
] as const

const deadlineIso = '2026-08-28T12:00:00.000Z'

const input = (overrides: Partial<PollBallotInput> = {}): PollBallotInput => ({
  eventId: 'event-1',
  actorId: 'participant-1',
  pollRevision: 7,
  validSlotIds: ['slot-1', 'slot-2'],
  votingDeadlineIso: deadlineIso,
  maxJournalResolutionAttempts: 2,
  eligibility: {
    repositoryAvailable: true,
    eventExists: true,
    voteAccess: { kind: 'ALLOWED', basis: 'ACCEPTED_ACTIVE_MEMBER' },
    eventStatus: 'POLLING',
  },
  initialEntries,
  ...overrides,
})

const actorFor = (machineInput = input()) => createActor(
  pollBallotSubmissionMachine,
  { input: machineInput },
).start()

const requestAndProvideClock = (
  actor: ReturnType<typeof actorFor>,
  nowIso = '2026-08-28T11:59:59.999Z',
) => {
  actor.send({ type: 'REQUEST_SUBMIT', operationId: 'operation-1' })
  actor.send({ type: 'SUBMISSION_CLOCK_SNAPSHOT', operationId: 'operation-1', nowIso })
  const pending = actor.getSnapshot().context.pendingCommand
  if (actor.getSnapshot().matches('persistingCommand') && pending !== null) {
    actor.send({
      type: 'COMMAND_JOURNALED',
      operationKey: ballotOperationKey(pending.identity),
      ballotFingerprint: pending.ballotFingerprint,
    })
    actor.send({
      type: 'COMMAND_DISPATCH_MARKED',
      operationKey: ballotOperationKey(pending.identity),
      ballotFingerprint: pending.ballotFingerprint,
    })
  }
}

const requestVoteAndProvideClock = (
  actor: ReturnType<typeof actorFor>,
  nowIso: string,
) => {
  actor.send({
    type: 'REQUEST_SET_VOTE', clockRequestId: 'vote-clock-1',
    slotId: 'slot-1', choice: 'NO',
  })
  actor.send({ type: 'VOTE_CLOCK_SNAPSHOT', clockRequestId: 'vote-clock-1', nowIso })
}

const enterDispatchJournalResolution = (maxJournalResolutionAttempts = 2) => {
  const actor = actorFor(input({ maxJournalResolutionAttempts }))
  actor.send({ type: 'REQUEST_SUBMIT', operationId: 'operation-1' })
  actor.send({
    type: 'SUBMISSION_CLOCK_SNAPSHOT', operationId: 'operation-1',
    nowIso: '2026-08-28T11:59:59.999Z',
  })
  const pending = actor.getSnapshot().context.pendingCommand!
  actor.send({
    type: 'COMMAND_JOURNALED', operationKey: ballotOperationKey(pending.identity),
    ballotFingerprint: pending.ballotFingerprint,
  })
  actor.send({
    type: 'COMMAND_DISPATCH_MARK_UNKNOWN', operationKey: ballotOperationKey(pending.identity),
    ballotFingerprint: pending.ballotFingerprint,
    error: { code: 'COMMAND_DISPATCH_MARK_FAILED', retryable: true, commitOutcome: 'UNKNOWN' },
  })
  return { actor, pending }
}

const command = buildBallotCommand({
  eventId: 'event-1', actorId: 'participant-1', pollRevision: 7, operationId: 'operation-1',
}, initialEntries, deadlineIso)

const receipt = (syncStatus: BallotReceipt['syncStatus'] = 'LOCAL_PENDING'): BallotReceipt => ({
  receiptId: 'receipt-1',
  operationKey: ballotOperationKey(command.identity),
  operationId: 'operation-1',
  eventId: 'event-1',
  actorId: 'participant-1',
  pollRevision: 7,
  ballotFingerprint: ballotFingerprint(initialEntries),
  acceptedAtIso: '2026-08-28T11:59:59.999Z',
  syncStatus,
  syncPayload: {
    schemaVersion: 1,
    localReceiptId: 'receipt-1',
    command,
  },
  serverReceiptId: null,
})

const syncAck = () => ({
  localReceiptId: 'receipt-1',
  serverReceiptId: 'server-receipt-1',
  identity: command.identity,
  ballotFingerprint: command.ballotFingerprint,
  outcome: 'APPLIED' as const,
})

test('vote mutation requires valid injected ISO instants and strict now < deadline', () => {
  assert.equal(canMutateVote('2026-08-28T12:00:00.000Z', '2026-08-28T11:59:59.999Z'), true)
  assert.equal(canMutateVote('2026-08-28T12:00:00.000Z', '2026-08-28T12:00:00.000Z'), false)
  assert.equal(canMutateVote('2026-08-28T12:00:00.000Z', '2026-08-28T12:00:00'), false)
  assert.equal(canMutateVote('invalid', '2026-08-28T11:00:00.000Z'), false)
  assert.equal(parseIsoInstant('2026-02-29T11:00:00.000Z'), null)
  assert.equal(parseIsoInstant('2026-04-31T11:00:00.000Z'), null)
  assert.equal(parseIsoInstant('2026-08-28T24:00:00.000Z'), null)
  assert.equal(parseIsoInstant('2026-08-28T11:00:00.000+14:01'), null)
  assert.notEqual(parseIsoInstant('2024-02-29T11:00:00.123456Z'), null)
})

test('closed ballot choices remain visible but disabled for interaction and VoiceOver', () => {
  assert.deepEqual(projectBallotChoiceAffordance(false), {
    visible: true, isEnabled: false, semanticState: 'DISABLED',
    voiceOverState: 'DISABLED', disabledReason: 'POLL_CLOSED',
  })
  assert.equal(projectBallotChoiceAffordance(true).isEnabled, true)
})

test('vote access admits organizer and accepted active member only from typed access axes', () => {
  assert.deepEqual(derivePollVoteAccess({
    actorId: 'organizer-1', eventOrganizerId: 'organizer-1', accessUserId: 'organizer-1',
    role: 'ORGANIZER', membership: 'NON_MEMBER', rsvp: 'ACCEPTED',
  }), { kind: 'ALLOWED', basis: 'ORGANIZER' })

  assert.deepEqual(derivePollVoteAccess({
    actorId: 'member-1', eventOrganizerId: 'organizer-1', accessUserId: 'member-1',
    role: 'MEMBER', membership: 'ACTIVE_MEMBER', rsvp: 'ACCEPTED',
  }), { kind: 'ALLOWED', basis: 'ACCEPTED_ACTIVE_MEMBER' })

  for (const decision of [
    derivePollVoteAccess({
      actorId: 'pending-1', eventOrganizerId: 'organizer-1', accessUserId: 'pending-1',
      role: 'MEMBER', membership: 'ACTIVE_MEMBER', rsvp: 'PENDING',
    }),
    derivePollVoteAccess({
      actorId: 'removed-1', eventOrganizerId: 'organizer-1', accessUserId: 'removed-1',
      role: 'MEMBER', membership: 'REMOVED', rsvp: 'ACCEPTED',
    }),
    derivePollVoteAccess({
      actorId: 'intruder', eventOrganizerId: 'organizer-1', accessUserId: 'member-1',
      role: 'MEMBER', membership: 'ACTIVE_MEMBER', rsvp: 'ACCEPTED',
    }),
    derivePollVoteAccess({
      actorId: '', eventOrganizerId: 'organizer-1', accessUserId: 'organizer-1',
      role: 'ORGANIZER', membership: 'NON_MEMBER', rsvp: 'ACCEPTED',
    }),
    derivePollVoteAccess({
      actorId: 'organizer-1', eventOrganizerId: 'organizer-1', accessUserId: '',
      role: 'ORGANIZER', membership: 'NON_MEMBER', rsvp: 'ACCEPTED',
    }),
    derivePollVoteAccess({
      actorId: 'organizer-1', eventOrganizerId: ' ', accessUserId: 'organizer-1',
      role: 'ORGANIZER', membership: 'NON_MEMBER', rsvp: 'ACCEPTED',
    }),
    derivePollVoteAccess({
      actorId: ' organizer-1', eventOrganizerId: 'organizer-1', accessUserId: 'organizer-1',
      role: 'ORGANIZER', membership: 'NON_MEMBER', rsvp: 'ACCEPTED',
    }),
  ]) assert.equal(decision.kind, 'DENIED')
})

test('v1 fingerprint has stable Kotlin-compatible golden vectors for Unicode and reserved text', () => {
  assert.equal(ballotFingerprint([
    { slotId: 'space slot', choice: 'NO' },
    { slotId: 'a/b?c=d&x', choice: 'YES' },
  ]), 'v1|612f623f633d642678=YES|737061636520736c6f74=NO')

  assert.equal(ballotFingerprint([
    { slotId: '🌍', choice: 'NO' },
    { slotId: '東京', choice: 'YES' },
    { slotId: 'é', choice: 'MAYBE' },
  ]), 'v1|c3a9=MAYBE|e69db1e4baac=YES|f09f8c8d=NO')
})

test('idempotency scopes operation id by the full tuple and conflicts only on changed same-tuple payload', () => {
  const existing = { command, receipt: receipt() }
  assert.equal(classifyIdempotency(existing, command).kind, 'RETURN_EXISTING_RECEIPT')

  const anotherEvent = buildBallotCommand({
    ...command.identity, eventId: 'event-2', operationId: 'operation-1',
  }, command.entries, deadlineIso)
  assert.equal(classifyIdempotency(existing, anotherEvent).kind, 'NEW_TUPLE')
  assert.notEqual(ballotOperationKey(command.identity), ballotOperationKey(anotherEvent.identity))

  const changedPayload = buildBallotCommand(command.identity, [
    { slotId: 'slot-1', choice: 'NO' },
    { slotId: 'slot-2', choice: 'MAYBE' },
  ], deadlineIso)
  assert.equal(classifyIdempotency(existing, changedPayload).kind, 'IDEMPOTENCY_CONFLICT')
})

test('poll revision is an exact non-negative safe integer for TypeScript and Kotlin interop', () => {
  assert.equal(isValidPollRevision(0), true)
  assert.equal(isValidPollRevision(Number.MAX_SAFE_INTEGER), true)
  assert.equal(isValidPollRevision(-1), false)
  assert.equal(isValidPollRevision(1.5), false)
  assert.equal(isValidPollRevision(Number.MAX_SAFE_INTEGER + 1), false)
  assert.throws(() => buildBallotCommand(
    { ...command.identity, pollRevision: 1.5 }, command.entries, deadlineIso))
  assert.throws(() => ballotOperationKey({ ...command.identity, actorId: '' }))
  assert.throws(() => ballotOperationKey({ ...command.identity, operationId: '\ud800' }))
})

test('concurrent tuple collision re-reads transactionally and converges or conflicts', () => {
  const existing = { command, receipt: receipt() }
  assert.equal(resolveConcurrentIdempotencyCollision(existing, command).kind,
    'RETURN_EXISTING_RECEIPT')
  const changed = buildBallotCommand(command.identity, [
    { slotId: 'slot-1', choice: 'NO' },
    { slotId: 'slot-2', choice: 'MAYBE' },
  ], deadlineIso)
  assert.equal(resolveConcurrentIdempotencyCollision(existing, changed).kind,
    'IDEMPOTENCY_CONFLICT')
  assert.equal(resolveConcurrentIdempotencyCollision(null, command).kind,
    'REPOSITORY_INCONSISTENT')
})

test('server rejects every legacy votes mutation and accepts only atomic poll_ballot UPDATE', () => {
  const payload = receipt().syncPayload
  for (const operation of ['CREATE', 'UPDATE', 'DELETE'] as const) {
    assert.deepEqual(authorizeBallotServerMutation({ table: 'votes', operation, payload }), {
      kind: 'REJECT', code: 'LEGACY_VOTES_MUTATION_FORBIDDEN', retryable: false,
    })
  }
  assert.deepEqual(authorizeBallotServerMutation({
    table: 'poll_ballot', operation: 'UPDATE', payload,
  }), { kind: 'APPLY_ATOMIC_POLL_BALLOT', authority: 'poll_ballot' })
  assert.equal(authorizeBallotServerMutation({
    table: 'poll_ballot', operation: 'CREATE', payload,
  }).kind, 'REJECT')
})

test('inconsistent pending metadata remains visible as terminal unknown inconsistency', () => {
  const valid = {
    operationKey: ballotOperationKey(command.identity),
    ...command.identity,
    ballotFingerprint: command.ballotFingerprint,
    journalStatus: 'DISPATCHED' as const,
    terminalDestination: null,
  }
  const invalid = { ...valid, operationKey: 'corrupt-key' }
  const projection = projectPendingBallotMetadataList(input(), [valid, invalid])
  assert.equal(projection.length, 2)
  assert.equal(projection[0]?.kind, 'VALID')
  assert.deepEqual(projection[1], {
    kind: 'INCONSISTENT', code: 'REPOSITORY_INCONSISTENT', retryable: false,
    commitOutcome: 'UNKNOWN', metadata: invalid,
  })
})

test('pending metadata projection is total and every corrupt field fails visibly without throw', () => {
  const valid = {
    operationKey: ballotOperationKey(command.identity),
    ...command.identity,
    ballotFingerprint: command.ballotFingerprint,
    journalStatus: 'DISPATCHED' as const,
    terminalDestination: null,
  }
  const corruptRows = [
    null,
    'not-an-object',
    { ...valid, eventId: '' },
    { ...valid, actorId: ' participant-1' },
    { ...valid, operationId: '\ud800' },
    { ...valid, operationKey: '\ud800' },
    { ...valid, ballotFingerprint: ' ' },
    { ...valid, pollRevision: 1.5 },
    { ...valid, journalStatus: 'CORRUPT' },
  ]
  let projection: ReturnType<typeof projectPendingBallotMetadataList> = []
  assert.doesNotThrow(() => {
    projection = projectPendingBallotMetadataList(input(), corruptRows as never)
  })
  assert.equal(projection.length, corruptRows.length)
  assert.equal(projection.every((item) => item.kind === 'INCONSISTENT'), true)
})

test('journal snapshot validator accepts only its total discriminated union', () => {
  const terminalFailure = {
    kind: 'TERMINAL_FAILURE', code: 'IDEMPOTENCY_CONFLICT',
    commitOutcome: 'NOT_COMMITTED',
  } as const
  const validSnapshots: unknown[] = [
    { journalStatus: 'STAGED_NOT_DISPATCHED', terminalDestination: null },
    { journalStatus: 'DISPATCHED', terminalDestination: null },
    { journalStatus: 'CANCELLED', terminalDestination: null },
    {
      journalStatus: 'DISPATCH_CANCELLATION_TOMBSTONED',
      terminalDestination: { kind: 'CANCELLED' },
    },
    {
      journalStatus: 'DISPATCH_CANCELLATION_TOMBSTONED',
      terminalDestination: terminalFailure,
    },
    {
      journalStatus: 'DISPATCH_CANCELLATION_TOMBSTONED',
      terminalDestination: { kind: 'REVISED' },
    },
  ]
  const invalidSnapshots: unknown[] = [
    null,
    { journalStatus: 'STAGED_NOT_DISPATCHED', terminalDestination: { kind: 'REVISED' } },
    { journalStatus: 'DISPATCHED', terminalDestination: { kind: 'REVISED' } },
    { journalStatus: 'CANCELLED', terminalDestination: { kind: 'CANCELLED' } },
    { journalStatus: 'DISPATCH_CANCELLATION_TOMBSTONED', terminalDestination: null },
    {
      journalStatus: 'DISPATCH_CANCELLATION_TOMBSTONED',
      terminalDestination: {
        kind: 'TERMINAL_FAILURE', code: 'NOT_A_FAILURE', commitOutcome: 'UNKNOWN',
      },
    },
    { journalStatus: 'CORRUPT', terminalDestination: null },
  ]

  assert.doesNotThrow(() => {
    for (const snapshot of validSnapshots) assert.equal(isValidBallotJournalSnapshot(snapshot), true)
    for (const snapshot of invalidSnapshots) assert.equal(isValidBallotJournalSnapshot(snapshot), false)
  })
})

test('pending sync join keeps absent, empty, malformed, and divergent rows visible', () => {
  const localReceipt = receipt()
  const changedCommand = buildBallotCommand(command.identity, [
    { slotId: 'slot-1', choice: 'NO' },
    { slotId: 'slot-2', choice: 'MAYBE' },
  ], deadlineIso)
  const rows: PendingBallotSyncMetadataRow[] = [
    {
      metadataId: 'metadata-valid', localReceiptId: localReceipt.receiptId,
      payloadSnapshot: { kind: 'PARSED', payload: localReceipt.syncPayload },
    },
    {
      metadataId: 'metadata-missing', localReceiptId: 'missing-receipt',
      payloadSnapshot: { kind: 'EMPTY' },
    },
    {
      metadataId: 'metadata-empty', localReceiptId: localReceipt.receiptId,
      payloadSnapshot: { kind: 'EMPTY' },
    },
    {
      metadataId: 'metadata-malformed', localReceiptId: localReceipt.receiptId,
      payloadSnapshot: { kind: 'MALFORMED', diagnosticId: 'decode-1' },
    },
    {
      metadataId: 'metadata-receipt-id', localReceiptId: localReceipt.receiptId,
      payloadSnapshot: {
        kind: 'PARSED', payload: { ...localReceipt.syncPayload, localReceiptId: 'other-receipt' },
      },
    },
    {
      metadataId: 'metadata-tuple', localReceiptId: localReceipt.receiptId,
      payloadSnapshot: {
        kind: 'PARSED',
        payload: {
          ...localReceipt.syncPayload,
          command: buildBallotCommand({ ...command.identity, actorId: 'other-actor' },
            command.entries, deadlineIso),
        },
      },
    },
    {
      metadataId: 'metadata-fingerprint', localReceiptId: localReceipt.receiptId,
      payloadSnapshot: {
        kind: 'PARSED', payload: { ...localReceipt.syncPayload, command: changedCommand },
      },
    },
  ]

  const projections = projectPendingBallotSyncJoins(rows, [localReceipt])
  assert.equal(projections.length, rows.length)
  assert.deepEqual(projections.map((item) => item.kind === 'VALID' ? 'VALID' : item.code), [
    'VALID', 'RECEIPT_MISSING', 'PAYLOAD_EMPTY', 'PAYLOAD_MALFORMED',
    'RECEIPT_ID_DIVERGENT', 'TUPLE_DIVERGENT', 'FINGERPRINT_DIVERGENT',
  ])
})

test('sync join rejects acknowledged or non-pending receipts and never resubmits them', () => {
  const metadata: PendingBallotSyncMetadataRow = {
    metadataId: 'metadata-1', localReceiptId: 'receipt-1',
    payloadSnapshot: { kind: 'PARSED', payload: receipt().syncPayload },
  }
  const syncedProjection = projectPendingBallotSyncJoins([metadata], [{
    ...receipt(), syncStatus: 'SYNCED', serverReceiptId: 'server-receipt-1',
  }])
  assert.deepEqual(syncedProjection.map((item) =>
    item.kind === 'INCONSISTENT' ? item.code : item.kind), ['RECEIPT_NOT_LOCAL_PENDING'])
  assert.deepEqual(pendingSyncPayloadsForDispatch(syncedProjection), [])

  const impossiblePendingProjection = projectPendingBallotSyncJoins([metadata], [{
    ...receipt(), syncStatus: 'LOCAL_PENDING', serverReceiptId: 'server-receipt-1',
  }])
  assert.deepEqual(impossiblePendingProjection.map((item) =>
    item.kind === 'INCONSISTENT' ? item.code : item.kind), ['RECEIPT_ALREADY_ACKNOWLEDGED'])
  assert.deepEqual(pendingSyncPayloadsForDispatch(impossiblePendingProjection), [])
})

test('vote edit requests an injected clock snapshot and rejects deadline or permission', () => {
  const atDeadline = actorFor()
  requestVoteAndProvideClock(atDeadline, '2026-08-28T12:00:00.000Z')
  assert.equal(atDeadline.getSnapshot().context.entries[0]?.choice, 'YES')
  assert.equal(atDeadline.getSnapshot().context.failure?.code, 'DEADLINE_REACHED')

  const forbidden = actorFor(input({
    eligibility: {
      repositoryAvailable: true,
      eventExists: true,
      voteAccess: { kind: 'DENIED', reason: 'RSVP_NOT_ACCEPTED' },
      eventStatus: 'POLLING',
    },
  }))
  requestVoteAndProvideClock(forbidden, '2026-08-28T11:00:00.000Z')
  assert.equal(forbidden.getSnapshot().context.entries[0]?.choice, 'YES')
  assert.equal(forbidden.getSnapshot().context.failure?.code, 'FORBIDDEN')
})

test('vote clock correlation rejects stale snapshots and applies one matching edit', () => {
  const actor = actorFor()
  actor.send({
    type: 'REQUEST_SET_VOTE', clockRequestId: 'vote-clock-1',
    slotId: 'slot-1', choice: 'NO',
  })
  assert.equal(actor.getSnapshot().matches('checkingVoteClock'), true)
  actor.send({
    type: 'VOTE_CLOCK_SNAPSHOT', clockRequestId: 'stale',
    nowIso: '2026-08-28T11:00:00.000Z',
  })
  assert.equal(actor.getSnapshot().matches('checkingVoteClock'), true)
  actor.send({
    type: 'VOTE_CLOCK_SNAPSHOT', clockRequestId: 'vote-clock-1',
    nowIso: '2026-08-28T11:00:00.000Z',
  })
  assert.equal(actor.getSnapshot().matches('editing'), true)
  assert.equal(actor.getSnapshot().context.entries[0]?.choice, 'NO')
  assert.equal(actor.getSnapshot().context.effects.filter(
    (effect) => effect === 'requestClockSnapshot').length, 1)
})

test('complete ballot dispatches one atomic command and local commit enables navigation', () => {
  const actor = actorFor()
  requestAndProvideClock(actor)
  assert.equal(actor.getSnapshot().matches('submitting'), true)
  assert.equal(actor.getSnapshot().context.effects.filter(
    (effect) => effect === 'dispatchAtomicBallotCommand').length, 1)

  actor.send({ type: 'LOCAL_COMMIT', receipt: receipt() })
  assert.equal(actor.getSnapshot().matches({ committed: 'pendingSync' }), true)
  assert.equal(actor.getSnapshot().context.effects.filter(
    (effect) => effect === 'navigateAfterLocalCommit').length, 1)
})

test('atomic command dispatch waits for a correlated durable journal acknowledgement', () => {
  const actor = actorFor()
  actor.send({ type: 'REQUEST_SUBMIT', operationId: 'operation-1' })
  actor.send({
    type: 'SUBMISSION_CLOCK_SNAPSHOT', operationId: 'operation-1',
    nowIso: '2026-08-28T11:59:59.999Z',
  })
  const pending = actor.getSnapshot().context.pendingCommand!
  assert.equal(actor.getSnapshot().matches('persistingCommand'), true)
  assert.equal(actor.getSnapshot().context.effects.includes('dispatchAtomicBallotCommand'), false)
  actor.send({
    type: 'COMMAND_JOURNALED',
    operationKey: 'foreign',
    ballotFingerprint: pending.ballotFingerprint,
  })
  assert.equal(actor.getSnapshot().matches('persistingCommand'), true)
  actor.send({
    type: 'COMMAND_JOURNALED',
    operationKey: ballotOperationKey(pending.identity),
    ballotFingerprint: pending.ballotFingerprint,
  })
  assert.equal(actor.getSnapshot().matches('authorizingDispatch'), true)
  assert.equal(actor.getSnapshot().context.journalStatus, 'STAGED_NOT_DISPATCHED')
  assert.equal(actor.getSnapshot().context.effects.includes('dispatchAtomicBallotCommand'), false)
  actor.send({
    type: 'COMMAND_DISPATCH_MARKED',
    operationKey: ballotOperationKey(pending.identity),
    ballotFingerprint: pending.ballotFingerprint,
  })
  assert.equal(actor.getSnapshot().matches('submitting'), true)
  assert.equal(actor.getSnapshot().context.journalStatus, 'DISPATCHED')
  assert.equal(actor.getSnapshot().context.effects.filter(
    (effect) => effect === 'dispatchAtomicBallotCommand').length, 1)
})

test('dispatch authorization failure and unknown result are typed and correlation fenced', () => {
  const failed = actorFor()
  failed.send({ type: 'REQUEST_SUBMIT', operationId: 'operation-1' })
  failed.send({
    type: 'SUBMISSION_CLOCK_SNAPSHOT', operationId: 'operation-1',
    nowIso: '2026-08-28T11:59:59.999Z',
  })
  const failedCommand = failed.getSnapshot().context.pendingCommand!
  failed.send({
    type: 'COMMAND_JOURNALED', operationKey: ballotOperationKey(failedCommand.identity),
    ballotFingerprint: failedCommand.ballotFingerprint,
  })
  failed.send({
    type: 'COMMAND_DISPATCH_MARK_FAILED', operationKey: 'foreign',
    ballotFingerprint: failedCommand.ballotFingerprint,
    error: {
      code: 'COMMAND_DISPATCH_MARK_FAILED', retryable: true, commitOutcome: 'NOT_COMMITTED',
    },
  })
  assert.equal(failed.getSnapshot().matches('authorizingDispatch'), true)
  failed.send({
    type: 'COMMAND_DISPATCH_MARK_FAILED', operationKey: ballotOperationKey(failedCommand.identity),
    ballotFingerprint: failedCommand.ballotFingerprint,
    error: {
      code: 'COMMAND_DISPATCH_MARK_FAILED', retryable: true, commitOutcome: 'NOT_COMMITTED',
    },
  })
  assert.equal(failed.getSnapshot().matches('failedBeforeCommit'), true)
  assert.equal(failed.getSnapshot().context.journalStatus, 'STAGED_NOT_DISPATCHED')
  assert.equal(failed.getSnapshot().context.effects.includes('dispatchAtomicBallotCommand'), false)

  const unknown = actorFor()
  unknown.send({ type: 'REQUEST_SUBMIT', operationId: 'operation-1' })
  unknown.send({
    type: 'SUBMISSION_CLOCK_SNAPSHOT', operationId: 'operation-1',
    nowIso: '2026-08-28T11:59:59.999Z',
  })
  const unknownCommand = unknown.getSnapshot().context.pendingCommand!
  unknown.send({
    type: 'COMMAND_JOURNALED', operationKey: ballotOperationKey(unknownCommand.identity),
    ballotFingerprint: unknownCommand.ballotFingerprint,
  })
  unknown.send({
    type: 'COMMAND_DISPATCH_MARK_UNKNOWN', operationKey: ballotOperationKey(unknownCommand.identity),
    ballotFingerprint: unknownCommand.ballotFingerprint,
    error: {
      code: 'COMMAND_DISPATCH_MARK_FAILED', retryable: true, commitOutcome: 'UNKNOWN',
    },
  })
  assert.equal(unknown.getSnapshot().matches('resolvingJournalStatus'), true)
  assert.equal(unknown.getSnapshot().context.effects.includes('resolvePendingCommandJournal'), true)
  unknown.send({
    type: 'COMMAND_JOURNAL_STATUS_RESOLVED', operationKey: 'foreign',
    ballotFingerprint: unknownCommand.ballotFingerprint, status: 'DISPATCHED',
    terminalDestination: null,
  })
  assert.equal(unknown.getSnapshot().matches('resolvingJournalStatus'), true)
  unknown.send({
    type: 'COMMAND_JOURNAL_STATUS_RESOLVED',
    operationKey: ballotOperationKey(unknownCommand.identity),
    ballotFingerprint: unknownCommand.ballotFingerprint, status: 'CANCELLED',
    terminalDestination: null,
  })
  assert.equal(unknown.getSnapshot().matches('resolvingJournalStatus'), true)
  unknown.send({
    type: 'COMMAND_JOURNAL_STATUS_RESOLVED', operationKey: ballotOperationKey(unknownCommand.identity),
    ballotFingerprint: unknownCommand.ballotFingerprint, status: 'DISPATCHED',
    terminalDestination: null,
  })
  assert.equal(unknown.getSnapshot().matches('submitting'), true)
  assert.equal(unknown.getSnapshot().context.journalStatus, 'DISPATCHED')
  assert.equal(unknown.getSnapshot().context.effects.filter(
    (effect) => effect === 'dispatchAtomicBallotCommand').length, 1)
})

test('journal status read failure, missing row, and malformed row retry within a bound then fail terminally', () => {
  const read = enterDispatchJournalResolution(2)
  assert.equal(read.actor.getSnapshot().context.journalResolutionAttempt, 1)
  read.actor.send({
    type: 'COMMAND_JOURNAL_STATUS_READ_FAILED', operationKey: 'foreign',
    ballotFingerprint: read.pending.ballotFingerprint,
    error: { code: 'COMMAND_JOURNAL_READ_FAILED', retryable: true, commitOutcome: 'UNKNOWN' },
  })
  assert.equal(read.actor.getSnapshot().matches('resolvingJournalStatus'), true)
  read.actor.send({
    type: 'COMMAND_JOURNAL_STATUS_READ_FAILED',
    operationKey: ballotOperationKey(read.pending.identity),
    ballotFingerprint: read.pending.ballotFingerprint,
    error: { code: 'COMMAND_JOURNAL_READ_FAILED', retryable: true, commitOutcome: 'UNKNOWN' },
  })
  assert.equal(read.actor.getSnapshot().matches('journalResolutionFailed'), true)
  read.actor.send({ type: 'RETRY_JOURNAL_RESOLUTION' })
  assert.equal(read.actor.getSnapshot().matches('resolvingJournalStatus'), true)
  assert.equal(read.actor.getSnapshot().context.journalResolutionAttempt, 2)
  read.actor.send({
    type: 'COMMAND_JOURNAL_STATUS_READ_FAILED',
    operationKey: ballotOperationKey(read.pending.identity),
    ballotFingerprint: read.pending.ballotFingerprint,
    error: { code: 'COMMAND_JOURNAL_READ_FAILED', retryable: true, commitOutcome: 'UNKNOWN' },
  })
  assert.equal(read.actor.getSnapshot().matches('resolutionFailed'), true)
  assert.deepEqual(read.actor.getSnapshot().context.failure, {
    code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
  })

  const nonRetryableRead = enterDispatchJournalResolution(2)
  nonRetryableRead.actor.send({
    type: 'COMMAND_JOURNAL_STATUS_READ_FAILED',
    operationKey: ballotOperationKey(nonRetryableRead.pending.identity),
    ballotFingerprint: nonRetryableRead.pending.ballotFingerprint,
    error: { code: 'COMMAND_JOURNAL_READ_FAILED', retryable: false, commitOutcome: 'UNKNOWN' },
  })
  assert.equal(nonRetryableRead.actor.getSnapshot().matches('terminalFailure'), true)
  assert.deepEqual(nonRetryableRead.actor.getSnapshot().context.failure, {
    code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
  })
  assert.equal(nonRetryableRead.actor.getSnapshot().status, 'done')

  for (const type of [
    'COMMAND_JOURNAL_STATUS_MISSING', 'COMMAND_JOURNAL_STATUS_MALFORMED',
  ] as const) {
    const current = enterDispatchJournalResolution(1)
    assert.doesNotThrow(() => current.actor.send({
      type, operationKey: ballotOperationKey(current.pending.identity),
      ballotFingerprint: current.pending.ballotFingerprint,
    }))
    assert.equal(current.actor.getSnapshot().matches('resolutionFailed'), true)
    assert.equal(current.actor.getSnapshot().context.failure?.code, 'REPOSITORY_INCONSISTENT')
    assert.equal(current.actor.getSnapshot().status, 'done')
  }
})

test('unknown durable cancellation result resolves to CANCELLED and never dispatches', () => {
  const actor = actorFor()
  actor.send({ type: 'REQUEST_SUBMIT', operationId: 'operation-1' })
  actor.send({
    type: 'SUBMISSION_CLOCK_SNAPSHOT', operationId: 'operation-1',
    nowIso: '2026-08-28T11:59:59.999Z',
  })
  const pending = actor.getSnapshot().context.pendingCommand!
  actor.send({ type: 'CANCEL' })
  actor.send({
    type: 'COMMAND_CANCEL_UNKNOWN', operationKey: ballotOperationKey(pending.identity),
    ballotFingerprint: pending.ballotFingerprint,
    error: { code: 'COMMAND_CANCEL_FAILED', retryable: true, commitOutcome: 'UNKNOWN' },
  })
  assert.equal(actor.getSnapshot().matches('resolvingJournalStatus'), true)
  actor.send({
    type: 'COMMAND_JOURNAL_STATUS_RESOLVED', operationKey: ballotOperationKey(pending.identity),
    ballotFingerprint: pending.ballotFingerprint, status: 'CANCELLED',
    terminalDestination: null,
  })
  assert.equal(actor.getSnapshot().matches('cancelled'), true)
  assert.equal(actor.getSnapshot().context.journalStatus, 'CANCELLED')
  assert.equal(actor.getSnapshot().context.effects.includes('dispatchAtomicBallotCommand'), false)
})

test('correlated journal failure cannot dispatch the atomic command', () => {
  const actor = actorFor()
  actor.send({ type: 'REQUEST_SUBMIT', operationId: 'operation-1' })
  actor.send({
    type: 'SUBMISSION_CLOCK_SNAPSHOT', operationId: 'operation-1',
    nowIso: '2026-08-28T11:59:59.999Z',
  })
  const pending = actor.getSnapshot().context.pendingCommand!
  actor.send({
    type: 'COMMAND_JOURNAL_FAILED',
    operationKey: ballotOperationKey(pending.identity),
    ballotFingerprint: 'foreign-payload',
    error: { code: 'COMMAND_JOURNAL_FAILED', retryable: true, commitOutcome: 'NOT_COMMITTED' },
  })
  assert.equal(actor.getSnapshot().matches('persistingCommand'), true)
  actor.send({
    type: 'COMMAND_JOURNAL_FAILED',
    operationKey: ballotOperationKey(pending.identity),
    ballotFingerprint: pending.ballotFingerprint,
    error: { code: 'COMMAND_JOURNAL_FAILED', retryable: true, commitOutcome: 'NOT_COMMITTED' },
  })
  assert.equal(actor.getSnapshot().matches('failedBeforeCommit'), true)
  assert.equal(actor.getSnapshot().context.effects.includes('dispatchAtomicBallotCommand'), false)

  const terminal = actorFor()
  terminal.send({ type: 'REQUEST_SUBMIT', operationId: 'operation-1' })
  terminal.send({
    type: 'SUBMISSION_CLOCK_SNAPSHOT', operationId: 'operation-1',
    nowIso: '2026-08-28T11:59:59.999Z',
  })
  const terminalPending = terminal.getSnapshot().context.pendingCommand!
  terminal.send({
    type: 'COMMAND_JOURNAL_FAILED',
    operationKey: ballotOperationKey(terminalPending.identity),
    ballotFingerprint: terminalPending.ballotFingerprint,
    error: { code: 'COMMAND_JOURNAL_FAILED', retryable: false, commitOutcome: 'NOT_COMMITTED' },
  })
  assert.equal(terminal.getSnapshot().matches('terminalFailure'), true)
  assert.equal(terminal.getSnapshot().status, 'done')
  assert.equal(terminal.getSnapshot().context.effects.includes('dispatchAtomicBallotCommand'), false)
})

test('ballot validation rejects missing, duplicate, unknown, and invalid choices', () => {
  assert.equal(validateBallotEntries(['a', 'b'], [{ slotId: 'a', choice: 'YES' }]), 'INCOMPLETE_BALLOT')
  assert.equal(validateBallotEntries(['a'], [
    { slotId: 'a', choice: 'YES' },
    { slotId: 'a', choice: 'NO' },
  ]), 'DUPLICATE_SLOT')
  assert.equal(validateBallotEntries(['a'], [{ slotId: 'b', choice: 'YES' }]), 'UNKNOWN_SLOT')
  assert.equal(validateBallotEntries(['a'], [{ slotId: 'a', choice: 'INVALID' as never }]), 'INVALID_CHOICE')
})

test('an incomplete ballot never dispatches persistence', () => {
  const actor = actorFor(input({ initialEntries: [initialEntries[0]] }))
  requestAndProvideClock(actor)
  assert.equal(actor.getSnapshot().matches('failedBeforeCommit'), true)
  assert.equal(actor.getSnapshot().context.failure?.code, 'INCOMPLETE_BALLOT')
  assert.equal(actor.getSnapshot().context.effects.includes('dispatchAtomicBallotCommand'), false)
})

test('invalid ISO and reached deadline fail closed before persistence', () => {
  for (const [nowIso, code] of [
    ['invalid', 'INVALID_NOW_ISO'],
    ['2026-08-28T12:00:00.000Z', 'DEADLINE_REACHED'],
    ['2026-08-28T12:00:00.001Z', 'DEADLINE_REACHED'],
  ] as const) {
    const actor = actorFor()
    requestAndProvideClock(actor, nowIso)
    assert.equal(actor.getSnapshot().matches('failedBeforeCommit'), true)
    assert.equal(actor.getSnapshot().context.failure?.code, code)
    assert.equal(actor.getSnapshot().context.effects.includes('dispatchAtomicBallotCommand'), false)
  }
})

test('permission and status are checked again before every known-noncommit retry', () => {
  const actor = actorFor()
  requestAndProvideClock(actor)
  actor.send({
    type: 'SUBMISSION_FAILED',
    operationId: 'operation-1',
    failure: { code: 'LOCAL_TRANSACTION_FAILED', retryable: true, commitOutcome: 'NOT_COMMITTED' },
  })
  actor.send({
    type: 'ELIGIBILITY_UPDATED',
    eligibility: {
      repositoryAvailable: true,
      eventExists: true,
      voteAccess: { kind: 'DENIED', reason: 'RSVP_NOT_ACCEPTED' },
      eventStatus: 'POLLING',
    },
  })
  actor.send({ type: 'RETRY_SUBMISSION' })
  actor.send({
    type: 'SUBMISSION_CLOCK_SNAPSHOT', operationId: 'operation-1',
    nowIso: '2026-08-28T11:59:59.999Z',
  })
  assert.equal(actor.getSnapshot().matches('failedBeforeCommit'), true)
  assert.equal(actor.getSnapshot().context.failure?.code, 'FORBIDDEN')
  assert.equal(actor.getSnapshot().context.effects.filter(
    (effect) => effect === 'dispatchAtomicBallotCommand').length, 1)
})

test('known non-commit retry keeps DISPATCHED journal and redispatches without restaging', () => {
  const actor = actorFor()
  requestAndProvideClock(actor)
  const durableCommand = actor.getSnapshot().context.pendingCommand
  actor.send({
    type: 'SUBMISSION_FAILED', operationId: 'operation-1',
    failure: { code: 'LOCAL_TRANSACTION_FAILED', retryable: true, commitOutcome: 'NOT_COMMITTED' },
  })
  actor.send({ type: 'RETRY_SUBMISSION' })
  assert.equal(actor.getSnapshot().matches('checkingDispatchedRetryClock'), true)
  actor.send({
    type: 'SUBMISSION_CLOCK_SNAPSHOT', operationId: 'operation-1',
    nowIso: '2026-08-28T11:59:59.999Z',
  })
  assert.equal(actor.getSnapshot().matches('submitting'), true)
  assert.equal(actor.getSnapshot().context.journalStatus, 'DISPATCHED')
  assert.deepEqual(actor.getSnapshot().context.pendingCommand, durableCommand)
  assert.equal(actor.getSnapshot().context.effects.filter(
    (effect) => effect === 'persistPendingCommand').length, 1)
  assert.equal(actor.getSnapshot().context.effects.filter(
    (effect) => effect === 'markPendingCommandDispatched').length, 1)
  assert.equal(actor.getSnapshot().context.effects.filter(
    (effect) => effect === 'dispatchAtomicBallotCommand').length, 2)
})

test('cancelling failedBeforeCommit after DISPATCHED waits for its durable tombstone', () => {
  const actor = actorFor()
  requestAndProvideClock(actor)
  const pending = actor.getSnapshot().context.pendingCommand!
  actor.send({
    type: 'SUBMISSION_FAILED', operationId: 'operation-1',
    failure: { code: 'LOCAL_TRANSACTION_FAILED', retryable: true, commitOutcome: 'NOT_COMMITTED' },
  })
  actor.send({ type: 'CANCEL' })
  assert.equal(actor.getSnapshot().matches('tombstoningDispatchedCancellation'), true)
  assert.equal(actor.getSnapshot().status, 'active')
  actor.send({
    type: 'DISPATCH_CANCELLATION_TOMBSTONED',
    operationKey: ballotOperationKey(pending.identity),
    ballotFingerprint: pending.ballotFingerprint,
    terminalDestination: { kind: 'CANCELLED' },
  })
  assert.equal(actor.getSnapshot().matches('cancelled'), true)
  assert.equal(actor.getSnapshot().context.journalStatus, 'DISPATCH_CANCELLATION_TOMBSTONED')
})

test('revising failedBeforeCommit after DISPATCHED tombstones REVISED before reset', () => {
  const actor = actorFor()
  requestAndProvideClock(actor)
  const pending = actor.getSnapshot().context.pendingCommand!
  actor.send({
    type: 'SUBMISSION_FAILED', operationId: 'operation-1',
    failure: { code: 'LOCAL_TRANSACTION_FAILED', retryable: true, commitOutcome: 'NOT_COMMITTED' },
  })
  actor.send({ type: 'REVISE_BALLOT' })
  assert.equal(actor.getSnapshot().matches('tombstoningDispatchedCancellation'), true)
  assert.equal(actor.getSnapshot().context.journalStatus, 'DISPATCHED')
  assert.deepEqual(actor.getSnapshot().context.pendingCommand, pending)
  assert.deepEqual(actor.getSnapshot().context.tombstoneTerminalDestination, { kind: 'REVISED' })
  actor.send({
    type: 'DISPATCH_CANCELLATION_TOMBSTONED',
    operationKey: ballotOperationKey(pending.identity),
    ballotFingerprint: pending.ballotFingerprint,
    terminalDestination: { kind: 'CANCELLED' },
  })
  assert.equal(actor.getSnapshot().matches('tombstoningDispatchedCancellation'), true)
  assert.deepEqual(actor.getSnapshot().context.pendingCommand, pending)
  actor.send({
    type: 'DISPATCH_CANCELLATION_TOMBSTONED',
    operationKey: ballotOperationKey(pending.identity),
    ballotFingerprint: pending.ballotFingerprint,
    terminalDestination: { kind: 'REVISED' },
  })
  assert.equal(actor.getSnapshot().matches('editing'), true)
  assert.equal(actor.getSnapshot().context.pendingCommand, null)
  assert.equal(actor.getSnapshot().context.journalStatus, null)
  assert.equal(actor.getSnapshot().context.effects.filter(
    (effect) => effect === 'dispatchAtomicBallotCommand').length, 1)

  const recreated = actorFor()
  recreated.send({
    type: 'REHYDRATE_UNKNOWN_OUTCOME', command: pending,
    journalStatus: 'DISPATCH_CANCELLATION_TOMBSTONED',
    terminalDestination: { kind: 'REVISED' },
    failure: { code: 'LOCAL_TRANSACTION_FAILED', retryable: true, commitOutcome: 'UNKNOWN' },
  })
  assert.equal(recreated.getSnapshot().matches('editing'), true)
  assert.equal(recreated.getSnapshot().context.pendingCommand, null)
  assert.equal(recreated.getSnapshot().context.effects.includes('dispatchAtomicBallotCommand'), false)
})

test('unknown-outcome retry replays the same operation and captured ballot', () => {
  const actor = actorFor()
  requestAndProvideClock(actor)
  const captured = actor.getSnapshot().context.submittedEntries
  actor.send({
    type: 'SUBMISSION_FAILED',
    operationId: 'operation-1',
    failure: { code: 'LOCAL_TRANSACTION_FAILED', retryable: true, commitOutcome: 'UNKNOWN' },
  })
  actor.send({ type: 'RETRY_RESOLUTION' })
  assert.equal(actor.getSnapshot().matches('submitting'), true)
  assert.equal(actor.getSnapshot().context.operationId, 'operation-1')
  assert.deepEqual(actor.getSnapshot().context.submittedEntries, captured)
  assert.equal(actor.getSnapshot().context.effects.filter(
    (effect) => effect === 'dispatchAtomicBallotCommand').length, 2)
})

test('DISPATCHED unknown outcome forbids a new submit and retries only the original command', () => {
  const actor = actorFor()
  actor.send({
    type: 'REHYDRATE_UNKNOWN_OUTCOME', command,
    journalStatus: 'DISPATCHED', terminalDestination: null,
    failure: { code: 'LOCAL_TRANSACTION_FAILED', retryable: true, commitOutcome: 'UNKNOWN' },
  })
  const original = actor.getSnapshot().context.pendingCommand
  actor.send({ type: 'REQUEST_SUBMIT', operationId: 'operation-2' })
  assert.equal(actor.getSnapshot().matches('outcomeUnknown'), true)
  assert.equal(actor.getSnapshot().context.operationId, 'operation-1')
  assert.deepEqual(actor.getSnapshot().context.pendingCommand, original)
  assert.equal(actor.getSnapshot().context.effects.includes('requestClockSnapshot'), false)

  actor.send({ type: 'RETRY_RESOLUTION' })
  assert.equal(actor.getSnapshot().matches('submitting'), true)
  assert.equal(actor.getSnapshot().context.operationId, 'operation-1')
  assert.deepEqual(actor.getSnapshot().context.pendingCommand, original)
  actor.send({ type: 'REQUEST_SUBMIT', operationId: 'operation-2' })
  assert.equal(actor.getSnapshot().context.operationId, 'operation-1')
  assert.deepEqual(actor.getSnapshot().context.pendingCommand, original)
  assert.equal(actor.getSnapshot().context.effects.filter(
    (effect) => effect === 'dispatchAtomicBallotCommand').length, 1)
})

test('unknown outcome rehydrates the exact durable command after view recreation', () => {
  const firstView = actorFor()
  requestAndProvideClock(firstView)
  const durableCommand = firstView.getSnapshot().context.pendingCommand
  assert.notEqual(durableCommand, null)
  assert.deepEqual(firstView.getSnapshot().context.effects.slice(-3), [
    'persistPendingCommand', 'markPendingCommandDispatched', 'dispatchAtomicBallotCommand',
  ])
  firstView.send({
    type: 'SUBMISSION_FAILED', operationId: 'operation-1',
    failure: { code: 'LOCAL_TRANSACTION_FAILED', retryable: true, commitOutcome: 'UNKNOWN' },
  })

  const recreatedView = actorFor(input({
    pollRevision: 99, validSlotIds: ['new-slot'],
    votingDeadlineIso: '2020-01-01T00:00:00.000Z',
  }))
  recreatedView.send({
    type: 'REHYDRATE_UNKNOWN_OUTCOME',
    command: durableCommand!,
    journalStatus: 'DISPATCHED',
    terminalDestination: null,
    failure: { code: 'LOCAL_TRANSACTION_FAILED', retryable: true, commitOutcome: 'UNKNOWN' },
  })
  assert.equal(recreatedView.getSnapshot().matches('outcomeUnknown'), true)
  assert.deepEqual(recreatedView.getSnapshot().context.pendingCommand, durableCommand)
  recreatedView.send({ type: 'RETRY_RESOLUTION' })
  assert.equal(recreatedView.getSnapshot().matches('submitting'), true)
  assert.equal(recreatedView.getSnapshot().context.operationId, 'operation-1')
  assert.deepEqual(recreatedView.getSnapshot().context.pendingCommand, durableCommand)
  recreatedView.send({ type: 'LOCAL_COMMIT', receipt: receipt() })
  assert.equal(recreatedView.getSnapshot().matches({ committed: 'pendingSync' }), true)
  assert.equal(recreatedView.getSnapshot().context.receipt?.pollRevision, 7)
})

test('non-retryable unknown outcome rehydrates to terminal failure without redispatch', () => {
  const recreatedView = actorFor(input({ pollRevision: 99, validSlotIds: ['new-slot'] }))
  recreatedView.send({
    type: 'REHYDRATE_UNKNOWN_OUTCOME',
    command,
    journalStatus: 'DISPATCHED',
    terminalDestination: null,
    failure: { code: 'LOCAL_TRANSACTION_FAILED', retryable: false, commitOutcome: 'UNKNOWN' },
  })
  assert.equal(recreatedView.getSnapshot().matches('resolutionFailed'), true)
  assert.equal(recreatedView.getSnapshot().status, 'done')
  recreatedView.send({ type: 'RETRY_RESOLUTION' })
  assert.equal(recreatedView.getSnapshot().context.effects.includes('dispatchAtomicBallotCommand'), false)
})

test('rehydration rejects foreign event or actor while ignoring current revision and slots', () => {
  for (const foreignIdentity of [
    { ...command.identity, eventId: 'event-foreign' },
    { ...command.identity, actorId: 'actor-foreign' },
  ]) {
    const actor = actorFor(input({ pollRevision: 99, validSlotIds: ['new-slot'] }))
    actor.send({
      type: 'REHYDRATE_UNKNOWN_OUTCOME',
      command: buildBallotCommand(foreignIdentity, command.entries, deadlineIso),
      journalStatus: 'DISPATCHED',
      terminalDestination: null,
      failure: { code: 'LOCAL_TRANSACTION_FAILED', retryable: true, commitOutcome: 'UNKNOWN' },
    })
    assert.equal(actor.getSnapshot().matches('resolutionFailed'), true)
    assert.equal(actor.getSnapshot().context.failure?.code, 'REPOSITORY_INCONSISTENT')
    assert.equal(actor.getSnapshot().context.effects.includes('dispatchAtomicBallotCommand'), false)
  }
})

test('malformed DISPATCHED journal rehydrates as terminal non-retryable inconsistency', () => {
  const actor = actorFor()
  actor.send({
    type: 'REHYDRATE_UNKNOWN_OUTCOME',
    command: { ...command, ballotFingerprint: 'corrupt' },
    journalStatus: 'DISPATCHED',
    terminalDestination: null,
    failure: { code: 'LOCAL_TRANSACTION_FAILED', retryable: true, commitOutcome: 'UNKNOWN' },
  })
  assert.equal(actor.getSnapshot().matches('resolutionFailed'), true)
  assert.deepEqual(actor.getSnapshot().context.failure, {
    code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
  })
  assert.equal(actor.getSnapshot().context.effects.includes('dispatchAtomicBallotCommand'), false)
})

test('a present malformed local receipt is terminal repository inconsistency and never throws', () => {
  const actor = actorFor()
  requestAndProvideClock(actor)
  assert.doesNotThrow(() => actor.send({
    type: 'LOCAL_COMMIT',
    receipt: { ...receipt(), syncPayload: null },
  } as never))
  assert.equal(actor.getSnapshot().matches('resolutionFailed'), true)
  assert.equal(actor.getSnapshot().status, 'done')
  assert.deepEqual(actor.getSnapshot().context.failure, {
    code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
  })
  assert.equal(actor.getSnapshot().context.effects.includes('navigateAfterLocalCommit'), false)
})

test('rehydration terminalizes every divergent journal status and destination pair', () => {
  const divergentSnapshots = [
    { journalStatus: 'DISPATCHED', terminalDestination: { kind: 'REVISED' } },
    {
      journalStatus: 'STAGED_NOT_DISPATCHED',
      terminalDestination: { kind: 'CANCELLED' },
    },
    {
      journalStatus: 'CANCELLED',
      terminalDestination: {
        kind: 'TERMINAL_FAILURE', code: 'IDEMPOTENCY_CONFLICT',
        commitOutcome: 'NOT_COMMITTED',
      },
    },
    { journalStatus: 'DISPATCH_CANCELLATION_TOMBSTONED', terminalDestination: null },
  ] as const

  for (const snapshot of divergentSnapshots) {
    const actor = actorFor()
    actor.send({
      type: 'REHYDRATE_UNKNOWN_OUTCOME',
      command,
      journalStatus: snapshot.journalStatus,
      terminalDestination: snapshot.terminalDestination,
      failure: { code: 'LOCAL_TRANSACTION_FAILED', retryable: true, commitOutcome: 'UNKNOWN' },
    })
    assert.equal(actor.getSnapshot().matches('editing'), false)
    assert.equal(actor.getSnapshot().matches('resolutionFailed'), true)
    assert.equal(actor.getSnapshot().status, 'done')
    assert.deepEqual(actor.getSnapshot().context.failure, {
      code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
    })
    assert.equal(actor.getSnapshot().context.effects.includes('dispatchAtomicBallotCommand'), false)
  }
})

test('rehydrated tombstone destination projects exact state and navigation', () => {
  assert.deepEqual(projectBallotTombstoneDestination({ kind: 'CANCELLED' }), {
    state: 'CANCELLED', navigation: 'RETURN_TO_EVENT', failure: null,
  })
  assert.deepEqual(projectBallotTombstoneDestination({ kind: 'REVISED' }), {
    state: 'EDITING', navigation: 'NONE', failure: null,
  })
  assert.deepEqual(projectBallotTombstoneDestination({
    kind: 'TERMINAL_FAILURE', code: 'IDEMPOTENCY_CONFLICT', commitOutcome: 'NOT_COMMITTED',
  }), {
    state: 'TERMINAL_FAILURE', navigation: 'NONE',
    failure: {
      code: 'IDEMPOTENCY_CONFLICT', retryable: false, commitOutcome: 'NOT_COMMITTED',
    },
  })

  const cases = [
    { destination: { kind: 'CANCELLED' } as const, state: 'cancelled', navigates: true },
    { destination: { kind: 'REVISED' } as const, state: 'editing', navigates: false },
    {
      destination: {
        kind: 'TERMINAL_FAILURE', code: 'IDEMPOTENCY_CONFLICT',
        commitOutcome: 'NOT_COMMITTED',
      } as const,
      state: 'terminalFailure', navigates: false,
    },
  ] as const
  for (const item of cases) {
    const actor = actorFor()
    const rehydrate = {
      type: 'REHYDRATE_UNKNOWN_OUTCOME', command,
      journalStatus: 'DISPATCH_CANCELLATION_TOMBSTONED',
      terminalDestination: item.destination,
      failure: { code: 'LOCAL_TRANSACTION_FAILED', retryable: true, commitOutcome: 'UNKNOWN' },
    } as const
    actor.send(rehydrate)
    assert.equal(actor.getSnapshot().matches(item.state), true)
    assert.equal(actor.getSnapshot().context.effects.filter(
      (effect) => effect === 'returnToEventOnBack').length, item.navigates ? 1 : 0)
    assert.equal(actor.getSnapshot().context.effects.includes('navigateAfterLocalCommit'), false)
    actor.send(rehydrate)
    assert.equal(actor.getSnapshot().context.effects.filter(
      (effect) => effect === 'returnToEventOnBack').length, item.navigates ? 1 : 0)
  }
})

test('cancel during journal persistence fences a late ACK and remains cancelled after recreation', () => {
  const actor = actorFor()
  actor.send({ type: 'REQUEST_SUBMIT', operationId: 'operation-1' })
  actor.send({
    type: 'SUBMISSION_CLOCK_SNAPSHOT', operationId: 'operation-1',
    nowIso: '2026-08-28T11:59:59.999Z',
  })
  const pending = actor.getSnapshot().context.pendingCommand!
  actor.send({ type: 'CANCEL' })
  assert.equal(actor.getSnapshot().matches('cancellingJournal'), true)
  assert.equal(actor.getSnapshot().context.effects.includes('cancelPendingCommandJournal'), true)
  actor.send({
    type: 'COMMAND_JOURNALED', operationKey: ballotOperationKey(pending.identity),
    ballotFingerprint: pending.ballotFingerprint,
  })
  assert.equal(actor.getSnapshot().matches('cancellingJournal'), true)
  assert.equal(actor.getSnapshot().context.effects.includes('dispatchAtomicBallotCommand'), false)
  actor.send({
    type: 'COMMAND_CANCELLED', operationKey: ballotOperationKey(pending.identity),
    ballotFingerprint: pending.ballotFingerprint,
  })
  assert.equal(actor.getSnapshot().matches('cancelled'), true)
  assert.equal(actor.getSnapshot().context.journalStatus, 'CANCELLED')

  const recreated = actorFor()
  recreated.send({
    type: 'REHYDRATE_UNKNOWN_OUTCOME', command: pending, journalStatus: 'CANCELLED',
    terminalDestination: null,
    failure: { code: 'LOCAL_TRANSACTION_FAILED', retryable: false, commitOutcome: 'UNKNOWN' },
  })
  assert.equal(recreated.getSnapshot().matches('cancelled'), true)
  assert.equal(recreated.getSnapshot().context.effects.includes('dispatchAtomicBallotCommand'), false)

  const staged = actorFor()
  staged.send({
    type: 'REHYDRATE_UNKNOWN_OUTCOME', command: pending,
    journalStatus: 'STAGED_NOT_DISPATCHED',
    terminalDestination: null,
    failure: { code: 'LOCAL_TRANSACTION_FAILED', retryable: true, commitOutcome: 'UNKNOWN' },
  })
  assert.equal(staged.getSnapshot().matches('cancellingJournal'), true)
  assert.equal(staged.getSnapshot().context.effects.includes('cancelPendingCommandJournal'), true)
  assert.equal(staged.getSnapshot().context.effects.includes('dispatchAtomicBallotCommand'), false)
})

test('repository inconsistency is a typed terminal unknown outcome', () => {
  const actor = actorFor()
  requestAndProvideClock(actor)
  actor.send({
    type: 'SUBMISSION_FAILED', operationId: 'operation-1',
    failure: { code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN' },
  })
  assert.equal(actor.getSnapshot().matches('resolutionFailed'), true)
  assert.equal(actor.getSnapshot().context.failure?.code, 'REPOSITORY_INCONSISTENT')
})

test('non-retryable repository rejection is terminal and cannot redispatch', () => {
  const actor = actorFor()
  requestAndProvideClock(actor)
  actor.send({
    type: 'SUBMISSION_FAILED', operationId: 'operation-1',
    failure: { code: 'IDEMPOTENCY_CONFLICT', retryable: false, commitOutcome: 'NOT_COMMITTED' },
  })
  const pending = actor.getSnapshot().context.pendingCommand!
  assert.equal(actor.getSnapshot().matches('tombstoningDispatchedCancellation'), true)
  assert.equal(actor.getSnapshot().status, 'active')
  actor.send({
    type: 'DISPATCH_CANCELLATION_TOMBSTONED', operationKey: 'foreign',
    ballotFingerprint: pending.ballotFingerprint,
    terminalDestination: {
      kind: 'TERMINAL_FAILURE', code: 'IDEMPOTENCY_CONFLICT',
      commitOutcome: 'NOT_COMMITTED',
    },
  })
  assert.equal(actor.getSnapshot().matches('tombstoningDispatchedCancellation'), true)
  actor.send({
    type: 'DISPATCH_CANCELLATION_TOMBSTONED',
    operationKey: ballotOperationKey(pending.identity),
    ballotFingerprint: pending.ballotFingerprint,
    terminalDestination: { kind: 'CANCELLED' },
  })
  assert.equal(actor.getSnapshot().matches('tombstoningDispatchedCancellation'), true)
  actor.send({
    type: 'DISPATCH_CANCELLATION_TOMBSTONED',
    operationKey: ballotOperationKey(pending.identity),
    ballotFingerprint: pending.ballotFingerprint,
    terminalDestination: {
      kind: 'TERMINAL_FAILURE', code: 'IDEMPOTENCY_CONFLICT',
      commitOutcome: 'NOT_COMMITTED',
    },
  })
  assert.equal(actor.getSnapshot().matches('terminalFailure'), true)
  assert.equal(actor.getSnapshot().status, 'done')
  actor.send({ type: 'RETRY_SUBMISSION' })
  assert.equal(actor.getSnapshot().context.effects.filter(
    (effect) => effect === 'dispatchAtomicBallotCommand').length, 1)

  const recreated = actorFor()
  recreated.send({
    type: 'REHYDRATE_UNKNOWN_OUTCOME', command: pending,
    journalStatus: 'DISPATCH_CANCELLATION_TOMBSTONED',
    terminalDestination: {
      kind: 'TERMINAL_FAILURE', code: 'IDEMPOTENCY_CONFLICT',
      commitOutcome: 'NOT_COMMITTED',
    },
    failure: { code: 'LOCAL_TRANSACTION_FAILED', retryable: true, commitOutcome: 'UNKNOWN' },
  })
  assert.equal(recreated.getSnapshot().matches('terminalFailure'), true)
  assert.deepEqual(recreated.getSnapshot().context.failure, {
    code: 'IDEMPOTENCY_CONFLICT', retryable: false, commitOutcome: 'NOT_COMMITTED',
  })
  assert.equal(recreated.getSnapshot().context.effects.includes('dispatchAtomicBallotCommand'), false)
})

test('cancellation after dispatch waits for repository proof and a racing commit wins', () => {
  const actor = actorFor()
  requestAndProvideClock(actor)
  actor.send({ type: 'CANCEL' })
  assert.equal(actor.getSnapshot().matches('cancelling'), true)
  assert.equal(actor.getSnapshot().context.effects.includes('requestOperationCancellation'), true)
  actor.send({ type: 'LOCAL_COMMIT', receipt: receipt() })
  assert.equal(actor.getSnapshot().matches({ committed: 'pendingSync' }), true)
})

test('PollVoting CLOSE and BACK after DISPATCHED wait for receipt or durable tombstone', () => {
  for (const type of ['CLOSE', 'BACK'] as const) {
    const actor = actorFor()
    requestAndProvideClock(actor)
    const pending = actor.getSnapshot().context.pendingCommand!
    actor.send({ type })
    assert.equal(actor.getSnapshot().matches('cancelling'), true)
    assert.equal(actor.getSnapshot().status, 'active')
    assert.equal(actor.getSnapshot().context.journalStatus, 'DISPATCHED')
    assert.deepEqual(actor.getSnapshot().context.pendingCommand, pending)

    if (type === 'CLOSE') {
      actor.send({ type: 'LOCAL_COMMIT', receipt: receipt() })
      assert.equal(actor.getSnapshot().matches({ committed: 'pendingSync' }), true)
      continue
    }

    actor.send({ type: 'CANCELLATION_CONFIRMED', operationId: 'operation-1' })
    assert.equal(actor.getSnapshot().matches('tombstoningDispatchedCancellation'), true)
    actor.send({
      type: 'DISPATCH_CANCELLATION_TOMBSTONE_FAILED',
      operationKey: ballotOperationKey(pending.identity),
      ballotFingerprint: pending.ballotFingerprint,
      error: {
        code: 'COMMAND_CANCELLATION_TOMBSTONE_FAILED', retryable: true,
        commitOutcome: 'NOT_COMMITTED',
      },
    })
    assert.equal(actor.getSnapshot().matches('dispatchedCancellationTombstoneFailed'), true)
    assert.equal(actor.getSnapshot().context.journalStatus, 'DISPATCHED')
    assert.deepEqual(actor.getSnapshot().context.pendingCommand, pending)
    actor.send({ type: 'BACK' })
    assert.equal(actor.getSnapshot().matches('dispatchedCancellationTombstoneFailed'), true)
    assert.deepEqual(actor.getSnapshot().context.pendingCommand, pending)
  }
})

test('PollVoting back during dispatch CAS and cancellation failure never clears the command', () => {
  const actor = actorFor()
  actor.send({ type: 'REQUEST_SUBMIT', operationId: 'operation-1' })
  actor.send({
    type: 'SUBMISSION_CLOCK_SNAPSHOT', operationId: 'operation-1',
    nowIso: '2026-08-28T11:59:59.999Z',
  })
  const pending = actor.getSnapshot().context.pendingCommand!
  actor.send({
    type: 'COMMAND_JOURNALED', operationKey: ballotOperationKey(pending.identity),
    ballotFingerprint: pending.ballotFingerprint,
  })
  assert.equal(actor.getSnapshot().matches('authorizingDispatch'), true)
  actor.send({ type: 'BACK' })
  assert.equal(actor.getSnapshot().matches('cancellingJournal'), true)
  actor.send({
    type: 'COMMAND_CANCEL_FAILED', operationKey: ballotOperationKey(pending.identity),
    ballotFingerprint: pending.ballotFingerprint,
    error: { code: 'COMMAND_CANCEL_FAILED', retryable: true, commitOutcome: 'NOT_COMMITTED' },
  })
  assert.equal(actor.getSnapshot().matches('journalCancellationFailed'), true)
  assert.equal(actor.getSnapshot().context.journalStatus, 'STAGED_NOT_DISPATCHED')
  assert.deepEqual(actor.getSnapshot().context.pendingCommand, pending)
  actor.send({ type: 'CLOSE' })
  assert.equal(actor.getSnapshot().matches('journalCancellationFailed'), true)
  assert.deepEqual(actor.getSnapshot().context.pendingCommand, pending)
})

test('confirmed dispatched cancellation becomes terminal only after durable tombstone resolution', () => {
  const actor = actorFor()
  requestAndProvideClock(actor)
  const pending = actor.getSnapshot().context.pendingCommand!
  actor.send({ type: 'CANCEL' })
  actor.send({ type: 'CANCELLATION_CONFIRMED', operationId: 'operation-1' })
  assert.equal(actor.getSnapshot().matches('tombstoningDispatchedCancellation'), true)
  assert.equal(actor.getSnapshot().status, 'active')
  assert.equal(actor.getSnapshot().context.effects.includes(
    'tombstoneDispatchedCancellation'), true)
  actor.send({
    type: 'DISPATCH_CANCELLATION_TOMBSTONE_UNKNOWN',
    operationKey: ballotOperationKey(pending.identity),
    ballotFingerprint: pending.ballotFingerprint,
    error: {
      code: 'COMMAND_CANCELLATION_TOMBSTONE_FAILED', retryable: true,
      commitOutcome: 'UNKNOWN',
    },
  })
  assert.equal(actor.getSnapshot().matches('resolvingJournalStatus'), true)
  actor.send({
    type: 'COMMAND_JOURNAL_STATUS_RESOLVED',
    operationKey: ballotOperationKey(pending.identity),
    ballotFingerprint: pending.ballotFingerprint,
    status: 'DISPATCH_CANCELLATION_TOMBSTONED',
    terminalDestination: { kind: 'CANCELLED' },
  })
  assert.equal(actor.getSnapshot().matches('cancelled'), true)
  assert.equal(actor.getSnapshot().context.journalStatus, 'DISPATCH_CANCELLATION_TOMBSTONED')
  assert.equal(actor.getSnapshot().context.effects.includes('navigateAfterLocalCommit'), false)

  const recreated = actorFor()
  recreated.send({
    type: 'REHYDRATE_UNKNOWN_OUTCOME', command: pending,
    journalStatus: 'DISPATCH_CANCELLATION_TOMBSTONED',
    terminalDestination: { kind: 'CANCELLED' },
    failure: { code: 'LOCAL_TRANSACTION_FAILED', retryable: true, commitOutcome: 'UNKNOWN' },
  })
  assert.equal(recreated.getSnapshot().matches('cancelled'), true)
  assert.equal(recreated.getSnapshot().context.effects.includes('dispatchAtomicBallotCommand'), false)
})

test('sync failure is post-commit and retry never resubmits the ballot', () => {
  const actor = actorFor()
  requestAndProvideClock(actor)
  actor.send({ type: 'LOCAL_COMMIT', receipt: receipt() })
  assert.equal(actor.getSnapshot().context.receipt?.syncStatus, 'LOCAL_PENDING')
  assert.deepEqual(actor.getSnapshot().context.receipt?.syncPayload.command,
    actor.getSnapshot().context.pendingCommand)
  actor.send({
    type: 'SYNC_FAILED', receiptId: 'receipt-1',
    error: { code: 'NETWORK_UNAVAILABLE', retryable: true },
  })
  assert.equal(actor.getSnapshot().matches({ committed: 'syncFailed' }), true)
  actor.send({ type: 'RETRY_SYNC' })
  assert.equal(actor.getSnapshot().matches({ committed: 'pendingSync' }), true)
  assert.equal(actor.getSnapshot().context.effects.filter(
    (effect) => effect === 'dispatchAtomicBallotCommand').length, 1)
  assert.equal(actor.getSnapshot().context.effects.filter(
    (effect) => effect === 'retryReceiptSync').length, 1)
  actor.send({ type: 'SYNC_COMPLETED', ack: syncAck() })
  assert.equal(actor.getSnapshot().matches('synced'), true)
  assert.equal(actor.getSnapshot().context.receipt?.syncStatus, 'SYNCED')
  assert.equal(actor.getSnapshot().context.receipt?.serverReceiptId, 'server-receipt-1')
  assert.equal(actor.getSnapshot().status, 'done')
})

test('non-retryable sync failure blocks retry and LOCAL_COMMIT cannot claim SYNCED', () => {
  const blocked = actorFor()
  requestAndProvideClock(blocked)
  blocked.send({ type: 'LOCAL_COMMIT', receipt: receipt() })
  blocked.send({
    type: 'SYNC_FAILED', receiptId: 'receipt-1',
    error: { code: 'PERMANENT_FAILURE', retryable: false },
  })
  blocked.send({ type: 'RETRY_SYNC' })
  assert.equal(blocked.getSnapshot().matches({ committed: 'syncFailed' }), true)
  assert.equal(blocked.getSnapshot().context.effects.includes('retryReceiptSync'), false)

  const acknowledged = actorFor()
  requestAndProvideClock(acknowledged)
  acknowledged.send({ type: 'LOCAL_COMMIT', receipt: receipt('SYNCED') })
  assert.equal(acknowledged.getSnapshot().matches('resolutionFailed'), true)
  assert.equal(acknowledged.getSnapshot().context.receipt, null)
  assert.deepEqual(acknowledged.getSnapshot().context.failure, {
    code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
  })
})

test('generic or mismatched server acknowledgement cannot mark the ballot synced', () => {
  const actor = actorFor()
  requestAndProvideClock(actor)
  actor.send({ type: 'LOCAL_COMMIT', receipt: receipt() })
  actor.send({
    type: 'SYNC_COMPLETED',
    ack: { ...syncAck(), ballotFingerprint: 'generic-batch-success' },
  })
  assert.equal(actor.getSnapshot().matches({ committed: 'pendingSync' }), true)
  assert.equal(actor.getSnapshot().context.receipt?.syncStatus, 'LOCAL_PENDING')
  actor.send({
    type: 'SYNC_COMPLETED',
    ack: { ...syncAck(), serverReceiptId: '' },
  })
  assert.equal(actor.getSnapshot().matches({ committed: 'pendingSync' }), true)
  assert.doesNotThrow(() => actor.send({
    type: 'SYNC_COMPLETED',
    ack: { ...syncAck(), identity: { ...syncAck().identity, pollRevision: 1.5 } },
  }))
  assert.equal(actor.getSnapshot().matches({ committed: 'pendingSync' }), true)
})

test('valid foreign receipts are stale while same-operation malformed receipts are inconsistent', () => {
  const actor = actorFor()
  requestAndProvideClock(actor)
  const foreignCommand = buildBallotCommand({
    ...command.identity, operationId: 'other-operation',
  }, command.entries, deadlineIso)
  actor.send({ type: 'LOCAL_COMMIT', receipt: {
    ...receipt(), operationId: 'other-operation',
    operationKey: ballotOperationKey(foreignCommand.identity),
    ballotFingerprint: foreignCommand.ballotFingerprint,
    syncPayload: { schemaVersion: 1, localReceiptId: 'receipt-1', command: foreignCommand },
  } })
  assert.equal(actor.getSnapshot().matches('submitting'), true)

  actor.send({ type: 'LOCAL_COMMIT', receipt: { ...receipt(), operationKey: 'foreign-key' } })
  assert.equal(actor.getSnapshot().matches('resolutionFailed'), true)
  assert.deepEqual(actor.getSnapshot().context.failure, {
    code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
  })
  assert.equal(actor.getSnapshot().context.effects.includes('navigateAfterLocalCommit'), false)
})

test('outer-current receipt with an inner command for operation two is never classified stale', () => {
  const actor = actorFor()
  requestAndProvideClock(actor)
  const operationTwo = buildBallotCommand({
    ...command.identity, operationId: 'operation-2',
  }, command.entries, deadlineIso)

  actor.send({
    type: 'LOCAL_COMMIT',
    receipt: {
      ...receipt(),
      syncPayload: { schemaVersion: 1, localReceiptId: 'receipt-1', command: operationTwo },
    },
  })

  assert.equal(actor.getSnapshot().matches('resolutionFailed'), true)
  assert.equal(actor.getSnapshot().status, 'done')
  assert.deepEqual(actor.getSnapshot().context.failure, {
    code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
  })
  assert.equal(actor.getSnapshot().context.effects.includes('navigateAfterLocalCommit'), false)
})
