#!/usr/bin/env bash
# Audits local App Store privacy-label alignment for the iOS release surface.

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_DIR="$PROJECT_DIR/docs/app-store-privacy"
FAIL_ON_FINDINGS=false

usage() {
    cat <<'USAGE'
Usage: ./scripts/audit-app-store-privacy-alignment.sh [--fail-on-findings] [--output-dir DIR]

Writes a Markdown report comparing the local privacy manifest, App Store
privacy-label draft, privacy policy source, Info.plist, and iOS source tracking
API usage. This is local evidence only; final closure still requires App Store
Connect labels, live privacy URL, owner approval, and the uploaded review build.

Options:
  --fail-on-findings  Exit non-zero when local contradictions are found.
  --output-dir DIR    Directory for the generated Markdown report.
  --help, -h          Show this help.
USAGE
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --fail-on-findings)
            FAIL_ON_FINDINGS=true
            shift
            ;;
        --output-dir)
            OUTPUT_DIR="${2:?Missing value for --output-dir}"
            shift 2
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage >&2
            exit 2
            ;;
    esac
done

mkdir -p "$OUTPUT_DIR"

timestamp="$(date -u '+%Y-%m-%dT%H-%M-%SZ')"
report="$OUTPUT_DIR/privacy-alignment-$timestamp.md"
tmp_dir="${TMPDIR:-/tmp}/wakeve-privacy-alignment-$$"
mkdir -p "$tmp_dir"
trap 'rm -rf "$tmp_dir"' EXIT

sanitize_report() {
    perl -pi -e 'BEGIN { $home = $ENV{"HOME"} // ""; $home = quotemeta($home); } s/\r//g; s/[ \t]+$//; s/$home/~/g if $home ne "";' "$report"
}

manifest="$PROJECT_DIR/iosApp/src/PrivacyInfo.xcprivacy"
info_plist="$PROJECT_DIR/iosApp/src/Info.plist"
labels_doc="$PROJECT_DIR/docs/APP_STORE_PRIVACY_LABELS.md"
policy_doc="$PROJECT_DIR/docs/PRIVACY_POLICY.md"
source_roots=(
    "$PROJECT_DIR/iosApp/src"
    "$PROJECT_DIR/shared/src/iosMain"
    "$PROJECT_DIR/shared/src/commonMain"
    "$PROJECT_DIR/composeApp/src"
)

required_types=(
    "NSPrivacyCollectedDataTypeName"
    "NSPrivacyCollectedDataTypeEmailAddress"
    "NSPrivacyCollectedDataTypeUserID"
    "NSPrivacyCollectedDataTypeDeviceID"
    "NSPrivacyCollectedDataTypeOtherUserContent"
    "NSPrivacyCollectedDataTypeCoarseLocation"
    "NSPrivacyCollectedDataTypeProductInteraction"
)

not_collected_labels=(
    "Contacts"
    "Browsing History"
    "Search History"
    "Financial Info"
    "Health and Fitness"
    "Sensitive Info"
    "Purchases"
    "Advertising Data"
)

findings=0
warnings=0

record_finding() {
    findings=$((findings + 1))
    printf '| FAIL | %s |\n' "$1" >> "$report"
}

record_pass() {
    printf '| PASS | %s |\n' "$1" >> "$report"
}

record_warning() {
    warnings=$((warnings + 1))
    printf '| PENDING | %s |\n' "$1" >> "$report"
}

manifest_json="$tmp_dir/privacy.json"
if ! plutil -convert json -o "$manifest_json" "$manifest" >/dev/null 2>&1; then
    echo "# App Store Privacy Alignment Audit" > "$report"
    echo "" >> "$report"
    echo "Result: FAIL. Could not parse \`iosApp/src/PrivacyInfo.xcprivacy\`." >> "$report"
    echo "$report"
    exit 1
fi

manifest_hash="$(shasum -a 256 "$manifest" | awk '{print $1}')"
policy_hash="$(shasum -a 256 "$policy_doc" | awk '{print $1}')"
labels_hash="$(shasum -a 256 "$labels_doc" | awk '{print $1}')"

tracking_value="$(ruby -rjson -e 'data = JSON.parse(File.read(ARGV.fetch(0))); puts data.fetch("NSPrivacyTracking", nil).inspect' "$manifest_json")"
tracking_domain_count="$(ruby -rjson -e 'data = JSON.parse(File.read(ARGV.fetch(0))); puts Array(data["NSPrivacyTrackingDomains"]).length' "$manifest_json")"
manifest_types_file="$tmp_dir/manifest-types.txt"
ruby -rjson -e 'data = JSON.parse(File.read(ARGV.fetch(0))); Array(data["NSPrivacyCollectedDataTypes"]).map { |entry| entry["NSPrivacyCollectedDataType"] }.compact.sort.each { |type| puts type }' "$manifest_json" > "$manifest_types_file"

{
    echo "# App Store Privacy Alignment Audit"
    echo ""
    echo "Generated: $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
    echo ""
    echo "Status: LOCAL EVIDENCE"
    echo ""
    echo "This report checks local privacy-label alignment. It does not close AS-05 until App Store Connect privacy labels, the live privacy URL, legal/privacy owner approval, and the uploaded review build are verified."
    echo ""
    echo "## Inputs"
    echo ""
    echo "| File | SHA-256 |"
    echo "| --- | --- |"
    echo "| \`iosApp/src/PrivacyInfo.xcprivacy\` | \`$manifest_hash\` |"
    echo "| \`docs/APP_STORE_PRIVACY_LABELS.md\` | \`$labels_hash\` |"
    echo "| \`docs/PRIVACY_POLICY.md\` | \`$policy_hash\` |"
    echo ""
    echo "## Local Checks"
    echo ""
    echo "| Result | Check |"
    echo "| --- | --- |"
} > "$report"

if [ "$tracking_value" = "false" ]; then
    record_pass "Privacy manifest declares NSPrivacyTracking=false."
else
    record_finding "Privacy manifest does not declare NSPrivacyTracking=false."
fi

if [ "$tracking_domain_count" = "0" ]; then
    record_pass "Privacy manifest declares no tracking domains."
else
    record_finding "Privacy manifest declares tracking domains while the privacy-label draft says no tracking."
fi

if grep -Fq "Does this app use data for tracking? **No**" "$labels_doc"; then
    record_pass "Privacy-label draft declares no tracking."
else
    record_finding "Privacy-label draft does not declare no tracking."
fi

if plutil -extract NSUserTrackingUsageDescription raw -o - "$info_plist" >/dev/null 2>&1; then
    record_finding "Info.plist declares NSUserTrackingUsageDescription while no-tracking is claimed."
else
    record_pass "Info.plist does not declare NSUserTrackingUsageDescription."
fi

tracking_api_pattern="AdSupport|ASIdentifierManager|advertisingIdentifier|AppTrackingTransparency|ATTrackingManager"
if rg -n "$tracking_api_pattern" "${source_roots[@]}" >/dev/null 2>&1; then
    record_finding "iOS/shared source contains IDFA or App Tracking Transparency API references."
else
    record_pass "iOS/shared source contains no IDFA or App Tracking Transparency API references."
fi

for data_type in "${required_types[@]}"; do
    if grep -Fxq "$data_type" "$manifest_types_file"; then
        record_pass "Privacy manifest declares $data_type."
    else
        record_finding "Privacy manifest is missing $data_type."
    fi

    if grep -Fq "$data_type" "$labels_doc"; then
        record_pass "Privacy-label draft mirrors $data_type."
    else
        record_finding "Privacy-label draft is missing $data_type."
    fi
done

for label in "${not_collected_labels[@]}"; do
    if grep -Fq "$label" "$labels_doc"; then
        record_pass "Privacy-label draft explicitly lists $label under data not collected."
    else
        record_finding "Privacy-label draft does not explicitly list $label under data not collected."
    fi
done

if grep -Fq "privacy@wakeve.app" "$policy_doc" && grep -Fq "Data Collection" "$policy_doc"; then
    record_pass "Privacy policy source includes contact and Data Collection sections."
else
    record_finding "Privacy policy source is missing privacy contact or Data Collection wording."
fi

if grep -Fq "Photos or Videos" "$labels_doc" && grep -Fq "calendar" "$labels_doc" && grep -Fq "Siri/speech" "$labels_doc" && grep -Fq "analytics/crash provider" "$labels_doc"; then
    record_pass "Privacy-label draft records required open verification questions."
else
    record_finding "Privacy-label draft is missing one or more required verification questions."
fi

record_warning "Open external question: App Store Connect privacy labels still need owner comparison against this draft."
record_warning "Open external question: live https://wakeve.app/privacy must be reachable and match docs/PRIVACY_POLICY.md."
record_warning "Open external question: uploaded review build must be checked for bundled PrivacyInfo.xcprivacy and no tracking strings."
record_warning "Open product/legal question: photos/media upload, calendar data, Siri/speech, analytics, and crash behavior need final owner confirmation."

{
    echo ""
    echo "## Manifest Data Types"
    echo ""
    echo '```text'
    cat "$manifest_types_file"
    echo '```'
    echo ""
    echo "## Summary"
    echo ""
    echo "| Metric | Count |"
    echo "| --- | ---: |"
    echo "| Local findings | $findings |"
    echo "| External pending confirmations | $warnings |"
    echo ""
    if [ "$findings" -eq 0 ]; then
        echo "Result: PASS for local privacy alignment. AS-05 remains open for App Store Connect, live URL, legal/privacy owner, and uploaded-build evidence."
    else
        echo "Result: FAIL. Local privacy alignment findings must be resolved before App Store privacy signoff."
    fi
} >> "$report"

sanitize_report

echo "$report"

if [ "$FAIL_ON_FINDINGS" = true ] && [ "$findings" -gt 0 ]; then
    exit 1
fi
