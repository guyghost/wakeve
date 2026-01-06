# Fix iOS Compilation Errors

## Context
The iOS project has multiple compilation errors due to:
1. Duplicate declarations of UI components across multiple files
2. Inaccessible types (BadgeType) from LiquidGlassBadge

## Objectives
- Remove all duplicate component declarations
- Fix BadgeType accessibility issues
- Ensure iOS project compiles successfully
- Maintain code functionality and design system conformance

## Scope
- Files to fix:
  - `iosApp/iosApp/Components/SharedComponents.swift` (remove FilterChip)
  - `iosApp/iosApp/Views/InboxView.swift` (keep FilterChip)
  - `iosApp/iosApp/Views/BudgetDetailView.swift` (remove FilterChip)
  - `iosApp/iosApp/Views/ActivityPlanningView.swift` (remove LiquidGlassCard)
  - `iosApp/iosApp/Views/ScenarioManagementView.swift` (remove all private duplicates)
  - `iosApp/iosApp/Views/ExploreView.swift` (remove EventStatusBadge, fix BadgeType)
  - `iosApp/iosApp/Views/ModernHomeView.swift` (keep EventStatusBadge)
  - `iosApp/iosApp/Views/ModernPollResultsView.swift` (remove duplicates)
  - `iosApp/iosApp/Views/PollResultsView.swift` (keep components)
  - `iosApp/iosApp/Views/ProfileScreen.swift` (fix BadgeType)
  - `iosApp/iosApp/Views/ScenarioDetailView.swift` (fix BadgeType)

## Constraints
- Platform: iOS (SwiftUI)
- Design System: Liquid Glass
- No breaking changes to UI behavior

## Tasks
- [ ] Analyze duplicate declarations and decide which to keep
- [ ] Remove duplicate FilterChip declarations (keep in InboxView or make it shared)
- [ ] Remove duplicate LiquidGlassCard declarations (use UIComponents version)
- [ ] Remove duplicates in ScenarioManagementView
- [ ] Fix BadgeType accessibility by exposing it or using different approach
- [ ] Verify iOS compilation succeeds
- [ ] Run iOS tests

## Files Modified
- `iosApp/iosApp/Components/SharedComponents.swift`
- `iosApp/iosApp/Views/InboxView.swift`
- `iosApp/iosApp/Views/BudgetDetailView.swift`
- `iosApp/iosApp/Views/ActivityPlanningView.swift`
- `iosApp/iosApp/Views/ScenarioManagementView.swift`
- `iosApp/iosApp/Views/ExploreView.swift`
- `iosApp/iosApp/Views/ModernHomeView.swift`
- `iosApp/iosApp/Views/ModernPollResultsView.swift`
- `iosApp/iosApp/Views/PollResultsView.swift`
- `iosApp/iosApp/Views/ProfileScreen.swift`
- `iosApp/iosApp/Views/ScenarioDetailView.swift`
- `iosApp/iosApp/UIComponents/LiquidGlassBadge.swift` (may need modification)
