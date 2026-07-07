#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
. "$PROJECT_DIR/scripts/lib/report-sanitization.sh"
OUTPUT_DIR="${OUTPUT_DIR:-$PROJECT_DIR/docs/app-store-payment}"
TIMESTAMP="$(date -u +"%Y-%m-%dT%H-%M-%SZ")"
REPORT="$OUTPUT_DIR/payment-compliance-$TIMESTAMP.md"

mkdir -p "$OUTPUT_DIR"

sanitize_report() {
    sanitize_report_file "$REPORT"
}

IAP_PATTERN='StoreKit|SKProduct|SKProductsRequest|SKPayment|Product\.products|InAppPurchase|purchase\(|paywall'
PAYMENT_SURFACE_PATTERN='PaymentPotRepository|TricountHandoffRepository|PaymentRoutes|paymentRoutes|tricountGroupUrl|providerUrl|isTrustedPaymentLink|isTrustedProviderUrl'
POLICY_PATTERN='real-world shared event expenses|do not sell or unlock app features|trusted-domain validated|Tricount'
SOURCE_SCOPE=(
    "$PROJECT_DIR/iosApp/src"
    "$PROJECT_DIR/shared/src/commonMain/kotlin"
    "$PROJECT_DIR/server/src/main/kotlin"
    "$PROJECT_DIR/apps/landing/src"
)
POLICY_SCOPE=(
    "$PROJECT_DIR/composeApp/metadata/ios/review_information/notes.txt"
    "$PROJECT_DIR/docs/APP_STORE_PAYMENT_COMPLIANCE.md"
    "$PROJECT_DIR/docs/APP_STORE_REVIEW_GUIDELINE_AUDIT.md"
)

scan_iap="$(rg -n "$IAP_PATTERN" "${SOURCE_SCOPE[@]}" 2>&1 || true)"
scan_payment_surfaces="$(rg -n "$PAYMENT_SURFACE_PATTERN" "${SOURCE_SCOPE[@]}" 2>&1 || true)"
scan_policy="$(rg -n "$POLICY_PATTERN" "${POLICY_SCOPE[@]}" 2>&1 || true)"

if [ -n "$scan_iap" ]; then
    status="REVIEW_REQUIRED_IAP_OR_PAYWALL_MATCH"
elif [ -z "$scan_payment_surfaces" ]; then
    status="PAYMENT_SURFACES_NOT_FOUND"
elif [ -z "$scan_policy" ]; then
    status="PAYMENT_POLICY_TEXT_NOT_FOUND"
else
    status="LOCAL_SOURCE_SCAN_READY"
fi

cat > "$REPORT" <<EOF
# App Store Payment Compliance Audit

Generated: $(date -u +"%Y-%m-%dT%H:%M:%SZ")

Status: \`$status\`

This report supports AS-11 payment and external-purchase compliance evidence.
It is a local source and policy scan only. It does not complete App Store
payment evidence until the uploaded TestFlight/App Review build is inspected.

## Summary

| Field | Value |
| --- | --- |
| IAP/paywall source matches | \`$(if [ -n "$scan_iap" ]; then printf 'present'; else printf 'none'; fi)\` |
| Payment/Tricount surface matches | \`$(if [ -n "$scan_payment_surfaces" ]; then printf 'present'; else printf 'missing'; fi)\` |
| Review-note/policy matches | \`$(if [ -n "$scan_policy" ]; then printf 'present'; else printf 'missing'; fi)\` |
| Generated report can close AS-11 | \`no - local scan only\` |

## StoreKit / IAP / Paywall Scan

Pattern:

\`\`\`text
$IAP_PATTERN
\`\`\`

Result:

\`\`\`text
$(if [ -n "$scan_iap" ]; then printf '%s\n' "$scan_iap"; else printf 'NO_MATCHES\n'; fi)
\`\`\`

## Payment Surface Scan

Pattern:

\`\`\`text
$PAYMENT_SURFACE_PATTERN
\`\`\`

Result:

\`\`\`text
$(if [ -n "$scan_payment_surfaces" ]; then printf '%s\n' "$scan_payment_surfaces"; else printf 'NO_MATCHES\n'; fi)
\`\`\`

## Policy And App Review Notes Scan

Pattern:

\`\`\`text
$POLICY_PATTERN
\`\`\`

Result:

\`\`\`text
$(if [ -n "$scan_policy" ]; then printf '%s\n' "$scan_policy"; else printf 'NO_MATCHES\n'; fi)
\`\`\`

## Required Closure Evidence

Record all fields below before setting \`APP_STORE_PAYMENT_EVIDENCE_COMPLETE=true\`:

| Field | Value |
| --- | --- |
| App Store Connect version | TODO |
| Build number | TODO |
| Release commit | TODO |
| Payment surfaces inspected in uploaded build | TODO |
| Payment pots limited to real-world shared-event expenses | TODO |
| Settlement suggestions do not unlock app features or digital content | TODO |
| Tricount/provider URLs are HTTPS-only and trusted-domain validated | TODO |
| Non-Tricount external payment-provider URLs rejected | TODO |
| StoreKit/IAP absence verified against uploaded build | TODO |
| App Review notes match uploaded build behavior | TODO |
| Reviewer/date | TODO |

## Non-Closure Conditions

Do not mark AS-11 complete if any of these are true:

- Only this local source scan is available.
- The uploaded TestFlight/App Review build was not inspected.
- Payment, settlement, or Tricount screens are hidden locally but visible in the uploaded build without review evidence.
- Any external payment flow unlocks app features, subscriptions, digital content, digital credits, boosts, or premium functionality.
- Arbitrary external payment-provider URLs are accepted for payment pots.
- App Review notes do not explain real-world shared-event expenses and no digital unlocks.
EOF

sanitize_report

echo "$REPORT"
