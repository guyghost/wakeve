# KMP Normalization Notes

This document records the repository normalization performed for module/path consistency.

## Module Naming

- Android app Gradle module: `wakeveApp`
- Shared KMP module: `shared`
- Backend module: `server`
- Web module: `webApp`

## Path Mapping

Use these mappings when updating docs, scripts, or local workflows:

- `composeApp/...` -> `wakeveApp/...`
- `composeApp:<task>` -> `wakeveApp:<task>`
- `iosApp/iosApp/...` -> `wakeveApp/wakeveApp/...`
- `iosApp/iosApp.xcodeproj` -> `wakeveApp/wakeveApp.xcodeproj`

## Canonical Build Commands

From repository root:

```bash
./gradlew build
./gradlew shared:test
./gradlew wakeveApp:assembleDebug
./gradlew server:run
```

For iOS project inspection:

```bash
xcodebuild -list -project wakeveApp/wakeveApp.xcodeproj
```

## Legacy Artifacts

Backup/suppressed sources (`*.bak`, `*.backup`, `*.disabled`) have been removed from tracked source trees and are now ignored via `.gitignore`.

## Intentional Leftovers

- Historical reports and archived notes may still mention `composeApp` or `iosApp` naming.
- These references are non-authoritative and should not be used as the source of truth for build/run instructions.

## Compatibility Shim

`shared/src/commonMain/kotlin/com/guyghost/wakeve/TransportService.kt` now acts as a deprecated wrapper to the canonical implementation:

- Canonical: `shared/src/commonMain/kotlin/com/guyghost/wakeve/transport/TransportService.kt`
