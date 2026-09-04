// ============================================================
// Auth
// ============================================================
export interface EmailOTPRequest { email: string }
export interface EmailOTPVerifyRequest { email: string; otp: string; deviceId: string }
export interface GuestSessionRequest { deviceId: string; displayName?: string }
export interface OTPRequestResponse { message: string; expiresIn: number }
export interface UserDTO {
  id: string
  email?: string
  displayName: string
  authMethod: 'EMAIL_OTP' | 'GOOGLE' | 'APPLE' | 'GUEST'
  accountType: 'GUEST' | 'REGISTERED'
  createdAt: string
}
export interface AuthResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: UserDTO
}
export interface TokenRefreshRequest { refreshToken: string }
export interface TokenRefreshResponse { accessToken: string; expiresIn: number }
export interface AuthErrorResponse { error: string; message: string }

export type EventStatus = 'DRAFT' | 'POLLING' | 'COMPARING' | 'CONFIRMED' | 'ORGANIZING' | 'FINALIZED' | 'EXPIRED' | 'ARCHIVED' | 'DELETED'
export type EventType = 'BIRTHDAY' | 'WEDDING' | 'CORPORATE' | 'TEAM_BUILDING' | 'CONCERT' | 'SPORTS_EVENT' | 'SPORT_EVENT' | 'FAMILY_REUNION' | 'GRADUATION' | 'HOLIDAY_PARTY' | 'NETWORKING' | 'CONFERENCE' | 'WORKSHOP' | 'DINNER_PARTY' | 'OUTDOOR_ADVENTURE' | 'CULTURAL_EVENT' | 'OTHER'
export type TimeOfDay = 'SPECIFIC' | 'ALL_DAY' | 'MORNING' | 'AFTERNOON' | 'EVENING'
export type VoteValue = 'YES' | 'MAYBE' | 'NO'

export interface TimeSlotResponse { id: string; startTime?: string; endTime?: string; timeOfDay: TimeOfDay; label?: string; voteCount?: number }
export interface EventResponse { id: string; title: string; description?: string; status: EventStatus; type: EventType; expectedParticipants?: number; deadline?: string; finalDate?: string; createdAt: string; updatedAt: string; organizerId: string; participantCount: number; proposedSlots: TimeSlotResponse[] }
export interface EventsListResponse { events: EventResponse[]; total: number }
export interface CreateTimeSlotRequest { start: string | null; end: string | null; timeOfDay: TimeOfDay; label?: string }
export interface CreateEventRequest { title: string; description?: string; type: EventType; expectedParticipants?: number; deadline?: string; timezone: string; proposedSlots: CreateTimeSlotRequest[] }
export interface UpdateEventStatusRequest { status: EventStatus }
export interface PollResponse { eventId: string; slots: TimeSlotResponse[]; votes: Record<string, Record<string, string>>; participantCount: number }
export interface AddVoteRequest { participantId: string; votes: Record<string, VoteValue> }
export interface AddParticipantRequest { eventId: string; participantId: string }
export interface ParticipantDTO { id: string; displayName: string; email?: string; joinedAt: string }
export interface ParticipantsResponse { participants: string[] }
export type CommentSection = 'GENERAL' | 'LOGISTICS' | 'BUDGET' | 'ACCOMMODATION' | 'TRANSPORT' | 'MEAL' | 'ACTIVITY' | 'EQUIPMENT' | 'OTHER'
export interface Comment { id: string; authorId: string; authorName: string; content: string; section: CommentSection; isPinned: boolean; createdAt: string; updatedAt: string }
export interface CreateCommentRequest { content: string; section: CommentSection }
export interface RawDashboardOverviewResponse { totalEvents?: number; totalParticipants?: number; averageParticipants?: number; averageParticipantsPerEvent?: number; totalVotes?: number; totalComments?: number; responseRate?: number; eventsByStatus?: Record<string, number> }
export interface DashboardOverviewResponse { totalEvents: number; totalParticipants: number; averageParticipantsPerEvent: number; totalVotes: number; totalComments: number; responseRate: number; eventsByStatus: Record<EventStatus, number>; activeEvents: number; completedEvents: number }
export interface RawDashboardEventItem { eventId?: string; id?: string; title?: string; status?: string; eventType?: string | null; type?: string | null; createdAt?: string; deadline?: string | null; participantCount?: number; voteCount?: number; commentCount?: number; responseRate?: number }
export type DashboardLifecycleStage = 'prepare' | 'decide' | 'organize' | 'done'
export type DashboardDeadlineState = 'none' | 'upcoming' | 'soon' | 'overdue'
export type DashboardNextActionKind = 'complete_draft' | 'validate_date' | 'follow_responses' | 'compare_options' | 'organize' | 'review' | 'archive'
export interface DashboardNextAction { kind: DashboardNextActionKind; label: string; href: string; urgency: 'low' | 'medium' | 'high' | 'done' }
export interface DashboardEventItem { id: string; title: string; status: EventStatus; type: EventType; participantCount: number; deadline?: string; createdAt: string; voteCount: number; commentCount: number; responseRate: number; responseRatePct: number; pendingParticipants: number; deadlineState: DashboardDeadlineState; lifecycleStage: DashboardLifecycleStage; nextAction: DashboardNextAction; isArchived: boolean; isPollExpired: boolean; isVoteClosed: boolean }
export interface RawDashboardEventsResponse { events?: RawDashboardEventItem[]; total?: number; totalCount?: number; limit?: number; offset?: number }
export interface DashboardEventsResponse { events: DashboardEventItem[]; total: number; limit?: number; offset?: number }
export interface RawTimelineEntry { date?: string; count?: number }
export interface RawTimeSlotAnalytics { slotId?: string; id?: string; label?: string; startTime?: string | null; endTime?: string | null; timeOfDay?: string | null; yesVotes?: number; maybeVotes?: number; noVotes?: number; yesCount?: number; maybeCount?: number; noCount?: number; totalVotes?: number; responseRate?: number }
export interface TimeSlotAnalytics { slotId: string; label: string; yesCount: number; maybeCount: number; noCount: number; totalVotes: number; responseRate: number }
export interface TimelineEntry { date: string; count: number }
export interface RawEventDetailedAnalyticsResponse { eventId?: string; title?: string; status?: string; voteTimeline?: RawTimelineEntry[]; participantTimeline?: RawTimelineEntry[]; popularTimeSlots?: RawTimeSlotAnalytics[]; pollCompletionRate?: number; totalParticipants?: number; votedParticipants?: number; totalVotes?: number; responseRate?: number; commentsBySection?: Record<string, number> }
export interface EventDetailedAnalyticsResponse { eventId: string; title: string; status: EventStatus; totalParticipants: number; votedParticipants: number; pendingParticipants: number; totalVotes: number; responseRate: number; responseRatePct: number; voteTimeline: TimelineEntry[]; participantTimeline: TimelineEntry[]; popularTimeSlots: TimeSlotAnalytics[]; commentsBySection: Record<string, number> }
// ============================================================
// Scenarios
// ============================================================
export type ScenarioStatus = 'DRAFT' | 'PROPOSED' | 'SELECTED' | 'REJECTED'
export type ScenarioGenerationType = 'MANUAL' | 'MATRIX'
export type ScenarioVoteType = 'PREFER' | 'NEUTRAL' | 'AGAINST'
export interface ScenarioResponse { id: string; eventId: string; name: string; dateOrPeriod: string; location: string; duration: number; estimatedParticipants: number; estimatedBudgetPerPerson: number; description: string; status: ScenarioStatus; createdAt: string; updatedAt: string; sourceTimeSlotId?: string | null; sourcePotentialLocationId?: string | null; generationType: ScenarioGenerationType }
export interface CreateScenarioRequest { eventId: string; name: string; dateOrPeriod: string; location: string; duration: number; estimatedParticipants: number; estimatedBudgetPerPerson: number; description: string; sourceTimeSlotId?: string; sourcePotentialLocationId?: string; generationType?: ScenarioGenerationType }
export interface ScenarioVoteRequest { participantId: string; vote: ScenarioVoteType }
export interface ScenarioVoteResponse { id: string; scenarioId: string; participantId: string; vote: ScenarioVoteType; createdAt: string }
export interface ScenarioVotingResult { scenarioId: string; preferCount: number; neutralCount: number; againstCount: number; totalVotes: number; score: number; preferPercentage: number; neutralPercentage: number; againstPercentage: number }
export interface ScenarioWithVotesResponse { scenario: ScenarioResponse; votes: ScenarioVoteResponse[]; result: ScenarioVotingResult }
export interface ScenariosListResponse { scenarios: ScenarioResponse[] }
export interface ScenariosWithVotesResponse { scenarios: ScenarioWithVotesResponse[] }

// ============================================================
// Transport
// ============================================================
export type OptimizationType = 'COST_MINIMIZE' | 'TIME_MINIMIZE' | 'BALANCED'
export interface TransportLocation { name: string; address?: string | null; latitude?: number | null; longitude?: number | null; iataCode?: string | null }
export interface TransportOption { id: string; mode: string; provider: string; departure: TransportLocation; arrival: TransportLocation; departureTime: string; arrivalTime: string; durationMinutes: number; cost: number; currency: string; stops?: TransportLocation[]; bookingUrl?: string | null }
export interface TransportRoute { id: string; segments: TransportOption[]; totalDurationMinutes: number; totalCost: number; currency: string; score: number }
export interface TransportPlan { id: string; eventId: string; participantRoutes: Record<string, TransportRoute>; groupArrivals: string[]; totalGroupCost: number; optimizationType: OptimizationType; createdAt: string }
export interface TransportPlansResponse { plans: TransportPlan[] }
export interface TransportReadiness { eventId: string; destination: TransportLocation; isComplete: boolean; canGeneratePlan: boolean; transportNotNeeded: boolean; canFinalizeWithoutPlan: boolean; missingDepartureParticipantIds: string[]; missingDepartureParticipantNames: string[] }
export interface SelectedTransportPlanSummary { eventId: string; planId: string; totalCost: number; optimizationType: OptimizationType; selectedAt: string; readiness: TransportReadiness }
export interface DepartureLocationRecord { eventId: string; participantId: string; location: TransportLocation; updatedByUserId: string; updatedAt: string }
export interface SaveDepartureRequest { participantId?: string; location: TransportLocation }
export interface GenerateTransportPlanRequest { optimizationType?: OptimizationType }
export interface TransportNotNeededResponse { transportNotNeeded: boolean }

// ============================================================
// Meals
// ============================================================
export type MealType = 'BREAKFAST' | 'LUNCH' | 'DINNER' | 'SNACK' | 'APERITIF'
export type MealStatus = 'PLANNED' | 'ASSIGNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
export type DietaryRestriction = 'VEGETARIAN' | 'VEGAN' | 'GLUTEN_FREE' | 'LACTOSE_INTOLERANT' | 'NUT_ALLERGY' | 'SHELLFISH_ALLERGY' | 'KOSHER' | 'HALAL' | 'DIABETIC' | 'OTHER'
export interface Meal { id: string; eventId: string; type: MealType; name: string; date: string; time: string; location?: string | null; responsibleParticipantIds: string[]; estimatedCost: number; actualCost?: number | null; servings: number; status: MealStatus; notes?: string | null; createdAt: string; updatedAt: string }
export interface CreateMealRequest { eventId: string; type: MealType; name: string; date: string; time: string; location?: string; responsibleParticipantIds: string[]; estimatedCost: number; actualCost?: number | null; servings: number; status?: MealStatus; notes?: string }
export interface DailyMealSchedule { date: string; meals: Meal[] }
export interface MealPlanningSummary { totalMeals: number; totalEstimatedCost: number; totalActualCost: number; mealsCompleted: number; mealsRemaining: number; mealsByType: Partial<Record<MealType, number>>; mealsByStatus: Partial<Record<MealStatus, number>> }

export interface ErrorResponse { error: string; message: string; statusCode: number }


// ============================================================
// Budget
// ============================================================
export type BudgetCategory = 'TRANSPORT' | 'ACCOMMODATION' | 'MEALS' | 'ACTIVITIES' | 'EQUIPMENT' | 'OTHER'
export interface Budget {
  id: string
  eventId: string
  totalEstimated: number
  totalActual: number
  transportEstimated: number
  transportActual: number
  accommodationEstimated: number
  accommodationActual: number
  mealsEstimated: number
  mealsActual: number
  activitiesEstimated: number
  activitiesActual: number
  equipmentEstimated: number
  equipmentActual: number
  otherEstimated: number
  otherActual: number
  createdAt: string
  updatedAt: string
}
export interface BudgetItem {
  id: string
  budgetId: string
  category: BudgetCategory
  name: string
  description: string
  estimatedCost: number
  actualCost: number
  isPaid: boolean
  paidBy: string | null
  sharedBy: string[]
  notes: string
  createdAt: string
  updatedAt: string
}
export interface BudgetItemsResponse { items: BudgetItem[]; count: number }
export interface CreateBudgetItemRequest {
  name: string
  description?: string
  category: BudgetCategory
  estimatedCost: number
  sharedBy?: string[]
}
export interface BudgetSummaryResponse { budget: Budget; summary: string; itemCount: number }
export interface SettlementRecord {
  settlementId: string
  eventId: string
  budgetId: string
  fromParticipantId: string
  toParticipantId: string
  amount: number
  status: string
  createdAt: string
  updatedAt: string
}
export interface BudgetSettlementsResponse { settlements: SettlementRecord[]; count: number }


// ============================================================
// Meetings (virtual meetings — Zoom / Google Meet)
// ============================================================
export type MeetingPlatform = 'ZOOM' | 'GOOGLE_MEET' | 'FACETIME'
export type MeetingStatus = 'SCHEDULED' | 'STARTED' | 'ENDED' | 'CANCELLED'
/** Mirror of the SQLDelight `meeting` row returned by GET /api/events/{eventId}/meetings */
export interface MeetingDTO { id: string; eventId: string; organizerId: string; title: string; description?: string; startTime: string; duration: string; platform: MeetingPlatform; meetingLink: string; provider: string; displayLabel: string; targetUrl: string; creatorId: string; verificationState: string; hostMeetingId: string; password: string; invitedParticipants: string; status: MeetingStatus; createdAt: string }
export interface MeetingsListResponse { meetings: MeetingDTO[]; count: number }
export interface CreateZoomMeetingRequest { eventId?: string; title: string; description?: string; scheduledFor: string; duration: number; timezone?: string; participantLimit?: number; requirePassword?: boolean; waitingRoom?: boolean }
export interface CreateZoomMeetingResponse { meetingId: string; joinUrl: string; password: string; hostUrl: string; hostKey: string; dialInNumber: string; dialInPassword: string }
export interface ZoomMeetingStatusResponse { meetingId: string; status: string; startTime: string; duration: number; participantCount: number }
export interface ZoomCancelResponse { success: boolean; message: string }
export interface CreateGoogleMeetRequest { eventId?: string; title: string; description?: string; scheduledFor: string; duration: number; timezone?: string }
export interface CreateGoogleMeetResponse { meetingUrl: string; meetingCode: string }

// ============================================================
// Notifications
// ============================================================
/** Notification type as returned by GET /api/notifications (server enum NotificationType). */
export type NotificationType =
  | 'DEADLINE_REMINDER'
  | 'EVENT_UPDATE'
  | 'VOTE_CLOSE_REMINDER'
  | 'EVENT_CONFIRMED'
  | 'PARTICIPANT_JOINED'
  | 'VOTE_SUBMITTED'
  | 'COMMENT_POSTED'
  | 'COMMENT_REPLY'
  | 'MENTION'

/**
 * A notification item (server NotificationMessage).
 * `readAt` is null while the notification is unread.
 */
export interface Notification {
  id: string
  userId: string
  type: NotificationType
  title: string
  body: string
  data: Record<string, string>
  sentAt: string | null
  readAt: string | null
}

/**
 * Preference type as accepted by GET/PUT /api/notifications/preferences
 * (server enum NotificationType from the notification package).
 */
export type NotificationPreferenceType =
  | 'EVENT_INVITE'
  | 'VOTE_REMINDER'
  | 'DATE_CONFIRMED'
  | 'NEW_SCENARIO'
  | 'SCENARIO_SELECTED'
  | 'NEW_COMMENT'
  | 'MENTION'
  | 'MEETING_REMINDER'
  | 'PAYMENT_DUE'
  | 'EVENT_UPDATE'
  | 'VOTE_CLOSE_REMINDER'
  | 'DEADLINE_REMINDER'

/** Quiet time bound (server QuietTime). */
export interface QuietTime {
  hour: number
  minute: number
}

/**
 * Notification preferences (server NotificationPreferences).
 * Also used as the PUT /api/notifications/preferences body: the server
 * replaces the stored preferences with the payload, so every field
 * (including userId and updatedAt) must be sent.
 */
export interface NotificationPreferences {
  userId: string
  enabledTypes: NotificationPreferenceType[]
  quietHoursStart: QuietTime | null
  quietHoursEnd: QuietTime | null
  soundEnabled: boolean
  vibrationEnabled: boolean
  updatedAt: string
}

// ── Participant RSVP (ParticipantRoutes.kt) ──
export type RsvpAttendance = 'CONFIRMED' | 'DECLINED' | 'TENTATIVE'
export type RsvpState = 'ACCEPTED' | 'DECLINED' | 'PENDING'
export type DateValidationState = 'VALIDATED_RETAINED_DATE' | 'NOT_VALIDATED'
export interface ParticipantRsvpRequest { slotId: string; attendance: RsvpAttendance }
export interface ParticipantRsvpResponse { eventId: string; userId: string; slotId: string; attendance: RsvpAttendance; hasValidatedDate: boolean; rsvpState: RsvpState; dateValidationState: DateValidationState }

// ── Invitations (InvitationRoutes.kt) ──
export interface CreateInvitationRequest { expiresAt?: string; maxUses?: number }
export interface InvitationResponse { id: string; code: string; eventId: string; createdBy: string; expiresAt?: string | null; maxUses?: number | null; currentUses: number; createdAt: string; inviteUrl: string; deepLinkUrl: string }
export interface InvitationResolveResponse { code: string; eventId: string; eventTitle: string; eventDescription: string; eventStatus: string; organizerId: string; participantCount: number; isValid: boolean; expiresAt?: string | null }
export interface InvitationAcceptResponse { success: boolean; eventId: string; message: string }

// ── Direct invite delivery (DirectInviteDeliveryRoutes.kt) ──
export interface DirectInviteCapability { state: string; eventId: string; actorId: string; accessRevision: number; sealingPublicKey?: string | null; keyVersion?: number | null }
export interface DirectInviteEnvelope { recipientKey: string; ciphertext: string; keyVersion: number; expiresAt: string }
export interface DirectInviteBatchRequest { accessRevision: number; batchId: string; operationId: string; envelopes: DirectInviteEnvelope[] }
export type DirectInviteOutcomeStatus = 'SERVER_ACCEPTED' | 'INVALID' | 'FAILED' | 'CANCELLED'
export interface DirectInviteOutcome { recipientKey: string; status: DirectInviteOutcomeStatus; invitationId?: string | null; reasonCode?: string | null }
export interface DirectInviteBatchResponse { batchId: string; operationId: string; status: string; outcomes: DirectInviteOutcome[] }
