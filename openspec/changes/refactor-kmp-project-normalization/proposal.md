# Change: Normalize Kotlin Multiplatform Project Structure

## Status
**PROPOSED**

## Why

The repository currently has structural drift that slows development and creates confusion:

- Docs reference `composeApp/` and `iosApp/` while the active module is `wakeveApp/`
- Duplicate domain/service implementations exist in parallel packages (e.g. transport/recommendation)
- Large numbers of `.bak` and `.disabled` files pollute source trees
- Build/test entry points are not consistently documented
- OpenSpec task reality and documentation are out of sync

Normalizing the KMP project will reduce onboarding friction, prevent regressions, and make cross-platform work predictable.

## What Changes

### 1) Module and source-set normalization
- Standardize references on actual modules (`shared`, `wakeveApp`, `server`, `webApp`)
- Align documented paths with real source-set layout
- Keep platform-specific code in proper `commonMain/androidMain/iosMain/jvmMain` boundaries

### 2) Duplicate/legacy code cleanup
- Identify and consolidate duplicate implementations (transport/recommendation/etc.)
- Remove or archive stale backup/disabled files from tracked source paths
- Keep only one canonical implementation per feature area

### 3) Build and test contract normalization
- Define one canonical set of Gradle commands for build/test/check
- Ensure README/docs/OpenSpec use the same commands
- Verify baseline builds/tests after normalization

### 4) Documentation and OpenSpec alignment
- Update architecture/status docs to reflect real repository state
- Align AGENTS and implementation docs with actual module names and paths
- Record unresolved gaps explicitly instead of outdated “implemented” claims

## Impact

### Affected Areas
- `README.md`, `AGENTS.md`, `docs/**`
- `shared/src/**` (duplicate/legacy cleanup)
- `wakeveApp/src/**` (module/path consistency references)
- `openspec/**` (tracking alignment)

### Breaking Changes
No functional product behavior changes intended; this is a structural normalization/refactor pass.

## Success Criteria

- [ ] No doc references to non-existing app module paths
- [ ] Duplicate service implementations are consolidated or clearly deprecated
- [ ] Tracked `.bak`/`.disabled` files are reduced to explicit allowlist only
- [ ] Canonical build/test commands documented once and reused everywhere
- [ ] `./gradlew build` and key test suites pass after cleanup

