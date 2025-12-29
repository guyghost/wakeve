# Calendar Integration Guide - iOS

This document describes the Calendar Integration UI components for the iOS Wakeve application.

## Overview

The Calendar Integration feature provides two main capabilities:
1. **Add to Calendar** - Adds the event to the device's native Calendar app (EventKit)
2. **Share Invitation** - Generates and shares an ICS invitation file

## Files Created

### Views

#### `CalendarIntegrationCard.swift`
Main card component that displays:
- Current calendar status (Not in calendar / Added to calendar / Error)
- "Add to Calendar" button
- "Share Invitation" button
- Error handling and loading states

**Location**: `iosApp/iosApp/Views/CalendarIntegrationCard.swift`

**Usage**:
```swift
CalendarIntegrationCard(
    event: event,
    userId: userId,
    onAddToCalendar: {
        // Handle successful calendar addition
        print("Event added to calendar")
    },
    onShareInvitation: {
        // Handle successful invitation share
        print("Invitation shared")
    }
)
```

**Features**:
- Liquid Glass design with `.glassCard()` and `.thinGlass()` modifiers
- Smooth loading states with ProgressView
- Error alerts with user-friendly messages
- Accessibility labels and hints
- ICS file generation for calendar invitations
- UIActivityViewController integration for sharing

### Components

#### `AddToCalendarButton.swift`
Reusable button component for standalone use.

**Location**: `iosApp/iosApp/Components/AddToCalendarButton.swift`

**Usage**:
```swift
AddToCalendarButton(
    event: event,
    isLoading: false,
    isEnabled: true,
    action: {
        // Handle button tap
    }
)
```

**Features**:
- Customizable loading and enabled states
- Accessible button with semantic labels
- Blue primary color matching design system
- Continuous corner radius (12pt)

## Integration with Shared Code

### CalendarService Integration

The CalendarService is available in the shared Kotlin Multiplatform code at:
```
shared/src/commonMain/kotlin/com/guyghost/wakeve/calendar/CalendarService.kt
```

Key methods to integrate:
- `addToNativeCalendar(eventId: String, participantId: String): Result<Unit>`
- `generateICSInvitation(eventId: String, invitees: List<String>): ICSDocument`
- `updateNativeCalendarEvent(eventId: String, participantId: String): Result<Unit>`

### Implementation Steps

1. **Create a ViewModel** (recommended approach):
```swift
class CalendarViewModel: ObservableObject {
    let calendarService: CalendarService
    @Published var isLoading = false
    @Published var error: String?
    
    func addToCalendar(eventId: String, participantId: String) async {
        isLoading = true
        do {
            let result = try await calendarService.addToNativeCalendar(
                eventId: eventId,
                participantId: participantId
            )
            // Handle result
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}
```

2. **Update CalendarIntegrationCard** to use the ViewModel:
```swift
struct CalendarIntegrationCard: View {
    @ObservedObject var viewModel: CalendarViewModel
    
    private func handleAddToCalendar() {
        Task {
            await viewModel.addToCalendar(
                eventId: event.id,
                participantId: userId
            )
        }
    }
}
```

## EventKit Integration

The underlying iOS implementation uses EventKit framework through the Kotlin Multiplatform code.

**Platform Implementation**:
```
shared/src/iosMain/kotlin/com/guyghost/wakeve/calendar/PlatformCalendarService.ios.kt
```

This provides:
- EKEventStore integration for native calendar access
- Event creation and modification
- Automatic timezone handling

## Design System Compliance

All components follow the Liquid Glass design guidelines:

- **Card styling**: `.glassCard()` and `.thinGlass()` modifiers
- **Corners**: `.continuousCornerRadius()` for smooth Apple-style corners
- **Colors**: Blue for primary actions, green for success states
- **Typography**: System fonts with appropriate weights
- **Spacing**: 12-16pt padding, 12pt between sections
- **Shadows**: Subtle shadows matching Liquid Glass aesthetic

See `iosApp/LIQUID_GLASS_GUIDELINES.md` for complete guidelines.

## User Flow

### Add to Calendar
1. User sees "Add to Calendar" button in CalendarIntegrationCard
2. Taps button → "Adding..." state appears
3. CalendarService creates event in native calendar (EventKit)
4. Status updates to "Added to calendar" with green checkmark
5. Success callback fires

### Share Invitation
1. User taps "Share Invitation" button
2. ICS file is generated from event data
3. UIActivityViewController presents sharing options
4. User selects how to share (Email, Messages, etc.)
5. File is shared and success callback fires

## Accessibility

All components include:
- Semantic button labels
- Accessibility hints explaining functionality
- Support for Dynamic Type
- VoiceOver compatibility
- High contrast support (automatic via Liquid Glass materials)

## Error Handling

The component handles:
- Calendar permission denied
- File system errors when generating ICS
- Event not found errors
- Generic network/system errors

All errors are presented to the user via alert dialogs with clear messaging.

## State Management

The card manages:
- `calendarStatus`: Current display state (notInCalendar / inCalendar / loading / error)
- `isAddingToCalendar`: Add to calendar button loading state
- `isShareInvitationLoading`: Share button loading state
- `showError`: Alert presentation state
- `errorMessage`: Error message content

## Testing

### Unit Tests
Add tests for:
- ICS content generation
- Error handling
- State transitions

### UI Tests
Test:
- Button interactions
- Loading states
- Error presentation
- Share sheet presentation

### Manual Testing
1. Add event to calendar and verify in native Calendar app
2. Share invitation and verify ICS file format
3. Test with various event types (polling, confirmed, organizing)
4. Test error states (permission denied, etc.)

## Future Enhancements

Potential improvements:
1. **Calendar Status Checking**: Check if event already in calendar using EventKit
2. **Update Existing Events**: Modify calendar events when Wakeve event changes
3. **Attendee Management**: Invite other participants directly
4. **Reminder Sync**: Sync Wakeve notifications with calendar reminders
5. **Multiple Calendar Support**: Choose which calendar to save to
6. **Recurring Events**: Support multi-day events and recurring patterns

## Troubleshooting

### Calendar Permission Issues
If "Calendar" permission is denied:
1. Check Info.plist includes `NSCalendarsUsageDescription`
2. Direct user to Settings > Wakeve > Calendar
3. Show appropriate error message

### ICS File Not Sharing
If share sheet doesn't appear:
1. Ensure temporary file is created successfully
2. Check UIApplication.shared.connectedScenes access
3. Verify rootViewController is available

### Event Not Added
Possible causes:
1. Calendar permission not granted
2. Invalid event dates
3. EventKit error in native code

## Related Documentation

- **CalendarService**: `shared/src/commonMain/kotlin/com/guyghost/wakeve/calendar/CalendarService.kt`
- **iOS Implementation**: `shared/src/iosMain/kotlin/com/guyghost/wakeve/calendar/PlatformCalendarService.ios.kt`
- **Liquid Glass Guidelines**: `iosApp/LIQUID_GLASS_GUIDELINES.md`
- **Design System**: `.opencode/design-system.md`
- **Phase 5 Cleanup**: `openspec/changes/cleanup-complete-calendar-management/`

## Support

For issues or improvements:
1. Check this guide and linked documentation
2. Review test cases in `shared/src/commonTest/kotlin/com/guyghost/wakeve/CalendarServiceTest.kt`
3. File an issue in the Wakeve project
