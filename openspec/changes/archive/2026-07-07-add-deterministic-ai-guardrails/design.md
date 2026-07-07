## Context
Wakeve has shared KMP AI ports for Android workflows and an isolated iOS WakeveAI module for Apple Foundation Models. Existing specs already require reviewable suggestions and no automatic persistence, but the guarantees are not yet expressed as a shared metadata contract or release-gated architecture boundary.

## Goals / Non-Goals
- Goals:
  - Keep business state deterministic and owned by existing models, state machines, validators, and repositories.
  - Attach explicit, sanitized AI interaction metadata to AI outputs that can influence user-visible event work.
  - Keep provider SDKs in edge adapters and out of deterministic domain/state-machine code.
  - Make architecture constraints testable through unit/source-scan tests and the critical release gate.
- Non-Goals:
  - Do not remove existing on-device WakeveAI or Android hybrid message fallback.
  - Do not introduce a new AI provider, database table, migration, telemetry backend, or autonomous agent runtime.
  - Do not require exact text matching for AI output; validate constraints and metadata instead.

## Decisions
- Decision: Add shared KMP metadata models in `com.guyghost.wakeve.ai`.
  - Rationale: Android/shared flows already consume these models, and they provide the canonical contract for future providers.
- Decision: Mirror the metadata shape in Swift for iOS Foundation Models output.
  - Rationale: iOS generation internals are Swift-only; a mirrored shape avoids forcing `@Generable` payloads through Kotlin/Native while contract tests keep behavior aligned.
- Decision: Treat AI validation as deterministic core logic.
  - Rationale: Validation must not depend on model behavior and must be testable without provider SDKs.
- Decision: Add source-scan guardrails rather than a new static-analysis dependency.
  - Rationale: The repo already uses shell release gates, and a lightweight scan is enough to prevent provider SDK leakage.

## Risks / Trade-offs
- Risk: Metadata fields could become noisy in compact mobile UI.
  - Mitigation: Store/expose full metadata in state, but render concise disclosures.
- Risk: Source scans can produce false positives.
  - Mitigation: Scope scans to deterministic packages and view files, and keep allowed AI edge directories explicit.
- Risk: Existing outputs require compatibility changes.
  - Mitigation: Keep `AiRoutingMetadata` and add richer metadata alongside it.

## Migration Plan
1. Add OpenSpec deltas and validate the proposal.
2. Add failing shared/iOS tests for metadata, validation, and architecture boundaries.
3. Add KMP metadata models and update current AI outputs.
4. Add Swift metadata mirror and attach metadata in WakeveAI generators/ViewModels.
5. Add release-gate scans and run focused verification.

## Open Questions
- None. Provider-specific cost values may remain `unknown` unless the provider exposes reliable telemetry.
