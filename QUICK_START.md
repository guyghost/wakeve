# Wakeve Quick Start

This guide gets a developer from a fresh checkout to the current local validation baseline. For roadmap status, use [ROADMAP.md](./ROADMAP.md). For specification workflow, use [openspec/AGENTS.md](./openspec/AGENTS.md).

## Current Status

Wakeve is in active release hardening, not the old Phase 2/Phase 3 plan.

Current active OpenSpec changes:

| Change | Status | Remaining blocker |
|---|---:|---|
| `add-event-weather-forecast` | 20/22 tasks | WeatherKit Apple Developer capability, signed entitlement, and physical-device WeatherKit validation. |
| `add-on-device-wakeve-ai` | 40/41 tasks | Supported physical-device profiling for Foundation Models latency, cancellation, memory, and production-log privacy. |

Recent archived work includes Android AI workflows, Android adaptive UI, account deletion, UGC moderation, web microfrontends, contact participant selection, scenario matrix voting, iOS naming/brand cleanup, and create-event slot previews.

## Prerequisites

Required:

```bash
java -version
./gradlew --version
```

For Android:

```bash
adb devices
```

For iOS:

```bash
xcodebuild -version
xcrun simctl list devices available
```

For web/App Store evidence tooling:

```bash
pnpm --version
bundle --version
```

## First Build

```bash
git clone https://github.com/guyghost/wakeve.git
cd wakeve

./gradlew build
```

If the full build is too broad for the task at hand, run the focused gates below.

## Core Validation

```bash
openspec validate --all --strict
./gradlew :shared:jvmTest
./gradlew :composeApp:assembleDebug
```

Useful release-oriented gates:

```bash
./scripts/test-critical-release-gates.sh
APP_REVIEW_PHONE_NUMBER='+33123456789' ./scripts/lint-store-metadata.sh --ios-only
```

The release gates intentionally keep external blockers open until real evidence exists. Do not mark WeatherKit, WakeveAI device profiling, App Store live URL, signed archive, TestFlight, or App Store Connect tasks complete from simulator-only evidence.

## Run the Backend

```bash
./gradlew :server:run
```

The local server starts at:

```text
http://localhost:8080
```

Health check:

```bash
curl http://localhost:8080/health
```

## Android

Build:

```bash
./gradlew :composeApp:assembleDebug
```

Install on a connected device or emulator:

```bash
./gradlew :composeApp:installDebug
```

Run Android unit tests where available:

```bash
./gradlew :composeApp:testDebugUnitTest
```

## iOS

Open the Xcode project:

```bash
open iosApp/iosApp.xcodeproj
```

Build/test from Xcode or with XcodeBuildMCP when available. The current desktop session uses:

- Project: `iosApp/iosApp.xcodeproj`
- Scheme: `WakeveApp`
- Simulator: `iPhone 17`

Focused WeatherKit/WakeveAI simulator test selection:

```bash
xcodebuild test \
  -project iosApp/iosApp.xcodeproj \
  -scheme WakeveApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  -only-testing:WakeveTests/EventWeatherProviderTests \
  -only-testing:WakeveTests/EventWeatherMapCardContractTests \
  -only-testing:WakeveTests/PremiumEventDetailContractTests/testEventDetailWeatherCardUsesWeatherKitAndMapKit \
  -only-testing:WakeveTests/WakeveAIContractTests \
  -only-testing:WakeveTests/WakeveAIGeneratorTests \
  -only-testing:WakeveTests/WakeveAIValidationTests
```

The latest XcodeBuildMCP focused run on 2026-06-20 passed `42/42` selected simulator tests. This does not close physical-device WeatherKit or WakeveAI profiling tasks.

## Project Layout

```text
wakeve/
├── shared/       Kotlin Multiplatform domain, repositories, services, tests
├── composeApp/   Android Jetpack Compose app
├── iosApp/       Native SwiftUI iOS app and Xcode project
├── server/       Ktor backend
├── apps/         Web/public surfaces
├── docs/         Product, release, architecture, and evidence documentation
├── openspec/     Specifications, active changes, and archived changes
└── scripts/      Release gates, audits, profiling helpers, evidence capture
```

## OpenSpec Workflow

Before implementing a meaningful feature or architectural/security/performance change:

```bash
openspec list
openspec list --specs
openspec show <change-id>
openspec validate <change-id> --strict
```

Use a new OpenSpec proposal for new capabilities, breaking changes, architecture shifts, security changes, and behavior-changing performance work. Bug fixes, docs updates, tests for existing behavior, and minor configuration cleanup can usually be done directly.

## Commit Format

Use Conventional Commits:

```text
feat(scope): add capability
fix(scope): correct behavior
test(scope): add regression coverage
docs(scope): update documentation
chore(scope): update tooling
```

Examples:

```text
docs(roadmap): refresh current project status
test(ios): refresh weather and ai validation evidence
fix(weather): map WeatherKit entitlement failures
```

## Common Troubleshooting

Clean Gradle build artifacts:

```bash
./gradlew clean
```

Run a focused shared test:

```bash
./gradlew :shared:jvmTest --tests "com.guyghost.wakeve.workflow.DraftWorkflowIntegrationTest"
```

Check active roadmap work:

```bash
openspec list
sed -n '1,120p' ROADMAP.md
```

Check App Store/release blockers:

```bash
sed -n '1,220p' docs/APP_STORE_BLOCKER_REGISTER.md
sed -n '1,220p' docs/APP_STORE_READINESS.md
```

## Next Best Work

The current roadmap priority is:

1. Close external App Store/live infrastructure blockers with real evidence.
2. Validate WeatherKit on a signed physical-device build.
3. Profile WakeveAI on a supported physical device with Foundation Models available.
4. Keep critical release flows green while avoiding broad new scope before release signoff.
