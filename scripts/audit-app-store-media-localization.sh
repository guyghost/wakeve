#!/usr/bin/env bash
# Audits the local iOS App Store screenshot and localized metadata payload.

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUTPUT_DIR="$PROJECT_DIR/docs/app-store-media-localization"
FAIL_ON_FINDINGS=false

usage() {
    cat <<'USAGE'
Usage: ./scripts/audit-app-store-media-localization.sh [--fail-on-findings] [--output-dir DIR]

Writes a Markdown report with the local iOS App Store screenshot inventory,
metadata field lengths, preview-video inventory, and aggregate media hash.

Options:
  --fail-on-findings  Exit non-zero when local media/metadata findings exist.
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
report="$OUTPUT_DIR/media-localization-$timestamp.md"
tmp_dir="${TMPDIR:-/tmp}/wakeve-media-localization-$$"
mkdir -p "$tmp_dir"
trap 'rm -rf "$tmp_dir"' EXIT

sanitize_report() {
    perl -pi -e 'BEGIN { $home = $ENV{"HOME"} // ""; $home = quotemeta($home); } s/\r//g; s/[ \t]+$//; s/$home/~/g if $home ne "";' "$report"
}

metadata_dir="$PROJECT_DIR/composeApp/metadata/ios"
upload_screenshot_dir="$PROJECT_DIR/composeApp/screenshots/ios"
locales=("en-US" "fr-FR")
metadata_fields=("name" "subtitle" "description" "keywords" "promotional_text" "release_notes" "privacy_url" "support_url")
findings=0

field_limit() {
    case "$1" in
        name) echo 30 ;;
        subtitle) echo 30 ;;
        description) echo 4000 ;;
        keywords) echo 100 ;;
        promotional_text) echo 170 ;;
        release_notes) echo 4000 ;;
        privacy_url) echo 500 ;;
        support_url) echo 500 ;;
        *) echo 99999 ;;
    esac
}

accepted_ios_size() {
    local width="$1"
    local height="$2"

    case "${width}x${height}" in
        1260x2736|2736x1260|1290x2796|2796x1290|1320x2868|2868x1320|\
1284x2778|2778x1284|1242x2688|2688x1242|1179x2556|2556x1179|\
1206x2622|2622x1206|1170x2532|2532x1170|1125x2436|2436x1125|\
1080x2340|2340x1080|1242x2208|2208x1242|750x1334|1334x750|\
2064x2752|2752x2064|2048x2732|2732x2048|1488x2266|2266x1488|\
1668x2420|2420x1668|1668x2388|2388x1668|1640x2360|2360x1640|\
1668x2224|2224x1668|1536x2048|2048x1536)
            return 0
            ;;
    esac

    return 1
}

device_family() {
    local width="$1"
    local height="$2"

    case "${width}x${height}" in
        2064x2752|2752x2064|2048x2732|2732x2048|1488x2266|2266x1488|\
1668x2420|2420x1668|1668x2388|2388x1668|1640x2360|2360x1640|\
1668x2224|2224x1668|1536x2048|2048x1536)
            echo "iPad"
            ;;
        *)
            echo "iPhone"
            ;;
    esac
}

char_count() {
    ruby -e 'print File.read(ARGV.fetch(0)).strip.length' "$1"
}

append_screenshot_table() {
    local root="$1"
    local label="$2"

    {
        echo "### $label"
        echo ""
        echo "| Locale | File | Dimensions | Family | SHA-256 | Result |"
        echo "| --- | --- | ---: | --- | --- | --- |"
    } >> "$report"

    local count=0
    while IFS= read -r file; do
        count=$((count + 1))
        local rel width height family hash result locale
        rel="${file#$PROJECT_DIR/}"
        locale="$(printf '%s' "$rel" | awk -F/ '
            $1 == "composeApp" && $2 == "metadata" && $3 == "ios" { print $4; next }
            $1 == "composeApp" && $2 == "screenshots" && $3 == "ios" { print $4; next }
            { print $(NF-1) }
        ')"
        width="$(sips -g pixelWidth "$file" 2>/dev/null | awk '/pixelWidth/ {print $2}')"
        height="$(sips -g pixelHeight "$file" 2>/dev/null | awk '/pixelHeight/ {print $2}')"
        hash="$(shasum -a 256 "$file" | awk '{print $1}')"
        if [ -n "$width" ] && [ -n "$height" ] && accepted_ios_size "$width" "$height"; then
            result="PASS"
        else
            result="FAIL"
            findings=$((findings + 1))
        fi
        family="$(device_family "${width:-0}" "${height:-0}")"
        echo "| \`$locale\` | \`$rel\` | ${width:-unknown}x${height:-unknown} | $family | \`$hash\` | $result |" >> "$report"
    done < <(find "$root" -type f \( -iname '*.png' -o -iname '*.jpg' -o -iname '*.jpeg' \) | sort)

    if [ "$count" -eq 0 ]; then
        echo "| n/a | \`$root\` | n/a | n/a | n/a | FAIL: no screenshots found |" >> "$report"
        findings=$((findings + 1))
    fi
    echo "" >> "$report"
}

aggregate_hash="unavailable"
if command -v shasum >/dev/null 2>&1; then
    aggregate_hash="$(cd "$PROJECT_DIR" && find composeApp/metadata/ios composeApp/screenshots/ios -type f \( -iname '*.png' -o -iname '*.jpg' -o -iname '*.jpeg' \) -print | sort | shasum -a 256 | awk '{print $1}')"
fi

preview_count="$(find "$metadata_dir" "$upload_screenshot_dir" -type f \( -iname '*.mov' -o -iname '*.mp4' -o -iname '*.m4v' \) | wc -l | tr -d ' ')"
if [ "$preview_count" != "0" ]; then
    findings=$((findings + 1))
fi

{
    echo "# App Store Media And Localization Audit"
    echo ""
    echo "Generated: $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
    echo ""
    echo "Status: LOCAL EVIDENCE"
    echo ""
    echo "This report audits local Fastlane/App Store metadata and screenshots. It does not close AS-20 until the App Store Connect media page and uploaded review build are checked."
    echo ""
    echo "## Summary"
    echo ""
    echo "| Check | Result |"
    echo "| --- | --- |"
    echo "| Locales | \`${locales[*]}\` |"
    echo "| Screenshot aggregate hash | \`$aggregate_hash\` |"
    echo "| App preview videos | $preview_count |"
    echo "| Findings | $findings |"
    echo ""
    echo "## Screenshot Inventory"
    echo ""
} > "$report"

append_screenshot_table "$upload_screenshot_dir" "Fastlane Upload Screenshots"
append_screenshot_table "$metadata_dir" "Metadata Screenshots"

{
    echo "## Localized Metadata Field Lengths"
    echo ""
    echo "| Locale | Field | Characters | Limit | Result |"
    echo "| --- | --- | ---: | ---: | --- |"
} >> "$report"

for locale in "${locales[@]}"; do
    for field in "${metadata_fields[@]}"; do
        file="$metadata_dir/$locale/$field.txt"
        limit="$(field_limit "$field")"
        if [ ! -f "$file" ]; then
            echo "| \`$locale\` | \`$field\` | n/a | $limit | FAIL: missing file |" >> "$report"
            findings=$((findings + 1))
            continue
        fi

        count="$(char_count "$file")"
        if [ "$count" -le "$limit" ]; then
            result="PASS"
        else
            result="FAIL"
            findings=$((findings + 1))
        fi
        echo "| \`$locale\` | \`$field\` | $count | $limit | $result |" >> "$report"
    done
done

{
    echo ""
    echo "## App Preview Inventory"
    echo ""
} >> "$report"

if [ "$preview_count" = "0" ]; then
    echo "No .mov, .mp4, or .m4v files were found under local iOS metadata or screenshot directories. First-release decision remains: omit app previews." >> "$report"
else
    echo '```text' >> "$report"
    find "$metadata_dir" "$upload_screenshot_dir" -type f \( -iname '*.mov' -o -iname '*.mp4' -o -iname '*.m4v' \) | sed "s|$PROJECT_DIR/||" | sort >> "$report"
    echo '```' >> "$report"
fi

{
    echo ""
    echo "## Closure Notes"
    echo ""
    echo "- Keep \`APP_STORE_MEDIA_LOCALIZATION_EVIDENCE_COMPLETE=false\` until App Store Connect media, localized metadata, editable status, and uploaded-build accuracy are reviewed."
    echo "- Attach the generated report to \`docs/APP_STORE_MEDIA_LOCALIZATION_EVIDENCE.md\` after meaningful media or metadata changes."
    echo "- If app previews are added, validate duration, format, poster frame, locale fallback, rights, and source-device capture."
} >> "$report"

sanitize_report

echo "$report"

if [ "$FAIL_ON_FINDINGS" = true ] && [ "$findings" -gt 0 ]; then
    exit 1
fi
