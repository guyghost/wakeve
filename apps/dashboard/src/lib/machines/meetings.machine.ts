import { setup, assign, fromPromise } from 'xstate'
import type {
  CreateGoogleMeetRequest,
  CreateZoomMeetingRequest,
  MeetingDTO
} from '$lib/types/api'
import * as meetingsApi from '$lib/api/meetings.api'
import { actorError, actorOutput } from './actor-event'

interface MeetingsContext {
  eventId: string
  meetings: MeetingDTO[]
  error: string | null
  isCreating: boolean
  createError: string | null
  cancellingId: string | null
  cancelError: string | null
}

type MeetingsEvent =
  | { type: 'CREATE_ZOOM'; input: CreateZoomMeetingRequest }
  | { type: 'CREATE_GOOGLE_MEET'; input: CreateGoogleMeetRequest }
  | { type: 'CANCEL_ZOOM'; meetingId: string }
  | { type: 'RELOAD' }

type MeetingsInput = { eventId: string }

export const meetingsMachine = setup({
  types: {
    context: {} as MeetingsContext,
    events: {} as MeetingsEvent,
    input: {} as MeetingsInput
  },
  actors: {
    loadMeetings: fromPromise(async ({ input }: { input: { eventId: string } }): Promise<MeetingDTO[]> => {
      const resp = await meetingsApi.list(input.eventId)
      return resp.meetings
    }),

    createZoomActor: fromPromise(async ({ input }: { input: CreateZoomMeetingRequest }) => {
      return meetingsApi.createZoom(input)
    }),

    createGoogleMeetActor: fromPromise(async ({ input }: { input: CreateGoogleMeetRequest }) => {
      return meetingsApi.createGoogleMeet(input)
    }),

    cancelZoomActor: fromPromise(async ({ input }: { input: { meetingId: string } }) => {
      return meetingsApi.zoomCancel(input.meetingId)
    })
  },
  actions: {
    assignMeetings: assign({
      meetings: ({ event }) => actorOutput<MeetingDTO[]>(event),
      error: null
    }),

    assignLoadError: assign({
      error: ({ event }) => actorError(event)
    }),

    assignCancelTarget: assign({
      cancellingId: ({ event }) => (event as { meetingId: string }).meetingId
    }),

    assignCancelled: assign({
      meetings: ({ context }) =>
        context.meetings.map((m) =>
          m.hostMeetingId === context.cancellingId ? { ...m, status: 'CANCELLED' as const } : m
        ),
      cancellingId: null,
      cancelError: null
    }),

    assignCancelError: assign({
      cancelError: ({ event }) => actorError(event),
      cancellingId: null
    }),

    assignCreateError: assign({
      createError: ({ event }) => actorError(event),
      isCreating: false
    }),

    clearCreateState: assign({
      isCreating: false,
      createError: null
    })
  }
}).createMachine({
  id: 'meetings',
  initial: 'loading',
  context: ({ input }) => ({
    eventId: input.eventId,
    meetings: [],
    error: null,
    isCreating: false,
    createError: null,
    cancellingId: null,
    cancelError: null
  }),
  states: {
    loading: {
      entry: assign({ error: null }),
      invoke: {
        src: 'loadMeetings',
        input: ({ context }) => ({ eventId: context.eventId }),
        onDone: { target: 'ready', actions: 'assignMeetings' },
        onError: { target: 'error', actions: 'assignLoadError' }
      }
    },

    error: {
      on: {
        RELOAD: { target: 'loading' }
      }
    },

    ready: {
      on: {
        CREATE_ZOOM: { target: 'creatingZoom' },
        CREATE_GOOGLE_MEET: { target: 'creatingMeet' },
        CANCEL_ZOOM: {
          target: 'cancelling',
          actions: 'assignCancelTarget'
        },
        RELOAD: { target: 'loading' }
      }
    },

    creatingZoom: {
      entry: assign({ isCreating: true, createError: null }),
      invoke: {
        src: 'createZoomActor',
        input: ({ event }) => (event as { input: CreateZoomMeetingRequest }).input,
        // Creation succeeded server-side: reload the authoritative list
        onDone: { target: 'reloadAfterCreate' },
        onError: { target: 'ready', actions: 'assignCreateError' }
      }
    },

    creatingMeet: {
      entry: assign({ isCreating: true, createError: null }),
      invoke: {
        src: 'createGoogleMeetActor',
        input: ({ event }) => (event as { input: CreateGoogleMeetRequest }).input,
        onDone: { target: 'reloadAfterCreate' },
        onError: { target: 'ready', actions: 'assignCreateError' }
      }
    },

    reloadAfterCreate: {
      invoke: {
        src: 'loadMeetings',
        input: ({ context }) => ({ eventId: context.eventId }),
        onDone: {
          target: 'ready',
          actions: ['assignMeetings', 'clearCreateState']
        },
        // List refresh failed after a successful creation — back to ready,
        // the meeting exists and appears on next manual reload
        onError: { target: 'ready', actions: 'clearCreateState' }
      }
    },

    cancelling: {
      invoke: {
        src: 'cancelZoomActor',
        input: ({ context }) => ({ meetingId: context.cancellingId! }),
        onDone: { target: 'ready', actions: 'assignCancelled' },
        onError: { target: 'ready', actions: 'assignCancelError' }
      }
    }
  }
})
