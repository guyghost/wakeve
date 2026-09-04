import type {
  CreateInvitationRequest,
  DirectInviteBatchRequest,
  DirectInviteBatchResponse,
  DirectInviteCapability,
  InvitationAcceptResponse,
  InvitationResolveResponse,
  InvitationResponse
} from '$lib/types/api'
import { apiFetch } from './client'

/**
 * Generate a shareable invitation link/code for an event (organizer only).
 * Body is optional (expiresAt / maxUses).
 */
export async function createInvitation(
  eventId: string,
  data: CreateInvitationRequest = {}
): Promise<InvitationResponse> {
  return apiFetch<InvitationResponse>(
    `/events/${encodeURIComponent(eventId)}/invite`,
    {
      method: 'POST',
      body: JSON.stringify(data)
    }
  )
}

/**
 * Resolve an invitation code. Public endpoint (no authentication required).
 */
export async function resolveInvitation(code: string): Promise<InvitationResolveResponse> {
  return apiFetch<InvitationResolveResponse>(
    `/invite/${encodeURIComponent(code)}`,
    { method: 'GET' },
    false
  )
}

/**
 * Accept an invitation and join the associated event (authenticated).
 */
export async function acceptInvitation(code: string): Promise<InvitationAcceptResponse> {
  return apiFetch<InvitationAcceptResponse>(
    `/invite/${encodeURIComponent(code)}/accept`,
    { method: 'POST' }
  )
}

/**
 * Check whether direct (email) invitation delivery is available for an event.
 * Returns 403 when the event/actor is not eligible (organizer + DRAFT only)
 * and 503 when the delivery provider is not configured.
 */
export async function getDirectInviteCapability(
  eventId: string
): Promise<DirectInviteCapability> {
  return apiFetch<DirectInviteCapability>(
    `/events/${encodeURIComponent(eventId)}/direct-invites/capability`
  )
}

/**
 * Submit a batch of sealed direct-invite envelopes for delivery.
 * `accessRevision` must match the event's current aggregate revision
 * (a stale value yields 409). Envelopes are opaque to this client:
 * `recipientKey` is a protected digest/identifier and `ciphertext` the
 * sealed payload for the delivery provider.
 */
export async function sendDirectInviteBatch(
  eventId: string,
  data: DirectInviteBatchRequest
): Promise<DirectInviteBatchResponse> {
  return apiFetch<DirectInviteBatchResponse>(
    `/events/${encodeURIComponent(eventId)}/direct-invites/batches`,
    {
      method: 'POST',
      body: JSON.stringify(data)
    }
  )
}
