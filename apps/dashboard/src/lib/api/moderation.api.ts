import type {
  ContentReport,
  CreateContentReportRequest,
  CreateModerationDecisionRequest,
  CreateUserBlockRequest,
  ModerationDecision,
  UserBlock,
  UserBlocksResponse
} from '$lib/types/api'
import { apiFetch } from './client'

/**
 * Report a piece of content (comment, chat message, event) or a user.
 *
 * Any authenticated user with access to the event can report.
 *
 * Backend: POST /api/moderation/reports
 */
export async function reportContent(data: CreateContentReportRequest): Promise<ContentReport> {
  return apiFetch<ContentReport>('/moderation/reports', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

/**
 * Record a moderation decision (hide, restore, reject…) on a report.
 *
 * Backend authorization: requires the MODERATOR or ADMIN role
 * (see ModerationRoutes.canModerate — the organizer role is NOT sufficient).
 *
 * Backend: POST /api/moderation/reports/{reportId}/decisions
 */
export async function decide(
  reportId: string,
  data: CreateModerationDecisionRequest
): Promise<ModerationDecision> {
  return apiFetch<ModerationDecision>(
    `/moderation/reports/${encodeURIComponent(reportId)}/decisions`,
    { method: 'POST', body: JSON.stringify(data) }
  )
}

/**
 * List the users blocked by the current user.
 *
 * Backend: GET /api/moderation/blocks
 */
export async function listBlocks(): Promise<UserBlocksResponse> {
  return apiFetch<UserBlocksResponse>('/moderation/blocks')
}

/**
 * Block a user, optionally scoped to an event.
 *
 * Backend: POST /api/moderation/blocks
 */
export async function blockUser(data: CreateUserBlockRequest): Promise<UserBlock> {
  return apiFetch<UserBlock>('/moderation/blocks', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

/**
 * Unblock a previously blocked user.
 *
 * Backend: DELETE /api/moderation/blocks/{blockedUserId}?eventId=
 */
export async function unblockUser(blockedUserId: string, eventId?: string): Promise<void> {
  const query = eventId ? `?eventId=${encodeURIComponent(eventId)}` : ''
  return apiFetch<void>(
    `/moderation/blocks/${encodeURIComponent(blockedUserId)}${query}`,
    { method: 'DELETE' }
  )
}

const TOKEN_KEY = 'wakeve_access_token'

/**
 * Decode a JWT payload without verifying the signature (read-only, client side).
 * Returns null when the token is missing or malformed.
 */
function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const base64 = token.split('.')[1]
    if (!base64) return null
    const normalized = base64.replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(atob(normalized)) as Record<string, unknown>
  } catch {
    return null
  }
}

/**
 * Whether the current user holds a role allowed to record moderation
 * decisions on the backend (MODERATOR or ADMIN — see ModerationRoutes.canModerate).
 * Used to reveal moderation actions in the UI; the backend re-checks anyway.
 */
export function hasModerationRole(): boolean {
  if (typeof localStorage === 'undefined') return false
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) return false
  const payload = decodeJwtPayload(token)
  const role = typeof payload?.role === 'string' ? payload.role.toUpperCase() : ''
  return role === 'MODERATOR' || role === 'ADMIN'
}
