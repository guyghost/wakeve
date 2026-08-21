## MODIFIED Requirements

### Requirement: Delete Event MUST be supported

The organizer of an event MUST be able to delete that event only while the event is not temporally `PAST` and its lifecycle status is not `FINALIZED`. Temporal classification MUST be computed from structured event bounds with a trusted clock by the owning use case; UI visibility alone is not authorization.

This is a breaking restriction for legacy historical events whose lifecycle status is not `FINALIZED`: they become read-only Archive records and MUST NOT expose or accept `DeleteEvent` from user-facing Event Information, menus, deep links, or stale actions. An organizer MAY open an authenticated support/privacy remediation request for historical data, but that request MUST use the existing audited account/privacy authority and MUST NOT dispatch `DeleteEvent` or mutate lifecycle state.

**Règles métier**

| État de l'événement | Suppression autorisée | Confirmation requise |
|----------------------|----------------------|---------------------|
| `DRAFT`, non-`PAST` | Oui | Simple (1 clic + dialog) |
| `POLLING`, non-`PAST` | Oui | Avec avertissement (votes perdus) |
| `CONFIRMED`, non-`PAST` | Oui | Avec avertissement fort |
| `COMPARING`, non-`PAST` | Oui | Avec avertissement fort |
| `ORGANIZING`, non-`PAST` | Oui | Avec avertissement fort |
| Toute lifecycle avec `temporalClass == PAST` | Non dans l'UI | Archive read-only ; remediation support/privacy uniquement |
| `FINALIZED` | Non | Événement terminé, archivé |

#### Scenario: Suppression d'un événement DRAFT par l'organisateur

- **GIVEN** un événement en status `DRAFT`
- **AND** l'événement n'est pas temporellement `PAST`
- **AND** l'utilisateur courant est l'organisateur
- **WHEN** l'utilisateur dispatch `Intent.DeleteEvent(eventId)`
- **THEN** l'événement est supprimé du repository
- **AND** les données liées sont supprimées en cascade
- **AND** un `SideEffect.ShowToast("Événement supprimé")` est émis
- **AND** un `SideEffect.NavigateBack` est émis
- **AND** le state est mis à jour (événement retiré de la liste).

#### Scenario: Suppression d'un événement POLLING par l'organisateur

- **GIVEN** un événement en status `POLLING`
- **AND** l'événement n'est pas temporellement `PAST`
- **AND** l'utilisateur courant est l'organisateur
- **WHEN** l'utilisateur dispatch `Intent.DeleteEvent(eventId)`
- **THEN** l'événement est supprimé du repository
- **AND** les participants, votes, time slots et nouvelles données liées sont supprimés en cascade
- **AND** un `SideEffect.ShowToast("Événement supprimé")` est émis
- **AND** un `SideEffect.NavigateBack` est émis.

#### Scenario: Tentative de suppression par un non-organisateur

- **GIVEN** un événement existant
- **AND** l'utilisateur courant n'est PAS l'organisateur
- **WHEN** l'utilisateur dispatch `Intent.DeleteEvent(eventId)`
- **THEN** l'événement n'est PAS supprimé
- **AND** un `SideEffect.ShowToast("Seul l'organisateur peut supprimer cet événement")` est émis
- **AND** le state.error contient le message d'erreur.

#### Scenario: Tentative de suppression d'un événement FINALIZED

- **GIVEN** un événement en status `FINALIZED`
- **AND** l'utilisateur courant est l'organisateur
- **WHEN** l'utilisateur dispatch `Intent.DeleteEvent(eventId)`
- **THEN** l'événement n'est PAS supprimé
- **AND** un `SideEffect.ShowToast("Impossible de supprimer un événement finalisé")` est émis
- **AND** le state.error contient le message d'erreur.

#### Scenario: Tentative de suppression d'un événement PAST non finalisé

- **GIVEN** un événement a `temporalClass == PAST`
- **AND** son lifecycle status n'est pas `FINALIZED`
- **AND** l'utilisateur courant est l'organisateur
- **WHEN** Event Information, Archive, un menu, un deep link ou une action obsolète tente d'exposer ou dispatcher `DeleteEvent`
- **THEN** l'action de suppression est absente ou rejetée avec un résultat historique read-only
- **AND** l'événement reste inchangé
- **AND** l'utilisateur MAY ouvrir le parcours authentifié de remediation support/privacy
- **AND** ce parcours ne dispatch pas `DeleteEvent`.

#### Scenario: Tentative de suppression d'un événement inexistant

- **GIVEN** un eventId qui n'existe pas dans le repository
- **WHEN** l'utilisateur dispatch `Intent.DeleteEvent(eventId)`
- **THEN** un `SideEffect.ShowToast("Événement introuvable")` est émis
- **AND** le state.error contient le message d'erreur.

### Requirement: Cascade Delete MUST be supported

When deleting an eligible event, all event-owned related data MUST be deleted in one coordinated cascade without orphan records or personal data.

**Données à supprimer en cascade**

1. **Participants** - Table `participant` où `eventId = ?`
2. **Time Slots** - Table `time_slot` où `eventId = ?`
3. **Votes** - Table `vote` où `eventId = ?`
4. **Potential Locations** - Table `potential_location` où `eventId = ?`
5. **Scenarios** - Table `scenario` où `eventId = ?`
6. **Scenario Votes** - Table `scenario_vote` où `scenarioId IN (SELECT id FROM scenario WHERE eventId = ?)`
7. **Confirmed Date** - Table `confirmed_date` où `eventId = ?`
8. **Sync Metadata/Operations** - Enregistrements event-scoped où `entityId`, `recordId` ou le subject cible l'événement ou ses enfants
9. **Artwork** - `event_artwork` et `event_artwork_migration_issue` où `eventId = ?`
10. **Operation Receipts** - `event_operation_receipt` où `eventId = ?`
11. **Direct Invite Batches/Outcomes** - `direct_invite_batch` et `direct_invite_recipient_outcome` liés à l'événement, y compris les identifiants pseudonymisés protégés et payloads de livraison arrivés à leur terme de retention
12. **Event Notification Preferences** - `event_notification_preference` où `eventId = ?`

Exclusive child tables MUST use tested foreign-key cascade where safe. Explicit repository deletion MUST preserve ordering where audit, sync, or shared resources require it. Deleting an event MUST remove its `SERVER_ASSET` reference atomically; the physical asset MUST be deleted only when the authoritative reference count reaches zero and retention/security policy permits it. A shared asset with remaining references MUST NOT be deleted. Preset assets MUST NOT be treated as event-owned binaries.

Authenticated account erasure MUST inventory the new tables across surviving events and delete or irreversibly anonymize user-linked event preferences, operation receipts, direct-invite protected recipient identifiers, and encrypted delivery data according to the existing account-deletion authority. Raw recipient email MUST never be stored as `recipient_key`, and no orphan PII may remain after event deletion, retention expiry, or account erasure.

#### Scenario: Cascade delete vérifié

- **GIVEN** un événement éligible avec participants, votes, time slots, artwork, receipts, direct-invite batches/outcomes, event notification preferences et sync metadata
- **WHEN** l'événement est supprimé
- **THEN** aucune donnée event-owned orpheline ne reste dans la base
- **AND** les queries des tables existantes et nouvelles par `eventId` retournent 0 résultats
- **AND** aucun identifiant destinataire protégé ou payload de livraison propre à l'événement ne reste au-delà de la retention autorisée.

#### Scenario: Shared server asset retains another reference

- **GIVEN** deux événements référencent le même `SERVER_ASSET`
- **WHEN** un événement éligible est supprimé
- **THEN** sa référence est retirée atomiquement
- **AND** le fichier serveur est conservé pour l'autre événement
- **AND** le reference count reste cohérent.

#### Scenario: Last server asset reference is deleted

- **GIVEN** l'événement supprimé possède la dernière référence d'un `SERVER_ASSET`
- **WHEN** la cascade commit et la politique de retention autorise la purge
- **THEN** la suppression physique est planifiée idempotemment
- **AND** un retry ne décrémente pas deux fois le reference count.

#### Scenario: Account erasure removes new user-linked records

- **GIVEN** un utilisateur authentifié demande l'effacement de son compte
- **WHEN** l'autorité d'effacement traite les événements survivants et supprimés
- **THEN** les préférences événementielles, receipts, identifiants destinataires protégés et données de livraison liés à cet utilisateur sont supprimés ou irréversiblement anonymisés selon la retention légale
- **AND** aucune adresse email brute ou PII orpheline ne reste dans les nouvelles tables
- **AND** les événements appartenant à d'autres utilisateurs restent référentiellement cohérents.

### Requirement: DeleteEvent Intent Authorization MUST be enforced

The `DeleteEvent` intent MUST include `userId` for authorization verification. The owning state machine/use case MUST re-read the event, verify organizer identity, reject `FINALIZED`, compute `temporalClass` from structured bounds with a trusted clock, reject `PAST`, and only then invoke repository deletion. A client-provided temporal class or hidden UI action MUST NOT replace this owner guard.

```kotlin
data class DeleteEvent(
    val eventId: String,
    val userId: String  // For authorization verification
) : Intent
```

#### Scenario: DeleteEvent intent with userId

- **GIVEN** a DeleteEvent intent with eventId and userId
- **WHEN** the state machine processes it
- **THEN** it verifies userId matches the repository-loaded event's organizerId
- **AND** verifies the event is neither `FINALIZED` nor temporally `PAST`
- **AND** proceeds with deletion only if every authorization and temporal guard passes.

#### Scenario: Stale client attempts to delete a historical event

- **GIVEN** an older or stale client exposes Delete for an event whose trusted temporal classification is now `PAST`
- **WHEN** the owner processes `DeleteEvent`
- **THEN** it rejects the operation as historical read-only
- **AND** writes no cascade, receipt, or sync mutation
- **AND** returns a typed result that can route the user to Archive or authenticated support/privacy remediation.

## ADDED Requirements

### Requirement: Repository-Backed Event Library Projections
Wakeve MUST provide repository-backed `DRAFTS`, `HOSTING`, `ATTENDING`, `UPCOMING`, and `PAST` event projections. Projections MUST be filters rather than lifecycle states, MAY overlap, and MUST use structured event dates, membership, RSVP, organizer identity, access, and a supplied clock. `ATTENDING` MUST include only active members with accepted RSVP. Undated drafts MUST appear only in `DRAFTS` and `HOSTING`.

Each event projection MUST expose exactly one typed next action from `CONTINUE_DRAFT | SUBMIT_VOTE | VIEW_POLL_RESULTS | COMPARE_OPTIONS | CONTINUE_ORGANIZATION | VIEW_EVENT | VIEW_ARCHIVE`. `PAST` or `FINALIZED` MUST select `VIEW_ARCHIVE` before lifecycle-specific mapping. Missing action data MAY fall back to `VIEW_EVENT` only for an interactive event. Conflict or permanent sync failure MUST add a warning and reload affordance without replacing the next action with a mutation.

Library loading MUST capture `PreviousStableState<LibrarySnapshot>` as exactly `IDLE | READY(snapshot, freshness) | EMPTY(scope)`. Cancellation MUST restore the captured value exactly.

#### Scenario: Organizer views overlapping projections
- **GIVEN** an organizer owns an upcoming DRAFT event with no date and an upcoming confirmed event they will attend
- **WHEN** repository projections are computed
- **THEN** the undated event appears in `DRAFTS` and `HOSTING` only
- **AND** the confirmed event may appear in `HOSTING`, `ATTENDING`, and `UPCOMING`
- **AND** each card exposes exactly one typed next action.

#### Scenario: Future finalized event remains read-only
- **GIVEN** a finalized event has a future structured end date
- **WHEN** Library projections are computed
- **THEN** the event MAY appear in `UPCOMING`
- **AND** its interaction policy is read-only
- **AND** its only event-opening action is `VIEW_ARCHIVE`.

#### Scenario: Library load is cancelled
- **GIVEN** Library is loading with a captured ready or empty stable state
- **WHEN** the load is cancelled
- **THEN** Library restores that exact stable state
- **AND** cancellation does not create a terminal failure or synthesized partial snapshot.

### Requirement: Creation Studio SHALL Persist a Versioned Event and Artwork Aggregate
Wakeve MUST support total Studio modes `NEW | EDIT_EXISTING(eventId, baseRevision)`. `EDIT_EXISTING` MUST be available only for a DRAFT event through an installed owner edit capability. A preview of the exact current `draftRevision` MUST be completed before commit.

Persisted artwork MUST be exactly `NONE | STRUCTURED(version, ref) | LEGACY_REMOTE(validatedHttpsUrl)`. A structured reference MUST identify `PRESET` or `SERVER_ASSET`, validated alt behavior, normalized focal point, and `FILL | FIT` crop. Remote URLs MUST use HTTPS and an allowlisted server/CDN host without userinfo, fragment, or secret-bearing query values. Rendering failure MUST NOT mutate the persisted artwork value.

Release 1 MUST allow `NONE`, curated `PRESET`, and an already-authorized `SERVER_ASSET` reference only when an existing owner capability supplies it. If no such owner exists, release 1 MUST expose `NONE` and `PRESET` only. Device photo selection/upload and local-file artwork persistence are out of scope and MUST NOT be exposed by release-1 Studio.

`UpdateDraftAggregate(eventId, actorId, expectedBaseRevision, eventDraft, artwork, operationId)` MUST re-read and revalidate organizer identity, `DRAFT` status, exact base revision, draft fields, artwork, and operation identity inside one repository transaction. That transaction MUST atomically persist event fields, artwork, incremented revision, operation receipt, and sync operation. A post-commit retry MUST replay only the existing sync operation and MUST NOT execute the aggregate update again.

During mixed-version rollout, every aggregate writer MUST preserve artwork, aggregate schema/revision, and operation receipt. A writer that cannot preserve those fields MUST be fenced with an upgrade/read-only result or routed through a field-aware compatibility adapter. Background sync MUST use revision-checked patch semantics rather than replacing the aggregate from an old payload. UI rollback MUST retain migrations, compatible writers, fencing, receipts, cascade behavior, and protected data.

#### Scenario: Organizer previews and creates a new event
- **GIVEN** a valid new Studio draft and resolved total artwork
- **WHEN** the organizer previews the current draft revision and confirms commit
- **THEN** the owner persists one event, one artwork value, one revision, one operation receipt, and one sync operation atomically
- **AND** the Studio observes repository truth as pending sync or completed
- **AND** it does not claim the event is shared or server-confirmed before acknowledgement.

#### Scenario: Organizer edits a stale DRAFT revision
- **GIVEN** an organizer opened `EDIT_EXISTING` at base revision 4
- **AND** the repository event is now revision 5
- **WHEN** `UpdateDraftAggregate` revalidates the commit
- **THEN** it returns a conflict without writing event fields, artwork, receipt, or sync operation
- **AND** the existing event remains unchanged.

#### Scenario: Release 1 has no server-asset owner
- **GIVEN** no existing authorized server-asset owner capability is installed
- **WHEN** the organizer chooses artwork in Studio
- **THEN** only `NONE` and curated presets are available
- **AND** no device photo picker, upload action, local file URL, or unowned server asset is offered.

#### Scenario: Unauthorized server asset reference is rejected
- **GIVEN** a Studio draft contains a server asset reference not issued by the installed owner capability
- **WHEN** the organizer previews or commits
- **THEN** validation rejects the reference without writing event, artwork, receipt, or sync state
- **AND** Wakeve does not fetch or persist the untrusted URL as artwork.

#### Scenario: Legacy artwork is migrated by a successful edit
- **GIVEN** an existing DRAFT projects valid `LEGACY_REMOTE` artwork
- **WHEN** an authorized edit successfully selects a curated preset or owner-authorized server asset
- **THEN** the transaction writes `STRUCTURED` artwork
- **AND** the structured value is never rewritten as legacy.

#### Scenario: Studio retries after local commit
- **GIVEN** the aggregate transaction committed and sync later failed
- **WHEN** the organizer retries
- **THEN** Wakeve replays the matching sync operation receipt
- **AND** does not call create or `UpdateDraftAggregate` again
- **AND** does not duplicate the event or increment its revision again.

#### Scenario: Old writer cannot preserve protected aggregate fields
- **GIVEN** an older client or sync payload cannot round-trip artwork, aggregate revision, or operation receipt
- **WHEN** it attempts an aggregate update after migration cutover
- **THEN** the owner fences the write or applies a compatibility patch that preserves every protected field
- **AND** the writer cannot replace the aggregate row with missing/default values.

#### Scenario: UI rollback preserves data and writer safety
- **GIVEN** the release-1 UI flag is disabled after new-schema writes exist
- **WHEN** the prior UI and background sync continue operating
- **THEN** the migrated data, artwork, revisions, receipts, cascade rules, and writer fencing remain active
- **AND** no down-migration or unfenced legacy writer is enabled.

### Requirement: Past and Finalized Events SHALL Use a Global Read-Only Archive Policy
Wakeve MUST compute `temporalClass` from structured event bounds and a supplied clock. `interactionPolicy` MUST become `READ_ONLY` when `temporalClass == PAST`, `eventStatus == FINALIZED`, or Archive is active. This override MUST be evaluated before every surface, route, menu, deep link, provider destination, and next-action mapper.

For a read-only override, mutating or stale routes MUST resolve to `VIEW_ARCHIVE`. If the Archive route is unavailable, Wakeve MUST show a local read-only summary. Archive MAY render accessible artwork, final structured date, organizer, permitted participant/context summaries, freshness, and sync warnings, but MUST NOT vote, invite, approve, edit, leave, remove, delete, change notification preference, or unfinalize.

#### Scenario: Stale deep link targets a past event mutation
- **GIVEN** an event is temporally `PAST`
- **AND** a deep link targets Poll, Studio, Audience management, Organization mutation, or Information mutation
- **WHEN** route preflight runs
- **THEN** the destination becomes `VIEW_ARCHIVE` before an owner callback is invoked
- **AND** no mutation is dispatched.

#### Scenario: Finalized event is not yet past
- **GIVEN** a future event has lifecycle status `FINALIZED`
- **WHEN** any surface computes interaction policy
- **THEN** the event remains read-only
- **AND** only Archive or a local read-only summary may open.

#### Scenario: Archive repository load fails with a prior snapshot
- **GIVEN** Archive is loading with a captured prior ready snapshot
- **WHEN** loading fails or is cancelled
- **THEN** Wakeve preserves or restores the exact prior snapshot with an honest freshness/error state
- **AND** retry reloads repository data only
- **AND** no lifecycle transition becomes available.

### Requirement: Event Information Mutations SHALL Be Capability-Gated and Operation-Specific
Event Information MUST derive available operations from typed viewer capabilities and existing guarded owner use cases. Every write MUST carry `OperationKey(subject, action, target, operationId)` where the action is `SAVE_EVENT_PREFERENCE | LEAVE_EVENT | REMOVE_PARTICIPANT | DELETE_EVENT`. Retry and acknowledgement MUST match every key dimension. Views MUST NOT write repositories directly or invent an adjacent lifecycle transition.

`LEAVE_EVENT` MUST be limited to an active non-organizer member on a non-finalized event with an installed use case. `REMOVE_PARTICIPANT` MUST be limited to an organizer, non-finalized event, and active non-organizer target. `DELETE_EVENT` MUST delegate to the existing guarded delete intent and MUST NOT create a `CANCELLED` event status. `PAST` or `FINALIZED` MUST remove all writes.

#### Scenario: Organizer cancels participant removal
- **GIVEN** an authorized remove operation is awaiting destructive confirmation
- **WHEN** the organizer cancels confirmation
- **THEN** Wakeve restores the stable Information snapshot
- **AND** does not dispatch the remove use case or write an operation receipt.

#### Scenario: Organizer attempts to leave their own event
- **GIVEN** the current user is the event organizer
- **WHEN** Event Information computes available membership actions
- **THEN** `LEAVE_EVENT` is absent
- **AND** ownership transfer is not inferred or offered.

#### Scenario: Late acknowledgement targets a different operation
- **GIVEN** Event Information is waiting for one subject, action, target, and operation id
- **WHEN** an acknowledgement arrives with any mismatched dimension
- **THEN** Wakeve ignores the acknowledgement
- **AND** preserves the current operation state and repository truth.
