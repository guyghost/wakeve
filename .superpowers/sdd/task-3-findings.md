# Task 3 Review Findings

1. Blocking: source literal scanner misses multiline/named-argument Compose forms and custom Swift controls. Detect real visible contexts across bounded windows/AST-like parsing.
2. Blocking: forbidden terms currently scan comments, identifiers, types, and routes. Restrict to extracted user-visible literals/resource values; internal `Scenario`/`Inbox` identifiers must not fail.
3. Important: parse Android XML safely, enforce matching tags and report duplicate keys/syntax errors rather than `.to_h` masking.
4. Important: fixtures must cover iOS and Siri parity/values, Swift sources, Kotlin+Swift multiline/named/custom controls, exact allowlist allowed occurrence and near-miss rejection, and comments/identifiers not detected.
5. Preserve exact allowlist semantics and repository debt failure; no platform debt correction in this task.
6. Append RED/GREEN evidence, syntax checks, fixture suite, repository audit summary, diff-check, commit.
