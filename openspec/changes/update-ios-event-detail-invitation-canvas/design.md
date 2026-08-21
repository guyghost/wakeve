## Context
Wakeve already specifies an immersive iOS Event Detail hierarchy and already exposes the required Event lifecycle, access mappers, next-action mapping, event mood palette, hero image URL, and Liquid Glass-compatible components. The selected visual target is the first generated option, stored at `assets/option-1-invitation-canvas.png`.

The work is a presentation refactor, not a workflow change. `EventStatus`, repository data, participant access mapping, and existing navigation callbacks remain authoritative.

## Goals / Non-Goals
- Goals:
  - Match the selected invitation-canvas composition inside the existing native SwiftUI app.
  - Make status, pending participation, actor, and next action legible in the first viewport.
  - Preserve all current Event Detail destinations and access gates below the fold.
  - Support a real event hero image with an event-mood fallback.
  - Support iOS 26+ Liquid Glass and accessible fallbacks.
- Non-Goals:
  - Add or change Event lifecycle transitions.
  - Infer lodging, transport, attendance, or readiness from prose or an LLM.
  - Add a new social, chat, task, calendar, or workspace surface.
  - Redesign Android or the cross-platform domain model.
  - Make “Annecy”, “Camille”, or the mock participant portraits production defaults.

## Model
The source-of-truth presentation model and invariant review live in:
- `models/ios-event-detail-invitation-canvas.md`
- `models/ios-event-detail-invitation-canvas.review.md`

The SwiftUI implementation SHALL consume a total pure presentation projection derived from existing structured inputs. The projection first selects exactly one lifecycle/access mode, then applies orthogonal image, sync, and accessibility decorations. Display text is localized output of that projection; it is never parsed to choose a state or destination.

## Decisions
- Decision: Keep `EventStatus` and repository permissions authoritative.
  - The view receives structured state and maps it to a presentation value. It does not instantiate or dispatch a business state machine.
- Decision: Require typed responsibility, readiness, and navigation inputs.
  - Current-user vote responsibility, readiness items, the responsible actor, and the action destination are structured inputs. `EventNextAction` display strings are insufficient to select an actor or destination and are never parsed.
- Decision: Make one invitation canvas the first viewport.
  - The event image, title, date/status, organizer, participant summary, and next action form one continuous branded content surface. Native back/share/menu controls remain platform chrome.
- Decision: Use `event.heroImageUrl` when present.
  - Loading, missing, invalid, or offline images fall back to `EventMoodPalette`; they never block the event action.
- Decision: Keep secondary modules in the existing scroll hierarchy after the invitation canvas.
  - Weather, anticipation, AI review, readiness, organization rows, and messages remain reachable without competing with the primary action above the fold.
- Decision: Use Liquid Glass selectively.
  - Native iOS 26+ glass is used for tappable circular controls and the next-action surface. Earlier versions and Reduce Transparency use existing material/opaque fallbacks. Multiple glass elements are grouped when native APIs are available.
- Decision: Preserve one visible primary action under accessibility reflow.
  - Standard sizes place the action inside the next-action surface. Compact-height devices and accessibility Dynamic Type move that same action to a persistent safe-area inset. Both branches are mutually exclusive.
- Decision: Preserve SF Symbols for standard actions.
  - Back, share, overflow, participant, and action symbols remain native. Event imagery is raster content, not code-drawn art.
- Decision: Treat the selected image as visual direction, not fixture data.
  - Production titles, dates, organizer names, participant counts, and actions come from repositories and localization. Preview/snapshot fixtures may use Annecy data to compare against the target.
- Decision: Delegate sharing to the secure invitation model.
  - The share control consumes a typed `shareCapability` and emits only the authorized callback owned by `harden-event-invitation-links`. It never calls `InvitationTokenCodec`, constructs a URL, or shares a locally derived token. Until the secure capability is available, production UI hides or honestly disables the share control.
- Decision: Keep base loading/error/retry ownership in the parent.
  - The canvas is rendered only with a structured Event. The parent owns event loading, repository retry, and terminal load errors. Auxiliary participant/readiness/share inputs expose typed loading/available/unavailable/failed states; the canvas omits claims it cannot prove and never initiates a business retry.

## Presentation Mapping
The exclusive lifecycle/access matrix is defined in `models/ios-event-detail-invitation-canvas.md`. It covers every `eventStatus × currentUserAccess` pair before vote, readiness, sync, share, image, or accessibility substate is applied.

Typed action destinations are limited to `editDraft`, `submitVote`, `viewPollResults`, `compareOptions`, `continueOrganization`, `viewFinalDetails`, `showAccessState`, and the always-safe local `showDetails` fallback. Canvas taps navigate to the owning flow or scroll to permitted details; the canvas never performs the business mutation itself. `FINALIZED` can emit only `viewFinalDetails`, `showAccessState`, or `showDetails`.

## Risks / Trade-offs
- Remote hero imagery may be unavailable offline.
  - Mitigation: immediate mood-gradient fallback with identical text contrast and action placement.
- Large Dynamic Type can push the action below the first viewport.
  - Mitigation: mutually exclusive adaptive placement moves the same action to a persistent safe-area inset, avoids fixed semantic heights, caps only decorative imagery, and is tested on compact-height devices and accessibility sizes.
- A faithful canvas can hide secondary features too aggressively.
  - Mitigation: preserve the complete current section order below the hero and add a clear localized “Details” affordance that scrolls to it.
- Native Liquid Glass is OS-version dependent.
  - Mitigation: gate native APIs with availability checks and preserve existing fallbacks for earlier iOS and accessibility settings.
- Contract tests currently assert the existing component inventory.
  - Mitigation: update them test-first to assert the new hierarchy while retaining every business/access boundary.
- Secure invitation issuance is not yet implemented.
  - Mitigation: keep the production share capability hidden or explicitly unavailable until `harden-event-invitation-links` provides an authorized server-issued URL callback; snapshot fixtures may inject a non-production ready capability without a real token.

## Active Change Coordination
1. `harden-event-invitation-links` owns issuance, permission, server URL readiness, and share retry/error states. This canvas consumes its future typed callback and must not implement a substitute.
2. `standardize-product-language` owns canonical user-visible state and action vocabulary. New canvas localization keys must follow or be reconciled with that model before either change is archived.
3. `add-event-weather-forecast` and `add-on-device-wakeve-ai` retain ownership of their existing below-canvas modules and tests.
4. `align-ios-android-feature-parity` is deferred. When resumed, it must route from the refactored Event Detail rather than restore or duplicate the old first viewport.

## Migration Plan
1. Add failing model and hierarchy contract tests.
2. Add the pure presentation projection and invitation-canvas components.
3. Replace only the first Event Detail viewport while retaining downstream sections and callbacks.
4. Add preview/snapshot fixtures for image success, image fallback, every access group, secure-share capability, compact height, Dynamic Type, increased contrast, long localization, landscape, VoiceOver order, and pending sync.
5. Compare the rendered iOS screenshot with the selected source and iterate through design QA.
6. Roll back by restoring the previous first-viewport composition; no data migration is required.

## Open Questions
- None blocking proposal review. Exact production copy remains localized and is validated by the presentation model tests.
