import type { Comment, CommentSection, CreateCommentRequest } from '$lib/types/api'
import { apiFetch } from './client'

/**
 * Fetch all comments for an event, optionally filtered by section.
 */
export async function list(eventId: string, section?: CommentSection): Promise<Comment[]> {
  const params = new URLSearchParams()
  if (section) params.set('section', section)
  const query = params.size > 0 ? `?${params.toString()}` : ''
  return apiFetch<Comment[]>(`/events/${encodeURIComponent(eventId)}/comments${query}`)
}

/**
 * Post a new comment on an event.
 */
export async function create(
  eventId: string,
  data: CreateCommentRequest
): Promise<Comment> {
  return apiFetch<Comment>(`/events/${encodeURIComponent(eventId)}/comments`, {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

/**
 * Delete a comment by its ID. Returns void (204 No Content).
 */
export async function deleteComment(
  eventId: string,
  commentId: string
): Promise<void> {
  return apiFetch<void>(
    `/events/${encodeURIComponent(eventId)}/comments/${encodeURIComponent(commentId)}`,
    { method: 'DELETE' }
  )
}
