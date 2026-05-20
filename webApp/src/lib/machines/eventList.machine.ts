import { setup, assign, fromPromise } from 'xstate'
import type { EventResponse, EventsListResponse } from '$lib/types/api'
import * as eventsApi from '$lib/api/events.api'

interface EventListContext {
  events: EventResponse[]
  total: number
  searchQuery: string
  statusFilter: string
  error: string | null
}

type EventListEvent =
  | { type: 'SEARCH'; query: string }
  | { type: 'FILTER'; status: string }
  | { type: 'RELOAD' }

const loadEventsActor = fromPromise(async ({
  input
}: {
  input: { statusFilter: string }
}): Promise<EventsListResponse> => {
  return eventsApi.list(
    input.statusFilter ? { status: input.statusFilter } : undefined
  )
})

export const eventListMachine = setup({
  types: {
    context: {} as EventListContext,
    events: {} as EventListEvent
  },
  actors: {
    loadEvents: loadEventsActor
  },
  actions: {
    assignEvents: assign({
      events: ({ event }) =>
        (event as { output: EventsListResponse }).output.events,
      total: ({ event }) =>
        (event as { output: EventsListResponse }).output.total
    }),
    assignError: assign({
      error: ({ event }) =>
        String((event as { error: unknown }).error)
    }),
    clearError: assign({ error: null }),
    assignSearchQuery: assign({
      searchQuery: ({ event }) =>
        (event as { type: 'SEARCH'; query: string }).query
    }),
    assignStatusFilter: assign({
      statusFilter: ({ event }) =>
        (event as { type: 'FILTER'; status: string }).status
    })
  }
}).createMachine({
  id: 'eventList',
  initial: 'loading',
  context: {
    events: [],
    total: 0,
    searchQuery: '',
    statusFilter: '',
    error: null
  },
  states: {
    loading: {
      entry: 'clearError',
      invoke: {
        src: 'loadEvents',
        input: ({ context }) => ({ statusFilter: context.statusFilter }),
        onDone: {
          target: 'ready',
          actions: 'assignEvents'
        },
        onError: {
          target: 'error',
          actions: 'assignError'
        }
      }
    },

    ready: {
      on: {
        SEARCH: {
          // Client-side filtering: just update searchQuery, stay in ready.
          // Components derive filteredEvents by filtering context.events
          // against context.searchQuery (title/description case-insensitive).
          actions: 'assignSearchQuery'
        },
        FILTER: {
          target: 'loading',
          actions: 'assignStatusFilter'
        },
        RELOAD: {
          target: 'loading'
        }
      }
    },

    error: {
      on: {
        RELOAD: { target: 'loading' },
        FILTER: {
          target: 'loading',
          actions: 'assignStatusFilter'
        }
      }
    }
  }
})

/**
 * Derive the client-side filtered list from context.
 * Call this in the component instead of using context.events directly.
 */
export function deriveFilteredEvents(
  events: EventResponse[],
  searchQuery: string
): EventResponse[] {
  if (!searchQuery.trim()) return events
  const q = searchQuery.toLowerCase()
  return events.filter(
    (e) =>
      e.title.toLowerCase().includes(q) ||
      (e.description?.toLowerCase().includes(q) ?? false)
  )
}
