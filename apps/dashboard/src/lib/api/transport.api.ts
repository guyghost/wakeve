import type {
  DepartureLocationRecord,
  GenerateTransportPlanRequest,
  SaveDepartureRequest,
  SelectedTransportPlanSummary,
  TransportLocation,
  TransportNotNeededResponse,
  TransportPlan,
  TransportPlansResponse,
  TransportReadiness
} from '$lib/types/api'
import { apiFetch } from './client'

/**
 * Fetch transport planning readiness for an event
 * (missing departures, destination, whether a plan can be generated).
 */
export async function getReadiness(eventId: string): Promise<TransportReadiness> {
  return apiFetch<TransportReadiness>(`/events/${encodeURIComponent(eventId)}/transport/readiness`)
}

/**
 * Fetch all transport plans generated for an event.
 */
export async function listPlans(eventId: string): Promise<TransportPlansResponse> {
  return apiFetch<TransportPlansResponse>(`/events/${encodeURIComponent(eventId)}/transport/plans`)
}

/**
 * Fetch a single transport plan by ID.
 */
export async function getPlan(eventId: string, planId: string): Promise<TransportPlan> {
  return apiFetch<TransportPlan>(
    `/events/${encodeURIComponent(eventId)}/transport/plans/${encodeURIComponent(planId)}`
  )
}

/**
 * Generate a new transport plan from participant departures to the
 * selected destination (organizer only). The destination always comes
 * from the selected event scenario.
 */
export async function generatePlan(
  eventId: string,
  data: GenerateTransportPlanRequest = {}
): Promise<TransportPlan> {
  return apiFetch<TransportPlan>(
    `/events/${encodeURIComponent(eventId)}/transport/plans/generate`,
    { method: 'POST', body: JSON.stringify(data) }
  )
}

/**
 * Select the final transport plan for the event (organizer only).
 */
export async function selectPlan(
  eventId: string,
  planId: string
): Promise<SelectedTransportPlanSummary> {
  return apiFetch<SelectedTransportPlanSummary>(
    `/events/${encodeURIComponent(eventId)}/transport/plans/${encodeURIComponent(planId)}/select`,
    { method: 'POST' }
  )
}

/**
 * Delete a transport plan (organizer only).
 */
export async function deletePlan(eventId: string, planId: string): Promise<void> {
  await apiFetch<void>(
    `/events/${encodeURIComponent(eventId)}/transport/plans/${encodeURIComponent(planId)}`,
    { method: 'DELETE' }
  )
}

/**
 * Mark transport planning as not needed for this event (organizer only).
 */
export async function markNotNeeded(eventId: string): Promise<TransportNotNeededResponse> {
  return apiFetch<TransportNotNeededResponse>(
    `/events/${encodeURIComponent(eventId)}/transport/not-needed`,
    { method: 'POST' }
  )
}

/**
 * Save a participant's departure location (self or organizer).
 */
export async function saveDeparture(
  eventId: string,
  participantId: string,
  location: TransportLocation
): Promise<DepartureLocationRecord> {
  const body: SaveDepartureRequest = { location }
  return apiFetch<DepartureLocationRecord>(
    `/events/${encodeURIComponent(eventId)}/transport/departures/${encodeURIComponent(participantId)}`,
    { method: 'PUT', body: JSON.stringify(body) }
  )
}
