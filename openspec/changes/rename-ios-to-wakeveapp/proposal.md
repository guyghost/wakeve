# Rename iosApp to wakeveApp

## Context
The iOS project is currently named "iosApp" which is inconsistent with the Android naming convention and the overall project structure. Renaming it to "wakeveApp" will improve consistency and clarity.

## Objectives
- Rename the iOS project folder from `iosApp` to `wakeveApp`
- Update Xcode project references (target name stays WakeveApp, but folder changes)
- Update Gradle build configuration references
- Update all documentation references
- Ensure the project compiles successfully after rename

## Scope
- Files to rename:
  - `iosApp/` → `wakeveApp/`
- Files to update:
  - `iosApp/iosApp.xcodeproj/project.pbxproj`
  - `build.gradle.kts` (shared module)
  - Any documentation files
  - Any build scripts or CI configurations

## Constraints
- Platform: iOS (SwiftUI)
- Must maintain Xcode project integrity
- Must not break Gradle build for shared module
- Git history preservation (use git mv)

## Tasks
- [ ] Analyze all references to iosApp in the codebase
- [ ] Rename folder using git mv
- [ ] Update Xcode project.pbxproj file
- [ ] Update Gradle build configuration
- [ ] Update documentation references
- [ ] Update any hardcoded paths in build scripts
- [ ] Compile iOS to verify
- [ ] Compile shared module to verify
- [ ] Test Android build (to ensure shared module works)

## Files Modified
- `build.gradle.kts` (references to iosApp)
- `iosApp/iosApp.xcodeproj/project.pbxproj`
- Documentation files
- CI/CD configurations (if any)
