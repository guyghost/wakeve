---
name: evidence-generator
description: Generate or refresh Wakeve App Store evidence documents (docs/APP_STORE_*.md) from a canonical catalog so they match the exact structure the store-readiness CI sanitizer greps for. Use when asked to "generate evidence", "refresh evidence", "create the <TYPE> evidence doc", "scaffold App Store evidence", or when reopening a stale APP_STORE_*_EVIDENCE.md.
metadata:
  author: wakeve
  version: "1.0.0"
  argument-hint: <evidence-type> [status]
---

# Wakeve App Store Evidence Generator

Wakeve's App Store readiness depends on ~39 evidence documents in `docs/APP_STORE_*.md`. Each follows a rigid contract: the `store-readiness` workflow greps for a `<NAME>_EVIDENCE_COMPLETE=false` marker and a fixed section order. Manually retyping this structure for every doc is what produced the repetitive "refresh … evidence" commit churn.

This skill generates or refreshes any of those docs from a single canonical catalog, so the agent writes the correct filename, marker, and section skeleton in one pass.

## Canonical catalog

The source of truth is `.github/skills/evidence-generator/catalog.json`. It lists every evidence type with:

- `file` — the `docs/APP_STORE_*_EVIDENCE.md` path
- `marker` — the `*_EVIDENCE_COMPLETE` flag the CI checks (omit for non-gated docs)
- `title` — the human heading (e.g. "App Store Account Access Evidence")
- `scope` — one-sentence description of what the doc proves
- `complements` — sibling docs this one references
- `apple_topic` — the Apple developer area to cite under "Apple References"

If the user asks for a type that is **not** in the catalog, stop and ask whether to (a) scaffold a generic evidence doc anyway, or (b) extend `catalog.json` first. Do not invent a marker the CI does not know about.

## How to use

1. Read `.github/skills/evidence-generator/catalog.json`.
2. If the user gave a short name (e.g. `dsa`, `live-url`, `account-access`), resolve it case-insensitively against the `aliases` field.
3. Determine the operation:
   - **create** — the target file does not exist → write the full skeleton at `PENDING` status.
   - **refresh** — the file exists → update the `Date:` line, the `Last checked:` line, and re-verify the section order and marker, without rewriting substantive evidence the user already filled in.
4. Emit the doc using the **Document skeleton** below, substituting from the catalog entry.
5. Set status from the optional second argument (`pending` | `complete` | `blocked`). Default `pending`. When `complete`, flip the marker to `=true`; otherwise keep `=false`. Lowercase the `scope` and ensure it ends with a period when splicing into prose.
6. After writing, print a one-line summary: the file path, the marker, and the status. Do not open a PR or commit unless asked.

## Document skeleton

Every generated doc must follow this exact structure so the CI sanitizer and the blocker register cross-references stay valid:

```
# {{title}} - Wakeve

Date: {{YYYY-MM-DD}}

Status: {{PENDING|COMPLETE|BLOCKED}}

{{status_guard_clause}}   (one sentence: what must be true before the marker can flip to `true`, e.g. "Do not change the marker below until the EU DSA trader status information is submitted and confirmed in App Store Connect.")

{{marker}}={{true|false}}

## Apple Source Baseline

Last checked: {{YYYY-MM-DD}}.

- Apple says ...   (one or more factual statements about {{apple_topic}})

## Required {{short_topic}} Evidence

| Area | Required Evidence | Result |
| --- | --- | --- |
| ... | ... | Pending |

## Local Gate Coverage

This section records repository-side release-gate coverage only. It does not prove {{scope}}.

Commands and files checked locally on {{YYYY-MM-DD}}:

```bash
# list the relevant scripts/workflows/env files
```

Observed local gate coverage:

- ...

## Apple References

- {{topic}}: https://developer.apple.com/...
```

Notes on the skeleton:

- The marker line (`{{marker}}=false`) must be on its own line with no surrounding backticks — the CI greps for the literal string.
- `status_guard_clause` explains what must be true before the marker can flip to `true`. Keep it to one sentence.
- For non-gated docs (no `marker` in the catalog, e.g. `APP_STORE_BLOCKER_REGISTER`, `APP_STORE_FINAL_SIGNOFF` — though those two DO have markers), omit the marker block and use the doc's own `Status:` line.
- `docs/APP_STORE_FINAL_SIGNOFF.md` uses marker `APP_STORE_FINAL_SIGNOFF_COMPLETE`; `docs/APP_STORE_BLOCKER_REGISTER.md` has no marker but a `Status: NOT READY` line. Trust the catalog.

## Refreshing the catalog

If Apple changes a requirement or Wakeve adds a new gated evidence type, update `catalog.json` first (add the entry + marker), then regenerate. The canonical list of gated markers lives in `.github/workflows/store-readiness.yml` — search for `_EVIDENCE_COMPLETE` to confirm the marker spelling before adding.

## Usage examples

- `evidence-generator dsa` → creates/refreshes `docs/APP_STORE_DSA_TRADER_STATUS.md` ... wait, that doc is **not** in the gated set; the gated file is `APP_STORE_DSA_TRADER_STATUS_EVIDENCE`? Check `catalog.json` — aliases disambiguate. Default to the gated file when two exist.
- `evidence-generator live-url-aasa complete` → refreshes `docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md` and flips its marker to `true`.
- `evidence-generator account-access` → scaffolds `docs/APP_STORE_ACCOUNT_ACCESS_EVIDENCE.md` at `PENDING`.
