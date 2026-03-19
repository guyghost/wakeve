# Phase 3 - Authentication, Notifications, and Collaboration

## Overview

Phase 3 adds comprehensive user authentication, push notifications, and real-time collaboration features to Wakeve. These capabilities transform Wakeve from a single-user planning tool into a collaborative social event planning platform.

## What's New

### 🔐 Flexible Authentication

Multiple sign-in options to suit user preferences:

- **Google Sign-In**: One-tap OAuth 2.0 authentication
- **Apple Sign-In**: Sign in with Apple (including web fallback for Android)
- **Email/OTP**: Passwordless authentication with one-time codes sent via email
- **Guest Mode**: Local-only mode for users who want to try the app without creating an account

**Security Features**:
- Secure token storage (Android Keystore, iOS Keychain)
- RGPD-compliant data minimization
- Automatic session restoration on app launch
- Graceful error handling with clear recovery options

### 🔔 Push Notifications

Stay informed about event activities with cross-platform push notifications:

**Notification Types**:
- `EVENT_INVITE`: When you're invited to an event
- `VOTE_REMINDER`: 24 hours before poll deadline
- `DATE_CONFIRMED`: When event date is finalized
- `NEW_SCENARIO`: When a new scenario is proposed
- `SCENARIO_SELECTED`: When final scenario is chosen
- `NEW_COMMENT`: When someone comments on your event
- `MENTION`: When you're @mentioned in a comment
- `MEETING_REMINDER`: 15 minutes before a meeting starts
- `PAYMENT_DUE`: When a payment settlement is pending

**User Preferences**:
- Enable/disable notification types individually
- Quiet hours (e.g., 22:00-08:00) for peaceful sleep
- Sound and vibration controls
- Notification history with read/unread tracking

### 💬 Real-Time Collaboration

Discuss, coordinate, and make decisions together with threaded comments:

**Comment Features**:
- Threaded replies with nested display
- @mentions to notify specific participants
- Edit and delete your own comments
- Organizer can pin important comments
- Moderation tools for organizers

**Section-Based Organization**:
Comments are organized by event sections:
- GENERAL: Overall event discussions
- SCENARIO: Destination and accommodation options
- BUDGET: Cost estimates and decisions
- TRANSPORT: Travel plans and logistics
- MEAL: Restaurant and meal choices
- ACTIVITY: Activity ideas and votes

**Permission System**:
- **Participants**: Add comments, edit/delete own comments
- **Organizers**: All participant permissions + delete any comment, pin comments

## Architecture

### Authentication System

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │   Android    │  │     iOS      │  │     Web      │ │
│  │   OAuth UI  │  │  OAuth UI    │  │  OAuth UI    │ │
│  └──────────────┘  └──────────────┘  └──────────────┘ │
├─────────────────────────────────────────────────────────┤
│                   Shared Layer (KMP)                     │
│  ┌──────────────────────────────────────────────────┐  │
│  │  AuthStateMachine (XState)                       │  │
│  │  - Handles OAuth flows                           │  │
│  │  - Manages guest mode                             │  │
│  │  - Session restoration                            │  │
│  └──────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────┐  │
│  │  AuthServices                                    │  │
│  │  - GoogleAuthService                             │  │
│  │  - AppleAuthService                              │  │
│  │  - EmailOTPService                              │  │
│  │  - SessionRepository                            │  │
│  └──────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│                    Backend (Ktor)                      │
│  ┌──────────────────────────────────────────────────┐  │
│  │  AuthRoutes                                     │  │
│  │  - POST /api/auth/google                       │  │
│  │  - POST /api/auth/apple                        │  │
│  │  - POST /api/auth/email/send-otp               │  │
│  │  - POST /api/auth/email/verify-otp             │  │
│  │  - POST /api/auth/guest                        │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### Notification System

```
┌─────────────────────────────────────────────────────────┐
│                   Event Triggers                        │
│  EventLifecycle → NotificationService.trigger()          │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│              NotificationService (Shared)                 │
│  - Get user preferences                                 │
│  - Check quiet hours                                    │
│  - Determine priority                                   │
│  - Route to platform service                             │
└─────────────────────────────────────────────────────────┘
                         ↓
              ┌────────────┴────────────┐
              ↓                         ↓
┌──────────────────┐         ┌──────────────────┐
│ FCMService       │         │ APNsService       │
│ (Android)        │         │ (iOS)            │
│                  │         │                  │
│ - onNewToken()   │         │ - registerFor... │
│ - onMessage()    │         │ - didReceive...  │
└──────────────────┘         └──────────────────┘
```

### Collaboration System

```
┌─────────────────────────────────────────────────────────┐
│              UI Layer (Compose/SwiftUI)                  │
│  CommentListScreen → MentionAutocomplete                │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│           CommentRepository (Shared)                    │
│  - createComment()                                      │
│  - getComments(eventId, section, threaded)             │
│  - updateComment()                                      │
│  - deleteComment()                                      │
│  - getMentions(commentId)                              │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│            Database (SQLDelight - SQLite)                │
│  comment table + mention table with indexes             │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│        NotificationService (trigger on actions)          │
│  - NEW_COMMENT to participants                         │
│  - MENTION to mentioned users                         │
│  - REPLY_COMMENT to parent author                     │
└─────────────────────────────────────────────────────────┘
```

## API Endpoints

### Authentication

```
POST   /api/auth/google                # Google Sign-In
POST   /api/auth/apple                 # Apple Sign-In
POST   /api/auth/email/send-otp        # Request OTP code
POST   /api/auth/email/verify-otp      # Verify OTP and login
POST   /api/auth/guest                  # Create guest session
GET    /api/auth/validate              # Validate current session
DELETE /api/auth/logout                 # Logout and delete token
```

### Notifications

```
POST   /api/notifications/register     # Register device token
DELETE /api/notifications/unregister   # Unregister device
GET    /api/notifications              # Get notification history
PUT    /api/notifications/{id}/read   # Mark as read
PUT    /api/notifications/read-all     # Mark all as read
GET    /api/notifications/preferences  # Get user preferences
PUT    /api/notifications/preferences  # Update preferences
```

### Comments

```
GET    /api/events/{id}/comments              # List comments
POST   /api/events/{id}/comments              # Add comment
GET    /api/events/{id}/comments/{commentId}  # Get single comment
PUT    /api/events/{id}/comments/{commentId}  # Edit comment
DELETE /api/events/{id}/comments/{commentId}  # Soft delete
POST   /api/events/{id}/comments/{commentId}/pin   # Pin comment
DELETE /api/events/{id}/comments/{commentId}/pin   # Unpin
POST   /api/events/{id}/comments/{commentId}/reactions  # Add/remove reaction
GET    /api/events/{id}/comments/stats       # Get statistics
```

## Database Schema

### New Tables (Phase 3)

```sql
-- Notification tokens
CREATE TABLE notification_token (
    user_id TEXT NOT NULL,
    platform TEXT NOT NULL,
    token TEXT NOT NULL,
    updated_at INTEGER NOT NULL,
    PRIMARY KEY (user_id, platform)
);

-- Notification history
CREATE TABLE notification (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    type TEXT NOT NULL,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    data TEXT,
    is_read INTEGER DEFAULT 0,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

-- Comments
CREATE TABLE comment (
    id TEXT PRIMARY KEY,
    event_id TEXT NOT NULL,
    section TEXT NOT NULL,
    section_item_id TEXT,
    author_id TEXT NOT NULL,
    parent_id TEXT,
    content TEXT NOT NULL,
    mentions TEXT, -- JSON array
    is_deleted INTEGER DEFAULT 0,
    is_pinned INTEGER DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE
);

-- Mentions (for efficient lookup)
CREATE TABLE mention (
    id TEXT PRIMARY KEY,
    comment_id TEXT NOT NULL,
    mentioned_user_id TEXT NOT NULL,
    start_index INTEGER NOT NULL,
    end_index INTEGER NOT NULL,
    FOREIGN KEY (comment_id) REFERENCES comment(id) ON DELETE CASCADE
);
```

## Testing

Phase 3 includes comprehensive test coverage:

- **Unit Tests**: 147 tests (auth, notifications, comments)
- **Integration Tests**: 38 tests (auth flows, notification routing, comment CRUD)
- **Platform-Specific Tests**: 35 tests (Android instrumented, iOS XCTest)
- **Total**: 220 tests, 100% passing

### Test Commands

```bash
# Run all tests
./gradlew shared:test

# Run auth tests
./gradlew shared:test --tests "*AuthServiceTest*"

# Run notification tests
./gradlew shared:test --tests "*NotificationServiceTest*"

# Run comment tests
./gradlew shared:test --tests "*CommentRepositoryTest*"

# Run Android instrumented tests
./gradlew wakeveApp:connectedAndroidTest

# Run iOS tests
xcodebuild test -scheme wakeveApp
```

## Migration Guide

### For Users

Upgrading from Phase 2:
- No data migration required for existing events
- Guest users can continue using local-only mode
- Authenticated users will be prompted to authenticate if tokens are expired

### For Developers

New dependencies added:
```kotlin
// build.gradle.kts (shared)
implementation("com.google.firebase:firebase-messaging-ktx:23.0.0")

// build.gradle.kts (android)
implementation("com.google.android.gms:play-services-auth:20.7.0")
implementation("com.apple.signin:sign-in-with-apple-kmm:1.0.0")

// build.gradle.kts (ios)
// Sign in with Apple framework automatically added by Xcode
```

## Known Limitations

1. **APNs Certificate**: Development certificate expires in 90 days; production certificate required for App Store
2. **Comment Pagination**: Simple limit/offset; infinite scroll planned for Phase 4
3. **Notification Digests**: Daily/weekly digest not yet implemented
4. **Comment Search**: Full-text search not yet available

## Future Enhancements (Phase 4+)

1. **Advanced Notifications**
   - Notification digests (daily/weekly)
   - Custom notification sounds
   - Notification channels (per event type)

2. **Rich Collaboration**
   - Rich text editor (bold, italic, links)
   - Image and file attachments
   - Emoji reactions
   - Full-text search across comments

3. **Analytics**
   - Event engagement metrics
   - Participant activity tracking
   - Comment sentiment analysis
   - Most active contributors leaderboard

## Documentation

- **Specs**: `openspec/specs/notification-management/`, `openspec/specs/collaboration-management/`, `openspec/specs/user-auth/`
- **API**: `server/src/main/kotlin/com/guyghost/wakeve/routes/`
- **Implementation**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/`

## Support

For questions or issues:
- Check the Phase 3 completion summary: `openspec/changes/complete-phase3-final/COMPLETION_SUMMARY.md`
- Review detailed specs in `openspec/specs/`
- Submit issues via GitHub Issues

---

**Phase 3 Status**: ✅ Complete and Production-Ready

**Test Coverage**: 220 tests (100% passing)

**Ready for**: Phase 4 - Advanced Features & Optimization
