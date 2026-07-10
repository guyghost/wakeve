# Task 5 Report — BLOCKED

## Status

Task 5 is **BLOCKED before production implementation** because the complete Android migration is too large to keep reviewable in one bounded implementation turn. No production subset was applied, no resource fallback was introduced, and no commit was created.

## TDD evidence

Created:

- `composeApp/src/androidUnitTest/kotlin/com/guyghost/wakeve/productlanguage/AndroidProductLanguageContractTest.kt`

The contract is limited to Android Kotlin production paths from `models/product-language.inventory.json` and protects:

- canonical navigation resources while retaining `Screen.Inbox`;
- hardcoded `Text` calls, including multiline and named `text` forms;
- hardcoded named Compose UI arguments;
- hardcoded `contentDescription` and semantics;
- exact diagnostics as `path:line:type:source`;
- an exact preview allowlist of `preview/factories/InboxItemFactory.kt`, `ui/components/WakeveAsyncImagePreview.kt`, and `ui/inbox/InboxScreenPreview.kt`.

RED command:

```text
./gradlew :composeApp:testDebugUnitTest --tests '*AndroidProductLanguageContractTest*' --no-daemon --no-configuration-cache
```

Observed result:

- Kotlin compilation succeeded;
- 4 tests executed;
- 4 tests failed for the expected missing migration categories;
- Gradle exited 1 with `BUILD FAILED`.

## Scope evidence

The source contract found overlapping categories across 24 Android production files:

- 141 hardcoded `Text` findings;
- 62 hardcoded named UI argument findings;
- 17 hardcoded accessibility/semantics findings;
- 177 distinct scanned literals;
- only 37 literals already have a matching Task 4 resource;
- approximately 140 semantic or formatted resource keys still require coherent translations in all six Android catalogs;
- meal planning and meal dialogs account for 74 findings alone.

Completing this as one migration would either evade the new contract, introduce untranslated fallbacks, or make review and targeted verification unreliable. All three violate the Task 5 brief.

## Required decomposition

Run these as explicit, independently reviewed GREEN batches while retaining the single contract as the invariant:

1. Navigation, authentication, profile, and inbox — tabs `Idées` / `Notifications` / `Messages`, route preservation, notification entry points, and action-target-state semantics.
2. Comments and collaboration — inputs, filters, replies, destructive confirmations, errors, and accessibility labels.
3. Budget, activities, accommodation, and equipment — formatted amounts, paid/unpaid state semantics, dialogs, and pending/error outcomes.
4. Meals — 74 findings across `MealPlanningScreen.kt` and `MealDialogs.kt`, including formatted dates, money, participants, constraints, delete confirmations, and semantics.
5. Event workflow, notifications, sync conflicts, settings, and albums — `projectEventState` projection, pending/offline/error/cancellation/permission/terminal outcomes, terminal CTA removal, and filter copy.
6. Six-catalog parity and final verification — translate every new key in `values`, `values-en`, `values-de`, `values-es`, `values-it`, and `values-pt`; run the full contract, notification filter test, deterministic Android-only scanner check, and `assembleDebug`.

Each batch must follow RED → GREEN → targeted test, and the final integrator must auto-review identifiers to confirm that `Screen.Inbox`, routes, intents, repositories, and analytics remain unchanged.

## Verification not claimed

Because production implementation intentionally did not begin, these completion gates were not run and are not claimed:

- `NotificationsScreenFilterTest` GREEN;
- Android `assembleDebug` after migration;
- font-scale and compact/medium/expanded design validation;
- zero Android findings after migration;
- final commit.

## Executable batch ownership (exact RED inventory)

### Count reconciliation

A fresh read of the Gradle XML result identifies **27 unique Kotlin paths**, not 24. The earlier 24-file count omitted three scanner findings that are not ordinary visible product copy but still fail the current contract:

- `navigation/AuthCallbacks.kt`: `Text("Sign in with Google")` inside KDoc/example text;
- `ui/event/DraftEventWizard.kt`: internal animation `label = "step_transition"`;
- `LoginScreen.kt`: decorative error glyph `text = "❌"`.

These are explicitly retained below so the executable partition matches the actual contract and no finding is silently lost. Each of the 27 raw paths has exactly one owning batch. There are no file overlaps between batches. Finding-category overlaps remain inside files: the same source expression is commonly reported once as `hardcoded Text` and once as a named `text` argument; `contentDescription` findings form a third category. Those overlapping diagnostics are one migration responsibility, not separate files or separate resource keys.

### Batch 1 — Navigation, authentication, profile, and inbox

Owning Kotlin files (9):

- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/InboxScreenWrapper.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/LoginScreen.android.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/LoginScreen.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/AuthCallbacks.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/ScreenWrappers.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveAdaptiveNavigationScaffold.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveBottomBar.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ProfileTabScreen.kt`

Resource families:

- `tab_*`, `nav_*`, `a11y_nav_*`, using canonical `tab_ideas`, Notifications, and Messages while preserving `Screen.Inbox`;
- `auth_*`, `login_*`, `error_*`, and provider/action labels;
- `inbox_*`, `messages_*`, `notifications_*`, filters, read/unread actions, cancellation, and completion;
- `profile_*`, `account_*`, `about_*`, quick actions, guest/account outcomes;
- formatted `event_next_action`, `event_status_and_title`, restricted-access copy;
- target/action/state accessibility keys for navigation and inbox icons.

Targeted test pattern:

```text
./gradlew :composeApp:testDebugUnitTest --tests '*AndroidProductLanguageContractTest.navigationUsesCanonicalResourcesWithoutRenamingRoutes' --tests '*AndroidProductLanguageBatch1Test*' --no-daemon --no-configuration-cache
```

Recommended new test class: `AndroidProductLanguageBatch1Test`, asserting canonical tab IDs, `Screen.Inbox` preservation, and no findings restricted to these nine paths.

#### Batch 1 TDD execution — 2026-07-10

- RED: the targeted Batch 1 command executed 3 tests and failed 3 as expected: the canonical `R.string.tab_ideas` reference was absent and the nine-file scanner reported hardcoded visible copy and semantics.
- GREEN: `AndroidProductLanguageBatch1Test` now scopes the contract to the nine owned paths; the canonical navigation test and Batch 1 tests pass without implying the global Task 5 contract is green.
- Catalogs: all new keys exist in `values`, `values-en`, `values-de`, `values-es`, `values-it`, and `values-pt`; `ProductLanguageCatalogContractTest` passes, including structural and placeholder parity.
- Navigation contract: visible Ideas/Notifications copy uses resources while `Screen.Inbox`, routes, intents, repository calls, and analytics identifiers remain unchanged.
- Accessibility: migrated navigation and inbox actions use resource-backed TalkBack labels naming the action and target; navigation labels also expose tab state through the Material navigation item semantics.
- Verification: targeted Batch 1 + catalog command passed (`BUILD SUCCESSFUL`); `:composeApp:assembleDebug --no-configuration-cache` passed. The full `AndroidProductLanguageContractTest` intentionally remains RED in its three broad scanner tests because Batches 2–5 are not migrated.
- Tooling concern: running `assembleDebug` with the repository default configuration cache reaches APK assembly but Gradle rejects caching `processDebugGoogleServices`; the required no-configuration-cache rerun succeeds.

#### Batch 1 review correction — 2026-07-10

- RED: `AndroidProductLanguageBatch1Test` was strengthened before production edits to inspect visible literals hidden in enum constructors, defaults, `Triple` values, helper returns, interpolations, and component arguments across the same nine owned files. The targeted run executed 3 tests and failed 2: route/taxonomy evidence was incomplete and the indirect scanner reported the remaining Batch 1 copy.
- GREEN: all Batch 1 indirect copy now resolves through Android resources; filters, selection count, inbox contexts and empty states, relative time, profile actions/defaults, wrapper errors, toasts, version copy, and access-denied copy are covered. The targeted Batch 1, canonical navigation, and catalog command passes.
- Accessibility correction: bottom-bar and adaptive navigation descriptions now receive the actual selected/unselected state. No description hardcodes an unselected state while Material selection semantics report selected.
- Normative taxonomy clarification: Android retains exactly three primary destinations (`Home`, internal `Screen.Inbox`, and `Explore`). Task 5A does not add a fourth Messages tab. `Screen.Comments` remains the event-scoped conversation route and is explicitly tested as distinct from `Screen.Notifications`; conversations are not renamed Notifications.
- Ownership correction: the premature Batch 5 localization wrapper around `EventPhotosFollowUpScreen` was reverted, and its two temporary formatted resources were removed from all catalogs. The Batch 1 scanner explicitly excludes that Batch 5-owned function while still scanning the rest of `WakeveNavHost`.
- Catalog structure: visible Batch 1 keys are defined in all six catalogs. Default-only `translatable="false"` glyph/brand resources are no longer duplicated in localized catalogs; `ProductLanguageCatalogContractTest` now explicitly permits only those canonical non-translatable omissions while preserving kind, quantity, array, and placeholder parity for translatable resources.
- Verification: targeted Batch 1 + catalog tests pass; `:composeApp:assembleDebug --no-daemon --no-configuration-cache` passes. The global Android contract remains RED only for later-batch ownership, including the restored Batch 5 `EventPhotosFollowUpScreen` literals and Batches 2–5 screens.

#### Batch 1 translation re-review correction — 2026-07-10

- RED: added `batch1TranslationsAreNotEnglishCopies` before editing catalogs. The isolated command executed 1 test and failed 1, reporting the wholesale English copies in DE/ES/IT/PT.
- Translation guard: checks the exact reviewed set of 56 Batch 1 string keys in each of the four non-English catalogs against EN. Identical values are accepted only through per-locale cognate/unit/brand allowlists (for example `Event`, `Budget`, `Dashboard`, compact time units, `Version`, and placeholders).
- GREEN: all 224 reviewed DE/ES/IT/PT string values now use natural locale copy except those explicit cognates/units/brands. The targeted command executes 6 tests with zero failures.
- Plural correction: `inbox_selected_count` is now a real `plurals` resource with `one` and `other` in all six catalogs (12 quantity entries), consumed with `pluralStringResource`; the catalog contract verifies quantity and `%1$d` signature parity.
- Fallback/error correction: the visible `currentUserName` fallback resolves through `profile_default_user_name`; `User` was removed from the source allowlist. Contact-permission presentation remains mapped by `contactAccessFailureMessage()`, while the Android exception carries the exact stable internal diagnostic `Contact permission denied by user`; the scanner allows that single reviewed diagnostic instead of all `IllegalStateException` text.
- Verification: Batch 1, canonical navigation, and catalog tests pass; `assembleDebug --no-daemon --no-configuration-cache` passes. The full Android product-language contract remains intentionally RED in 3 scanner tests only for Batches 2–5.

### Batch 2 — Comments and collaboration

Owning Kotlin files (3):

- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/collaboration/CommentInput.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/collaboration/CommentListScreen.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/comment/CommentsScreen.kt`

Resource families:

- `comment_*`, `comments_*`, `reply_*`, `mention_*`, edited state, empty state, filters, close/cancel/retry;
- plurals `comment_reply_count` and formatted character-count/mention keys;
- destructive confirmation title/body/action keys;
- `a11y_comment_*` keys naming action, comment/author target, and applicable state.

Targeted test pattern:

```text
./gradlew :composeApp:testDebugUnitTest --tests '*AndroidProductLanguageBatch2Test*' --tests '*Comments*Test*' --no-daemon --no-configuration-cache
```

Recommended new test class: `AndroidProductLanguageBatch2Test`, restricting the source-contract helper to these three files and checking plural placeholder parity.

### Batch 3 — Budget, activities, accommodation, and equipment

Owning Kotlin files from current RED findings (6):

- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/activity/ActivityDialogs.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/activity/ActivityPlanningScreen.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetDetailScreen.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetOverviewScreen.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/equipment/EquipmentChecklistScreen.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/equipment/EquipmentDialogs.kt`

`AccommodationScreen.kt` remains part of the broader Task 5 review surface but has no current contract finding, so it is not falsely counted as one of the RED paths.

Resource families:

- `currency_*`, `budget_*`, estimated/actual/remaining/overrun and paid-count plurals;
- `activity_*`, activity cost and dialog field/action copy;
- `equipment_*`, quantity, total/shared cost, checklist actions and dialog copy;
- formatted amount/ratio resources with positional placeholders instead of Kotlin-built visible strings;
- `a11y_budget_*`, `a11y_activity_*`, `a11y_equipment_*`, and accommodation paid/unpaid action-target-state semantics.

Targeted test pattern:

```text
./gradlew :composeApp:testDebugUnitTest --tests '*AndroidProductLanguageBatch3Test*' --tests '*Budget*Test*' --tests '*Equipment*Test*' --tests '*Activity*Test*' --no-daemon --no-configuration-cache
```

Recommended new test class: `AndroidProductLanguageBatch3Test`, validating formatted-resource placeholders and paid/unpaid semantic keys for this ownership set.

### Batch 4 — Meals

Owning Kotlin files (2):

- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/meal/MealDialogs.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/meal/MealPlanningScreen.kt`

Resource families:

- `meal_*`, planning title, prepare/add/delete/cancel/close actions and terminal/empty/error states;
- `meal_field_*` and `meal_hint_*` for name, dates, time, location, people, estimated/actual cost, notes;
- `meal_constraint_*` for dietary constraints, participant, details, addition/removal and confirmations;
- plurals/formats for participant count, meal count, per-person cost, total cost, date range, and delete target name;
- `a11y_meal_*` and `a11y_meal_constraint_*` action-target-state descriptions.

Targeted test pattern:

```text
./gradlew :composeApp:testDebugUnitTest --tests '*AndroidProductLanguageBatch4Test*' --tests '*Meal*Test*' --no-daemon --no-configuration-cache
```

Recommended new test class: `AndroidProductLanguageBatch4Test`, restricting source checks to the two meal files and verifying every plural/format signature across six catalogs.

### Batch 5 — Workflow, media, notifications, sync, and terminal outcomes

Owning Kotlin files (7):

- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/components/WakeveAsyncImage.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/event/DraftEventWizard.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/event/ModernEventDetailView.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/invitation/InvitationShareScreen.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/notification/NotificationsScreen.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/screens/AlbumsScreen.kt`
- `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/sync/ConflictResolutionDialog.kt`

Resource families:

- `event_state_*`, `event_action_*`, `event_pending_*`, projection keys from `projectEventState`, and terminal copy with no CTA;
- `draft_*`, location/time-slot formats, invitation/QR/share copy;
- `notifications_filter_*`, priority/reason labels, retry/error/offline/permission outcomes;
- `sync_*`, conflict counts, mine/theirs selection and keep-all actions;
- `album_*`, relevance percentage and media state copy;
- `a11y_image_*`, `a11y_event_*`, `a11y_notification_*`, `a11y_sync_*`, naming action, target, and state;
- internal diagnostic/animation labels such as `step_transition` must move to constants or an exact non-product mechanism rather than user-facing string resources.

Targeted test pattern:

```text
./gradlew :composeApp:testDebugUnitTest --tests '*AndroidProductLanguageBatch5Test*' --tests '*NotificationsScreenFilterTest*' --tests '*ProductLanguageTest*' --no-daemon --no-configuration-cache
```

Recommended new test class: `AndroidProductLanguageBatch5Test`, covering the seven owned files, projection-to-resource mapping, terminal no-CTA behavior, and action-target-state semantics.

### Batch 6 — Catalog parity, scanner proof, and integration

Owning Kotlin files from RED findings: none. Batches 1–5 own all 27 raw Kotlin paths exactly once. Batch 6 owns integration artifacts only:

- `composeApp/src/androidMain/res/values/strings.xml`
- `composeApp/src/androidMain/res/values-en/strings.xml`
- `composeApp/src/androidMain/res/values-de/strings.xml`
- `composeApp/src/androidMain/res/values-es/strings.xml`
- `composeApp/src/androidMain/res/values-it/strings.xml`
- `composeApp/src/androidMain/res/values-pt/strings.xml`
- `composeApp/src/androidUnitTest/kotlin/com/guyghost/wakeve/productlanguage/AndroidProductLanguageContractTest.kt`
- existing catalog/projection/filter tests.

Resource families:

- parity for every family introduced in batches 1–5;
- identical resource kinds (`string`, `plurals`, arrays), plural quantities, positional placeholders, and formatting signatures in all six catalogs;
- exact preview/diagnostic allowlist only; no production-source exception;
- deterministic Android-only extraction of global scanner results, because known future iOS debt may keep the global scanner exit non-zero.

Targeted and final test pattern:

```text
./gradlew :composeApp:testDebugUnitTest --tests '*AndroidProductLanguageContractTest*' --tests '*ProductLanguageCatalogContractTest*' --tests '*NotificationsScreenFilterTest*' --no-daemon --no-configuration-cache
scripts/audit-product-language.sh --forbidden-terms-only > /tmp/task-5-product-language-audit.txt 2>&1 || true
if rg '^android/' /tmp/task-5-product-language-audit.txt; then exit 1; fi
./gradlew :composeApp:assembleDebug --no-daemon
```

The scanner proof must also record the global exit and non-Android finding count so a zero-byte output or scanner crash cannot be mistaken for zero Android findings.
