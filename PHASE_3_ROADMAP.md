# Wakeve Phase 3 Roadmap

## Overview
Phase 3 extends the core Event Organization capability with user authentication, offline sync, and notifications. This document outlines the roadmap, technical decisions, and implementation strategy.

## Current State (Phase 2 Complete)
✅ Event organization with polls and voting
✅ Multiplatform database (SQLDelight)
✅ REST API server (Ktor)
✅ Android Compose UI
✅ 36 comprehensive tests

## Phase 3 Objectives

### Objective 1: Secure User Authentication
**Goal**: Replace static user IDs with OAuth2-based authentication

**Components**:
- OAuth2 provider integrations (Google, Apple)
- Secure token storage and refresh
- User profile management
- Session persistence

**Benefits**:
- Real user identification
- Social login convenience
- Secure credential handling
- User preference persistence

### Objective 2: Offline-First Synchronization
**Goal**: Ensure changes made while offline are synced to server

**Components**:
- Offline change tracking (SyncMetadata table ready)
- Server sync endpoint
- Conflict resolution strategy
- Sync status UI indicators
- Retry logic for failed syncs

**Benefits**:
- Better user experience offline
- No data loss
- Consistent state across devices
- Automatic background sync

### Objective 3: Push Notifications
**Goal**: Keep users informed of event updates and voting deadlines

**Components**:
- Firebase Cloud Messaging (FCM) for Android
- Apple Push Notification (APNs) for iOS
- Notification scheduling
- User preference management
- Deep linking to event details

**Benefits**:
- Timely deadline reminders
- Event update notifications
- Increased user engagement
- Cross-platform consistency

### Objective 4: Calendar Integration
**Goal**: Add events to native calendars for better planning

**Components**:
- Android Calendar Provider integration
- iOS EventKit integration
- Timezone-aware event creation
- Recurring event support
- Calendar selection (which calendar to add to)

**Benefits**:
- Better calendar awareness
- Native calendar app integration
- Timezone handling
- Single source of truth for time management

## Implementation Plan

### Phase 3 Sprint 1: User Authentication (Weeks 1-2)

#### Sprint Goals
- [ ] Implement OAuth2 client library integration
- [ ] Support Google and Apple authentication
- [ ] Secure token storage
- [ ] User session management
- [ ] Update API endpoints for authenticated requests

#### Technical Decisions
- **OAuth2 Library**: Use Ktor OAuth client for server, platform-specific clients for mobile
- **Token Storage**: Secure encrypted storage (Keystore on Android, Keychain on iOS)
- **User Identification**: Replace static IDs with authenticated user from provider
- **Backward Compatibility**: Maintain existing APIs with auth headers

#### Deliverables
- [ ] OAuth2 setup for Google and Apple
- [ ] User authentication flow (login, logout, refresh)
- [ ] Token management and refresh
- [ ] API authentication middleware
- [ ] Tests for authentication flows

### Phase 3 Sprint 2: Offline Sync (Weeks 3-4)

#### Sprint Goals
- [ ] Implement server sync endpoint
- [ ] Detect network status and queue changes
- [ ] Implement sync retry logic
- [ ] Handle conflict resolution
- [ ] Add sync status UI indicators

#### Technical Decisions
- **Sync Strategy**: Incremental sync using SyncMetadata table timestamps
- **Conflict Resolution**: Last-write-wins (v1) → CRDT (v2+)
- **Queue Strategy**: Local transaction queue, process in order
- **Network Detection**: Platform-specific connectivity monitoring
- **Background Sync**: Using WorkManager (Android) and BackgroundTasks (iOS)

#### Deliverables
- [ ] POST /api/sync endpoint (batch operations)
- [ ] Sync status tracking in SyncMetadata
- [ ] Network status detection
- [ ] Background sync scheduling
- [ ] Conflict resolution logic
- [ ] Sync tests and offline scenarios

### Phase 3 Sprint 3: Notifications & Calendar (Weeks 5-6)

#### Sprint Goals
- [ ] Setup push notification infrastructure
- [ ] Implement notification delivery
- [ ] Integrate native calendar APIs
- [ ] Add timezone-aware event scheduling
- [ ] Create user notification preferences

#### Technical Decisions
- **Push Notifications**: Firebase Cloud Messaging (FCM) backend
- **Scheduling**: Kotlin Coroutines for scheduled notifications
- **Calendar Access**: Platform-specific (Calendar Provider on Android, EventKit on iOS)
- **Timezone Handling**: Store in UTC, convert for calendar events
- **User Preferences**: Settings screen for notification opt-in/out

#### Deliverables
- [ ] FCM server integration
- [ ] Notification templates and scheduling
- [ ] Android Calendar Provider integration
- [ ] iOS EventKit integration
- [ ] Notification settings UI
- [ ] Comprehensive tests for all features

## Architecture Changes

### Authentication Layer
```
Mobile App (OAuth Login)
    ↓
OAuth Provider (Google/Apple)
    ↓
Auth Service (Ktor)
    ↓
Database (User table)
```

### Sync Layer
```
Mobile App (Offline Changes)
    ↓
SyncMetadata Table (queues changes)
    ↓
Sync Service (detects network)
    ↓
/api/sync Endpoint (batch upload)
    ↓
Conflict Resolution
    ↓
Database (apply changes)
```

### Notification Layer
```
Server (sends event update)
    ↓
FCM/APNs (push service)
    ↓
Mobile App (receives notification)
    ↓
Deep Link to Event Details
```

### Calendar Layer
```
Mobile App (creates calendar event)
    ↓
Calendar Provider / EventKit
    ↓
Native Calendar App
```

## Database Schema Extensions

### New Tables

**users**
```sql
CREATE TABLE users (
  id TEXT PRIMARY KEY,
  provider_id TEXT UNIQUE,      -- OAuth provider ID
  email TEXT UNIQUE,
  name TEXT,
  avatar_url TEXT,
  created_at TEXT,
  updated_at TEXT
);
```

**user_tokens**
```sql
CREATE TABLE user_tokens (
  id TEXT PRIMARY KEY,
  user_id TEXT REFERENCES users(id),
  access_token TEXT,
  refresh_token TEXT,
  expires_at TEXT,
  created_at TEXT
);
```

**notification_preferences**
```sql
CREATE TABLE notification_preferences (
  id TEXT PRIMARY KEY,
  user_id TEXT REFERENCES users(id) UNIQUE,
  deadline_reminder INTEGER DEFAULT 1,
  event_update INTEGER DEFAULT 1,
  vote_close_reminder INTEGER DEFAULT 1,
  timezone TEXT
);
```

### Extended Tables

**events** - Add organizer_auth_id linking to users table
**participants** - Update to reference users table

## Testing Strategy

### Phase 3 Tests

**Authentication Tests**:
- OAuth flow validation
- Token refresh scenarios
- Secure token storage
- Session persistence
- Authentication error handling

**Sync Tests**:
- Offline change queuing
- Network status detection
- Sync success and failures
- Conflict resolution
- Batch operation handling

**Notification Tests**:
- Notification scheduling
- Push delivery
- Deep link handling
- User preference respect

**Calendar Tests**:
- Event creation
- Timezone conversion
- Calendar selection
- Recurrence handling

## Deployment Considerations

### Phase 3 Deployment
1. **Database Migration**: Add new tables, update schema
2. **Backend Deployment**: New auth and sync endpoints
3. **Mobile Updates**: New OAuth UI, sync logic, notifications
4. **OAuth Provider Setup**: Create apps on Google and Apple
5. **FCM/APNs Setup**: Configure push notification certificates

### Rollout Strategy
1. **Beta**: Deploy to beta testers with auth enabled
2. **Gradual**: Rollout to 50%, then 100% of users
3. **Monitoring**: Track auth failures, sync errors, notification delivery
4. **Fallback**: Maintain old session system as fallback during transition

## Success Criteria

### Phase 3 Complete When:
- ✅ All OAuth2 flows working for Google and Apple
- ✅ Offline changes sync successfully to server
- ✅ Push notifications deliver in <30 seconds
- ✅ Calendar events appear in native calendar app
- ✅ All Phase 3 tests passing (50+ new tests)
- ✅ Zero breaking changes to existing API
- ✅ Documentation updated for new features

## Future Enhancements (Phase 4+)

### Possible Phase 4 Features:
- **Calendar Sync**: Two-way sync with native calendars
- **Smart Suggestions**: AI-powered time slot recommendations
- **Team Scheduling**: Cross-organization event planning
- **Travel Planning**: Flights, hotels, activities coordination
- **Advanced Notifications**: Custom notification channels, scheduling

### Possible Phase 5 Features:
- **Internationalization**: Multi-language support
- **Accessibility**: WCAG compliance, screen reader support
- **Advanced Analytics**: User engagement metrics
- **Custom Branding**: White-label support for organizations

## Timeline

**Phase 3 Total Duration**: 6 weeks (mid-December 2025 to mid-January 2026)

```
Sprint 1 (Dec 1-14):  OAuth2 Authentication
Sprint 2 (Dec 15-31): Offline-First Sync
Sprint 3 (Jan 1-15):  Notifications & Calendar
─────────────────────────────────────────────
Phase 3 Complete:     Mid-January 2026
```

## Risk Mitigation

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|-----------|
| OAuth provider API changes | Low | Medium | Monitor provider changelogs, use stable APIs |
| Sync conflicts in production | Medium | High | Comprehensive testing, rollback plan |
| Notification delivery failures | Medium | Medium | Retry logic, fallback UI indicators |
| Calendar permission issues | Medium | Low | Clear permission requests, graceful degradation |

## Sign-Off

**Prepared By**: OpenCode AI Assistant  
**Date**: November 12, 2025  
**Status**: Ready for Phase 3 Planning  
**Next Step**: Create GitHub Issues for each Phase 3 sprint
