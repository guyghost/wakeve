# iOS Event Invitation Experience Model

## Status and purpose

This document is the behavioral source of truth for `expand-ios-invitation-experience`. It models a connected iOS event experience; it does not replace or weaken the existing domain workflows. `ios-event-detail-invitation-canvas.md` remains the presentation-only model for the Event Detail hero and is consumed as one building block.

This model is normative for state, ownership, guards, routing, persistence, offline truthfulness, and read-only behavior. Mobbin references are non-normative visual references. Wakeve domain rules always win.

## Canonical visual references

| Reference | Wakeve adaptation |
|---|---|
| [Invites library](https://mobbin.com/screens/d04f0876-4c6c-461e-bcba-ce6fb752ea95) | Scannable event library with explicit projections and one next action per card |
| [Creation](https://mobbin.com/screens/e07fe9b7-1409-4345-9df0-56806b591ba6) | Artwork-led studio with a required preview before persistence |
| [Contextual modules](https://mobbin.com/screens/e5d7db23-34bd-4aa5-bbbf-6d74bd65e18e) | Event-scoped modules ordered by current lifecycle and access |
| [Share and audience](https://mobbin.com/screens/d726cfff-2f7f-406c-b56a-b88fafcade02) | Typed audience state and server-authorized sharing only |
| [Approvals](https://mobbin.com/screens/93e6c76c-c79b-4494-8cff-b61c72736251) | Explicit guest approval policy and request states when backend-supported |
| [Settings](https://mobbin.com/screens/c77b278e-3957-4b76-a1f7-60580a313347) | Compact event-scoped information and permission-gated controls |
| [Wallet stack](https://mobbin.com/screens/30c73314-7baa-46a1-9419-155aa6282488) | Layered, glanceable event cards; no pass or payment semantics |
| [Pass detail](https://mobbin.com/screens/e1061c73-0be8-42ba-aaed-3bb1c3321a4e) | Artwork-led detail hierarchy with structured metadata |
| [Information](https://mobbin.com/screens/f3ce79fa-8661-4833-a601-04c40cc95ef5) | Secondary metadata in a dedicated information sheet |
| [Archive](https://mobbin.com/screens/4e2bd1e9-9a04-46ba-9bd9-db7093ace671) | Deliberately read-only past/finalized browsing |

## Authority and ownership

| Concern | Sole owner | iOS responsibility |
|---|---|---|
| `EventStatus` and lifecycle transitions | `EventManagementStateMachine` plus the already-approved scenario workflow where applicable | Dispatch typed intents and render repository projections; never write status directly |
| Public invitation issuance, open, inspect, redeem, rotate, revoke, expiry, authentication, and retry | Active change `harden-event-invitation-links` and its server/backend authority | Render the injected secure-share capability and return its opaque server-issued payload unchanged |
| Events, artwork, audience, membership, preferences, operation receipts, and library projections | Corresponding repositories; local SQLDelight is the read source and sync queue records pending writes | Observe persisted snapshots and show their sync state |
| Participant organization access | `ParticipantAccessMapper` and `EventAccessPolicy` | Consume typed decisions; never parse role or RSVP strings in a view |
| Poll, Participants, Organization, Calendar, Maps, Weather | Their existing flow/service owners | Route or open typed destinations; do not duplicate mutations in Event Detail |
| Canvas presentation | `ios-event-detail-invitation-canvas.md` | Render its total projection and forward its typed action |

There is no second lifecycle machine in this experience. A repository update is the only handoff between owners. Display copy, artwork, alt text, event description, provider text, and LLM output never authorize a transition or permission.

### Canonical lifecycle reference, never reimplemented by iOS

| Typed event | From → to | Guard/effect owner |
|---|---|---|
| `CreateEvent` | none → `DRAFT` | `EventManagementStateMachine` validates the aggregate and repository persists it |
| `StartPoll` | `DRAFT` → `POLLING` | Organizer and required-slot guards in `EventManagementStateMachine` |
| `PublishScenarioMatrix` | `DRAFT` → `COMPARING` | Approved scenario workflow and repository transaction |
| `ConfirmDate` | `POLLING` → `CONFIRMED` | Organizer, vote, slot, and confirmation guards in `EventManagementStateMachine` |
| `SelectScenarioAsFinal` | `COMPARING` → `CONFIRMED` | Approved scenario workflow and repository transaction |
| `TransitionToOrganizing` | `CONFIRMED` → `ORGANIZING` | Organizer and workflow guards in `EventManagementStateMachine` |
| `MarkAsFinalized` | `ORGANIZING` → `FINALIZED` | Organizer, readiness, and sync-safety guards in `EventManagementStateMachine` |

The iOS surfaces dispatch these typed intents only through installed owner callbacks, then observe the repository result. Failure, cancellation, or pending sync does not advance a locally invented presentation status.

## Shared typed inputs

- `eventStatus`: `DRAFT | POLLING | COMPARING | CONFIRMED | ORGANIZING | FINALIZED`.
- `viewerRole`: `ORGANIZER | MEMBER | NON_MEMBER` from persisted membership.
- `participantAccess`: typed RSVP/date-validation decision from `ParticipantAccessMapper`.
- `temporalClass`: `UNDATED_DRAFT | UPCOMING | PAST`, computed from structured event bounds and a supplied clock. `FINALIZED` does not itself mean `PAST`.
- `syncState(subject)`: `SYNCED | PENDING(operationId) | CONFLICT(operationId) | PERMANENT_FAILURE(operationId) | UNAVAILABLE`.
- `freshness`: `CURRENT | STALE(cachedAt) | UNAVAILABLE`.
- `interactionPolicy`: `READ_ONLY` when `temporalClass == PAST`, `eventStatus == FINALIZED`, or the archive surface is active; otherwise `INTERACTIVE` subject to capability guards. This override is evaluated before every surface, route, menu, deep link, and next-action mapper.
- `routeCapability`: typed destinations actually installed for `DRAFT_EDITOR | POLL | PARTICIPANTS | ORGANIZATION | EVENT_INFORMATION | ARCHIVE_DETAIL`.
- `PreviousStableState<Snapshot>`: exactly `IDLE | READY(snapshot, freshness) | EMPTY(scope)`. Every load captures one value and cancellation restores that exact value; it is never optional or synthesized from partial data.

Unknown enum values, missing repository records, or unavailable access projections fail closed to read-only summary or no surface. No nearby status is guessed.

For `READ_ONLY` caused by `PAST` or `FINALIZED`, the only event-opening action is `VIEW_ARCHIVE`. A deep link to Poll, Participants mutation, Organization mutation, Studio, Audience management, or Information mutation is resolved to `ARCHIVE_DETAIL`; if that read route is unavailable, it shows a local read-only summary. Sync conflict/failure is a warning with `RELOAD_PROJECTION` only, never `RESOLVE_SYNC` or a mutation.

The global `PAST` read-only override is an intentional breaking behavior of this experience, not a fallback to the previous mutable presentation.

## Persisted projections

### Event Library

Library membership is repository-derived; projections are filters, not lifecycle states, and may overlap:

- `DRAFTS`: organizer-owned events with `status == DRAFT`.
- `HOSTING`: events whose persisted `organizerId` equals the viewer.
- `ATTENDING`: active member records with `rsvp == ACCEPTED`; pending, declined, left, and removed records are excluded.
- `UPCOMING`: accessible non-draft events whose structured end/final bound is not before the supplied clock.
- `PAST`: accessible events whose structured end/final bound is before the supplied clock.

An undated draft appears only in `DRAFTS` and `HOSTING`. A future `FINALIZED` event may be in `UPCOMING` but remains read-only. Ordering uses typed date, then stable event id; never title or prose.

`LibraryNextAction` is exactly one of `CONTINUE_DRAFT | SUBMIT_VOTE | VIEW_POLL_RESULTS | COMPARE_OPTIONS | CONTINUE_ORGANIZATION | VIEW_EVENT | VIEW_ARCHIVE`. It consumes the same typed context/action projection as Event Detail. `VIEW_ARCHIVE` overrides every other action for `PAST` or `FINALIZED`; a sync conflict/permanent failure adds a warning and read-only reload affordance without replacing the next action. Unavailable action data falls back to `VIEW_EVENT` only for interactive events.

Library load state is `IDLE | LOADING(previousStableState) | READY(snapshot, freshness) | EMPTY(projection) | FAILED(error, previousStableState)`, where the captured value is `PreviousStableState<LibrarySnapshot>`. `CANCEL_LOAD` restores it exactly and is not terminal. Repository observation may update any state to `READY` or `EMPTY`.

### Creation Studio

`StudioMode` is total: `NEW | EDIT_EXISTING(eventId, baseRevision)`. The UI exposes `EDIT_EXISTING` only with an owner-supplied edit capability; this is a display guard, not authorization. On commit, the typed owner callback `UpdateDraftAggregate(eventId, actorId, expectedBaseRevision, eventDraft, artwork, operationId)` MUST re-read and revalidate organizer identity, `status == DRAFT`, and exact `baseRevision` inside `EventManagementStateMachine`/the repository transaction, then atomically persist event fields, artwork, incremented revision, and operation receipt. Release 1 exposes edit only for `DRAFT`. `CreationDraft` is transient and carries a separate monotonically increasing `draftRevision`.

Persisted artwork is a non-null union:

```text
Artwork = NONE
        | STRUCTURED(version, ref)
        | LEGACY_REMOTE(validatedHttpsUrl)
ref     = ArtworkRef(source, alt, focalPoint, crop)
source  = PRESET(presetId) | SERVER_ASSET(assetId, canonicalHttpsUrl, assetRevision)
alt     = DECORATIVE | INFORMATIVE(nonBlankLocalizedText)
```

`focalPoint` is normalized to `0...1`; `crop` is `FILL | FIT`. `NONE` renders the deterministic event-mood fallback. A valid historical `heroImageUrl` projects to `LEGACY_REMOTE`; an invalid/unavailable legacy URL renders the mood fallback plus a migration warning. Migration is one-way: only an already-authorized owner-supplied `SERVER_ASSET` may replace legacy artwork as `STRUCTURED`; structured artwork is never rewritten as legacy. Rendering failure never silently changes the persisted union.

All canonical or legacy URLs require HTTPS, an allowlisted server/CDN host, no userinfo/credentials, no fragment, and no secret-bearing query value. URLs and asset payloads are not logs, analytics fields, or invitation data.

Release 1 artwork choice is exactly `KEEP_EXISTING | NONE | PRESET(presetId) | EXISTING_SERVER_ASSET(assetId)`. `KEEP_EXISTING` applies only to `EDIT_EXISTING`. `ArtworkSelectionCapability` is `HIDDEN | UNAVAILABLE(reason) | READY(actorId, accessRevision, authorizedAssetsByOpaqueId)` and is supplied by the asset owner. `SELECT_SERVER_ASSET` succeeds only when the id and asset revision are present in the current capability; the Studio persists that existing canonical reference without fetching, uploading, or constructing a URL. Missing/stale capability fails closed while preserving the draft choice for later review.

Studio state is `IDLE(mode) | LOADING_EXISTING(previousStableStudioState) | EDITING(mode, baseRevision, draftRevision) | RESOLVING_ARTWORK(draftRevision) | PREVIEW_READY(draftRevision) | PREVIEWING(draftRevision) | COMMITTING(operationId, mode, draftRevision) | PENDING_SYNC(eventId, committedRevision, operationId) | FAILED_BEFORE_COMMIT(error, mode, draft) | SYNC_FAILED(eventId, committedRevision, operationId, error) | COMPLETED(eventId, committedRevision) | CLOSED`. For `NEW`, `baseRevision` is the typed sentinel `NOT_APPLICABLE`; it is never null. `previousStableStudioState` is the exact prior `IDLE` or editing/preview state. `COMPLETED` and `CLOSED` are terminal session states; cancelling a load restores that exact state.

| Event | Guard | State/effect |
|---|---|---|
| `LOAD_EXISTING` / `EXISTING_LOADED` | matching event id/repository revision and edit capability | Load persisted event/artwork into `EDITING(EDIT_EXISTING, baseRevision, 0)` |
| `UPDATE_FIELD` / `UPDATE_ARTWORK` | interactive Studio | Increment `draftRevision`; never change `baseRevision` |
| `REQUEST_PREVIEW` / `OPEN_PREVIEW` | fields valid; preset or selected server asset is capability-authorized | Resolve artwork, then preview the exact `draftRevision` |
| `CONFIRM_COMMIT` | preview revision current; typed owner callback installed | Use stable operation id and dispatch `CreateEvent` for `NEW` or `UpdateDraftAggregate` for `EDIT_EXISTING`; owner revalidates actor, `DRAFT`, and base revision |
| `LOCAL_COMMIT` | matching operation id/revision and successful owner transaction | Observe atomically stored event, total artwork, incremented event revision, and operation receipt; enter `PENDING_SYNC` or `COMPLETED` |
| `FAIL_BEFORE_LOCAL_COMMIT` | no repository transaction committed | `FAILED_BEFORE_COMMIT`; preserve mode/draft for corrected retry |
| `SYNC_FAILED` | local transaction already committed | Preserve event id, committed revision, and receipt; never return to create/update |
| `RETRY_BEFORE_COMMIT` | typed retryable pre-commit error | Retry the mode-specific create/update with its stable idempotency key |
| `RETRY_SYNC` | matching post-commit receipt | Replay only that sync operation; never call `CreateEvent` or `UpdateDraftAggregate` again |
| `CANCEL_LOAD` | captured `previousStableStudioState` exists | Restore it exactly; not terminal |
| `CLOSE` | before commit or after observing commit | For `NEW`, discard only transient draft; for `EDIT_EXISTING`, leave the existing event unchanged; after commit, keep repository truth |

Stale load, artwork, commit, and sync callbacks are ignored unless event id, base/event revision, draft revision, capability revision, and operation id match. Offline local creation/edit may become `PENDING_SYNC`, but is never “shared” or “server confirmed.”

### Audience and Invitations

The surface combines independent, typed regions:

- `AudienceSnapshot`: `IDLE | LOADING(previousStableState) | READY(axesByIdentity, freshness) | EMPTY | FAILED(error, previousStableState)`, using `PreviousStableState<AudienceSnapshot>`; `CANCEL_LOAD` restores it exactly.
- `DirectInviteCapability`: `HIDDEN | UNAVAILABLE(reason) | READY(eventId, actorId, accessRevision, allowedEventStatuses)`, supplied by the owning participant/invitation use case. Release 1's canonical allowed set is exactly `{DRAFT}` until that owner is explicitly expanded.
- `RecipientKey`: a protected pseudonymous `OPAQUE_ID` or service-keyed digest scoped to the event/batch; it is not a raw email, phone number, contact name, or reversible client hash.
- `DirectInviteOperation`: `IDLE | SUBMITTING(batchId, operationId, recipientKeys) | PENDING_SYNC(batchId, operationId, recipientKeys) | COMPLETED(batchId, outcomesByRecipientKey) | FAILED(batchId, operationId, outcomesByRecipientKey, batchError) | CANCELLED(batchId, outcomesByRecipientKey)`.
- `DirectInviteRecipientOutcome`: `SERVER_ACCEPTED(invitationId) | INVALID(reason) | FAILED(error) | CANCELLED`; every requested `RecipientKey` has exactly one outcome in a completed/failed batch.
- `SecureShareCapability`: `HIDDEN | UNAVAILABLE(reason) | LOADING | READY(binding, serverIssuedPayload) | FAILED(reason)` supplied only by `harden-event-invitation-links`, where `binding = (eventId, actorId, accessRevision, capabilityId)`.
- `GuestApprovalPolicy`: `NOT_SUPPORTED | AUTO_ACCEPT | REQUIRE_APPROVAL`.
- `InviteDeliveryState`: `NONE | QUEUED_LOCAL(operationId) | SUBMITTING(operationId) | SERVER_ACCEPTED(invitationId) | DELIVERY_PENDING(invitationId) | DELIVERED(invitationId) | FAILED_BEFORE_SERVER(operationId, error) | FAILED_AFTER_SERVER(invitationId, error) | REVOKED(invitationId)`.
- `ApprovalState`: `NOT_APPLICABLE | REQUESTED(requestId) | APPROVED(requestId) | REJECTED(requestId)` plus online-only `ApprovalOperation = IDLE | APPROVING(requestId, operationId) | REJECTING(requestId, operationId) | FAILED(requestId, operationId, error)`.
- `MembershipState`: `NON_MEMBER | ACTIVE_MEMBER(memberId) | LEFT(memberId) | REMOVED(memberId)`.
- `RsvpState`: `NOT_APPLICABLE | PENDING | ACCEPTED | DECLINED | UNAVAILABLE`.
- `DateValidationState`: `NOT_APPLICABLE | NOT_VALIDATED | VALIDATED_RETAINED_DATE | UNAVAILABLE`.

Counts and identities derive from the same repository snapshot. Delivery, approval, membership, RSVP, and date validation are orthogonal axes; no value on one axis propagates or infers a value on another. For `NON_MEMBER` or invite/request-only identities, RSVP and date validation are `NOT_APPLICABLE`. For an active member, a missing RSVP or applicable retained-date record is `UNAVAILABLE`, never inferred as `PENDING`/`NOT_VALIDATED`; date validation is also `NOT_APPLICABLE` before a retained date exists. In particular, server acceptance is not delivery, approval is not membership, membership is not RSVP acceptance, and RSVP acceptance is not retained-date validation.

| Event | Guard | State/effect |
|---|---|---|
| `INVITE_DIRECT(transientRecipientInputs)` | non-empty input; interactive organizer; matching `DirectInviteCapability.READY` | Owner normalizes/deduplicates input, produces protected `RecipientKey`s and owner-encrypted delivery envelopes, then submits once; the surface/offline projection records only keys as `PENDING_SYNC`/`QUEUED_LOCAL` |
| `DIRECT_INVITE_ACK(batchId, operationId, outcomesByRecipientKey)` | matching batch and stable operation id; protected key set equals requested key set | Store each keyed outcome independently and refresh one coherent audience snapshot; never collapse to one invitation id |
| `RETRY_DIRECT_INVITE` | capability still matches and at least one keyed outcome is retryable | Replay only retryable unresolved protected keys under the batch/idempotency contract; never duplicate successful recipients |
| `CANCEL_DIRECT_INVITE` | batch has unresolved recipients and owner cancellation capability | Cancel only unresolved recipients; already accepted recipients remain accepted and require an explicit supported revoke action |
| `REQUEST_PUBLIC_LINK` | interactive organizer and secure capability installed | Delegate `ISSUE` to `harden-event-invitation-links` |
| `SHARE_PUBLIC_LINK` | `READY` binding exactly matches current event, actor, access revision, and capability id | Pass payload unchanged to the secure share callback |
| `RETRY_PUBLIC_LINK` | capability failure is retryable | Delegate `RETRY`; iOS never synthesizes a link |
| `APPROVE_GUEST` / `REJECT_GUEST` | interactive organizer, `REQUIRE_APPROVAL`, pending request, backend capability installed, network available | Submit online-only decision; membership/redeem outcome remains server-authoritative |
| `RETRY_APPROVAL` | matching retryable failed request, network available | Replay stable operation id; no offline optimistic approval |

Raw recipient input exists only inside the invitation owner's transient normalization/submission boundary. Outcome maps, sync receipts, diagnostics, logs, analytics, and UI identifiers contain `RecipientKey` only. When delivery must be retried/offline-queued, only the owner may retain a separately encrypted delivery envelope, keyed by `RecipientKey`, until acknowledgement or bounded expiry. The owner clears raw transient input after sealing/submission and includes envelopes/protected invite records in deletion, retention, and account erasure; no raw recipient value, envelope, or orphaned protected key may survive the owning invite/account lifecycle.

Any event, actor, access revision, or capability-id change invalidates `READY` immediately to `UNAVAILABLE(STALE_BINDING)` until the owner issues a fresh capability. The opaque payload is held in memory only for the share invocation: it is never parsed, logged, persisted, cached, indexed, or sent to analytics. `HIDDEN`, `UNAVAILABLE`, `LOADING`, and `FAILED` never expose a share action. On the six surfaces modeled here, local token, locally constructed public URL, local QR, token decoding, and client-authorized redeem are forbidden. Project-wide cutover/removal of legacy invitation mechanisms outside these surfaces is not asserted by this model and remains solely owned by `harden-event-invitation-links`. Link open/redeem cancellation and terminal states are exactly those of that owner, not redefined here.

### Event Detail and real flow routing

Event Detail composes the existing canvas projection with repository-backed contextual modules. The canvas performs no mutation. Its typed actions route as follows:

| Canvas action | Required capability | Destination |
|---|---|---|
| `EDIT_DRAFT` | `routeCapability.DRAFT_EDITOR`, organizer, `DRAFT`, owner edit capability | Creation/Draft flow |
| `SUBMIT_VOTE` | eligible polling access | Poll flow |
| `VIEW_POLL_RESULTS` | poll read access | Poll flow |
| `COMPARE_OPTIONS` | scenario access | Comparison flow within Organization |
| `CONTINUE_ORGANIZATION` | typed available readiness destination | Organization flow |
| `SHOW_ACCESS_STATE` | none | Local permitted information sections |
| `SHOW_DETAILS` / `VIEW_FINAL_DETAILS` | none | Local scroll or read-only finalized detail |

Participants opens only when `routeCapability.PARTICIPANTS` and access policy permit it. Organization opens only from the typed access/readiness decision. Missing routes fall back to local details; they never dispatch an adjacent mutation.
The global `PAST`/`FINALIZED` router preflight runs before this table, so a stale mutating action or deep link can only become `VIEW_ARCHIVE`, never reach its requested owner callback.

### Event Information sheet

`EventInformationSnapshot` contains event id/status, organizer identity, typed provider metadata, structured calendar/maps/weather destinations, current viewer capabilities, and three separate notification axes. Provider text is display-only.

- `EventNotificationPreference`, persisted by `(eventId, userId)`: `INHERIT_ACCOUNT | ALL_EVENT_UPDATES | ESSENTIAL_ONLY | MUTED`.
- `AccountNotificationPreference`, read-only here: the existing enabled notification types, quiet hours, sound, and vibration policy.
- `SystemNotificationAuthorization`, projected from the iOS port as a total value: `UNAVAILABLE | NOT_DETERMINED | PROVISIONAL | AUTHORIZED | EPHEMERAL | DENIED | RESTRICTED`. `UNAVAILABLE` means the port has not yet supplied a snapshot or is unavailable; it is distinct from the concrete iOS authorization value `NOT_DETERMINED`.

While system authorization is `UNAVAILABLE`, system delivery is ineligible and Event Information displays an honest unavailable state. This state never requests permission and never mutates the event or account notification axes. It may transition to a concrete authorization value only through the typed `SYSTEM_NOTIFICATION_AUTHORIZATION_SNAPSHOT_RECEIVED(value)` callback emitted by the iOS authorization port; view appearance, retry text, or an event-preference write cannot synthesize that transition.

Effective routing uses this priority table:

| Priority | Condition/input | Effective rule |
|---|---|---|
| 1 | OS is `UNAVAILABLE | DENIED | RESTRICTED | NOT_DETERMINED` | No system delivery. `UNAVAILABLE` renders honestly as unavailable; no app preference or critical classification bypasses this gate. |
| 2 | Message is critical security | Event preference does not apply; the existing security/account routing policy decides eligibility, still subject to the OS gate. |
| 3 | Non-critical account preference | Begin with the account-enabled event notification types; globally disabled types remain disabled. |
| 4 | Event preference | Intersect the account-enabled set with: all event types for `INHERIT_ACCOUNT`/`ALL_EVENT_UPDATES`, essential event types for `ESSENTIAL_ONLY`, or the empty set for `MUTED`. |
| 5 | Account quiet hours | Always defer/silence eligible non-critical delivery according to the account policy; no event preference bypasses quiet hours. |

`ALL_EVENT_UPDATES` therefore means “apply no additional event-level type restriction”; it never re-enables a type disabled globally. The sheet displays all three axes and the resulting effective state, but editing mutates only the event-scoped record. It never changes account preferences or triggers the system permission prompt; those require their dedicated flows. Until the event-scoped repository contract exists, its control is unavailable.

Each external destination is `HIDDEN | LOADING | READY(typedDestination) | UNAVAILABLE(reason) | FAILED(reason)`. Calendar is available only for a structured confirmed date and eligible access; Maps only for a validated location; Weather only for a typed supported forecast snapshot/location. Opening a destination is a non-domain side effect and does not claim completion.

Every information write carries `OperationKey(subject, action, target, operationId)`, where subject is exactly `EVENT_NOTIFICATION(eventId, userId) | MEMBERSHIP(eventId, memberId) | EVENT(eventId)`, action is exactly `SAVE_EVENT_PREFERENCE | LEAVE_EVENT | REMOVE_PARTICIPANT | DELETE_EVENT`, and target contains the affected user/event id. Information interaction state is `IDLE | READY(snapshot, freshness) | EMPTY | LOADING(previousStableState) | SAVING(OperationKey) | PENDING_SYNC(OperationKey) | CONFIRMING(subject, action, target) | EXECUTING(OperationKey) | FAILED(OperationKey, error, previousStableState)`, using `PreviousStableState<EventInformationSnapshot>`. Retry, acknowledgement, and late-callback matching require all four key dimensions. `CANCEL_LOAD` restores the exact captured state; `CANCEL_CONFIRMATION` restores the stable state that opened confirmation; neither is terminal.

- `LEAVE_EVENT`: active non-organizer member, non-finalized, installed repository use case.
- `REMOVE_PARTICIPANT`: organizer, non-finalized, active target other than organizer, installed repository use case.
- `DELETE_EVENT`: organizer and non-finalized only; it dispatches the existing guarded `DeleteEvent` intent. It does not invent a `CANCELLED` `EventStatus`.
- Organizer cannot leave their own event. Ownership transfer is out of scope.
- Destructive confirmation cancellation has no side effect. After a commit, repository truth wins and UI observes the result; cancellation never rolls it back implicitly.
- Under `PAST` or `FINALIZED`, all writes are absent and the sheet is read-only.

### Past and Finalized archive

The archive accepts events where `temporalClass == PAST` or `eventStatus == FINALIZED`. Its interaction policy is always `READ_ONLY`, even when a past event has an unexpectedly non-finalized lifecycle. It may show artwork, final structured date, organizer, participant summary, settled contextual summaries, and sync/freshness warnings. It cannot vote, invite, approve, edit, leave, remove, cancel, or alter notification preferences. Unknown/restricted fields are omitted, not replaced with sample content.

Archive load state is `IDLE | LOADING(previousStableState) | READY(snapshot, freshness) | EMPTY | FAILED(error, previousStableState)`, using `PreviousStableState<ArchiveSnapshot>`. `CANCEL_LOAD` restores the exact captured state; retry reloads repository data only. There is no lifecycle “unfinalize” transition.

## Surface × role × EventStatus matrix

Legend: `R` read, `M` permitted surface mutations/routes under guards, `RO` forced read-only, `—` hidden, `I` invitation preview only. Every cell also requires repository membership/access. Before this table is evaluated, `PAST` overrides every visible cell to `RO` and every event-opening action/deep link to `VIEW_ARCHIVE`; `FINALIZED` does the same in its column. `M*` is owner-capability-only and never includes direct invite after `DRAFT` in release 1.

| Surface | Role/access | `DRAFT` | `POLLING` | `COMPARING` | `CONFIRMED` | `ORGANIZING` | `FINALIZED` |
|---|---|---|---|---|---|---|---|
| Library | organizer | M | M | M | M | M | RO |
| Library | eligible member | — | M | M | R/M | R/M | RO |
| Library | pending/restricted | — | I | I | I | I | I/RO |
| Library | non-member | — | — | — | — | — | — |
| Creation Studio | organizer | M | — | — | — | — | — |
| Creation Studio | eligible member | — | — | — | — | — | — |
| Creation Studio | pending/restricted | — | — | — | — | — | — |
| Creation Studio | non-member | new-event M only | — | — | — | — | — |
| Audience | organizer | M | R/M* | R/M* | R/M* | R/M* | RO |
| Audience | eligible member | — | R | R | R | R | RO |
| Audience | pending/restricted | — | I | I | I | I | I/RO |
| Audience | non-member | — | I only through secure link owner | I | I | I | I/RO |
| Event Detail | organizer | R/M | R/M | R/M | R/M | R/M | RO |
| Event Detail | eligible member | — | R/M | R/M | R/M | R/M | RO |
| Event Detail | pending/restricted | — | I | I | I | I | I/RO |
| Event Detail | non-member | — | I | I | I | I | I/RO |
| Information | organizer | R/M | R/M | R/M | R/M | R/M | RO |
| Information | eligible member | — | R/M | R/M | R/M | R/M | RO |
| Information | pending/restricted | — | I | I | I | I | I/RO |
| Information | non-member | — | — | — | — | — | — |
| Archive | organizer | RO only when past | RO when past | RO when past | RO when past | RO when past | RO |
| Archive | eligible member | — | RO when past | RO when past | RO when past | RO when past | RO |
| Archive | pending/restricted | — | permitted preview only | permitted preview only | permitted preview only | permitted preview only | permitted preview only |
| Archive | non-member | — | — | — | — | — | — |

## Persistence compatibility and erasure

The persisted event aggregate envelope contains event fields, total artwork, `aggregateRevision`, and operation receipts. Every repository writer—new or legacy, feature flag on or off, and every supported app/backend version—must use one of these typed capabilities: `FULL_AGGREGATE_WRITER`, `PRESERVING_LEGACY_WRITER`, or `FENCED`. A full writer validates and writes the whole envelope. A preserving legacy writer requires exact base-revision compare-and-swap, advances `aggregateRevision` exactly once, and preserves artwork, receipts, and unknown new fields byte-for-byte. A writer that cannot guarantee this is fenced/rejected before mutation; disabling the UI feature never authorizes a lossy write.

UI cancellation/rollback restores presentation state only. It never writes an older aggregate, decrements `aggregateRevision`, removes artwork, or deletes an operation receipt.

New artwork references, event notification preferences, protected direct-invite batches/outcomes, approval records, and operation receipts participate in event cascade deletion and relevant account erasure. Deletion removes or anonymizes user-scoped rows according to their owner retention policy. Server-asset references are released/ref-counted transactionally; a shared asset blob is retained while referenced and deleted only by the asset owner's retention policy. Expired receipts and protected recipient keys follow bounded retention. No raw contact value, user-linked preference, unreferenced asset row, protected key, or other orphan PII may survive the owning event/account lifecycle.

## Error, offline, retry, and cancellation rules

Typed errors are `NETWORK_UNAVAILABLE | REPOSITORY_UNAVAILABLE | NOT_FOUND | FORBIDDEN | VALIDATION | CONFLICT | REMOTE_ARTWORK_UNAVAILABLE | PROVIDER_UNAVAILABLE | SERVER_UNAVAILABLE | PERMANENT_FAILURE`. Secure invitation errors remain owned by `harden-event-invitation-links`.

- Cached Library, Detail, Information, Audience, and Archive data may render as `STALE`; stale data never expands permissions.
- Event/artwork creation, direct invites, and event notification preference writes are local-first only where a repository contract exists; each pending write has an operation id and visible sync subject. Guest approval is online-only in this model and has no optimistic/offline state.
- `PENDING` is never labeled delivered, approved, joined, accepted, finalized on the server, or synced.
- Automatic sync retry follows `offline-sync`; explicit retry is offered only for typed retryable failure. Permanent/forbidden/validation/not-found errors do not loop.
- Conflicts expose a read-only warning and `RELOAD_PROJECTION` only. Resolution remains repository/sync-owner behavior; archive and Library never turn it into a mutation or primary action.
- Cancelling a load restores its captured `previousStableState` exactly. Cancelling a share sheet does not revoke or rotate a link. Cancelling an external provider leaves domain state unchanged.
- A late callback must match event id, entity id, operation id, and draft revision where applicable; otherwise it is ignored.
- Terminal states are Studio session `COMPLETED/CLOSED`, direct batch operation `COMPLETED/CANCELLED`, persisted approval `APPROVED/REJECTED`, lifecycle `FINALIZED`, and all terminal invitation-link states defined by `harden-event-invitation-links`. Load cancellation and pre/post-commit failures are not terminal.

## Release boundary

### Release 1: in scope for `expand-ios-invitation-experience`

- Repository-backed Library projections and deterministic next action.
- Creation Studio `NEW` and guarded `EDIT_EXISTING` sessions, preview plus atomic event/artwork/revision/operation persistence, total legacy migration, and capability-gated selection of existing owner-authorized server assets.
- Existing direct-invite owner capability surfaced in canonical `DRAFT` only, with orthogonal delivery/approval/membership/RSVP/date-validation axes.
- Existing invitation canvas routed to real Poll, Participants, and Organization flows.
- Event Information snapshot, three-axis notification projection, event-scoped preference repository contract, validated external destinations, operation-specific pending/failure state, and existing guarded leave/remove/delete capabilities where their use cases exist.
- Mixed-version writer fencing plus cascade-delete/account-erasure coverage for all new persistence rows.
- Global `PAST`/`FINALIZED` read-only routing and archive override; the `PAST` change is intentional breaking behavior.

### Dependency on `harden-event-invitation-links`

- Public-link issuance, bound in-memory payload sharing, Universal Link open, authentication continuation, inspect/redeem, rotation/revocation, and their retries remain unavailable/hidden until that change provides its typed capability.
- Guest approval is `NOT_SUPPORTED` in release 1 unless a reviewed online backend policy/request contract is delivered with the secure invitation dependency. The client never simulates approval or membership.

### Explicitly out of scope

- Device photo picker, local file selection/URL, media upload, and creation/import of an asset from a device or arbitrary external URL; these are deferred beyond release 1.
- On these six surfaces: local invitation tokens, locally constructed invitation URLs, client-generated QR codes, and offline public-link issuance. Global legacy cutover remains owned by `harden-event-invitation-links`.
- A new `CANCELLED` event lifecycle, ownership transfer, public event discovery, social feed, generic chat/tasks/workspace behavior, or Wallet/payment/pass semantics.
- LLM-selected artwork, role, permission, status, next action, approval, or transition. An AI worker may return reviewable content only; typed models decide.

## Invariants

1. `EventManagementStateMachine` is the sole owner of `EventStatus`; this experience never writes it directly.
2. Secure invitation issuance/open/redeem is owned by `harden-event-invitation-links`; iOS passes only a context-bound server-issued opaque payload and never logs or persists it.
3. Repository snapshots are the source of all lists, counts, roles, preferences, artwork, and sync decorations.
4. Each card/canvas exposes at most one next action, selected from typed capability and state.
5. `PAST` and `FINALIZED` globally force `READ_ONLY`, `VIEW_ARCHIVE`, and archive-safe deep-link resolution before any surface-specific action.
6. Archive is always read-only; sync conflict/failure never replaces its action or enables mutation and provides warning/reload only.
7. Preview of the current draft revision is mandatory before new or edited commit; stale previews cannot be committed.
8. Persisted artwork is exactly `NONE | STRUCTURED(version, ref) | LEGACY_REMOTE(validatedURL)`; release 1 remote selection references only an existing owner-authorized `SERVER_ASSET`, never a local file/upload.
9. Participant counts and identities come from one snapshot; delivery, approval, membership, RSVP, and date validation are total separate axes with explicit `NOT_APPLICABLE`/`UNAVAILABLE` and no implicit propagation.
10. Offline/pending/stale/failed states are visible and never represented as server success.
11. Missing access, route, provider, repository, or secure-share capability fails closed.
12. Permissions and transitions never depend on free text, localization, color, image analysis, generated content, or LLM output.
13. `UpdateDraftAggregate` revalidates organizer, `DRAFT`, and base revision inside the owner transaction; `EDIT_EXISTING` cancellation never deletes or rolls back the event, and post-commit retry replays sync only.
14. Event notification preference, account preference, and OS authorization remain separate; `UNAVAILABLE`, OS denial, restriction, and concrete `NOT_DETERMINED` are never bypassed. The sheet mutates/prompts neither account nor system, and only a typed OS-port callback may replace `UNAVAILABLE` with a concrete authorization snapshot.
15. Information pending/failure callbacks match subject, action, target, and operation id; load cancellation restores the exact `previousStableState`.
16. Destructive actions require explicit confirmation and an existing guarded use case; cancellation is effect-free before commit.
17. External Calendar, Maps, and Weather destinations cannot mutate Wakeve lifecycle state.
18. Direct-invite outcome/receipt keys are protected pseudonymous identifiers; raw contact inputs remain transient and obey retention/account erasure.
19. Every mixed-version/feature-flag writer preserves artwork, aggregate revision, and receipts or is fenced before writing; UI rollback never becomes a data rollback.
20. Every new persistence row cascades or is erased/anonymized with its event/account, and server-asset references obey owner retention without orphan PII.
21. No surface duplicates a transition already owned by another state machine or repository use case.

## Local normative references

- `models/ios-event-detail-invitation-canvas.md`
- `models/ios-event-detail-invitation-canvas.review.md`
- `openspec/changes/harden-event-invitation-links/`
- `openspec/specs/workflow-coordination/spec.md`
- `openspec/specs/event-organization/spec.md`
- `openspec/specs/collaboration-management/spec.md`
- `openspec/specs/offline-sync/spec.md`
- `openspec/specs/notification-management/spec.md`
- `openspec/specs/calendar-management/spec.md`
- `openspec/specs/product-excellence/spec.md`
