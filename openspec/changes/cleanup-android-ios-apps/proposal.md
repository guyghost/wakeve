# Proposal: Cleanup Android & iOS Apps

## Change ID
`cleanup-android-ios-apps`

## Status
**DRAFT** - Pending Review

## Summary

Clean up and consolidate the Android (Kotlin/Compose) and iOS (Swift/SwiftUI) applications by:
1. Removing orphaned/deprecated files that are no longer used
2. Wiring missing screens to their respective navigation systems
3. Eliminating duplicate screen implementations
4. Ensuring feature parity between platforms

## Problem Statement

After extensive development, the codebase has accumulated:

### Android (`wakeveApp/src/`)
- **8 screens with routes defined but NOT fully wired** in navigation
- **Duplicate screens** in both `androidMain` and `commonMain` (e.g., `SettingsScreen`, `ScenarioDetailScreen`)
- **Orphaned screens** that are no longer referenced (e.g., `EventsTabScreen`, `ScenarioListScreen`)
- **Unused ViewModels** that may have been replaced

### iOS (`wakeveApp/wakeveApp/`)
- **~38 Swift view files** with potential orphans
- **Deprecated views** marked for removal (e.g., `AppleInvitesEventCreationView.swift`)
- **Duplicate views** (Modern vs Legacy: `PollVotingView` vs `ModernPollVotingView`)
- **Views not connected to navigation** (Settings, MeetingList, MeetingDetail in AppView enum but not always accessible)

This leads to:
- Increased bundle size
- Developer confusion about which implementation to use
- Maintenance burden
- Potential bugs from stale code paths

## Scope

### In Scope
1. **Delete orphaned Android files** (~4 files, ~800 lines)
2. **Delete orphaned iOS files** (~5 files, ~1,500 lines)
3. **Wire missing Android screens** to `WakevNavHost.kt`
4. **Wire missing iOS views** to `ContentView.swift` navigation
5. **Consolidate duplicate implementations** (prefer `commonMain` over `androidMain`)
6. **Update imports** where necessary
7. **Verify tests still pass** after cleanup

### Out of Scope
- Adding new features
- Refactoring working code
- Design system changes
- Backend/server changes
- Shared module changes (unless directly related to cleanup)

## Analysis

### Android Files to DELETE

| File | Reason | Lines |
|------|--------|-------|
| `wakeveApp/src/androidMain/.../EventsTabScreen.kt` | Functionality moved to HomeScreen | ~150 |
| `wakeveApp/src/androidMain/.../ScenarioListScreen.kt` | Replaced by ScenarioManagementScreen | ~200 |
| `wakeveApp/src/androidMain/.../ScenarioDetailScreen.kt` | Duplicate of commonMain version | ~180 |
| `wakeveApp/src/androidMain/.../ScenarioComparisonScreen.kt` | Duplicate of commonMain version | ~200 |
| `wakeveApp/src/androidMain/.../GetStartedScreen.kt` | May be duplicate of commonMain (verify) | ~100 |

### iOS Files to DELETE

| File | Reason | Lines |
|------|--------|-------|
| `wakeveApp/wakeveApp/Views/AppleInvitesEventCreationView.swift` | Deprecated, replaced by CreateEventView | ~300 |
| `wakeveApp/wakeveApp/Views/EventsTabView.swift` | Replaced by ModernHomeView | ~200 |
| `wakeveApp/wakeveApp/Views/PollVotingView.swift` | Replaced by ModernPollVotingView | ~250 |
| `wakeveApp/wakeveApp/Views/PollResultsView.swift` | Replaced by ModernPollResultsView | ~200 |
| `wakeveApp/wakeveApp/Views/ExploreTabView.swift` | May be duplicate of ExploreView (verify) | ~150 |

### Android Screens to WIRE (routes defined but incomplete/placeholder)

| Screen | Route | Current Status |
|--------|-------|----------------|
| `BudgetOverviewScreen` | `event/{eventId}/budget` | Route defined, screen exists, NOT wired |
| `BudgetDetailScreen` | `event/{eventId}/budget/{budgetItemId}` | Route defined, screen exists, NOT wired |
| `AccommodationScreen` | `event/{eventId}/accommodation` | Route defined, screen exists, NOT wired |
| `MealPlanningScreen` | `event/{eventId}/meals` | Route defined, screen exists, NOT wired |
| `EquipmentChecklistScreen` | `event/{eventId}/equipment` | Route defined, screen exists, NOT wired |
| `ActivityPlanningScreen` | `event/{eventId}/activities` | Route defined, screen exists, NOT wired |
| `CommentsScreen` | `event/{eventId}/comments` | Route defined, screen exists, NOT wired |
| `ScenarioManagementScreen` | `event/{eventId}/scenarios/manage` | Route defined, screen exists, NOT wired |

### iOS Views to WIRE (defined in AppView enum but not accessible)

| View | AppView case | Current Status |
|------|--------------|----------------|
| `SettingsView` | (not in AppView) | Exists but not navigable from Profile |
| `MeetingListView` | `.meetingList` | Defined but no navigation path |
| `MeetingDetailView` | `.meetingDetail` | Defined but no navigation path |
| `ScenarioDetailView` | `.scenarioDetail` | Defined but incomplete navigation |

## Feature Parity Matrix

| Feature | Android | iOS | Notes |
|---------|---------|-----|-------|
| Event Creation | commonMain | SwiftUI | Both use wizard flow |
| Event List | commonMain | SwiftUI | OK |
| Event Detail | commonMain | SwiftUI | OK |
| Participant Management | commonMain | SwiftUI | OK |
| Poll Voting | commonMain | SwiftUI (Modern) | Delete legacy iOS |
| Poll Results | commonMain | SwiftUI (Modern) | Delete legacy iOS |
| Scenario List | NOT wired | SwiftUI | Wire Android |
| Scenario Detail | commonMain | SwiftUI | OK |
| Scenario Comparison | commonMain | SwiftUI | OK |
| Budget Overview | NOT wired | SwiftUI | Wire Android |
| Budget Detail | NOT wired | SwiftUI | Wire Android |
| Accommodation | NOT wired | SwiftUI | Wire Android |
| Meal Planning | NOT wired | SwiftUI | Wire Android |
| Equipment Checklist | NOT wired | SwiftUI | Wire Android |
| Activity Planning | NOT wired | SwiftUI | Wire Android |
| Comments | NOT wired | N/A | Wire Android, add iOS |
| Meetings | Partial | Partial | Both need work |
| Settings | Wired | NOT wired | Wire iOS |

## Impact

### Positive
- **~2,500 lines removed** across both platforms
- Clearer codebase for developers
- Reduced cognitive load
- Smaller bundle sizes
- Easier maintenance

### Risks
- Breaking existing navigation flows (mitigated by testing)
- Missing edge cases in wiring (mitigated by manual QA)

## Implementation Plan

See `tasks.md` for detailed implementation checklist.

## Success Criteria

1. All tests pass after cleanup
2. No orphaned files remain
3. All defined routes are wired to screens
4. Feature parity matrix shows no gaps for core features
5. App builds and runs on both platforms
6. No compilation errors or warnings from deleted code

## References

- [AGENTS.md](../../../AGENTS.md) - Project conventions
- [WakevNavHost.kt](../../wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakevNavHost.kt) - Android navigation
- [ContentView.swift](../../wakeveApp/wakeveApp/ContentView.swift) - iOS navigation
- [Screen.kt](../../wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/Screen.kt) - Route definitions
