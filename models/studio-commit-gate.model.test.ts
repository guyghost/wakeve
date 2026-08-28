import assert from 'node:assert/strict'
import test from 'node:test'

import {
  applyStudioServerCommit,
  buildStudioCommitEnvelope,
  deriveStudioExpectedResultingArtwork,
  finalizeStudioServerReceiptRace,
  isStudioPendingSync,
  projectCreationSubmitAffordance,
  projectStudioDurableEnvelope,
  projectStudioChoiceAffordance,
  reduceStudioCommitGate,
  studioSyncFailureFromServerReject,
  studioPendingSyncBinding,
  type StudioCommitEnvelope,
  type StudioCommitGateState,
  type StudioLocalCommitReceipt,
  type StudioPendingSyncSubject,
} from './studio-commit-gate.model.ts'

const requestPayload = {
  schemaVersion: 1 as const,
  subject: { kind: 'NEW' as const, eventId: 'event-1' },
  actorId: 'organizer-1',
  draftRevision: 4,
  canonicalDraftJson: '{"title":"Weekend"}',
  artwork: { kind: 'NONE' as const },
  expectedResultingArtwork: { kind: 'NONE' as const },
}

const envelope: StudioCommitEnvelope = buildStudioCommitEnvelope(
  { operationId: 'create-1', draftRevision: 4 }, requestPayload, 2,
)

const localReceipt = (overrides: Partial<StudioLocalCommitReceipt> = {}): StudioLocalCommitReceipt => ({
  receiptId: 'receipt-1',
  eventId: 'event-1',
  committedRevision: 5,
  commitEnvelope: envelope,
  syncStatus: 'PENDING_SYNC',
  serverReceiptId: null,
  pendingSubject: {
    schemaVersion: 1,
    eventId: 'event-1',
    committedRevision: 5,
    localReceiptId: 'receipt-1',
    envelope,
    expectedResultingArtwork: envelope.expectedResultingArtwork,
  },
  ...overrides,
})

const begin = () => reduceStudioCommitGate(
  { kind: 'IDLE' },
  { type: 'CONFIRM_COMMIT', envelope },
)

test('Studio commit requires a complete bounded durable envelope', () => {
  const invalid = reduceStudioCommitGate({ kind: 'IDLE' }, {
    type: 'CONFIRM_COMMIT', envelope: { ...envelope, durableOperationRef: '' },
  })
  assert.deepEqual(invalid, { state: { kind: 'IDLE' }, effects: [] })
})

test('durable Studio reference and fingerprint derive from one persisted payload', () => {
  assert.deepEqual(projectStudioDurableEnvelope(envelope), { kind: 'VALID', envelope })
  const divergentPayload = {
    ...envelope,
    requestPayload: { ...envelope.requestPayload, canonicalDraftJson: '{"title":"Other"}' },
  }
  const projection = projectStudioDurableEnvelope(divergentPayload)
  assert.equal(projection.kind, 'INCONSISTENT')
  assert.deepEqual(projection.kind === 'INCONSISTENT' ? {
    code: projection.code, retryable: projection.retryable,
    commitOutcome: projection.commitOutcome,
  } : null, {
    code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
  })
  assert.equal(projectStudioDurableEnvelope({
    ...envelope, requestFingerprint: 'foreign',
  }).kind, 'INCONSISTENT')
})

test('Studio commit gate permits one save, callback, and navigation consumption', () => {
  const started = begin()
  let state: StudioCommitGateState = started.state
  const effects = [...started.effects]

  for (const event of [
    { type: 'CONFIRM_COMMIT' as const, envelope },
    {
      type: 'CONFIRM_COMMIT' as const,
      envelope: { ...envelope, identity: { operationId: 'create-2', draftRevision: 5 } },
    },
    {
      type: 'CONFIRM_COMMIT' as const,
      envelope: { ...envelope, maxResolutionAttempts: 3 },
    },
  ]) {
    const result = reduceStudioCommitGate(state, event)
    state = result.state
    effects.push(...result.effects)
  }

  let result = reduceStudioCommitGate(state, {
    type: 'LOCAL_COMMIT', envelope, receipt: localReceipt(),
  })
  state = result.state
  effects.push(...result.effects)
  result = reduceStudioCommitGate(state, {
    type: 'LOCAL_COMMIT', envelope, receipt: localReceipt(),
  })
  effects.push(...result.effects)
  result = reduceStudioCommitGate(state, { type: 'OPEN_COMMITTED_EVENT' })
  effects.push(...result.effects)

  assert.equal(state.kind, 'PENDING_SYNC')
  assert.equal(effects.filter((effect) => typeof effect === 'object' &&
    effect.type === 'PERSIST_DURABLE_ENVELOPE_THEN_INVOKE').length, 1)
  assert.equal(effects.filter((effect) => effect === 'DELIVER_COMPLETION_CALLBACK').length, 1)
  assert.equal(effects.filter((effect) => effect === 'NAVIGATE_TO_COMMITTED_EVENT').length, 1)
  assert.equal(effects.filter((effect) => typeof effect === 'object' &&
    effect.type === 'TRIGGER_SYNC_IMMEDIATELY').length, 1)
  assert.equal(effects.filter((effect) => effect === 'REJECT_COMMIT_ALREADY_IN_FLIGHT').length, 2)
})

test('Studio pending sync dispatches its subject and only an exact server ACK can mark SYNCED', () => {
  const committed = reduceStudioCommitGate(begin().state, {
    type: 'LOCAL_COMMIT', envelope, receipt: localReceipt(),
  })
  assert.equal(committed.state.kind, 'PENDING_SYNC')
  assert.deepEqual(committed.effects.slice(-1), [{
    type: 'TRIGGER_SYNC_IMMEDIATELY', subject: localReceipt().pendingSubject,
    idempotent: true,
  }])
  assert.deepEqual('receipt' in committed.state ? committed.state.receipt.pendingSubject : null,
    localReceipt().pendingSubject)

  const genericBatchSuccess = reduceStudioCommitGate(committed.state, {
    type: 'GENERIC_BATCH_SUCCESS', batchId: 'batch-1',
  } as never)
  assert.equal(genericBatchSuccess.state.kind, 'PENDING_SYNC')

  const ack = {
    localReceiptId: 'receipt-1', serverReceiptId: 'server-receipt-1',
    eventId: 'event-1', committedRevision: 5,
    durableOperationRef: envelope.durableOperationRef,
    requestFingerprint: envelope.requestFingerprint,
    outcome: 'APPLIED' as const,
    disposition: 'CREATED' as const,
    artwork: { kind: 'NONE' as const },
  }
  const mismatched = reduceStudioCommitGate(committed.state, {
    type: 'SYNC_ACK', ack: { ...ack, requestFingerprint: 'foreign' },
  })
  assert.equal(mismatched.state.kind, 'PENDING_SYNC')
  const wrongDisposition = reduceStudioCommitGate(committed.state, {
    type: 'SYNC_ACK', ack: { ...ack, disposition: 'UPDATED' },
  })
  assert.equal(wrongDisposition.state.kind, 'PENDING_SYNC')
  const wrongArtwork = reduceStudioCommitGate(committed.state, {
    type: 'SYNC_ACK', ack: { ...ack, artwork: { kind: 'PRESET', presetId: 'other' } },
  })
  assert.equal(wrongArtwork.state.kind, 'PENDING_SYNC')
  assert.equal(isStudioPendingSync(wrongDisposition.state), true)
  assert.equal(isStudioPendingSync(wrongArtwork.state), true)

  const genericConflict = reduceStudioCommitGate(committed.state, {
    type: 'GENERIC_SYNC_CONFLICT', code: 'conflict',
  } as never)
  assert.equal(genericConflict.state.kind, 'PENDING_SYNC')

  const terminalMetadata = studioSyncFailureFromServerReject(localReceipt(), {
    kind: 'REJECT', code: 'STALE_BASE_REVISION', retryable: false,
  })
  const foreignTerminal = reduceStudioCommitGate(committed.state, {
    type: 'SYNC_FAILED', metadata: { ...terminalMetadata, localReceiptId: 'foreign-receipt' },
  })
  assert.equal(foreignTerminal.state.kind, 'PENDING_SYNC')
  const terminal = reduceStudioCommitGate(committed.state, {
    type: 'SYNC_FAILED', metadata: terminalMetadata,
  })
  assert.equal(terminal.state.kind, 'SYNC_TERMINAL_FAILURE')
  assert.deepEqual('metadata' in terminal.state ? terminal.state.metadata : null,
    terminalMetadata)
  assert.deepEqual(reduceStudioCommitGate(terminal.state, { type: 'RETRY_SYNC' }).effects, [])
  const genericTerminal = reduceStudioCommitGate(committed.state, {
    type: 'SYNC_FAILED',
    metadata: {
      ...terminalMetadata,
      error: { code: 'GENERIC_CONFLICT', retryable: false },
    },
  } as never)
  assert.equal(genericTerminal.state.kind, 'PENDING_SYNC')

  const failed = reduceStudioCommitGate(committed.state, {
    type: 'SYNC_FAILED',
    metadata: {
      localReceiptId: 'receipt-1', eventId: 'event-1', committedRevision: 5,
      durableOperationRef: envelope.durableOperationRef,
      requestFingerprint: envelope.requestFingerprint,
      status: 'RETRYABLE_FAILURE',
      error: { code: 'NETWORK_UNAVAILABLE', retryable: true },
    },
  })
  assert.equal(failed.state.kind, 'SYNC_FAILED')
  const retry = reduceStudioCommitGate(failed.state, { type: 'RETRY_SYNC' })
  assert.equal(retry.state.kind, 'PENDING_SYNC')
  assert.deepEqual(retry.effects, [{
    type: 'RETRY_PENDING_SYNC_SUBJECT', subject: localReceipt().pendingSubject,
  }])
  assert.equal(retry.effects.some((effect) => typeof effect === 'object' &&
    effect.type === 'PERSIST_DURABLE_ENVELOPE_THEN_INVOKE'), false)

  const synced = reduceStudioCommitGate(retry.state, { type: 'SYNC_ACK', ack })
  assert.equal(synced.state.kind, 'SYNCED')
  assert.equal('serverReceiptId' in synced.state ? synced.state.serverReceiptId : null,
    'server-receipt-1')
})

test('Studio rehydrate closes the SQLite-to-sync crash window exactly once without navigation', () => {
  const rehydrate = {
    type: 'REHYDRATE_STUDIO_RECEIPT' as const,
    session: { attachment: 'ATTACHED' as const, eventId: 'event-1', actorId: 'organizer-1' },
    record: localReceipt(),
  }
  const restored = reduceStudioCommitGate({ kind: 'IDLE' }, rehydrate)
  assert.equal(restored.state.kind, 'PENDING_SYNC')
  assert.equal('navigationConsumed' in restored.state ? restored.state.navigationConsumed : null,
    false)
  assert.deepEqual(restored.effects, [{
    type: 'TRIGGER_SYNC_IMMEDIATELY', subject: localReceipt().pendingSubject, idempotent: true,
  }])
  assert.equal(restored.effects.includes('NAVIGATE_TO_COMMITTED_EVENT'), false)

  const replay = reduceStudioCommitGate(restored.state, rehydrate)
  assert.deepEqual(replay, { state: restored.state, effects: [] })

  const detached = reduceStudioCommitGate({ kind: 'IDLE' }, {
    ...rehydrate,
    session: { ...rehydrate.session, attachment: 'DETACHED' as const },
  })
  assert.equal(detached.state.kind, 'DETACHED_PENDING_SYNC')
  assert.equal(isStudioPendingSync(detached.state), true)
  assert.deepEqual(studioPendingSyncBinding(detached.state), {
    localReceiptId: 'receipt-1', subject: localReceipt().pendingSubject,
  })
  assert.deepEqual(detached.effects, restored.effects)
  assert.equal(detached.effects.filter((effect) => typeof effect === 'object' &&
    effect.type === 'TRIGGER_SYNC_IMMEDIATELY' && effect.idempotent).length, 1)
})

test('Studio rehydrate fails closed for foreign or malformed pending receipts', () => {
  const forgedRefEnvelope = { ...envelope, durableOperationRef: 'forged-ref' }
  const forgedFingerprintEnvelope = { ...envelope, requestFingerprint: 'forged-fingerprint' }
  const records: unknown[] = [
    { ...localReceipt(), commitEnvelope: null },
    { ...localReceipt(), pendingSubject: null },
    localReceipt({
      commitEnvelope: forgedRefEnvelope,
      pendingSubject: { ...localReceipt().pendingSubject, envelope: forgedRefEnvelope },
    }),
    localReceipt({
      commitEnvelope: forgedFingerprintEnvelope,
      pendingSubject: { ...localReceipt().pendingSubject, envelope: forgedFingerprintEnvelope },
    }),
    localReceipt({ committedRevision: 6 }),
    localReceipt({
      pendingSubject: { ...localReceipt().pendingSubject, envelope: forgedRefEnvelope },
    }),
  ]
  for (const record of records) {
    const result = reduceStudioCommitGate({ kind: 'IDLE' }, {
      type: 'REHYDRATE_STUDIO_RECEIPT',
      session: { attachment: 'ATTACHED', eventId: 'event-1', actorId: 'organizer-1' },
      record,
    })
    assert.equal(result.state.kind, 'REHYDRATION_FAILED')
    assert.deepEqual('failure' in result.state ? result.state.failure : null, {
      code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
    })
    assert.deepEqual(result.effects, [])
    assert.equal(projectCreationSubmitAffordance(result.state).isEnabled, false)
  }

  const foreignActor = reduceStudioCommitGate({ kind: 'IDLE' }, {
    type: 'REHYDRATE_STUDIO_RECEIPT',
    session: { attachment: 'ATTACHED', eventId: 'event-1', actorId: 'intruder-1' },
    record: localReceipt(),
  })
  assert.equal(foreignActor.state.kind, 'REHYDRATION_FAILED')
})

test('Studio server atomically persists artwork and receipt then replays the exact ACK', () => {
  const serverEnvelope = buildStudioCommitEnvelope(envelope.identity, {
    ...requestPayload,
    artwork: { kind: 'PRESET', presetId: 'aurora' },
    expectedResultingArtwork: { kind: 'PRESET', presetId: 'aurora' },
  }, 2)
  const subject: StudioPendingSyncSubject = {
    schemaVersion: 1, eventId: 'event-1', committedRevision: 1,
    localReceiptId: 'local-server-1', envelope: serverEnvelope,
    expectedResultingArtwork: serverEnvelope.expectedResultingArtwork,
  }
  const applied = applyStudioServerCommit('organizer-1', subject, null, null)
  assert.equal(applied.kind, 'APPLY_ATOMIC')
  assert.deepEqual(applied.kind === 'APPLY_ATOMIC' ? applied.writes.aggregate.artwork : null, {
    kind: 'PRESET', presetId: 'aurora',
  })
  assert.deepEqual(applied.kind === 'APPLY_ATOMIC' ? {
    organizerId: applied.writes.aggregate.organizerId,
    status: applied.writes.aggregate.status,
  } : null, { organizerId: 'organizer-1', status: 'DRAFT' })
  assert.equal(applied.kind === 'APPLY_ATOMIC' ? applied.ack.committedRevision : null, 1)
  assert.deepEqual(applied.kind === 'APPLY_ATOMIC' ? applied.writes.receipt.idempotencyKey : null, {
    durableOperationRef: serverEnvelope.durableOperationRef,
    requestFingerprint: serverEnvelope.requestFingerprint,
  })

  assert.equal(applied.kind, 'APPLY_ATOMIC')
  if (applied.kind !== 'APPLY_ATOMIC') return
  const replay = applyStudioServerCommit(
    'organizer-1', subject, {
      ...applied.writes.aggregate,
      organizerId: 'replacement-organizer', status: 'FINALIZED', revision: 99,
      artwork: { kind: 'NONE' },
    }, applied.writes.receipt)
  assert.deepEqual(replay, {
    kind: 'RETURN_EXISTING_ACK', ack: applied.ack, aggregateMutation: null,
  })
  assert.deepEqual(replay.kind === 'RETURN_EXISTING_ACK' ? replay.ack : null, applied.ack)
  assert.deepEqual(applied.ack, {
    localReceiptId: 'local-server-1',
    serverReceiptId: applied.writes.receipt.serverReceiptId,
    eventId: 'event-1', committedRevision: 1,
    durableOperationRef: serverEnvelope.durableOperationRef,
    requestFingerprint: serverEnvelope.requestFingerprint,
    outcome: 'APPLIED', disposition: 'CREATED',
    artwork: { kind: 'PRESET', presetId: 'aurora' },
  })
  assert.deepEqual(Object.keys(applied.writes).sort(), ['aggregate', 'receipt'])

  const divergentEnvelope = {
    ...serverEnvelope,
    requestPayload: {
      ...serverEnvelope.requestPayload,
      artwork: { kind: 'PRESET' as const, presetId: 'different' },
    },
  }
  assert.deepEqual(applyStudioServerCommit('organizer-1', {
    ...subject, envelope: divergentEnvelope,
  },
    applied.writes.aggregate, applied.writes.receipt), {
    kind: 'REJECT', code: 'IDEMPOTENCY_CONFLICT', retryable: false,
  })
})

test('two Studio server instances converge on the winner receipt after a unique-key race', () => {
  const subject: StudioPendingSyncSubject = {
    schemaVersion: 1, eventId: 'event-1', committedRevision: 1,
    localReceiptId: 'race-local-1', envelope,
    expectedResultingArtwork: envelope.expectedResultingArtwork,
  }
  const instanceA = applyStudioServerCommit('organizer-1', subject, null, null)
  const instanceB = applyStudioServerCommit('organizer-1', subject, null, null)
  assert.equal(instanceA.kind, 'APPLY_ATOMIC')
  assert.equal(instanceB.kind, 'APPLY_ATOMIC')
  if (instanceA.kind !== 'APPLY_ATOMIC' || instanceB.kind !== 'APPLY_ATOMIC') return

  // Instance A wins the atomic aggregate+receipt insert. Instance B's unique-key finalizer
  // re-reads that non-null winner inside its transaction instead of returning generic conflict.
  const losingFinalizer = finalizeStudioServerReceiptRace(
    'organizer-1', subject, instanceA.writes.receipt)
  assert.deepEqual(losingFinalizer, {
    kind: 'RETURN_EXISTING_ACK', ack: instanceA.ack, aggregateMutation: null,
  })
  assert.deepEqual(losingFinalizer.kind === 'RETURN_EXISTING_ACK'
    ? losingFinalizer.ack : null, instanceA.writes.receipt.ack)
  assert.notEqual(losingFinalizer, null)

  const corruptWinner = {
    ...instanceA.writes.receipt,
    ack: {
      ...instanceA.writes.receipt.ack,
      artwork: { kind: 'PRESET' as const, presetId: 'corrupt-race-artwork' },
    },
  }
  assert.deepEqual(finalizeStudioServerReceiptRace('organizer-1', subject, corruptWinner), {
    kind: 'REJECT', code: 'REPOSITORY_INCONSISTENT', retryable: false,
  })
})

test('Studio EDIT rejects stale base revision and ACKs the actual atomic revision', () => {
  const current = {
    eventId: 'event-1', organizerId: 'organizer-1', status: 'DRAFT' as const,
    revision: 3, canonicalDraftJson: '{"title":"Old"}',
    artwork: { kind: 'NONE' as const },
  }
  const editEnvelope = (baseRevision: number) => buildStudioCommitEnvelope(
    { operationId: `edit-${baseRevision}`, draftRevision: 8 },
    {
      schemaVersion: 1,
      subject: { kind: 'EDIT_EXISTING', eventId: 'event-1', baseRevision },
      actorId: 'organizer-1', draftRevision: 8,
      canonicalDraftJson: '{"title":"Updated"}',
      artwork: { kind: 'EXISTING_SERVER_ASSET', assetId: 'asset-1', assetRevision: 6 },
      expectedResultingArtwork: {
        kind: 'EXISTING_SERVER_ASSET', assetId: 'asset-1', assetRevision: 6,
      },
    },
    2,
  )
  const staleEnvelope = editEnvelope(2)
  assert.deepEqual(applyStudioServerCommit('organizer-1', {
    schemaVersion: 1, eventId: 'event-1', committedRevision: 3,
    localReceiptId: 'edit-local-stale', envelope: staleEnvelope,
    expectedResultingArtwork: staleEnvelope.expectedResultingArtwork,
  }, current, null), {
    kind: 'REJECT', code: 'STALE_BASE_REVISION', retryable: false,
  })

  const currentEnvelope = editEnvelope(3)
  const applied = applyStudioServerCommit('organizer-1', {
    schemaVersion: 1, eventId: 'event-1', committedRevision: 4,
    localReceiptId: 'edit-local-current', envelope: currentEnvelope,
    expectedResultingArtwork: currentEnvelope.expectedResultingArtwork,
  }, current, null)
  assert.equal(applied.kind, 'APPLY_ATOMIC')
  assert.equal(applied.kind === 'APPLY_ATOMIC' ? applied.ack.committedRevision : null, 4)
  assert.deepEqual(applied.kind === 'APPLY_ATOMIC' ? applied.writes.aggregate.artwork : null, {
    kind: 'EXISTING_SERVER_ASSET', assetId: 'asset-1', assetRevision: 6,
  })
})

test('KEEP_EXISTING binds exact snapshot artwork through client ACK, replay, and race', () => {
  const aggregateArtwork = { kind: 'PRESET' as const, presetId: 'snapshot-artwork' }
  const expectedResultingArtwork = deriveStudioExpectedResultingArtwork(
    { kind: 'KEEP_EXISTING' }, aggregateArtwork)
  assert.deepEqual(expectedResultingArtwork, aggregateArtwork)
  if (expectedResultingArtwork === null) return
  const keepEnvelope = buildStudioCommitEnvelope(
    { operationId: 'keep-artwork-1', draftRevision: 10 },
    {
      schemaVersion: 1,
      subject: { kind: 'EDIT_EXISTING', eventId: 'event-1', baseRevision: 3 },
      actorId: 'organizer-1', draftRevision: 10,
      canonicalDraftJson: '{"title":"Keep artwork"}',
      artwork: { kind: 'KEEP_EXISTING' },
      expectedResultingArtwork,
    },
    2,
  )
  const subject: StudioPendingSyncSubject = {
    schemaVersion: 1, eventId: 'event-1', committedRevision: 4,
    localReceiptId: 'keep-local-1', envelope: keepEnvelope, expectedResultingArtwork,
  }
  const applied = applyStudioServerCommit('organizer-1', subject, {
    eventId: 'event-1', organizerId: 'organizer-1', status: 'DRAFT', revision: 3,
    canonicalDraftJson: '{"title":"Before"}', artwork: aggregateArtwork,
  }, null)
  assert.equal(applied.kind, 'APPLY_ATOMIC')
  if (applied.kind !== 'APPLY_ATOMIC') return
  assert.deepEqual(applied.ack.artwork, expectedResultingArtwork)

  const receipt: StudioLocalCommitReceipt = {
    receiptId: 'keep-local-1', eventId: 'event-1', committedRevision: 4,
    commitEnvelope: keepEnvelope, syncStatus: 'PENDING_SYNC', serverReceiptId: null,
    pendingSubject: subject,
  }
  const pending = reduceStudioCommitGate({ kind: 'IDLE' }, {
    type: 'REHYDRATE_STUDIO_RECEIPT',
    session: { attachment: 'ATTACHED', eventId: 'event-1', actorId: 'organizer-1' },
    record: receipt,
  })
  assert.equal(pending.state.kind, 'PENDING_SYNC')
  const substitutedAck = {
    ...applied.ack, artwork: { kind: 'PRESET' as const, presetId: 'substituted-artwork' },
  }
  assert.equal(reduceStudioCommitGate(pending.state, {
    type: 'SYNC_ACK', ack: substitutedAck,
  }).state.kind, 'PENDING_SYNC')
  assert.equal(reduceStudioCommitGate(pending.state, {
    type: 'SYNC_ACK', ack: applied.ack,
  }).state.kind, 'SYNCED')

  const substitutedWinner = {
    ...applied.writes.receipt,
    ack: substitutedAck,
  }
  assert.deepEqual(applyStudioServerCommit(
    'organizer-1', subject, applied.writes.aggregate, substitutedWinner), {
    kind: 'REJECT', code: 'REPOSITORY_INCONSISTENT', retryable: false,
  })
  assert.deepEqual(finalizeStudioServerReceiptRace(
    'organizer-1', subject, substitutedWinner), {
    kind: 'REJECT', code: 'REPOSITORY_INCONSISTENT', retryable: false,
  })

  assert.deepEqual(applyStudioServerCommit('organizer-1', subject, {
    eventId: 'event-1', organizerId: 'organizer-1', status: 'DRAFT', revision: 3,
    canonicalDraftJson: '{"title":"Before"}', artwork: { kind: 'NONE' },
  }, null), {
    kind: 'REJECT', code: 'REPOSITORY_INCONSISTENT', retryable: false,
  })
})

test('Studio server rejects forged actors and non-draft edits before any write', () => {
  const createSubject: StudioPendingSyncSubject = {
    schemaVersion: 1, eventId: 'event-1', committedRevision: 1,
    localReceiptId: 'auth-create-1', envelope,
    expectedResultingArtwork: envelope.expectedResultingArtwork,
  }
  assert.deepEqual(applyStudioServerCommit('intruder-1', createSubject, null, null), {
    kind: 'REJECT', code: 'FORBIDDEN', retryable: false,
  })

  const editEnvelope = buildStudioCommitEnvelope(
    { operationId: 'auth-edit-1', draftRevision: 9 },
    {
      ...requestPayload,
      subject: { kind: 'EDIT_EXISTING', eventId: 'event-1', baseRevision: 3 },
      draftRevision: 9,
    },
    2,
  )
  const editSubject: StudioPendingSyncSubject = {
    schemaVersion: 1, eventId: 'event-1', committedRevision: 4,
    localReceiptId: 'auth-edit-local-1', envelope: editEnvelope,
    expectedResultingArtwork: editEnvelope.expectedResultingArtwork,
  }
  const draftAggregate = {
    eventId: 'event-1', organizerId: 'organizer-1', status: 'DRAFT' as const,
    revision: 3, canonicalDraftJson: '{"title":"Old"}', artwork: { kind: 'NONE' as const },
  }
  assert.deepEqual(applyStudioServerCommit('organizer-1', editSubject, {
    ...draftAggregate, organizerId: 'another-organizer',
  }, null), {
    kind: 'REJECT', code: 'FORBIDDEN', retryable: false,
  })
  assert.deepEqual(applyStudioServerCommit('organizer-1', editSubject, {
    ...draftAggregate, status: 'POLLING',
  }, null), {
    kind: 'REJECT', code: 'EVENT_NOT_DRAFT', retryable: false,
  })
})

test('failed-before-commit retains envelope and retry uses stable operation', () => {
  const foreignFailure = reduceStudioCommitGate(begin().state, {
    type: 'FAIL_BEFORE_COMMIT',
    envelope: { ...envelope, durableOperationRef: 'foreign' },
    failure: { code: 'REPOSITORY_UNAVAILABLE', retryable: true },
  })
  assert.equal(foreignFailure.state.kind, 'COMMITTING')

  const failed = reduceStudioCommitGate(begin().state, {
    type: 'FAIL_BEFORE_COMMIT',
    envelope,
    failure: { code: 'REPOSITORY_UNAVAILABLE', retryable: true },
  })
  assert.equal(failed.state.kind, 'FAILED_BEFORE_COMMIT')
  assert.deepEqual('envelope' in failed.state ? failed.state.envelope : null, envelope)

  const retry = reduceStudioCommitGate(failed.state, {
    type: 'RETRY_BEFORE_COMMIT', envelope,
  })
  assert.deepEqual(retry.state, { kind: 'COMMITTING', envelope })
  assert.deepEqual(retry.effects, [{
    type: 'PERSIST_DURABLE_ENVELOPE_THEN_INVOKE', durableEnvelope: envelope,
  }])

  const nonRetryable = reduceStudioCommitGate({
    kind: 'FAILED_BEFORE_COMMIT', envelope,
    failure: { code: 'PERMANENT_FAILURE', retryable: false },
  }, { type: 'RETRY_BEFORE_COMMIT', envelope })
  assert.equal(nonRetryable.state.kind, 'FAILED_BEFORE_COMMIT')
  assert.deepEqual(nonRetryable.effects, [])
})

test('attached unknown commit outcome enters durable resolution, never pre-commit failure', () => {
  const unknown = reduceStudioCommitGate(begin().state, {
    type: 'OUTCOME_UNKNOWN', envelope,
    failure: { code: 'COMMIT_OUTCOME_UNKNOWN', retryable: true },
  })
  assert.equal(unknown.state.kind, 'DETACHED_COMMITTING')
  assert.deepEqual('envelope' in unknown.state ? unknown.state.envelope : null, envelope)
  assert.deepEqual('lastFailure' in unknown.state ? unknown.state.lastFailure : null, {
    code: 'COMMIT_OUTCOME_UNKNOWN', retryable: true,
  })
  assert.equal(unknown.effects.some((effect) => typeof effect === 'object' &&
    effect.type === 'PERSIST_DURABLE_ENVELOPE_THEN_INVOKE'), false)

  const foreignEnvelope = buildStudioCommitEnvelope(envelope.identity, {
    ...requestPayload, canonicalDraftJson: '{"title":"Foreign"}',
  }, 2)
  const foreignRetry = reduceStudioCommitGate(unknown.state, {
    type: 'RETRY_RESOLUTION', envelope: foreignEnvelope,
    attempt: { attemptId: 'foreign-resolution-1', fence: 1 },
  })
  assert.equal(foreignRetry.state.kind, 'DETACHED_COMMITTING')
  assert.deepEqual(foreignRetry.effects, [])

  const retry = reduceStudioCommitGate(unknown.state, {
    type: 'RETRY_RESOLUTION', envelope,
    attempt: { attemptId: 'attached-unknown-resolution-1', fence: 1 },
  })
  assert.equal(retry.state.kind, 'DETACHED_RESOLVING')
  assert.deepEqual(retry.effects, ['RESOLVE_OUTCOME_IDEMPOTENTLY'])

  const blankResolutionProof = reduceStudioCommitGate(retry.state, {
    type: 'RESOLUTION_RESULT', envelope,
    attempt: { attemptId: 'resolution-1', fence: 1 },
    outcome: { kind: 'PROVEN_NON_COMMIT', proofRef: '   ' },
  })
  assert.equal(blankResolutionProof.state.kind, 'DETACHED_RESOLVING')
  assert.deepEqual(blankResolutionProof.effects, [])

  const terminal = reduceStudioCommitGate(begin().state, {
    type: 'OUTCOME_UNKNOWN', envelope,
    failure: { code: 'COMMIT_OUTCOME_UNKNOWN', retryable: false },
  })
  assert.equal(terminal.state.kind, 'DETACHED_RESOLUTION_FAILED')
})

test('outer-current resolution with a divergent inner commit envelope is inconsistency UNKNOWN', () => {
  const unknown = reduceStudioCommitGate(begin().state, {
    type: 'OUTCOME_UNKNOWN', envelope,
    failure: { code: 'COMMIT_OUTCOME_UNKNOWN', retryable: true },
  })
  const resolving = reduceStudioCommitGate(unknown.state, {
    type: 'RETRY_RESOLUTION', envelope,
    attempt: { attemptId: 'decode-inner-1', fence: 1 },
  })
  const innerEnvelope = buildStudioCommitEnvelope(envelope.identity, {
    ...requestPayload,
    artwork: { kind: 'PRESET', presetId: 'inner-other' },
    expectedResultingArtwork: { kind: 'PRESET', presetId: 'inner-other' },
  }, 2)
  const innerReceipt = localReceipt({
    commitEnvelope: innerEnvelope,
    pendingSubject: { ...localReceipt().pendingSubject, envelope: innerEnvelope },
  })
  const result = reduceStudioCommitGate(resolving.state, {
    type: 'RESOLUTION_RESULT', envelope,
    attempt: { attemptId: 'decode-inner-1', fence: 1 },
    outcome: { kind: 'LOCAL_COMMIT', receipt: innerReceipt },
  })
  assert.equal(result.state.kind, 'DETACHED_RESOLUTION_FAILED')
  assert.deepEqual('failure' in result.state ? result.state.failure : null, {
    code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
  })
  assert.deepEqual(result.effects, [])
  const corruptionRetry = reduceStudioCommitGate(result.state, {
    type: 'RETRY_RESOLUTION', envelope,
    attempt: { attemptId: 'must-not-retry-corruption', fence: 2 },
  })
  assert.equal(corruptionRetry.state.kind, 'DETACHED_RESOLUTION_FAILED')
  assert.deepEqual(corruptionRetry.effects, [])

  const missingEnvelope = reduceStudioCommitGate(resolving.state, {
    type: 'RESOLUTION_RESULT', envelope,
    attempt: { attemptId: 'decode-inner-1', fence: 1 },
    outcome: {
      kind: 'LOCAL_COMMIT',
      receipt: { ...localReceipt(), commitEnvelope: null },
    },
  })
  assert.equal(missingEnvelope.state.kind, 'DETACHED_RESOLUTION_FAILED')
  assert.deepEqual('failure' in missingEnvelope.state ? missingEnvelope.state.failure : null, {
    code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
  })
  assert.equal(projectCreationSubmitAffordance(missingEnvelope.state).isEnabled, false)
})

test('close during commit retains envelope and late commit never auto-navigates', () => {
  const detached = reduceStudioCommitGate(begin().state, { type: 'CLOSE' })
  assert.equal(detached.state.kind, 'DETACHED_COMMITTING')
  assert.deepEqual('envelope' in detached.state ? detached.state.envelope : null, envelope)
  assert.deepEqual(detached.effects, ['CONSUME_CLOSE_WITHOUT_CANCELLATION'])
  assert.deepEqual(reduceStudioCommitGate(detached.state, { type: 'CLOSE' }).effects,
    ['CONSUME_CLOSE_WITHOUT_CANCELLATION'])

  const blankReceipt = reduceStudioCommitGate(detached.state, {
    type: 'LATE_LOCAL_COMMIT', envelope, receipt: { ...localReceipt(), receiptId: '' },
  })
  assert.equal(blankReceipt.state.kind, 'DETACHED_RESOLUTION_FAILED')
  assert.deepEqual('failure' in blankReceipt.state ? blankReceipt.state.failure : null, {
    code: 'REPOSITORY_INCONSISTENT', retryable: false, commitOutcome: 'UNKNOWN',
  })

  const committed = reduceStudioCommitGate(detached.state, {
    type: 'LATE_LOCAL_COMMIT', envelope, receipt: localReceipt(),
  })
  assert.equal(committed.state.kind, 'DETACHED_PENDING_SYNC')
  assert.deepEqual(committed.effects, [{
    type: 'TRIGGER_SYNC_IMMEDIATELY', subject: localReceipt().pendingSubject,
    idempotent: true,
  }])

  const open = reduceStudioCommitGate(committed.state, { type: 'OPEN_COMMITTED_EVENT' })
  assert.deepEqual(open.effects, ['NAVIGATE_TO_COMMITTED_EVENT'])
  assert.equal('navigationConsumed' in open.state && open.state.navigationConsumed, true)
  assert.deepEqual(reduceStudioCommitGate(open.state, { type: 'OPEN_COMMITTED_EVENT' }).effects, [])
})

test('proven non-commit closes while unknown outcome retries same envelope within budget', () => {
  const detached = reduceStudioCommitGate(begin().state, { type: 'CLOSE' }).state
  const unknown = reduceStudioCommitGate(detached, {
    type: 'OUTCOME_UNKNOWN', envelope,
    failure: { code: 'COMMIT_OUTCOME_UNKNOWN', retryable: true },
  })
  const retry = reduceStudioCommitGate(unknown.state, {
    type: 'RETRY_RESOLUTION', envelope,
    attempt: { attemptId: 'resolution-1', fence: 1 },
  })
  assert.equal(retry.state.kind, 'DETACHED_RESOLVING')
  assert.deepEqual('envelope' in retry.state ? retry.state.envelope : null, envelope)
  assert.deepEqual(retry.effects, ['RESOLVE_OUTCOME_IDEMPOTENTLY'])

  const duplicateRetry = reduceStudioCommitGate(retry.state, {
    type: 'RETRY_RESOLUTION', envelope,
    attempt: { attemptId: 'resolution-2', fence: 2 },
  })
  assert.equal(duplicateRetry.state.kind, 'DETACHED_RESOLVING')
  assert.deepEqual(duplicateRetry.effects, [])

  const staleResult = reduceStudioCommitGate(retry.state, {
    type: 'RESOLUTION_RESULT', envelope,
    attempt: { attemptId: 'stale', fence: 0 },
    outcome: {
      kind: 'UNKNOWN', failure: { code: 'RESOLUTION_OUTCOME_UNKNOWN', retryable: true },
    },
  })
  assert.equal(staleResult.state.kind, 'DETACHED_RESOLVING')

  const correlatedUnknown = reduceStudioCommitGate(retry.state, {
    type: 'RESOLUTION_RESULT', envelope,
    attempt: { attemptId: 'resolution-1', fence: 1 },
    outcome: {
      kind: 'UNKNOWN', failure: { code: 'RESOLUTION_OUTCOME_UNKNOWN', retryable: true },
    },
  })
  assert.equal(correlatedUnknown.state.kind, 'DETACHED_COMMITTING')

  const reusedAttempt = reduceStudioCommitGate(correlatedUnknown.state, {
    type: 'RETRY_RESOLUTION', envelope,
    attempt: { attemptId: 'resolution-1', fence: 1 },
  })
  assert.equal(reusedAttempt.state.kind, 'DETACHED_COMMITTING')
  assert.deepEqual(reusedAttempt.effects, [])

  const reusedIdWithNextFence = reduceStudioCommitGate(correlatedUnknown.state, {
    type: 'RETRY_RESOLUTION', envelope,
    attempt: { attemptId: 'resolution-1', fence: 2 },
  })
  assert.equal(reusedIdWithNextFence.state.kind, 'DETACHED_COMMITTING')
  assert.deepEqual(reusedIdWithNextFence.effects, [])

  const lateResult = reduceStudioCommitGate(correlatedUnknown.state, {
    type: 'RESOLUTION_RESULT', envelope,
    attempt: { attemptId: 'resolution-1', fence: 1 },
    outcome: { kind: 'LOCAL_COMMIT', receipt: localReceipt() },
  })
  assert.equal(lateResult.state.kind, 'DETACHED_COMMITTING')
  assert.deepEqual(lateResult.effects, [])

  const nextRetry = reduceStudioCommitGate(correlatedUnknown.state, {
    type: 'RETRY_RESOLUTION', envelope,
    attempt: { attemptId: 'resolution-2', fence: 2 },
  })
  assert.equal(nextRetry.state.kind, 'DETACHED_RESOLVING')
  assert.deepEqual(nextRetry.effects, ['RESOLVE_OUTCOME_IDEMPOTENTLY'])
  assert.deepEqual(reduceStudioCommitGate(nextRetry.state, { type: 'CLOSE' }).effects,
    ['CONSUME_CLOSE_WITHOUT_CANCELLATION'])

  const blankDirectProof = reduceStudioCommitGate(detached, {
    type: 'PROVEN_NON_COMMIT', envelope, proofRef: '   ',
  })
  assert.equal(blankDirectProof.state.kind, 'DETACHED_COMMITTING')

  const closed = reduceStudioCommitGate(detached, {
    type: 'PROVEN_NON_COMMIT', envelope, proofRef: 'rollback-proof-1',
  })
  assert.deepEqual(closed.state, { kind: 'CLOSED' })
})

test('unknown outcome becomes terminal when non-retryable or budget is exhausted', () => {
  const detached = reduceStudioCommitGate(begin().state, { type: 'CLOSE' }).state
  const terminal = reduceStudioCommitGate(detached, {
    type: 'OUTCOME_UNKNOWN', envelope,
    failure: { code: 'COMMIT_OUTCOME_UNKNOWN', retryable: false },
  })
  assert.equal(terminal.state.kind, 'DETACHED_RESOLUTION_FAILED')
  assert.deepEqual(terminal.effects, [])

  const exhaustedState: StudioCommitGateState = {
    kind: 'DETACHED_COMMITTING', envelope,
    resolutionAttempt: envelope.maxResolutionAttempts,
    lastFailure: { code: 'RESOLUTION_OUTCOME_UNKNOWN', retryable: true },
    lastAttempt: { attemptId: 'resolution-2', fence: 2 },
  }
  const exhausted = reduceStudioCommitGate(exhaustedState, {
    type: 'RETRY_RESOLUTION', envelope,
    attempt: { attemptId: 'resolution-exhausted', fence: 3 },
  })
  assert.equal(exhausted.state.kind, 'DETACHED_RESOLUTION_FAILED')
  assert.deepEqual(exhausted.effects, [])
})

test('in-flight creation and closed choices expose disabled VoiceOver semantics', () => {
  assert.deepEqual(projectCreationSubmitAffordance(begin().state), {
    visible: true, isEnabled: false, semanticState: 'DISABLED',
    voiceOverState: 'DISABLED', disabledReason: 'IN_FLIGHT',
  })
  assert.deepEqual(projectStudioChoiceAffordance('CLOSED'), {
    visible: true, isEnabled: false, semanticState: 'DISABLED',
    voiceOverState: 'DISABLED', disabledReason: 'CLOSED_CHOICE',
  })
  assert.equal(projectStudioChoiceAffordance('OPEN').isEnabled, true)
})
