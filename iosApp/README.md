# Wakeve iOS App

This is the iOS implementation of the Wakeve event scheduling application using SwiftUI.

## Architecture

The iOS app consists of several key SwiftUI views that follow the same flow as the Android Compose implementation:

### Views

1. **AppView** (`Views/AppView.swift`)
   - Main navigation controller
   - Manages navigation between different screens
   - Maintains app state including the EventRepository and current events list

2. **EventCreationView** (`Views/EventCreationView.swift`)
   - Allows organizers to create new events
   - Input fields for title, description, deadline
   - Add/remove time slot functionality
   - Validation of required fields

3. **ParticipantManagementView** (`Views/ParticipantManagementView.swift`)
   - Manage event participants
   - Add/remove participants with email validation
   - Transition event from DRAFT to POLLING status
   - Display current participants list

4. **PollVotingView** (`Views/PollVotingView.swift`)
   - Vote on proposed time slots
   - Three voting options: Yes, Maybe, No
   - Submit votes once all slots are voted on
   - Display voting deadline

5. **PollResultsView** (`Views/PollResultsView.swift`)
   - Display poll results with scoring
   - Visual score breakdown (Yes/Maybe/No counts)
   - Select and confirm final date for the event
   - Organizer-only confirmation

## Navigation Flow

```
EventList → EventCreation → ParticipantManagement → PollVoting → PollResults → EventList
```

## Shared Code

The iOS app shares the following with Android via KMP (Kotlin Multiplatform):

- `EventRepository` - Event data management
- `PollLogic` - Poll scoring logic
- Domain models: `Event`, `TimeSlot`, `Vote`, `Poll`, `EventStatus`
- Database layer via `DatabaseEventRepository`

## Building

The iOS app requires Xcode 15.0+ and iOS 14.0+ as the target.

### Prerequisites

1. Xcode 15.0 or later
2. iOS deployment target: 14.0+
3. The shared KMP framework must be built

### Build Steps

```bash
cd iosApp
xcodebuild -scheme iosApp build
```

## Testing

Unit tests are available in `iosAppTests/iOSAppTests.swift` and test:

- View initialization
- Navigation flow
- State management
- Integration with EventRepository

Run tests with:

```bash
xcodebuild test -scheme iosApp
```

## Key Differences from Android

1. **State Management**: Uses SwiftUI's `@State` and `@StateObject` instead of Compose's `remember`
2. **Navigation**: Manual navigation state machine instead of Jetpack Navigation Compose
3. **TextInput**: Uses SwiftUI's `TextField` and `TextEditor` instead of Compose's `TextField`
4. **Lists**: Uses SwiftUI's `List` and `ForEach` instead of Compose's `LazyColumn`
5. **Styling**: Uses SwiftUI's `buttonStyle()` modifiers instead of Compose's `ButtonDefaults`

## Future Enhancements

- Date picker integration for better deadline/slot selection
- Native calendar integration
- Offline persistence using Core Data
- Dark mode support
- Localization support
- Push notifications via UserNotifications framework
