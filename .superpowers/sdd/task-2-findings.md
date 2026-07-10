# Task 2 Review Findings

1. Critical: `SYNC_CONFLICT` must project a conflict semantic status and `sharedConfirmation = false`, including when combined with `LOCAL_MUTATION`.
2. Critical: `FINALIZED` must suppress all CTAs regardless of stale caller `allowedAction`.
3. Add behavioral negative tests for conflict alone, conflict+local mutation, and finalized with CONTINUE/RETRY_SYNC.
4. Keep KMP projection semantically aligned with the Task 1 XState source of truth; do not expose RETRY_SYNC without a relevant sync fact.
5. Append RED/GREEN evidence, run focused KMP and EventValidation tests, diff-check, commit.
