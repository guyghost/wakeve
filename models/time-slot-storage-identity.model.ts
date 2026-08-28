export interface LogicalSlotIdentity {
  eventId: string
  logicalSlotId: string
}

export interface SlotStorageIdentity extends LogicalSlotIdentity {
  physicalSlotId: string
}

export type SlotIdentityIndexFailureCode =
  | 'INVALID_IDENTIFIER'
  | 'NON_DETERMINISTIC_PHYSICAL_ID'
  | 'DUPLICATE_LOGICAL_MAPPING'
  | 'PHYSICAL_ID_COLLISION'

export type SlotIdentityIndexResult =
  | { kind: 'READY'; logicalToPhysical: ReadonlyMap<string, string>; physicalToLogical: ReadonlyMap<string, LogicalSlotIdentity> }
  | { kind: 'INCONSISTENT'; code: SlotIdentityIndexFailureCode; record: SlotStorageIdentity }

export type SlotLookupResult =
  | { kind: 'FOUND'; identity: SlotStorageIdentity }
  | {
      kind: 'INCONSISTENT'
      code: 'INVALID_LOGICAL_IDENTITY' | 'MISSING_LOGICAL_MAPPING' | 'MISSING_PHYSICAL_MAPPING'
      requested: string | LogicalSlotIdentity
    }

export interface MigratableSlotStorageIdentity extends LogicalSlotIdentity {
  currentPhysicalSlotId: string
}

export type SlotReferenceKind = 'VOTE' | 'RECEIPT' | 'SYNC_METADATA'

export interface SlotStorageReference {
  referenceId: string
  kind: SlotReferenceKind
  physicalSlotId: string
}

export type SlotIdentityMigrationResult =
  | {
      kind: 'COMMITTED'
      status: 'MIGRATION_COMPLETE'
      slots: readonly SlotStorageIdentity[]
      references: readonly SlotStorageReference[]
    }
  | {
      kind: 'ROLLED_BACK'
      status: 'ROLLED_BACK'
      code: 'INVALID_IDENTIFIER' | 'SOURCE_ID_COLLISION' |
        'TARGET_ID_COLLISION' | 'MISSING_SLOT_REFERENCE'
      slots: readonly MigratableSlotStorageIdentity[]
      references: readonly SlotStorageReference[]
    }

const utf8 = (value: string): Uint8Array => new TextEncoder().encode(value)
const hex = (value: string): string =>
  [...utf8(value)].map((byte) => byte.toString(16).padStart(2, '0')).join('')
const field = (value: string): string => `${utf8(value).length}:${hex(value)}`
const unicodeScalarString = (value: string): boolean => {
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

const validIdentifier = (value: string): boolean =>
  value.trim().length > 0 && value.trim() === value && unicodeScalarString(value)

/** Injective, locale-free physical identity; identical logical ids in different events never collide. */
export const physicalSlotIdFor = ({ eventId, logicalSlotId }: LogicalSlotIdentity): string => {
  if (!validIdentifier(eventId) || !validIdentifier(logicalSlotId)) {
    throw new TypeError('INVALID_IDENTIFIER')
  }
  return `slot:v1|${field(eventId)}|${field(logicalSlotId)}`
}

const logicalKey = (identity: LogicalSlotIdentity): string =>
  physicalSlotIdFor(identity)

export const buildSlotIdentityIndex = (
  records: readonly SlotStorageIdentity[],
): SlotIdentityIndexResult => {
  const logicalToPhysical = new Map<string, string>()
  const physicalToLogical = new Map<string, LogicalSlotIdentity>()

  for (const record of records) {
    if (!validIdentifier(record.eventId) || !validIdentifier(record.logicalSlotId) ||
        !validIdentifier(record.physicalSlotId)) {
      return { kind: 'INCONSISTENT', code: 'INVALID_IDENTIFIER', record }
    }
    const expectedPhysical = physicalSlotIdFor(record)
    if (record.physicalSlotId !== expectedPhysical) {
      return { kind: 'INCONSISTENT', code: 'NON_DETERMINISTIC_PHYSICAL_ID', record }
    }
    const key = logicalKey(record)
    if (logicalToPhysical.has(key)) {
      return { kind: 'INCONSISTENT', code: 'DUPLICATE_LOGICAL_MAPPING', record }
    }
    if (physicalToLogical.has(record.physicalSlotId)) {
      return { kind: 'INCONSISTENT', code: 'PHYSICAL_ID_COLLISION', record }
    }
    logicalToPhysical.set(key, record.physicalSlotId)
    physicalToLogical.set(record.physicalSlotId, {
      eventId: record.eventId,
      logicalSlotId: record.logicalSlotId,
    })
  }

  return { kind: 'READY', logicalToPhysical, physicalToLogical }
}

export const lookupConfirmationPhysicalSlot = (
  index: Extract<SlotIdentityIndexResult, { kind: 'READY' }>,
  logical: LogicalSlotIdentity,
): SlotLookupResult => {
  if (!validIdentifier(logical.eventId) || !validIdentifier(logical.logicalSlotId)) {
    return { kind: 'INCONSISTENT', code: 'INVALID_LOGICAL_IDENTITY', requested: logical }
  }
  const physicalSlotId = index.logicalToPhysical.get(logicalKey(logical))
  return physicalSlotId === undefined
    ? { kind: 'INCONSISTENT', code: 'MISSING_LOGICAL_MAPPING', requested: logical }
    : { kind: 'FOUND', identity: { ...logical, physicalSlotId } }
}

export const lookupSyncLogicalSlot = (
  index: Extract<SlotIdentityIndexResult, { kind: 'READY' }>,
  physicalSlotId: string,
): SlotLookupResult => {
  const logical = index.physicalToLogical.get(physicalSlotId)
  return logical === undefined
    ? { kind: 'INCONSISTENT', code: 'MISSING_PHYSICAL_MAPPING', requested: physicalSlotId }
    : { kind: 'FOUND', identity: { ...logical, physicalSlotId } }
}

/** One projection per pending row: unresolved metadata remains visible as INCONSISTENT. */
export const projectPendingPhysicalSlots = (
  index: Extract<SlotIdentityIndexResult, { kind: 'READY' }>,
  physicalSlotIds: readonly string[],
): SlotLookupResult[] => physicalSlotIds.map((physicalSlotId) =>
  lookupSyncLogicalSlot(index, physicalSlotId))

/**
 * Pure representation of one storage transaction. No migrated row escapes unless every slot and
 * every vote/receipt/sync reference can be rewritten to the event-scoped slot:v1 identity.
 */
export const migrateSlotStorageIdentitiesAtomically = (
  slots: readonly MigratableSlotStorageIdentity[],
  references: readonly SlotStorageReference[],
): SlotIdentityMigrationResult => {
  const rollback = (
    code: Extract<SlotIdentityMigrationResult, { kind: 'ROLLED_BACK' }>['code'],
  ): SlotIdentityMigrationResult => ({
    kind: 'ROLLED_BACK', status: 'ROLLED_BACK', code, slots, references,
  })
  const sourceToTarget = new Map<string, string>()
  const targets = new Set<string>()

  for (const slot of slots) {
    if (!validIdentifier(slot.eventId) || !validIdentifier(slot.logicalSlotId) ||
        !validIdentifier(slot.currentPhysicalSlotId)) return rollback('INVALID_IDENTIFIER')
    if (sourceToTarget.has(slot.currentPhysicalSlotId)) return rollback('SOURCE_ID_COLLISION')
    const target = physicalSlotIdFor(slot)
    if (targets.has(target)) return rollback('TARGET_ID_COLLISION')
    sourceToTarget.set(slot.currentPhysicalSlotId, target)
    targets.add(target)
  }

  for (const reference of references) {
    if (!validIdentifier(reference.referenceId) || !validIdentifier(reference.physicalSlotId)) {
      return rollback('INVALID_IDENTIFIER')
    }
    if (!sourceToTarget.has(reference.physicalSlotId)) return rollback('MISSING_SLOT_REFERENCE')
  }

  return {
    kind: 'COMMITTED',
    status: 'MIGRATION_COMPLETE',
    slots: slots.map((slot) => ({
      eventId: slot.eventId,
      logicalSlotId: slot.logicalSlotId,
      physicalSlotId: sourceToTarget.get(slot.currentPhysicalSlotId)!,
    })),
    references: references.map((reference) => ({
      ...reference,
      physicalSlotId: sourceToTarget.get(reference.physicalSlotId)!,
    })),
  }
}

export const slotIdentityConsumersEnabled = (result: SlotIdentityMigrationResult): boolean =>
  result.kind === 'COMMITTED' && result.status === 'MIGRATION_COMPLETE'

export const timeSlotStorageIdentityInvariants = [
  'Physical slot identity is the injective event-scoped v1 encoding of eventId and logicalSlotId.',
  'Confirmation lookup is logical-to-physical and sync lookup is physical-to-logical through one validated bidirectional index.',
  'Duplicate, divergent, missing, or colliding mappings are explicit INCONSISTENT results; no slot is silently dropped.',
  'Legacy or mixed slot ids migrate in one transaction with vote, receipt, and sync references; any collision or missing reference rolls the entire migration back.',
  'Confirmation and sync consumers remain disabled until the durable migration status is MIGRATION_COMPLETE.',
] as const
