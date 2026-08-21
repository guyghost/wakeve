# iOS Event Detail Invitation Canvas Model Review

## Nominal Coverage
- The total matrix covers every lifecycle/access pair and selects exactly one canvas mode.
- DRAFT organizer state is split by structured prerequisite readiness.
- POLLING eligible-participant state is split by current-user vote responsibility.
- CONFIRMED, COMPARING, and ORGANIZING separate organizer, eligible participant, and restricted access.
- FINALIZED has only read-only, access-state, or local details actions.
- Participant counts, actor, readiness, secure share, and hero treatment are orthogonal typed projections and cannot overwrite lifecycle/access mode.

## Error and Missing-Data Coverage
- Base Event loading, terminal error, retry budget, and retry effects are explicitly parent-owned; the canvas does not render without a structured Event.
- Participant, readiness, share, freshness, sync, and image inputs each expose typed unavailable/failed states.
- Missing or failed auxiliary data omits claims rather than fabricating counts, responsibility, readiness, or confirmation.
- Missing/invalid dates use localized absence output and cannot be labeled confirmed unless the structured lifecycle/date model proves it.

## Cancellation and Navigation Coverage
- Back, share dismissal, menu dismissal, and aborted secondary navigation leave Event state unchanged.
- A cancelled image request moves only to loading/fallback presentation.
- Every action is a typed navigation callback; the view never treats button labels as commands.
- Share cancellation never revokes, rotates, regenerates, or logs the server invitation.

## Retry Coverage
- Image retry changes only hero image state.
- Event/repository/network retry is parent-owned; the canvas consumes the resulting typed state and owns no retry loop.
- Sync remains pending until repository acknowledgement and preserves its typed subject.
- Secure invitation requesting/failure/retry is owned by `harden-event-invitation-links`; the canvas only renders its capability state.
- Existing weather and AI retries remain below the canvas and cannot affect its lifecycle/access/action projection.

## Permission Coverage
- The matrix separates organizer, eligible participant, restricted participant, and non-participant for every lifecycle status.
- Permission changes cause complete reprojection and remove newly unauthorized actions, share controls, identities, and sections.
- Restricted and non-participant states cannot access organization details.
- Secure sharing requires both authorized access and `shareCapability == READY`.

## Offline and Sync Coverage
- Pending sync identifies the affected subject and overrides a confirmed claim only for that subject.
- Unknown sync state makes no claim.
- Stale auxiliary data is labeled last-known and cannot be upgraded to current or confirmed.
- Hero imagery has a local event-mood fallback and never blocks the task.
- The canvas never claims that invitation issuance, redemption, membership, or any pending local update succeeded offline.

## Terminal Coverage
- FINALIZED maps only to `FINALIZED_READ_ONLY` or `FINALIZED_RESTRICTED_READ_ONLY`.
- Allowed FINALIZED actions are `VIEW_FINAL_DETAILS`, `SHOW_ACCESS_STATE`, or the safe local `SHOW_DETAILS` fallback.
- No presentation event exits FINALIZED or any other lifecycle state; only the existing business model can do so.

## Dynamic Type and Accessibility Coverage
- Exactly one primary action remains discoverable: in-canvas for standard layouts or persistent safe-area for compact/accessibility layouts.
- Placement branches are mutually exclusive and produce one accessibility element.
- VoiceOver semantic order is explicit and independent from decorative image layering.
- Reduce Motion, Reduce Transparency, and increased contrast change visuals only.
- Verification includes compact height, accessibility Dynamic Type, long localized strings, light/dark mode, increased contrast, landscape, and 44-point targets.

## Secure Share Boundary Review
- The canvas never uses `InvitationTokenCodec` or locally constructs a redeemable URL.
- The share control is hidden or honestly unavailable until the authorized server invitation flow reports a ready payload.
- The active `harden-event-invitation-links` change owns issuance, URL validity, permission, retry, cancellation, and terminal share outcomes.

## AI Boundary Review
- The canvas does not read AI summaries, invitations, or generated suggestions to choose state, permission, responsibility, readiness, sync, sharing, or navigation.
- Reviewable AI content remains a secondary section and requires explicit user actions under its existing deterministic guards.

## Active Change Conflict Review
- `harden-event-invitation-links`: explicit dependency for visible share readiness; no substitute implementation.
- `standardize-product-language`: localization keys and user-facing state/action concepts require reconciliation before archive.
- `add-event-weather-forecast` and `add-on-device-wakeve-ai`: their existing modules stay below the canvas and retain ownership.
- `align-ios-android-feature-parity`: deferred; future work consumes this refactored screen and must not restore the old hierarchy.

## Review Result
The revised model is total and deterministic, guarantees exactly one non-mutating navigation/details action for every rendered canvas, covers nominal, error, missing-data, cancellation, retry, permission, offline, terminal, accessibility, and secure-share cases, and contains no implicit or free-text-driven transition. It is ready for independent re-review and human OpenSpec approval. Implementation remains blocked until that approval.
