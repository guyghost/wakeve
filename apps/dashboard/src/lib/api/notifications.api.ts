import type { Notification, NotificationPreferences } from '$lib/types/api'
import { apiFetch } from './client'

/** The server clamps the history limit to 1..100 (NotificationRoutes.kt). */
const MAX_HISTORY_LIMIT = 100

/**
 * List the latest notifications of the authenticated user.
 */
export async function list(limit: number = MAX_HISTORY_LIMIT): Promise<Notification[]> {
  return apiFetch<Notification[]>(`/notifications?limit=${limit}`)
}

/**
 * List the unread notifications of the authenticated user.
 */
export async function listUnread(limit: number = MAX_HISTORY_LIMIT): Promise<Notification[]> {
  return apiFetch<Notification[]>(`/notifications/unread?limit=${limit}`)
}

/**
 * Number of unread notifications.
 *
 * The backend exposes the unread list only (no dedicated count endpoint),
 * so the count is derived from the list length.
 */
export async function unreadCount(): Promise<number> {
  const unread = await listUnread()
  return unread.length
}

/**
 * Mark a single notification as read.
 */
export async function markRead(id: string): Promise<void> {
  await apiFetch<unknown>(`/notifications/${encodeURIComponent(id)}/read`, { method: 'PUT' })
}

/**
 * Mark every notification of the authenticated user as read.
 */
export async function markAllRead(): Promise<void> {
  await apiFetch<unknown>('/notifications/read-all', { method: 'PUT' })
}

/**
 * Delete a notification.
 */
export async function remove(id: string): Promise<void> {
  await apiFetch<unknown>(`/notifications/${encodeURIComponent(id)}`, { method: 'DELETE' })
}

/**
 * Get the notification preferences of the authenticated user.
 * Returns the effective (default) preferences when none are stored yet.
 */
export async function getPreferences(): Promise<NotificationPreferences> {
  return apiFetch<NotificationPreferences>('/notifications/preferences')
}

/**
 * Replace the notification preferences of the authenticated user.
 * The payload must be the full preferences object: the server expects
 * userId and updatedAt and rejects a userId owned by another user.
 */
export async function updatePreferences(preferences: NotificationPreferences): Promise<void> {
  await apiFetch<unknown>('/notifications/preferences', {
    method: 'PUT',
    body: JSON.stringify(preferences)
  })
}
