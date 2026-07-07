# Tasks: Complete Event Organization Flow

## 0. Proposal Gate
- [x] 0.1 Validate this change with `openspec validate complete-event-organization-flow --strict`.
- [x] 0.2 Wait for human approval before implementation starts.

## 1. Invitation, Participants, and Access
- [x] 1.1 **@tests** Add shared access-policy and participant-state tests for invitation preview, RSVP, confirmed-attendee access, and declined/non-member denial.
- [x] 1.2 **@codegen** Implement participant lifecycle fields and confirmed-attendee access checks in shared repositories/state machines and backend routes.
- [x] 1.3 **@codegen** Wire Android/iOS invitation and participant management screens to local-first repositories.
- [x] 1.4 **@tests** Add repository/offline tests for invitation creation, acceptance, RSVP updates, and sync replay.
- [x] 1.5 **@review** Review authorization, IDOR resistance, and UX clarity before phase completion.

## 2. Polling, Confirmation, Calendar, and Notifications
- [x] 2.1 **@tests** Add state-machine tests for `DRAFT -> POLLING -> CONFIRMED`, invalid transitions, deadline enforcement, and date confirmation notifications.
- [x] 2.2 **@codegen** Ensure poll votes, attendance confirmation, calendar invitation generation, and transition notifications are local-first.
- [x] 2.3 **@tests** Add backend route tests for poll voting, confirmation, calendar ICS download, and notification scheduling authorization.
- [x] 2.4 **@docs** Update workflow documentation for organizer and participant polling paths.

## 3. Scenarios, Destination, and Lodging
- [x] 3.1 **@tests** Add shared tests for scenario creation/voting/scoring, destination/lodging selection, and final scenario selection.
- [x] 3.2 **@codegen** Connect scenario comparison to destination/lodging providers and persist selected destination/lodging choices.
- [x] 3.3 **@codegen** Wire Android/iOS scenario comparison, destination, and lodging UI for confirmed participants.
- [x] 3.4 **@tests** Add offline repository tests for scenario votes and selected lodging sync conflicts.
- [x] 3.5 **@review** Review comparison UX, accessibility, and business-rule guards.

## 4. Transport Planning
Remediation gate: see `phase-4-remediation.md`. Phase 5 MUST NOT start until tasks 4.5, 4.6, and 4.7 are complete and the Phase 4 review explicitly approves checking this section.

Reopened gate: the final read-only Phase 4 review found one remaining P1 on transport offline sync replayability. Phase 6 work MUST NOT continue until tasks 4.10 through 4.12 are complete and the final Phase 4 re-review explicitly approves transport again.

- [x] 4.1 **@tests** Add shared tests for departure locations, route optimization modes, missing-data states, and selected transport plan readiness.
- [x] 4.2 **@codegen** Implement local-first transport planning, provider abstraction persistence, and backend route contracts.
- [x] 4.3 **@codegen** Wire Android/iOS transport screens to confirmed destination/date and participant departure locations.
- [x] 4.4 **@tests** Add API and offline sync tests for transport plan creation/update/delete.
- [x] 4.5 **@tests** Add regression coverage for final Phase 4 review blockers: shared `generatePlan` must require an organizer actor, Android direct transport entry must load event/access state from local repositories, and organizer departure writes must reject unconfirmed or unknown participants before queuing sync.
- [x] 4.6 **@codegen** Fix final Phase 4 review blockers without changing the approved workflow: require an explicit organizer actor for local plan generation, back Android transport entry with persisted event/participant state, and align shared organizer departure guards with backend access rules.
- [x] 4.7 **@tests** Add Android regression coverage that transport direct entry ignores stale in-memory event/scenario/access state when it does not match the route `eventId`.
- [x] 4.8 **@codegen** Fix Android transport direct entry route scoping so transient event/scenario/access state is used only when it matches the route `eventId`.
- [x] 4.9 **@review** Re-review Phase 4 transport blockers and confirm whether the phase can be checked before Phase 5 starts.
- [x] 4.10 **@tests** Add regression coverage for the reopened Phase 4 blocker: transport conflict-resolution sync metadata must either be replayable or not remain/count as pending sync on shared, Android, and iOS repository-backed state.
- [x] 4.11 **@codegen** Fix reopened Phase 4 blocker without widening workflow/access: ensure transport pending sync indicators and replay only expose operations that can actually replay, or mark audit-only conflict resolution rows as synced/non-pending.
- [x] 4.11a **@tests** Add regression coverage that legacy pending transport `CONFLICT_RESOLVED` audit rows do not block organization readiness or finalization sync checks.
- [x] 4.11b **@codegen** Fix finalization readiness sync checks so non-replayable transport audit rows are excluded or cleared consistently with `TransportRepository.hasPendingTransportSync`.
- [x] 4.12 **@review** Final re-review of Phase 4 after transport pending-sync remediation; Phase 6 may resume only if this review explicitly approves transport.

Phase 4 reopened review blockers:
- Final read-only review after the three previous P1 corrections found one remaining P1: `TransportRepository.applyRemoteSelectedPlan` queues `CONFLICT_RESOLVED` through `queueSyncMetadata` as `synced = 0`, while `replayPendingSync` excludes `CONFLICT_RESOLVED`; shared `hasPendingTransportSync`, Android `WakeveNavHost`, and iOS `TransportPlanningViewModel` still count the row from `selectPending()`, so transport can show or block on pending sync that cannot replay.

Phase 4.10 RED evidence:
- `@tests` added reopened-blocker coverage in `shared/src/jvmTest/kotlin/com/guyghost/wakeve/transport/TransportOfflineRepositoryPhase4Test.kt`, `composeApp/src/androidUnitTest/kotlin/com/guyghost/wakeve/navigation/TransportNavigationContractTest.kt`, and `iosApp/WakeveTests/TransportPlanningContractTests.swift`.
- Shared expected RED: `remote selected plan conflict resolution is not counted as replayable pending transport sync` and the updated conflict replay assertion fail while `CONFLICT_RESOLVED` remains `synced=0` and is still counted by `TransportRepository.hasPendingTransportSync(eventId)` after `replayPendingSync`.
- Android expected RED: `navHostPendingTransportSyncExcludesAuditOnlyConflictResolutionRows` fails while `WakeveNavHost` derives `pendingTransportSync` from raw `selectPending()` without excluding `CONFLICT_RESOLVED` or delegating to `TransportRepository.hasPendingTransportSync`.
- iOS expected RED: `testTransportPlanningViewModelPendingSyncExcludesAuditOnlyConflictResolutionRows` fails while `TransportPlanningViewModel.hasReplayablePendingSync` derives pending sync from raw `selectPending()` without excluding `CONFLICT_RESOLVED` or delegating to the shared repository pending API.
- Targeted commands attempted on 2026-05-22: `./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.transport.TransportOfflineRepositoryPhase4Test' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process`, `./gradlew :composeApp:testDebugUnitTest --tests 'com.guyghost.wakeve.navigation.TransportNavigationContractTest' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process`, and `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/TransportPlanningContractTests -derivedDataPath /tmp/wakeve-ios-dd-transport-phase4-red CODE_SIGNING_ALLOWED=NO`. These runs were interrupted by concurrent Gradle/Xcode activity in the same checkout with `Gradle build daemon has been stopped: stop command received`; no production code was changed.
- Parent RED rerun on 2026-05-22: `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/TransportPlanningContractTests -derivedDataPath /tmp/wakeve-ios-dd-transport-phase4-red-parent CODE_SIGNING_ALLOWED=NO` fails only the reopened transport pending-sync contract `TransportPlanningContractTests.testTransportPlanningViewModelPendingSyncExcludesAuditOnlyConflictResolutionRows()`. Parent Gradle reruns for shared and Android still fail before assertions due local Gradle/Kotlin daemon/build artefact instability after prior concurrent runs, but the added tests are present and the iOS contract proves the intended RED.

Phase 4.11 GREEN evidence:
- `@codegen` fixed the reopened transport blocker by inserting `CONFLICT_RESOLVED` selected-plan metadata as synced audit rows, restricting `TransportRepository.hasPendingTransportSync(eventId)` to replayable `CREATE`/`UPDATE`/`DELETE` transport operations, marking any legacy non-replayable transport pending rows synced during replay, and delegating Android/iOS pending indicators to the shared repository/bridge.
- Shared GREEN: the first exact shared command requested failed before assertions due local Gradle/Kotlin generated-output instability (`NoSuchFileException`/missing generated SQLDelight or classpath snapshot artifacts). After clearing stale shared build outputs with `./gradlew :shared:clean :shared:jvmTest --tests 'com.guyghost.wakeve.transport.TransportOfflineRepositoryPhase4Test' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process`, the exact requested command `./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.transport.TransportOfflineRepositoryPhase4Test' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process` passed.
- Android GREEN: `./gradlew :composeApp:testDebugUnitTest --tests 'com.guyghost.wakeve.navigation.TransportNavigationContractTest' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process` passes.
- iOS GREEN: `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/TransportPlanningContractTests -derivedDataPath /tmp/wakeve-ios-dd-transport-phase4-green CODE_SIGNING_ALLOWED=NO` passes.
- OpenSpec GREEN: `openspec validate complete-event-organization-flow --strict` passes.

Phase 4.12 review blockers:
- `@review` worker `019e4f2f-e43a-7b01-b0da-c0aa1105b8ae` blocked final Phase 4 approval: `EventOrganizationReadinessRepository` still builds finalization sync readiness from raw `syncMetadataQueries.selectPending()` and counts critical transport entities without checking replayability, so a legacy `transport_plan_selection` / `CONFLICT_RESOLVED` / `synced=0` row can still become `CRITICAL_SYNC_PENDING` and block `ORGANIZING -> FINALIZED` unless `TransportRepository.replayPendingSync()` happens to run first.

Phase 4.11a RED evidence:
- `@tests` added finalization-readiness coverage in `shared/src/jvmTest/kotlin/com/guyghost/wakeve/organization/EventOrganizationPhase6FinalizationReadinessTest.kt` for an otherwise-ready `ORGANIZING` event with a legacy pending `transport_plan_selection` / `CONFLICT_RESOLVED` / `synced=0` audit row.
- Targeted command attempted on 2026-05-22: `./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.organization.EventOrganizationPhase6FinalizationReadinessTest' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process`.
- Expected RED observed: `5 tests completed, 1 failed`; the new test `Phase4 11a legacy transport conflict resolution audit row does not block finalization readiness` fails while readiness still reports `CRITICAL_SYNC_PENDING` for the non-replayable audit row. Gradle then also reported it could not write the XML test result file after the assertion failure.

Phase 4.11b GREEN evidence:
- `@codegen` confirmed finalization readiness now mirrors transport replayability rules locally: pending `transport_*` rows are critical only for replayable `CREATE`/`UPDATE`/`DELETE`/`UPSERT` operations, so legacy `transport_plan_selection` / `CONFLICT_RESOLVED` audit rows do not produce `CRITICAL_SYNC_PENDING`, while replayable transport writes remain blockers.
- Finalization readiness GREEN: `./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.organization.EventOrganizationPhase6FinalizationReadinessTest' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process` passes with `BUILD SUCCESSFUL`.
- Transport replayability GREEN: the first exact transport command failed before assertions due missing generated SQLDelight files under `shared/build/generated/sqldelight`; after `./gradlew :shared:clean :shared:jvmTest --tests 'com.guyghost.wakeve.transport.TransportOfflineRepositoryPhase4Test' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process` passed with `BUILD SUCCESSFUL`, the exact requested command `./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.transport.TransportOfflineRepositoryPhase4Test' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process` also passed with `BUILD SUCCESSFUL`. The exact rerun hit Kotlin daemon incremental-cache registration errors, fell back to non-daemon compilation, and completed successfully.
- OpenSpec GREEN: `openspec validate complete-event-organization-flow --strict` passes with `Change 'complete-event-organization-flow' is valid`.

Phase 4.12 final review approval:
- `@review` worker `019e4f43-3dbb-7481-9ca0-803e218cd9fd` found no remaining P1/P2 blocker on Phase 4 Transport after 4.10/4.11/4.11a/4.11b. The review explicitly approved checking Phase 4 and resuming Phase 6.
- Review confirmed actor/workflow guards in `TransportRepository`, replayable-only pending sync handling for transport, Android/iOS repository-backed transport state, finalization readiness exclusion of non-replayable legacy transport audit rows, and backend organizer-only transport mutations.
- Parent verification on 2026-05-22: `git diff --check` passes and `openspec validate complete-event-organization-flow --strict` passes.

## 5. Meetings, Budget, Payment, and Tricount
Phase 5 started after Phase 4 final review approved checking transport planning. `@tests` worker `019e4eb9-a9f7-7900-a287-a8591344e6c3` added RED coverage for tasks 5.1 and 5.5. `@tests` worker `019e4ec8-a513-7a63-92b3-46c1cf245bcc` added RED coverage for 5.2 meeting calendar/notification integration. `@tests` worker `019e4ec9-8a17-7cf1-9fad-4661f6c24194` added RED coverage for 5.3 payment pot backend routes.

Remediation gate: Phase 6 MUST NOT start until tasks 5.6 through 5.11 are complete and the final Phase 5 re-review explicitly approves checking this section.

- [x] 5.1 **@tests** Add shared tests for meeting readiness, budget baseline, expense splits, settlements, pot lifecycle, and Tricount handoff state.
- [x] 5.2 **@codegen** Connect meeting creation/link generation to calendar and notifications for confirmed participants.
- [x] 5.3 **@codegen** Complete budget/expense/payment pot/Tricount local-first repositories and backend routes.
- [x] 5.4 **@codegen** Wire Android/iOS organizing dashboards for meetings, budget, payment, and Tricount.
- [x] 5.5 **@tests** Add backend authorization tests for budget, payment, Tricount, and external link safety.
- [x] 5.6 **@tests** Add regression coverage for Phase 5 review blockers: generated meeting URLs must be real safe links, meeting/payment pot/Tricount writes must queue replayable sync metadata, shared meeting creation must reject non-organizer actors, iOS meeting creation must use the current user and hide create actions from non-organizers, payment/Tricount dashboards must expose lifecycle actions, and Tricount backend mutations must be `ORGANIZING`-guarded.
- [x] 5.7 **@codegen** Fix Phase 5 review blockers without widening the workflow: correct meeting URL interpolation, queue pending sync for meeting/payment pot/Tricount local writes, enforce organizer-only meeting creation in shared/backend/UI, replace placeholder iOS actor usage, wire payment pot and Tricount lifecycle actions on Android/iOS, and guard backend Tricount linking by event status.
- [x] 5.8 **@review** Re-review Phase 5 blockers and confirm whether the phase can be checked before Phase 6 starts.
- [x] 5.9 **@tests** Add backend regression coverage for Phase 5.8 meeting proxy blockers: Zoom creation must be organizer-only, Google Meet creation must be event-scoped and organizer-only, and Zoom response metadata must be internally consistent.
- [x] 5.10 **@codegen** Fix Phase 5.8 meeting proxy blockers without widening access: enforce organizer/event-status guards on provider creation, add event scope to Google Meet creation, and derive response safe-link metadata from one provider meeting id.
- [x] 5.11 **@review** Final re-review of Phase 5 after meeting proxy remediation; Phase 6 may start only if this review explicitly approves checking Phase 5.

Phase 5 RED evidence:
- `./gradlew :shared:jvmTest --tests '*Phase5*' --no-daemon` fails with `Phase5 meeting link is persisted as safe link metadata and not only a raw URL`, `Phase5 budget readiness accepts explicit baseline not needed decision`, `Phase5 offline expense writes include replayable sync payload and retry state`, `Phase5 payment pot lifecycle is persisted for shared settlement readiness`, and `Phase5 confirmed participant settlement visibility is scoped to own obligations`.
- `./gradlew :server:test --tests '*Phase5*' --no-daemon` fails with `Phase5 unconfirmed participant cannot read meeting details` and `Phase5 non participant cannot read budget details`.
- After the first Phase 5 GREEN pass, `./gradlew :shared:jvmTest --tests '*Phase5*' --no-daemon` fails with `Phase5 createMeeting schedules reminders only for confirmed participants`, `Phase5 createMeeting prepares calendar entries only for confirmed participants`, and `Phase5 createMeeting notifies confirmed participants with meeting id and excludes non confirmed users`.
- After the first Phase 5 GREEN pass, `./gradlew :server:test --tests '*Phase5*' --no-daemon` fails on missing payment pot routes: `Phase5 organizer can create active payment pot for organizing event`, `Phase5 organizer and confirmed participant can read payment pot while denied users are audited`, `Phase5 payment pot closure is organizer only and returns closed local first status`, and `Phase5 payment pot closure rejects mismatched event scope`.
- `@tests` worker `019e4eda-d79b-7872-a75d-1abda90bb523` added 5.6 shared/offline regression coverage in `EventOrganizationPhase56SharedOfflineRedTest.kt`; `./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.organization.EventOrganizationPhase56SharedOfflineRedTest' --no-daemon` fails on literal Zoom/Meet interpolation placeholders, non-organizer meeting create persistence, and missing replayable sync metadata for meeting, payment pot, and Tricount local writes.
- `@tests` worker `019e4eda-fc71-7152-bd78-227bb57ca313` added 5.6 backend regression coverage in `EventOrganizationPhase5RoutesTest.kt`; `./gradlew :server:test --tests '*Phase5*' --no-daemon` fails because Tricount link mutations are accepted outside `ORGANIZING` and literal template payment/Tricount links are not rejected.
- `@tests` worker `019e4edb-29c7-7702-aff2-857ff6323812` added 5.6 Android/iOS UI contract coverage in `Phase5OrganizationUiContractTest.kt` and `OrganizationPhase5ContractTests.swift`; Android `./gradlew :composeApp:testDebugUnitTest --tests '*Phase5*' --no-daemon --no-configuration-cache` fails on missing payment pot/Tricount lifecycle actions and safe-link abstraction, while iOS `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/OrganizationPhase5ContractTests -derivedDataPath /tmp/wakeve-ios-dd-phase5-ui-contract CODE_SIGNING_ALLOWED=NO` fails on current-user organizer gating and payment/Tricount lifecycle actions.
- `@tests` worker `019e4efa-c057-7093-a823-12f1c94f0c03` added 5.9 backend regression coverage in `EventOrganizationPhase5RoutesTest.kt`; `./gradlew :server:test --tests '*Phase5*' --no-daemon` fails because Zoom proxy creation accepts confirmed non-organizer participants, Google Meet creation accepts missing `eventId` and confirmed non-organizers, and Zoom `meetingId` differs from the id embedded in `joinUrl`/`hostUrl`.

Phase 5 GREEN evidence:
- `@codegen` worker `019e4ecd-fced-7e50-a804-1f234e9c4f5b` connected `MeetingService.createMeeting` to confirmed-participant reminders, calendar entries, and notifications carrying `meetingId`; parent `./gradlew :shared:jvmTest --tests '*Phase5*' --no-daemon` passes.
- `@codegen` worker `019e4ece-7cd2-7171-ab1b-c99d1dce2120` added backend payment pot create/read/close routes with organizer mutation guards, confirmed-participant read access, event-scope checks, and audit denials; parent `./gradlew :server:test --tests '*Phase5*' --no-daemon` passes.
- Android Phase 5 organizing routes and access/offline UI are covered by parent `./gradlew :composeApp:testDebugUnitTest --tests com.guyghost.wakeve.navigation.Phase5OrganizationUiContractTest --no-daemon --no-configuration-cache`, which passes.
- iOS Phase 5 organizing routes and access/offline UI are covered by parent `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/OrganizationPhase5ContractTests -derivedDataPath /tmp/wakeve-ios-dd-phase5-ui-parent CODE_SIGNING_ALLOWED=NO`, which passes.
- `@codegen` worker `019e4ee3-4ff5-7ed3-ba7b-5b0c5cfad4da` fixed shared/backend Phase 5.7 blockers for safe meeting links, organizer-only meeting creation, replayable meeting/payment/Tricount sync, and Tricount `ORGANIZING` guards; parent `./gradlew :shared:jvmTest --tests '*Phase5*' --no-daemon`, `./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.organization.EventOrganizationPhase56SharedOfflineRedTest' --no-daemon`, and `./gradlew :server:test --tests '*Phase5*' --no-daemon` pass.
- `@codegen` worker `019e4ee3-84e9-7d11-811c-d46137b65b6f` fixed Android/iOS Phase 5.7 UI blockers for current-user organizer gating, payment pot lifecycle actions, Tricount lifecycle actions, and safe external link opening; parent `./gradlew :composeApp:testDebugUnitTest --tests '*Phase5*' --no-daemon --no-configuration-cache` and `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/OrganizationPhase5ContractTests -derivedDataPath /tmp/wakeve-ios-dd-phase5-ui-parent-after-57 CODE_SIGNING_ALLOWED=NO` pass.
- Parent re-validation after final Phase 5.7 remediation: `./gradlew :shared:jvmTest --tests '*Phase5*' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1`, `./gradlew :server:test --tests '*Phase5*' --no-daemon --no-configuration-cache --no-build-cache`, `./gradlew :composeApp:testDebugUnitTest --tests com.guyghost.wakeve.navigation.Phase5OrganizationUiContractTest --no-daemon --no-configuration-cache --no-build-cache`, `./gradlew :composeApp:compileDebugKotlinAndroid --no-daemon --no-configuration-cache`, `./gradlew :server:test --no-daemon --no-configuration-cache`, `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,id=80255C41-42B5-4704-83F0-1F4752E9D0B3' -only-testing:WakeveTests/OrganizationPhase5ContractTests CODE_SIGNING_ALLOWED=NO`, and `git diff --check && openspec validate complete-event-organization-flow --strict` pass.
- `@review` worker `019e4ed3-df01-7211-9f66-33e1ff6da1c0` re-reviewed Phase 5 blockers read-only and found no remaining blocker for the five remediation points; conclusion: Phase 5 can be checked.
- `@codegen` worker `019e4efe-d0ba-76d1-b4b7-e544e760a8c9` fixed Phase 5.8 backend meeting proxy blockers by making Zoom/Google Meet provider creation organizer-only and `ORGANIZING`-guarded, requiring Google Meet `eventId`, and deriving Zoom `joinUrl`/`hostUrl` from the response `meetingId`; parent `./gradlew :server:test --tests '*Phase5*' --no-daemon --no-configuration-cache --no-build-cache --rerun-tasks` passes.
- `@review` worker `019e4f02-cb01-7042-aa68-7db4e7b609dc` completed final Phase 5.11 read-only review after meeting proxy remediation with no P1/P2 blockers; it noted only a non-blocking P3 branch coverage gap and explicitly approved checking Phase 5 and starting Phase 6.

Phase 5 review blockers:
- `@review` worker `019e4ed3-df01-7211-9f66-33e1ff6da1c0` blocked Phase 5: generated Zoom/Meet links still contain escaped interpolation placeholders, meeting/payment pot/Tricount writes do not all queue replayable sync metadata, shared/iOS meeting creation does not enforce organizer actor correctly, payment/Tricount dashboards are still mostly placeholders instead of lifecycle UIs, and backend Tricount linking is not `ORGANIZING`-guarded.
- `@review` worker `019e4ef5-d3fc-7522-a3e5-891c3360477b` blocked Phase 5 after 5.7: backend Zoom proxy creation still allows confirmed non-organizer participants via `hasMeetingProxyEventAccess`, backend Google Meet proxy creation is not event-scoped or actor-checked, and Zoom proxy safe-link metadata can be inconsistent because `meetingId`, `joinUrl`, and `hostUrl` are generated from separate random ids.

## 6. Finalization and Offline Sync Integrity
- [x] 6.1 **@tests** Add state-machine tests for finalization readiness blockers and successful `ORGANIZING -> FINALIZED`.
- [x] 6.2 **@codegen** Implement computed readiness checks and block finalization while critical logistics are missing or pending sync.
- [x] 6.3 **@tests** Add end-to-end shared workflow test covering creation, invites, votes, confirmation, scenarios, logistics, budget/payment, sync, and finalization.
- [x] 6.4 **@tests** Add conflict/retry tests for offline operations across all critical sections.

Phase 6 RED evidence:
- `@tests` worker `019e4ef9-0e76-7183-a11e-66ec21a250a1` added finalization readiness and E2E sync coverage in `EventOrganizationPhase6FinalizationReadinessTest.kt` and `EventOrganizationPhase6EndToEndSyncTest.kt`.
- `./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.organization.EventOrganizationPhase6FinalizationReadinessTest' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process` fails because readiness does not expose all critical finalization sections and repository finalization still succeeds with pending critical sync/blockers.
- `./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.workflow.EventOrganizationPhase6EndToEndSyncTest' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process` fails because the E2E workflow finalizes before offline sync convergence, failed critical retries/conflicts do not block finalization, and finalized events still accept at least one organization mutation.

Phase 6 GREEN evidence:
- Current production includes computed finalization readiness for participants, scenarios, destination, lodging, transport, meetings, calendar, notifications, budget, payment, Tricount, sync, unsafe external links, and access control; repository finalization gates `ORGANIZING -> FINALIZED` through that readiness view and preserves read-only organization sections after finalization.
- Parent verification after Phase 6 codegen/remediation: `./gradlew :shared:clean :shared:jvmTest --tests '*Phase6*' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process` passes. The first attempt hit a stale generated-output/cache issue (`NoSuchFileException` under `shared/build/classes/kotlin`); rerunning with `:shared:clean` produced `BUILD SUCCESSFUL`.

## 7. Cross-Platform UX
- [x] 7.1 **@codegen** Align Android Compose and iOS SwiftUI navigation, empty states, offline indicators, pending states, and access-denied states across all organization phases.
- [x] 7.2 **@tests** Add targeted Android/iOS UI tests for primary organizer and confirmed participant paths.
- [x] 7.3 **@designer** Validate Material You and Liquid Glass consistency from screenshots where UI changes are made.
- [x] 7.4 **@review** Review accessibility, text fit, localization keys, and platform interaction patterns.
- [x] 7.5 **@tests** Add UX copy regression coverage that technical contract markers are not visible to users in finalized, access-denied, and pending-sync states.
- [x] 7.6 **@codegen** Replace visible technical UX markers with product-ready Android/iOS copy while preserving stable non-displayed contract anchors.
- [x] 7.7 **@tests** Add regression coverage for Phase 7.4 blockers: iOS meeting cancellation must be organizer/current-actor gated, Android direct organization routes must reject pre-organizing statuses, Android/iOS finalized organization routes must hide or disable meeting/payment/Tricount mutations, iOS finalized events must keep transport details reachable read-only, and Android meeting visible copy must be product-ready/localized.
- [x] 7.8 **@codegen** Fix Phase 7.4 blockers without widening access: gate iOS destructive meeting actions by current organizer, add status guards to direct Android organization routes, pass workflow/read-only state into Android/iOS Phase 5 organization screens, preserve finalized read-only transport navigation on iOS, and replace remaining Android meeting English action/empty/dialog copy.
- [x] 7.9 **@review** Re-review Phase 7 UX/accessibility/localization after 7.7/7.8 and confirm whether 7.4 can be checked.
- [x] 7.10 **@tests** Add regression coverage for remaining Phase 7.9 blockers: Android budget detail/empty-state workflow and read-only guards, iOS payment/Tricount pre-`ORGANIZING` workflow gating, and Android access-denied French copy.
- [x] 7.11 **@codegen** Fix remaining Phase 7.9 blockers without widening access: apply Phase 5 access/read-only policy to Android budget detail and empty states, gate iOS payment/Tricount direct access before `ORGANIZING`, and localize Android access-denied organization messages.
- [x] 7.12 **@review** Final re-review of Phase 7.4 blockers after 7.10/7.11; Phase 7.4 may be checked only if this review explicitly approves.
- [x] 7.13 **@tests** Add regression coverage for the remaining Phase 7.12 P1: iOS meeting detail cancellation/generate-link actions and shared cancel meeting use case must require the current organizer actor and reject participants/read-only states.
- [x] 7.14 **@codegen** Fix the remaining Phase 7.12 P1 without widening access: pass current actor/read-only state into iOS meeting detail, hide or disable meeting detail mutations for participants and finalized events, and enforce actor comparison in shared meeting cancellation.
- [x] 7.15 **@review** Re-review Phase 7 after 7.13/7.14; Phase 7.4 and 7.12 may be checked only if this review explicitly approves.
- [x] 7.16 **@tests** Add regression coverage for remaining Phase 7.15 blockers: real iOS meeting details must not use DEBUG-only preview initializers, and meeting detail loading must receive the event id.
- [x] 7.17 **@codegen** Fix remaining Phase 7.15 blockers without widening access: use a production-safe meeting detail initializer for real meetings and pass event id into the meeting detail view model/load path.
- [x] 7.18 **@review** Final Phase 7 re-review after 7.16/7.17; Phase 7.4, 7.12, and 7.15 may be checked only if this review explicitly approves.
- [x] 7.19 **@tests** Add Android regression coverage for remaining Phase 7.18 localization blocker: meeting UI must not render enum-backed `platform.name`, `meeting.platform.name`, or English `month.name` fragments in visible copy.
- [x] 7.20 **@codegen** Fix remaining Phase 7.18 localization blocker without widening access: replace Android meeting enum labels and month abbreviations with product-ready French display strings.
- [x] 7.21 **@review** Final Phase 7 re-review after 7.19/7.20; Phase 7.4, 7.12, 7.15, and 7.18 may be checked only if this review explicitly approves.

Phase 7 orchestration:
- Prepared `delegations/phase-7-tests.md` for `@tests` to add RED cross-platform UX contract coverage before `@codegen` starts UI alignment, preserving the project TDD rule even though the checklist lists 7.1 before 7.2.

Phase 7.2 RED evidence:
- Android contract coverage exists in `composeApp/src/androidUnitTest/kotlin/com/guyghost/wakeve/navigation/Phase7OrganizationUxContractTest.kt` for finalization readiness sections, access/readiness/sync/read-only state distinction, finalized read-only UX, pre-confirmed/unauthorized detail hiding, confirmed/denied access states, offline pending/failed sync language, and stable readiness labels.
- iOS contract coverage exists in `iosApp/WakeveTests/OrganizationPhase7ContractTests.swift` for the same cross-platform UX concerns.
- Android RED on 2026-05-22: `./gradlew :composeApp:testDebugUnitTest --tests '*Phase7*' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1` fails with `Phase7OrganizationUxContractTest > finalizedOrganizationUxKeepsDetailsButDisablesMutationActions`, because finalized organization sections are not yet rendered explicitly read-only.
- iOS RED on 2026-05-22: `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/OrganizationPhase7ContractTests -derivedDataPath /tmp/wakeve-ios-dd-phase7-ui-contract CODE_SIGNING_ALLOWED=NO` fails `testFinalizedOrganizationUxKeepsDetailsButDisablesMutationActions`, `testOfflinePendingAndFailedSyncStatesDoNotReadAsServerConfirmed`, `testOrganizationStatesAreVisiblyDistinctAcrossAccessSyncAndReadiness`, and `testReadinessSectionsAndStateBadgesUseStableLocalizationKeys`.
- No production behavior was implemented for 7.2; Phase 7.1 must make these Android/iOS UX contracts green without weakening access or finalization guards.

Phase 7.1 GREEN evidence:
- `@codegen` worker `019e4f50-3132-7240-af58-cd7c2e2aa3a5` aligned Android/iOS organization UX by gating pre-confirmed and unauthorized detail access, making `FINALIZED` organization controls read-only/view-only, deriving pending-sync state from local repositories where needed, adding clear pending/failed sync copy, and keeping stable cross-platform organization section/state labels in source.
- `@codegen` follow-up removed technical `readOnly`/`viewOnly`/`mutationsDisabled` markers from visible user copy while preserving non-displayed source anchors for the contracts.
- Android GREEN after clearing stale Gradle outputs: `rm -rf composeApp/build/tmp/kotlin-classes composeApp/build/classes composeApp/build/kotlin && ./gradlew :composeApp:testDebugUnitTest --tests '*Phase7*' --no-daemon --no-configuration-cache --no-build-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process -Dkotlin.incremental=false` passes with `BUILD SUCCESSFUL`.
- iOS GREEN: `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/OrganizationPhase7ContractTests -derivedDataPath /tmp/wakeve-ios-dd-phase7-ui-parent-2 CODE_SIGNING_ALLOWED=NO` passes with `** TEST SUCCEEDED **` and all 7 `OrganizationPhase7ContractTests` cases passing.
- OpenSpec GREEN: `git diff --check` passes and `openspec validate complete-event-organization-flow --strict` passes.

Phase 7.3 design blockers:
- `@designer` worker `019e4f71-f52f-75f1-9670-0d65c9a95d78` blocked Phase 7 design approval in read-only source/design-system review because visible user copy still exposes technical markers: `readOnly/viewOnly` in Android event details, `AccessDenied` titles in Android/iOS access-denied states, `pendingSync`/`queued`/`not server confirmed` phrasing in pending-sync banners, and mostly English visible transport copy on iOS.
- No screenshots were available in the repo, so the design validation was source/design-system only; Material You and Liquid Glass usage were otherwise not blocked.

Phase 7.5 RED evidence:
- `@tests` added visible-copy regression coverage in `composeApp/src/androidUnitTest/kotlin/com/guyghost/wakeve/navigation/Phase7OrganizationUxContractTest.kt` and `iosApp/WakeveTests/OrganizationPhase7ContractTests.swift`. The new checks inspect likely visible UI string literals near `Text`/`Button`/`Label`/`Section`-style UI calls and allow non-displayed contract anchors/constants to remain in source.
- Android RED on 2026-05-22: `./gradlew :composeApp:testDebugUnitTest --tests '*Phase7*' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1` compiles and fails with `Phase7OrganizationUxContractTest > visiblePendingSyncBannersUseLocalizedUserCopy` and `visibleOrganizationCopyDoesNotExposeTechnicalContractMarkers`. A later exact rerun after concurrent build-output churn failed earlier in Gradle resource/Kotlin cache handling (`NoSuchFileException` under `composeApp/build/intermediates`/`composeApp/build/kotlin`) and did not supersede the assertion RED already captured in `composeApp/build/test-results/testDebugUnitTest/TEST-com.guyghost.wakeve.navigation.Phase7OrganizationUxContractTest.xml`.
- Android failing assertions include visible strings such as `Offline pending sync - queued for sync, not server confirmed`, `Réunions queued - pendingSync, pending server confirmation`, `Offline - modifications queued, pending sync, not server confirmed`, `Modifications queued - pendingSync, pending server confirmation`, and finalized detail copy containing `readOnly/viewOnly`.
- iOS verification on 2026-05-22: `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/OrganizationPhase7ContractTests -derivedDataPath /tmp/wakeve-ios-dd-phase7-copy-red CODE_SIGNING_ALLOWED=NO` builds the app/tests after the test-only helper compile fix, then fails to launch XCTest in CoreSimulator with `NSMachErrorDomain Code=-308 "(ipc/mig) server died"` / `** BUILD INTERRUPTED **`; no iOS assertion result was produced by that infrastructure failure.
- Validation after adding 7.5 coverage: `git diff --check` passes and `openspec validate complete-event-organization-flow --strict` passes.

Phase 7.6 GREEN evidence:
- `@codegen` worker `019e4f83-8665-7943-bab0-8bf6b5432d1c` replaced visible technical copy in Android/iOS finalized details, access-denied states, pending-sync banners, and iOS transport planning primary states while preserving non-displayed stable contract anchors.
- Android GREEN: `./gradlew :composeApp:testDebugUnitTest --tests '*Phase7*' --no-daemon --no-configuration-cache --no-build-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process -Dkotlin.incremental=false` passes with `BUILD SUCCESSFUL`.
- iOS GREEN: `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/OrganizationPhase7ContractTests -derivedDataPath /tmp/wakeve-ios-dd-phase7-copy-parent CODE_SIGNING_ALLOWED=NO` passes with `** TEST SUCCEEDED **` and all 10 `OrganizationPhase7ContractTests` cases passing.
- OpenSpec GREEN: `git diff --check` passes and `openspec validate complete-event-organization-flow --strict` passes.

Phase 7.3 design approval:
- `@designer` worker `019e4f92-57b2-7352-ae67-a932255df65c` re-reviewed Phase 7 after 7.5/7.6 and concluded `Phase 7 design approuvée` with no P1/P2 blockers.
- The re-review confirmed that visible technical copy markers were removed, access-denied titles are product-ready, pending-sync banners use clear French copy, iOS transport primary states are localized, and Android/iOS still follow Material 3/MaterialTheme and Wakeve Liquid Glass components respectively.
- Limitation: no screenshots were available in the repo, so design validation was source/design-system only. Non-blocking P3s remain for some English copy outside the Phase 7 blocker scope and an Android transport `Event $eventId` detail.

Phase 7.4 review blockers:
- `@review` worker `019e4f96-7dbc-7291-83ce-5f268ee3cb2a` blocked Phase 7.4 in read-only static review. Decision: `Phase 7.4 cannot be checked`.
- P1: Android finalized events still expose organization mutations because meeting/payment/Tricount routes pass organizer state directly into mutable screens without workflow/read-only state.
- P1: iOS payment and Tricount routes have the same finalized mutation gap; finalized organizers can still create/activate/close cagnotte and link/unlink/mark Tricount not needed.
- P2: iOS finalized organization hides transport instead of keeping details consultable read-only.
- P2: Android meeting visible copy still contains English labels/actions/dialog text in a French product flow.
- Review limitation: static source review only, no screenshots/runtime, and no tests run by `@review`.
- Additional `@review` worker `019e4f94-5237-7f70-92af-bd34de90e669` also blocked Phase 7.4: iOS meeting cancellation swipe is visible without organizer/current-actor gating, Phase 5 logistics screens are not consistently read-only for `FINALIZED`, and direct Android organization routes do not explicitly reject DRAFT/POLLING/pre-organizing statuses.

Phase 7.7 RED evidence:
- `@tests` worker `019e4f9a-3f3a-76f3-85e6-b48b1bab7d9d` added regression coverage in `composeApp/src/androidUnitTest/kotlin/com/guyghost/wakeve/navigation/Phase7OrganizationUxContractTest.kt` and `iosApp/WakeveTests/OrganizationPhase7ContractTests.swift`; no production files were modified.
- Android RED command: `./gradlew :composeApp:testDebugUnitTest --tests 'com.guyghost.wakeve.navigation.Phase7OrganizationUxContractTest' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process -Dkotlin.incremental=false`.
- Android RED assertions: `finalizedAndroidOrganizationRoutesPassWorkflowReadOnlyStateToMutablePhase5Screens`, `finalizedAndroidMeetingPaymentAndTricountSurfacesExposeReadOnlyMutationGuards`, `androidMeetingVisibleCopyIsProductReadyAndLocalizedForFrenchUsers`, and existing `directAndroidPhase5OrganizationRoutesRejectPreOrganizingStatuses`.
- iOS XCTest compiled but CoreSimulator failed before assertions twice with `NSMachErrorDomain Code=-308`; a source-contract mirror confirmed the intended RED conditions: payment/Tricount routes lack finalized/read-only state, payment/Tricount mutation buttons are disabled only by organizer state, and `canAccessTransportPlanning` excludes `.finalized`.
- Verification: `git diff --check` for the two test files passes and `openspec validate complete-event-organization-flow --strict` passes.

Phase 7.8 GREEN evidence:
- `@codegen` worker `019e4fa8-dd55-7ba2-ac6d-72be4cb4d136` fixed production files only: Android `WakeveNavHost.kt`, `MeetingListScreen.kt`, `BudgetOverviewScreen.kt`; iOS `ContentView.swift` and `MeetingListView.swift`.
- Android organization routes are now limited to `ORGANIZING`/`FINALIZED`; `FINALIZED` passes read-only state into meeting/payment/Tricount surfaces and mutation capability is no longer derived from organizer state alone.
- Android meeting visible copy was francized for the remaining Phase 7.4 strings.
- iOS payment and Tricount surfaces receive read-only state; finalized transport remains reachable read-only; meeting cancellation is organizer/current-actor/read-only gated.
- Parent Android verification: first `./gradlew :composeApp:testDebugUnitTest --tests '*Phase7*' --no-daemon --no-configuration-cache --no-build-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process -Dkotlin.incremental=false` failed after compilation on a generated `composeApp/build/tmp/kotlin-classes` copy artifact, not on assertions. After removing generated ComposeApp build outputs, the same command passed with `BUILD SUCCESSFUL`.
- Parent iOS verification: `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/OrganizationPhase7ContractTests -derivedDataPath /tmp/wakeve-ios-dd-phase7-remediation-parent CODE_SIGNING_ALLOWED=NO` passes with `** TEST SUCCEEDED **` and 13/13 `OrganizationPhase7ContractTests` passing.
- OpenSpec/hygiene: `git diff --check` passes and `openspec validate complete-event-organization-flow --strict` passes.

Phase 7.9 review blockers:
- `@review` worker `019e4fb1-b595-75b2-8714-a7162e66b0ba` found the 7.8 fixes present but blocked Phase 7.4. Decision: `Phase 7.4 cannot be checked`.
- P1: Android budget screens still bypass the Phase 5 workflow/read-only policy. `BudgetDetail` direct entry checks only organization-detail access instead of `ORGANIZING`/`FINALIZED`, receives no read-only state, and exposes add/delete/pay mutations; budget overview empty state can still call `createBudget` in read-only/finalized or confirmed-participant contexts.
- P1: iOS direct payment/Tricount access lacks workflow-status gating before `ORGANIZING`; pre-organizing organizers can still reach and mutate these screens because `isReadOnly` is true only in `FINALIZED`.
- P2: Android organization access-denied messages for meeting/payment/Tricount/budget/expense remain visible English copy: `Confirm your attendance...`.
- Review limitation: static read-only review only, no screenshots/runtime, and no tests run by `@review`.

Phase 7.10 RED evidence:
- `@tests` worker `019e4fb5-27df-7431-ac56-a4e24bd74d40` added regression tests only in `composeApp/src/androidUnitTest/kotlin/com/guyghost/wakeve/navigation/Phase7OrganizationUxContractTest.kt` and `iosApp/WakeveTests/OrganizationPhase7ContractTests.swift`; no production files were modified.
- Android RED command: `./gradlew :composeApp:testDebugUnitTest --tests 'com.guyghost.wakeve.navigation.Phase7OrganizationUxContractTest' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process -Dkotlin.incremental=false`.
- Android RED result: compile OK, 16 tests executed, expected failures in `androidPhase5AccessDeniedCopyIsFrenchAndProductReady`, `finalizedAndroidBudgetSurfacesHideOrDisableBudgetMutations`, and `androidBudgetDetailDirectRouteRequiresPhase5WorkflowAndReadOnlyState`.
- iOS RED command: `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/OrganizationPhase7ContractTests -derivedDataPath /tmp/wakeve-ios-dd-phase7-710-red CODE_SIGNING_ALLOWED=NO`.
- iOS RED result: simulator launched and assertions were reached; expected failures in `testIOSPaymentAndTricountMutationGuardsRequireOrganizingAndFinalizedReadOnly` and `testIOSPaymentAndTricountRoutesRequirePhase5WorkflowStatus`.
- Verification: `openspec validate complete-event-organization-flow --strict` passes; whitespace check on the two test files reports no issues.

Phase 7.11 GREEN evidence:
- Android `@codegen` worker `019e4fb8-12a0-73a3-b117-2a35ec765ab7` fixed `WakeveNavHost.kt`, `BudgetDetailScreen.kt`, and `BudgetOverviewScreen.kt`: `BudgetDetail` direct entry now uses Phase 5 `canEnterOrganizationRoutes`, `BudgetDetailScreen` receives explicit `isReadOnly`/budget mutation capability, finalized/read-only states hide budget mutations, the budget empty state gates creation, and Phase 5 access-denied messages are French product copy.
- iOS `@codegen` worker `019e4fb8-af4d-7262-8a77-8edbeac3fd2e` fixed `ContentView.swift`: `.paymentPot` and `.tricount` now require `ORGANIZING` or `FINALIZED`, mutation capability is derived from `event.status == .organizing && event.organizerId == userId`, and finalized events remain read-only.
- Parent Android verification: `./gradlew :composeApp:testDebugUnitTest --tests '*Phase7*' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process -Dkotlin.incremental=false` passes with `BUILD SUCCESSFUL`.
- Parent iOS verification: `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/OrganizationPhase7ContractTests -derivedDataPath /tmp/wakeve-ios-dd-phase7-final-parent CODE_SIGNING_ALLOWED=NO` passes with `** TEST SUCCEEDED **` and 15/15 `OrganizationPhase7ContractTests` passing.
- OpenSpec/hygiene: `git diff --check` passes and `openspec validate complete-event-organization-flow --strict` passes.

Phase 7.12 review blockers:
- `@review` worker `019e4fc0-45c8-7db3-acc0-9cedfa1c105b` blocked Phase 7.12. Decision: `Phase 7 UX bloquée`.
- P1: iOS meeting detail still exposes `Generate link` and `Cancel meeting` actions without organizer/current-user/read-only gating, and the shared cancellation path does not compare the current actor with the meeting organizer. `MeetingDetailView` calls `viewModel.cancelMeeting()` directly; `MeetingListViewModel.cancelMeeting` only verifies `currentUserId` is non-empty; `MeetingServiceStateMachine` reconstructs the organizer from the meeting; `CancelMeetingUseCase` does not reject non-organizer actors.

Phase 7.13 RED evidence:
- `@tests` added regression coverage in `iosApp/WakeveTests/OrganizationPhase7ContractTests.swift` and `shared/src/jvmTest/kotlin/com/guyghost/wakeve/presentation/usecase/CancelMeetingUseCaseTest.kt`; no production files were modified.
- Shared RED command: `./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.presentation.usecase.CancelMeetingUseCaseTest' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process -Dkotlin.incremental=false`.
- Shared RED result: 4 tests executed, expected failure in `invoke rejects organizer cancellation when event is finalized read only`; `CancelMeetingUseCase` still allows the real organizer to cancel a meeting after the event is switched to `FINALIZED`, instead of returning a read-only/workflow failure and leaving the meeting scheduled.
- iOS RED command: `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/OrganizationPhase7ContractTests -derivedDataPath /tmp/wakeve-ios-dd-phase7-713-red CODE_SIGNING_ALLOWED=NO`.
- iOS RED result: XCTest reached assertions and failed `OrganizationPhase7ContractTests.testMeetingDetailMutationsAreCurrentActorAndReadOnlyGated`; `MeetingDetailView` still calls `viewModel.generateMeetingLink(platform: platform)` without passing current actor, organizer, and read-only authorization context.
- Validation after adding RED coverage: `git diff --check -- iosApp/WakeveTests/OrganizationPhase7ContractTests.swift shared/src/jvmTest/kotlin/com/guyghost/wakeve/presentation/usecase/CancelMeetingUseCaseTest.kt` passes and `openspec validate complete-event-organization-flow --strict` passes.

Phase 7.14 GREEN evidence:
- `@codegen` worker `019e4fc8-4fcd-77b3-a6b8-26c387fc874f` fixed the remaining meeting P1 by carrying current actor/read-only state through iOS meeting detail/list flows, hiding or disabling meeting detail mutations for participants and finalized/read-only states, carrying `currentUserId` through shared `CancelMeeting`, and enforcing organizer comparison in shared cancellation.
- Parent shared verification: `./gradlew :shared:jvmTest --tests 'com.guyghost.wakeve.presentation.usecase.CancelMeetingUseCaseTest' --tests 'com.guyghost.wakeve.presentation.statemachine.MeetingServiceStateMachineTest' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process` passes with `BUILD SUCCESSFUL`.
- Parent iOS verification: `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/OrganizationPhase7ContractTests -derivedDataPath /tmp/wakeve-ios-dd-phase7-meeting-detail-parent CODE_SIGNING_ALLOWED=NO` passes with `** TEST SUCCEEDED **` and 17/17 `OrganizationPhase7ContractTests` passing.
- Parent Android verification: `./gradlew :composeApp:testDebugUnitTest --tests '*Phase7*' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process -Dkotlin.incremental=false` passes with `BUILD SUCCESSFUL`.
- OpenSpec/hygiene: `git diff --check` passes and `openspec validate complete-event-organization-flow --strict` passes.

Phase 7.15 review blockers:
- `@review` worker `019e4fdb-7a99-7063-859d-0a7e355381a9` blocked Phase 7.15. Decision: `Phase 7 UX bloquée`.
- P1: `MeetingListView` calls the `MeetingDetailView(... previewMeeting:)` initializer for real meetings, but that initializer is DEBUG-only; non-DEBUG iOS builds would fail, and DEBUG builds treat real meetings as previews and block mutation paths through preview guards.
- P2: `MeetingDetailViewModel` still loads meetings with `LoadMeetings(eventId: "")` and does not receive the event id, so direct detail navigation can show `Réunion introuvable` instead of the real meeting.

Phase 7.16 RED evidence:
- `@tests` worker `019e4fe0-154c-7172-84c7-475c343ee094` added iOS contract coverage in `iosApp/WakeveTests/OrganizationPhase7ContractTests.swift`; no production files were modified.
- Added tests: `testMeetingListDoesNotUseDebugPreviewMeetingInitializerForRealDetailNavigation`, `testMeetingDetailRealPathPropagatesEventIdIntoViewModel`, and `testMeetingDetailViewModelStoresEventIdAndNeverLoadsAllMeetingsWithEmptyEventId`.
- RED command: `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/OrganizationPhase7ContractTests -derivedDataPath /tmp/wakeve-ios-dd-phase7-716-red CODE_SIGNING_ALLOWED=NO`.
- RED result: exit `65`, `** TEST FAILED **`; the three new Phase 7.16 tests fail while real meeting navigation still uses the DEBUG-only preview initializer and `MeetingDetailViewModel` still dispatches `LoadMeetings(eventId: "")`.
- Verification: `git diff --check -- iosApp/WakeveTests/OrganizationPhase7ContractTests.swift` passes and `openspec validate complete-event-organization-flow --strict` passes.

Phase 7.17 GREEN evidence:
- `@codegen` worker `019e4fe3-5fb0-7dc3-a825-c54738e8e5b1` confirmed the iOS real meeting detail fix in the shared worktree: real `MeetingListView` navigation no longer passes `previewMeeting:`, `MeetingDetailView` passes `eventId` into `MeetingDetailViewModel`, and `MeetingDetailViewModel` stores/uses `eventId` for `LoadMeetings(eventId: eventId)`.
- Parent iOS verification: `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/OrganizationPhase7ContractTests -derivedDataPath /tmp/wakeve-ios-dd-phase7-717-parent CODE_SIGNING_ALLOWED=NO` passes with `** TEST SUCCEEDED **` and 20/20 `OrganizationPhase7ContractTests` passing.
- OpenSpec/hygiene: `git diff --check` passes and `openspec validate complete-event-organization-flow --strict` passes.

Phase 7.18 review blockers:
- `@review` worker `019e4fe7-e28c-7b03-8eb6-b40e31c5f5d1` blocked final Phase 7 approval. Decision: `Phase 7.4`, `7.12`, `7.15`, and `7.18` cannot be checked yet.
- P1: Android meeting UI still renders non-localized dynamic enum labels in visible copy: `meeting.platform.name`, `localDateTime.month.name.take(3)`, and `platform.name` in `MeetingListScreen.kt`. These can expose values like `GOOGLE_MEET` and English month fragments to users.
- The review confirmed the iOS Meeting Detail 7.15 fixes and existing actor/read-only guards remain intact.

Phase 7.19 RED evidence:
- `@tests` worker `019e4feb-ca03-7d62-bab2-b4f16253634d` added Android contract coverage in `composeApp/src/androidUnitTest/kotlin/com/guyghost/wakeve/navigation/Phase7OrganizationUxContractTest.kt`; no production files were modified.
- Added tests: `androidMeetingCardsUseDisplayLabelsInsteadOfPlatformEnumNames`, `androidMeetingGenerateLinkDialogUsesDisplayLabelsInsteadOfPlatformEnumNames`, and `androidMeetingDateFormattingDoesNotExposeEnglishEnumMonthNames`.
- RED command: `./gradlew :composeApp:testDebugUnitTest --tests 'com.guyghost.wakeve.navigation.Phase7OrganizationUxContractTest' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process -Dkotlin.incremental=false`.
- RED result: `BUILD FAILED`; `22 tests completed, 6 failed`, including the three new Phase 7.19 tests and existing adjacent Android meeting localization contracts for raw enum names in visible text/detail.
- Verification: `git diff --check -- composeApp/src/androidUnitTest/kotlin/com/guyghost/wakeve/navigation/Phase7OrganizationUxContractTest.kt` passes and `openspec validate complete-event-organization-flow --strict` passes.

Phase 7.20 GREEN evidence:
- `@codegen` worker `019e4fef-822b-7640-ae21-9f8723da49f2` fixed Android meeting visible localization in production only: `MeetingListScreen.kt` and `MeetingDetailScreen.kt` now use product display labels for platforms and French month labels for visible date formatting.
- Parent Android verification: first targeted run was interrupted before assertions by `Gradle build daemon has been stopped: stop command received`; rerun with `--no-build-cache` passed: `./gradlew :composeApp:testDebugUnitTest --tests 'com.guyghost.wakeve.navigation.Phase7OrganizationUxContractTest' --no-daemon --no-configuration-cache --no-build-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process -Dkotlin.incremental=false` passes with `BUILD SUCCESSFUL`.
- Source verification: `rg` confirms `meeting.platform.name`, visible `platform.name`, `month.name`, and `.name.take(3)` are no longer present in Android meeting list/detail UI; `git diff --check -- composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/meeting/MeetingListScreen.kt composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/meeting/MeetingDetailScreen.kt composeApp/src/androidUnitTest/kotlin/com/guyghost/wakeve/navigation/Phase7OrganizationUxContractTest.kt` passes.

Phase 7.21 final review approval:
- `@review` worker `019e500f-d88c-7982-8622-4b002ac45119` concluded `Phase 7.21 peut être approuvée` with no remaining P1/P2 on the 7.19/7.20 Android meeting localization blocker.
- Review confirmed visible labels no longer render raw platform enum names or English enum month fragments in Android meeting list/detail UI, and that platform/date display now uses product labels and French month strings.
- Non-blocking P3 noted: `MeetingDetailScreen.kt` still contains some English copy, but that Android detail screen is not wired through `WakeveNavHost` at this stage.

## 8. Documentation and Closure
- [x] 8.1 **@docs** Update guides for the complete event organization flow and offline behavior.
- [x] 8.2 **@review** Perform final read-only review of specs, tests, security, and UX.
- [x] 8.5 **@tests** Add backend regression coverage for final review blockers: event status transitions must require organizer actors and valid workflow transitions, and budget baseline/item mutations must reject non-`ORGANIZING`/`FINALIZED` read-only states.
- [x] 8.6 **@codegen** Fix final review backend blockers without widening access: align `PUT /api/events/{id}/status` with shared workflow/organizer guards and apply organization-status mutation guards to budget baseline/item routes.
- [x] 8.7 **@review** Re-review Phase 8.2 blockers after 8.5/8.6; Phase 8.2 may be checked only if this review explicitly approves.
- [x] 8.8 **@tests** Add backend regression coverage for the Phase 8.7 non-blocking gap: confirmed participants in `ORGANIZING` cannot mutate budget baseline/items while retaining permitted read/expense access.
- [x] 8.3 Run the agreed test suite and record commands/results in the implementation notes.
- [x] 8.4 Archive the OpenSpec change only after implementation is complete, approved, and deployed.

Phase 8.1 documentation evidence:
- `@docs` worker `019e4f96-a4f9-71b3-8d74-c71beef0d032` added `docs/guides/complete-event-organization-flow.md`, covering the full `DRAFT -> POLLING -> CONFIRMED -> COMPARING -> ORGANIZING -> FINALIZED` lifecycle, access rules for organizers and confirmed participants, logistics sections, offline-first SQLDelight state, replayable pending sync, conflicts, finalization blockers, and Android/iOS UX consistency.
- `@docs` updated `docs/README.md` to link the new guide.
- Verification: `git diff --check -- docs/README.md docs/guides/complete-event-organization-flow.md` passes and `openspec validate complete-event-organization-flow --strict` passes.

Phase 8.2 review blockers:
- `@review` worker `019e4fe9-a7c3-7d61-a90a-73ca6e337535` blocked Phase 8.2 final approval.
- P1: `PUT /api/events/{id}/status` enforces organizer/auth workflow rules only for `CONFIRMED`; authenticated non-organizers can request other statuses, and `FINALIZED` relies on repository readiness/status checks without a backend organizer check.
- P1: budget baseline/item mutation routes allow confirmed attendees to update budget baseline and create/update/delete budget items without an `ORGANIZING` mutation guard or `FINALIZED` read-only guard, while expense routes already enforce the workflow.
- P3: backend tests need targeted coverage for non-organizer/invalid event status transitions and finalized budget baseline/item mutation rejection before the final 8.3 suite.
- OpenSpec validation during review passed with `openspec validate complete-event-organization-flow --strict`.

Phase 8.5 RED evidence:
- `@tests` worker `019e4fec-6ec3-7802-983a-c05477b0c2e4` added backend regression coverage in `server/src/test/kotlin/com/guyghost/wakeve/routes/EventOrganizationPhase2BackendRoutesTest.kt` and `server/src/test/kotlin/com/guyghost/wakeve/routes/EventOrganizationPhase5RoutesTest.kt`; no production files were modified.
- Event status tests cover non-organizer rejection for `COMPARING`/`ORGANIZING` and `FINALIZED`, plus invalid `DRAFT -> ORGANIZING` rejection with unchanged status.
- Budget route tests cover `PUT /budget`, `POST /budget/items`, `PUT /budget/items/{itemId}`, and `DELETE /budget/items/{itemId}` rejection before `ORGANIZING` and in `FINALIZED`.
- RED command: `./gradlew :server:test --tests 'com.guyghost.wakeve.routes.EventOrganizationPhase2BackendRoutesTest' --tests 'com.guyghost.wakeve.routes.EventOrganizationPhase5RoutesTest' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process`.
- RED result: compilation OK, `40 tests completed, 5 failed`; the failures reproduce the two Phase 8.2 P1 blockers.
- Hygiene during test addition: `git diff --check` passes and `openspec validate complete-event-organization-flow --strict` passes.

Phase 8.6 GREEN evidence:
- `@codegen` remediation kept production edits scoped to `server/src/main/kotlin/com/guyghost/wakeve/routes/BudgetRoutes.kt`; `server/src/main/kotlin/com/guyghost/wakeve/routes/EventRoutes.kt` already contained the backend organizer/auth workflow guard in the current worktree.
- Budget baseline and item create/update/delete now load the event, require `event.organizerId == authenticated user`, return `403 Forbidden` with audit metadata for non-organizer mutation attempts, and return `409 Conflict` before persistence when the event is not `ORGANIZING` including `FINALIZED`.
- Targeted GREEN: `./gradlew :server:test --tests 'com.guyghost.wakeve.routes.EventOrganizationPhase2BackendRoutesTest' --tests 'com.guyghost.wakeve.routes.EventOrganizationPhase5RoutesTest' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process` passed with `BUILD SUCCESSFUL`.
- Broader server verification: `./gradlew :server:test --no-daemon --no-configuration-cache` passed with `BUILD SUCCESSFUL`.
- Hygiene: `git diff --check -- server/src/main/kotlin/com/guyghost/wakeve/routes/EventRoutes.kt server/src/main/kotlin/com/guyghost/wakeve/routes/BudgetRoutes.kt server/src/test/kotlin/com/guyghost/wakeve/routes/EventOrganizationPhase2BackendRoutesTest.kt server/src/test/kotlin/com/guyghost/wakeve/routes/EventOrganizationPhase5RoutesTest.kt` passes and `openspec validate complete-event-organization-flow --strict` passes.

Phase 8.7 final re-review evidence:
- `@review` worker `019e500b-5077-7ae2-8f64-81a14fc4a237` found no P1/P0 after 8.5/8.6.
- Review verified `PUT /api/events/{id}/status` requires JWT principal, organizer actor, valid workflow transition, and repository finalization readiness before mutation.
- Review verified budget baseline/items load the event, require organizer, and require `ORGANIZING` before persistence while confirmed participants retain budget read and expense access.
- Review explicitly approved checking 8.7 and 8.2, with a non-blocking P2 requesting extra regression coverage for confirmed participant budget baseline/item mutation attempts in `ORGANIZING`.

Phase 8.8 targeted coverage evidence:
- Added `Phase8 confirmed participant cannot mutate budget baseline or items while organizing` to `server/src/test/kotlin/com/guyghost/wakeve/routes/EventOrganizationPhase5RoutesTest.kt`.
- The test creates an `ORGANIZING` fixture, authenticates as `confirmedParticipantId`, asserts `403 Forbidden` for `PUT /budget`, `POST /budget/items`, `PUT /budget/items/{itemId}`, and `DELETE /budget/items/{itemId}`, verifies the seeded baseline/item state did not change, then verifies confirmed participant budget read and expense creation still succeed.
- Targeted GREEN: `./gradlew :server:test --tests 'com.guyghost.wakeve.routes.EventOrganizationPhase5RoutesTest' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process` passed with `BUILD SUCCESSFUL`.

Phase 8.3 final verification evidence:
- Hygiene GREEN: `git diff --check` passes.
- OpenSpec GREEN: `openspec validate complete-event-organization-flow --strict` passes with `Change 'complete-event-organization-flow' is valid`.
- Shared finalization/E2E GREEN: after stopping Gradle daemons and clearing stale shared generated outputs, `./gradlew :shared:jvmTest --tests '*Phase6*' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process` passes with `BUILD SUCCESSFUL`.
- Backend route security/workflow GREEN: `./gradlew :server:test --tests 'com.guyghost.wakeve.routes.EventOrganizationPhase2BackendRoutesTest' --tests 'com.guyghost.wakeve.routes.EventOrganizationPhase5RoutesTest' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process` passes with `BUILD SUCCESSFUL`.
- Android organization UX GREEN: `./gradlew :composeApp:testDebugUnitTest --tests '*Phase7*' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process -Dkotlin.incremental=false` passes with `BUILD SUCCESSFUL`.
- iOS organization UX/logistics GREEN: after fixing the Phase 5 meeting route contract, `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -only-testing:WakeveTests/OrganizationPhase7ContractTests -only-testing:WakeveTests/OrganizationPhase5ContractTests -only-testing:WakeveTests/TransportPlanningContractTests -derivedDataPath /tmp/wakeve-ios-dd-final-organization-8-3-rerun CODE_SIGNING_ALLOWED=NO` passes with `** TEST SUCCEEDED **`.
- Note: `8.4` remains unchecked because OpenSpec archiving is reserved for after explicit deployment/closure approval.

Phase 8.4 archive readiness:
- Implementation, reviews, documentation, and agreed verification are complete for `complete-event-organization-flow`.
- Remaining action is intentionally limited to `openspec archive complete-event-organization-flow --yes` after deployment or explicit closure approval.
- Do not archive before that gate: OpenSpec Stage 3 moves deltas into `openspec/specs/` and should represent the deployed project truth.
- Closure approval received from the user on 2026-05-22: `j'approuve`.
