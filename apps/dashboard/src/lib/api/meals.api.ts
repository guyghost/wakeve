import type {
  CreateMealRequest,
  DailyMealSchedule,
  Meal,
  MealPlanningSummary
} from '$lib/types/api'
import { apiFetch } from './client'

/**
 * Fetch all meals planned for an event.
 */
export async function list(eventId: string): Promise<Meal[]> {
  return apiFetch<Meal[]>(`/events/${encodeURIComponent(eventId)}/meals`)
}

/**
 * Fetch the daily meal schedule (meals grouped by date).
 */
export async function getSchedule(eventId: string): Promise<DailyMealSchedule[]> {
  return apiFetch<DailyMealSchedule[]>(`/events/${encodeURIComponent(eventId)}/meals/schedule`)
}

/**
 * Fetch the meal planning summary (totals, costs, counts by type/status).
 */
export async function getSummary(eventId: string): Promise<MealPlanningSummary> {
  return apiFetch<MealPlanningSummary>(`/events/${encodeURIComponent(eventId)}/meals/summary`)
}

/**
 * Fetch upcoming meals for an event.
 */
export async function getUpcoming(eventId: string, limit = 10): Promise<Meal[]> {
  return apiFetch<Meal[]>(
    `/events/${encodeURIComponent(eventId)}/meals/upcoming?limit=${limit}`
  )
}

/**
 * Create a new meal for an event (organizer only).
 */
export async function create(eventId: string, data: CreateMealRequest): Promise<Meal> {
  return apiFetch<Meal>(`/events/${encodeURIComponent(eventId)}/meals`, {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

/**
 * Delete a meal (organizer only).
 */
export async function remove(eventId: string, mealId: string): Promise<void> {
  await apiFetch<void>(
    `/events/${encodeURIComponent(eventId)}/meals/${encodeURIComponent(mealId)}`,
    { method: 'DELETE' }
  )
}

/**
 * Fetch dietary restriction counts per participant for an event.
 */
export async function getDietaryCounts(
  eventId: string
): Promise<Partial<Record<string, number>>> {
  return apiFetch<Partial<Record<string, number>>>(
    `/events/${encodeURIComponent(eventId)}/dietary-restrictions/counts`
  )
}
