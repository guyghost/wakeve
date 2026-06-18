## 1. Specification
- [x] 1.1 Add OpenSpec proposal, design, and requirement deltas for Android intelligent AI workflows.
- [x] 1.2 Validate the OpenSpec change with `openspec validate add-android-intelligent-ai-workflows --strict`.

## 2. Tests First
- [x] 2.1 Add shared tests for on-device event summary success and unavailable-model behavior.
- [x] 2.2 Add shared tests for hybrid organizer message local routing and cloud fallback routing.
- [x] 2.3 Add shared tests for fake planning-agent session events and confirmation requests.
- [x] 2.4 Add ViewModel/UI projection tests for planning-agent event rendering.

## 3. Domain Models and Ports
- [x] 3.1 Add `EventPlanningPromptContext`, `EventAiSummary`, `GeneratedOrganizerMessage`, `PlanningAgentSession`, and `PlanningAgentEvent`.
- [x] 3.2 Add `EventSummaryAiAssistant`, `OrganizerMessageAiAssistant`, and `PlanningAgentClient` ports.
- [x] 3.3 Add provider-neutral routing, availability, privacy, and message-type models needed by the ports.

## 4. Providers
- [x] 4.1 Implement `OnDeviceEventSummaryAiAssistant` with compact prompt construction and no cloud fallback.
- [x] 4.2 Implement `HybridOrganizerMessageAiAssistant` with local-first routing and cloud fallback metadata.
- [x] 4.3 Implement `FakePlanningAgentClient` with deterministic progress, recommendations, missing logistics, task splits, budget categories, and user confirmation requests.
- [x] 4.4 Add fake summary and message providers for tests.

## 5. ViewModel and UI
- [x] 5.1 Add ViewModel integration for summary generation, message generation, and planning-agent sessions.
- [x] 5.2 Add a Compose debug/demo AI workflow screen that renders summaries, messages, route transparency, agent progress, and confirmation requests.
- [x] 5.3 Wire Android DI so the ViewModel receives replaceable AI ports.

## 6. Verification
- [x] 6.1 Run focused shared tests for `com.guyghost.wakeve.ai`.
- [x] 6.2 Run focused Compose/ViewModel tests for the AI workflow screen.
- [x] 6.3 Verify OpenSpec remains valid after implementation.
