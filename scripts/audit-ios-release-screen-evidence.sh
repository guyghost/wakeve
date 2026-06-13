#!/usr/bin/env bash
# Audits local iOS screenshot evidence against the release-flow screens listed
# in ROADMAP.md P1.2.

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="$PROJECT_DIR/docs/ios-release-screen-evidence"
FAIL_ON_MISSING=false

usage() {
    cat <<'USAGE'
Usage: ./scripts/audit-ios-release-screen-evidence.sh [--fail-on-missing] [--output-dir DIR]

Writes a Markdown report mapping local simulator screenshots to the release
screens required by ROADMAP.md P1.2: onboarding, login/guest, create event,
event detail, and organization.

Options:
  --fail-on-missing  Exit non-zero when any required screen has no local evidence.
  --output-dir DIR   Directory for the generated Markdown report.
  --help, -h         Show this help.
USAGE
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --fail-on-missing)
            FAIL_ON_MISSING=true
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
report="$OUTPUT_DIR/release-screen-evidence-$timestamp.md"
index_file="$PROJECT_DIR/docs/app-store-evidence/README.md"
tmp_dir="${TMPDIR:-/tmp}/wakeve-release-screen-evidence-$$"
mkdir -p "$tmp_dir"
trap 'rm -rf "$tmp_dir"' EXIT

inventory="$tmp_dir/inventory.txt"
if [ -f "$index_file" ]; then
    rg '^\| `[^`]+\.(png|jpg|jpeg)` \|' "$index_file" | sed 's/`//g' > "$inventory"
else
    find "$PROJECT_DIR/docs/app-store-evidence" \
        -maxdepth 1 \
        -type f \
        \( -iname '*.png' -o -iname '*.jpg' -o -iname '*.jpeg' \) \
        -print 2>/dev/null | sed "s|$PROJECT_DIR/docs/app-store-evidence/||" > "$inventory"
fi

missing=0
required_count=0
passed_count=0

screen_match() {
    local pattern="$1"
    rg -i "$pattern" "$inventory" || true
}

append_screen() {
    local name="$1"
    local pattern="$2"
    local required="$3"
    local capture_target="$4"
    local matches_file="$tmp_dir/$name.txt"

    required_count=$((required_count + 1))
    screen_match "$pattern" > "$matches_file"

    local count
    count="$(grep -c . "$matches_file" 2>/dev/null || true)"
    local result="PASS"
    if [ "$count" -eq 0 ]; then
        result="MISSING"
        missing=$((missing + 1))
    else
        passed_count=$((passed_count + 1))
    fi

    {
        echo "| $required | $result | $count | $capture_target |"
    } >> "$report"

    {
        echo "### $required"
        echo ""
        if [ "$count" -eq 0 ]; then
            echo "No matching local screenshot evidence was found."
        else
            echo '```text'
            sed 's/^/- /' "$matches_file"
            echo '```'
        fi
        echo ""
        echo "Next capture target: $capture_target"
        echo ""
    } >> "$tmp_dir/details.md"
}

{
    echo "# iOS Release Screen Evidence Audit"
    echo ""
    echo "Generated: $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
    echo ""
    echo "Status: LOCAL EVIDENCE"
    echo ""
    echo "This report maps local simulator screenshots to the release-flow screens listed in ROADMAP.md P1.2. It does not close App Store/TestFlight screenshot evidence; final closure still requires the uploaded review build or TestFlight build."
    echo ""
    echo "## Summary"
    echo ""
    echo "| Required screen | Local result | Local matches | Next capture target |"
    echo "| --- | --- | ---: | --- |"
} > "$report"

details_file="$tmp_dir/details.md"
: > "$details_file"

append_screen "onboarding" "onboarding" "Onboarding" "Uploaded review build: first-run onboarding carousel"
append_screen "login_guest" "login|guest" "Login and guest path" "Uploaded review build: login screen plus Continue as guest result"
append_screen "create_event" "create[- ]event|event creation" "Create event" "Uploaded review build: create-event wizard and preview"
append_screen "event_detail" "event[- ]detail|detail view|event detail" "Event detail" "Uploaded review build: event detail for a seeded or reviewer-created event"
append_screen "organization" "organization|organisation|organizing|scenario organization" "Organization" "Uploaded review build: confirmed/organizing event with scenario or organization dashboard visible"

{
    echo ""
    echo "Local coverage: $passed_count / $required_count required screens"
    echo ""
    echo "Missing required screens: $missing"
    echo ""
    echo "## Inventory Source"
    echo ""
    if [ -f "$index_file" ]; then
        echo "- Source index: \`docs/app-store-evidence/README.md\`"
    else
        echo "- Source index missing; inventory was built directly from \`docs/app-store-evidence/*.{jpg,jpeg,png}\`."
    fi
    echo "- Inventory rows considered: $(grep -c . "$inventory" 2>/dev/null || true)"
    echo ""
    echo "## Matched Evidence"
    echo ""
    cat "$details_file"
    echo "## Closure Notes"
    echo ""
    echo "- Treat MISSING rows as the next screenshot-capture targets."
    echo "- Local PASS rows prove only that a matching local simulator screenshot is indexed; they do not prove App Store Connect media readiness."
    echo "- Do not set App Store media, accessibility, or TestFlight evidence markers true from this local audit alone."
    echo "- Re-run after capturing screenshots from the uploaded TestFlight/App Review build."
} >> "$report"

echo "$report"

if [ "$FAIL_ON_MISSING" = true ] && [ "$missing" -gt 0 ]; then
    exit 1
fi
