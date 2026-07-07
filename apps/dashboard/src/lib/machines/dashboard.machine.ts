import { setup, assign, fromPromise } from 'xstate'
import type {
  DashboardEventItem,
  DashboardOverviewResponse,
  EventDetailedAnalyticsResponse
} from '$lib/types/api'
import * as dashboardApi from '$lib/api/dashboard.api'
import { ApiError } from '$lib/api/client'
import { actorError, actorOutput } from './actor-event'

export type DashboardErrorKind = 'offline' | 'permission' | 'auth' | 'server'

interface DashboardContext {
  overview: DashboardOverviewResponse | null
  events: DashboardEventItem[]
  selectedEventId: string | null
  analytics: EventDetailedAnalyticsResponse | null
  error: string | null
  errorKind: DashboardErrorKind | null
  analyticsError: string | null
  analyticsErrorKind: DashboardErrorKind | null
  copiedEventId: string | null
  isOffline: boolean
}

type DashboardEvent =
  | { type: 'SELECT_EVENT'; id: string }
  | { type: 'CLOSE_DETAILS' }
  | { type: 'COPY_LINK_SUCCESS'; id: string }
  | { type: 'CLEAR_COPY_FEEDBACK' }
  | { type: 'NETWORK_CHANGED'; isOffline: boolean }
  | { type: 'RELOAD' }

interface LoadDashboardOutput {
  overview: DashboardOverviewResponse
  events: DashboardEventItem[]
}

const loadDashboardActor = fromPromise(async (): Promise<LoadDashboardOutput> => {
  const [overviewResp, eventsResp] = await Promise.all([
    dashboardApi.getOverview(),
    dashboardApi.getEvents()
  ])
  return { overview: overviewResp, events: eventsResp.events }
})

const loadAnalyticsActor = fromPromise(async ({
  input
}: {
  input: { eventId: string }
}): Promise<EventDetailedAnalyticsResponse> => {
  return dashboardApi.getEventAnalytics(input.eventId)
})

function eventError(event: unknown): unknown {
  return (event as { error?: unknown }).error
}

function classifyError(error: unknown): DashboardErrorKind {
  if (typeof navigator !== 'undefined' && !navigator.onLine) return 'offline'
  if (error instanceof ApiError) {
    if (error.status === 401) return 'auth'
    if (error.status === 403) return 'permission'
  }
  if (error instanceof TypeError || String(error).includes('Failed to fetch')) return 'offline'
  return 'server'
}

export const dashboardMachine = setup({
  types: {
    context: {} as DashboardContext,
    events: {} as DashboardEvent
  },
  actors: {
    loadDashboard: loadDashboardActor,
    loadAnalytics: loadAnalyticsActor
  },
  actions: {
    assignDashboard: assign({
      overview: ({ event }) =>
        actorOutput<LoadDashboardOutput>(event).overview,
      events: ({ event }) =>
        actorOutput<LoadDashboardOutput>(event).events
    }),

    assignLoadError: assign({
      error: ({ event }) => actorError(event),
      errorKind: ({ event }) => classifyError(eventError(event))
    }),

    assignSelectedEventId: assign({
      selectedEventId: ({ event }) =>
        (event as { type: 'SELECT_EVENT'; id: string }).id,
      analytics: null,
      analyticsError: null,
      analyticsErrorKind: null
    }),

    assignCopiedEventId: assign({
      copiedEventId: ({ event }) =>
        (event as { type: 'COPY_LINK_SUCCESS'; id: string }).id
    }),

    clearCopyFeedback: assign({
      copiedEventId: null
    }),

    assignNetwork: assign({
      isOffline: ({ event }) =>
        (event as { type: 'NETWORK_CHANGED'; isOffline: boolean }).isOffline
    }),

    assignAnalytics: assign({
      analytics: ({ event }) =>
        actorOutput<EventDetailedAnalyticsResponse>(event),
      analyticsError: null,
      analyticsErrorKind: null
    }),

    assignAnalyticsError: assign({
      analyticsError: ({ event }) =>
        actorError(event),
      analyticsErrorKind: ({ event }) => classifyError(eventError(event))
    }),

    clearSelection: assign({
      selectedEventId: null,
      analytics: null,
      analyticsError: null,
      analyticsErrorKind: null
    }),

    clearError: assign({ error: null, errorKind: null })
  }
}).createMachine({
  id: 'dashboard',
  initial: 'loading',
  context: {
    overview: null,
    events: [],
    selectedEventId: null,
    analytics: null,
    error: null,
    errorKind: null,
    analyticsError: null,
    analyticsErrorKind: null,
    copiedEventId: null,
    isOffline: typeof navigator !== 'undefined' ? !navigator.onLine : false
  },
  on: {
    COPY_LINK_SUCCESS: { actions: 'assignCopiedEventId' },
    CLEAR_COPY_FEEDBACK: { actions: 'clearCopyFeedback' },
    NETWORK_CHANGED: { actions: 'assignNetwork' }
  },
  states: {
    loading: {
      entry: 'clearError',
      invoke: {
        src: 'loadDashboard',
        onDone: {
          target: 'ready',
          actions: 'assignDashboard'
        },
        onError: {
          target: 'error',
          actions: 'assignLoadError'
        }
      }
    },

    ready: {
      on: {
        SELECT_EVENT: {
          target: 'loadingAnalytics',
          actions: 'assignSelectedEventId'
        },
        RELOAD: { target: 'loading' }
      }
    },

    loadingAnalytics: {
      invoke: {
        src: 'loadAnalytics',
        input: ({ context }) => ({ eventId: context.selectedEventId! }),
        onDone: {
          target: 'showingDetails',
          actions: 'assignAnalytics'
        },
        onError: {
          target: 'showingDetails',
          actions: 'assignAnalyticsError'
        }
      },
      on: {
        CLOSE_DETAILS: {
          target: 'ready',
          actions: 'clearSelection'
        }
      }
    },

    showingDetails: {
      on: {
        CLOSE_DETAILS: {
          target: 'ready',
          actions: 'clearSelection'
        },
        SELECT_EVENT: {
          target: 'loadingAnalytics',
          actions: 'assignSelectedEventId'
        },
        RELOAD: {
          target: 'loading',
          actions: 'clearSelection'
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
