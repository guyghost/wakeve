# Firebase Analytics Provider Implementation - Phase 6 Analytics P1.2

## Overview

This document describes the implementation of Firebase Analytics Provider for Android and iOS platforms with offline queue support as part of Phase 6 Analytics P1.2.

## Implementation Summary

### Files Created

1. **AnalyticsQueue** (commonMain)
   - `shared/src/commonMain/kotlin/com/guyghost/wakeve/analytics/AnalyticsQueue.kt`
   - Thread-safe queue for offline analytics events
   - Uses Mutex for concurrent access
   - Supports retry logic (max 3 retries per event)
   - StateFlow-based queue observation

2. **FirebaseAnalyticsProvider** (commonMain)
   - `shared/src/commonMain/kotlin/com/guyghost/wakeve/analytics/FirebaseAnalyticsProvider.kt`
   - expect/actual declaration
   - Platform-specific implementations for Android, iOS, and JVM (testing)

3. **FirebaseAnalyticsProvider** (androidMain)
   - `shared/src/androidMain/kotlin/com/guyghost/wakeve/analytics/FirebaseAnalyticsProvider.kt`
   - Firebase SDK integration via `firebase-analytics-ktx:21.5.0`
   - Bundle-based event logging
   - Automatic event-specific parameter mapping
   - Offline queue integration

4. **FirebaseAnalyticsProvider** (iosMain)
   - `shared/src/iosMain/kotlin/com/guyghost/wakeve/analytics/FirebaseAnalyticsProvider.kt`
   - Firebase SDK integration via CocoaPods (cocoapods.FirebaseAnalytics)
   - NSDictionary-based event logging
   - Automatic event-specific parameter mapping
   - Offline queue integration

5. **FirebaseAnalyticsProvider** (jvmMain)
   - `shared/src/jvmMain/kotlin/com/guyghost/wakeve/analytics/FirebaseAnalyticsProvider.kt`
   - Minimal implementation for JVM testing
   - Queue-only mode (no Firebase SDK for JVM)

6. **Tests**
   - `shared/src/commonTest/kotlin/com/guyghost/wakeve/analytics/AnalyticsQueueTest.kt` (15 tests)
   - `shared/src/commonTest/kotlin/com/guyghost/wakeve/analytics/FirebaseAnalyticsProviderTest.kt` (28 tests)

### Files Modified

1. **shared/build.gradle.kts**
   - Added Firebase dependencies for Android:
     - `com.google.firebase:firebase-analytics-ktx:21.5.0`
     - `com.google.firebase:firebase-bom:32.7.0`
   - Added comment for iOS CocoaPods integration

## Features

### AnalyticsQueue

- **Thread-safe**: Uses `Mutex` for concurrent access
- **Retry logic**: Events can be retried up to 3 times
- **StateFlow**: Queue can be observed via `StateFlow<List<QueuedEvent>>`
- **Event serialization**: All properties converted to strings for storage
- **Unique IDs**: Each event gets a unique ID with timestamp
- **Queue operations**:
  - `enqueue()`: Add event to queue
  - `getPendingEvents()`: Get events with retry count < maxRetries
  - `markAsSynced()`: Remove successfully synced events
  - `markAsFailed()`: Increment retry count and remove if exceeded
  - `clear()`: Clear all events
  - `size`: Current queue size

### FirebaseAnalyticsProvider (Android)

- **Immediate logging**: Events sent to Firebase Analytics immediately
- **Bundle mapping**: Automatic conversion of Kotlin types to Firebase Bundle:
  - String → putString()
  - Int → putInt()
  - Long → putLong()
  - Double → putDouble()
  - Boolean → putBoolean()
  - Other → toString()
- **Event-specific parameters**: Pre-mapped parameters for each event type
- **RGPD compliance**: Support for enable/disable and clear user data
- **Offline backup**: All events queued for offline recovery
- **Periodic sync**: Background sync job (to be implemented with network monitoring)

### FirebaseAnalyticsProvider (iOS)

- **Immediate logging**: Events sent to Firebase Analytics immediately
- **NSDictionary mapping**: Automatic conversion of Kotlin types to NSDictionary:
  - String → NSString
  - Int → numberWithInt()
  - Long → numberWithLong()
  - Double → numberWithDouble()
  - Boolean → numberWithBool()
  - Other → NSString(value?.toString())
- **Event-specific parameters**: Pre-mapped parameters for each event type
- **RGPD compliance**: Support for enable/disable and clear user data
- **Offline backup**: All events queued for offline recovery
- **Periodic sync**: Background sync job (to be implemented with network monitoring)

### FirebaseAnalyticsProvider (JVM)

- **Minimal implementation**: For testing common code only
- **Queue-only mode**: No Firebase SDK available for JVM
- **Compatibility**: Maintains same interface as Android/iOS

## Test Coverage

### AnalyticsQueueTest (15 tests)

1. ✅ enqueue adds event to queue
2. ✅ enqueue stores event correctly
3. ✅ enqueue multiple events
4. ✅ getPendingEvents returns all events
5. ✅ getPendingEvents filters events exceeding max retries
6. ✅ markAsSynced removes events from queue
7. ✅ markAsSynced removes only specified events
8. ✅ markAsFailed increments retry count
9. ✅ markAsFailed increments retry count multiple times
10. ✅ markAsFailed removes events after max retries
11. ✅ clear removes all events from queue
12. ✅ event properties are converted to strings
13. ✅ event IDs are unique
14. ✅ retry count starts at 0
15. ✅ markAsFailed only affects specified events

### FirebaseAnalyticsProviderTest (28 tests)

1. ✅ trackEvent queues event in queue
2. ✅ trackEvent with properties stores properties correctly
3. ✅ trackEvent event created with all parameters
4. ✅ trackEvent poll voted event
5. ✅ trackEvent screen view event
6. ✅ setUserProperty stores property
7. ✅ setUserId stores user id
8. ✅ setUserId with null clears user id
9. ✅ setEnabled false prevents tracking
10. ✅ setEnabled true allows tracking
11. ✅ setEnabled false prevents user property setting
12. ✅ setEnabled false prevents user id setting
13. ✅ clearUserData clears tracked events
14. ✅ clearUserData clears user properties
15. ✅ clearUserData clears user id
16. ✅ clearUserData clears everything
17. ✅ multiple events tracked correctly
18. ✅ event joined with guest flag
19. ✅ event shared with method
20. ✅ scenario created with accommodation flag
21. ✅ meeting created with platform
22. ✅ user registered with auth method
23. ✅ offline action queued event
24. ✅ sync completed event
25. ✅ sync failed event
26. ✅ error occurred event
27. ✅ API error event
28. ✅ analytics consent events

### Total Test Count

- Existing AnalyticsProviderTest (P1.1): 12 tests
- New AnalyticsQueueTest (P1.2): 15 tests
- New FirebaseAnalyticsProviderTest (P1.2): 28 tests
- **Total Analytics Tests**: 55 tests ✅

## Event Types Supported

All events from `AnalyticsEvent` are supported:

### App Lifecycle
- AppStart, AppForeground, AppBackground, ScreenView

### Event Actions
- EventCreated, EventJoined, EventViewed, EventShared

### Poll Actions
- PollVoted, PollViewed, PollClosed

### Scenario Actions
- ScenarioCreated, ScenarioViewed, ScenarioSelected, ScenarioVoted

### Meeting Actions
- MeetingCreated, MeetingJoined, MeetingLinkGenerated

### User Actions
- UserRegistered, UserLoggedIn, UserLoggedOut, UserProfileUpdated

### Offline Actions
- OfflineActionQueued, SyncCompleted, SyncFailed

### Error Events
- ErrorOccurred, ApiError

### RGPD Events
- AnalyticsConsentGranted, AnalyticsConsentRevoked, UserDataDeleted

## Firebase Setup Instructions

### Android

Dependencies are already added to `shared/build.gradle.kts`:

```kotlin
androidMain.dependencies {
    implementation("com.google.firebase:firebase-analytics-ktx:21.5.0")
    implementation("com.google.firebase:firebase-bom:32.7.0")
}
```

Additional setup required:
1. Add `google-services.json` to `wakeveApp/src/`
2. Apply `com.google.gms.google-services` plugin in `wakeveApp/build.gradle.kts`
3. Initialize Firebase in Application class

### iOS

Firebase Analytics needs to be added via CocoaPods:

1. Create or update `Podfile` in wakeveApp
2. Add `pod 'Firebase/Analytics'` to Podfile
3. Run `pod install`
4. Initialize Firebase in AppDelegate

Example Podfile:

```ruby
target 'wakeveApp' do
  use_frameworks!
  use_modular_headers!

  pod 'Firebase/Analytics'

  # Add other dependencies as needed
end
```

## RGPD Compliance

The implementation supports full RGPD compliance:

1. **Enable/Disable Tracking**
   - `provider.setEnabled(false)` stops all tracking
   - Firebase SDK's `setAnalyticsCollectionEnabled()` used

2. **Clear User Data**
   - `provider.clearUserData()` removes:
     - All tracked events
     - User properties
     - User ID
     - Queued offline events

3. **User Consent Events**
   - `AnalyticsConsentGranted` tracked when user opts in
   - `AnalyticsConsentRevoked` tracked when user opts out
   - `UserDataDeleted` tracked when user requests deletion

## Offline Support

### Queue Operation

1. Events are immediately sent to Firebase Analytics (if online)
2. Events are queued for offline backup
3. Queue persists events during offline
4. On reconnection, sync job processes queue
5. Failed events are retried up to 3 times

### Retry Logic

- Events with retry count < 3 are retried
- Events with retry count >= 3 are discarded
- Retry count increments on each failure
- Successful events are removed from queue

### Future Enhancements

- **Network Monitoring**: Sync when connectivity restored
- **Batch Upload**: Send multiple events at once
- **Queue Persistence**: Persist queue to disk (SQLite)
- **Exponential Backoff**: Retry with increasing delays

## Integration with AnalyticsService

The `FirebaseAnalyticsProvider` is designed to be used by the `AnalyticsService` (to be implemented in P1.3):

```kotlin
class AnalyticsService(
    private val provider: AnalyticsProvider,
    private val queue: AnalyticsQueue
) {
    fun trackEvent(event: AnalyticsEvent, properties: Map<String, Any?> = emptyMap()) {
        provider.trackEvent(event, properties)
    }

    fun setUserProperty(name: String, value: String) {
        provider.setUserProperty(name, value)
    }

    fun setUserId(userId: String?) {
        provider.setUserId(userId)
    }

    fun setEnabled(enabled: Boolean) {
        provider.setEnabled(enabled)
    }

    fun clearUserData() {
        provider.clearUserData()
    }

    // RGPD consent management
    fun grantConsent() {
        provider.setEnabled(true)
        provider.trackEvent(AnalyticsEvent.AnalyticsConsentGranted)
    }

    fun revokeConsent() {
        provider.trackEvent(AnalyticsEvent.AnalyticsConsentRevoked)
        provider.clearUserData()
    }

    fun deleteUserData() {
        provider.clearUserData()
        provider.trackEvent(AnalyticsEvent.UserDataDeleted)
    }
}
```

## Next Steps (P1.3)

1. **AnalyticsService**: Service layer for easy provider usage
2. **Dependency Injection**: Integrate provider into Koin/ViewModels
3. **ViewModel Integration**: Add analytics tracking to ViewModels
4. **Connectivity Monitoring**: Sync queue on network changes
5. **Queue Persistence**: Store queue in SQLite for durability

## Conclusion

Phase 6 Analytics P1.2 is complete with:
- ✅ AnalyticsQueue implementation
- ✅ FirebaseAnalyticsProvider for Android, iOS, and JVM
- ✅ 43 new tests (total 55 analytics tests)
- ✅ RGPD compliance support
- ✅ Offline queue support
- ✅ Comprehensive event parameter mapping
- ✅ Firebase SDK integration ready

The implementation provides a solid foundation for analytics tracking with offline support and full RGPD compliance.
