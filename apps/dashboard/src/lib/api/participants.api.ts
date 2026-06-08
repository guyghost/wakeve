import type { AddParticipantRequest, ParticipantDTO, ParticipantsResponse } from '$lib/types/api'
import { apiFetch } from './client'

/**
 * Fetch the list of participants for an event.
 */
export async function list(eventId: string): Promise<ParticipantsResponse> {
  return apiFetch<ParticipantsResponse>(
    `/events/${encodeURIComponent(eventId)}/participants`
  )
}

/**
 * Add a participant to an event.
 */
export async function add(
  eventId: string,
  data: AddParticipantRequest
): Promise<ParticipantDTO> {
  return apiFetch<ParticipantDTO>(
    `/events/${encodeURIComponent(eventId)}/participants`,
    {
      method: 'POST',
      body: JSON.stringify(data)
    }
  )
}
