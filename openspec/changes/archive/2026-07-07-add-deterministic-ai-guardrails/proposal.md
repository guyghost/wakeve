# Change: Add Deterministic AI Guardrails

## Why
Wakeve already uses AI for drafting, summarizing, message generation, and planning assistance. The missing cross-cutting contract is that every AI surface must remain replaceable and reviewable while deterministic domain models, state machines, typed validators, and tests remain the source of truth.

## What Changes
- Add project-wide AI boundary requirements: AI may draft, summarize, classify, rewrite, recommend, or explain, but it must not decide permissions, lifecycle transitions, invitations, payments, sync/conflict behavior, notifications, or persistence.
- Add a shared AI interaction metadata contract with routing, sanitized disclosure, confidence, reasoning summary, latency, validation result, and optional cost/usage.
- Require current Android/shared AI outputs and iOS WakeveAI outputs to expose review metadata without moving provider internals into domain code.
- Add architecture guardrails that block AI provider SDKs from deterministic domain/state-machine packages and block direct provider calls from UI views.
- Clarify that suggestion scoring and workflow application stay deterministic; AI can only generate bounded copy or proposals from existing facts.

## Product Excellence Fit
This change helps private groups trust Wakeve event planning because AI suggestions remain transparent, reviewable, and subordinate to confirmed event state. It reduces organizer effort by preserving useful drafting and summarization while making pending, confirmed, and rejected AI output explicit. It keeps mobile flows understandable by requiring concise metadata and explicit apply/ignore decisions, and avoids generic assistant drift by limiting AI to event-scoped preparation, coordination, and explanation.

## Impact
- Affected specs: `wakeve-ai`, `android-ai-workflows`, `workflow-coordination`, `suggestion-engine`, `product-excellence`
- Affected code:
  - `shared/src/commonMain/kotlin/com/guyghost/wakeve/ai/**`
  - `shared/src/commonTest/kotlin/com/guyghost/wakeve/ai/**`
  - `shared/src/jvmTest/kotlin/com/guyghost/wakeve/ai/**`
  - `iosApp/src/WakeveAI/**`
  - `iosApp/WakeveTests/WakeveAITests/**`
  - `scripts/test-critical-release-gates.sh`
