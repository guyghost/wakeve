import type {
  DashboardEventsResponse,
  DashboardOverviewResponse,
  EventDetailedAnalyticsResponse
} from '$lib/types/api'
import { apiFetch } from './client'

/**
 * Fetch the high-level dashboard overview metrics:
 * total events, participants, votes, comments, and response rate.
 */
export async function getOverview(): Promise<DashboardOverviewResponse> {
  return apiFetch<DashboardOverviewResponse>('/dashboard/overview')
}

/**
 * Fetch the paginated list of events shown on the dashboard,
 * including per-event vote and comment counts.
 */
export async function getEvents(): Promise<DashboardEventsResponse> {
  return apiFetch<DashboardEventsResponse>('/dashboard/events')
}

/**
 * Fetch detailed analytics for a single event:
 * per-slot vote breakdown, response rate, and comments by section.
 */
export async function getEventAnalytics(
  eventId: string
): Promise<EventDetailedAnalyticsResponse> {
  return apiFetch<EventDetailedAnalyticsResponse>(
    `/dashboard/events/${encodeURIComponent(eventId)}/analytics`
  )
}
