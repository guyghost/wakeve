# Change: Expand the iOS invitation experience

## Why

The completed `update-ios-event-detail-invitation-canvas` change successfully established the immersive Event Detail hero, its deterministic state projection, accessible action placement, and secure-share boundary. Its scope was intentionally presentation-only, however, and is too narrow to deliver the connected invitation workflow represented by the approved product model.

This change corrects that scope without discarding or reopening the canvas work. The existing invitation canvas remains a finished building block inside a broader repository-backed experience that lets a private group find an event, create or edit it with persisted artwork, invite the right audience, follow real planning routes, inspect event information, and revisit past/finalized events safely.

## What Changes

- Add six connected, functional iOS surfaces:
  1. **Library** — repository-backed Drafts, Hosting, Attending, Upcoming, and Past projections with one deterministic next action per event.
  2. **Creation Studio** — new-event and guarded DRAFT editing sessions with release-1 preset artwork or an already-authorized server asset reference, required preview, revisions, atomic aggregate persistence, and truthful sync state.
  3. **Audience** — coherent participant projections, DRAFT-only direct invitation batches in release 1, independent delivery/approval/membership/RSVP/date-validation axes, and an injected secure public-share capability.
  4. **Event Detail routes** — retain the completed invitation canvas and connect its typed actions to the real Poll, Participants, Organization, local details, and Archive owners.
  5. **Information** — event metadata, validated Calendar/Maps/Weather destinations, event-scoped notification preference, and existing permission-gated leave/remove/delete operations.
  6. **Archive** — a deliberately read-only view for `PAST` or `FINALIZED` events, including safe routing from stale actions and deep links.
- Add shared domain contracts, SQLDelight persistence/migrations, repository projections, operation receipts, and total state models required by those surfaces.
- Persist artwork as the total union `NONE | STRUCTURED | LEGACY_REMOTE`, with one-way migration from valid legacy hero URLs and a deterministic event-mood fallback. Release 1 exposes `NONE`, presets, and already-authorized server assets only; if no server-asset owner exists, it exposes `NONE` and presets only.
- Add `UpdateDraftAggregate` as the guarded owner transaction for DRAFT edits; it revalidates organizer, status, base revision, artwork, and operation identity atomically.
- Require mixed-version writer fencing and compatibility adapters so old/new clients, background sync, and UI rollback preserve aggregate revision, artwork, and operation receipts instead of overwriting unknown fields.
- Keep direct invitation, approval, membership, RSVP, and retained-date validation as independent typed axes rather than one ambiguous participant status.
- Store direct-invite recipient identity only as an opaque keyed digest or protected pseudonymous identifier; normalized email input remains transient or protected only where delivery requires it, never a raw persisted recipient key.
- Add an event-scoped notification preference while preserving account preference and iOS system authorization as separate axes with deterministic effective routing.
- Apply the global `PAST`/`FINALIZED` read-only override before every surface, menu, route, deep link, and next-action projection.
- Prohibit the six new surfaces from introducing or consuming local invitation tokens, locally constructed invitation URLs, local QR codes, or client-authorized redeem behavior. This change does not perform global invitation-link cleanup outside those surfaces.
- **BREAKING**: user-facing deletion is no longer available for a temporally `PAST` event even if its legacy lifecycle status is not `FINALIZED`. Archive remains read-only; historical-data remediation is handled only through the defined authenticated support/privacy process.
- Extend event cascade deletion and account-erasure coverage to artwork, migration issues, operation receipts, direct-invite batches/outcomes, event notification preferences, protected recipient identifiers, and server-asset reference retention.
- Add English, French, Spanish, Italian, and Portuguese copy plus repository-flow, accessibility, simulator, and visual-comparison verification.

## Release Boundary

### Release 1

Release 1 includes all six repository-backed surfaces, guarded DRAFT editing with `NONE`, preset artwork, and already-authorized server asset references when an owner exists, DRAFT-only direct invitation batches through an existing owner capability, real Detail routes, event Information with three-axis notification state, and the global read-only Archive override.

Device photo selection, upload authorization, upload retry, and conversion into `SERVER_ASSET` are outside both release 1 and the current approved behavioral model. Any future OpenSpec proposal for that flow MUST first extend and review the model, then define its owner and dependencies before implementation.

### Dependency on `harden-event-invitation-links`

Public-link issuance, bound payload sharing, Universal Link open, authentication continuation, inspect/redeem, rotation, revocation, expiry, and related retries remain owned by the pending `harden-event-invitation-links` change. Until that dependency provides a matching typed capability, public sharing stays hidden or honestly unavailable. Guest approval remains `NOT_SUPPORTED` unless the dependency delivers a reviewed online backend policy and request contract.

This change does not implement a substitute token, URL, QR code, membership decision, or optimistic approval.

Legacy invitation implementation outside the six new surfaces remains under the authoritative cutover plan in `harden-event-invitation-links`, including its task 5.2. This proposal neither removes global legacy token code nor broadens cleanup beyond the new surfaces.

## Canonical Visual References

The Mobbin sources are canonical visual references, not behavioral authority:

- [Apple Invites library](https://mobbin.com/screens/d04f0876-4c6c-461e-bcba-ce6fb752ea95)
- [Apple Invites creation](https://mobbin.com/screens/e07fe9b7-1409-4345-9df0-56806b591ba6)
- [Apple Invites contextual modules](https://mobbin.com/screens/e5d7db23-34bd-4aa5-bbbf-6d74bd65e18e)
- [Apple Invites share and audience](https://mobbin.com/screens/d726cfff-2f7f-406c-b56a-b88fafcade02)
- [Apple Invites approvals](https://mobbin.com/screens/93e6c76c-c79b-4494-8cff-b61c72736251)
- [Apple Invites settings](https://mobbin.com/screens/c77b278e-3957-4b76-a1f7-60580a313347)
- [Apple Wallet stack](https://mobbin.com/screens/30c73314-7baa-46a1-9419-155aa6282488)
- [Apple Wallet pass detail](https://mobbin.com/screens/e1061c73-0be8-42ba-aaed-3bb1c3321a4e)
- [Apple Wallet information](https://mobbin.com/screens/f3ce79fa-8661-4833-a601-04c40cc95ef5)
- [Apple Wallet archive](https://mobbin.com/screens/4e2bd1e9-9a04-46ba-9bd9-db7093ace671)

Apple Invites informs the private event workflow. Apple Wallet informs glanceable information architecture only. Wakeve does not adopt PassKit, passes, payment semantics, scannable admission, or locally generated QR codes.

## Product Excellence Fit

The connected experience helps a private group prepare, decide, coordinate, and finish one event without reconstructing context across disconnected screens. Library projections reduce search effort; the Studio makes creation and DRAFT revision reviewable; Audience separates what was sent, approved, joined, accepted, and date-validated; Detail exposes the real next owner flow; Information keeps metadata and permissions event-scoped; Archive makes completed work safe to revisit.

Every primary card or canvas exposes at most one next useful action, shows pending/offline state honestly, and identifies read-only outcomes before navigation. The six surfaces remain compact and native on mobile, support accessibility settings, and progressively disclose secondary work. The change adds no social feed, generic chat, generic task manager, generic calendar workspace, or AI authority. LLM output cannot select artwork, role, permission, status, approval, membership, action, or transition.

## Coordination and Conflicts

- `update-ios-event-detail-invitation-canvas` is complete and consumed as the Event Detail hero; this change must not duplicate, replace, or regress its total projection and visual QA.
- `harden-event-invitation-links` remains the sole owner of secure public invitation issuance/open/redeem and must provide the bound capability before share is enabled.
- `standardize-product-language` owns canonical user-visible vocabulary; new localization keys must use its state/action concepts before either change is archived.
- `add-event-weather-forecast` and `add-on-device-wakeve-ai` retain ownership of their contextual modules. This change routes to or renders their typed outputs without duplicating provider or AI logic.
- `align-ios-android-feature-parity` remains deferred. It must consume these real iOS routes later and must not restore placeholders or a competing Event Detail hierarchy.
- Existing `offline-sync` requirements already cover local reads, queued writes, conflict handling, retry, and visible sync state. This proposal consumes them without changing that capability, so no `offline-sync` delta is added.
- Existing account-deletion security remains authoritative. This change inventories its new pseudonymous and event-scoped records for cascade deletion, retention, and authenticated erasure without adding a competing account-erasure owner.

## Impact

- Affected specs: `event-organization`, `collaboration-management`, `notification-management`, `ios-design-system`.
- Behavioral source of truth: `models/ios-event-invitation-experience.md`.
- Existing presentation dependency: `models/ios-event-detail-invitation-canvas.md`.
- Expected shared impact: event/artwork/revision models, mixed-version compatibility and writer fencing, repository projections and use cases, SQLDelight migrations/cascade rules, direct-invite protected operation state, event notification preference, and contract/integration tests.
- Expected iOS impact: Library, Creation Studio, Audience, Event Detail routing, Information, Archive, navigation/deep-link preflight, localizations, previews, XCTest, XCUITest, and visual QA evidence.
- Breaking behavior: `PAST` non-finalized events become user-visible read-only archive records and cannot be deleted through `DeleteEvent`; a defined authenticated support/privacy remediation path replaces the former UI action for historical data.
- No Android redesign, local-photo picker/upload, PassKit target, payment flow, public discovery, generic social surface, new lifecycle status, or LLM-controlled transition is intended.

## Approval Gate

This proposal is pending human approval. No production implementation or production test change may begin until the proposal and deltas pass strict validation, receive independent review, and the user explicitly approves implementation. After approval, RED tests must be demonstrated before any production code or migration is written.
