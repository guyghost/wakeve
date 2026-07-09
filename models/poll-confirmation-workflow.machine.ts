import { assign, setup } from 'xstate'

export type EventStatus = 'DRAFT' | 'POLLING' | 'CONFIRMED' | 'ORGANIZING' | 'FINALIZED'
export type DecisionSyncStatus = 'localPending' | 'serverAcknowledged'
export type EffectDispatchStatus = 'queued' | 'partiallyProcessed' | 'terminalWithFailures'
export type ConfirmationFailureCode =
  | 'EVENT_NOT_FOUND' | 'NOT_ORGANIZER' | 'INVALID_EVENT_STATUS' | 'NO_VOTES'
  | 'SLOT_NOT_FOUND' | 'SLOT_NOT_CONFIRMABLE'
  | 'ALREADY_CONFIRMED_DIFFERENT_SLOT' | 'LOCAL_PERSISTENCE_FAILED'
  | 'REPOSITORY_UNAVAILABLE'

export interface ConfirmationEligibility {
  repositoryAvailable: boolean
  eventExists: boolean
  actorIsOrganizer: boolean
  eventStatus: EventStatus
  pollHasVotes: boolean
  validSlotIds: readonly string[]
  confirmedSlotId: string | null
  votingDeadline: string
  now: string
}

export interface PollConfirmationInput {
  eventId: string
  actorId: string
  eligibility: ConfirmationEligibility
}

export interface ConfirmationEffectOutbox {
  domainEventId: string
  /** The single local domain envelope. Provider and calendar rows are backend-owned. */
  effectKey: string
}

export interface ConfirmationReceipt {
  receiptId: string
  operationId: string
  eventId: string
  slotId: string
  decisionSyncStatus: DecisionSyncStatus
  effectDispatchStatus: EffectDispatchStatus
  effectOutbox: ConfirmationEffectOutbox
}

export type RepositoryProjection =
  | { kind: 'reviewing'; eventId: string }
  /** A historical confirmation that predates the durable outbox contract. */
  | { kind: 'legacyApplied'; eventId: string; slotId: string; receiptId: string }
  /** Historical data that cannot safely be represented as a confirmation. */
  | { kind: 'quarantined'; eventId: string; reason: string }
  | {
      kind: 'confirmed'; eventId: string; slotId: string; receiptId: string
      decisionSyncStatus: DecisionSyncStatus
      effectDispatchStatus: EffectDispatchStatus
    }

export interface ConfirmationFailure {
  code: ConfirmationFailureCode
  retryable: boolean
}

export type WorkflowEffect =
  | 'presentConfirmPrompt' | 'dismissConfirmPrompt' | 'dispatchConfirmationCommand'
  | 'successFeedback' | 'navigationEligible'

interface PollConfirmationContext {
  eventId: string
  actorId: string
  eligibility: ConfirmationEligibility
  selectedSlotId: string | null
  operationId: string | null
  confirmationReceiptId: string | null
  decisionSyncStatus: DecisionSyncStatus | null
  effectDispatchStatus: EffectDispatchStatus | null
  diagnosticReason: string | null
  failure: ConfirmationFailure | null
  effects: WorkflowEffect[]
}

export type PollConfirmationEvent =
  | { type: 'OPEN_CONFIRM_PROMPT'; slotId: string }
  | { type: 'CANCEL_CONFIRMATION' }
  | { type: 'SUBMIT_CONFIRMATION'; operationId: string }
  | { type: 'CONFIRMATION_COMMITTED'; receipt: ConfirmationReceipt }
  | {
      type: 'CONFIRMATION_READ_ONLY'
      operationId: string
      projection: Extract<RepositoryProjection, { kind: 'legacyApplied' | 'quarantined' }>
    }
  | { type: 'CONFIRMATION_FAILED'; operationId: string; error: ConfirmationFailure }
  | { type: 'CONFIRMATION_CONFLICT'; operationId: string; code: 'ALREADY_CONFIRMED_DIFFERENT_SLOT' }
  | { type: 'RETRY_CONFIRMATION' }
  | { type: 'DISMISS_FAILURE' }
  | { type: 'SYNC_COMPLETED'; receiptId: string }
  | { type: 'SYNC_FAILED'; receiptId: string; error: string }
  | { type: 'REHYDRATE'; projection: RepositoryProjection }

/** Deadline affects vote mutation only; it is intentionally not a confirmation guard. */
export const canMutateVote = (eligibility: ConfirmationEligibility): boolean =>
  Date.parse(eligibility.now) < Date.parse(eligibility.votingDeadline)

const validationFailure = (context: PollConfirmationContext): ConfirmationFailure => {
  const selected = context.selectedSlotId
  if (!context.eligibility.repositoryAvailable) return { code: 'REPOSITORY_UNAVAILABLE', retryable: true }
  if (!context.eligibility.eventExists) return { code: 'EVENT_NOT_FOUND', retryable: false }
  if (!context.eligibility.actorIsOrganizer) return { code: 'NOT_ORGANIZER', retryable: false }
  if (context.eligibility.eventStatus === 'CONFIRMED' &&
      context.eligibility.confirmedSlotId &&
      context.eligibility.confirmedSlotId !== selected) {
    return { code: 'ALREADY_CONFIRMED_DIFFERENT_SLOT', retryable: false }
  }
  if (context.eligibility.eventStatus !== 'POLLING' && !(
    context.eligibility.eventStatus === 'CONFIRMED' &&
    context.eligibility.confirmedSlotId === selected
  )) return { code: 'INVALID_EVENT_STATUS', retryable: false }
  const replayingConfirmedSlot = context.eligibility.eventStatus === 'CONFIRMED' &&
    context.eligibility.confirmedSlotId === selected
  if (!replayingConfirmedSlot && !context.eligibility.pollHasVotes) {
    return { code: 'NO_VOTES', retryable: false }
  }
  if (!selected || !context.eligibility.validSlotIds.includes(selected)) {
    return { code: 'SLOT_NOT_FOUND', retryable: false }
  }
  if (context.eligibility.confirmedSlotId && context.eligibility.confirmedSlotId !== selected) {
    return { code: 'ALREADY_CONFIRMED_DIFFERENT_SLOT', retryable: false }
  }
  return { code: 'SLOT_NOT_CONFIRMABLE', retryable: false }
}

export const confirmationEffectKeys = (eventId: string, slotId: string) => {
  const domainEventId = `poll-date-confirmed:${eventId}:${slotId}:v1`
  return {
    domainEventId,
    effectKey: `${domainEventId}:confirmation`,
  }
}

/** Backend projection identities. These are derivations, never local outbox rows. */
export const recipientKey = (
  effectKey: string,
  participantId: string,
  notificationChannel: string,
): string => `${effectKey}:${participantId}:${notificationChannel}`

export const deliveryKey = (
  recipientKey: string,
  installationId: string,
  provider: string,
): string => `${recipientKey}:${installationId}:${provider}`

export const calendarArtifactKey = (
  effectKey: string,
  participantId: string,
  calendarProvider: string,
): string => `${effectKey}:${participantId}:${calendarProvider}`

const receiptMatchesContext = (
  context: PollConfirmationContext,
  receipt: ConfirmationReceipt,
): boolean => {
  if (receipt.operationId !== context.operationId ||
      receipt.eventId !== context.eventId ||
      receipt.slotId !== context.selectedSlotId) return false
  const expected = confirmationEffectKeys(receipt.eventId, receipt.slotId)
  return receipt.effectOutbox.domainEventId === expected.domainEventId &&
    receipt.effectOutbox.effectKey === expected.effectKey
}

export const pollConfirmationInvariants = [
  'The deterministic model is the only authority for the date decision.',
  'Cancellation has no business, persistence, success, or navigation effect.',
  'At most one operation is effective in confirming; retry reuses its operation id.',
  'Confirmed requires an atomic receipt for event, exact slot, local effect outbox, and sync metadata.',
  'A failed local commit produces no receipt, success feedback, or navigation.',
  'A different slot after confirmation is a conflict, never an overwrite.',
  'Decision sync status is independent from effect dispatch status.',
  'confirmed.synced means server business acknowledgement, never APNs or calendar acceptance.',
  'Exactly one local confirmation effect envelope exists; it has one domainEventId and one effectKey.',
  'Notification recipients, provider deliveries, and calendar artifacts are backend projections, not local rows.',
  'Rehydration never redispatches confirmation or replays navigation.',
  'Legacy classifications are read-only and never create pending sync or delivery work.',
  'Voting closes at the deadline; organizer confirmation remains eligible.',
] as const

export const pollConfirmationWorkflowMachine = setup({
  types: {
    context: {} as PollConfirmationContext,
    events: {} as PollConfirmationEvent,
    input: {} as PollConfirmationInput,
  },
  guards: {
    canStageSlot: ({ context, event }) =>
      event.type === 'OPEN_CONFIRM_PROMPT' && context.eligibility.validSlotIds.includes(event.slotId),
    canSubmitConfirmation: ({ context }) => {
      const failure = validationFailure(context)
      return failure.code === 'SLOT_NOT_CONFIRMABLE' &&
        context.selectedSlotId !== null && context.operationId === null
    },
    receiptMatchesOperation: ({ context, event }) => event.type === 'CONFIRMATION_COMMITTED' &&
      receiptMatchesContext(context, event.receipt),
    readOnlyMatchesOperation: ({ context, event }) => event.type === 'CONFIRMATION_READ_ONLY' &&
      event.operationId === context.operationId && event.projection.eventId === context.eventId,
    readOnlyLegacyAppliedMatchesOperation: ({ context, event }) =>
      event.type === 'CONFIRMATION_READ_ONLY' &&
      event.operationId === context.operationId &&
      event.projection.eventId === context.eventId &&
      event.projection.kind === 'legacyApplied',
    receiptMatchesAndIsServerAcknowledged: ({ context, event }) =>
      event.type === 'CONFIRMATION_COMMITTED' &&
      receiptMatchesContext(context, event.receipt) &&
      event.receipt.decisionSyncStatus === 'serverAcknowledged',
    failureMatchesOperation: ({ context, event }) => event.type === 'CONFIRMATION_FAILED' &&
      event.operationId === context.operationId,
    conflictMatchesOperation: ({ context, event }) => event.type === 'CONFIRMATION_CONFLICT' &&
      event.operationId === context.operationId,
    isRetryable: ({ context }) => context.failure?.retryable === true,
    receiptMatchesConfirmation: ({ context, event }) =>
      (event.type === 'SYNC_COMPLETED' || event.type === 'SYNC_FAILED') &&
      event.receiptId === context.confirmationReceiptId,
    rehydratesConfirmedSynced: ({ context, event }) => event.type === 'REHYDRATE' &&
      event.projection.eventId === context.eventId && event.projection.kind === 'confirmed' &&
      event.projection.decisionSyncStatus === 'serverAcknowledged',
    rehydratesConfirmedPending: ({ context, event }) => event.type === 'REHYDRATE' &&
      event.projection.eventId === context.eventId && event.projection.kind === 'confirmed' &&
      event.projection.decisionSyncStatus === 'localPending',
    rehydratesReviewing: ({ context, event }) => event.type === 'REHYDRATE' &&
      event.projection.eventId === context.eventId && event.projection.kind === 'reviewing',
    rehydratesLegacyApplied: ({ context, event }) => event.type === 'REHYDRATE' &&
      event.projection.eventId === context.eventId && event.projection.kind === 'legacyApplied',
    rehydratesQuarantined: ({ context, event }) => event.type === 'REHYDRATE' &&
      event.projection.eventId === context.eventId && event.projection.kind === 'quarantined',
  },
  actions: {
    stageSlot: assign({ selectedSlotId: ({ event }) => event.type === 'OPEN_CONFIRM_PROMPT' ? event.slotId : null }),
    clearAttempt: assign({ selectedSlotId: null, operationId: null, failure: null }),
    captureOperation: assign({
      operationId: ({ event }) => event.type === 'SUBMIT_CONFIRMATION' ? event.operationId : null,
      failure: null,
    }),
    captureValidationFailure: assign({ failure: ({ context }) => validationFailure(context) }),
    captureFailure: assign({ failure: ({ event }) => event.type === 'CONFIRMATION_FAILED' ? event.error : null }),
    captureConflict: assign({
      failure: ({ event }) => event.type === 'CONFIRMATION_CONFLICT'
        ? { code: event.code, retryable: false }
        : null,
    }),
    captureReceipt: assign({
      confirmationReceiptId: ({ event }) => event.type === 'CONFIRMATION_COMMITTED' ? event.receipt.receiptId : null,
      selectedSlotId: ({ event }) => event.type === 'CONFIRMATION_COMMITTED' ? event.receipt.slotId : null,
      decisionSyncStatus: ({ event }) => event.type === 'CONFIRMATION_COMMITTED' ? event.receipt.decisionSyncStatus : null,
      effectDispatchStatus: ({ event }) => event.type === 'CONFIRMATION_COMMITTED' ? event.receipt.effectDispatchStatus : null,
      failure: null,
    }),
    captureProjection: assign({
      selectedSlotId: ({ event }) => event.type === 'REHYDRATE' && event.projection.kind === 'confirmed' ? event.projection.slotId : null,
      confirmationReceiptId: ({ event }) => event.type === 'REHYDRATE' && event.projection.kind === 'confirmed' ? event.projection.receiptId : null,
      decisionSyncStatus: ({ event }) => event.type === 'REHYDRATE' && event.projection.kind === 'confirmed' ? event.projection.decisionSyncStatus : null,
      effectDispatchStatus: ({ event }) => event.type === 'REHYDRATE' && event.projection.kind === 'confirmed' ? event.projection.effectDispatchStatus : null,
      operationId: null, diagnosticReason: null, failure: null,
    }),
    captureLegacyAppliedProjection: assign({
      selectedSlotId: ({ event }) => event.type === 'REHYDRATE' && event.projection.kind === 'legacyApplied' ? event.projection.slotId : null,
      confirmationReceiptId: ({ event }) => event.type === 'REHYDRATE' && event.projection.kind === 'legacyApplied' ? event.projection.receiptId : null,
      decisionSyncStatus: null,
      effectDispatchStatus: null,
      operationId: null,
      diagnosticReason: null,
      failure: null,
    }),
    captureQuarantinedProjection: assign({
      selectedSlotId: null,
      confirmationReceiptId: null,
      decisionSyncStatus: null,
      effectDispatchStatus: null,
      operationId: null,
      diagnosticReason: ({ event }) => event.type === 'REHYDRATE' && event.projection.kind === 'quarantined' ? event.projection.reason : null,
      failure: null,
    }),
    captureReadOnlyLegacyApplied: assign({
      selectedSlotId: ({ event }) => event.type === 'CONFIRMATION_READ_ONLY' && event.projection.kind === 'legacyApplied'
        ? event.projection.slotId : null,
      confirmationReceiptId: ({ event }) => event.type === 'CONFIRMATION_READ_ONLY' && event.projection.kind === 'legacyApplied'
        ? event.projection.receiptId : null,
      decisionSyncStatus: null,
      effectDispatchStatus: null,
      operationId: null,
      diagnosticReason: null,
      failure: null,
    }),
    captureReadOnlyQuarantined: assign({
      selectedSlotId: null,
      confirmationReceiptId: null,
      decisionSyncStatus: null,
      effectDispatchStatus: null,
      operationId: null,
      diagnosticReason: ({ event }) => event.type === 'CONFIRMATION_READ_ONLY' && event.projection.kind === 'quarantined'
        ? event.projection.reason : null,
      failure: null,
    }),
    markServerAcknowledged: assign({ decisionSyncStatus: 'serverAcknowledged' }),
    presentPrompt: assign({ effects: ({ context }) => [...context.effects, 'presentConfirmPrompt'] }),
    dismissPrompt: assign({ effects: ({ context }) => [...context.effects, 'dismissConfirmPrompt'] }),
    dispatchCommand: assign({ effects: ({ context }) => [...context.effects, 'dispatchConfirmationCommand'] }),
    successFeedback: assign({ effects: ({ context }) => [...context.effects, 'successFeedback'] }),
    navigationEligible: assign({ effects: ({ context }) => [...context.effects, 'navigationEligible'] }),
  },
}).createMachine({
  id: 'pollConfirmationWorkflow',
  initial: 'reviewingResults',
  context: ({ input }) => ({
    eventId: input.eventId, actorId: input.actorId, eligibility: input.eligibility,
    selectedSlotId: null, operationId: null, confirmationReceiptId: null,
    decisionSyncStatus: null, effectDispatchStatus: null, diagnosticReason: null, failure: null, effects: [],
  }),
  on: {
    REHYDRATE: [
      { guard: 'rehydratesConfirmedSynced', target: '.confirmed.synced', actions: 'captureProjection' },
      { guard: 'rehydratesConfirmedPending', target: '.confirmed.pendingSync', actions: 'captureProjection' },
      { guard: 'rehydratesLegacyApplied', target: '.confirmed.legacyApplied', actions: 'captureLegacyAppliedProjection' },
      { guard: 'rehydratesQuarantined', target: '.quarantined', actions: 'captureQuarantinedProjection' },
      { guard: 'rehydratesReviewing', target: '.reviewingResults', actions: 'captureProjection' },
    ],
  },
  states: {
    reviewingResults: { on: { OPEN_CONFIRM_PROMPT: {
      guard: 'canStageSlot', target: 'confirmPrompt', actions: ['stageSlot', 'presentPrompt'],
    } } },
    confirmPrompt: { on: {
      CANCEL_CONFIRMATION: { target: 'reviewingResults', actions: ['clearAttempt', 'dismissPrompt'] },
      SUBMIT_CONFIRMATION: [
        { guard: 'canSubmitConfirmation', target: 'confirming', actions: ['captureOperation', 'dispatchCommand'] },
        { target: 'failed', actions: ['captureOperation', 'captureValidationFailure'] },
      ],
    } },
    confirming: { on: {
      SUBMIT_CONFIRMATION: {},
      CONFIRMATION_READ_ONLY: [
        { guard: 'readOnlyLegacyAppliedMatchesOperation', target: 'confirmed.legacyApplied',
          actions: 'captureReadOnlyLegacyApplied' },
        { guard: 'readOnlyMatchesOperation', target: 'quarantined', actions: 'captureReadOnlyQuarantined' },
      ],
      CONFIRMATION_COMMITTED: [
        { guard: 'receiptMatchesAndIsServerAcknowledged', target: 'confirmed.synced',
          actions: ['captureReceipt', 'successFeedback', 'navigationEligible'],
          reenter: false },
        { guard: 'receiptMatchesOperation', target: 'confirmed.pendingSync',
          actions: ['captureReceipt', 'successFeedback', 'navigationEligible'] },
      ],
      CONFIRMATION_FAILED: { guard: 'failureMatchesOperation', target: 'failed', actions: 'captureFailure' },
      CONFIRMATION_CONFLICT: { guard: 'conflictMatchesOperation', target: 'failed', actions: 'captureConflict' },
    } },
    failed: { on: {
      RETRY_CONFIRMATION: { guard: 'isRetryable', target: 'confirming', actions: ['captureFailure', 'dispatchCommand'] },
      DISMISS_FAILURE: { target: 'reviewingResults', actions: 'clearAttempt' },
    } },
    confirmed: { initial: 'pendingSync', states: {
      pendingSync: { on: {
        SYNC_COMPLETED: { guard: 'receiptMatchesConfirmation', target: 'synced', actions: 'markServerAcknowledged' },
        SYNC_FAILED: { guard: 'receiptMatchesConfirmation' },
      } },
      synced: {},
      legacyApplied: {},
    } },
    quarantined: {},
  },
})
