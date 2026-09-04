import type {
  AddParticipantRequest,
  ParticipantRsvpRequest,
  ParticipantRsvpResponse,
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

/**
 * Record or update a participant's RSVP (CONFIRMED / DECLINED / TENTATIVE)
 * for the retained date. The `slotId` must match the confirmed retained slot.
 *
 * Allowed for the participant themselves and for the event organizer.
 * There is no DELETE endpoint for participants on the backend.
 */
export async function rsvp(
  eventId: string,
  userId: string,
  data: ParticipantRsvpRequest
): Promise<ParticipantRsvpResponse> {
  return apiFetch<ParticipantRsvpResponse>(
    `/events/${encodeURIComponent(eventId)}/participants/${encodeURIComponent(userId)}/rsvp`,
    {
      method: 'POST',
      body: JSON.stringify(data)
    }
  )
}
