import { assign, setup } from 'xstate'

export type EventStatus = 'DRAFT' | 'POLLING' | 'COMPARING' | 'CONFIRMED' | 'ORGANIZING' | 'FINALIZED'
export type AllowedAction = 'EDIT' | 'RETRY_SYNC' | 'RESOLVE_CONFLICT' | 'OPEN_SETTINGS' | 'CONTINUE_MANUALLY' | null
export type ProjectionInput = {
  status: EventStatus
  role: 'ORGANIZER' | 'PARTICIPANT'
  pendingFacts: readonly ('LOCAL_MUTATION' | 'SYNC_CONFLICT')[]
  allowedAction: AllowedAction
  validation?: {
    code: 'INVALID_FIELD'
    field: 'TITLE' | 'DESCRIPTION' | 'TIME_SLOT'
    correction: 'FOCUS_FIELD'
  }
  cancellation?: 'CANCELLED'
  permission?: 'DENIED' | 'RESTRICTED'
  aiOutcome?: 'UNAVAILABLE' | 'REJECTED'
}
export type ProjectionOutput = {
  domainStatus: EventStatus
  titleKey: string
  statusKey: string | null
  detailKey: string | null
  primaryActionKey: string | null
  secondaryActionKey: string | null
  sharedConfirmation: boolean
}

const titleKeys: Record<EventStatus, string> = {
  DRAFT: 'event.state.draft',
  POLLING: 'event.state.polling',
  COMPARING: 'event.state.comparing',
  CONFIRMED: 'event.state.confirmed',
  ORGANIZING: 'event.state.organizing',
  FINALIZED: 'event.state.finalized',
}

export function projectProductLanguage(input: ProjectionInput): ProjectionOutput {
  const hasConflict = input.pendingFacts.includes('SYNC_CONFLICT')
  const pendingSync = input.pendingFacts.includes('LOCAL_MUTATION')
  let statusKey: string | null = null
  let detailKey: string | null = null
  let primaryActionKey: string | null = null
  let secondaryActionKey: string | null = null

  if (input.status === 'FINALIZED') {
    // Terminal projection always wins over stale caller actions.
  } else if (hasConflict) {
    statusKey = 'sync.conflict'
    detailKey = 'sync.conflict.event-details'
    primaryActionKey = input.allowedAction === 'RESOLVE_CONFLICT'
      ? 'sync.resolve'
      : input.allowedAction === 'RETRY_SYNC'
        ? 'sync.retry'
        : null
  } else if (input.permission) {
    statusKey = `permission.${input.permission.toLowerCase()}`
    detailKey = 'permission.impact.event-update'
    primaryActionKey = input.allowedAction === 'OPEN_SETTINGS' ? 'permission.open-settings' : null
  } else if (input.validation) {
    statusKey = 'validation.invalid-field'
    detailKey = `validation.field.${input.validation.field.toLowerCase().replace('_', '-')}`
    primaryActionKey = input.allowedAction === 'EDIT' && input.validation.correction === 'FOCUS_FIELD'
      ? 'validation.focus-field'
      : null
  } else if (input.cancellation) {
    statusKey = 'operation.cancelled'
  } else if (input.aiOutcome) {
    statusKey = `ai.${input.aiOutcome.toLowerCase()}`
    primaryActionKey = input.allowedAction === 'CONTINUE_MANUALLY' ? 'ai.continue-manually' : null
  } else if (pendingSync) {
    statusKey = 'sync.waiting'
    primaryActionKey = input.allowedAction === 'RETRY_SYNC' ? 'sync.retry' : null
  } else if (input.allowedAction === 'EDIT') {
    primaryActionKey = 'event.action.continue'
  }

  return {
    domainStatus: input.status,
    titleKey: titleKeys[input.status],
    statusKey,
    detailKey,
    primaryActionKey,
    secondaryActionKey,
    sharedConfirmation: !pendingSync && !hasConflict && !input.cancellation,
  }
}

export const productLanguageMachine = setup({
  types: {
    context: {} as { input: ProjectionInput; projection: ProjectionOutput },
    input: {} as ProjectionInput,
    events: {} as
      | { type: 'SYNC_SUCCEEDED' }
      | { type: 'SYNC_FAILED' }
      | { type: 'RETRY_SYNC' }
      | { type: 'RESOLVE_CONFLICT' },
  },
  guards: {
    isTerminal: ({ context }) => context.input.status === 'FINALIZED',
    hasConflict: ({ context }) => context.input.pendingFacts.includes('SYNC_CONFLICT'),
    hasPermissionBlock: ({ context }) => context.input.permission !== undefined,
    hasValidationError: ({ context }) => context.input.validation !== undefined,
    wasCancelled: ({ context }) => context.input.cancellation !== undefined,
    needsManualFallback: ({ context }) => context.input.aiOutcome !== undefined,
    hasPendingSync: ({ context }) => context.input.pendingFacts.includes('LOCAL_MUTATION'),
    canResolveConflict: ({ context }) => context.input.allowedAction === 'RESOLVE_CONFLICT',
    canRetrySync: ({ context }) => context.input.allowedAction === 'RETRY_SYNC',
  },
  actions: {
    markSynced: assign(({ context }) => {
      const input = {
        ...context.input,
        pendingFacts: context.input.pendingFacts.filter(fact => fact !== 'LOCAL_MUTATION'),
        allowedAction: null,
      }
      return { input, projection: projectProductLanguage(input) }
    }),
    markSyncFailed: assign(({ context }) => ({
      projection: {
        ...context.projection,
        statusKey: 'sync.failed',
        primaryActionKey: context.input.allowedAction === 'RETRY_SYNC' ? 'sync.retry' : null,
        sharedConfirmation: false,
      },
    })),
    markRetrying: assign(({ context }) => ({
      projection: { ...context.projection, statusKey: 'sync.waiting', primaryActionKey: 'sync.retry', sharedConfirmation: false },
    })),
    beginConflictResolution: assign(({ context }) => {
      const input = {
        ...context.input,
        pendingFacts: ['LOCAL_MUTATION'] as const,
      }
      return { input, projection: projectProductLanguage(input) }
    }),
  },
}).createMachine({
  id: 'productLanguage',
  context: ({ input }) => ({ input, projection: projectProductLanguage(input) }),
  initial: 'classify',
  states: {
    classify: {
      always: [
        { guard: 'isTerminal', target: 'terminal' },
        { guard: 'hasConflict', target: 'syncConflict' },
        { guard: 'hasPermissionBlock', target: 'permissionBlocked' },
        { guard: 'hasValidationError', target: 'validationError' },
        { guard: 'wasCancelled', target: 'cancelled' },
        { guard: 'needsManualFallback', target: 'manualFallback' },
        { guard: 'hasPendingSync', target: 'pendingSync' },
        { target: 'ready' },
      ],
    },
    ready: {},
    pendingSync: { on: { SYNC_SUCCEEDED: { target: 'ready', actions: 'markSynced' }, SYNC_FAILED: { target: 'syncFailed', actions: 'markSyncFailed' } } },
    syncFailed: { on: { RETRY_SYNC: { guard: 'canRetrySync', target: 'pendingSync', actions: 'markRetrying' } } },
    syncConflict: {
      on: {
        RESOLVE_CONFLICT: { guard: 'canResolveConflict', target: 'pendingSync', actions: 'beginConflictResolution' },
        RETRY_SYNC: { guard: 'canRetrySync', target: 'pendingSync', actions: 'beginConflictResolution' },
      },
    },
    validationError: {},
    cancelled: {},
    permissionBlocked: {},
    manualFallback: {},
    terminal: { type: 'final' },
  },
})
