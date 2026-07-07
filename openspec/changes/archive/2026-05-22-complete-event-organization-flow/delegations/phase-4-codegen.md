# Delegation: @codegen Phase 4 Transport Remediation

## Scope
Fix only the RED tests produced by `@tests` for Phase 4 transport remediation. Do not modify tests.

## Context
- Change: `complete-event-organization-flow`
- Phase: `4. Transport Planning`
- Source of truth: `phase-4-remediation.md`
- Related task: `4.6`

## Required Fixes
1. Require an explicit organizer actor for every shared/local `TransportRepository.generatePlan` write path.
   - Remove the null actor bypass.
   - Rejected generation must persist no plan/routes and queue no sync metadata.
2. Make Android transport direct entry repository-backed for event/access/date state.
   - Preserve existing in-memory state when present.
   - Fall back to local repositories when `EventManagementViewModel.state.selectedEvent` is empty.
   - Do not fabricate workflow status or organizer access.
3. Align shared organizer departure guards with backend access rules.
   - Organizer departure writes are allowed only for confirmed participants on the event.
   - Unknown, invited, pending, and declined participant targets must be rejected before persistence/sync.

## Expected Files
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/transport/TransportRepository.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt`
- Additional production files only if needed to reuse existing repository APIs cleanly.

## Verification Commands
```bash
./gradlew :shared:jvmTest --tests com.guyghost.wakeve.transport.TransportOfflineRepositoryPhase4Test --no-daemon
./gradlew :composeApp:testDebugUnitTest --tests com.guyghost.wakeve.navigation.TransportNavigationContractTest --no-daemon --no-configuration-cache
./gradlew :server:test --tests com.guyghost.wakeve.routes.EventOrganizationPhase4TransportRoutesTest --no-daemon
./gradlew :shared:jvmTest --no-daemon
./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon --no-configuration-cache
openspec validate complete-event-organization-flow --strict
```

## Return Format
- Production files modified.
- Verification commands and results.
- Confirm tests were not modified.
- Confirm Phase 5 was not started.
