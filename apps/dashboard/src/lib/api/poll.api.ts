import type { AddVoteRequest, PollResponse } from '$lib/types/api'
import { apiFetch } from './client'

/**
 * Fetch the current poll state for an event,
 * including all proposed slots and the votes cast so far.
 */
export async function get(eventId: string): Promise<PollResponse> {
  return apiFetch<PollResponse>(`/events/${encodeURIComponent(eventId)}/poll`)
}

/**
 * Submit (or update) votes for a participant on an event poll.
 */
export async function vote(eventId: string, data: AddVoteRequest): Promise<PollResponse> {
  return apiFetch<PollResponse>(`/events/${encodeURIComponent(eventId)}/poll/votes`, {
    method: 'POST',
    body: JSON.stringify(data)
  })
}
