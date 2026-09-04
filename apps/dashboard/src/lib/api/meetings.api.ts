import type {
  CreateGoogleMeetRequest,
  CreateGoogleMeetResponse,
  CreateZoomMeetingRequest,
  CreateZoomMeetingResponse,
  MeetingsListResponse,
  ZoomCancelResponse,
  ZoomMeetingStatusResponse
} from '$lib/types/api'
import { apiFetch } from './client'

/**
 * Fetch all virtual meetings attached to an event.
 * GET /api/events/{eventId}/meetings
 */
export async function list(eventId: string): Promise<MeetingsListResponse> {
  return apiFetch<MeetingsListResponse>(`/events/${encodeURIComponent(eventId)}/meetings`)
}

/**
 * Create a Zoom meeting via the server proxy.
 * POST /api/meetings/proxy/zoom/create
 */
export async function createZoom(data: CreateZoomMeetingRequest): Promise<CreateZoomMeetingResponse> {
  return apiFetch<CreateZoomMeetingResponse>('/meetings/proxy/zoom/create', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

/**
 * Create a Google Meet meeting via the server proxy.
 * POST /api/meetings/proxy/google-meet/create
 */
export async function createGoogleMeet(
  data: CreateGoogleMeetRequest
): Promise<CreateGoogleMeetResponse> {
  return apiFetch<CreateGoogleMeetResponse>('/meetings/proxy/google-meet/create', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

/**
 * Fetch the live status of a Zoom meeting via the server proxy.
 * GET /api/meetings/proxy/zoom/{meetingId}/status
 */
export async function zoomStatus(meetingId: string): Promise<ZoomMeetingStatusResponse> {
  return apiFetch<ZoomMeetingStatusResponse>(
    `/meetings/proxy/zoom/${encodeURIComponent(meetingId)}/status`
  )
}

/**
 * Cancel a Zoom meeting via the server proxy.
 * POST /api/meetings/proxy/zoom/{meetingId}/cancel
 */
export async function zoomCancel(meetingId: string): Promise<ZoomCancelResponse> {
  return apiFetch<ZoomCancelResponse>(`/meetings/proxy/zoom/${encodeURIComponent(meetingId)}/cancel`, {
    method: 'POST'
  })
}
