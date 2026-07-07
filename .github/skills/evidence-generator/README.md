# evidence-generator

A Copilot CLI skill that generates and refreshes Wakeve's App Store evidence documents
(`docs/APP_STORE_*.md`) from a single canonical catalog, so every doc matches the exact
structure the `store-readiness` CI workflow greps for.

## Why

~39 `APP_STORE_*` docs share the same shape — a `Date`, `Status`, a
`*_EVIDENCE_COMPLETE=false` marker line, an "Apple Source Baseline", required-evidence
tables, "Local Gate Coverage", and "Apple References". Writing these by hand produced a
long tail of repetitive `docs(app-store): refresh … evidence` commits (56 in 60 days).
This skill turns that into one parameterized call.

## Files

- `SKILL.md` — the skill the agent loads (invocation contract + document skeleton).
- `catalog.json` — the canonical list of evidence docs, their CI markers, scopes, and
  short aliases. **This is the source of truth.** Edit it first when Apple changes a
  requirement or a new gated evidence type appears.

## Usage

In a Copilot CLI session:

```
Use the evidence-generator skill to create the DSA trader status evidence at pending.
Use the evidence-generator skill to refresh live-url-aasa and set it complete.
```

The skill resolves short names like `dsa`, `live-url`, `account-access`, `sdk-privacy`
via the `aliases` in `catalog.json`, then writes/refreshes the right file.

## Keeping the catalog in sync

The gated markers must match `.github/workflows/store-readiness.yml` exactly. Verify with:

```bash
# markers the CI checks for
grep -oE "APP_STORE_[A-Z_]+_EVIDENCE_COMPLETE" .github/workflows/store-readiness.yml | sort -u
```

Every marker in that list must appear as `"marker"` in `catalog.json` under `gated` (or
`signoff` for `APP_STORE_FINAL_SIGNOFF_COMPLETE`). Run this check after editing the
catalog or the workflow.

## Limits

The skill writes the **skeleton** — the correct filename, marker, section order, and
status line. The substantive evidence (actual URLs, command output, status of real
checks) still requires a human or an audit run, because that content is not derivable
from a template.
