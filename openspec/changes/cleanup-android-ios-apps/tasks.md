# Tasks: Cleanup Android & iOS Apps

## Change ID
`cleanup-android-ios-apps`

## Phase 1: Analysis & Verification (Before Deletion)

### 1.1 Verify Android Orphans
- [x] Confirm `EventsTabScreen.kt` is not imported anywhere
- [x] Confirm `ScenarioListScreen.kt` is replaced by ScenarioManagementScreen
- [x] Confirm `ScenarioDetailScreen.kt` in androidMain duplicates commonMain
- [x] Confirm `ScenarioComparisonScreen.kt` in androidMain duplicates commonMain
- [ ] Check if `GetStartedScreen.kt` in androidMain is used or duplicates commonMain

### 1.2 Verify iOS Orphans
- [x] Confirm `AppleInvitesEventCreationView.swift` is deprecated (check for imports)
- [x] Confirm `EventsTabView.swift` is replaced by ModernHomeView
- [x] Confirm `PollVotingView.swift` is replaced by ModernPollVotingView
- [x] Confirm `PollResultsView.swift` is replaced by ModernPollResultsView
- [ ] Check if `ExploreTabView.swift` duplicates `ExploreView.swift`

---

## Phase 2: Delete Orphaned Android Files

### 2.1 Delete Confirmed Orphans
- [x] Delete `wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/EventsTabScreen.kt`
- [x] Delete `wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/ScenarioListScreen.kt`

### 2.2 Consolidate Duplicates (Android)
- [x] Compare `ScenarioDetailScreen.kt` (androidMain vs commonMain) - delete androidMain if identical
- [x] Compare `ScenarioComparisonScreen.kt` (androidMain vs commonMain) - delete androidMain if identical
- [ ] Compare `GetStartedScreen.kt` (androidMain) vs usage in WakevNavHost - keep only one

### 2.3 Update Imports
- [x] Remove any lingering imports of deleted files in WakevNavHost.kt
- [x] Fix any compilation errors after deletion

---

## Phase 3: Delete Orphaned iOS Files

### 3.1 Delete Confirmed Orphans
- [x] Delete `wakeveApp/wakeveApp/Views/AppleInvitesEventCreationView.swift`
- [x] Delete `wakeveApp/wakeveApp/Views/EventsTabView.swift`

### 3.2 Delete Legacy Duplicates (iOS)
- [x] Delete `wakeveApp/wakeveApp/Views/PollVotingView.swift` (keep ModernPollVotingView)
- [x] Delete `wakeveApp/wakeveApp/Views/PollResultsView.swift` (keep ModernPollResultsView)

### 3.3 Consolidate Duplicates (iOS)
- [ ] Compare `ExploreTabView.swift` vs `ExploreView.swift` - keep one, delete other

### 3.4 Update Imports
- [x] Remove any lingering imports of deleted files in ContentView.swift
- [x] Fix any compilation errors after deletion

---

## Phase 4: Wire Missing Android Screens

### 4.1 Add Budget Screens to WakevNavHost
- [x] Wire `Screen.BudgetOverview` → `BudgetOverviewScreen`
- [x] Wire `Screen.BudgetDetail` → `BudgetDetailScreen`

### 4.2 Add Logistics Screens to WakevNavHost
- [x] Wire `Screen.Accommodation` → `AccommodationScreen`
- [x] Wire `Screen.MealPlanning` → `MealPlanningScreen`
- [x] Wire `Screen.EquipmentChecklist` → `EquipmentChecklistScreen`
- [x] Wire `Screen.ActivityPlanning` → `ActivityPlanningScreen`

### 4.3 Add Communication Screens to WakevNavHost
- [x] Wire `Screen.Comments` → `CommentsScreen`

### 4.4 Add Scenario Management to WakevNavHost
- [x] Wire `Screen.ScenarioManagement` → `ScenarioManagementScreen`

---

## Phase 5: Wire Missing iOS Views

### 5.1 Add Settings Navigation
- [x] Add SettingsView navigation from ProfileTabView
- [x] Ensure back navigation works (via sheet dismiss)

### 5.2 Add Meeting Navigation
- [x] Wire `.meetingList` case in ContentView to MeetingListView
- [x] Wire `.meetingDetail` case in ContentView to MeetingDetailView
- [x] Add navigation path from EventDetail to Meetings

### 5.3 Complete Scenario Navigation
- [x] Wire `.scenarioDetail` case properly with selectedScenario
- [x] Wire `.budgetDetail` case for budget detail navigation

---

## Phase 6: Verification & Testing

### 6.1 Build Verification
- [x] Android: `./gradlew wakeveApp:assembleDebug` passes ✅
- [ ] iOS: Xcode build succeeds (or skip if not on macOS)

### 6.2 Test Execution
- [x] Run `./gradlew shared:jvmTest` - 648/671 tests pass (23 pre-existing failures in VoiceAccessibilityTest)
- [ ] Run `./gradlew wakeveApp:testDebugUnitTest` - all tests pass

### 6.3 Navigation Testing (Manual)
- [ ] Android: Verify all bottom tabs work
- [ ] Android: Verify event creation → participant → poll flow
- [ ] Android: Verify newly wired screens are accessible
- [ ] iOS: Verify all tabs work (if testing on macOS)
- [ ] iOS: Verify settings is accessible from profile

---

## Phase 7: Documentation Update

### 7.1 Update AGENTS.md
- [ ] Update screen inventory if needed
- [ ] Update feature parity matrix

### 7.2 Create Migration Notes
- [ ] Document any breaking changes for other developers

---

## Summary

| Phase | Tasks | Estimated Impact | Status |
|-------|-------|------------------|--------|
| Phase 1 | 10 verification tasks | No code changes | ✅ 8/10 done |
| Phase 2 | 6 deletion tasks | ~730 lines removed | ✅ 5/6 done |
| Phase 3 | 6 deletion tasks | ~1,100 lines removed | ✅ 5/6 done |
| Phase 4 | 8 wiring tasks | ~200 lines added | ✅ 8/8 done |
| Phase 5 | 5 wiring tasks | ~100 lines added | ✅ 6/6 done |
| Phase 6 | 6 verification tasks | No code changes | 🔄 2/6 done |
| Phase 7 | 2 documentation tasks | Minor updates | ⏳ 0/2 done |

**Net Impact**: ~1,500+ lines removed, cleaner codebase

## Additional Fixes Applied

During the wiring process, several pre-existing bugs were fixed:

1. **AndroidAuthService.kt** - Fixed corrupted file structure (duplicate closing braces)
2. **GoogleSignInProvider.kt** - Fixed return type mismatch in `signOut()` method
3. **MainActivity.kt** - Fixed several issues:
   - Wrong import `AndroidAuthService` → `AuthService`
   - `onNewIntent(intent: Intent?)` → `onNewIntent(intent: Intent)` (API change)
   - Added missing `Guest` case in `when` expression
   - Fixed `saveToken` → `storeAccessToken`/`storeTokenExpiry`
   - Fixed `error.message` references on sealed class without message property
4. **WakevNavHost.kt** - Fixed `ParticipantInfo` mapping from `List<String>` participants
5. **BudgetDetailScreen** - Fixed parameter name `eventId` → `budgetId`
