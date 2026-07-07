# Change: Add On-Device WakeveAI

## Why
Wakeve can reduce organizer effort by turning vague social plans into structured, reviewable planning suggestions. Apple Foundation Models allow this assistance to run privately on supported iOS devices while keeping the app fully usable without Apple Intelligence.

## What Changes
- Add an isolated iOS `WakeveAI` module built around Apple Foundation Models for typed generation, tool calling, streaming, cancellation, availability checks, and performance measurement.
- Add Smart Event Draft, poll suggestions, checklist generation, invitation message generation, event summaries, and transport coordination suggestions.
- Add internal tools that expose real Wakeve event, group, participant, vote, transport, and preference context to the model without allowing it to invent business facts.
- Add ViewModel-facing services so SwiftUI views consume stable state and never call Foundation Models directly.
- Add fallbacks and subtle UX states for unavailable, disabled, not-ready, or incompatible Apple Intelligence conditions.
- Add validation, fixtures, and tests for typed output bounds, vague inputs, partial inputs, multilingual prompts, missing model availability, no fact invention, maximum suggestion counts, cancellation, and timeout behavior.

## Approval Checklist
- Approve the first release platform scope: iOS-only on-device Foundation Models, with Android and shared KMP receiving only non-model contracts/fallback-compatible state where needed.
- Approve the privacy stance: no server fallback for these AI features, no production logging of personal prompt context, and no persistence of AI output until explicit user action.
- Approve the UX rule that every AI result is labeled as a suggestion and requires an explicit user action before creating, sending, inviting, modifying, or applying anything.
- Approve that existing server-side `suggestion-engine` remains the source for scored external recommendations while `WakeveAI` handles local drafting, summarization, and coordination copy.
- Approve the implementation gate that Foundation Models APIs are compiled behind OS/SDK availability checks and a manual fallback remains available for all flows.

## Impact
- Affected specs: `wakeve-ai`, `event-organization`, `suggestion-engine`, `transport-optimization`, `ios-design-system`
- Affected code:
  - `iosApp/src/WakeveAI/**`
  - `iosApp/src/ViewModels/CreateEventViewModel.swift`
  - `iosApp/src/ViewModels/EventDetailViewModel.swift`
  - `iosApp/src/ViewModels/TransportPlanningViewModel.swift`
  - `iosApp/src/Views/Events/CreateEventSheet.swift`
  - `iosApp/src/Views/Events/TransportPlanningView.swift`
  - `iosApp/src/Components/AIBadgeView.swift`
  - `iosApp/src/Models/AISuggestionModels.swift`
  - `iosApp/src/Services/DebugLogging.swift`
  - `shared/src/commonMain/kotlin/com/guyghost/wakeve/models/*` only if shared DTOs are required for ViewModel bridging
  - `iosApp/*Tests*/WakeveAITests/**`
