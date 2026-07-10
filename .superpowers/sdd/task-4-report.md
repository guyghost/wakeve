# Task 4 Report — Complete Android Catalogs and Semantic Keys

## Scope

- Reconciled the six requested Android catalogs: `values`, `values-en`, `values-de`, `values-es`, `values-it`, and `values-pt`.
- Added a catalog contract covering required semantic keys and complete parity across strings, plurals, and string arrays.
- Did not migrate Kotlin call sites or modify the Task 3 scanner.

## TDD evidence

RED:

```text
ProductLanguageCatalogContractTest > everyAndroidLocaleHasCanonicalKeys FAILED
java.lang.AssertionError: tab_ideas missing from values
```

GREEN:

```text
./gradlew :composeApp:testDebugUnitTest --tests '*ProductLanguageCatalogContractTest*' --no-daemon --no-configuration-cache
BUILD SUCCESSFUL
```

## Catalog review

- All six catalogs contain the same 622 resource keys.
- Resource kinds (`string`, `plurals`, `string-array`) match for every key.
- Positional placeholder signatures match the canonical catalog for every key.
- French lifecycle values are exactly: `Brouillon`, `Sondage en cours`, `Options à comparer`, `Date confirmée`, `Détails à préparer`, `Prêt`.
- DE/ES/IT/PT/EN additions are native translations of the concepts, including onboarding debt; French values were not copied into other locales.
- New keys cover Ideas, lifecycle, notification filters, benefit-led AI copy, proposal review, sync/error recovery, notification permission recovery, contextual accessibility, and private milestones.

Android-only verification:

```text
Android-only: 6/6 catalogs, 622 equal keys; resource types and positional placeholders match
```

## Repository scanner debt

`scripts/audit-product-language.sh --catalogs-only` exits 1 with 2541 findings: 623 Android, 1912 iOS, and 6 Siri. The iOS and Siri findings are expected future-task debt. The Android findings are caused by the unchanged Task 3 scanner mapping `fr` to `values-fr/strings.xml`, while this repository and Task 4 define French as the default `values/strings.xml`; it consequently treats the nonexistent `values-fr` catalog as empty. The precise Android-only check above validates the six catalogs in Task 4 scope without changing the scanner.

## Self-review

- XML parsed successfully through both Android resource compilation and REXML.
- No Kotlin production files were changed.
- No placeholders or plurals were downgraded or translated structurally.
- Existing translations were preserved; only previously absent keys were added.

## Review-fix addendum

The earlier scanner-debt paragraph is superseded by this review fix.

- Corrected Android locale resolution in the shared audit: French now maps to the default `values/strings.xml`, and English maps to `values-en/strings.xml`, for both repository and fixture roots.
- Added RED/GREEN scanner fixtures for the real locale mapping plus resource-kind drift, plural-quantity drift, string-array item-count drift, and positional-placeholder drift.
- Extended `ProductLanguageCatalogContractTest` to parse XML and compare resource kind, plural quantities, string-array item structure, and per-resource/per-item positional placeholder signatures across all six catalogs.
- No translation resource was changed by the review fix.

RED fixture evidence before the scanner fix:

```text
missing expected finding: android fr: missing tab_ideas
android fr: missing file .../android/values-fr/strings.xml
```

Final verification:

```text
scripts/tests/audit-product-language-test.sh
PASS (exit 0)

./gradlew :composeApp:testDebugUnitTest --tests '*ProductLanguageCatalogContractTest*' --no-daemon --no-configuration-cache
BUILD SUCCESSFUL

./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:processDebugResources --no-daemon --no-configuration-cache
BUILD SUCCESSFUL

scripts/audit-product-language.sh --catalogs-only
exit 1 (intentionally preserved global result)
android_findings=0
ios_findings=1912
siri_findings=6

git diff --check
PASS
```

The remaining 1918 catalog findings are exclusively the known iOS and Siri debt assigned to Tasks 6 and 8; the global scanner exit remains nonzero and is not masked.
