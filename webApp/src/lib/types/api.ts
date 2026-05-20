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

export type EventStatus = 'DRAFT' | 'POLLING' | 'COMPARING' | 'CONFIRMED' | 'ORGANIZING' | 'FINALIZED' | 'EXPIRED' | 'DELETED'
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
export interface AddParticipantRequest { email?: string; displayName: string }
export interface ParticipantDTO { id: string; displayName: string; email?: string; joinedAt: string }
export interface ParticipantsResponse { participants: ParticipantDTO[]; total: number }
export type CommentSection = 'GENERAL' | 'LOGISTICS' | 'BUDGET' | 'ACCOMMODATION' | 'TRANSPORT' | 'MEAL' | 'ACTIVITY' | 'EQUIPMENT' | 'OTHER'
export interface Comment { id: string; authorId: string; authorName: string; content: string; section: CommentSection; isPinned: boolean; createdAt: string; updatedAt: string }
export interface CreateCommentRequest { content: string; section: CommentSection }
export interface DashboardOverviewResponse { totalEvents: number; totalParticipants: number; averageParticipantsPerEvent: number; totalVotes: number; totalComments: number; responseRate: number; eventsByStatus: Record<EventStatus, number> }
export interface DashboardEventItem { id: string; title: string; status: EventStatus; type: EventType; participantCount: number; deadline?: string; createdAt: string; voteCount: number; commentCount: number }
export interface DashboardEventsResponse { events: DashboardEventItem[]; total: number }
export interface TimeSlotAnalytics { slotId: string; label: string; yesCount: number; maybeCount: number; noCount: number; responseRate: number }
export interface EventDetailedAnalyticsResponse { eventId: string; title: string; totalParticipants: number; totalVotes: number; responseRate: number; popularTimeSlots: TimeSlotAnalytics[]; commentsBySection: Record<string, number> }
export interface ErrorResponse { error: string; message: string; statusCode: number }
