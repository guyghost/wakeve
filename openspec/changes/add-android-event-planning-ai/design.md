## Context
ML Kit GenAI Prompt API provides customizable text generation on Gemini Nano through Android AICore. The API is beta, requires Android API 26+, and exposes feature status states such as available, downloadable, downloading, and unavailable before inference. Wakeve's minSdk is 24, so the Android provider must be guarded and fallback-capable.

## Goals / Non-Goals
- Goals: local-first prompt extraction, replaceable provider, structured `EventPlanDraft`, fallback behavior, Flow-based ViewModel state, and no UI-level AI API calls.
- Goals: text input now, speech-ready request metadata for future transcription.
- Non-Goals: LiteRT-LM integration, cloud/server fallback, automatic event persistence, and direct voice capture in this change.

## Decisions
- Decision: Put the domain port and result model in shared `commonMain`, with Android-only ML Kit implementation in `androidMain`.
- Decision: Use ML Kit GenAI Prompt API for custom entity extraction because the use case needs flexible structured extraction rather than fixed summarization/proofreading/rewriting.
- Decision: Ask the model for compact JSON and parse it with kotlinx.serialization, then normalize and validate through common code.
- Decision: Wrap ML Kit status failures, quota errors, parsing failures, and unavailable devices in a fallback assistant that keeps the feature usable without network calls.
- Decision: Keep generated plans transient until the user explicitly applies them to event creation.

## Risks / Trade-offs
- Prompt API is beta and can change; mitigate by isolating all ML Kit references inside `MlKitEventPlanningAiAssistant`.
- Device/language support is limited and evolving; mitigate with deterministic rule-based extraction and UI status copy that explains fallback use.
- Model JSON can be malformed; mitigate with strict JSON extraction, validation, and fallback.

## Migration Plan
1. Add shared contracts and tests.
2. Add Android provider and Gradle dependency.
3. Add ViewModel and Compose test screen.
4. Validate OpenSpec and run focused tests.

## Open Questions
- None for the first text-input implementation. Speech input will be specified separately when capture UX and permissions are in scope.
