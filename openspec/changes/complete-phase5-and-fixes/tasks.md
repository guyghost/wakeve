# Tasks: Complete Phase 5 and Critical Fixes

**Change ID**: `complete-phase5-and-fixes`  
**Status**: 🟡 In Progress  
**Last Updated**: 2026-01-30

---

## Phase 1: Critical TODOs (Security & Data Integrity) 🟡

### 🔴 Security (Priority: CRITICAL)
- [ ] **TODO-001**: Move OAuth config to BuildConfig/environment variables
  - File: `wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/MainActivity.kt:34`
  - Action: Extract to local.properties, add .gitignore
  
- [ ] **TODO-002**: Implement SHA-256 on iOS
  - File: `shared/src/iosMain/kotlin/com/guyghost/wakeve/SessionRepository.ios.kt:21`
  - Action: Use CommonCrypto or CryptoKit

### 🟡 Data Integrity (Priority: HIGH)
- [ ] **TODO-003**: Connect AccommodationRoutes to repository (12 TODOs)
  - File: `server/src/main/kotlin/com/guyghost/wakeve/routes/AccommodationRoutes.kt`
  - Action: Implement repository calls for all endpoints
  
- [ ] **TODO-004**: Fix hardcoded participantCount in BudgetRoutes
  - File: `server/src/main/kotlin/com/guyghost/wakeve/routes/BudgetRoutes.kt:378`
  - Action: Get from event repository
  
- [ ] **TODO-005**: Fix hardcoded paidBy in BudgetDetailScreen
  - File: `wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetDetailScreen.kt:238,669`
  - Action: Get actual user from auth state

### 🟡 Validation (Priority: HIGH)
- [ ] **TODO-006**: Add organizer validation in EventManagementStateMachine
  - File: `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/statemachine/EventManagementStateMachine.kt:668,731,826,900`
  - Action: Add userId parameter validation
  
- [ ] **TODO-007**: Add organizer validation in ScenarioManagementStateMachine
  - File: `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/statemachine/ScenarioManagementStateMachine.kt:422`
  - Action: Add userId parameter validation

### 🟡 Navigation & UX (Priority: MEDIUM)
- [ ] **TODO-008**: Get actual user data in WakevNavHost
  - File: `wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakevNavHost.kt:601,785`
  - Action: Connect to auth state
  
- [ ] **TODO-009**: Set proper deadline in DraftEventWizard
  - File: `wakeveApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/event/DraftEventWizard.kt:160`
  - Action: Calculate based on event date

---

## Phase 2: MeetingService Completion 🟡

### Requirements
- [ ] **MEET-001**: Create meeting provider interfaces
  - File: `shared/src/commonMain/kotlin/com/guyghost/wakeve/meeting/MeetingProvider.kt`
  - Action: Define interface for Zoom, Meet, FaceTime
  
- [ ] **MEET-002**: Implement Zoom provider
  - File: `shared/src/commonMain/kotlin/com/guyghost/wakeve/meeting/providers/ZoomProvider.kt`
  - Action: Generate Zoom meeting links via API
  
- [ ] **MEET-003**: Implement Google Meet provider
  - File: `shared/src/commonMain/kotlin/com/guyghost/wakeve/meeting/providers/GoogleMeetProvider.kt`
  - Action: Generate Meet links via API
  
- [ ] **MEET-004**: Implement FaceTime provider
  - File: `shared/src/commonMain/kotlin/com/guyghost/wakeve/meeting/providers/FaceTimeProvider.kt`
  - Action: Generate FaceTime links (iOS-specific)
  
- [ ] **MEET-005**: Update MeetingService
  - File: `shared/src/commonMain/kotlin/com/guyghost/wakeve/MeetingService.kt`
  - Action: Integrate providers, generate links
  
- [ ] **MEET-006**: Backend proxy for API security
  - File: `server/src/main/kotlin/com/guyghost/wakeve/routes/MeetingProxyRoutes.kt`
  - Action: Proxy requests to hide API keys
  
- [ ] **MEET-007**: Android UI integration
  - File: `wakeveApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/meeting/MeetingListScreen.kt`
  - Action: Connect to service, enable link generation
  
- [ ] **MEET-008**: iOS UI integration
  - File: `iosApp/iosApp/Views/MeetingListView.swift`
  - Action: Connect to service, enable link generation
  
- [ ] **MEET-009**: Write tests
  - File: `shared/src/commonTest/kotlin/com/guyghost/wakeve/meeting/MeetingServiceTest.kt`
  - Action: Test all providers, link generation
  
- [ ] **MEET-010**: Rewrite MeetingServiceStateMachineTest
  - File: `shared/src/commonTest/kotlin/com/guyghost/wakeve/presentation/statemachine/MeetingServiceStateMachineTest.kt`
  - Action: Proper mock implementations

---

## Phase 3: PaymentService Completion 🟡

### Requirements
- [ ] **PAY-001**: Create payment provider interfaces
  - File: `shared/src/commonMain/kotlin/com/guyghost/wakeve/payment/PaymentProvider.kt`
  - Action: Define interface for Tricount, Lydia, etc.
  
- [ ] **PAY-002**: Implement Tricount provider
  - File: `shared/src/commonMain/kotlin/com/guyghost/wakeve/payment/providers/TricountProvider.kt`
  - Action: Integrate Tricount API for expense sharing
  
- [ ] **PAY-003**: Implement Lydia provider (optional)
  - File: `shared/src/commonMain/kotlin/com/guyghost/wakeve/payment/providers/LydiaProvider.kt`
  - Action: Integrate Lydia API for cagnottes
  
- [ ] **PAY-004**: Update PaymentService
  - File: `shared/src/commonMain/kotlin/com/guyghost/wakeve/payment/PaymentService.kt`
  - Action: Integrate providers, track payments
  
- [ ] **PAY-005**: Backend integration
  - File: `server/src/main/kotlin/com/guyghost/wakeve/routes/PaymentRoutes.kt`
  - Action: API endpoints for payment management
  
- [ ] **PAY-006**: Android UI integration
  - File: Wake existing budget screens with payment actions
  - Action: Add payment links, tracking
  
- [ ] **PAY-007**: Write tests
  - File: `shared/src/commonTest/kotlin/com/guyghost/wakeve/payment/PaymentServiceTest.kt`
  - Action: Test providers, payment tracking

---

## Phase 4: E2E Tests 🟡

### Requirements
- [ ] **E2E-001**: Complete PRD workflow test
  - File: `shared/src/jvmTest/kotlin/com/guyghost/wakeve/e2e/PrdWorkflowE2ETest.kt`
  - Action: Full workflow: Creation → Poll → Scenarios → Selection → Organization → Finalization
  
- [ ] **E2E-002**: Multi-user collaboration test
  - File: New test file
  - Action: Test multiple users voting, commenting simultaneously
  
- [ ] **E2E-003**: Offline sync test
  - File: New test file
  - Action: Test offline creation, sync on reconnection, conflict resolution
  
- [ ] **E2E-004**: Cross-platform sync test
  - File: New test file
  - Action: Test Android + iOS sync scenarios
  
- [ ] **E2E-005**: Complete meeting workflow test
  - File: New test file
  - Action: Test meeting creation, link generation, invitations
  
- [ ] **E2E-006**: Payment workflow test
  - File: New test file
  - Action: Test expense creation, payment tracking, Tricount integration
  
- [ ] **E2E-007**: Error handling test
  - File: New test file
  - Action: Test network errors, API failures, recovery

---

## Phase 5: iOS Navigation Integration 🟡

### Requirements
- [ ] **IOS-001**: Update AppView enum
  - File: `iosApp/iosApp/ContentView.swift`
  - Action: Add missing cases (scenarioList, budgetDetail, accommodation, etc.)
  
- [ ] **IOS-002**: Add navigation states
  - File: `iosApp/iosApp/ContentView.swift`
  - Action: Add @State properties for navigation
  
- [ ] **IOS-003**: Implement navigation transitions
  - File: `iosApp/iosApp/ContentView.swift`
  - Action: Add NavigationLink and .sheet transitions
  
- [ ] **IOS-004**: Add action buttons in EventDetail
  - File: `iosApp/iosApp/Views/ModernEventDetailView.swift`
  - Action: Buttons for Budget, Logistique, Activités based on status
  
- [ ] **IOS-005**: Integrate native services
  - File: `iosApp/iosApp/Services/`
  - Action: Connect EventKit, permissions
  
- [ ] **IOS-006**: Test navigation flows
  - File: `iosApp/iosApp/Tests/NavigationFlowTests.swift`
  - Action: Test all navigation paths

---

## Phase 6: tidy-first Refactoring 🟡

### Requirements
- [ ] **TIDY-001**: Guard clauses in EventManagementStateMachine
  - File: `shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/statemachine/EventManagementStateMachine.kt`
  - Action: Invert conditions, reduce nesting (tidy-guard-clauses)
  
- [ ] **TIDY-002**: Extract magic strings to constants
  - Files: Multiple files with hardcoded IDs
  - Action: Create named constants (tidy-extract-constant)
  
- [ ] **TIDY-003**: Rename unclear variables
  - Files: Variables like `u` → `currentUser`
  - Action: Improve naming (tidy-rename-variable)
  
- [ ] **TIDY-004**: Normalize symmetries
  - Files: Similar patterns in state machines
  - Action: Consistent structure (tidy-normalize-symmetries)
  
- [ ] **TIDY-005**: Remove dead code
  - Files: All files
  - Action: Remove resolved TODOs, unused code (tidy-inline-dead-code)
  
- [ ] **TIDY-006**: Improve code cohesion
  - Files: Large files (>500 lines)
  - Action: Group related code (tidy-cohesion-order)

---

## Phase 7: Final Validation 🟡

### Requirements
- [ ] **VAL-001**: Run all tests
  - Command: `./gradlew test`
  - Target: 100% pass rate
  
- [ ] **VAL-002**: Build verification
  - Command: `./gradlew build`
  - Target: BUILD SUCCESSFUL
  
- [ ] **VAL-003**: TODO count verification
  - Command: `grep -r "TODO\|FIXME" shared/ wakeveApp/ server/ iosApp/ | wc -l`
  - Target: < 20 remaining (only non-critical)
  
- [ ] **VAL-004**: Security audit
  - Check: No hardcoded credentials
  - Target: All security TODOs resolved
  
- [ ] **VAL-005**: iOS build verification
  - Command: Build in Xcode
  - Target: No errors, all views accessible
  
- [ ] **VAL-006**: Documentation update
  - Files: Update README, USER_GUIDE, API.md
  - Action: Document new features

---

## Progress Tracking

| Phase | Tasks | Completed | Progress |
|-------|-------|-----------|----------|
| Phase 1 | 9 | 0 | 0% |
| Phase 2 | 10 | 0 | 0% |
| Phase 3 | 7 | 0 | 0% |
| Phase 4 | 7 | 0 | 0% |
| Phase 5 | 6 | 0 | 0% |
| Phase 6 | 6 | 0 | 0% |
| Phase 7 | 6 | 0 | 0% |
| **Total** | **51** | **0** | **0%** |

---

## Notes

- **Convention de commits**: Utiliser Conventional Commits
  - `fix(security): resolve OAuth hardcoded credentials`
  - `feat(meeting): implement Zoom link generation`
  - `tidy(statemachine): add guard clauses in EventManagement`
  
- **Tests**: Chaque changement de comportement doit avoir des tests
- **Ralph mode**: Max 10 iterations, s'arrêter si verdict = APPROVED ou BLOCKED
