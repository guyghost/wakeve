#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="${OUTPUT_DIR:-$PROJECT_DIR/docs/app-store-dsa}"
TIMESTAMP="$(date -u +"%Y-%m-%dT%H-%M-%SZ")"
REPORT="$OUTPUT_DIR/dsa-trader-status-$TIMESTAMP.md"

mkdir -p "$OUTPUT_DIR"

sanitize_report() {
    perl -pi -e 'BEGIN { $home = $ENV{"HOME"} // ""; $home = quotemeta($home); } s/\r//g; s/[ \t]+$//; s/$home/~/g if $home ne "";' "$REPORT"
}

dsa_file="$PROJECT_DIR/docs/APP_STORE_DSA_TRADER_STATUS.md"
pricing_file="$PROJECT_DIR/docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md"
availability_file="$PROJECT_DIR/docs/APP_STORE_AVAILABILITY_EVIDENCE.md"
final_signoff_file="$PROJECT_DIR/docs/APP_STORE_FINAL_SIGNOFF.md"

marker_state() {
    local file="$1"
    local marker="$2"

    if [ ! -f "$file" ]; then
        printf 'missing file'
    elif grep -Fxq "$marker=true" "$file"; then
        printf 'complete marker present'
    elif grep -Fxq "$marker=false" "$file"; then
        printf 'incomplete marker present'
    else
        printf 'marker missing'
    fi
}

dsa_marker="$(marker_state "$dsa_file" "APP_STORE_DSA_TRADER_STATUS_EVIDENCE_COMPLETE")"
pricing_marker="$(marker_state "$pricing_file" "APP_STORE_PRICING_AVAILABILITY_EVIDENCE_COMPLETE")"
availability_marker="$(marker_state "$availability_file" "APP_STORE_AVAILABILITY_EVIDENCE_COMPLETE")"
signoff_marker="$(marker_state "$final_signoff_file" "APP_STORE_FINAL_SIGNOFF_COMPLETE")"

status="PENDING_APP_STORE_CONNECT_DSA_DECISION"

cat > "$REPORT" <<EOF
# App Store DSA Trader Status Evidence Preparation

Generated: $(date -u +"%Y-%m-%dT%H:%M:%SZ")

Status: \`$status\`

This report supports AS-08 Digital Services Act trader-status evidence.
It is preparation evidence only. It cannot determine Wakeve's legal trader
status, cannot change App Store Connect, and cannot complete AS-08.

## Local Readiness Snapshot

| Field | Value |
| --- | --- |
| DSA evidence marker | \`$dsa_marker\` |
| Pricing/availability evidence marker | \`$pricing_marker\` |
| App availability evidence marker | \`$availability_marker\` |
| Final signoff marker | \`$signoff_marker\` |
| Generated report can close AS-08 | \`no - preparation evidence only\` |

## App Store Connect Decision Paths

Choose exactly one path before final upload:

1. Trader for EU distribution.
2. Non-trader for EU distribution.
3. EU storefronts disabled for the first release.

## Required Closure Evidence

Record all fields below before setting
\`APP_STORE_DSA_TRADER_STATUS_EVIDENCE_COMPLETE=true\` or
\`APP_STORE_DSA_TRADER_STATUS_CONFIRMED=true\`:

| Field | Value |
| --- | --- |
| App Store Connect app record | TODO |
| Bundle ID | TODO: com.guyghost.wakeve |
| Selected DSA path | TODO: trader / non-trader / EU storefronts disabled |
| Account-level DSA status reviewed | TODO |
| App-specific DSA status reviewed | TODO |
| EU storefront availability matches selected path | TODO |
| Pricing and availability evidence cross-check | TODO |
| App availability evidence cross-check | TODO |
| Product/legal owner approval | TODO |
| App Store Connect screenshot or export path | TODO |
| Reviewer/date | TODO |

### If Trader

| Field | Value |
| --- | --- |
| Account type checked | TODO: organization / individual |
| Contact address present for EU product page | TODO |
| Contact phone present for EU product page | TODO |
| Contact email present for EU product page | TODO |
| Required payment account details present | TODO |
| EU-law certification accepted | TODO |
| Labels and Markings URL entered or not applicable | TODO |

### If Non-Trader

| Field | Value |
| --- | --- |
| Non-trader declaration selected in App Store Connect | TODO |
| Consumer-rights disclosure accepted by owner | TODO |
| EU storefront list reviewed | TODO |

### If EU Storefronts Disabled

| Field | Value |
| --- | --- |
| EU countries excluded from storefront availability | TODO |
| Pricing/availability evidence records the same exclusion | TODO |
| Re-enable criteria or owner decision recorded | TODO |

## Non-Closure Conditions

Do not mark AS-08 complete if any of these are true:

- Only this preparation report exists.
- The selected DSA path is not visible in App Store Connect.
- Owner approval is missing.
- Pricing/availability evidence contradicts the selected DSA path.
- App availability evidence contradicts the selected DSA path.
- The report includes private trader contact details that should not be committed.
EOF

sanitize_report

echo "$REPORT"
