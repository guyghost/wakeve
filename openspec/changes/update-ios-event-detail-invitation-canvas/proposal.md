# Change: Update iOS event detail to an invitation canvas

## Why
The current iOS Event Detail screen contains the required planning information, but it presents too many modules at similar visual priority. The selected “Affiche vivante” direction makes the event itself the visual anchor and puts the confirmed state, pending participation, responsible actor, and next useful action above secondary organization content.

## What Changes
- Introduce a deterministic Event Detail presentation model under `models/` that projects existing event, permission, participant, and sync inputs into hero status and next-action content.
- Recompose the primary iOS Event Detail viewport as an immersive invitation canvas with native navigation controls, event imagery or a mood-gradient fallback, title/date/status, organizer context, participant state, and one prominent next-action surface.
- Keep weather, anticipation, AI suggestions, readiness, organization routes, messages, and other secondary content available through progressive disclosure below the primary canvas.
- Use native iOS 26+ Liquid Glass for meaningful elevated controls and the next-action surface, with existing accessible material/opaque fallbacks for earlier iOS versions and accessibility settings.
- Gate the visible share control behind the authorized server-issued invitation capability defined by `harden-event-invitation-links`; the canvas never constructs a token or URL.
- Keep exactly one primary action visible through an adaptive in-canvas or persistent safe-area placement on small screens and at accessibility Dynamic Type sizes.
- Add localized copy and contract, presentation-model, accessibility, snapshot, and visual design-QA coverage.
- Preserve all existing workflow, repository, access-control, offline, and AI reviewability boundaries. The redesigned view never decides an event transition.

## Product Excellence Fit
The change helps a private group understand and continue one event without scanning a dashboard. It reduces mental load by showing the event’s decision state, outstanding responses, responsible actor, and next action together. The primary action remains reachable on a phone, while secondary planning tools stay event-scoped and progressively disclosed. The design does not add a social feed, generic chat, task manager, calendar workspace, or any new AI authority.

## Impact
- Affected specs: `ios-design-system`
- Behavioral model: `models/ios-event-detail-invitation-canvas.md`
- Design source: `openspec/changes/update-ios-event-detail-invitation-canvas/assets/option-1-invitation-canvas.png`
- Expected iOS code:
  - `iosApp/src/Views/App/ContentView.swift`
  - `iosApp/src/Components/DesignSystem/**`
  - `iosApp/src/Theme/**`
  - `iosApp/src/Resources/*.lproj/Localizable.strings`
  - `iosApp/src/Preview/**`
  - iOS contract/snapshot/UI tests under `iosApp/WakeveTests` and `iosApp/iosAppUITests`
- No shared-domain, database, backend, Android, or event-state-machine change is intended.
- Related active changes: secure sharing depends on `harden-event-invitation-links`; localization must coordinate with `standardize-product-language`; existing weather and reviewable AI modules remain owned by `add-event-weather-forecast` and `add-on-device-wakeve-ai`; the deferred parity change must consume this refactored surface later rather than duplicate it.
