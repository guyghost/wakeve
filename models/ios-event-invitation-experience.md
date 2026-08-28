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
| Poll ballot editing and submission | `models/poll-ballot-submission.machine.ts` plus the poll repository transaction | Supply typed vote events and injected clock snapshots; never write slot votes one by one |
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
- `clockPort`: injected monotonic interaction clock whose snapshots are offset-qualified ISO instants. Views and models never call a hard-coded date or infer time from display text; the repository transaction uses its own injected authoritative clock before mutation.
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

### Poll voting and atomic ballot submission

`models/poll-ballot-submission.machine.ts` is the normative session machine. Its pure core validates time and ballot completeness; its shell only orchestrates the injected clock, the single repository command, cancellation, receipt observation, and sync retry.

`actorCanVote` is derived only from typed repository access axes, never from a UI boolean or role/RSVP text. Before policy evaluation, `actorId`, `eventOrganizerId`, and `accessUserId` must each be non-blank and already trimmed; failure on any one of the three is `DENIED(IDENTITY_MISMATCH)`, even when blank or padded strings happen to compare equal. `ParticipantAccessMapper` supplies `role` and `rsvp`; persisted membership supplies `ACTIVE_MEMBER | NON_MEMBER | LEFT | REMOVED | UNAVAILABLE`. The exact policy is:

- `ORGANIZER` is allowed when `access.userId == actorId == event.organizerId`, regardless of whether the primary canvas action currently prefers poll results.
- `MEMBER` is allowed only when `access.userId == actorId`, membership is `ACTIVE_MEMBER`, and RSVP is `ACCEPTED`—the same effective participant eligibility used by the Library `SUBMIT_VOTE` projection.
- pending, declined, not-applicable/unavailable RSVP; inactive/unavailable membership; non-member; foreign identity; or unknown access fails closed. Retained-date validation does not gate `POLLING`, because no retained date exists yet.

The repository transaction recomputes this decision from current records; a previously allowed client projection is not authorization.

`canMutateVote(deadlineIso, nowIso)` is true **if and only if** both values are strict, offset-qualified ISO instants and `now < deadline`. Strict parsing validates the calendar components before conversion, so normalized-but-invalid values such as February 29 in a non-leap year, April 31, `24:00`, invalid offset ranges, missing offsets, and trailing data are rejected. Therefore `now == deadline` and `now > deadline` are both closed. An invalid/missing deadline or clock snapshot fails closed.

`REQUEST_SET_VOTE(clockRequestId, slotId, choice)` deliberately carries no time. It enters `checkingVoteClock` and emits `requestClockSnapshot(clockRequestId)`; only a matching `VOTE_CLOCK_SNAPSHOT(clockRequestId, nowIso)` can apply the edit. A stale/foreign snapshot is inert. `REQUEST_SUBMIT(operationId)` follows the equivalent correlated submission-clock path. No 2025/other fixture value, device-local date parsing, UI string, event-supplied `now`, or direct system-clock read may decide mutability.

A `CompleteBallot` contains exactly one `YES | MAYBE | NO` entry for every slot in the current repository poll revision. Its slot-id set MUST equal the repository slot-id set: missing, duplicate, unknown, invalid-Unicode-scalar id, stale-revision, or invalid-choice entries are rejected before any write. Array order is non-semantic. `pollRevision` uses the interoperable numeric subset `Number.isSafeInteger(value) && value >= 0`; negative, fractional, non-finite, and integers above `2^53 - 1` fail closed before key construction or persistence.

The fingerprint is the cross-platform `v1` contract implemented identically in TypeScript and Kotlin: encode each valid slot id as UTF-8 bytes, sort by unsigned UTF-8 byte order, render the id as lowercase UTF-8 hex, append `=<YES|MAYBE|NO>`, and join segments after `v1` with `|`. Locale collation, URL encoding, JSON object order, platform hash codes, and Unicode normalization are forbidden. Canonically equivalent but byte-distinct Unicode ids remain distinct repository ids. Golden vectors are normative:

- `a/b?c=d&x=YES`, `space slot=NO` → `v1|612f623f633d642678=YES|737061636520736c6f74=NO`
- `é=MAYBE`, `東京=YES`, `🌍=NO` → `v1|c3a9=MAYBE|e69db1e4baac=YES|f09f8c8d=NO`

The only write is `CommitCompleteBallot(eventId, actorId, pollRevision, canonicalEntries, operationId)`. In one all-or-nothing transaction, the repository MUST:

1. re-read the event, participant access, status, current poll revision, deadline, and complete slot set;
2. read `now` from its injected authoritative clock and require valid ISO plus `now < deadline`;
3. revalidate voter permission, `status == POLLING`, exact revision, and exact slot coverage;
4. write all slot choices and one operation receipt, then commit; or roll everything back.

No per-slot repository loop is a valid implementation. The idempotency identity is the full tuple `(eventId, actorId, pollRevision, operationId)`, encoded with length-prefixed UTF-8-hex string fields. Before key construction, every string id MUST be non-blank, untrimmed-byte-stable, and a valid Unicode-scalar string (no unpaired surrogate); revision MUST be a non-negative safe integer. Invalid identity fails closed and no operation key is produced. The durable store has a unique constraint over those four logical fields—not `operationId` alone. Lookup and receipt/ballot insertion occur inside the same transaction. The same `operationId` for another event, actor, or poll revision is a different tuple and is allowed. For one exact tuple, replay of the same canonical payload returns the same receipt without rewriting; only reuse of that same tuple with a different payload is `IDEMPOTENCY_CONFLICT`. If concurrent writers race on the unique constraint, the loser re-reads the winning row transactionally: identical canonical command returns `RETURN_EXISTING_RECEIPT`, different payload returns `IDEMPOTENCY_CONFLICT`; it never blindly retries insertion. A missing row after a reported collision is `REPOSITORY_INCONSISTENT`.

After the correlated clock check, the machine enters explicit `persistingCommand` and emits only `persistPendingCommand`. The durable journal is monotone: `STAGED_NOT_DISPATCHED -> DISPATCHED | CANCELLED`, then only a proven-no-commit cancellation may advance `DISPATCHED -> DISPATCH_CANCELLATION_TOMBSTONED`; neither cancellation status can move backward. The owner first responds with correlated `COMMAND_JOURNALED(operationKey, fingerprint)` or typed `COMMAND_JOURNAL_FAILED(operationKey, fingerprint, error)`. A matching stage ACK enters `authorizingDispatch` and requests the durable `DISPATCHED` transition. That compare-and-set has three correlated outcomes: `COMMAND_DISPATCH_MARKED`, typed known `COMMAND_DISPATCH_MARK_FAILED`, or `COMMAND_DISPATCH_MARK_UNKNOWN`; the unknown path reads the durable journal status before deciding. `dispatchAtomicBallotCommand` is emitted only after a correlated `DISPATCHED` acknowledgement or resolution. The cancellation compare-and-set likewise has correlated success, known failure, and unknown outcomes; unknown resolves the durable row and only the appropriate durable cancellation status is success. Foreign/mismatched replies and late ACKs are inert. A retryable journal/repository error exposes only its typed retry path; a non-retryable error enters terminal `terminalFailure` or `resolutionFailed` and can never dispatch.

`resolvingJournalStatus` accepts only correlation-matching status/read outcomes. `COMMAND_JOURNAL_STATUS_READ_FAILED`, `COMMAND_JOURNAL_STATUS_MISSING`, and `COMMAND_JOURNAL_STATUS_MALFORMED` are explicit outcomes, never absence/timeouts inferred by the view. Each resolution read increments `journalResolutionAttempt`; `maxJournalResolutionAttempts` is a positive safe-integer bound. A matching retryable problem exposes retry only while the next attempt remains within that bound. A matching `READ_FAILED(retryable=false)` outside tombstone persistence enters `terminalFailure(REPOSITORY_INCONSISTENT, retryable=false, UNKNOWN)` immediately; exhausted retryable read/missing/malformed resolution enters terminal `resolutionFailed` with the same typed inconsistency. During tombstone persistence, read failure/exhaustion remains a visible nonterminal tombstone failure because no final/cancel state may be claimed without the tombstone. Foreign outcomes are inert, free text cannot retry, and missing/malformed data can never be interpreted as `DISPATCHED` or cancellation success.

A known-not-committed retry keeps the operation/payload and obtains a fresh clock snapshot. If its journal is already `DISPATCHED`, the accepted retry redispatches that exact command directly: it neither restages the journal nor clears/re-writes its status. An unknown-outcome retry loads the exact journaled envelope, not transient view state, and replays it to resolve its receipt even if the deadline has since elapsed; it never becomes a new ballot mutation. While any `DISPATCHED`/unknown command exists, `REQUEST_SUBMIT` for a new operation id is forbidden in every such state; only `RETRY_RESOLUTION` may replay the original operation id and byte-identical payload, so a session never owns two active ballot commands. The durable envelope includes the authoritative deadline observed by the repository command. On view/process recreation, `REHYDRATE_UNKNOWN_OUTCOME(command, journalStatus, typedFailure)` validates the durable envelope against its own version, safe identity tuple, canonical entries, fingerprint, and strict authoritative deadline, and requires its `eventId` and `actorId` to equal the current session. This validation deliberately ignores the recreated view's current slot projection, deadline, and poll revision: those may have advanced and cannot corrupt historical resolution. The journal snapshot is a total discriminated union: `STAGED_NOT_DISPATCHED`, `DISPATCHED`, and `CANCELLED` require `terminalDestination == null`; `DISPATCH_CANCELLATION_TOMBSTONED` requires exactly one valid typed destination. Every divergent status/destination pair, including `DISPATCHED + REVISED`, enters terminal `resolutionFailed(REPOSITORY_INCONSISTENT, retryable=false, UNKNOWN)` and never remains silently in `editing`. Only `DISPATCHED` may restore `outcomeUnknown`; `STAGED_NOT_DISPATCHED` is durably cancelled without dispatch, and `CANCELLED` restores terminal cancellation. A foreign/malformed envelope becomes typed terminal `REPOSITORY_INCONSISTENT`. A valid `DISPATCHED` retryable `UNKNOWN` restores `outcomeUnknown`; `RETRY_RESOLUTION` redispatches the unchanged envelope. A valid `UNKNOWN` with `retryable == false` enters terminal `resolutionFailed` and ignores retry/dispatch events. A post-commit sync retry uses the receipt only and never calls `CommitCompleteBallot` again.

Before a journal exists, `CANCEL` is effect-free and terminal. During `persistingCommand` or `authorizingDispatch`, `CANCEL`, PollVoting `CLOSE`, and PollVoting `BACK` enter `cancellingJournal` and race a correlated durable `CANCELLED` compare-and-set against dispatch authorization. A late stage ACK after cancellation is inert; if `CANCELLED` wins, recreation cannot dispatch that command. If the durable `DISPATCHED` compare-and-set had already won, its correlated ACK moves to ordinary post-dispatch cancellation and the owner must deliver/resolve that already-authorized command rather than lose it. Once `journalStatus == DISPATCHED`, `CLOSE` and `BACK` are cancellation requests rather than presentation exits: the surface remains active until a matching receipt wins or correlated proof of non-commit is followed by a durable typed tombstone. After actual dispatch, cancellation enters `cancelling`; repository proof of non-commit moves first to `tombstoningDispatchedCancellation`, never directly to terminal cancellation. A terminal known `NOT_COMMITTED` submission failure, `CANCEL`, `CLOSE`, `BACK`, or `REVISE_BALLOT` from `failedBeforeCommit` with retained `DISPATCHED` status uses the same tombstone gate. The durable tombstone carries exactly one `terminalDestination = CANCELLED | TERMINAL_FAILURE(code, commitOutcome) | REVISED`; it is not inferred from transient view state or a generic failure parameter. Only a correlated tombstone ACK or bounded journal resolution whose status and full destination exactly match may enter `cancelled`, `terminalFailure`, or reset to `editing`. Rehydration follows the stored destination exactly: `CANCELLED` cancels, `TERMINAL_FAILURE` restores its stored code/outcome as non-retryable terminal failure, and `REVISED` returns to editing without redispatching the old command. An absent, unreadable, malformed, merely `DISPATCHED`, or destination-divergent row is not cancellation/terminal/revision success. CAS or tombstone failure preserves the command, journal status, destination intent, and operation correlation; it never clears them or exits. Tombstone persistence failure remains an explicit nonterminal blocked/retryable state rather than bypassing durability. A racing matching receipt before proof wins and transitions to committed. Permission denial, status/revision change, invalid time, validation failure, repository inconsistency, and transaction failure cannot produce success or navigation.

Status resolution is intent-exact. Pre-dispatch cancellation accepts `COMMAND_JOURNAL_STATUS_RESOLVED(CANCELLED, destination=null)` only when `journalIntent == CANCEL`. The guard named `resolvedCancelled` is reserved for the post-dispatch path and accepts only `journalIntent == TOMBSTONE_DISPATCHED_CANCELLATION`, in-memory destination `CANCELLED`, and durable `(status=DISPATCH_CANCELLATION_TOMBSTONED, destination=CANCELLED)`. `TERMINAL_FAILURE(code,outcome)` and `REVISED` similarly require byte-for-byte matching durable destinations before their distinct transitions. Tombstone consumption has a total projection: `CANCELLED -> cancelled + RETURN_TO_EVENT/onBack exactly once`, `REVISED -> editing + no navigation`, and `TERMINAL_FAILURE -> terminalFailure(stored code/outcome) + no navigation`. The executable effect for the first branch is `returnToEventOnBack`; because `cancelled` is terminal, replaying the rehydration event cannot emit it again. Rehydration uses this same projection; no generic tombstone completion may choose navigation. `CANCELLED` observed while authorizing dispatch, a tombstone with a different destination, or either status under a foreign intent is inert and cannot manufacture cancellation, failure, or revision.

When vote mutation is closed, every `YES | MAYBE | NO` choice remains visible for context but projects `isEnabled=false`, semantic state `DISABLED`, VoiceOver state `DISABLED`, and reason `POLL_CLOSED`. Color, opacity, ignored taps, or an overlay without disabled accessibility semantics is insufficient.

A matching local receipt makes navigation eligible exactly once but is worded honestly as `LOCAL_PENDING`, never “synchronized” or “sent”. It carries a real `BallotSyncPayload(schemaVersion, localReceiptId, commandEnvelope)`; `localReceiptId` must equal the containing receipt id and the complete versioned command contains the full tuple, canonical entries, fingerprint, and authoritative deadline required by the sync owner. Receipt classification first correlates the outer `operationKey`, operation id, event, actor, poll revision, and fingerprint to the current command. If those outer fields identify the current command, any malformed or divergent inner `syncPayload/command`—including an inner second operation—is terminal `REPOSITORY_INCONSISTENT(retryable=false, commitOutcome=UNKNOWN)`, never `STALE`. `STALE` is reserved for a genuinely outer-foreign receipt that is itself structurally valid and internally coherent. Receipt acceptance then re-correlates the complete sync command, accepted instant, local sync status, and absent server receipt. It always validates the receipt against the durable command deadline and requires `acceptedAt < authoritativeDeadline`; it never substitutes the recreated view's current deadline or skips the deadline check because a transient clock value is absent. `LOCAL_COMMIT` is accepted only with `syncStatus == LOCAL_PENDING` and no `serverReceiptId`; it can never manufacture `SYNCED`. `SYNC_FAILED(error: BallotSyncError)` retains this payload; `RETRY_SYNC` is accepted only for typed `retryable == true` and resubmits this sync payload—not the local ballot command. Only a matching backend ACK with a non-blank `serverReceiptId` changes the retained receipt to `SYNCED` before entering a genuinely done final state. ACK matching is a total fail-closed predicate: malformed/blank ids or an invalid/fractional revision return false and leave `LOCAL_PENDING`; they never throw. Local pending, typed sync failure, retrying sync, and acknowledged sync never roll back or duplicate the local ballot.

The server applies `BallotSyncPayload` through the same complete-ballot atomicity contract: one transaction validates the full idempotency tuple, schema/fingerprint, current compatible poll revision, effective vote access, and exact complete slot set, then writes all server ballot entries plus its receipt or writes none. Sync table `poll_ballot` with operation `UPDATE` is the sole mutation authority. Every server sync mutation targeting legacy table `votes`—create, update, or delete—is rejected as non-retryable `LEGACY_VOTES_MUTATION_FORBIDDEN`; compatibility reads may exist but never grant write authority. The server returns only a correlated `BallotServerAck(localReceiptId, serverReceiptId, fullIdentityTuple, ballotFingerprint, APPLIED | ALREADY_APPLIED)`. The client may mark `SYNCED` only when every correlation field matches. A generic HTTP/batch success, queue acceptance, partial per-slot result, stale revision, access rejection, payload conflict, or idempotency conflict is not an acknowledgement; it leaves the local receipt `LOCAL_PENDING` or enters typed `SYNC_FAILED` for explicit policy-driven retry/reconciliation.

Pending journal/sync metadata is projected one-for-one as `VALID(metadata) | INCONSISTENT(REPOSITORY_INCONSISTENT, retryable=false, commitOutcome=UNKNOWN, metadata)`. Projection is a total boundary: it prechecks every identifier, revision, fingerprint, journal status, and operation key, and protects canonical-key construction; corrupt repository data returns `INCONSISTENT` and never throws. The executable metadata-to-receipt join additionally exposes `RECEIPT_MISSING | PAYLOAD_EMPTY | PAYLOAD_MALFORMED | RECEIPT_ID_DIVERGENT | TUPLE_DIVERGENT | FINGERPRINT_DIVERGENT | OPERATION_KEY_DIVERGENT | RECEIPT_NOT_LOCAL_PENDING | RECEIPT_ALREADY_ACKNOWLEDGED`. Every metadata row yields exactly one projection. Only a fully correlated receipt with `syncStatus == LOCAL_PENDING` and `serverReceiptId == null` contributes a payload to the dispatcher. A `SYNCED` receipt or any receipt already carrying a server id remains visible as inconsistent and is never resubmitted. Invalid keys, identities, revisions, fingerprints, slot mappings, receipts, empty payloads, and decode failures remain visible for reconciliation/diagnostics; no `filterNotNull`, `mapNotNull`, catch-and-drop, or empty-list fallback may hide them.

### Time-slot storage identity

`models/time-slot-storage-identity.model.ts` is normative for the boundary between domain slot ids and storage ids. A logical identity is `(eventId, logicalSlotId)`. Its physical id is the injective, locale-free `slot:v1` encoding of both length-prefixed UTF-8-hex fields; the event namespace is mandatory even when two events reuse the same logical slot id.

One validated index owns both directions. Confirmation resolves `(eventId, logicalSlotId) -> physicalSlotId`; ballot sync and stored receipts resolve `physicalSlotId -> (eventId, logicalSlotId)`. Both paths use the same index and require exact round-trip identity. Non-deterministic physical ids, duplicate logical mappings, physical collisions, and missing mappings produce explicit `INCONSISTENT` outcomes before confirmation/sync. Batch and pending projections preserve one result per input, including failures; they never overwrite a map entry or silently lose a slot.

Legacy and mixed physical ids are upgraded only by `migrateSlotStorageIdentitiesAtomically`. One transaction first proves an injective source-to-`slot:v1` mapping, then rewrites all slot rows and every `VOTE | RECEIPT | SYNC_METADATA` foreign reference. Any invalid id, duplicate source, target collision, or missing reference returns `ROLLED_BACK` with the original rows/references; partial rewrites are forbidden. Confirmation lookup, receipt projection, and sync consumers are gated off until the durable result is exactly `MIGRATION_COMPLETE`.

### Creation Studio

`StudioMode` is total: `NEW | EDIT_EXISTING(eventId, baseRevision)`. `EventManagementStateMachine` plus its repository transaction is the **single persistence owner**. For `NEW`, iOS dispatches exactly one typed `CreateEvent(eventDraft, artwork, operationId)` command and observes its result; neither `CreateEventViewModel`, `ContentView`, completion callbacks, nor navigation code may call a second `save/create/upsert`. For edit, the typed owner callback `UpdateDraftAggregate(eventId, actorId, expectedBaseRevision, eventDraft, artwork, operationId)` MUST re-read and revalidate organizer identity, `status == DRAFT`, and exact `baseRevision` inside the owner transaction. Release 1 exposes edit only for `DRAFT`. `CreationDraft` is transient and carries a separate monotonically increasing `draftRevision`.

`StudioPreviewSnapshot(draftRevision)` is an immutable projection of the exact current draft revision. Every leaf is copied from its typed scalar source: `title`, `descriptionText <- draft.description`, `eventType`, `eventTypeCustom`, participant bounds, structured location fields, structured slot instants/time-of-day, and artwork choice. The preview may localize labels and format a copied typed instant for display, but it MUST NOT use `String(describing:)`, reflective/debug output, an aggregate `fields.description`, or any technical object representation as user content. Changing any draft leaf invalidates the snapshot and requires a new preview.

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

The commit gate is normatively executable in `models/studio-commit-gate.model.ts`. Its durable `StudioCommitEnvelope` is `(identity(operationId, draftRevision), requestPayload, expectedResultingArtwork, durableOperationRef, requestFingerprint, maxResolutionAttempts)`. The request payload contains canonical scalar draft JSON, the discriminated artwork choice/reference, and the same `expectedResultingArtwork`. For a concrete replacement choice, the expected result equals that choice. For `KEEP_EXISTING`, `deriveStudioExpectedResultingArtwork` copies the exact non-`KEEP_EXISTING` artwork from the base-revision aggregate snapshot; absence of that snapshot/proof cannot build a valid envelope. `requestFingerprint` is the canonical UTF-8 fingerprint of that entire typed payload, expected result included; `durableOperationRef` is derived from the same payload fingerprint plus operation id. The persistence owner stores and reads this full envelope as one durable record. `projectStudioDurableEnvelope` recomputes both values and returns visible non-retryable `REPOSITORY_INCONSISTENT/UNKNOWN` for any missing or divergent field; neither value may be independently assembled from mutable Studio fields. Commit-gate state is `IDLE | COMMITTING(envelope) | FAILED_BEFORE_COMMIT(envelope, failure) | DETACHED_COMMITTING(envelope, resolutionAttempt, lastFailure?, lastAttempt?) | DETACHED_RESOLVING(envelope, resolutionAttempt, lastFailure, attemptId, fence) | PENDING_SYNC(envelope, localReceipt, navigationConsumed) | SYNC_FAILED(envelope, localReceipt, retryableFailureMetadata, navigationConsumed) | SYNC_TERMINAL_FAILURE(envelope, localReceipt, terminalFailureMetadata, navigationConsumed) | DETACHED_PENDING_SYNC(envelope, localReceipt, navigationConsumed) | SYNCED(envelope, localReceipt, serverReceiptId, navigationConsumed) | DETACHED_RESOLUTION_FAILED(envelope, terminalFailure) | REHYDRATION_FAILED(REPOSITORY_INCONSISTENT) | CLOSED`. The broader Studio projection additionally owns `LOADING_EXISTING(previousStableStudioState) | EDITING(mode, baseRevision, draftRevision) | RESOLVING_ARTWORK(draftRevision) | PREVIEW_READY(draftRevision) | PREVIEWING(draftRevision) | PENDING_SYNC(eventId, committedRevision, operationId) | SYNC_FAILED(eventId, committedRevision, operationId, error) | COMPLETED(eventId, committedRevision)`. For `NEW`, `baseRevision` is the typed sentinel `NOT_APPLICABLE`; it is never null. `previousStableStudioState` is the exact prior `IDLE` or editing/preview state. Every successful local `StudioLocalCommitReceipt` contains non-null `commitEnvelope` and non-null `StudioPendingSyncSubject`; both repeat the exact expected resulting artwork and must be mutually correlated. `PENDING_SYNC`, `SYNC_FAILED`, `SYNC_TERMINAL_FAILURE`, `DETACHED_PENDING_SYNC`, and `SYNCED` all prove a successful local commit and can never represent `UNKNOWN`; entering either pending-sync state emits idempotent `TRIGGER_SYNC_IMMEDIATELY(subject)` after the local receipt is durable, because merely observing pending work is insufficient. Only their remote-sync/navigation axes differ. `DETACHED_PENDING_SYNC` is settled and non-navigating by default but retains one explicit user-driven navigation-consumption transition; `SYNC_TERMINAL_FAILURE`, `DETACHED_RESOLUTION_FAILED`, `REHYDRATION_FAILED`, `SYNCED`, and `CLOSED` are terminal. Cancelling a load restores the captured state exactly.

`REHYDRATE_STUDIO_RECEIPT(session, record)` closes the crash window after the SQLite transaction and before the first sync effect. It is accepted only from `IDLE` and only for a complete `PENDING_SYNC` local receipt whose non-null `commitEnvelope`, non-null pending subject, event, actor, committed revision, durable operation reference, and request fingerprint all correlate exactly. Resolution uses the identical decoder and full correlation rule. `ATTACHED` restores `PENDING_SYNC`; `DETACHED` restores `DETACHED_PENDING_SYNC` with the receipt-owned binding loaded, `isStudioPendingSync == true`, and no committed-terminal projection. Neither branch emits callback or navigation, and both emit exactly one idempotent `TRIGGER_SYNC_IMMEDIATELY` observation for the receipt-owned subject. Replaying the rehydrate event after restoration is inert. Missing, null, malformed, foreign, or divergent data enters terminal `REHYDRATION_FAILED(REPOSITORY_INCONSISTENT, retryable=false, commitOutcome=UNKNOWN)`, blocks the editor, and never dispatches sync.

`StudioPreCommitFailure` is a closed type containing only failures for which the owner proves no transaction committed. `StudioResolutionFailure` is disjoint and contains commit-unknown, resolution-unknown, exhausted, permanent, and repository-inconsistent resolution outcomes. `COMMIT_OUTCOME_UNKNOWN` received while still attached in `COMMITTING` transfers directly to durable `DETACHED_COMMITTING`; it can never enter `FAILED_BEFORE_COMMIT`, `PENDING_SYNC`, or trigger another create/save. `PENDING_SYNC` requires a fully correlated local receipt and is positive proof of local commit. A resolution result first correlates its outer envelope to the current operation, then decodes and correlates the receipt's complete inner commit envelope and pending subject. Outer-current plus missing/malformed/divergent inner envelope enters terminal `DETACHED_RESOLUTION_FAILED(REPOSITORY_INCONSISTENT, retryable=false, commitOutcome=UNKNOWN)` directly; it can never pass through or return to `DETACHED_COMMITTING`. A genuinely undecidable but structurally valid outcome is separately typed `RESOLUTION_OUTCOME_UNKNOWN(retryable=true)` and alone may return to `DETACHED_COMMITTING` within budget. `resolutionAttempt` starts at zero, `maxResolutionAttempts` is a positive safe-integer owner-supplied bound, and retry eligibility requires byte-for-byte correlation of identity, `requestPayload`, `durableOperationRef`, `requestFingerprint`, attempt bound, `retryable == true`, and `resolutionAttempt < maxResolutionAttempts`. Each accepted retry enters `DETACHED_RESOLVING` with one non-blank, never-reused `attemptId` and the exact fence `resolutionAttempt + 1`; the accepted fence becomes the new attempt count. A correlated resolution-unknown result returns to `DETACHED_COMMITTING` while retaining `lastAttempt`, so neither that id nor its fence can be reused. While resolution is active, every further retry is inert; only a result matching the full envelope, `attemptId`, and fence may settle it or unlock the next strictly higher fence. Late results from a completed fence are inert. The bound and fence are deterministic; free text cannot enable another attempt.

| Event | Guard | State/effect |
|---|---|---|
| `LOAD_EXISTING` / `EXISTING_LOADED` | matching event id/repository revision and edit capability | Load persisted event/artwork into `EDITING(EDIT_EXISTING, baseRevision, 0)` |
| `UPDATE_FIELD` / `UPDATE_ARTWORK` | interactive Studio | Increment `draftRevision`; never change `baseRevision` |
| `REQUEST_PREVIEW` / `OPEN_PREVIEW` | fields valid; preset or selected server asset is capability-authorized | Resolve artwork, then preview the exact `draftRevision` |
| `CONFIRM_COMMIT(envelope)` | preview revision current; typed owner callback installed; operation ref and request fingerprint recompute from the same request payload | Enter `COMMITTING(envelope)` and emit exactly one typed `PERSIST_DURABLE_ENVELOPE_THEN_INVOKE(envelope)` owner effect. The owner durably stores the complete envelope before invoking that exact `CreateEvent`/`UpdateDraftAggregate`; it revalidates actor, `DRAFT`, and base revision |
| duplicate `CONFIRM_COMMIT(envelope)` while `COMMITTING` | same Studio session has an in-flight envelope | Coalesce/ignore an exactly identical envelope; reject a different identity, durable operation reference, or fingerprint as typed `COMMIT_ALREADY_IN_FLIGHT`. Never call persistence or completion callback again |
| `LOCAL_COMMIT` | full durable envelope matches and receipt contains the matching event/revision plus complete `StudioPendingSyncSubject` | Observe atomically stored event, total artwork, incremented event revision, and operation receipt; emit navigation exactly once, enter `PENDING_SYNC`, then emit idempotent `TRIGGER_SYNC_IMMEDIATELY(subject)`. A present malformed/divergent receipt becomes non-retryable `REPOSITORY_INCONSISTENT`, never success |
| `FAIL_BEFORE_COMMIT(identity, failure)` | matching envelope and owner proves no repository transaction committed | `FAILED_BEFORE_COMMIT(envelope, failure)`; retain the exact durable operation reference and fingerprint, never reconstruct from the mutable draft |
| `OUTCOME_UNKNOWN(identity, COMMIT_OUTCOME_UNKNOWN)` from attached `COMMITTING` | matching envelope; no proof of commit or non-commit | Transfer to `DETACHED_COMMITTING` with the exact envelope and typed resolution failure (or terminal detached resolution failure when non-retryable); never misclassify as `FAILED_BEFORE_COMMIT`, re-invoke persistence, deliver callback, or navigate |
| `SYNC_FAILED(metadata)` | local transaction already committed; receipt id, event, revision, operation ref and fingerprint match; status agrees with typed retryability | Retryable transport/server availability records `RETRYABLE_FAILURE` in `SYNC_FAILED`; `FORBIDDEN`, `EVENT_NOT_DRAFT`, `STALE_BASE_REVISION`, idempotency/repository inconsistency, or permanent rejection records `TERMINAL_FAILURE` and enters terminal `SYNC_TERMINAL_FAILURE`. Generic conflict text/result is inert |
| `RETRY_BEFORE_COMMIT(identity)` | matching envelope and typed retryable pre-commit error | Re-enter `COMMITTING` and invoke the exact durable command with the same operation id/reference/fingerprint; never allocate or rebuild |
| `RETRY_SYNC` | matching post-commit receipt and typed retryable sync error | Replay the typed `StudioPendingSyncSubject` through the repository sync owner only; never call `CreateEvent`, `UpdateDraftAggregate`, or manual SQL |
| `SYNC_ACK` | local/server receipt ids non-blank and event, committed revision, operation ref, request fingerprint, disposition, exact `expectedResultingArtwork`, and applied outcome all match | `PENDING_SYNC | SYNC_FAILED | DETACHED_PENDING_SYNC -> SYNCED`; substituted artwork—including under `KEEP_EXISTING`—stays unsynced, and generic HTTP/batch/queue success or conflict is inert |
| `CANCEL_LOAD` | captured `previousStableStudioState` exists | Restore it exactly; not terminal |
| `CLOSE` before `COMMITTING` | no persistence command is in flight | For `NEW`, discard only transient draft; for `EDIT_EXISTING`, leave the existing event unchanged |
| `CLOSE` during `COMMITTING(envelope)` | matching operation is already in flight | Enter `DETACHED_COMMITTING(envelope, 0, none)`; do not cancel, report failure, navigate, or discard the durable operation reference. The repository/receipt owner resolves the outcome and preserves any eventual local commit |
| `CLOSE` during `DETACHED_COMMITTING` or `DETACHED_RESOLVING` | presentation is already detached and durable work remains | Consume explicitly as `CONSUME_CLOSE_WITHOUT_CANCELLATION`; preserve state, envelope, active attempt/fence, and failure exactly; emit no persistence, retry, cancellation, callback, or navigation |
| `CLOSE` after observing commit | matching receipt exists | Close presentation only; keep repository truth and sync work |
| `LATE_LOCAL_COMMIT(receipt)` from detached resolution | receipt matches the full durable envelope and includes the complete pending sync subject | Enter settled `DETACHED_PENDING_SYNC(envelope, receipt, UNCONSUMED)` and dispatch only its typed sync subject; emit no navigation, success presentation, or creation command |
| `PROVEN_NON_COMMIT(identity, proof)` from `DETACHED_COMMITTING` | owner proof is non-blank after trimming, matches the operation, and establishes transaction rollback/no receipt | Enter terminal `CLOSED`; preserve no fictitious success and emit no navigation |
| `OUTCOME_UNKNOWN(identity, failure)` from `DETACHED_COMMITTING` | matching envelope; failure is typed `RETRYABLE`; retry budget remains | Stay `DETACHED_COMMITTING`, retain the exact durable envelope, failure, and attempt count; do not navigate or retry implicitly |
| `RETRY_RESOLUTION(identity, attemptId, fence)` from `DETACHED_COMMITTING` | matching envelope, retryable typed failure, attempt below bound, new `attemptId`, and `fence == resolutionAttempt + 1` | Enter `DETACHED_RESOLVING`; set `resolutionAttempt = fence` and emit one idempotent outcome-resolution replay by `durableOperationRef`. Same id/fence reuse and any skipped/regressed fence are inert |
| `RESOLUTION_RESULT(envelope, attemptId, fence, outcome)` from `DETACHED_RESOLVING` | full durable envelope and exact active attempt/fence match; local receipt is complete or `PROVEN_NON_COMMIT.proofRef` is non-blank after trimming | `LOCAL_COMMIT -> DETACHED_PENDING_SYNC`, `PROVEN_NON_COMMIT -> CLOSED`, retryable `UNKNOWN -> DETACHED_COMMITTING`; stale/foreign/blank-proof result is inert and cannot unlock another retry |
| non-retryable/exhausted correlated result, or `RESOLUTION_TERMINAL_FAILURE(identity, attemptId, fence, failure)` | exact active attempt and resolution can no longer prove commit/non-commit within policy | Enter terminal `DETACHED_RESOLUTION_FAILED(envelope, failure)`; retain sanitized diagnostic and operation identity for support/reconciliation; emit no navigation and never claim non-commit |

Stale load and artwork callbacks are ignored unless event id, base/event revision, draft revision, and capability revision match. Commit/resolution callbacks require the exact durable envelope; sync callbacks additionally require the full receipt/subject correlation. Offline local creation/edit may become `PENDING_SYNC`, but is never “shared” or “server confirmed.” A sync callback can only mutate the sync decoration attached to the committed receipt. `RETRY_SYNC` delegates that receipt-owned subject to the sync owner; it cannot re-enter `COMMITTING`, allocate another event id, re-run creation, issue manual SQL, or treat a generic batch success as acknowledgement.

`applyStudioServerCommit` is the executable server contract. Its inputs include the trusted `authenticatedActorId`; equality with `envelope.requestPayload.actorId` is checked before replay or mutation. Its durable idempotency key is exactly `(durableOperationRef, requestFingerprint)`. Receipt lookup, aggregate mutation, and final `StudioServerCommitReceipt(key, commitEnvelope, exactAck)` insertion occur under one transaction and one transactionally unique receipt key; there is no aggregate-only state and therefore no crash window between aggregate application and durable ACK. `NEW` assigns `organizerId = authenticatedActorId = envelope.actorId`. `EDIT_EXISTING` additionally requires `authenticatedActorId == aggregate.organizerId`, `aggregate.status == DRAFT`, and `baseRevision == currentAggregate.revision` inside that transaction. A forged actor is non-retryable `FORBIDDEN`; a non-draft edit is non-retryable `EVENT_NOT_DRAFT`; a stale base is non-retryable `STALE_BASE_REVISION`; each writes neither aggregate nor receipt. Before writing, the computed aggregate artwork must exactly equal the fingerprint-bound `expectedResultingArtwork`; this applies to every choice and proves the base snapshot under `KEEP_EXISTING`. For an authenticated exact replay, the server reads and validates the final receipt before every mutable aggregate guard and returns its ACK byte-for-byte with `aggregateMutation = null`, including the same outcome/disposition, `serverReceiptId`, resulting revision and exact expected artwork even if the event has subsequently changed. If two instances race after both initially observe no receipt, exactly one atomic insert wins; the loser's `finalizeStudioServerReceiptRace` re-reads the non-null winner receipt inside its transaction result/finalizer and returns that exact ACK only when its artwork also equals the expected result. It cannot return null or a generic conflict after the winner committed; malformed or artwork-substituted winner data is explicit `REPOSITORY_INCONSISTENT`. Reuse of that key with a divergent payload before a matching winner is established is non-retryable `IDEMPOTENCY_CONFLICT`.

Navigation consumption is explicit and idempotent: a local commit receipt is projected as `UNCONSUMED | CONSUMED` for the originating Studio operation. A foreground matching `LOCAL_COMMIT` may emit navigation once and atomically mark that observation consumed. Rehydrating/restoring a committed receipt has no navigation entry action. Navigation after restoration requires either explicit user event `OPEN_COMMITTED_EVENT` or a newly delivered matching `OBSERVE_UNCONSUMED_LOCAL_COMMIT` that consumes the marker; an already-consumed observation is inert. `DETACHED_COMMITTING` resolution never reopens or navigates the closed Studio automatically.

`COMMITTING` owns exactly one persistence invocation and exactly one completion callback delivery. Button taps, keyboard submission, accessibility activation, repeated SwiftUI actions, and late duplicate callbacks all pass through the same durable envelope gate; none can create a second save. `FAILED_BEFORE_COMMIT` and `DETACHED_COMMITTING` retain that envelope across retries, dismissal, and view recreation, so retry and outcome resolution address the original command rather than a newly projected draft.

Creation controls consume typed semantic affordances, never color/opacity alone. While `COMMITTING`, the create button is visible but `isEnabled=false`, semantic state `DISABLED`, VoiceOver state `DISABLED`, reason `IN_FLIGHT`; repeated activation cannot emit `CONFIRM_COMMIT`. Closed Studio choices, when present, remain visible for context but are likewise interaction-disabled and exposed to VoiceOver as disabled with reason `CLOSED_CHOICE`. An inaccessible overlay or ignored tap is not a valid disabled state.

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

`models/archive-navigation.machine.ts` is the explicit navigation axis: `PRESENTED(returnTarget: LIBRARY) | CLOSED`. `CLOSE` (close affordance) and `RETURN` (navigation back gesture/action) are the only exit events; either transitions directly to terminal `CLOSED` and emits exactly one typed `NavigateToLibrary` effect. There is no implicit dismissal, inferred tab selection, or text-driven transition. A visible, accessible close/back affordance MUST remain installed even when the tab bar is hidden. Archive content, loading failure, empty state, stale data, deep-link entry, and sync warning can never remove this exit or substitute a domain mutation. Because `CLOSED` is final/done, repeated or late exit events are inert.

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
- Complete ballots are local-first through one atomic transaction and receipt. A partial ballot is never a persisted or navigable success.
- `PENDING` is never labeled delivered, approved, joined, accepted, finalized on the server, or synced.
- Automatic sync retry follows `offline-sync`; explicit retry is offered only for typed retryable failure. Permanent/forbidden/validation/not-found errors do not loop.
- Conflicts expose a read-only warning and `RELOAD_PROJECTION` only. Resolution remains repository/sync-owner behavior; archive and Library never turn it into a mutation or primary action.
- Cancelling a load restores its captured `previousStableState` exactly. Cancelling a share sheet does not revoke or rotate a link. Cancelling an external provider leaves domain state unchanged.
- A late callback must match event id, entity id, operation id, and draft revision where applicable; otherwise it is ignored.
- Terminal states are ballot session `CANCELLED/DETACHED/SYNCED`, Studio session `COMPLETED/CLOSED`, archive navigation `CLOSED`, direct batch operation `COMPLETED/CANCELLED`, persisted approval `APPROVED/REJECTED`, lifecycle `FINALIZED`, and all terminal invitation-link states defined by `harden-event-invitation-links`. Load cancellation and pre/post-commit failures are not terminal.

## Release boundary

### Release 1: in scope for `expand-ios-invitation-experience`

- Repository-backed Library projections and deterministic next action.
- Strict injected-clock poll mutability and atomic, complete, idempotent ballot submission.
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
22. Vote mutation is allowed only for valid instants with `now < deadline`; the complete ballot and receipt commit in one transaction or not at all.
23. Creation has one persistence owner. A matching local commit is navigable exactly once; sync failure/retry never re-executes creation.
24. Studio preview copies the exact current draft scalars and never renders a technical/debug representation.
25. Archive remains read-only and always exposes explicit `CLOSE/RETURN` routing to the Library, independently of tab-bar visibility or load state.
26. Closing Studio while commit is in flight detaches presentation but never claims cancellation; outcome resolution remains receipt-driven and restoration never auto-navigates from consumed history.
27. Sync retry requires a typed retryable failure; acknowledgement updates the receipt and reaches a genuinely terminal machine state.
28. Every `DETACHED_COMMITTING` exit is explicit and operation-correlated: late commit preserves durable truth without navigation, proven non-commit closes, unknown outcome retries the same command only within typed bounds, and unresolved terminal failure never invents an outcome.
29. Poll vote access is a typed policy decision: the exact organizer is allowed; only an accepted active member is allowed; all missing, inactive, foreign, or unavailable access fails closed.
30. Ballot idempotency is scoped to the full four-field tuple, the fingerprint is the portable UTF-8/hex `v1` contract, and unknown-outcome restoration replays the durable envelope unchanged.
31. A ballot local receipt remains `LOCAL_PENDING` with a complete sync payload until typed ACK marks it `SYNCED`; sync retry never repeats the local transaction.
32. Creation Studio admits only one `CONFIRM_COMMIT` flight, one persistence save, and one completion callback for a session operation.
33. Ballot dispatch is impossible before a correlated `COMMAND_JOURNALED`; journal failure is known-not-committed and cannot leak a repository command.
34. Rehydrated unknown-outcome resolution validates historical durable truth independently of current view slots/revision; non-retryable unknown outcome is terminal and cannot redispatch.
35. Durable ballot uniqueness covers all four identity fields, and a concurrent collision is resolved only by transactional re-read of the winning row.
36. Studio pre-commit failure and detached resolution retain the exact durable command envelope; retries preserve its identity/reference/fingerprint and remain bounded.
37. Ballot rehydration correlates durable `eventId` and `actorId` to the session while deliberately ignoring current revision/slot projection.
38. The ballot journal is durably monotone `STAGED_NOT_DISPATCHED -> DISPATCHED | CANCELLED`; only `DISPATCHED` can resolve/retry and cancellation fences late ACKs across recreation.
39. `REPOSITORY_INCONSISTENT` is a closed, non-retryable terminal ballot failure and never dispatches or navigates.
40. Studio permits one detached-resolution attempt in flight; a matching `attemptId`/`fence` result is required before another retry.
41. Studio resolution fences are strictly consecutive and attempt ids are fresh; a completed attempt is retained so its retry or late result remains inert.
42. Server sync rejects every legacy `votes` mutation; only atomic `poll_ballot UPDATE` may mutate ballot authority.
43. Confirmation and sync share one deterministic event-scoped bidirectional slot identity index; collisions, missing mappings, and pending inconsistencies remain explicit and visible.
44. No ballot dispatch or retry occurs without durable `DISPATCHED`; non-retryable persistence/inconsistency failures are terminal.
45. Ballot receipts are accepted only after full receipt, operation-key, command, identity, fingerprint, timestamp, and sync-status correlation.
46. Studio consumes close during attached/detached in-flight work without cancellation, distinguishes commit-unknown from resolution-unknown, and exposes in-flight/closed controls as disabled to VoiceOver.
47. Studio durable operation reference and request fingerprint are recomputed from one complete persisted request payload; resolution correlates the full envelope rather than parallel mutable fields.
48. Studio `PENDING_SYNC` is local-commit proof carrying a dispatchable typed subject; only a fully correlated server ACK reaches terminal `SYNCED`, and generic batch success is insufficient.
49. PollVoting close/back after durable dispatch cannot exit until a matching receipt wins or correlated non-commit proof and typed tombstone settle; CAS/tombstone failures preserve the command.
50. Every present malformed or same-operation divergent ballot receipt is terminal non-retryable `REPOSITORY_INCONSISTENT/UNKNOWN`; valid foreign receipts alone are stale and inert.
51. Every persisted Studio local receipt immediately emits one idempotent typed sync trigger; merely observing `PENDING_SYNC` is not considered dispatch.
52. Outer-current Studio resolution validates the complete inner commit envelope and subject; malformed/divergent inner data is terminal non-retryable `REPOSITORY_INCONSISTENT/UNKNOWN`.
53. Studio server creation/edit writes aggregate artwork and idempotency receipt atomically; exact replay returns the same ACK, stale edit revision and changed same-key payload are non-retryable conflicts, and ACK revision is the actual result.
54. Ballot tombstone rehydration has one discriminated state/navigation projection: cancelled returns, revised edits in place, and terminal failure never navigates.
55. A dispatched unknown ballot command excludes every new submit; resolution retry preserves the sole active operation id and payload.
56. Studio pending-receipt rehydration closes the local-commit crash window: full receipt/envelope/session correlation restores attached or detached pending sync, emits one idempotent immediate trigger, and never navigates implicitly.
57. Studio server authorization is fail-closed: authenticated actor, envelope actor, and organizer are identical; edit additionally requires `DRAFT`, while rejection writes neither aggregate nor receipt.
58. A Studio local receipt is invalid unless `commitEnvelope` and `StudioPendingSyncSubject` are both present and fully identical on operation, payload, reference, fingerprint, event and revision; resolution/rehydration corruption blocks the editor as `REPOSITORY_INCONSISTENT/UNKNOWN`.
59. Studio server aggregate and final receipt/ACK are one transaction; exact replay reads that receipt before mutable event guards and reproduces disposition, receipt id, revision and artwork byte-for-byte.
60. Studio sync failure metadata is receipt-correlated and separates retryable transport failure from terminal forbidden, non-draft, stale-revision and permanent rejection; generic conflicts cannot leave or settle pending sync.
61. A rehydrated ballot cancellation tombstone emits `RETURN_TO_EVENT/onBack` exactly once; revised and terminal-failure destinations never emit navigation.
62. Studio inner-record corruption is terminal non-retryable `REPOSITORY_INCONSISTENT/UNKNOWN`; only a structurally valid typed outcome-unknown may re-enter detached resolution.
63. A detached rehydrated local pending receipt stays `DETACHED_PENDING_SYNC`, retains its exact binding, reports `isStudioPendingSync`, and emits one idempotent observation-trigger without pretending terminal commit.
64. Studio client ACK correlation includes disposition and resulting artwork in addition to receipt/event/revision/ref/fingerprint; any mismatch remains unsynced and cannot project committed server success.
65. Concurrent Studio server instances converge through the transactionally unique receipt: the loser re-reads the winner in its transaction finalizer and returns that exact ACK, never null or generic conflict after commit.
66. Studio `expectedResultingArtwork` is snapshot-derived and fingerprint-bound in envelope and pending subject; client ACK, replay receipt, and race winner must match it exactly for every mode, including `KEEP_EXISTING`.

## Local normative references

- `models/ios-event-detail-invitation-canvas.md`
- `models/ios-event-detail-invitation-canvas.review.md`
- `models/poll-ballot-submission.machine.ts`
- `models/poll-ballot-submission.machine.test.ts`
- `models/archive-navigation.machine.ts`
- `models/archive-navigation.machine.test.ts`
- `models/studio-commit-gate.model.ts`
- `models/studio-commit-gate.model.test.ts`
- `models/time-slot-storage-identity.model.ts`
- `models/time-slot-storage-identity.model.test.ts`
- `openspec/changes/harden-event-invitation-links/`
- `openspec/specs/workflow-coordination/spec.md`
- `openspec/specs/event-organization/spec.md`
- `openspec/specs/collaboration-management/spec.md`
- `openspec/specs/offline-sync/spec.md`
- `openspec/specs/notification-management/spec.md`
- `openspec/specs/calendar-management/spec.md`
- `openspec/specs/product-excellence/spec.md`
