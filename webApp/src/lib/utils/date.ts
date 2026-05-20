/**
 * Format an ISO date string as a localised date (no time component).
 *
 * @example formatDate('2025-06-15T10:30:00Z') // "15 juin 2025"
 */
export function formatDate(iso: string, locale = 'fr-FR'): string {
  const date = new Date(iso)
  if (isNaN(date.getTime())) return '—'
  return date.toLocaleDateString(locale, {
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  })
}

/**
 * Format an ISO date string as a localised date + time string.
 *
 * @example formatDateTime('2025-06-15T10:30:00Z') // "15 juin 2025 à 12:30"
 */
export function formatDateTime(iso: string, locale = 'fr-FR'): string {
  const date = new Date(iso)
  if (isNaN(date.getTime())) return '—'
  return date.toLocaleString(locale, {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

/**
 * Combine a date string ("YYYY-MM-DD") and a time string ("HH:MM") into
 * a full UTC ISO-8601 timestamp.
 *
 * @example toISOLocal('2025-06-15', '14:30') // "2025-06-15T12:30:00.000Z" (UTC)
 */
export function toISOLocal(date: string, time: string): string {
  return new Date(`${date}T${time}`).toISOString()
}

const THRESHOLDS: { unit: Intl.RelativeTimeFormatUnit; ms: number }[] = [
  { unit: 'second', ms: 60_000 },
  { unit: 'minute', ms: 3_600_000 },
  { unit: 'hour', ms: 86_400_000 },
  { unit: 'day', ms: 604_800_000 },
  { unit: 'week', ms: 2_592_000_000 },
  { unit: 'month', ms: 31_536_000_000 },
  { unit: 'year', ms: Infinity }
]

const DIVISORS: Record<Intl.RelativeTimeFormatUnit, number> = {
  second: 1_000,
  minute: 60_000,
  hour: 3_600_000,
  day: 86_400_000,
  week: 604_800_000,
  month: 2_592_000_000,
  year: 31_536_000_000,
  quarter: 7_776_000_000
}

/**
 * Return a human-readable relative time string (e.g. "il y a 3 heures").
 *
 * @example timeAgo('2025-06-14T08:00:00Z') // "il y a 2 jours"
 */
export function timeAgo(iso: string, locale = 'fr'): string {
  const date = new Date(iso)
  if (isNaN(date.getTime())) return '—'

  const diffMs = date.getTime() - Date.now()
  const absDiff = Math.abs(diffMs)

  const threshold = THRESHOLDS.find((t) => absDiff < t.ms)
  const unit: Intl.RelativeTimeFormatUnit = threshold?.unit ?? 'year'
  const divisor = DIVISORS[unit]
  const value = Math.round(diffMs / divisor)

  return new Intl.RelativeTimeFormat(locale, { numeric: 'auto' }).format(value, unit)
}

/**
 * Return true when the given ISO date string is in the past.
 */
export function isExpired(iso: string): boolean {
  const date = new Date(iso)
  if (isNaN(date.getTime())) return false
  return date.getTime() < Date.now()
}
