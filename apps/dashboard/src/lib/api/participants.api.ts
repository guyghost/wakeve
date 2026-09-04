import type {
  AddParticipantRequest,
  ParticipantsResponse
} from '$lib/types/api'
import { apiFetch } from './client'

/**
 * Fetch the list of participants for an event.
 *
 * The backend returns plain user IDs (ParticipantRoutes.kt:
 * `mapOf("participants" to participants)` where participants is a List<String>).
 */
export async function list(eventId: string): Promise<ParticipantsResponse> {
  return apiFetch<ParticipantsResponse>(
    `/events/${encodeURIComponent(eventId)}/participants`
  )
}

/**
 * Add a participant to an event (organizer only).
 * The request body must carry `eventId` matching the path parameter.
 */
export async function add(
  eventId: string,
  data: AddParticipantRequest
): Promise<ParticipantsResponse> {
  return apiFetch<ParticipantsResponse>(
    `/events/${encodeURIComponent(eventId)}/participants`,
    {
      method: 'POST',
      body: JSON.stringify(data)
    }
  )
}
