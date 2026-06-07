## Context
Apple Foundation Models provide on-device language generation, guided Swift structures through `@Generable`, tool calling, streaming partial responses, and runtime availability via `SystemLanguageModel.default.availability`. Wakeve should use these APIs for private, contextual planning assistance without making AI a critical dependency.

Existing Wakeve architecture separates shared KMP domain/state machines from native UI. The new model integration is Apple-specific, so the Foundation Models client belongs in the iOS app. Shared KMP remains the source for event, participant, vote, scenario, transport, and persistence data.

## Goals
- Keep all model execution on device for the scoped AI features.
- Keep SwiftUI views clean: views observe ViewModels; ViewModels call WakeveAI services.
- Return typed, validated Swift structures instead of free-form JSON.
- Use Wakeve tools for business facts and reject or downgrade output that invents participants, votes, availability, addresses, prices, or existing transport data.
- Provide manual fallbacks for every AI-assisted flow.
- Support streaming, cancellation, timeout, and lightweight performance instrumentation.

## Non-Goals
- No cloud LLM fallback.
- No autonomous sending, event mutation, vote creation, participant invitation, or transport plan selection.
- No replacement of existing scoring algorithms or provider-backed destination/transport recommendations.
- No Android model integration in this change.
- No persistent prompt or model-output analytics containing personal event content.

## Architecture

```text
iosApp/src/WakeveAI/
├── WakeveAIAvailabilityService.swift
├── WakeveAIClient.swift
├── WakeveAIModels.swift
├── WakeveAIValidation.swift
├── Generators/
│   ├── EventDraftGenerator.swift
│   ├── PollSuggestionGenerator.swift
│   ├── ChecklistGenerator.swift
│   ├── InvitationMessageGenerator.swift
│   ├── EventSummaryGenerator.swift
│   └── TransportSuggestionGenerator.swift
├── Tools/
│   ├── GetCurrentGroupTool.swift
│   ├── GetEventContextTool.swift
│   ├── GetParticipantStatusesTool.swift
│   ├── GetVoteResultsTool.swift
│   ├── GetTransportContextTool.swift
│   └── GetUserPreferencesTool.swift
├── Prompts/
│   ├── WakeveAIPrompt.swift
│   └── WakeveAIPromptCatalog.swift
└── Instrumentation/
    ├── WakeveAIMetrics.swift
    └── WakeveAILogger.swift
```

### Data Flow
1. SwiftUI sends user intent to a ViewModel.
2. ViewModel checks `WakeveAIAvailabilityService`.
3. ViewModel starts a generator through `WakeveAIClient`.
4. Generator builds a short versioned prompt and passes only minimal context.
5. Foundation Models may call internal tools for real Wakeve facts.
6. Generator streams typed partial structures where the use case is long-running.
7. Validator clamps counts, validates strings/enums, and rejects fact-inventing output.
8. ViewModel exposes suggestion state with `Suggestion`, `Modifier`, `Appliquer`, and `Ignorer` actions.
9. User confirmation maps accepted output to existing Wakeve event, poll, checklist, message, summary, or transport flows.

## Foundation Models Integration
- `WakeveAIAvailabilityService` reads `SystemLanguageModel.default.availability` and maps it to app states: `available`, `appleIntelligenceDisabled`, `notReady`, `unsupportedDevice`, and `unknownUnavailable`.
- `WakeveAIClient` owns `LanguageModelSession` creation and wraps typed generation, streaming generation, cancellation, timeout, tool registration, metrics, and debug logging.
- `@Generable` types are Swift-only and intentionally small. They use bounded arrays and simple enums to improve generation reliability.
- Prompts are versioned by use case: `event_draft_v1`, `poll_suggestions_v1`, `checklist_v1`, `invitation_message_v1`, `event_summary_v1`, and `transport_suggestions_v1`.
- Long use cases use streaming: Smart Event Draft, weekend planning/checklist, event summary, and transport/lodging coordination suggestions.

## Typed Output
Initial `@Generable` structures:
- `EventDraft`
- `DateOption`
- `PollSuggestion`
- `ChecklistItem`
- `TransportHint`
- `InvitationMessageSet`
- `EventSummary`
- `TransportCoordinationSuggestion`

Validation rules:
- At most 3 date options, poll suggestions, checklist items per category, message variants, and transport hints.
- Short, warm, non-corporate text.
- Date hints remain hints if the user input is vague.
- No exact date, address, price, participant, vote, or availability is accepted unless present in input context or returned by a Wakeve tool.
- Empty required generated fields downgrade to fallback/manual state rather than crashing the flow.

## Internal Tools
Tools are read-only and expose sanitized DTOs:
- `GetCurrentGroupTool`
- `GetEventContextTool`
- `GetParticipantStatusesTool`
- `GetVoteResultsTool`
- `GetTransportContextTool`
- `GetUserPreferencesTool`

Rules:
- Tools return only the current user's authorized event/group scope.
- Tool outputs include stable IDs where needed but avoid direct contact details unless already visible in the current Wakeve UI.
- Tool data is not logged in production.
- A generation that references a business fact not present in prompt input or tool output is rejected or annotated as an uncertain suggestion.

## UX
- AI entry points are contextual and hidden when unavailable or irrelevant.
- Smart Event Draft appears in create event as “Décris ton événement” with progressive sections and a review screen.
- Event summaries appear on event detail only when there is meaningful state to summarize.
- Transport helper appears in transport planning only after enough event/participant context exists.
- Every AI result shows “Suggestion” and explicit `Modifier`, `Appliquer`, and `Ignorer` controls.
- The UI never sends messages, creates votes, invites participants, selects transport plans, or edits events automatically.

## Performance and Privacy
- Do not load the model at app launch.
- Prepare or warm only when the user opens create event, starts typing a free-form event phrase, or opens an event with missing actions.
- Measure generation duration, timeout, cancellation, availability state, and validation result without storing personal prompt text.
- Use a reasonable timeout per generator and expose cancellation to ViewModels.
- Debug prompt logging must be compile-time or runtime disabled for production builds.
- AI output is transient until the user explicitly applies or saves it.

## Testing
- Unit tests for availability mapping, prompt selection, output validation, fallback behavior, bounded arrays, and no-fact-invention checks.
- Generator tests use deterministic fake `WakeveAIClient` responses rather than relying on the live model.
- Fixtures cover beach party, child birthday, friends weekend, family trip, simple dinner, and road trip in French and English.
- ViewModel tests cover unavailable model fallback, cancellation, timeout, streaming section updates, and explicit apply/ignore behavior.
- Manual real-device profiling is required before marking performance tasks complete.
