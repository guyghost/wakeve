# Calendar Implementation Summary

This document summarizes the Calendar Integration implemented in Wakeve (Phase 4 on Android, Phase 5 on iOS). It describes the architecture, key files, and tests added to support ICS generation and native calendar operations.

## Architecture Overview

- Core calendar business logic lives in the shared Kotlin Multiplatform module. This includes ICS generation (RFC 5545), timezone-aware date handling, attendee lists and reminder creation.
- Platform drivers implement an expect/actual `PlatformCalendarService` contract to interact with native APIs:
  - Android: CalendarContract (Calendar write/read)
  - iOS: EventKit (EKEvent / EKEventStore)
- UI integration uses platform-native components (Jetpack Compose on Android; SwiftUI + Liquid Glass on iOS). The Event Details view exposes a CalendarIntegrationCard with two actions: Add to calendar and Share invite.

## Key Files (Implemented)

Shared (Kotlin Multiplatform)
- shared/src/commonMain/kotlin/com/guyghost/wakeve/calendar/CalendarService.kt
  - Exposes methods: generateICSInvitation, addToNativeCalendar, updateNativeCalendarEvent, removeFromNativeCalendar, sendMeetingReminders
- shared/src/commonMain/kotlin/com/guyghost/wakeve/calendar/Models.kt
  - CalendarEvent, ICSDocument, MeetingReminderTiming, helpers for ICS building
- shared/src/commonTest/kotlin/com/guyghost/wakeve/calendar/CalendarServiceTest.kt
  - Unit tests covering ICS content, timezone handling, and platform driver result handling

Android (Phase 4)
- shared/src/androidMain/kotlin/com/guyghost/wakeve/calendar/PlatformCalendarService.android.kt
  - Actual implementation using CalendarContract with runtime permission checks
- composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/event/CalendarIntegrationCard.kt
  - Composable UI used in Event Details (exposes onAddToCalendar/onShareInvite)
- composeApp/src/androidMain/AndroidManifest.xml
  - Declares WRITE_CALENDAR permission
- composeApp/src/androidInstrumentedTest/kotlin/com/guyghost/wakeve/ui/event/CalendarIntegrationInstrumentedTest.kt
  - Instrumented tests for runtime permission flows and integration with PlatformCalendarService

iOS (Phase 5)
- shared/src/iosMain/kotlin/com/guyghost/wakeve/calendar/PlatformCalendarService.ios.kt
  - Actual implementation that bridges to EventKit via Kotlin/Native interop
- iosApp/iosApp/Views/CalendarIntegrationCard.swift
  - SwiftUI card in Event Details that calls shared CalendarService through the K/N bridge
- iosApp/iosApp/Services/CalendarPermissionsHelper.swift
  - Helper that requests EventKit access with `EKEventStore.requestAccess` and handles user-facing prompts
- iosApp/iosApp/Tests/CalendarIntegrationTests.swift
  - XCTest cases validating the UI wiring and permission behavior

Server / API
- server/src/main/kotlin/com/guyghost/wakeve/routes/CalendarRoutes.kt
  - Endpoints for ICS generation and download: `POST /api/events/{id}/calendar/ics`, `GET /api/events/{id}/calendar/ics`

## Tests

- Shared unit tests: `shared/src/commonTest/kotlin/com/guyghost/wakeve/calendar/CalendarServiceTest.kt` (10 tests)
  - generate ICS content
  - timezone/TZID handling
  - ICS filename sanitization
  - ICS VALARM reminders
  - PlatformCalendarService contract handling

- Android instrumented tests: `composeApp/src/androidInstrumentedTest/kotlin/com/guyghost/wakeve/ui/event/CalendarIntegrationInstrumentedTest.kt` (integration & permission flows)

- iOS XCTest: `iosApp/iosApp/Tests/CalendarIntegrationTests.swift` (UI wiring, permission prompts simulation)

## Notes and Decisions

- Reminders: ICS documents include VALARM entries so recipients importing the ICS get reminders. Native reminders for devices are orchestrated by CalendarService in combination with NotificationService for push-based reminders; the latter is planned in a follow-up.
- The integration intentionally keeps calendar logic in the shared module so ICS generation and timezone logic remain single-source-of-truth across platforms.
- When calendar access is denied, the app falls back to ICS generation + share sheet to preserve functionality.

## Future Improvements

- Allow selecting target calendar account (multiple calendars)
- Support recurring events and series updates
- Improve matching strategy for update/delete operations to avoid duplicates
- Offline ICS generation and batch operations
- Enhanced reminders via push notifications using NotificationService

---

Last updated: 28 December 2025
