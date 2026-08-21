## Context

The approved `models/ios-event-invitation-experience.md` defines a connected invitation experience around the completed Event Detail invitation canvas. The canvas remains the presentation owner for the Event Detail hero, but it cannot by itself own Library filtering, event/artwork persistence, audience operations, notification preferences, real route installation, destructive permissions, or archive policy.

The architecture remains Kotlin Multiplatform and local-first: shared domain/use cases and SQLDelight repositories own persisted truth, while native SwiftUI surfaces render projections and dispatch typed callbacks. `EventManagementStateMachine` remains the sole owner of `EventStatus`. The pending `harden-event-invitation-links` change remains the sole owner of public invitation validity and membership redemption.

## Goals / Non-Goals

### Goals

- Deliver six connected iOS surfaces: Library, Creation Studio, Audience, Event Detail routes, Information, and Archive.
- Persist total, validated artwork and support guarded edits to existing DRAFT events.
- Make invitation delivery, approval, membership, RSVP, and date validation explicit and independent.
- Preserve local-first repository truth, operation identity, cancellation, conflicts, and honest pending sync.
- Make `PAST` and `FINALIZED` a global read-only routing preflight.
- Keep platform chrome native and Wakeve expression in event content surfaces.

### Non-Goals

- Replacing the completed invitation canvas or creating a second Event Detail state mapper.
- Adding a second lifecycle machine, a `CANCELLED` event status, ownership transfer, or implicit transition.
- Implementing the pending secure invitation backend/Universal Link flow inside this change.
- Implementing device photo selection, local-photo upload, upload retry, or a new server-asset issuance owner in release 1. Any future proposal for that behavior MUST first extend and review the approved model before implementation.
- Adding PassKit, Apple Wallet passes, admission/payment semantics, local tokens, local invitation URLs, or local QR codes.
- Adding public discovery, a social feed, generic chat/tasks/workspace behavior, or LLM decision authority.
- Redesigning Android in release 1.

## Canonical Reference Interpretation

| Reference | Normative adaptation |
|---|---|
| Apple Invites Library | Event projections and one next action per card; Wakeve repository/access rules determine membership. |
| Apple Invites Creation | Artwork-led Studio with mandatory preview before persistence. |
| Apple Invites contextual modules | Existing Wakeve modules ordered by lifecycle, access, and readiness. |
| Apple Invites Audience/Approvals/Settings | Typed audience axes, capability-gated operations, and event-scoped Information. |
| Apple Wallet stack/detail/information/archive | Glanceable layered information hierarchy and read-only archive only. |

Mobbin URLs are recorded in `proposal.md` and in the approved model. They are visual references only. Apple Invites informs workflow; Apple Wallet informs information architecture only.

## Release Boundary

### Release 1

- Repository-backed Library projections and deterministic next action.
- Studio `NEW` plus owner-capability-gated `EDIT_EXISTING` for DRAFT only.
- Persisted artwork union, revision, operation receipt, one-way legacy migration, release-1 `NONE`/preset/already-authorized-server-asset selection, preview, atomic commit, and truthful sync states.
- DRAFT-only direct invite batches through the existing owner capability.
- Audience projections with independent invitation/approval/membership/RSVP/date-validation axes.
- Existing canvas actions routed to installed Poll, Participants, Organization, details, and Archive destinations.
- Information snapshot, event notification preference, validated Calendar/Maps/Weather destinations, and existing guarded destructive use cases.
- Global `PAST`/`FINALIZED` read-only routing and Archive.

### Dependency release

`harden-event-invitation-links` owns public link issuance, context-bound share payload, open, authentication continuation, inspect/redeem, rotate/revoke/expire, retry, and terminal outcomes. Until it is delivered, `SecureShareCapability` cannot become `READY`. Guest approval remains `NOT_SUPPORTED` unless that dependency adds and reviews an online approval contract.

Local photo picker/upload behavior is outside both release 1 and the current approved model. A future proposal MUST first extend and review the model, then define authorization, upload ownership, storage, privacy, retry, deletion, and server-asset issuance before implementation. If no existing authorized server-asset owner is installed, Studio offers only `NONE` and curated presets.

## Ownership and Data Flow

```text
SQLDelight repositories
  -> shared typed snapshots/use cases
  -> total presentation projections and route capabilities
  -> SwiftUI surfaces
  -> typed callback to owning use case/state machine
  -> repository transaction + operation receipt
  -> observed repository projection
```

- Views never write a status, participant role, artwork row, preference, or membership directly.
- Repository observation is the only handoff between business owners and surfaces.
- Display strings, imagery, alt text, provider copy, AI output, and localized copy are outputs only.
- Unknown enum values, absent records, stale capability bindings, and missing routes fail closed to read-only summary or hidden/unavailable action.

## Domain Decisions

### Library projections are filters, not states

`DRAFTS`, `HOSTING`, `ATTENDING`, `UPCOMING`, and `PAST` are repository-derived projections and may overlap. `ATTENDING` contains active members with accepted RSVP only. Undated drafts appear only in `DRAFTS` and `HOSTING`. Ordering uses structured dates then stable event id.

`LibraryNextAction` is total: `CONTINUE_DRAFT | SUBMIT_VOTE | VIEW_POLL_RESULTS | COMPARE_OPTIONS | CONTINUE_ORGANIZATION | VIEW_EVENT | VIEW_ARCHIVE`. The global read-only preflight selects `VIEW_ARCHIVE` for `PAST` or `FINALIZED` before any lifecycle-specific mapping. Conflicts add a warning and reload affordance; they do not invent a mutation.

### Persisted artwork is a total union

```text
Artwork = NONE
        | STRUCTURED(version, ref)
        | LEGACY_REMOTE(validatedHttpsUrl)

ArtworkRef.source = PRESET(presetId)
                  | SERVER_ASSET(assetId, canonicalHttpsUrl, assetRevision)
ArtworkRef.alt    = DECORATIVE
                  | INFORMATIVE(nonBlankLocalizedText)
ArtworkRef.crop   = FILL | FIT
ArtworkRef.focalPoint = normalized x/y in 0...1
```

Every event has exactly one logical artwork value. Rendering or download failure never changes the persisted discriminator. `NONE`, invalid legacy data, offline image failure, and unavailable image providers render the deterministic event-mood fallback.

All URLs require HTTPS, an allowlisted server/CDN host, no userinfo, no fragment, and no secret-bearing query value. URL values and asset payloads are excluded from logs and analytics.

### Studio owns transient draft state; repositories own commits

`StudioMode` is total: `NEW | EDIT_EXISTING(eventId, baseRevision)`. New sessions use a typed `NOT_APPLICABLE` base revision. A monotonically increasing `draftRevision` invalidates stale preview, artwork-resolution, and commit callbacks.

Release-1 artwork selection uses exactly the approved model contract: `KEEP_EXISTING | NONE | PRESET(presetId) | EXISTING_SERVER_ASSET(assetId)`. `KEEP_EXISTING` is available only in `EDIT_EXISTING`; `NEW` MUST resolve to `NONE`, a curated `PRESET`, or an authorized existing server asset. For `EXISTING_SERVER_ASSET(assetId)`, the choice carries only the opaque `assetId`: the asset owner resolves and validates the current canonical reference from `ArtworkSelectionCapability.READY(actorId, accessRevision, authorizedAssetsByOpaqueId)`. No URL or asset revision transits through the Studio choice. A missing or stale capability fails closed while preserving the draft for review. Studio does not request photo-library permission, stage a device file, upload media, or persist a local file URL. Without that owner, release 1 exposes only `KEEP_EXISTING` where applicable, `NONE`, and `PRESET`.

A preview of the exact current `draftRevision` is mandatory before commit. Pre-commit failure preserves the draft. Post-commit sync failure retains the committed event revision and receipt; retry replays sync only.

### `UpdateDraftAggregate` is the sole DRAFT edit transaction

```text
UpdateDraftAggregate(
  eventId,
  actorId,
  expectedBaseRevision,
  eventDraft,
  artwork,
  operationId
)
```

Inside one owner transaction it MUST:

1. re-read the event and operation receipt;
2. return the existing result for a matching completed `operationId`;
3. verify organizer identity;
4. verify `status == DRAFT`;
5. verify exact aggregate revision;
6. validate all draft fields and the total artwork union;
7. persist event fields and artwork atomically;
8. increment aggregate revision;
9. write the operation receipt and sync operation atomically.

A conflict or failed guard writes nothing. Cancellation of `EDIT_EXISTING` leaves the original aggregate unchanged. No view can bypass this transaction by writing repository fields separately.

### Mixed-version writers must preserve the aggregate

Artwork, aggregate revision, and operation receipt become protected aggregate fields at migration cutover. Every writer—new client, older client, backend compatibility route, background sync, conflict replay, and rolled-back UI—MUST either preserve and round-trip these fields or be fenced from aggregate mutation.

- Each aggregate write carries `writerSchemaVersion`, expected aggregate revision, and operation id.
- The repository/server publishes a minimum compatible aggregate-writer version.
- An old writer that cannot preserve artwork/revision/receipt is rejected with a typed upgrade/read-only result or routed through a compatibility adapter that patches only fields it understands while preserving protected fields.
- Background sync replays field-aware operations against the expected revision; it never replaces the whole row from an older payload.
- UI rollback disables the new navigation/surfaces only. New migrations, compatible owner use cases, fencing, receipts, and protected-field preservation remain active.
- No rollback performs a down-migration or restores an unfenced legacy writer.

### Audience axes never propagate implicitly

Each identity has independent typed axes:

- invitation delivery;
- guest approval;
- membership;
- RSVP;
- retained-date validation.

Server acceptance is not delivery; approval is not membership; membership is not accepted RSVP; accepted RSVP is not retained-date validation. `NOT_APPLICABLE` and `UNAVAILABLE` are explicit. Counts and identities derive from one repository snapshot.

Direct invites use a matching `DirectInviteCapability`, one batch id, one stable operation id, and one outcome for every requested recipient. Retry replays only unresolved retryable recipients. Cancellation affects unresolved recipients only; accepted recipients require a separate supported revoke capability.

The persisted recipient key is never a raw or reversibly normalized email address. It is either a stable protected pseudonymous id supplied by the invitation owner or a versioned keyed HMAC/digest computed inside the trusted repository/backend boundary. Normalized recipient input exists transiently for validation/delivery and, if a delivery queue must retain it, is encrypted/protected at rest with access limited to that operation. Raw identifiers and keyed material are excluded from logs, analytics, UI identifiers, and error text.

### Notification routing has three independent axes

1. `EventNotificationPreference`: `INHERIT_ACCOUNT | ALL_EVENT_UPDATES | ESSENTIAL_ONLY | MUTED`, persisted by `(eventId, userId)`.
2. `AccountNotificationPreference`: existing account-enabled types, quiet hours, sound, and vibration; read-only from Event Information.
3. `SystemNotificationAuthorization`: `UNAVAILABLE | NOT_DETERMINED | PROVISIONAL | AUTHORIZED | EPHEMERAL | DENIED | RESTRICTED`, projected from the iOS authorization port. `UNAVAILABLE` is the total fallback while the port has not supplied a snapshot or is unavailable; it is not the concrete iOS value `NOT_DETERMINED`.

Effective routing evaluates OS authorization first, then critical-security account policy, account-enabled types, event-level intersection, and account quiet hours. `UNAVAILABLE` permits no system delivery and renders an honest unavailable state. It triggers no permission prompt and mutates neither event nor account preference. Only a typed `SYSTEM_NOTIFICATION_AUTHORIZATION_SNAPSHOT_RECEIVED(value)` callback from the iOS port may replace it with a concrete authorization value. `ALL_EVENT_UPDATES` cannot re-enable an account-disabled type. Event Information edits only the event preference and never prompts for OS authorization or changes the account record.

Preference writes carry `OperationKey(subject, action, target, operationId)` and late acknowledgements must match all dimensions. Cancellation restores the exact prior stable snapshot.

### Global `PAST`/`FINALIZED` override runs first

`interactionPolicy == READ_ONLY` when `temporalClass == PAST`, `eventStatus == FINALIZED`, or Archive is active. The router evaluates this before surface visibility, menu actions, deep links, canvas actions, or provider destinations.

A mutating or stale route becomes `VIEW_ARCHIVE`. If Archive is unavailable, the app shows a local read-only summary. Archive can reload repository data but cannot vote, invite, approve, edit, leave, remove, delete, change preference, or unfinalize.

This is an intentional breaking change to the existing delete contract: a historical `PAST` event is read-only even when its legacy lifecycle status is not `FINALIZED`. Event Information and Archive expose no delete action. An authenticated support/privacy remediation entry point may collect a request and route it to the existing account/privacy authority; it does not dispatch `DeleteEvent`. The owner also rechecks temporal classification with a trusted clock so a stale UI cannot delete a historical event.

### Secure public sharing is context-bound and injected

`SecureShareCapability.READY` contains an opaque server payload plus `(eventId, actorId, accessRevision, capabilityId)`. Every dimension must match the current context at tap time. A change invalidates readiness immediately.

The payload lives in memory only for the share invocation and is never parsed, logged, persisted, cached, indexed, placed in analytics, or rendered as a client-generated QR code. `HIDDEN`, `UNAVAILABLE`, `LOADING`, and `FAILED` expose no share action. Direct invitations and public-link sharing remain separate capabilities.

This prohibition is scoped to the six new surfaces. Legacy invitation code and cleanup outside them remain governed by the authoritative `harden-event-invitation-links` cutover, including its task 5.2. This change does not delete global `InvitationTokenCodec` code or change unrelated legacy routes before that owner is ready.

## Logical Persistence Schema and Migration

Exact SQL names may follow existing SQLDelight conventions, but these semantics are required.

### Aggregate revision and artwork

```text
event.aggregate_revision INTEGER NOT NULL DEFAULT 0
event.aggregate_schema_version INTEGER NOT NULL DEFAULT 1

event_artwork(
  event_id PRIMARY KEY,
  kind, version,
  source_type, preset_id,
  asset_id, canonical_https_url, asset_revision,
  alt_kind, localized_alt_text,
  focal_x, focal_y, crop,
  updated_at
)

event_artwork_migration_issue(
  event_id PRIMARY KEY,
  issue_code,
  recorded_at
)
```

Constraints enforce the union: `NONE` has no source fields; `STRUCTURED` has a valid version/ref; `LEGACY_REMOTE` has only a validated allowlisted HTTPS URL. Invalid legacy URLs are not retained in logs or issue text.

### Idempotency and Studio sync

```text
event_operation_receipt(
  operation_id PRIMARY KEY,
  event_id,
  action,
  expected_base_revision,
  committed_revision,
  sync_state,
  created_at
)
```

The receipt and existing sync queue record are written in the same transaction as the aggregate. Unique operation identity prevents duplicate create/update on replay.

### Direct invitation batches

```text
direct_invite_batch(
  batch_id PRIMARY KEY,
  event_id, actor_id, access_revision,
  operation_id UNIQUE,
  operation_state,
  created_at, updated_at
)

direct_invite_recipient_outcome(
  batch_id, protected_recipient_key, key_version,
  outcome_kind, invitation_id, reason_code,
  expires_at,
  PRIMARY KEY(batch_id, protected_recipient_key)
)
```

`protected_recipient_key` is an owner-issued pseudonymous identifier or a versioned keyed HMAC/digest, never raw email. If delivery requires a normalized recipient value, only the protected delivery operation may retain it encrypted for the minimum delivery/retry lifetime. Every completed/failed batch must reconcile requested protected keys to outcome keys exactly.

### Event notification preference

```text
event_notification_preference(
  event_id, user_id,
  preference,
  revision,
  updated_at,
  PRIMARY KEY(event_id, user_id)
)
```

Writes reuse the existing sync queue and persist their operation key. Account preference and system authorization are not copied into this table.

### Deletion, retention, and erasure

Event deletion extends the existing atomic cascade to `event_artwork`, `event_artwork_migration_issue`, `event_operation_receipt`, `direct_invite_batch`, `direct_invite_recipient_outcome`, `event_notification_preference`, and their event-scoped sync metadata. Foreign keys use cascade where ownership is exclusive and tested; explicit repository deletion is used where ordering, audit, or shared references require it.

Deleting an event removes its `SERVER_ASSET` reference in the same transaction. Physical asset deletion occurs only when the authoritative reference count reaches zero and security/audit retention allows it. Shared assets with remaining references are preserved. Presets have no event-owned binary to delete.

Direct-invite protected keys and any encrypted delivery recipient are retained only for their delivery/idempotency/audit window, then deleted or irreversibly anonymized. Authenticated account erasure inventories preferences, direct-invite batches/outcomes, operation receipts, membership-linked records, and protected recipient identifiers across surviving events; it deletes or anonymizes user-linked data under the existing account-deletion authority without restoring deleted data or leaving orphan PII.

### Migration sequence

1. Add aggregate revision/schema version, artwork, migration issue, operation receipt, direct-invite batch/outcome, and event-notification-preference storage without removing legacy fields.
2. For every event, validate `heroImageUrl`: valid allowlisted HTTPS becomes `LEGACY_REMOTE`; missing becomes `NONE`; invalid becomes `NONE` plus a sanitized migration issue.
3. Backfill revision `0` and verify every event has exactly one logical artwork projection.
4. Install minimum-writer fencing and compatibility adapters before enabling any new aggregate writer; verify old client, new client, background sync, and rolled-back UI paths preserve protected fields.
5. Dual-read legacy artwork only during the migration window; all successful preset or authorized-server-reference edits write `STRUCTURED` and never rewrite legacy.
6. Enable repository projections and writes behind the release flag after migration, mixed-version, cascade-delete, retention, and erasure tests pass.
7. Remove dual-read or relax fencing only in a separately approved cleanup after evidence proves no unmigrated or incompatible writer remains.

## iOS Surface Composition

- **Library** uses layered, glanceable cards and native segmented/filter controls; projections and actions come from shared repositories.
- **Studio** is artwork-led but remains a real persisted draft editor; release 1 offers `NONE`, curated presets, and an already-authorized server asset only when its owner capability exists. It exposes no device photo picker/upload.
- **Audience** renders independent axes and capability states; it does not compress them into colored avatars or one status string.
- **Event Detail** reuses the completed canvas unchanged as the hero and installs real owner routes beneath it.
- **Information** uses a native sheet/form hierarchy for metadata, provider destinations, notification axes, and guarded operations.
- **Archive** uses artwork-led, Wallet-inspired information density while remaining a conventional Wakeve read-only view, not a pass.

Native tab/navigation bars, toolbars, menus, sheets, alerts, forms, search, and standard actions remain familiar iOS chrome. Wakeve expression belongs to event artwork, mood, hierarchy, participants, and state clarity. Liquid Glass is limited to meaningful controls/elevated surfaces with opaque/material fallbacks.

## Error, Cancellation, Retry, and Accessibility

- Every load captures `PreviousStableState`; cancellation restores it exactly.
- Stale data may render but never expands permission or upgrades pending work to server success.
- Late callbacks match event/entity, revision, operation, batch, and recipient dimensions as applicable.
- Typed retryable failures may replay the same operation identity; permanent, forbidden, validation, and not-found failures do not loop.
- Conflicts are read-only warnings with `RELOAD_PROJECTION`, not client conflict resolution.
- Each primary card/canvas exposes at most one action and keeps it reachable with Dynamic Type.
- All six surfaces support VoiceOver order/actions, 44-point targets, light/dark mode, accessibility Dynamic Type, increased contrast, Reduce Motion, Reduce Transparency, compact height, and landscape.
- Visual QA compares real repository-backed simulator flows to the canonical Mobbin references; fixtures cannot substitute for persistence/routing verification.

## Rollout

1. Land and verify schema migrations with the feature flag off.
2. Install mixed-version writer fencing/compatibility and shared models/repositories/use cases behind `iosInvitationExperienceV1` after RED tests.
3. Enable internal Library and Archive reads first.
4. Enable Studio persistence and DRAFT edit after migration/idempotency/mixed-version tests; expose only `NONE`/presets unless an authorized server-asset owner already exists.
5. Enable Audience direct invites and Information preference writes after access/offline tests.
6. Enable real Detail routing and the complete six-surface navigation in internal builds.
7. Run simulator repository flows, accessibility passes, localization parity, and Mobbin/in-app visual QA.
8. Keep public secure share unavailable until `harden-event-invitation-links` independently satisfies its gates.

## Rollback

- Disable `iosInvitationExperienceV1` and route users back to the existing Library/Create/Event Detail entry points.
- Keep migrated artwork, aggregate schema/revisions, receipts, protected direct-invite outcomes, event preferences, writer fencing, compatibility adapters, cascade rules, and retention/erasure behavior active; rollback changes UI routing only and does not drop or rewrite data.
- The completed invitation canvas remains available as the stable Event Detail surface.
- Pending sync operations remain owned by the existing sync queue and may finish safely.
- A destructive schema down-migration is forbidden. Any later cleanup requires a new reviewed proposal.

## Risks / Trade-offs

- Cross-capability scope is large. Mitigation: release flag, ordered shared foundations, six bounded surfaces, and TDD gates before each production slice.
- Legacy artwork may be invalid. Mitigation: total union, allowlisted validation, sanitized issue record, and deterministic fallback.
- DRAFT edits can race. Mitigation: owner re-read, exact base revision, atomic transaction, and stable idempotency receipt.
- Old and rolled-back clients can erase protected aggregate fields. Mitigation: minimum-writer fencing, field-aware compatibility adapters, patch-based sync, and mixed-version regression tests.
- Audience state can become misleading. Mitigation: five independent axes and one coherent repository snapshot.
- Direct-invite identifiers can leak personal data. Mitigation: protected pseudonymous/HMAC keys, encrypted short-lived delivery payloads, retention limits, cascade deletion, and account erasure coverage.
- Secure sharing dependency may not be ready. Mitigation: hidden/unavailable state with no local substitute.
- Notification settings can appear to override iOS/account settings. Mitigation: explicit three-axis display and deterministic effective priority.
- Future parity work could duplicate routes. Mitigation: this change owns the native routes; deferred parity consumes them.

## Open Questions

None blocking proposal approval. Guest approval and public secure sharing remain explicitly dependency-gated rather than unresolved release-1 assumptions.
