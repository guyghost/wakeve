# Task 3 Report — Locale Parity, Forbidden Terms, and Literal Audit Foundation

## Scope

Implemented only the product-language audit foundation:

- executable Bash scanner wrapper;
- Ruby catalog parity, forbidden-term, and production-literal scanner;
- exact-identity allowlist with no active exemptions;
- black-box fixture suite;
- critical release-gate integration.

No Android/iOS catalog or production UI debt was changed. Those corrections remain for Tasks 4–6.

## TDD evidence

### RED

The initial scanner was an executable no-op that always exited 0. The fixture suite was written before the scanner implementation and run with:

```text
bash scripts/tests/audit-product-language-test.sh
```

Observed result:

```text
exit 1
expected failure containing: android fr: missing tab_ideas
```

This was the expected failure: the no-op incorrectly accepted a French catalog missing `tab_ideas`.

### GREEN — fixtures

After implementing the scanner contract, the same fixture suite exited 0. It proves these precise failures are emitted and observed:

- `android fr: missing tab_ideas`
- `android/de:tab_ideas: forbidden visible term`
- `composeApp/src/androidMain/kotlin/example/Example.kt:1: production literal`

The final clean six-locale fixture also exits 0.

## Verification evidence

The final verification command checked:

- `bash -n` for the wrapper, fixture suite, and critical release gate;
- `ruby -c scripts/lib/audit_product_language.rb` (`Syntax OK`);
- the complete fixture suite (exit 0);
- the repository audit;
- the exact release-gate wiring;
- `git diff --check`.

### Repository audit — expected FAIL due to current debt

`scripts/audit-product-language.sh` exits 1 against the repository and reports 3,585 findings. The first findings are current Android locale parity gaps such as:

```text
android de: missing and_separator
android de: missing custom_event_type_example
android de: missing custom_event_type_supporting
android de: missing generic_error_title
```

This repository result is intentionally **not GREEN**. The fixture suite is GREEN; the repository audit is an expected debt FAIL until Tasks 4–6 repair the catalogs and visible literals.

### Release gate integration

`scripts/test-critical-release-gates.sh` contains:

```text
run_gate "product language" scripts/audit-product-language.sh
```

The gate script parses successfully. The full critical release gate was not required to pass because the newly integrated audit intentionally blocks on the documented repository debt.

## Auto-review

- Scope is limited to scripts, tests, allowlist, and release-gate wiring.
- The allowlist contains comments only and no active exemptions.
- Source/resource exemptions use exact identity equality; glob patterns and term-wide rules receive no special handling.
- Fixture mode audits its isolated Android catalogs and Kotlin source without depending on repository iOS/Siri resources.
- Repository mode covers Android, iOS, Siri, Kotlin, and Swift inputs specified by the task.

## Review remediation evidence

### RED

The review fixture expansion was run against commit `c2dca407` before changing the scanner:

```text
bash scripts/tests/audit-product-language-test.sh
exit 1
expected failure containing: ios de: missing tab_ideas
```

This proved fixture mode did not inspect its iOS or Siri catalogues. The same added fixtures also cover Kotlin and Swift multiline/named arguments, Wakeve custom controls, exact allowlist positive and near-miss identities, comments/identifiers/routes, Android duplicate keys, and malformed/mismatched XML.

### GREEN

Final checks:

```text
bash -n scripts/audit-product-language.sh scripts/tests/audit-product-language-test.sh scripts/test-critical-release-gates.sh
ruby -c scripts/lib/audit_product_language.rb
Syntax OK
bash scripts/tests/audit-product-language-test.sh
exit 0
git diff --check
exit 0
```

The Android catalogue reader now uses REXML, reports syntax/tag errors and duplicate keys, and preserves resource-value scanning. Source findings are produced only from literals in recognized Kotlin/Compose and SwiftUI/Wakeve visible contexts after comments are removed; identifiers, types, and route literals are not scanned as visible language.

The repository audit remains intentionally red without changing product debt:

```text
scripts/audit-product-language.sh
exit 1
2966 findings
android de: missing and_separator
android de: missing custom_event_type_example
android de: missing custom_event_type_supporting
android de: missing generic_error_title
```
