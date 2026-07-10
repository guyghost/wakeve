# Task 1 Review Findings

## Important

1. `models/product-language.machine.ts:16`: `SYNC_CONFLICT` is typed but ignored. It must never project `sharedConfirmation: true`; model the conflict state, affected-data semantic key, and deterministic resolution/retry actions.
2. `models/product-language.machine.ts:22`: `FINALIZED` must suppress invalid editing CTAs regardless of the caller-provided `allowedAction`.
3. `models/product-language.machine.ts:64`: `SYNC_FAILED` must recalculate failure copy/state and expose an explicit deterministic retry event rather than accepting external success directly.
4. `models/product-language.review.md:23`: validation, cancellation, and permission branches must be executable/modelled, not described as safe by absence.
5. `models/product-language.review.md:28`: do not accept Task 1 while sync conflict is incomplete.
6. `models/product-language.review.md:30`: model AI unavailable/rejected with a usable deterministic manual path and no state transition.
7. `models/product-language.machine.test.ts:12`: add behavioral tests for sync failure/retry, conflict, permission, terminal with invalid action, validation, cancellation, and AI manual fallback.
8. `models/product-language.inventory.json:4`: fix categorization. The case-insensitive `AI` pattern matches `main`, causing 192/263 files to be mislabeled. Categories must cover Android UI, iOS UI, shared, delivery/Siri, AI, and gamification/profile meaningfully.

## Minor

- Clarify in the review evidence that path exhaustiveness and semantic category validation are separate checks.
- Node emits `MODULE_TYPELESS_PACKAGE_JSON`; record it accurately. Fix only if a narrow task-local command/config avoids wider package changes.

## Required fix evidence

- Follow TDD for each new branch: append RED and GREEN commands/output to `task-1-report.md`.
- Re-run the focused model tests, inventory schema/category assertions, OpenSpec strict validation, and `git diff --check`.
- Commit fixes, append the report, and return the new head SHA.

## Re-review Important Findings

9. Conflict projection must derive its visible action from `allowedAction`; `null` must expose no CTA and `RETRY_SYNC` must not silently become resolve.
10. Guard `RESOLVE_CONFLICT` and `RETRY_SYNC` events so unauthorized events cannot transition to `pendingSync`.
11. `SYNC_FAILED` must not create a retry permission when input `allowedAction` is null.
12. Add negative behavioral tests for forbidden conflict/retry events and no-action projections.
13. Validation must identify the affected field/action correction, not only the generic `INVALID_FIELD` fact.
14. Inventory categorization must classify delivery services such as `iosApp/src/Services/APNsService.swift` as delivery/Siri; add exact semantic category assertions, not only non-empty distribution.
