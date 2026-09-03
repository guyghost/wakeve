import type {
  LeaderboardResponse,
  LeaderboardType,
  UserBadgesResponse,
  UserPointsResponse
} from '$lib/types/api'
import { apiFetch } from './client'

/**
 * Get the community leaderboard, ranked by points.
 *
 * Backend: GET /api/leaderboard?limit=&type=
 */
export async function getLeaderboard(
  options: { limit?: number; type?: LeaderboardType } = {}
): Promise<LeaderboardResponse> {
  const params = new URLSearchParams()
  if (options.limit != null) params.set('limit', String(options.limit))
  if (options.type) params.set('type', options.type)
  const query = params.size > 0 ? `?${params.toString()}` : ''
  return apiFetch<LeaderboardResponse>(`/leaderboard${query}`)
}

/**
 * Get the badges earned by a user.
 *
 * The backend only allows reading one's own gamification profile
 * (or an admin reading someone else's).
 *
 * Backend: GET /api/users/{userId}/badges
 */
export async function getUserBadges(userId: string): Promise<UserBadgesResponse> {
  return apiFetch<UserBadgesResponse>(`/users/${encodeURIComponent(userId)}/badges`)
}

/**
 * Get a user's point total, per-action breakdown and level.
 *
 * The backend only allows reading one's own gamification profile
 * (or an admin reading someone else's).
 *
 * Backend: GET /api/users/{userId}/points
 */
export async function getUserPoints(userId: string): Promise<UserPointsResponse> {
  return apiFetch<UserPointsResponse>(`/users/${encodeURIComponent(userId)}/points`)
}
