# Task 5 Final Review Findings

## Fix now

1. Android production must consume shared `projectEventState` for lifecycle/pending/conflict/action projection; remove/demote divergent presentation enum. Add source/behavior tests proving consumption and terminal/pending/conflict mapping.
2. Fix shared iOS compilation for `@JvmInline` (`kotlin.jvm.JvmInline` import or portable equivalent), then run iOS simulator compile and shared tests.
3. ConflictResolutionDialog summary must not duplicate descendant/interactive choice announcements; add real Compose semantics-tree coverage with both choices actionable once.
4. Global Android product-language contract must reuse exhaustive strengthened indirect scanner and exact reviewed occurrence allowlist for all inventory-owned Android paths.

## Human decision required

5. Plan Task5 text says visible tabs use Ideas/Notifications/Messages, while normative product-language spec says Messages is event-scoped conversations/comments and does not explicitly require a fourth Android tab. Existing Android has three tabs. Do not implement or forbid a fourth Messages tab until human chooses which interpretation governs.
