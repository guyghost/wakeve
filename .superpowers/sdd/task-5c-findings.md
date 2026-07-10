# Task 5C Activities Report

- RED confirmed from `46c5c02d`: Batch 3 reported direct and helper-produced Activity copy in both Activity surfaces.
- Replaced visible Activity literals and French copy helpers with Android string/plural resources across the six locale catalogs.
- Preserved activity creation, editing, deletion, registration, filtering, and schedule-summary behavior; dates, counts, ratios, durations, and amounts now use localized format resources.
- Added target-specific TalkBack semantics for edit, participant management, and delete actions.
- Updated `ActivityCopyTest` to assert resource and accessibility contracts instead of French helper return values.
- Verification: `ActivityCopyTest` passes; `assembleDebug` succeeds; `git diff --check` succeeds. Full Batch 3 remains RED only in the intentionally untouched Budget/Equipment subsets (for example `de:budget_hint copied English`).

## Task 5C Budget Report

- RED confirmed from `5d2da3e8`: Batch 3 reported direct, indirect, and helper-produced Budget copy in `BudgetDetailScreen.kt` and `BudgetOverviewScreen.kt`, plus missing formatted/semantic resource contracts.
- Replaced all visible Budget literals and French helper output with Android strings/plurals across FR, EN, DE, ES, IT, and PT; legacy Budget tests now assert the resource projections instead of a fixed French locale.
- Preserved budget creation, item mutation, filtering, settlement calculations, payment-pot, Tricount, offline, and read-only behavior. Amounts, ratios, usage, participant counts, reimbursements, and category totals now use localized formatted resources.
- Added TalkBack semantics that name the mark-paid action, expense target, and unpaid state; paid/unpaid state resources both retain their target placeholder.
- Tightened the shared Batch 3 test partition to exclude pre-existing generic Budget keys, recognize precision placeholders, preserve URLs while stripping comments, and allow technical provider/status identifiers.
- Verification: targeted Budget tests pass; `assembleDebug` succeeds; `git diff --check` succeeds. Full Batch 3 remains RED only for the intentionally untouched Equipment subset (`equipment_progress_ratio`, Equipment literals/catalog parity).
