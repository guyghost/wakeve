# Task 5A Review Findings

1. Blocking: migrate every visible intermediate/enum/parameter/fallback string in the nine Batch1 files, including InboxScreenWrapper, ProfileTabScreen, WakeveNavHost, ScreenWrappers examples cited by reviewer.
2. Blocking: strengthen Batch1 source contract to detect visible strings routed through intermediate properties/arguments and prove Notifications/Ideas labels plus route stability. It must fail before fixes and pass after.
3. Important: navigation accessibility must not hardcode contradictory selected state. Use localized action/target and platform selection semantics or selected/unselected localized variants driven by state.
4. Taxonomy clarification: do NOT add a new Android Messages tab. Normative OpenSpec requires Messages for event-scoped conversation surfaces, not a fourth Android primary destination; Task10 owns full taxonomy. Batch1 must avoid relabeling conversations as Notifications.
5. Remove/revert Batch5 EventPhotosFollowUp localization from this batch, or formally keep only if required to compile due a shared changed call; do not leave mixed localized envelope around French String projections.
6. Keep `Screen.Inbox`, route strings, intents, repositories, analytics stable.
7. Add RED/GREEN evidence, six catalog parity, targeted tests/build, commit and append report.

## Re-review findings

8. Blocking: translate every new Batch1 translatable value in DE/ES/IT/PT; no wholesale English copies.
9. Localize `currentUserName` fallback `"User"` and remove it from any technical allowlist.
10. Narrow/remove blanket `IllegalStateException` allowlisting; keep diagnostic exception text internal and map a localized presentation error if surfaced.
11. Convert `inbox_selected_count` to a real `<plurals>` resource in all six locales.
12. Add a Batch1 translation guard comparing new translatable values against EN, with only exact reviewed cognates/brand tokens allowed; demonstrate RED before translations and GREEN after.
