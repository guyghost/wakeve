## 1. Tests First
- [x] 1.1 Add WakeveAI validation tests for `EventDraft`, `DateOption`, `PollSuggestion`, `ChecklistItem`, `TransportHint`, invitation messages, summaries, and transport coordination suggestions.
- [x] 1.2 Add fixture tests for vague, partial, and multilingual FR/EN inputs: beach party, child birthday, friends weekend, family trip, simple dinner, and road trip.
- [x] 1.3 Add tests proving generated arrays are capped at 3 items per category and overlong text is rejected or trimmed by validators.
- [x] 1.4 Add tests proving participants, votes, availability, addresses, prices, and transport facts are rejected when absent from user input or tool context.
- [x] 1.5 Add availability mapping tests for available, Apple Intelligence disabled, model not ready, unsupported device, and unknown unavailable states.
- [x] 1.6 Add ViewModel tests for fallback, timeout, cancellation, streaming section updates, and explicit apply/ignore behavior.
- [x] 1.7 Add fake-client generator tests for Smart Event Draft, poll suggestions, checklist, invitation messages, event summary, and transport helper.

## 2. WakeveAI Foundation
- [x] 2.1 Create `iosApp/src/WakeveAI/` module folders for generators, tools, prompts, validation, instrumentation, and tests.
- [x] 2.2 Implement `WakeveAIAvailabilityService` around `SystemLanguageModel.default.availability` with app-specific availability states.
- [ ] 2.3 Implement `WakeveAIClient` wrapping Foundation Models typed generation, streaming generation, tool registration, cancellation, timeout, metrics, and production-safe logging.
- [x] 2.4 Add small `@Generable` Swift structures and enums for all initial AI outputs.
- [x] 2.5 Implement validators and sanitizers that enforce bounded counts, short text, enum validity, hint semantics, and no-invented-business-fact rules.
- [x] 2.6 Add `WakeveAIMetrics` and `WakeveAILogger` with debug logging disabled for production personal prompt context.

## 3. Prompts and Tools
- [x] 3.1 Add versioned prompts: `event_draft_v1`, `poll_suggestions_v1`, `checklist_v1`, `invitation_message_v1`, `event_summary_v1`, and `transport_suggestions_v1`.
- [x] 3.2 Implement `GetCurrentGroupTool` with sanitized group member context for the selected group.
- [x] 3.3 Implement `GetEventContextTool` with title, date, location, participants, votes, tasks, and recent messages visible to the current user.
- [x] 3.4 Implement `GetParticipantStatusesTool` for accepted, pending, and declined statuses.
- [x] 3.5 Implement `GetVoteResultsTool` for active polls and current results.
- [x] 3.6 Implement `GetTransportContextTool` for proposed trips, concerned participants, and known schedules.
- [x] 3.7 Implement `GetUserPreferencesTool` for local user preferences when available.

## 4. Generators
- [x] 4.1 Implement `EventDraftGenerator` for “Créer avec une phrase” with streaming typed `EventDraft` updates.
- [x] 4.2 Implement `PollSuggestionGenerator` that returns up to 3 reviewable poll suggestions from event context.
- [x] 4.3 Implement `ChecklistGenerator` that returns short actionable items grouped by food, transport, venue, guests, equipment, and budget.
- [x] 4.4 Implement `InvitationMessageGenerator` returning simple, warm, and short WhatsApp variants.
- [x] 4.5 Implement `EventSummaryGenerator` using tools for decided items, missing items, and recommended next action.
- [x] 4.6 Implement `TransportSuggestionGenerator` using transport tools for coordination suggestions and group message drafts.

## 5. ViewModels and UI
- [x] 5.1 Extend `CreateEventViewModel` with Smart Event Draft state, generation, cancellation, fallback, and apply-to-existing-create-event mapping.
- [x] 5.2 Update `CreateEventSheet` with “Décris ton événement”, progressive generation sections, stable streaming layout, review screen, and “Utiliser ce brouillon”.
- [x] 5.3 Add poll suggestion UI behind event context with `Suggestion`, `Modifier`, `Appliquer`, and `Ignorer` actions.
- [x] 5.4 Add checklist suggestion UI with explicit apply/save behavior and no automatic persistence.
- [x] 5.5 Add invitation message UI with three variants and no automatic sending.
- [x] 5.6 Add event summary UI to event detail with decided, missing, and next action sections.
- [x] 5.7 Add transport helper UI to transport planning with coordination suggestions and editable group message draft.
- [x] 5.8 Update `AIBadgeView` and related AI copy to match Wakeve's calm native tone and avoid generic chatbot language.

## 6. Verification
- [x] 6.1 Run OpenSpec validation for `add-on-device-wakeve-ai`.
- [x] 6.2 Run iOS unit tests for WakeveAI validation, generators, and ViewModels.
- [x] 6.3 Run relevant shared/KMP tests if shared DTOs or state-machine contracts are touched. Not applicable: no shared/KMP DTO or state-machine contract was changed in this pass.
- [x] 6.4 Build the iOS app with Foundation Models availability checks enabled.
- [ ] 6.5 Manually verify fallback on an unavailable/incompatible simulator or device.
- [ ] 6.6 Profile generation latency, cancellation, and memory on a real supported device and record results in implementation notes.
- [x] 6.7 Verify no production logs contain prompt text, participant names beyond existing UI scope, votes, addresses, prices, or generated personal content.
