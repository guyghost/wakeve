import { setup, assign, fromPromise } from 'xstate'
import type {
  Budget,
  BudgetItem,
  CreateBudgetItemRequest,
  ParticipantDTO,
  SettlementRecord
} from '$lib/types/api'
import { ApiError } from '$lib/api/client'
import * as budgetApi from '$lib/api/budget.api'
import { list as listParticipants } from '$lib/api/participants.api'
import { actorError, actorOutput } from './actor-event'

export type BudgetErrorKind = 'offline' | 'auth' | 'permission' | 'server'

interface BudgetContext {
  eventId: string
  items: BudgetItem[]
  budget: Budget | null
  settlements: SettlementRecord[]
  participants: ParticipantDTO[]
  error: string | null
  errorKind: BudgetErrorKind | null
  itemError: string | null
  deleteError: string | null
  deletingItemId: string | null
}

type BudgetMachineEvent =
  | { type: 'ADD_ITEM'; data: CreateBudgetItemRequest }
  | { type: 'DELETE_ITEM'; itemId: string }
  | { type: 'RELOAD' }

type BudgetMachineInput = { eventId: string }

interface LoadBudgetOutput {
  items: BudgetItem[]
  budget: Budget | null
  settlements: SettlementRecord[]
  participants: ParticipantDTO[]
}

function eventError(event: unknown): unknown {
  return (event as { error?: unknown }).error
}

function classifyError(error: unknown): BudgetErrorKind {
  if (typeof navigator !== 'undefined' && !navigator.onLine) return 'offline'
  if (error instanceof ApiError) {
    if (error.status === 401) return 'auth'
    if (error.status === 403) return 'permission'
  }
  if (error instanceof TypeError || String(error).includes('Failed to fetch')) return 'offline'
  return 'server'
}

const loadBudgetActor = fromPromise(async ({
  input
}: {
  input: BudgetMachineInput
}): Promise<LoadBudgetOutput> => {
  // A missing budget baseline (404) is equivalent to an empty budget.
  const [items, summary, settlementsResponse, participantsResponse] = await Promise.all([
    budgetApi.list(input.eventId).catch((error) => {
      if (error instanceof ApiError && error.status === 404) return [] as BudgetItem[]
      throw error
    }),
    budgetApi.summary(input.eventId).catch(() => null),
    budgetApi.settlements(input.eventId).catch(() => ({ settlements: [] as SettlementRecord[] })),
    listParticipants(input.eventId).catch(() => ({ participants: [] as ParticipantDTO[] }))
  ])
  return {
    items,
    budget: summary?.budget ?? null,
    settlements: settlementsResponse.settlements,
    participants: participantsResponse.participants
  }
})

const createItemActor = fromPromise(async ({
  input
}: {
  input: { eventId: string; data: CreateBudgetItemRequest }
}): Promise<BudgetItem> => {
  return budgetApi.create(input.eventId, input.data)
})

const deleteItemActor = fromPromise(async ({
  input
}: {
  input: { eventId: string; itemId: string }
}): Promise<string> => {
  await budgetApi.remove(input.eventId, input.itemId)
  return input.itemId
})

export const budgetMachine = setup({
  types: {
    context: {} as BudgetContext,
    events: {} as BudgetMachineEvent,
    input: {} as BudgetMachineInput
  },
  actors: {
    loadBudget: loadBudgetActor,
    createItem: createItemActor,
    deleteItem: deleteItemActor
  },
  actions: {
    assignData: assign({
      items: ({ event }) => actorOutput<LoadBudgetOutput>(event).items,
      budget: ({ event }) => actorOutput<LoadBudgetOutput>(event).budget,
      settlements: ({ event }) => actorOutput<LoadBudgetOutput>(event).settlements,
      participants: ({ event }) => actorOutput<LoadBudgetOutput>(event).participants,
      error: null,
      errorKind: null
    }),

    assignLoadError: assign({
      error: ({ event }) => actorError(event),
      errorKind: ({ event }) => classifyError(eventError(event))
    }),

    storeDeletingId: assign({
      deletingItemId: ({ event }) =>
        (event as { type: 'DELETE_ITEM'; itemId: string }).itemId
    }),

    clearDeletingId: assign({ deletingItemId: null }),

    assignItemError: assign({
      itemError: ({ event }) => actorError(event)
    }),

    assignDeleteError: assign({
      deleteError: ({ event }) => actorError(event)
    })
  }
}).createMachine({
  id: 'budget',
  initial: 'loading',
  context: ({ input }) => ({
    eventId: input.eventId,
    items: [],
    budget: null,
    settlements: [],
    participants: [],
    error: null,
    errorKind: null,
    itemError: null,
    deleteError: null,
    deletingItemId: null
  }),
  states: {
    loading: {
      entry: assign({ itemError: null, deleteError: null }),
      invoke: {
        src: 'loadBudget',
        input: ({ context }) => ({ eventId: context.eventId }),
        onDone: { target: 'ready', actions: 'assignData' },
        onError: { target: 'error', actions: 'assignLoadError' }
      }
    },

    ready: {
      on: {
        ADD_ITEM: { target: 'addingItem' },
        DELETE_ITEM: { target: 'deletingItem', actions: 'storeDeletingId' },
        RELOAD: { target: 'loading' }
      }
    },

    addingItem: {
      entry: assign({ itemError: null }),
      invoke: {
        src: 'createItem',
        input: ({ context, event }) => {
          const e = event as { type: 'ADD_ITEM'; data: CreateBudgetItemRequest }
          return { eventId: context.eventId, data: e.data }
        },
        // Items, summary and settlements all change: reload everything.
        onDone: { target: 'loading' },
        onError: { target: 'ready', actions: 'assignItemError' }
      }
    },

    deletingItem: {
      invoke: {
        src: 'deleteItem',
        input: ({ context }) => ({
          eventId: context.eventId,
          itemId: context.deletingItemId!
        }),
        onDone: { target: 'loading' },
        onError: {
          target: 'ready',
          actions: ['assignDeleteError', 'clearDeletingId']
        }
      }
    },

    error: {
      on: {
        RELOAD: { target: 'loading' }
      }
    }
  }
})
