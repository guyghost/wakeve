## 1. OpenSpec
- [x] 1.1 Create deltas for deterministic AI boundaries across affected capabilities.
- [x] 1.2 Validate `add-deterministic-ai-guardrails` with `openspec validate --strict`.

## 2. Shared KMP Contract
- [x] 2.1 Add tests for `AiInteractionMetadata`, `AiValidationResult`, and `AiCostEstimate`.
- [x] 2.2 Add shared metadata models and deterministic validation helpers.
- [x] 2.3 Update `EventPlanDraft`, `EventAiSummary`, `GeneratedOrganizerMessage`, and `PlanningAgentSession` to expose metadata.
- [x] 2.4 Update Android/shared AI tests for metadata presence, rejected output, and routing compatibility.

## 3. Architecture Guardrails
- [x] 3.1 Add source-scan tests that block AI provider SDK imports from domain/state-machine code.
- [x] 3.2 Add source-scan tests that block direct AI provider calls from Compose and SwiftUI views.
- [x] 3.3 Add a critical-release-gate check for deterministic AI boundaries.

## 4. iOS WakeveAI
- [x] 4.1 Add Swift-side AI metadata models matching the shared contract.
- [x] 4.2 Attach metadata to WakeveAI generation state without changing typed generated payloads.
- [x] 4.3 Add WakeveAI tests for metadata, validation disclosure, latency, and production log privacy.

## 5. Verification
- [x] 5.1 Run `openspec validate add-deterministic-ai-guardrails --strict`.
- [x] 5.2 Run `./gradlew :shared:jvmTest`.
- [x] 5.3 Run `xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17' -only-testing:WakeveTests/WakeveAITests` when the simulator toolchain is available.
  - Note: this machine does not have a plain `iPhone 17` simulator. The same WakeveAI suite passed on installed `iPhone 17 Pro`.
- [x] 5.4 Run `RUN_IOS_CONTRACTS=1 ./scripts/test-critical-release-gates.sh` when local iOS contracts are available.
  - Note: ran with `IOS_CONTRACTS_DESTINATION='platform=iOS Simulator,name=iPhone 17 Pro'`.
