# Task 2 Report — Shared Concept Registry and State Projection Core

## Status

Implemented the pure Kotlin Multiplatform product-language registry and event-state projection described by the Task 2 brief. The implementation consumes the existing `EventStatus` enum and does not modify domain state identifiers or transition code.

## Model and scope review

- Source model reviewed: Task 1 artifacts under `/models`, including the product-language state machine and review.
- Domain identity remains owned by `EventStatus`; the projection returns the same enum instance in `domainStatus`.
- Product language is represented only by `SemanticKey`; no localized copy or free text decides state.
- Supported locale identifiers are exactly `en`, `fr`, `de`, `es`, `it`, and `pt`, with `en` as fallback.
- A local mutation projects `sync.waiting` and makes `sharedConfirmation` false, so pending local state is never represented as shared confirmation.
- A terminal projection has no CTA when the modeled `allowedAction` is null, as required by the input contract.
- No `EventStatus`, state-machine transition, repository, UI, API, localization content, or LLM integration was changed.

## TDD evidence

### RED

Created the neutral compilable API first, as explicitly required by the brief, with `unmodeled`, empty locales, and no action. Then added `ProductLanguageTest.canonicalStatesKeepDomainIdentity`.

Command:

```text
./gradlew :shared:jvmTest --tests '*ProductLanguageTest*' --no-daemon
```

Result: expected failure, exit code 1. Kotlin compilation succeeded; one test ran and failed at `ProductLanguageTest.kt:31` because the title remained `unmodeled` instead of the canonical semantic key. This was a behavioral assertion failure, not a compilation or Gradle test-selection error.

### GREEN

Implemented the smallest projection from the brief:

- canonical state title: `event.state.<event-status-lowercase>`;
- local pending status: `sync.waiting`;
- actions: `event.action.continue` and `sync.retry`;
- shared confirmation: false for `LOCAL_MUTATION`, true otherwise;
- exact six-locale registry and English fallback.

Command:

```text
./gradlew :shared:jvmTest --tests '*ProductLanguageTest*' --tests '*EventValidationTest*' --no-daemon
```

Result: `BUILD SUCCESSFUL`, exit code 0. Test result XML reports 1/1 `ProductLanguageTest` and 6/6 `EventValidationTest`, with zero failures, skips, or errors.

The Gradle command in the brief correctly targets both common tests on the JVM target; no alternate task was required.

## Auto-review

- `git diff -- shared/src/commonMain/kotlin/com/guyghost/wakeve/models/Event.kt` is empty.
- Existing enum identifiers remain exactly `DRAFT`, `POLLING`, `COMPARING`, `CONFIRMED`, `ORGANIZING`, and `FINALIZED`.
- Projection code is pure and deterministic: it reads typed enums/sets and returns typed semantic keys without I/O, localization lookup, generated text, or transition dispatch.
- `confirmedFacts` and `role` are intentionally retained as typed input for later projections but are not interpreted in this minimal task.
- Terminal CTA suppression relies on the modeled `allowedAction = null` invariant from the Task 2 contract; this task does not infer or mutate domain transitions.
- `git diff --check` passed.

## Files

- `shared/src/commonMain/kotlin/com/guyghost/wakeve/productlanguage/ProductLanguage.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/productlanguage/SupportedProductLocales.kt`
- `shared/src/commonTest/kotlin/com/guyghost/wakeve/productlanguage/ProductLanguageTest.kt`

## Concerns

The targeted build emits pre-existing deprecation and coroutine opt-in warnings outside the new package. They do not fail compilation or tests and were not changed to keep Task 2 scoped.

## Review correction — conflict, terminal, and action invariants

### Model review

Re-read `models/product-language.machine.ts` before changing production code. The Kotlin projection now preserves the model's classification priority for its current typed surface:

1. `FINALIZED` is terminal and suppresses status/action output even when callers provide stale sync facts or actions.
2. `SYNC_CONFLICT` takes precedence over `LOCAL_MUTATION`, projects `sync.conflict`, and prevents shared confirmation.
3. `LOCAL_MUTATION` without conflict projects `sync.waiting` and prevents shared confirmation.
4. `RETRY_SYNC` projects `sync.retry` only when a conflict or local mutation makes that action relevant.

No UI, localized strings, `EventStatus`, or state-transition code was added or changed.

### RED evidence

Added executable negative tests for conflict alone, conflict combined with local mutation, terminal state with both stale `CONTINUE` and `RETRY_SYNC`, and retry without a sync fact.

Command:

```text
./gradlew :shared:jvmTest --tests '*ProductLanguageTest*' --no-daemon
```

Result: expected `BUILD FAILED`, exit code 1. Compilation succeeded and 5 tests executed; 4 new tests failed on the intended behavioral assertions:

- conflict alone returned no `sync.conflict` status;
- conflict plus local mutation returned `sync.waiting` instead of `sync.conflict`;
- finalized retained output derived from stale facts/actions;
- retry without a sync fact exposed `sync.retry`.

### GREEN and regression evidence

Command:

```text
./gradlew :shared:jvmTest --tests '*ProductLanguageTest*' --tests '*EventValidationTest*' --no-daemon
```

Result: `BUILD SUCCESSFUL`, exit code 0. XML evidence reports:

- `ProductLanguageTest`: 5 tests, 0 failures, 0 errors, 0 skipped;
- `EventValidationTest`: 6 tests, 0 failures, 0 errors, 0 skipped.

`git diff --check` passed. `git diff -- shared/src/commonMain/kotlin/com/guyghost/wakeve/models/Event.kt` is empty, proving the existing six `EventStatus` identifiers were not modified. The only build warning in the GREEN run was a pre-existing deprecated Java `Locale` constructor in `LocalizationService.jvm.kt`.
