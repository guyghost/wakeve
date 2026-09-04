import type {
  BudgetItemsResponse,
  BudgetItem,
  BudgetSettlementsResponse,
  BudgetSummaryResponse,
  CreateBudgetItemRequest
} from '$lib/types/api'
import { apiFetch } from './client'

function basePath(eventId: string): string {
  return `/events/${encodeURIComponent(eventId)}/budget`
}

/**
 * Fetch all budget items for an event.
 */
export async function list(eventId: string): Promise<BudgetItem[]> {
  const response = await apiFetch<BudgetItemsResponse>(`${basePath(eventId)}/items`)
  return response.items
}

/**
 * Fetch the budget summary (baseline totals + per-category breakdown).
 */
export async function summary(eventId: string): Promise<BudgetSummaryResponse> {
  return apiFetch<BudgetSummaryResponse>(`${basePath(eventId)}/summary`)
}

/**
 * Fetch settlement suggestions (who owes what to whom).
 */
export async function settlements(eventId: string): Promise<BudgetSettlementsResponse> {
  return apiFetch<BudgetSettlementsResponse>(`${basePath(eventId)}/settlements`)
}

/**
 * Create a new budget item.
 */
export async function create(
  eventId: string,
  data: CreateBudgetItemRequest
): Promise<BudgetItem> {
  return apiFetch<BudgetItem>(`${basePath(eventId)}/items`, {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

/**
 * Delete a budget item by its ID.
 */
export async function remove(eventId: string, itemId: string): Promise<void> {
  await apiFetch<{ message: string }>(
    `${basePath(eventId)}/items/${encodeURIComponent(itemId)}`,
    { method: 'DELETE' }
  )
}
