import { assign, setup } from 'xstate'

export type EventStatus = 'DRAFT' | 'POLLING' | 'COMPARING' | 'CONFIRMED' | 'ORGANIZING' | 'FINALIZED'
export type ProjectionInput = { status: EventStatus; role: 'ORGANIZER' | 'PARTICIPANT'; pendingFacts: readonly ('LOCAL_MUTATION' | 'SYNC_CONFLICT')[]; allowedAction: 'EDIT' | 'RETRY_SYNC' | null }
export type ProjectionOutput = { domainStatus: EventStatus; titleKey: string; statusKey: string | null; primaryActionKey: string | null; sharedConfirmation: boolean }

const titleKeys: Record<EventStatus, string> = {
  DRAFT: 'event.state.draft',
  POLLING: 'event.state.polling',
  COMPARING: 'event.state.comparing',
  CONFIRMED: 'event.state.confirmed',
  ORGANIZING: 'event.state.organizing',
  FINALIZED: 'event.state.finalized',
}

export function projectProductLanguage(input: ProjectionInput): ProjectionOutput {
  const pendingSync = input.pendingFacts.includes('LOCAL_MUTATION')
  return {
    domainStatus: input.status,
    titleKey: titleKeys[input.status],
    statusKey: pendingSync ? 'sync.waiting' : null,
    primaryActionKey: input.allowedAction === 'RETRY_SYNC'
      ? 'sync.retry'
      : input.allowedAction === 'EDIT'
        ? 'event.action.continue'
        : null,
    sharedConfirmation: !pendingSync,
  }
}

export const productLanguageMachine = setup({
  types: {
    context: {} as { input: ProjectionInput; projection: ProjectionOutput },
    input: {} as ProjectionInput,
    events: {} as { type: 'SYNC_SUCCEEDED' } | { type: 'SYNC_FAILED' },
  },
  guards: {
    isTerminal: ({ context }) => context.input.status === 'FINALIZED',
    hasPendingSync: ({ context }) => context.input.pendingFacts.includes('LOCAL_MUTATION'),
  },
  actions: {
    markSynced: assign(({ context }) => {
      const input = {
        ...context.input,
        pendingFacts: context.input.pendingFacts.filter(fact => fact !== 'LOCAL_MUTATION'),
        allowedAction: 'EDIT' as const,
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
        { guard: 'hasPendingSync', target: 'pendingSync' },
        { target: 'ready' },
      ],
    },
    ready: {},
    pendingSync: {
      on: {
        SYNC_SUCCEEDED: { target: 'ready', actions: 'markSynced' },
        SYNC_FAILED: 'syncFailed',
      },
    },
    syncFailed: {
      on: {
        SYNC_SUCCEEDED: { target: 'ready', actions: 'markSynced' },
      },
    },
    terminal: { type: 'final' },
  },
})
