# iOS Meeting UI — add-ios-meeting-ui

Implémenter les vues SwiftUI Meeting List + Meeting Detail pour iOS.
Les ViewModels sont déjà prêts (`MeetingListViewModel.swift`, `MeetingDetailViewModel.swift`).

## Context
- ViewModels prêts: `MeetingListViewModel.swift` (226 LOC), `MeetingDetailViewModel.swift`
- Android référence: `MeetingListScreen.kt` (551 LOC, commonMain)
- Sheet existante: `MeetingGenerateLinkSheet.swift` (patterns à réutiliser)
- ContentView.swift: remplacer "Meeting List - Coming Soon" / "Meeting Detail - Coming Soon"
- `selectedMeetingId: String?` déjà dans `AuthenticatedView`

## Goals
- `MeetingListView.swift` — liste des meetings avec statut, platform icon, actions
- `MeetingDetailView.swift` — détail meeting, génération lien, partage, annulation
- `MeetingRowView.swift` — composant ligne réutilisable
- Branchement `ContentView.swift`
- Tests `MeetingListViewModelTests.swift`
- Non-régression: 1158 tests JVM

## Pipeline Checklist
- [x] @codegen — MeetingListView.swift + MeetingRowView.swift
- [x] @codegen — MeetingDetailView.swift
- [x] @codegen — Branchement ContentView.swift
- [x] @tests — MeetingViewModelTests.swift
- [x] @integrator — Cohérence design system
- [x] @validator — FC&IS + state machine patterns
- [x] @review — Verdict final

## Verification
```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme WakeveApp \
  -destination 'id=80929F60-B378-42CC-B1D0-C4AD1102AEC8' \
  build 2>&1 | grep -E "error:|BUILD SUCCEEDED|BUILD FAILED"

./gradlew shared:jvmTest --no-configuration-cache 2>&1 | tail -3
```

## Notes
- `MeetingListViewModel` expose: `state`, `meetings` (depuis state.meetings), `isLoading`, `isEmpty`
- `MeetingDetailViewModel` expose: `meeting: VirtualMeeting?`, `showDeleteConfirm`, `isEditing`
- Pattern state machine: `dispatch(Intent...)` — voir `ScenarioListViewModel.swift` pour référence
- `VirtualMeeting` type KMP: `id`, `title`, `platform`, `meetingUrl`, `scheduledFor`, `status`
- `MeetingPlatform`: `.zoom`, `.googleMeet`, `.faceTime`, `.teams`, `.other`
- Design: `.regularMaterial` cards, `.ultraThinMaterial` backgrounds
