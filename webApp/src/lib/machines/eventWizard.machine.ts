import { setup, assign, fromPromise } from 'xstate'
import type {
  CreateEventRequest,
  EventResponse,
  EventType,
  TimeOfDay
} from '$lib/types/api'
import * as eventsApi from '$lib/api/events.api'
import { validateSlot, buildSlotRequest } from '$lib/utils/slot'

export interface WizardSlot {
  id: string
  timeOfDay: TimeOfDay
  date: string
  startTime: string
  endTime: string
  errors: string[]
}

type WizardStep = 'step1' | 'step2' | 'step3' | 'step4'

// Fields the user fills out across all steps
export interface WizardFields {
  title: string
  description: string
  type: EventType
  expectedParticipants: string
  deadline: string
  timezone: string
}

interface WizardContext {
  fields: WizardFields
  slots: WizardSlot[]
  invites: string[]
  formError: string | null
  createdEventId: string | null
}

type WizardEvent =
  | { type: 'NEXT' }
  | { type: 'PREV' }
  | { type: 'UPDATE_FIELD'; field: keyof WizardFields; value: string }
  | { type: 'ADD_SLOT' }
  | { type: 'REMOVE_SLOT'; id: string }
  | { type: 'UPDATE_SLOT'; id: string; field: keyof Omit<WizardSlot, 'id' | 'errors'>; value: string }
  | { type: 'ADD_INVITE'; email: string }
  | { type: 'REMOVE_INVITE'; email: string }
  | { type: 'SUBMIT' }

function makeSlot(): WizardSlot {
  return {
    id: crypto.randomUUID(),
    timeOfDay: 'ALL_DAY',
    date: '',
    startTime: '',
    endTime: '',
    errors: []
  }
}

function validateStep1(ctx: WizardContext): string | null {
  if (!ctx.fields.title.trim()) return 'Le titre est requis'
  if (!ctx.fields.deadline) return 'La date limite est requise'
  return null
}

function validateStep2(ctx: WizardContext): string | null {
  if (ctx.slots.length === 0) return 'Ajoutez au moins un créneau'
  for (const slot of ctx.slots) {
    const errs = validateSlot(slot)
    if (errs.length > 0) return 'Certains créneaux contiennent des erreurs'
  }
  return null
}

const submitActor = fromPromise(async ({
  input
}: {
  input: { context: WizardContext }
}): Promise<EventResponse> => {
  const { fields, slots } = input.context
  const payload: CreateEventRequest = {
    title: fields.title.trim(),
    description: fields.description.trim() || undefined,
    type: fields.type,
    expectedParticipants: fields.expectedParticipants
      ? parseInt(fields.expectedParticipants, 10)
      : undefined,
    deadline: fields.deadline
      ? new Date(fields.deadline).toISOString()
      : undefined,
    timezone: fields.timezone,
    proposedSlots: slots.map(buildSlotRequest)
  }
  return eventsApi.create(payload)
})

export const eventWizardMachine = setup({
  types: {
    context: {} as WizardContext,
    events: {} as WizardEvent
  },
  actors: {
    submitEvent: submitActor
  },
  guards: {
    step1Valid: ({ context }) => validateStep1(context) === null,
    step2Valid: ({ context }) => validateStep2(context) === null
  },
  actions: {
    assignFormError: assign({
      formError: ({ event }) =>
        String((event as { error: unknown }).error)
    }),
    clearFormError: assign({ formError: null }),

    setStep1Error: assign({
      formError: ({ context }) => validateStep1(context)
    }),

    setStep2Error: assign({
      formError: ({ context }) => validateStep2(context)
    }),

    updateField: assign({
      fields: ({ context, event }) => {
        const e = event as { type: 'UPDATE_FIELD'; field: keyof WizardFields; value: string }
        return { ...context.fields, [e.field]: e.value }
      }
    }),

    addSlot: assign({
      slots: ({ context }) => [...context.slots, makeSlot()]
    }),

    removeSlot: assign({
      slots: ({ context, event }) => {
        const e = event as { type: 'REMOVE_SLOT'; id: string }
        return context.slots.filter((s) => s.id !== e.id)
      }
    }),

    updateSlot: assign({
      slots: ({ context, event }) => {
        const e = event as {
          type: 'UPDATE_SLOT'
          id: string
          field: keyof Omit<WizardSlot, 'id' | 'errors'>
          value: string
        }
        return context.slots.map((slot) => {
          if (slot.id !== e.id) return slot
          const updated = { ...slot, [e.field]: e.value }
          // Re-validate on every change so errors are live
          updated.errors = validateSlot(updated)
          return updated
        })
      }
    }),

    addInvite: assign({
      invites: ({ context, event }) => {
        const e = event as { type: 'ADD_INVITE'; email: string }
        const email = e.email.trim().toLowerCase()
        if (!email || context.invites.includes(email)) return context.invites
        return [...context.invites, email]
      }
    }),

    removeInvite: assign({
      invites: ({ context, event }) => {
        const e = event as { type: 'REMOVE_INVITE'; email: string }
        return context.invites.filter((i) => i !== e.email)
      }
    }),

    assignCreatedEventId: assign({
      createdEventId: ({ event }) =>
        (event as { output: EventResponse }).output.id
    })
  }
}).createMachine({
  id: 'eventWizard',
  initial: 'step1',
  context: {
    fields: {
      title: '',
      description: '',
      type: 'OTHER',
      expectedParticipants: '',
      deadline: '',
      timezone: Intl.DateTimeFormat().resolvedOptions().timeZone
    },
    slots: [makeSlot()],
    invites: [],
    formError: null,
    createdEventId: null
  },
  states: {
    step1: {
      entry: 'clearFormError',
      on: {
        UPDATE_FIELD: { actions: 'updateField' },
        NEXT: [
          {
            guard: 'step1Valid',
            target: 'step2',
            actions: 'clearFormError'
          },
          {
            actions: 'setStep1Error'
          }
        ]
      }
    },

    step2: {
      entry: 'clearFormError',
      on: {
        UPDATE_FIELD: { actions: 'updateField' },
        ADD_SLOT: { actions: 'addSlot' },
        REMOVE_SLOT: { actions: 'removeSlot' },
        UPDATE_SLOT: { actions: 'updateSlot' },
        NEXT: [
          {
            guard: 'step2Valid',
            target: 'step3',
            actions: 'clearFormError'
          },
          {
            actions: 'setStep2Error'
          }
        ],
        PREV: { target: 'step1' }
      }
    },

    step3: {
      // Location selection — available in future phase; always allow NEXT/PREV
      entry: 'clearFormError',
      on: {
        NEXT: { target: 'step4' },
        PREV: { target: 'step2' }
      }
    },

    step4: {
      entry: 'clearFormError',
      on: {
        ADD_INVITE: { actions: 'addInvite' },
        REMOVE_INVITE: { actions: 'removeInvite' },
        PREV: { target: 'step3' },
        SUBMIT: { target: 'submitting' }
      }
    },

    submitting: {
      invoke: {
        src: 'submitEvent',
        input: ({ context }) => ({ context }),
        onDone: {
          target: 'success',
          actions: 'assignCreatedEventId'
        },
        onError: {
          target: 'step4',
          actions: 'assignFormError'
        }
      }
    },

    success: {
      type: 'final'
    }
  }
})
