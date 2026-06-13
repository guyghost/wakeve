# Offline Critical Scenarios

Date: 2026-06-13
Status: local release-hardening evidence

This document tracks the offline-first scenarios that must stay covered while Wakeve prepares for the first iOS release. It is intentionally scoped to behavior that can be verified locally. TestFlight, device, and production backend evidence remain covered by the App Store evidence files.

## Strategy

- SQLDelight is the local source of truth for shared KMP state.
- Release behavior stays on last-write-wins for non-critical conflicts unless a concrete product bug proves that CRDT is needed.
- Critical conflicts must be surfaced or block finalization instead of being silently overwritten.
- Pending sync records must be replayable when they are counted as critical blockers.
- Non-replayable audit records, such as historical conflict-resolution rows, must not block finalization readiness.
- Finalized events are read-only for organization mutations.

## Critical Scenario Matrix

| Scenario | Expected behavior | Evidence |
| --- | --- | --- |
| Guest mode offline | Guest users can start locally without backend sync, and guest cleanup stays local. | `shared/src/commonTest/kotlin/com/guyghost/wakeve/auth/offline/GuestModeOfflineTest.kt` |
| Draft/workflow offline operations | Local operations remain usable offline and sync when online. | `shared/src/commonTest/kotlin/com/guyghost/wakeve/e2e/WorkflowE2ETest.kt` |
| Offline event deletion | Offline deletion removes local event state and cascades poll/vote data for supported statuses. | `shared/src/commonTest/kotlin/com/guyghost/wakeve/offline/DeleteEventOfflineTest.kt` |
| Invitation acceptance and RSVP | Invitation creation, participant acceptance, RSVP updates, and retained date validation write locally first and replay in creation order. | `shared/src/commonTest/kotlin/com/guyghost/wakeve/offline/InvitationParticipantOfflineRepositoryTest.kt` |
| Scenario planning offline | Scenario repository writes and selections remain local-first for Phase 3 scenario work. | `shared/src/jvmTest/kotlin/com/guyghost/wakeve/scenario/ScenarioOfflineRepositoryPhase3Test.kt` |
| Transport planning offline | Departure locations, generated plans, final plan selection, not-needed decisions, and deterministic conflict replay persist locally and queue sync metadata. | `shared/src/jvmTest/kotlin/com/guyghost/wakeve/transport/TransportOfflineRepositoryPhase4Test.kt` |
| Phase 5 organization sync | Expenses, payment pots, Tricount handoff, and meeting sync payloads include replayable payload, retry state, and retry count. | `shared/src/jvmTest/kotlin/com/guyghost/wakeve/organization/EventOrganizationPhase5ReadinessTest.kt` |
| Phase 5/6 shared offline edges | Shared offline organization operations record replayable sync metadata for downstream readiness. | `shared/src/jvmTest/kotlin/com/guyghost/wakeve/organization/EventOrganizationPhase56SharedOfflineRedTest.kt` |
| Phase 6 finalization readiness | Finalization stays blocked while critical offline sync is pending, failed, or conflicted; it succeeds after convergence. | `shared/src/jvmTest/kotlin/com/guyghost/wakeve/workflow/EventOrganizationPhase6EndToEndSyncTest.kt` |
| Conflict resolution | Non-critical conflicts auto-resolve with LWW; critical title/status/participant conflicts require explicit resolution. | `shared/src/commonTest/kotlin/com/guyghost/wakeve/sync/conflict/ConflictResolutionIntegrationTest.kt` |
| Sync retries | Failed sync operations retry within configured limits and expose terminal failure state. | `shared/src/jvmTest/kotlin/com/guyghost/wakeve/sync/SyncManagerTest.kt` |
| Analytics queue offline | Analytics events queue offline, retry, and can be cleared for privacy/account deletion flows. | `shared/src/commonTest/kotlin/com/guyghost/wakeve/analytics/AnalyticsQueueTest.kt` |

## CRDT Decision

CRDT is not part of the first release plan. The current evidence supports LWW plus explicit critical-conflict handling:

- Non-critical fields can be resolved by timestamp without user interruption.
- Critical fields such as title, status, description, and participant list are detected and surfaced.
- Organization finalization is blocked by unresolved critical sync or conflict state.

Revisit CRDT only if one of these conditions is observed in production or TestFlight:

- Repeated user-visible data loss under concurrent edits.
- Collaboration surfaces that require multi-author merging of the same text/body field.
- Product requirements for real-time co-editing instead of local-first replay.
- Conflict-resolution support load that cannot be reduced by clearer LWW and critical-conflict UX.

## Verification Commands

Run the focused offline and sync evidence:

```bash
./gradlew :shared:jvmTest \
  --tests com.guyghost.wakeve.auth.offline.GuestModeOfflineTest \
  --tests com.guyghost.wakeve.offline.DeleteEventOfflineTest \
  --tests com.guyghost.wakeve.offline.InvitationParticipantOfflineRepositoryTest \
  --tests com.guyghost.wakeve.sync.conflict.ConflictResolutionIntegrationTest \
  --tests com.guyghost.wakeve.scenario.ScenarioOfflineRepositoryPhase3Test \
  --tests com.guyghost.wakeve.transport.TransportOfflineRepositoryPhase4Test \
  --tests com.guyghost.wakeve.organization.EventOrganizationPhase5ReadinessTest \
  --tests com.guyghost.wakeve.organization.EventOrganizationPhase56SharedOfflineRedTest \
  --tests com.guyghost.wakeve.workflow.EventOrganizationPhase6EndToEndSyncTest \
  --tests com.guyghost.wakeve.sync.SyncManagerTest \
  --tests com.guyghost.wakeve.analytics.AnalyticsQueueTest
```

Run the release-critical subset:

```bash
./scripts/test-critical-release-gates.sh
```

## Open Gaps

- Device/TestFlight offline draft persistence remains part of `docs/APP_STORE_TESTFLIGHT_EVIDENCE.md`.
- Production backend sync monitoring remains part of `docs/APP_STORE_OBSERVABILITY_EVIDENCE.md`.
- If CRDT becomes necessary, create an OpenSpec proposal before changing conflict semantics.
