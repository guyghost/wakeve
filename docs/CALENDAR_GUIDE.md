# Calendar Integration Guide

This guide explains how to use Wakeve's Calendar Integration on Android and iOS. It covers permissions, UI interactions (Add to Calendar / Share ICS), and troubleshooting steps.

## Overview

Wakeve can generate ICS invitations and add confirmed events directly to users' native calendars (Android Calendar apps via CalendarContract; iOS calendars via EventKit). The calendar integration is implemented in the shared Kotlin module and exposed to platform UIs.

Supported actions:
- Generate and download/share ICS invitation (RFC 5545)
- Add event to native calendar for a specific participant
- Update or remove event from native calendar

## Android (Add to native calendar)

### Permissions

The Android app requires the WRITE_CALENDAR permission to create events in the user's calendar.

- Manifest:
  - `android.permission.WRITE_CALENDAR` is declared in `composeApp/src/androidMain/AndroidManifest.xml`.
- Runtime:
  - The app checks permission with `ContextCompat.checkSelfPermission()` and requests it at runtime if not granted.
  - If the user denies the permission, the app will show a friendly explanation and fall back to offering the ICS download/share flow.

### How to add an event

1. Open the Event Details screen for a confirmed event.
2. Tap the "Add to calendar" button in the Calendar card.
3. If prompted, allow calendar access.
4. Wakeve will add the event to the default calendar using CalendarContract and return a success/failure result.

UI notes:
- The CalendarIntegrationCard exposes two actions: `onAddToCalendar` and `onShareInvite`.
- The Android implementation is located at:
  - `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/event/CalendarIntegrationCard.kt`
  - Platform driver: `shared/src/androidMain/kotlin/com/guyghost/wakeve/calendar/PlatformCalendarService.android.kt`

### Sharing / Downloading ICS

If the user prefers not to grant calendar access, or to share an invite with others, use the "Share invite" action which generates an ICS file and opens Android's share sheet.
- ICS generation lives in the shared module: `shared/src/commonMain/kotlin/com/guyghost/wakeve/calendar/CalendarService.kt`.
- The server-side route to generate ICS (for download via web) is available at: `POST /api/events/{id}/calendar/ics` implemented in `server/src/main/kotlin/com/guyghost/wakeve/routes/CalendarRoutes.kt`.

## iOS (Add to native calendar)

### Permissions

iOS uses EventKit. The app requests calendar access using `EKEventStore.requestAccess(to: .event)` the first time it needs to create events. The helper is implemented in:
- `iosApp/iosApp/Services/CalendarPermissionsHelper.swift`

If the user denies permission, Wakeve will offer the ICS download/share flow as an alternative.

### How to add an event

1. Open the Event Details screen for a confirmed event.
2. Tap the "Add to calendar" button in the Calendar card.
3. If prompted, allow calendar access.
4. Wakeve will create an EKEvent and save it to the user's selected calendar. The iOS platform implementation in the shared module is:
  - `shared/src/iosMain/kotlin/com/guyghost/wakeve/calendar/PlatformCalendarService.ios.kt`
  - UI: `iosApp/iosApp/Views/CalendarIntegrationCard.swift`

### Sharing / Downloading ICS

The "Share invite" action creates an ICS document in the shared module and opens the iOS share sheet to allow sending via Messages, Mail, AirDrop, etc. ICS generation is performed by:
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/calendar/CalendarService.kt`

## Troubleshooting

Common issues and how to resolve them:

- Permission denied (Android):
  - The user denied WRITE_CALENDAR. Guide the user to App Settings > Permissions to enable calendar access, or use "Share invite" instead.
  - The app gracefully falls back to ICS generation and share.

- Permission denied (iOS):
  - User denied EventKit access. Present an explanation and buttons to open Settings.
  - Use "Share invite" as fallback.

- Event not appearing in calendar:
  - Verify the event was saved to the correct calendar (some devices default to a different calendar account).
  - Check for errors in logs. Instrumented Android tests and iOS tests validate the add/update/delete flows.

- Timezone issues:
  - Wakeve generates ICS with timezone-aware DTSTART/DTEND and includes TZID. If local calendar displays a different time, verify system timezone settings.

- Duplicate events after update:
  - Updates attempt to find the native calendar entry by the internal ID or matching fields; if not found, the service may create a new entry. In that case, remove the duplicate and ensure the app has permission to update events.

## Developer notes

- Calendar logic (ICS generation, timezone handling, attendee list) is centralized in the shared module to ensure consistent behavior across platforms.
- Platform drivers implement expect/actual interfaces to call native APIs (CalendarContract on Android, EventKit on iOS).
- Tests:
  - Shared tests: `shared/src/commonTest/kotlin/com/guyghost/wakeve/calendar/CalendarServiceTest.kt`
  - Android instrumented tests: `composeApp/src/androidInstrumentedTest/kotlin/com/guyghost/wakeve/ui/event/CalendarIntegrationInstrumentedTest.kt`
  - iOS XCTest targets: `iosApp/iosApp/Tests/CalendarIntegrationTests.swift`

## Feedback & Issues

If you encounter problems not covered here, please open an issue with:
- Steps to reproduce
- Device (model, OS version)
- Logs if available

Thank you for testing Calendar Integration in Wakeve!