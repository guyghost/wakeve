// ============================================================
// Auth types
// ============================================================

export interface EmailOTPRequest {
  email: string;
}

export interface EmailOTPVerifyRequest {
  email: string;
  otp: string;
}

export interface GuestSessionRequest {
  deviceId?: string;
}

export interface OTPRequestResponse {
  success: boolean;
  message: string;
  expiresInSeconds: number;
}

export interface AuthResponse {
  user: UserDTO;
  accessToken: string;
  refreshToken?: string;
  expiresInSeconds: number;
}

export interface UserDTO {
  id: string;
  email: string | null;
  name: string | null;
  isGuest: boolean;
  authMethod?: string;
}

export interface TokenRefreshRequest {
  refreshToken: string;
}

export interface TokenRefreshResponse {
  accessToken: string;
  refreshToken?: string;
  expiresInSeconds: number;
}

export interface AuthErrorResponse {
  error: string;
  message: string;
  details?: string;
}

// ============================================================
// Event types
// ============================================================

export type EventStatus =
  | 'DRAFT'
  | 'POLLING'
  | 'COMPARING'
  | 'CONFIRMED'
  | 'ORGANIZING'
  | 'FINALIZED';

export type EventType =
  | 'BIRTHDAY'
  | 'WEDDING'
  | 'TEAM_BUILDING'
  | 'CONFERENCE'
  | 'WORKSHOP'
  | 'PARTY'
  | 'SPORTS_EVENT'
  | 'CULTURAL_EVENT'
  | 'FAMILY_GATHERING'
  | 'SPORT_EVENT'
  | 'OUTDOOR_ACTIVITY'
  | 'FOOD_TASTING'
  | 'TECH_MEETUP'
  | 'WELLNESS_EVENT'
  | 'CREATIVE_WORKSHOP'
  | 'OTHER'
  | 'CUSTOM';

export type TimeOfDay = 'ALL_DAY' | 'MORNING' | 'AFTERNOON' | 'EVENING' | 'SPECIFIC';

export type VoteValue = 'YES' | 'MAYBE' | 'NO';

export interface TimeSlotResponse {
  id: string;
  start: string | null;
  end: string | null;
  timezone: string;
  timeOfDay?: string;
}

export interface EventResponse {
  id: string;
  title: string;
  description: string;
  organizerId: string;
  participants: string[];
  deadline: string;
  status: EventStatus;
  proposedSlots: TimeSlotResponse[];
  finalDate?: string;
  eventType?: string;
  eventTypeCustom?: string;
  minParticipants?: number;
  maxParticipants?: number;
  expectedParticipants?: number;
}

export interface EventsListResponse {
  events: EventResponse[];
}

export interface CreateEventRequest {
  title: string;
  description: string;
  organizerId: string;
  deadline: string;
  proposedSlots: CreateTimeSlotRequest[];
  eventType?: string;
  eventTypeCustom?: string;
  minParticipants?: number;
  maxParticipants?: number;
  expectedParticipants?: number;
}

export interface CreateTimeSlotRequest {
  id: string;
  start: string | null;
  end: string | null;
  timezone: string;
  timeOfDay?: string;
}

export interface UpdateEventStatusRequest {
  eventId: string;
  status: string;
  finalDate?: string;
}

// ============================================================
// Poll / Vote types
// ============================================================

export interface PollResponse {
  eventId: string;
  /** participantId -> slotId -> vote (YES | MAYBE | NO) */
  votes: Record<string, Record<string, string>>;
}

export interface AddVoteRequest {
  eventId: string;
  participantId: string;
  slotId: string;
  vote: VoteValue;
}

// ============================================================
// Participant types
// ============================================================

export interface AddParticipantRequest {
  eventId: string;
  participantId: string;
}

export interface ParticipantsResponse {
  participants: string[];
}

// ============================================================
// Comment types
// ============================================================

export type CommentSection =
  | 'GENERAL'
  | 'SCENARIO'
  | 'POLL'
  | 'TRANSPORT'
  | 'ACCOMMODATION'
  | 'MEAL'
  | 'EQUIPMENT'
  | 'ACTIVITY'
  | 'BUDGET';

export interface Comment {
  id: string;
  eventId: string;
  section: CommentSection;
  sectionItemId?: string;
  authorId: string;
  authorName: string;
  content: string;
  parentCommentId?: string;
  mentions: string[];
  isDeleted: boolean;
  isPinned: boolean;
  createdAt: string;
  updatedAt?: string;
  isEdited: boolean;
  replyCount: number;
}

export interface CreateCommentRequest {
  section: CommentSection;
  sectionItemId?: string;
  content: string;
  parentCommentId?: string;
  authorId: string;
  authorName: string;
}

// ============================================================
// Generic
// ============================================================

export interface ErrorResponse {
  error: string;
  message?: string;
}
