## 1. Audit Baseline
- [x] 1.1 Produce a normalized module/source-set map from actual filesystem structure
- [x] 1.2 Enumerate doc/path mismatches (`composeApp`/`iosApp` vs `wakeveApp`)
- [x] 1.3 Enumerate duplicate implementations by package and responsibility
- [x] 1.4 Enumerate tracked `.bak` / `.disabled` files with keep/remove decision

## 2. Structural Cleanup
- [x] 2.1 Normalize documentation references to real modules and paths
- [x] 2.2 Consolidate duplicate service implementations into canonical packages
- [x] 2.3 Remove stale backup/disabled files from active source trees
- [x] 2.4 Preserve compatibility shims only when strictly needed

## 3. Build/Test Contract
- [x] 3.1 Define canonical Gradle commands for build, test, and targeted checks
- [x] 3.2 Align README/AGENTS/docs to the same command set
- [x] 3.3 Run verification (`build`, shared tests, Android compile checks)
  - Verified commands:
    - `./gradlew :shared:test :shared:compileDebugKotlinAndroid :server:compileKotlin`
    - `./gradlew :shared:compileCommonMainKotlinMetadata`
  - Unblocked by:
    - making Google Services task skippable when `wakeveApp/google-services.json` is absent
    - fixing pre-existing `shared` multiplatform compile/test errors (notification/comment/user repository + parser/services)

## 4. Closeout
- [x] 4.1 Update OpenSpec progress and implementation status docs
- [x] 4.2 Document any intentional leftovers with rationale
- [x] 4.3 Prepare concise migration notes for contributors
