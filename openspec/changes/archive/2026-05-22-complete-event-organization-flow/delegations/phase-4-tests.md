# Delegation: @tests Phase 4 Transport Remediation

## Scope
Add RED regression tests only. Do not modify production code.

## Context
- Change: `complete-event-organization-flow`
- Phase: `4. Transport Planning`
- Source of truth: `phase-4-remediation.md`
- Related task: `4.5`

## Required RED Coverage
1. Shared `TransportRepository.generatePlan` rejects local/offline calls without an explicit organizer actor.
   - No `transport_plan` row is created.
   - No `transport_route` row is created.
   - No pending transport sync metadata is queued.
2. Android direct route `event/{eventId}/transport` loads event status, organizer identity, participant access, and confirmed date from local repositories when `EventManagementViewModel.state.selectedEvent` is empty.
3. Shared organizer departure writes reject unknown or unconfirmed participant IDs before persistence and before sync metadata is queued.

## Expected Files
- `shared/src/jvmTest/kotlin/com/guyghost/wakeve/transport/TransportOfflineRepositoryPhase4Test.kt`
- `composeApp/src/androidUnitTest/kotlin/com/guyghost/wakeve/navigation/TransportNavigationContractTest.kt`
- Add another test file only if the existing files cannot express the contract clearly.

## Verification Commands
```bash
./gradlew :shared:jvmTest --tests com.guyghost.wakeve.transport.TransportOfflineRepositoryPhase4Test --no-daemon
./gradlew :composeApp:testDebugUnitTest --tests com.guyghost.wakeve.navigation.TransportNavigationContractTest --no-daemon --no-configuration-cache
```

## Return Format
- Files modified.
- Which tests are RED and why.
- Command outputs summarized with failing test names.
- Confirm production files were not modified.
