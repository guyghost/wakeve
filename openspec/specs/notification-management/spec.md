# Notification Management Specification

## Version
**Version**: 1.0.0
**Status**: ✅ Implémenté
**Date**: 3 février 2026

## Overview

The notification system provides cross-platform push notifications using Firebase Cloud Messaging (FCM) for Android and Apple Push Notification Service (APNs) for iOS. It supports multiple notification types, user preferences, quiet hours, and notification history.

## Requirements

### Requirement: Notification Service
The system SHALL provide a unified notification service for cross-platform push notifications.

**ID**: `notif-001`

#### Scenario: Register device for push notifications
- **GIVEN** a user is authenticated
- **WHEN** the app launches or user enables notifications
- **THEN** the system SHALL:
  - Request platform permission (Android 13+, iOS always)
  - Retrieve FCM token (Android) or APNs token (iOS)
  - Register token with backend via `/api/notifications/register`
  - Store token in local preferences

#### Scenario: Receive push notification in foreground
- **GIVEN** the app is in foreground
- **WHEN** a push notification is received
- **THEN** the system SHALL:
  - Display in-app banner/snackbar
  - Play notification sound (if enabled)
  - Update notification badge
  - Store in local notification history

#### Scenario: Receive push notification in background
- **GIVEN** the app is in background or killed
- **WHEN** a push notification is received
- **THEN** the system SHALL:
  - Display system notification
  - Play notification sound
  - Update badge count
  - On tap: open relevant screen

#### Scenario: Notification types
- **GIVEN** various event activities occur
- **WHEN** notifications are triggered
- **THEN** the system SHALL support types:
  - `EVENT_INVITE`: User invited to event
  - `VOTE_REMINDER`: Poll deadline approaching
  - `DATE_CONFIRMED`: Event date confirmed
  - `NEW_SCENARIO`: New scenario proposed
  - `SCENARIO_SELECTED`: Final scenario selected
  - `NEW_COMMENT`: New comment on event
  - `MENTION`: User mentioned in comment
  - `MEETING_REMINDER`: Meeting starting soon
  - `PAYMENT_DUE`: Settlement pending

### Requirement: Notification Preferences
The system SHALL allow users to customize notification preferences.

**ID**: `notif-002`

#### Scenario: Configure notification types
- **GIVEN** user opens notification settings
- **WHEN** they toggle notification types
- **THEN** the system SHALL:
  - Persist preferences locally
  - Sync to backend
  - Respect preferences when routing notifications

#### Scenario: Quiet hours
- **GIVEN** user configured quiet hours (e.g., 22:00-08:00)
- **WHEN** a notification arrives during quiet hours
- **THEN** the system SHALL:
  - Queue notification for later (if non-urgent)
  - Display immediately (if urgent: meeting starting)
  - Not play sound or vibrate

### Requirement: Notification History
The system SHALL maintain a notification history.

**ID**: `notif-003`

#### Scenario: View notification history
- **GIVEN** user opens notification center
- **WHEN** viewing the list
- **THEN** the system SHALL display:
  - Unread notifications (bold/highlighted)
  - Read notifications
  - Timestamp
  - Associated event
  - Action button (View, Dismiss)

#### Scenario: Mark as read
- **GIVEN** user views notification
- **WHEN** they interact with it
- **THEN** the system SHALL mark it as read and update badge

## Data Models

### Notification Token

```kotlin
@Serializable
data class NotificationToken(
    val userId: String,
    val platform: Platform, // ANDROID, IOS
    val token: String,
    val updatedAt: Instant
)
```

### Notification Message

```kotlin
@Serializable
data class NotificationMessage(
    val id: String,
    val userId: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val data: Map<String, String>, // eventId, etc.
    val read: Boolean,
    val createdAt: Instant
)
```

### Notification Preferences

```kotlin
@Serializable
data class NotificationPreferences(
    val userId: String,
    val enabledTypes: Set<NotificationType>,
    val quietHoursStart: LocalTime?,
    val quietHoursEnd: LocalTime?,
    val soundEnabled: Boolean,
    val vibrationEnabled: Boolean
)
```

### Notification Type

```kotlin
@Serializable
enum class NotificationType {
    EVENT_INVITE,
    VOTE_REMINDER,
    DATE_CONFIRMED,
    NEW_SCENARIO,
    SCENARIO_SELECTED,
    NEW_COMMENT,
    MENTION,
    MEETING_REMINDER,
    PAYMENT_DUE
}
```

## API Endpoints

```
POST   /api/notifications/register      # Register device token
DELETE /api/notifications/unregister    # Unregister device
GET    /api/notifications               # Get notification history
PUT    /api/notifications/{id}/read     # Mark as read
PUT    /api/notifications/read-all      # Mark all as read
GET    /api/notifications/preferences   # Get preferences
PUT    /api/notifications/preferences   # Update preferences
```

### POST /api/notifications/register

**Description**: Register a device token for push notifications.

**Request Body**:
```json
{
  "platform": "ANDROID",
  "token": "fcm_token_or_apns_token"
}
```

**Response**: 200 OK

### POST /api/notifications/send

**Description**: Send a push notification to a user (internal endpoint).

**Request Body**:
```json
{
  "userId": "user-123",
  "type": "EVENT_INVITE",
  "title": "New Event Invitation",
  "body": "You've been invited to Birthday Party",
  "data": {
    "eventId": "event-456",
    "screen": "event-detail"
  }
}
```

**Response**: 200 OK

## Database Schema

```sql
-- Notification Tokens
CREATE TABLE notification_token (
    user_id TEXT NOT NULL,
    platform TEXT NOT NULL,
    token TEXT NOT NULL,
    updated_at INTEGER NOT NULL,
    PRIMARY KEY (user_id, platform)
);

-- Notifications
CREATE TABLE notification (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    type TEXT NOT NULL,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    data TEXT, -- JSON
    is_read INTEGER DEFAULT 0,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
);

CREATE INDEX idx_notification_user ON notification(user_id);
CREATE INDEX idx_notification_read ON notification(user_id, is_read);
```

## Platform-Specific Implementation

### Android (FCM)

**Dependencies**:
```kotlin
implementation("com.google.firebase:firebase-messaging-ktx:23.0.0")
```

**Key Components**:
- `FCMService.kt`: Extends `FirebaseMessagingService`
- Handles `onNewToken()` for token registration
- Handles `onMessageReceived()` for foreground messages

**Permissions** (AndroidManifest.xml):
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

**Token Registration**:
```kotlin
FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
    if (task.isSuccessful) {
        val token = task.result
        // Send token to backend
        notificationService.registerToken(token)
    }
}
```

### iOS (APNs)

**Key Components**:
- `APNsService.swift`: Manages APNs registration
- Requests notification permission on app launch
- Forwards device token to Kotlin/Native layer

**Permission Request**:
```swift
UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
    if granted {
        DispatchQueue.main.async {
            UIApplication.shared.registerForRemoteNotifications()
        }
    }
}
```

## Notification Priority

The system categorizes notifications by urgency:

| Priority | Types | Quiet Hours Behavior |
|----------|-------|---------------------|
| Urgent | MEETING_REMINDER | Display immediately |
| High | EVENT_INVITE, DATE_CONFIRMED | Queue if quiet hours |
| Medium | VOTE_REMINDER, NEW_COMMENT | Queue if quiet hours |
| Low | SCENARIO_SELECTED, PAYMENT_DUE | Queue until active |

## Testing

### Unit Tests
- `NotificationServiceTest`: Token registration, routing, preferences
- `NotificationPreferencesRepositoryTest`: CRUD operations, quiet hours

### Integration Tests
- `NotificationFlowTest`: Register → Send → Receive → Mark read
- `FCMIntegrationTest`: FCM token registration and message handling
- `APNsIntegrationTest`: APNs token registration and message handling

### Platform-Specific Tests
- Android instrumented tests for permission handling
- iOS XCTest for notification authorization flows

## Implementation Files

### Shared Layer
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/notification/NotificationService.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/notification/NotificationTypes.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/notification/NotificationPreferences.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/notification/NotificationPreferencesRepository.kt`

### Backend
- `server/src/main/kotlin/com/guyghost/wakeve/routes/NotificationRoutes.kt`

### Android
- `wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/service/FCMService.kt`

### iOS
- `iosApp/iosApp/Services/APNsService.swift`

## Integration & Cross-References

- References: `openspec/specs/collaboration-management/spec.md` (NEW_COMMENT, MENTION notifications)
- References: `openspec/specs/event-organization/spec.md` (EVENT_INVITE, DATE_CONFIRMED notifications)
- References: `openspec/specs/meeting-service/spec.md` (MEETING_REMINDER notifications)
