# Change: Add Android Intelligent AI Workflows

## Why
Wakeve Android needs AI assistance for event summaries, organizer messages, and complex planning workflows while preserving privacy, latency, and provider replaceability. Existing AI work covers event draft extraction; this change adds separate summary, message, and planning-agent boundaries.

## What Changes
- Add shared AI domain models for event prompt context, summaries, generated organizer messages, planning agent sessions, and planning agent events.
- Add domain ports for event summaries, organizer messages, and planning agent sessions so UI code never calls ML Kit, Firebase AI Logic, Gemini, or future backend agent SDKs directly.
- Add Android-ready implementations for on-device event summaries, hybrid organizer message generation, and a fake local planning agent client.
- Add ViewModel state and a Compose debug/demo screen that shows generation results, routing transparency, planning progress, and requested user confirmations.
- Add fake providers and unit tests for local success, hybrid fallback, unavailable on-device model handling, and planning-agent event rendering.

## Impact
- Affected specs: `android-ai-workflows`
- Affected code:
  - `shared/src/commonMain/kotlin/com/guyghost/wakeve/ai/**`
  - `shared/src/androidMain/kotlin/com/guyghost/wakeve/ai/**`
  - `shared/src/commonTest/kotlin/com/guyghost/wakeve/ai/**`
  - `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/viewmodel/**`
  - `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/ai/**`
  - `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/di/**`
  - `composeApp/src/commonTest/kotlin/com/guyghost/wakeve/ui/ai/**`
