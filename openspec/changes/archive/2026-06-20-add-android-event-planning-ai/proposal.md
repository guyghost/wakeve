# Change: Add Android Event Planning AI

## Why
Wakeve Android needs a private, on-device assistant that turns natural language event descriptions into structured event planning drafts. Gemini Nano through ML Kit GenAI can handle the happy path locally on supported devices, while unsupported devices still need a deterministic fallback.

## What Changes
- Add a shared event planning AI domain model and replaceable assistant port.
- Add an Android ML Kit GenAI Prompt API provider using Gemini Nano through AICore when available.
- Add a rule-based fallback provider for unsupported, unavailable, downloading, or failed model states.
- Add ViewModel state and a basic Compose testing screen for text-first event draft extraction.
- Prepare the port for future speech input by keeping input modality metadata separate from extraction logic.
- Add unit tests for successful parsing, missing fields, unsupported devices, and fallback behavior.

## Impact
- Affected specs: `wakeve-ai`, `event-organization`
- Affected code:
  - `shared/src/commonMain/kotlin/com/guyghost/wakeve/ai/**`
  - `shared/src/androidMain/kotlin/com/guyghost/wakeve/ai/**`
  - `shared/src/commonTest/kotlin/com/guyghost/wakeve/ai/**`
  - `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/viewmodel/**`
  - `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/ai/**`
  - `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/di/**`
  - `gradle/libs.versions.toml`, `shared/build.gradle.kts`
