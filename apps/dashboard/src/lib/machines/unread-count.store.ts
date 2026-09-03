import { writable } from 'svelte/store'
import * as notificationsApi from '$lib/api/notifications.api'

/**
 * Shared unread-notifications counter driving the navigation badge.
 * A plain writable store so both `.svelte` components (AppHeader)
 * and plain `.ts` modules (inbox machine) can share the same value.
 */
export const unreadCount = writable(0)

/**
 * Refetch the unread count from the API and update the store.
 * Failures keep the previous value: a stale badge is preferred
 * over flickering to zero.
 */
export async function refreshUnreadCount(): Promise<void> {
  try {
    unreadCount.set(await notificationsApi.unreadCount())
  } catch {
    // Keep the last known count.
  }
}
