import { setup, assign, fromPromise } from 'xstate'
import type {
  DashboardEventItem,
  DashboardOverviewResponse,
  EventDetailedAnalyticsResponse
} from '$lib/types/api'
import * as dashboardApi from '$lib/api/dashboard.api'

interface DashboardContext {
  overview: DashboardOverviewResponse | null
  events: DashboardEventItem[]
  selectedEventId: string | null
  analytics: EventDetailedAnalyticsResponse | null
  error: string | null
  analyticsError: string | null
}

type DashboardEvent =
  | { type: 'SELECT_EVENT'; id: string }
  | { type: 'CLOSE_ANALYTICS' }
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
        (event as { output: LoadDashboardOutput }).output.overview,
      events: ({ event }) =>
        (event as { output: LoadDashboardOutput }).output.events
    }),

    assignLoadError: assign({
      error: ({ event }) => String((event as { error: unknown }).error)
    }),

    assignSelectedEventId: assign({
      selectedEventId: ({ event }) =>
        (event as { type: 'SELECT_EVENT'; id: string }).id,
      analytics: null,
      analyticsError: null
    }),

    assignAnalytics: assign({
      analytics: ({ event }) =>
        (event as { output: EventDetailedAnalyticsResponse }).output,
      analyticsError: null
    }),

    assignAnalyticsError: assign({
      analyticsError: ({ event }) =>
        String((event as { error: unknown }).error)
    }),

    clearSelection: assign({
      selectedEventId: null,
      analytics: null,
      analyticsError: null
    }),

    clearError: assign({ error: null })
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
    analyticsError: null
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
          target: 'showingAnalytics',
          actions: 'assignAnalytics'
        },
        onError: {
          target: 'showingAnalytics',
          actions: 'assignAnalyticsError'
        }
      },
      on: {
        CLOSE_ANALYTICS: {
          target: 'ready',
          actions: 'clearSelection'
        }
      }
    },

    showingAnalytics: {
      on: {
        CLOSE_ANALYTICS: {
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
