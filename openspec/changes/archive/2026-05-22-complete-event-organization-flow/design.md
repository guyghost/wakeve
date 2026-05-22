# Design: Complete Event Organization Flow

## Context
The current repository has useful pieces of the complete product, but the product contract is spread across specs and code. The end-to-end user promise needs one authoritative proposal that coordinates lifecycle status, access control, logistics readiness, offline sync, and platform UX.

## Goals
- Keep `Event.status` as the shared workflow source of truth.
- Keep state-machine coordination repository-mediated.
- Make each phase vertically testable with local persistence, sync queue behavior, backend API contract, and UI surface where relevant.
- Require confirmed-attendee access before exposing sensitive organization details.
- Preserve offline-first behavior for all critical actions.

## Non-Goals
- No app code implementation in the proposal step.
- No new mandatory production payment provider.
- No wholesale navigation or architecture rewrite.
- No CRDT migration.

## Key Existing Evidence
- `EventStatus` already includes `DRAFT`, `POLLING`, `COMPARING`, `CONFIRMED`, `ORGANIZING`, and `FINALIZED`.
- `EventManagementStateMachine` already handles `StartPoll`, `ConfirmDate`, `TransitionToOrganizing`, and `MarkAsFinalized`.
- `ScenarioManagementStateMachine` already handles final scenario selection, but the workflow contract needs to specify how selection participates in the full logistics readiness gate.
- SQLDelight files exist for invitations, participants, scenarios, votes, meetings, calendar sync, budget, pots, contributions, transport, activities, equipment, meals, comments, notifications, and sync metadata.
- Specs for several capabilities are descriptive but not fully represented as OpenSpec `Requirement` entries.

## Architecture Decisions

### Decision: Event Status Remains the Workflow Spine
All modules MUST derive availability from `Event.status` and repository data. Modules MUST NOT call each other directly to force workflow progression.

### Decision: Readiness Is Computed, Not Stored as One Boolean
Finalization readiness SHOULD be computed from domain checks:
- confirmed final date,
- required participant confirmations,
- final scenario or explicit no-scenario decision,
- selected destination/lodging or explicit not-needed decision,
- transport plan or explicit not-needed decision,
- meeting link or explicit not-needed decision,
- calendar invitation state,
- notification schedule state,
- budget baseline,
- payment/tricount state or explicit not-needed decision,
- no blocking unsynced critical operations.

### Decision: Offline-First Write Path Is Mandatory
Every critical action MUST write locally first and enqueue a sync operation. If a provider call is unavailable offline, the local intent is stored as pending and the UI shows the pending state.

### Decision: Confirmed-Attendee Access Is Section-Based
Full organization details are only visible to organizers and participants whose attendance is confirmed for the retained date. Public invitation resolution may show only limited event metadata.

### Decision: External Links Are Wrapped in Safe Link Metadata
Payment, Tricount, meeting, booking, and calendar download links MUST store provider, display label, target URL, createdBy, createdAt, and verification status so the UI can prevent ambiguous or suspicious links.

## Workflow
1. `DRAFT`: organizer creates event, potential locations, time slots, and invitation links.
2. `POLLING`: invited participants vote and can confirm or decline participation intent.
3. `CONFIRMED`: organizer locks date; confirmed-attendee details unlock.
4. `COMPARING`: destination/lodging scenarios are proposed, discussed, voted, and selected.
5. `ORGANIZING`: logistics, meetings, budget, payments, calendar, and notifications are completed.
6. `FINALIZED`: event becomes read-only except explicitly allowed post-finalization actions such as viewing and export.

## Access Matrix
| Area | Organizer | Invited pending participant | Confirmed participant | Declined/non-member |
| --- | --- | --- | --- | --- |
| Invitation preview | Yes | Yes | Yes | Limited public metadata |
| Poll vote | Yes | Yes | Yes | No |
| Full logistics | Yes | No | Yes | No |
| Budget/payment details | Yes | No | Yes | No |
| Finalization | Yes | No | No | No |

## Vertical Implementation Phases
Each phase starts with tests delegated to `@tests`, then implementation delegated to `@codegen`, documentation to `@docs`, and read-only review to `@review`.

1. Invitation and confirmed-attendee access.
2. Polling, RSVP confirmation, calendar and notifications.
3. Scenario comparison with destination and lodging.
4. Transport planning from participant departures.
5. Meetings, budget, expenses, payment pot, and Tricount handoff.
6. Finalization readiness and offline sync integrity.
7. Android/iOS UX parity and accessibility.
8. End-to-end workflow verification and documentation.

## Test Strategy
- Shared unit tests for pure logic: scoring, readiness, access policies, budget splits, payment settlements, sync conflict helpers.
- State-machine tests for all valid and invalid transitions, including offline queued transitions.
- Repository/offline tests for SQLDelight persistence and sync queue entries for every critical write.
- Backend route tests for authorization, IDOR prevention, validation, and sync payload handling.
- Android Compose and iOS SwiftUI UI tests for the primary organizer and participant paths when the UI is touched.
- End-to-end shared workflow test: create event, invite participants, vote, confirm date, compare scenarios, select destination/lodging, plan transport, create meeting, schedule notifications/calendar, record budget/payment/tricount, sync, finalize.

## Risks and Mitigations
- Risk: Existing specs are descriptive and may drift from generated code. Mitigation: add OpenSpec deltas with strict requirement/scenario format and keep implementation tasks vertical.
- Risk: Too much scope in one implementation branch. Mitigation: phases are independently testable and can be delivered behind feature flags.
- Risk: Provider integrations block offline behavior. Mitigation: local pending intents and mock/provider abstractions are required before real providers.
- Risk: Access control inconsistencies across backend and local UI. Mitigation: shared access policy tests plus backend route tests for each sensitive area.
