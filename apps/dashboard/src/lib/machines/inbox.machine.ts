import { setup, assign, fromPromise } from 'xstate'
import type { Notification } from '$lib/types/api'
import * as notificationsApi from '$lib/api/notifications.api'
import { actorError, actorOutput } from './actor-event'
import { unreadCount, refreshUnreadCount } from './unread-count.store'

interface InboxContext {
  notifications: Notification[]
  unreadOnly: boolean
  error: string | null
  mutationError: string | null
  /** Snapshot of the list before the optimistic mutation, for rollback. */
  previousNotifications: Notification[] | null
}

type InboxEvent =
  | { type: 'SET_UNREAD_FILTER'; value: boolean }
  | { type: 'MARK_READ'; id: string }
  | { type: 'MARK_ALL_READ' }
  | { type: 'DELETE'; id: string }
  | { type: 'RELOAD' }

function isUnread(notification: Notification): boolean {
  return notification.readAt === null
}

/**
 * Adjust the shared badge counter. Called from optimistic actions so the
 * badge reacts instantly; mutations failing server-side trigger a full
 * resync via `refreshUnreadCount()`.
 */
function adjustUnreadBadge(delta: number): void {
  unreadCount.update((count) => Math.max(0, count + delta))
}

/** Filter the list client-side according to the unread-only toggle. */
export function deriveVisibleNotifications(
  notifications: Notification[],
  unreadOnly: boolean
): Notification[] {
  return unreadOnly ? notifications.filter(isUnread) : notifications
}

/** Number of unread notifications in the given list. */
export function countUnreadNotifications(notifications: Notification[]): number {
  return notifications.filter(isUnread).length
}

export const inboxMachine = setup({
  types: {
    context: {} as InboxContext,
    events: {} as InboxEvent
  },
  actors: {
    loadNotifications: fromPromise((): Promise<Notification[]> => notificationsApi.list()),
    markRead: fromPromise(async ({ input }: { input: { id: string } }) =>
      notificationsApi.markRead(input.id)
    ),
    markAllRead: fromPromise(() => notificationsApi.markAllRead()),
    deleteNotification: fromPromise(async ({ input }: { input: { id: string } }) =>
      notificationsApi.remove(input.id)
    )
  },
  actions: {
    assignNotifications: assign({
      notifications: ({ event }) => actorOutput<Notification[]>(event),
      error: null
    }),
    assignError: assign({
      error: ({ event }) => actorError(event)
    }),
    assignUnreadOnly: assign({
      unreadOnly: ({ event }) => (event as { type: 'SET_UNREAD_FILTER'; value: boolean }).value
    }),
    // Optimistic: mark one notification as read and decrement the badge.
    // The badge side effect is kept next to the list update so both stay atomic.
    markReadOptimistic: assign(({ context, event }) => {
      const { id } = event as { type: 'MARK_READ'; id: string }
      const previous = context.notifications
      const target = previous.find((n) => n.id === id)
      if (target && isUnread(target)) adjustUnreadBadge(-1)
      return {
        previousNotifications: previous,
        mutationError: null,
        notifications: previous.map((n) =>
          n.id === id && isUnread(n) ? { ...n, readAt: new Date().toISOString() } : n
        )
      }
    }),
    markAllReadOptimistic: assign(({ context }) => {
      const previous = context.notifications
      const now = new Date().toISOString()
      adjustUnreadBadge(-countUnreadNotifications(previous))
      return {
        previousNotifications: previous,
        mutationError: null,
        notifications: previous.map((n) => (isUnread(n) ? { ...n, readAt: now } : n))
      }
    }),
    deleteOptimistic: assign(({ context, event }) => {
      const { id } = event as { type: 'DELETE'; id: string }
      const previous = context.notifications
      const target = previous.find((n) => n.id === id)
      if (target && isUnread(target)) adjustUnreadBadge(-1)
      return {
        previousNotifications: previous,
        mutationError: null,
        notifications: previous.filter((n) => n.id !== id)
      }
    }),
    revertMutation: assign(({ context }) => {
      // Roll back the optimistic list, then resync the badge from the server.
      void refreshUnreadCount()
      return {
        notifications: context.previousNotifications ?? context.notifications,
        previousNotifications: null
      }
    }),
    clearMutationSnapshot: assign({ previousNotifications: null }),
    assignMutationError: assign({
      mutationError: ({ event }) => actorError(event)
    }),
    clearErrors: assign({ error: null, mutationError: null })
  }
}).createMachine({
  id: 'inbox',
  initial: 'loading',
  context: {
    notifications: [],
    unreadOnly: false,
    error: null,
    mutationError: null,
    previousNotifications: null
  },
  states: {
    loading: {
      entry: 'clearErrors',
      invoke: {
        src: 'loadNotifications',
        onDone: { target: 'ready', actions: 'assignNotifications' },
        onError: { target: 'error', actions: 'assignError' }
      }
    },

    ready: {
      on: {
        SET_UNREAD_FILTER: { actions: 'assignUnreadOnly' },
        MARK_READ: { target: 'markingRead', actions: 'markReadOptimistic' },
        MARK_ALL_READ: { target: 'markingAllRead', actions: 'markAllReadOptimistic' },
        DELETE: { target: 'deleting', actions: 'deleteOptimistic' },
        RELOAD: { target: 'loading' }
      }
    },

    markingRead: {
      invoke: {
        src: 'markRead',
        input: ({ event }) => {
          const e = event as { type: 'MARK_READ'; id: string }
          return { id: e.id }
        },
        onDone: { target: 'ready', actions: 'clearMutationSnapshot' },
        onError: { target: 'ready', actions: ['revertMutation', 'assignMutationError'] }
      }
    },

    markingAllRead: {
      invoke: {
        src: 'markAllRead',
        onDone: { target: 'ready', actions: 'clearMutationSnapshot' },
        onError: { target: 'ready', actions: ['revertMutation', 'assignMutationError'] }
      }
    },

    deleting: {
      invoke: {
        src: 'deleteNotification',
        input: ({ event }) => {
          const e = event as { type: 'DELETE'; id: string }
          return { id: e.id }
        },
        onDone: { target: 'ready', actions: 'clearMutationSnapshot' },
        onError: { target: 'ready', actions: ['revertMutation', 'assignMutationError'] }
      }
    },

    error: {
      on: {
        RELOAD: { target: 'loading' }
      }
    }
  }
})
