# Tasks: Connect iOS Notifications to Real Data

## Phase 1 — ViewModel & Data Layer

- [x] **@codegen** — `InboxViewModel.swift` (ViewModel connecté au NotificationService KMP + mapping NotificationMessage → InboxItemModel)
- [x] **@codegen** — `NotificationPreferencesView.swift` + `NotificationPreferencesViewModel.swift` (toggles par type, quiet hours, sound/vibration)

## Phase 2 — UI Branchement

- [x] **@codegen** — Modifier `InboxView.swift` (remplacer mock par ViewModel, pull-to-refresh, binding unreadCount)
- [x] **@codegen** — Modifier `InboxDetailView.swift` (déjà branché via markItemAsRead → viewModel)
- [x] **@codegen** — Brancher `NotificationPreferencesView` dans navigation (ProfileSettingsSheet + ContentView sheet)

## Phase 3 — Tests

- [x] **@tests** — `InboxViewModelTests.swift` (initial state, markAsRead, markAllAsRead, delete, item model tests)

## Phase 4 — Validation

- [ ] **@integrator** — Vérification cohérence design system iOS + compilation
- [ ] **@validator** — Vérification FC&IS et patterns KMP interop
- [ ] **@review** — Verdict final

## Verification

```bash
# Build iOS (vérifie compilation Swift)
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16 Pro' build 2>&1 | tail -5

# Tests JVM (non-régression)
./gradlew shared:jvmTest --no-configuration-cache
```
