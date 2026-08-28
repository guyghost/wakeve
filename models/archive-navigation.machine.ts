import { assign, setup } from 'xstate'

export type ArchiveNavigationEvent =
  | { type: 'CLOSE' }
  | { type: 'RETURN' }

export interface ArchiveNavigationContext {
  returnTarget: 'LIBRARY'
  effects: readonly ['NavigateToLibrary'] | readonly []
}

export const archiveNavigationInvariants = [
  'CLOSE and RETURN are the only archive exit events.',
  'Either exit event transitions directly to terminal CLOSED and emits NavigateToLibrary exactly once.',
  'No archive load, empty, failure, stale, sync, or read-only content state can remove or reinterpret the exit.',
  'Events received after CLOSED are inert because the actor is done.',
] as const

export const archiveNavigationMachine = setup({
  types: {
    context: {} as ArchiveNavigationContext,
    events: {} as ArchiveNavigationEvent,
  },
  actions: {
    navigateToLibraryOnce: assign({
      effects: ['NavigateToLibrary'] as const,
    }),
  },
}).createMachine({
  id: 'archiveNavigation',
  initial: 'presented',
  context: {
    returnTarget: 'LIBRARY',
    effects: [],
  },
  states: {
    presented: {
      on: {
        CLOSE: { target: 'closed', actions: 'navigateToLibraryOnce' },
        RETURN: { target: 'closed', actions: 'navigateToLibraryOnce' },
      },
    },
    closed: { type: 'final' },
  },
})
