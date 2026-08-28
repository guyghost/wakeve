import assert from 'node:assert/strict'
import test from 'node:test'

import {
  buildSlotIdentityIndex,
  lookupConfirmationPhysicalSlot,
  lookupSyncLogicalSlot,
  migrateSlotStorageIdentitiesAtomically,
  physicalSlotIdFor,
  projectPendingPhysicalSlots,
  slotIdentityConsumersEnabled,
  type MigratableSlotStorageIdentity,
  type SlotStorageReference,
  type SlotStorageIdentity,
} from './time-slot-storage-identity.model.ts'

const record = (eventId: string, logicalSlotId: string): SlotStorageIdentity => ({
  eventId,
  logicalSlotId,
  physicalSlotId: physicalSlotIdFor({ eventId, logicalSlotId }),
})

test('physical slot identity is deterministic and namespaced by event', () => {
  const first = record('event-a', 'slot-1')
  const same = record('event-a', 'slot-1')
  const anotherEvent = record('event-b', 'slot-1')
  assert.equal(first.physicalSlotId, same.physicalSlotId)
  assert.notEqual(first.physicalSlotId, anotherEvent.physicalSlotId)
  assert.notEqual(
    physicalSlotIdFor({ eventId: 'a', logicalSlotId: 'b|c' }),
    physicalSlotIdFor({ eventId: 'a|b', logicalSlotId: 'c' }),
  )
  assert.throws(() => physicalSlotIdFor({ eventId: 'event-a', logicalSlotId: '\ud800' }))
})

test('confirmation and sync use the same lossless bidirectional mapping', () => {
  const stored = [record('event-a', 'slot-1'), record('event-a', 'slot-2')]
  const built = buildSlotIdentityIndex(stored)
  assert.equal(built.kind, 'READY')
  if (built.kind !== 'READY') return

  assert.deepEqual(lookupConfirmationPhysicalSlot(built, {
    eventId: 'event-a', logicalSlotId: 'slot-2',
  }), { kind: 'FOUND', identity: stored[1] })
  assert.deepEqual(lookupSyncLogicalSlot(built, stored[0]!.physicalSlotId), {
    kind: 'FOUND', identity: stored[0],
  })
})

test('non-deterministic or duplicate mapping fails instead of overwriting', () => {
  const correct = record('event-a', 'slot-1')
  assert.equal(buildSlotIdentityIndex([
    { ...correct, physicalSlotId: 'legacy-unscoped-slot-1' },
  ]).kind, 'INCONSISTENT')
  assert.deepEqual(buildSlotIdentityIndex([correct, correct]), {
    kind: 'INCONSISTENT', code: 'DUPLICATE_LOGICAL_MAPPING', record: correct,
  })
})

test('missing pending slot metadata stays visible as INCONSISTENT', () => {
  const correct = record('event-a', 'slot-1')
  const built = buildSlotIdentityIndex([correct])
  assert.equal(built.kind, 'READY')
  if (built.kind !== 'READY') return

  const projection = projectPendingPhysicalSlots(built, [correct.physicalSlotId, 'missing-physical'])
  assert.equal(projection.length, 2)
  assert.equal(projection[0]?.kind, 'FOUND')
  assert.deepEqual(projection[1], {
    kind: 'INCONSISTENT', code: 'MISSING_PHYSICAL_MAPPING', requested: 'missing-physical',
  })
})

test('one atomic migration rewrites mixed legacy/v1 ids and every reference family', () => {
  const slots: MigratableSlotStorageIdentity[] = [
    { eventId: 'event-a', logicalSlotId: 'slot-1', currentPhysicalSlotId: 'legacy-slot-1' },
    {
      eventId: 'event-a', logicalSlotId: 'slot-2',
      currentPhysicalSlotId: physicalSlotIdFor({ eventId: 'event-a', logicalSlotId: 'slot-2' }),
    },
  ]
  const references: SlotStorageReference[] = [
    { referenceId: 'vote-1', kind: 'VOTE', physicalSlotId: 'legacy-slot-1' },
    { referenceId: 'receipt-1', kind: 'RECEIPT', physicalSlotId: 'legacy-slot-1' },
    { referenceId: 'sync-1', kind: 'SYNC_METADATA', physicalSlotId: slots[1]!.currentPhysicalSlotId },
  ]

  const result = migrateSlotStorageIdentitiesAtomically(slots, references)
  assert.equal(result.kind, 'COMMITTED')
  assert.equal(slotIdentityConsumersEnabled(result), true)
  if (result.kind !== 'COMMITTED') return
  assert.deepEqual(result.slots, [record('event-a', 'slot-1'), record('event-a', 'slot-2')])
  assert.equal(result.references[0]?.physicalSlotId, result.slots[0]?.physicalSlotId)
  assert.equal(result.references[1]?.physicalSlotId, result.slots[0]?.physicalSlotId)
  assert.equal(result.references[2]?.physicalSlotId, result.slots[1]?.physicalSlotId)
})

test('missing migration reference rolls back every row with no partial rewrite', () => {
  const slots: MigratableSlotStorageIdentity[] = [
    { eventId: 'event-a', logicalSlotId: 'slot-1', currentPhysicalSlotId: 'legacy-slot-1' },
  ]
  const references: SlotStorageReference[] = [
    { referenceId: 'vote-1', kind: 'VOTE', physicalSlotId: 'missing-slot' },
  ]
  const result = migrateSlotStorageIdentitiesAtomically(slots, references)
  assert.deepEqual(result, {
    kind: 'ROLLED_BACK', status: 'ROLLED_BACK', code: 'MISSING_SLOT_REFERENCE',
    slots, references,
  })
  assert.equal(slotIdentityConsumersEnabled(result), false)
})

test('source and target collisions abort the whole migration', () => {
  const sourceCollision: MigratableSlotStorageIdentity[] = [
    { eventId: 'event-a', logicalSlotId: 'slot-1', currentPhysicalSlotId: 'legacy-shared' },
    { eventId: 'event-b', logicalSlotId: 'slot-2', currentPhysicalSlotId: 'legacy-shared' },
  ]
  const sourceResult = migrateSlotStorageIdentitiesAtomically(sourceCollision, [])
  assert.equal(sourceResult.kind, 'ROLLED_BACK')
  assert.equal('code' in sourceResult && sourceResult.code, 'SOURCE_ID_COLLISION')

  const targetCollision: MigratableSlotStorageIdentity[] = [
    { eventId: 'event-a', logicalSlotId: 'slot-1', currentPhysicalSlotId: 'legacy-a' },
    { eventId: 'event-a', logicalSlotId: 'slot-1', currentPhysicalSlotId: 'legacy-b' },
  ]
  const result = migrateSlotStorageIdentitiesAtomically(targetCollision, [])
  assert.equal(result.kind, 'ROLLED_BACK')
  assert.equal('code' in result && result.code, 'TARGET_ID_COLLISION')
  assert.equal(slotIdentityConsumersEnabled(result), false)
})
