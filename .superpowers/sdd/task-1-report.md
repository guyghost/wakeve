# Task 1 Report — Executable Projection Model and Review Matrix

## Status

DONE_WITH_CONCERNS

Commit: `0cde7719` (`docs(product-language): model deterministic projections`)

The deterministic product-language projection foundation is implemented and reviewed. No platform code, existing business transition, permission, route, persistence contract, analytics identifier, or domain status was changed.

## Changes

- Added `models/product-language.machine.ts` with the typed pure projection and XState v5 classifier.
- Added `models/product-language.machine.test.ts` with the two exact behavioral tests from the brief.
- Generated `models/product-language.inventory.json` using the exact deterministic command from the brief; it contains 263 sorted non-empty paths.
- Added `models/product-language.review.md` with invariants, inventory disposition, and all ten required review cases.
- Reviewed `openspec/changes/standardize-product-language/tasks.md`; no checkbox in 1.1–2.5 was changed because Task 1 does not prove the broader full-string/locale registry, notification/accessibility models, or cross-discipline reviews those items require.

## TDD evidence

### RED

Command:

`node --experimental-strip-types --test models/product-language.machine.test.ts`

Result: exit 1; 2 tests executed, 0 passed, 2 failed. The failures were the expected behavioral assertions:

- actual `unmodeled` versus expected `event.state.draft`;
- actual machine state `ready` versus expected `pendingSync`.

The neutral scaffold was limited to the exact compilable API described in the brief. There was no module-resolution or test-infrastructure failure.

### GREEN

Command:

`node --experimental-strip-types --test models/product-language.machine.test.ts`

Result: exit 0; 2 tests passed, 0 failed. Node emitted the existing package-level `MODULE_TYPELESS_PACKAGE_JSON` warning; changing package module configuration was outside Task 1.

## Final verification

- `node --experimental-strip-types --test models/product-language.machine.test.ts` — PASS, 2/2.
- `openspec validate standardize-product-language --strict` — PASS, change valid.
- `jq -e '.version == 1 and (.files | length > 0) and all(.files[]; (.path | length > 0) and (.category | length > 0))' models/product-language.inventory.json` — PASS.
- `git diff --check` — PASS.

## Files

- `models/product-language.machine.ts`
- `models/product-language.machine.test.ts`
- `models/product-language.review.md`
- `models/product-language.inventory.json`
- `openspec/changes/standardize-product-language/tasks.md` reviewed, intentionally unchanged

## Self-review

- The projection preserves all six domain status identities.
- Copy is represented by stable semantic keys, not literals used as state discriminators.
- Offline local mutation is distinguished from shared confirmation.
- Sync success recomputes from typed facts and removes only `LOCAL_MUTATION`.
- The terminal branch is final and the tested terminal input has no action.
- No LLM or free text can select a branch.
- The implementation is exactly bounded to the brief's API and does not alter existing workflow machines.

## Concerns

- The exact inventory categorization command matches `AI` case-insensitively inside common path segments such as `main`, so many paths receive `ai-entry-point`. The inventory remains deterministic and complete as requested, but categories are discovery heuristics rather than semantic proof.
- `SYNC_CONFLICT`, validation, cancellation, permission, AI failure/manual fallback, and long-text/accessibility rendering are reviewed as explicit gaps, not falsely claimed as implemented branches.
- OpenSpec tasks 1.1–2.5 remain open because their acceptance criteria exceed the evidence produced by this bounded task.

## Review-fix report — 2026-07-10

Status: FIXED

### RED evidence

- Full focused suite after adding branch tests: exit 1; 11 tests executed, 2 passed and 9 failed by expected assertions for conflict, failure copy/retry, validation, cancellation, denied/restricted permission, terminal CTA suppression, and AI unavailable/rejected manual fallback.
- Conflict transition test after initial GREEN: `--test-name-pattern='sync conflict names'` exited 1 by assertion (`sync.conflict` vs expected `sync.waiting`), proving resolution had not recomputed the projection.

### GREEN evidence

- Full focused suite after minimal implementation: exit 0; 11/11 passed.
- Conflict-only suite after deterministic resolution action: exit 0; 1/1 passed.
- Node continued to emit the existing `MODULE_TYPELESS_PACKAGE_JSON` warning; package-wide module configuration remains outside this bounded fix.

### Fix scope

- Added executable conflict, sync failure/retry, validation, cancellation, permission, terminal, and AI manual-fallback branches; domain status is immutable and no text/LLM input selects a transition.
- Regenerated the 263-path inventory with semantic AI boundaries and verified distribution: AI 8, Android UI 53, delivery/Siri 36, gamification/profile 9, iOS UI 72, shared 85.
- Updated the review matrix to state only executable evidence; long-text/accessibility and broader registry/locale/cross-discipline acceptance remain unproven, so OpenSpec tasks 1.1–2.5 remain unchecked.

### Fresh final verification

- Model suite: PASS, 11/11.
- Inventory schema, six-category distribution, and source-count equality checks: PASS; 263/263 paths.
- `openspec validate standardize-product-language --strict`: PASS.
- `git diff --check`: PASS.
