## 1. Model and Test Baseline
- [x] 1.1 @tests Add failing table-driven tests for every lifecycle/access pair across DRAFT, POLLING, CONFIRMED, COMPARING, ORGANIZING, and FINALIZED, plus vote responsibility, readiness, typed destinations, missing/failed auxiliary data, subject-specific pending sync, and proof that every rendered canvas has exactly one non-mutating navigation/details action.
- [x] 1.2 @tests Update Event Detail hierarchy contracts to require the invitation canvas, participant-state summary, responsible actor, one primary action, progressive details, and preserved access-control boundaries.
- [x] 1.3 @review Review `models/ios-event-detail-invitation-canvas.md` and its invariant review against nominal, error, cancellation, retry, permission, offline, and terminal cases before code generation.

## 2. iOS Implementation
- [x] 2.1 @codegen Implement a total pure Swift Event Detail presentation mapper that consumes existing structured domain/repository inputs and emits one exclusive canvas mode, localized presentation keys, responsible actor, and typed navigation action.
- [x] 2.2 @codegen Build the immersive event hero with `heroImageUrl`, mood-gradient loading/error/offline fallback, readable scrim, Dynamic Type, and Reduce Motion/Transparency support.
- [x] 2.3 @codegen Build the organizer and participant-state treatment without fixture names or portrait defaults; retain initials/accessibility fallbacks.
- [x] 2.4 @codegen Build the next-action Liquid Glass surface and native circular controls with iOS 26+ APIs, grouped glass where appropriate, 44pt hit targets, mutually exclusive adaptive primary-action placement, and earlier-iOS fallbacks.
- [x] 2.5 @codegen Recompose `EventDetailView` so the invitation canvas leads and all existing weather, anticipation, AI review, readiness, organization, invitation, message, and access-gated content remains progressively available below it.
- [x] 2.6 @codegen Add localized copy for supported locales and preview/snapshot fixtures matching the selected Annecy visual state.
- [x] 2.7 @codegen Gate sharing behind the typed authorized server-issued invitation capability; remove any invitation-canvas dependency on `InvitationTokenCodec` or locally constructed URLs.
- [x] 2.8 @docs Document the invitation-canvas pattern, its native/branded layer boundary, image fallback, secure-share dependency, adaptive action placement, and state-clarity rules in the iOS design guidance.

## 3. Verification
- [x] 3.1 @tests Run model, iOS contract, localization, access-control, offline/pending-sync, secure-share, compact-height, landscape, Dynamic Type, increased contrast, Reduce Motion, Reduce Transparency, and VoiceOver-order tests.
- [x] 3.2 @tests Build the iOS app for an available simulator destination and capture the selected Event Detail fixture at the target viewport.
- [x] 3.3 @designer Compare the selected source image and simulator capture for hierarchy, crop, typography, spacing, colors, glass, image quality, and copy.
- [x] 3.4 @codegen Fix all P0/P1/P2 fidelity findings without moving business decisions out of the presentation model.
- [x] 3.5 @review Verify that no workflow transition, access rule, sync claim, or AI authority was introduced in the view.
- [x] 3.6 @tests Run `openspec validate update-ios-event-detail-invitation-canvas --strict` and relevant regression suites.
- [x] 3.7 @review Confirm `design-qa.md` records the source, implementation capture, comparison history, and `final result: passed` before handoff.
