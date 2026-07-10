# Task 4 Review Findings

1. Blocking: fix Task 3 scanner mapping for real repository Android catalogs: `fr -> values`, `en -> values-en`; fixture mapping must remain explicit and cover this convention. `--catalogs-only` must report zero Android parity findings after Task 4, while iOS/Siri future debt may remain.
2. Important: ProductLanguageCatalogContractTest must assert resource-kind parity, plural quantities, string-array structure/item count, and positional placeholder signatures for every key/locale, not only key-name sets.
3. Add RED fixtures/tests demonstrating old scanner mapping and structural/signature drift; then GREEN.
4. Re-run scanner fixtures, targeted Gradle test, Android-only scanner finding assertion, XML parse/build, diff-check; commit and append report.
