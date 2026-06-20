## Context
Wakeve already has Android-side event draft extraction behind `EventPlanningAiAssistant` and iOS-only WakeveAI work behind a separate module. This change adds Android AI workflows that need three different execution profiles: local-only summaries, hybrid local/cloud message generation, and future backend agent sessions.

## Goals / Non-Goals
- Goals: isolate AI SDKs behind replaceable ports, keep sensitive event summary data local, expose hybrid routing decisions internally and in UI state, and model future backend agent events without implementing the backend.
- Goals: use Kotlin coroutines and Flow for generation progress and planning agent events.
- Non-Goals: production ADK backend, A2UI, autonomous client-side planning, automatic persistence of AI suggestions, and cloud fallback for event summaries.

## Decisions
- Decision: Place stable domain models and ports in shared `commonMain` under `com.guyghost.wakeve.ai` so ViewModels and tests can run without Android SDK dependencies.
- Decision: Keep provider adapters small. `OnDeviceEventSummaryAiAssistant` owns local summary generation through an injectable local text generation client; the Android `actual` provider can later wrap ML Kit GenAI Prompt API and Gemini Nano.
- Decision: `HybridOrganizerMessageAiAssistant` owns routing between an on-device message provider and a cloud message provider. It prefers local inference and records route, provider name, and whether cloud was used without logging prompt text or participant details.
- Decision: Use `PlanningAgentClient` as an event-driven client boundary returning session state and `Flow<PlanningAgentEvent>`. The initial `FakePlanningAgentClient` emits deterministic progress, suggestions, missing logistics, and confirmation requests.
- Decision: Keep Compose screens passive. They render ViewModel state and dispatch actions; they do not import or instantiate ML Kit, Firebase AI Logic, Gemini, or backend agent clients.

## Privacy Controls
- Event summaries are local-only. If Gemini Nano is unavailable, the summary assistant returns unavailable or deterministic local fallback state rather than sending event context to the cloud.
- Organizer message cloud fallback is allowed only through the selected hybrid provider route. The route is exposed to ViewModels so UI copy can make cloud usage transparent.
- Production logs record routing, provider, latency bucket, and status only. Prompt text, participant names, addresses, votes, budgets, and generated personal content are not logged.

## Risks / Trade-offs
- ML Kit GenAI Prompt API and Firebase AI Logic APIs may change; isolating them behind Android source-set adapters limits churn.
- Hybrid fallback improves availability but can send event context to cloud for organizer messages; route metadata and UI transparency mitigate this.
- Fake planning-agent events do not prove backend compatibility; the event model should remain narrow and versioned to support a real backend later.

## Migration Plan
1. Add tests for domain ports, routing, unavailable model behavior, and planning-agent UI event projection.
2. Add domain models, ports, fake providers, and implementation shells.
3. Add ViewModel state/actions and the Compose debug/demo screen.
4. Wire Android DI to replaceable providers.
5. Run focused shared and Compose tests.
