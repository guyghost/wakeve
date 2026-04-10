import type {
  CreateEventRequest,
  EventResponse,
  EventsListResponse,
  UpdateEventStatusRequest
} from '$lib/types/api'
import { apiFetch } from './client'

export interface EventListFilters {
  status?: string
  search?: string
}

/**
 * Fetch the list of events for the authenticated user,
 * with optional status and search filters.
 */
export async function list(filters?: EventListFilters): Promise<EventsListResponse> {
  const params = new URLSearchParams()
  if (filters?.status) params.set('status', filters.status)
  if (filters?.search) params.set('search', filters.search)
  const query = params.size > 0 ? `?${params.toString()}` : ''
  return apiFetch<EventsListResponse>(`/events${query}`)
}

/**
 * Fetch a single event by its ID.
 */
export async function get(id: string): Promise<EventResponse> {
  return apiFetch<EventResponse>(`/events/${encodeURIComponent(id)}`)
}

/**
 * Create a new event.
 */
export async function create(data: CreateEventRequest): Promise<EventResponse> {
  return apiFetch<EventResponse>('/events', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

/**
 * Update the status of an event (e.g. DRAFT → POLLING, POLLING → CONFIRMED).
 */
export async function updateStatus(
  id: string,
  data: UpdateEventStatusRequest
): Promise<EventResponse> {
  return apiFetch<EventResponse>(`/events/${encodeURIComponent(id)}/status`, {
    method: 'PUT',
    body: JSON.stringify(data)
  })
}
