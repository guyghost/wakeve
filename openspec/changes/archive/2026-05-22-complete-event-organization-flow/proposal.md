# Change: Complete Event Organization Flow

## Why
Wakeve already contains several isolated capabilities for event creation, polling, scenarios, meetings, calendar, notifications, budget, and offline sync, but the product is not specified as one complete organizer journey from `DRAFT` to `FINALIZED`.

This change defines the missing end-to-end contract so an organizer can run a complete event and confirmed participants can collaborate on the logistical details while the app remains offline-first and access-controlled.

## Current State From Exploration
- `event-organization` and `workflow-coordination` define the DRAFT, polling, confirmation, organizing, and finalized states, and the code includes `EventManagementStateMachine`, `ScenarioManagementStateMachine`, and `MeetingServiceStateMachine`.
- Several specs exist but are not complete OpenSpec requirement sets yet: `collaboration-management`, `scenario-management`, `destination-planning`, `transport-optimization`, `payment-management`, and `calendar-management` are largely descriptive or counted as zero requirements by OpenSpec.
- Shared code already includes models or partial services for invitations, participants, scenarios, transport, budget, meetings, notifications, calendar, and sync, but the finalization gate does not yet require logistics readiness across destination/lodging, transport, meetings, budget/payment, calendar, notifications, and offline synchronization.
- Active changes currently present are `add-ios-design-system` and `connect-ios-notifications`; this proposal avoids changing their files and treats iOS notification work as a dependency to integrate, not duplicate.

## What Changes
- Define a complete vertical workflow from event creation to finalization, preserving `DRAFT -> POLLING -> CONFIRMED -> COMPARING -> ORGANIZING -> FINALIZED`.
- Formalize participant invitation, RSVP/date validation, confirmed-attendee access, collaboration, and section-level permissions.
- Require scenario comparison to connect destination, lodging, estimated budget, participant votes, and final scenario selection.
- Require transport planning from participant departure locations to the confirmed destination/date.
- Require meetings or virtual links for events that need remote coordination, including notification and calendar integration.
- Require budget, shared expenses, settlements, payment pot, and Tricount handoff to participate in the organizing readiness gate.
- Require notifications and calendar artifacts for key workflow transitions.
- Require offline-first local writes, queued sync, conflict behavior, and visible sync status for every critical organizing operation.
- Require coherent Android Compose and iOS SwiftUI UX for the same workflow, with platform-native presentation.

## Non-Goals
- Real payment capture in production providers; mock/provider abstraction remains acceptable until a provider-specific change is approved.
- Replacing last-write-wins conflict resolution with CRDT.
- Rewriting existing state machines or repositories before the proposal is approved.
- Implementing code in this change creation step.

## Impact
- Affected specs: `event-organization`, `workflow-coordination`, `collaboration-management`, `scenario-management`, `destination-planning`, `transport-optimization`, `notification-management`, `budget-management`, `payment-management`, `meeting-service`, `calendar-management`, `security-management`, `offline-sync`, `cross-platform-organization-ux`
- Affected shared code after approval: event, participant, invitation, scenario, destination/lodging, transport, meeting, calendar, notification, budget, payment, sync, security, and presentation state-machine modules.
- Affected backend after approval: event, participant, invitation, scenario, destination/accommodation, transport, meeting, calendar, notification, budget, payment, and sync routes.
- Affected UI after approval: Android Compose organization flow and iOS SwiftUI organization flow.
- Test impact after approval: shared unit tests, state-machine tests, repository/offline tests, backend route tests, targeted Android/iOS UI tests, and one end-to-end workflow test.

## Approval Gate
Implementation MUST NOT start until this proposal and all deltas validate with `openspec validate complete-event-organization-flow --strict` and receive human approval.
