import type {
  AuthResponse,
  EmailOTPRequest,
  EmailOTPVerifyRequest,
  GuestSessionRequest,
  OTPRequestResponse,
  TokenRefreshResponse,
  EventResponse,
  EventsListResponse,
  CreateEventRequest,
  UpdateEventStatusRequest,
  PollResponse,
  AddVoteRequest,
  ParticipantsResponse,
  AddParticipantRequest,
  Comment,
  CreateCommentRequest,
} from '../types/api';

// ---------------------------------------------------------------------------
// Token management
// ---------------------------------------------------------------------------

const TOKEN_KEY = 'wakeve_access_token';
const REFRESH_KEY = 'wakeve_refresh_token';
const TOKEN_EXPIRY_KEY = 'wakeve_token_expiry';

function getAccessToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_KEY);
}

function setTokens(accessToken: string, refreshToken?: string | null, expiresInSeconds?: number) {
  localStorage.setItem(TOKEN_KEY, accessToken);
  if (refreshToken) {
    localStorage.setItem(REFRESH_KEY, refreshToken);
  }
  if (expiresInSeconds) {
    const expiry = Date.now() + expiresInSeconds * 1000;
    localStorage.setItem(TOKEN_EXPIRY_KEY, String(expiry));
  }
}

function clearTokens() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_KEY);
  localStorage.removeItem(TOKEN_EXPIRY_KEY);
  localStorage.removeItem('wakeve_user');
}

function isTokenExpired(): boolean {
  const expiry = localStorage.getItem(TOKEN_EXPIRY_KEY);
  if (!expiry) return true;
  // Refresh 30 seconds before actual expiry
  return Date.now() > Number(expiry) - 30_000;
}

// ---------------------------------------------------------------------------
// Base fetch wrapper
// ---------------------------------------------------------------------------

const BASE_URL = '/api';

class ApiError extends Error {
  status: number;
  code?: string;

  constructor(status: number, message: string, code?: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
  }
}

let isRefreshing = false;
let refreshPromise: Promise<void> | null = null;

async function tryRefreshToken(): Promise<void> {
  if (isRefreshing && refreshPromise) {
    return refreshPromise;
  }

  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    clearTokens();
    throw new ApiError(401, 'No refresh token available');
  }

  isRefreshing = true;
  refreshPromise = (async () => {
    try {
      const resp = await fetch(`${BASE_URL}/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      });

      if (!resp.ok) {
        clearTokens();
        throw new ApiError(resp.status, 'Token refresh failed');
      }

      const data: TokenRefreshResponse = await resp.json();
      setTokens(data.accessToken, data.refreshToken, data.expiresInSeconds);
    } finally {
      isRefreshing = false;
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
  requiresAuth = true,
): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> || {}),
  };

  if (requiresAuth) {
    if (isTokenExpired()) {
      try {
        await tryRefreshToken();
      } catch {
        // Will fail at request level
      }
    }
    const token = getAccessToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers,
  });

  if (response.status === 401 && requiresAuth) {
    // Try refresh once
    try {
      await tryRefreshToken();
      const token = getAccessToken();
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      const retryResponse = await fetch(`${BASE_URL}${path}`, {
        ...options,
        headers,
      });
      if (!retryResponse.ok) {
        const errorData = await retryResponse.json().catch(() => ({}));
        throw new ApiError(
          retryResponse.status,
          errorData.message || errorData.error || 'Request failed',
          errorData.error,
        );
      }
      if (retryResponse.status === 204) return undefined as T;
      return retryResponse.json();
    } catch {
      clearTokens();
      window.dispatchEvent(new CustomEvent('wakeve:auth-expired'));
      throw new ApiError(401, 'Session expired');
    }
  }

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new ApiError(
      response.status,
      errorData.message || errorData.error || 'Request failed',
      errorData.error,
    );
  }

  if (response.status === 204) return undefined as T;
  return response.json();
}

// ---------------------------------------------------------------------------
// Auth API
// ---------------------------------------------------------------------------

export const authApi = {
  requestOtp(data: EmailOTPRequest): Promise<OTPRequestResponse> {
    return apiFetch('/auth/email/request', {
      method: 'POST',
      body: JSON.stringify(data),
    }, false);
  },

  verifyOtp(data: EmailOTPVerifyRequest): Promise<AuthResponse> {
    return apiFetch('/auth/email/verify', {
      method: 'POST',
      body: JSON.stringify(data),
    }, false);
  },

  loginGuest(data: GuestSessionRequest = {}): Promise<AuthResponse> {
    return apiFetch('/auth/guest', {
      method: 'POST',
      body: JSON.stringify(data),
    }, false);
  },

  refreshToken(): Promise<TokenRefreshResponse> {
    const refreshToken = getRefreshToken();
    return apiFetch('/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    }, false);
  },

  /** Save auth tokens after successful login */
  saveAuthData(data: AuthResponse) {
    setTokens(data.accessToken, data.refreshToken, data.expiresInSeconds);
    localStorage.setItem('wakeve_user', JSON.stringify(data.user));
  },

  /** Get stored user from localStorage */
  getStoredUser() {
    const raw = localStorage.getItem('wakeve_user');
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  },

  /** Check if user is logged in */
  isLoggedIn(): boolean {
    return !!getAccessToken();
  },

  /** Logout */
  logout() {
    clearTokens();
    window.dispatchEvent(new CustomEvent('wakeve:auth-expired'));
  },
};

// ---------------------------------------------------------------------------
// Events API
// ---------------------------------------------------------------------------

export const eventsApi = {
  list(): Promise<EventsListResponse> {
    return apiFetch('/events');
  },

  get(id: string): Promise<EventResponse> {
    return apiFetch(`/events/${encodeURIComponent(id)}`);
  },

  create(data: CreateEventRequest): Promise<EventResponse> {
    return apiFetch('/events', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  updateStatus(id: string, data: UpdateEventStatusRequest): Promise<EventResponse> {
    return apiFetch(`/events/${encodeURIComponent(id)}/status`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },
};

// ---------------------------------------------------------------------------
// Poll / Votes API
// ---------------------------------------------------------------------------

export const pollApi = {
  get(eventId: string): Promise<PollResponse> {
    return apiFetch(`/events/${encodeURIComponent(eventId)}/poll`);
  },

  vote(eventId: string, data: AddVoteRequest): Promise<PollResponse> {
    return apiFetch(`/events/${encodeURIComponent(eventId)}/poll/votes`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
};

// ---------------------------------------------------------------------------
// Participants API
// ---------------------------------------------------------------------------

export const participantsApi = {
  list(eventId: string): Promise<ParticipantsResponse> {
    return apiFetch(`/events/${encodeURIComponent(eventId)}/participants`);
  },

  add(eventId: string, data: AddParticipantRequest): Promise<ParticipantsResponse> {
    return apiFetch(`/events/${encodeURIComponent(eventId)}/participants`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },
};

// ---------------------------------------------------------------------------
// Comments API
// ---------------------------------------------------------------------------

export const commentsApi = {
  list(eventId: string, section?: string): Promise<Comment[]> {
    const params = section ? `?section=${section}` : '';
    return apiFetch(`/events/${encodeURIComponent(eventId)}/comments${params}`);
  },

  create(eventId: string, data: CreateCommentRequest): Promise<Comment> {
    return apiFetch(`/events/${encodeURIComponent(eventId)}/comments`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  delete(eventId: string, commentId: string): Promise<void> {
    return apiFetch(`/events/${encodeURIComponent(eventId)}/comments/${encodeURIComponent(commentId)}`, {
      method: 'DELETE',
    });
  },
};

// ---------------------------------------------------------------------------
// Dashboard API
// ---------------------------------------------------------------------------

export interface DashboardOverviewResponse {
  totalEvents: number;
  totalParticipants: number;
  averageParticipants: number;
  totalVotes: number;
  totalComments: number;
  eventsByStatus: Record<string, number>;
}

export interface DashboardEventItem {
  eventId: string;
  title: string;
  status: string;
  eventType?: string;
  createdAt: string;
  deadline: string;
  participantCount: number;
  voteCount: number;
  commentCount: number;
  responseRate: number;
}

export interface DashboardEventsResponse {
  events: DashboardEventItem[];
}

export interface EventDetailedAnalyticsResponse {
  eventId: string;
  title: string;
  status: string;
  voteTimeline: { date: string; count: number }[];
  participantTimeline: { date: string; count: number }[];
  popularTimeSlots: {
    slotId: string;
    startTime?: string;
    endTime?: string;
    timeOfDay?: string;
    yesVotes: number;
    maybeVotes: number;
    noVotes: number;
    totalVotes: number;
  }[];
  pollCompletionRate: number;
  totalParticipants: number;
  votedParticipants: number;
}

export const dashboardApi = {
  getOverview(): Promise<DashboardOverviewResponse> {
    return apiFetch('/dashboard/overview');
  },

  getEvents(): Promise<DashboardEventsResponse> {
    return apiFetch('/dashboard/events');
  },

  getEventAnalytics(eventId: string): Promise<EventDetailedAnalyticsResponse> {
    return apiFetch(`/dashboard/events/${encodeURIComponent(eventId)}/analytics`);
  },
};

export { ApiError, clearTokens };
