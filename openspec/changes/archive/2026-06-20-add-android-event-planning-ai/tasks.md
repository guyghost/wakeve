## 1. Specification
- [x] 1.1 Add OpenSpec deltas for Android event planning AI.
- [x] 1.2 Validate the OpenSpec change with `openspec validate add-android-event-planning-ai --strict`.

## 2. Shared AI Domain
- [x] 2.1 Add `EventPlanDraft` with destination, dates, participant count, budget, event type, constraints, and missing information.
- [x] 2.2 Add `EventPlanningAiAssistant` port with Flow-based extraction and provider availability.
- [x] 2.3 Add fake and rule-based implementations for tests and unsupported devices.
- [x] 2.4 Add JSON parsing and normalization helpers for structured model output.

## 3. Android Provider
- [x] 3.1 Add ML Kit GenAI Prompt API dependency guarded to Android source sets.
- [x] 3.2 Implement `MlKitEventPlanningAiAssistant` with AICore/Gemini Nano status checks and fallback routing.
- [x] 3.3 Ensure no network fallback is introduced and prompt content is not logged.

## 4. ViewModel and UI
- [x] 4.1 Add ViewModel state/actions for text prompt extraction, loading, result, missing fields, and errors.
- [x] 4.2 Add a basic Compose screen for entering natural language event descriptions and reviewing extracted fields.
- [x] 4.3 Wire Android DI so the ViewModel receives the replaceable assistant port.

## 5. Verification
- [x] 5.1 Add unit tests for parsing success and missing fields.
- [x] 5.2 Add unit tests for unsupported device and fallback behavior.
- [x] 5.3 Run focused shared and Compose tests.
