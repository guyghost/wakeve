# Wakeve iOS App

This is the iOS implementation of the Wakeve event scheduling application using SwiftUI.

## Design System: Apple Liquid Glass

The iOS app follows Apple's Liquid Glass design guidelines, featuring:

- **Glass Morphism Effects**: Frosted glass appearance with blur and gradient borders
- **Material Design**: Deep color hierarchy with support for light and dark modes
- **Spacing System**: Consistent 8pt grid-based spacing (4, 8, 12, 16, 24, 32pt)
- **Typography**: Hierarchical font system with clear visual hierarchy
- **Color Palette**:
  - Primary Blue: `#007AFF` (Apple Blue)
  - Success Green: `#34C759`
  - Warning Orange: `#FF9500`
  - Error Red: `#FF3B30`
  - Dark Background: `#1C1C1E`
  - Light Background: `#FAFAFE`

### Design Components

The design system is implemented in `Design/LiquidGlassDesign.swift` and includes:

- `GlassView`: Reusable component for glass effect surfaces
- `LiquidGlassCard`: Pre-styled cards with glass morphism
- `LiquidGlassButtonStyle`: Custom primary button style
- `LiquidGlassSecondaryButtonStyle`: Secondary button style
- `LiquidGlassTextFieldStyle`: Styled input fields

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

## Localization

The UI is currently in French (`fr`) with support for expanding to other languages. All UI strings are defined in the view files.

## Future Enhancements

- Date picker integration for better deadline/slot selection
- Native calendar integration
- Offline persistence using Core Data (integration with SQLDelight)
- Expanded localization support (English, Spanish, German, etc.)
- Push notifications via UserNotifications framework
- Haptic feedback for user interactions
- Animated transitions between views
- Real-time collaboration features
