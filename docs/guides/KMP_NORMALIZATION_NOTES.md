# KMP Normalization Notes

This document records the repository normalization performed for module/path consistency.

## Module Naming

- Android app Gradle module: `composeApp`
- Shared KMP module: `shared`
- Backend module: `server`
- Web module: `apps/landing`

## Path Mapping

Use these mappings when updating docs, scripts, or local workflows:

- `wakeveApp/...` -> `composeApp/...`
- `wakeveApp:<task>` -> `:composeApp:<task>`
- `iosApp/src/...` -> `iosApp/src/...` (no change)
- `iosApp/iosApp.xcodeproj` -> `iosApp/iosApp.xcodeproj` (no change)

## Canonical Build Commands

From repository root:

```bash
./gradlew build
./gradlew shared:test
./gradlew :composeApp:assembleDebug
./gradlew server:run
```

For iOS project inspection:

```bash
xcodebuild -list -project iosApp/iosApp.xcodeproj
```

## Legacy Artifacts

Backup/suppressed sources (`*.bak`, `*.backup`, `*.disabled`) have been removed from tracked source trees and are now ignored via `.gitignore`.

## Intentional Leftovers

- Historical reports and archived notes may still mention `composeApp` or `iosApp` naming.
- These references are non-authoritative and should not be used as the source of truth for build/run instructions.

## Compatibility Shim

`shared/src/commonMain/kotlin/com/guyghost/wakeve/TransportService.kt` now acts as a deprecated wrapper to the canonical implementation:

- Canonical: `shared/src/commonMain/kotlin/com/guyghost/wakeve/transport/TransportService.kt`
