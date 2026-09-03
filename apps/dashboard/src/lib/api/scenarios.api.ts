import type {
  CreateScenarioRequest,
  ScenarioResponse,
  ScenarioVoteRequest,
  ScenarioVoteResponse,
  ScenarioVotingResult,
  ScenariosListResponse,
  ScenariosWithVotesResponse
} from '$lib/types/api'
import { apiFetch } from './client'

/**
 * Fetch all scenarios for an event, with votes and aggregated results.
 */
export async function listWithVotes(eventId: string): Promise<ScenariosWithVotesResponse> {
  return apiFetch<ScenariosWithVotesResponse>(
    `/events/${encodeURIComponent(eventId)}/scenarios/with-votes`
  )
}

/**
 * Fetch all scenarios for an event (without votes).
 */
export async function list(eventId: string): Promise<ScenariosListResponse> {
  return apiFetch<ScenariosListResponse>(`/events/${encodeURIComponent(eventId)}/scenarios`)
}

/**
 * Create a new scenario (destination + lodging) for an event (organizer only).
 */
export async function create(
  eventId: string,
  data: CreateScenarioRequest
): Promise<ScenarioResponse> {
  return apiFetch<ScenarioResponse>(`/events/${encodeURIComponent(eventId)}/scenarios`, {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

/**
 * Cast (or update) the current user's vote on a scenario.
 */
export async function vote(
  eventId: string,
  scenarioId: string,
  data: ScenarioVoteRequest
): Promise<ScenarioVoteResponse> {
  return apiFetch<ScenarioVoteResponse>(
    `/events/${encodeURIComponent(eventId)}/scenarios/${encodeURIComponent(scenarioId)}/vote`,
    { method: 'POST', body: JSON.stringify(data) }
  )
}

/**
 * Get aggregated voting results for a scenario.
 */
export async function getVotes(
  eventId: string,
  scenarioId: string
): Promise<ScenarioVotingResult> {
  return apiFetch<ScenarioVotingResult>(
    `/events/${encodeURIComponent(eventId)}/scenarios/${encodeURIComponent(scenarioId)}/votes`
  )
}

/**
 * Select a matrix scenario as the final one (organizer only).
 */
export async function selectFinal(
  eventId: string,
  scenarioId: string
): Promise<{ status: string }> {
  return apiFetch<{ status: string }>(
    `/events/${encodeURIComponent(
      eventId
    )}/scenarios/${encodeURIComponent(scenarioId)}/select-final`,
    { method: 'POST' }
  )
}

/**
 * Generate a draft scenario matrix from confirmed slots and locations (organizer only).
 */
export async function generateMatrix(eventId: string): Promise<ScenariosListResponse> {
  return apiFetch<ScenariosListResponse>(
    `/events/${encodeURIComponent(eventId)}/scenarios/matrix/generate`,
    { method: 'POST' }
  )
}

/**
 * Publish the draft scenario matrix so participants can vote (organizer only).
 */
export async function publishMatrix(eventId: string): Promise<{ status: string }> {
  return apiFetch<{ status: string }>(
    `/events/${encodeURIComponent(eventId)}/scenarios/matrix/publish`,
    { method: 'POST' }
  )
}
