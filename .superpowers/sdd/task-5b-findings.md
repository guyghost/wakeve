# Task 5B Review Findings

1. Remove nine hard-coded compatibility helpers/literals from production and delete `compatibilityCopy` test escape hatch; update old tests to assert semantic keys/resources rather than production localized literals.
2. Consume `a11y_comment_reply` with author/comment target on both reply actions without duplicate TalkBack output; test actual usage.
3. Expand translation guard to every Batch2 translatable string/plural key and validate plural quantity placeholder signatures, with only exact cognate allowances.
4. RED then GREEN, targeted Comments/catalog tests, global diagnostics no Batch2, assemble, diff-check, commit/report.
