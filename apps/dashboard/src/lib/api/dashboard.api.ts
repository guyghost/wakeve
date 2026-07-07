import type {
  DashboardEventsResponse,
  DashboardOverviewResponse,
  DashboardEventItem,
  DashboardDeadlineState,
  DashboardLifecycleStage,
  DashboardNextAction,
  EventDetailedAnalyticsResponse,
  EventStatus,
  EventType,
  RawDashboardEventItem,
  RawDashboardEventsResponse,
  RawDashboardOverviewResponse,
  RawEventDetailedAnalyticsResponse,
  RawTimeSlotAnalytics,
  RawTimelineEntry,
  TimeSlotAnalytics,
  TimelineEntry
} from '$lib/types/api'
import { apiFetch } from './client'

const EVENT_STATUSES: EventStatus[] = [
  'DRAFT',
  'POLLING',
  'COMPARING',
  'CONFIRMED',
  'ORGANIZING',
  'FINALIZED',
  'EXPIRED',
  'ARCHIVED',
  'DELETED'
]

const EVENT_TYPES: EventType[] = [
  'BIRTHDAY',
  'WEDDING',
  'CORPORATE',
  'TEAM_BUILDING',
  'CONCERT',
  'SPORTS_EVENT',
  'SPORT_EVENT',
  'FAMILY_REUNION',
  'GRADUATION',
  'HOLIDAY_PARTY',
  'NETWORKING',
  'CONFERENCE',
  'WORKSHOP',
  'DINNER_PARTY',
  'OUTDOOR_ADVENTURE',
  'CULTURAL_EVENT',
  'OTHER'
]

function finiteNumber(value: unknown, fallback = 0): number {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback
}

function clamp(value: number, min = 0, max = 1): number {
  return Math.min(max, Math.max(min, value))
}

function normalizeRate(value: unknown): number {
  const raw = finiteNumber(value)
  return clamp(raw > 1 ? raw / 100 : raw)
}

function normalizeStatus(value: unknown): EventStatus {
  return EVENT_STATUSES.includes(value as EventStatus) ? value as EventStatus : 'DRAFT'
}

function normalizeEventType(value: unknown): EventType {
  return EVENT_TYPES.includes(value as EventType) ? value as EventType : 'OTHER'
}

function deadlineState(deadline?: string | null, status?: EventStatus): DashboardDeadlineState {
  if (!deadline || status === 'FINALIZED' || status === 'DELETED' || status === 'ARCHIVED') return 'none'
  const date = new Date(deadline)
  if (Number.isNaN(date.getTime())) return 'none'
  const diffMs = date.getTime() - Date.now()
  if (diffMs < 0) return 'overdue'
  if (diffMs < 48 * 60 * 60 * 1000) return 'soon'
  return 'upcoming'
}

function lifecycleStage(status: EventStatus): DashboardLifecycleStage {
  if (status === 'DRAFT') return 'prepare'
  if (status === 'POLLING' || status === 'COMPARING' || status === 'EXPIRED') return 'decide'
  if (status === 'CONFIRMED' || status === 'ORGANIZING') return 'organize'
  return 'done'
}

function nextActionFor(event: {
  id: string
  status: EventStatus
  deadlineState: DashboardDeadlineState
  responseRate: number
}): DashboardNextAction {
  const href = `/app/events/${encodeURIComponent(event.id)}`

  if (event.status === 'DRAFT') {
    return { kind: 'complete_draft', label: 'Compléter le brouillon', href, urgency: 'medium' }
  }
  if (event.status === 'POLLING' && event.deadlineState === 'overdue') {
    return { kind: 'validate_date', label: 'Valider une date', href, urgency: 'high' }
  }
  if (event.status === 'POLLING') {
    return {
      kind: 'follow_responses',
      label: event.responseRate < 0.8 ? 'Relancer les réponses' : 'Suivre le vote',
      href,
      urgency: event.responseRate < 0.5 ? 'high' : 'medium'
    }
  }
  if (event.status === 'COMPARING') {
    return { kind: 'compare_options', label: 'Comparer les options', href, urgency: 'medium' }
  }
  if (event.status === 'CONFIRMED' || event.status === 'ORGANIZING') {
    return { kind: 'organize', label: 'Préparer l’organisation', href, urgency: 'medium' }
  }
  if (event.status === 'EXPIRED') {
    return { kind: 'validate_date', label: 'Décider de la suite', href, urgency: 'high' }
  }
  if (event.status === 'ARCHIVED' || event.status === 'DELETED') {
    return { kind: 'archive', label: 'Consulter l’archive', href, urgency: 'done' }
  }
  return { kind: 'review', label: 'Voir le récapitulatif', href, urgency: 'done' }
}

function normalizeEvent(raw: RawDashboardEventItem): DashboardEventItem {
  const id = raw.eventId ?? raw.id ?? ''
  const status = normalizeStatus(raw.status)
  const responseRate = normalizeRate(raw.responseRate)
  const state = deadlineState(raw.deadline, status)
  const isArchived = status === 'ARCHIVED' || status === 'DELETED'
  const isVoteClosed = status === 'FINALIZED' || isArchived
  const event: DashboardEventItem = {
    id,
    title: raw.title?.trim() || 'Événement sans titre',
    status,
    type: normalizeEventType(raw.eventType ?? raw.type),
    participantCount: Math.max(0, finiteNumber(raw.participantCount)),
    deadline: raw.deadline ?? undefined,
    createdAt: raw.createdAt ?? new Date(0).toISOString(),
    voteCount: Math.max(0, finiteNumber(raw.voteCount)),
    commentCount: Math.max(0, finiteNumber(raw.commentCount)),
    responseRate,
    responseRatePct: Math.round(responseRate * 100),
    pendingParticipants: 0,
    deadlineState: state,
    lifecycleStage: lifecycleStage(status),
    nextAction: { kind: 'review', label: 'Ouvrir', href: `/app/events/${encodeURIComponent(id)}`, urgency: 'low' },
    isArchived,
    isPollExpired: status === 'EXPIRED' || (status === 'POLLING' && state === 'overdue'),
    isVoteClosed
  } satisfies DashboardEventItem

  event.pendingParticipants = Math.max(0, Math.ceil(event.participantCount * (1 - responseRate)))
  event.nextAction = nextActionFor(event)
  return event
}

function emptyEventsByStatus(): Record<EventStatus, number> {
  return EVENT_STATUSES.reduce((acc, status) => {
    acc[status] = 0
    return acc
  }, {} as Record<EventStatus, number>)
}

function normalizeTimeline(entries?: RawTimelineEntry[]): TimelineEntry[] {
  return (entries ?? [])
    .map((entry) => ({
      date: entry.date ?? '',
      count: Math.max(0, finiteNumber(entry.count))
    }))
    .filter((entry) => entry.date || entry.count > 0)
}

function slotLabel(slot: RawTimeSlotAnalytics): string {
  if (slot.label?.trim()) return slot.label.trim()
  if (slot.startTime || slot.endTime) {
    return [slot.startTime, slot.endTime].filter(Boolean).join(' → ')
  }
  if (slot.timeOfDay) return slot.timeOfDay
  return 'Créneau'
}

function normalizeSlot(slot: RawTimeSlotAnalytics): TimeSlotAnalytics {
  const yesCount = Math.max(0, finiteNumber(slot.yesVotes ?? slot.yesCount))
  const maybeCount = Math.max(0, finiteNumber(slot.maybeVotes ?? slot.maybeCount))
  const noCount = Math.max(0, finiteNumber(slot.noVotes ?? slot.noCount))
  const totalVotes = Math.max(0, finiteNumber(slot.totalVotes, yesCount + maybeCount + noCount))
  return {
    slotId: slot.slotId ?? slot.id ?? slotLabel(slot),
    label: slotLabel(slot),
    yesCount,
    maybeCount,
    noCount,
    totalVotes,
    responseRate: normalizeRate(slot.responseRate)
  }
}

function normalizeOverview(raw: RawDashboardOverviewResponse): DashboardOverviewResponse {
  const eventsByStatus = emptyEventsByStatus()
  Object.entries(raw.eventsByStatus ?? {}).forEach(([status, count]) => {
    eventsByStatus[normalizeStatus(status)] += Math.max(0, finiteNumber(count))
  })

  const totalEvents = Math.max(0, finiteNumber(raw.totalEvents))
  const totalParticipants = Math.max(0, finiteNumber(raw.totalParticipants))
  const totalVotes = Math.max(0, finiteNumber(raw.totalVotes))
  const responseRate = raw.responseRate == null
    ? clamp(totalParticipants > 0 ? totalVotes / totalParticipants : 0)
    : normalizeRate(raw.responseRate)

  return {
    totalEvents,
    totalParticipants,
    averageParticipantsPerEvent: finiteNumber(raw.averageParticipantsPerEvent ?? raw.averageParticipants),
    totalVotes,
    totalComments: Math.max(0, finiteNumber(raw.totalComments)),
    responseRate,
    eventsByStatus,
    activeEvents:
      eventsByStatus.DRAFT +
      eventsByStatus.POLLING +
      eventsByStatus.COMPARING +
      eventsByStatus.CONFIRMED +
      eventsByStatus.ORGANIZING,
    completedEvents: eventsByStatus.FINALIZED + eventsByStatus.ARCHIVED + eventsByStatus.DELETED
  }
}

function normalizeEvents(raw: RawDashboardEventsResponse): DashboardEventsResponse {
  return {
    events: (raw.events ?? []).map(normalizeEvent).filter((event) => event.id),
    total: Math.max(0, finiteNumber(raw.total ?? raw.totalCount)),
    limit: raw.limit,
    offset: raw.offset
  }
}

function normalizeAnalytics(raw: RawEventDetailedAnalyticsResponse): EventDetailedAnalyticsResponse {
  const popularTimeSlots = (raw.popularTimeSlots ?? []).map(normalizeSlot)
  const totalVotes = Math.max(
    0,
    finiteNumber(raw.totalVotes, popularTimeSlots.reduce((sum, slot) => sum + slot.totalVotes, 0))
  )
  const totalParticipants = Math.max(0, finiteNumber(raw.totalParticipants))
  const responseRate = raw.responseRate == null
    ? normalizeRate(raw.pollCompletionRate)
    : normalizeRate(raw.responseRate)
  const votedParticipants = Math.max(
    0,
    finiteNumber(raw.votedParticipants, Math.round(totalParticipants * responseRate))
  )

  return {
    eventId: raw.eventId ?? '',
    title: raw.title?.trim() || 'Analytiques',
    status: normalizeStatus(raw.status),
    totalParticipants,
    votedParticipants,
    pendingParticipants: Math.max(0, totalParticipants - votedParticipants),
    totalVotes,
    responseRate,
    responseRatePct: Math.round(responseRate * 100),
    voteTimeline: normalizeTimeline(raw.voteTimeline),
    participantTimeline: normalizeTimeline(raw.participantTimeline),
    popularTimeSlots,
    commentsBySection: raw.commentsBySection ?? {}
  }
}

/**
 * Fetch the high-level dashboard overview metrics:
 * total events, participants, votes, comments, and response rate.
 */
export async function getOverview(): Promise<DashboardOverviewResponse> {
  const raw = await apiFetch<RawDashboardOverviewResponse>('/dashboard/overview')
  return normalizeOverview(raw)
}

/**
 * Fetch the paginated list of events shown on the dashboard,
 * including per-event vote and comment counts.
 */
export async function getEvents(): Promise<DashboardEventsResponse> {
  const raw = await apiFetch<RawDashboardEventsResponse>('/dashboard/events')
  return normalizeEvents(raw)
}

/**
 * Fetch detailed analytics for a single event:
 * per-slot vote breakdown, response rate, and comments by section.
 */
export async function getEventAnalytics(
  eventId: string
): Promise<EventDetailedAnalyticsResponse> {
  const raw = await apiFetch<RawEventDetailedAnalyticsResponse>(
    `/dashboard/events/${encodeURIComponent(eventId)}/analytics`
  )
  return normalizeAnalytics(raw)
}
