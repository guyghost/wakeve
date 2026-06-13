#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="$PROJECT_DIR/docs/a11y"
FAIL_ON_FINDINGS=false

usage() {
    cat <<'USAGE'
Usage: scripts/audit-ios-accessibility-source.sh [options]

Scans SwiftUI source for release-accessibility risks that are easy to regress:
hardcoded VoiceOver strings and single-line text without a nearby scaling/wrap
fallback. Writes a Markdown report under docs/a11y/.

Options:
  --fail-on-findings  Exit non-zero when findings are present.
  --output DIR        Output directory. Default: docs/a11y.
  -h, --help          Show this help.

This is a source audit. It does not replace Dynamic Type, VoiceOver, contrast,
or reduced-motion validation on the signed TestFlight/App Review build.
USAGE
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --fail-on-findings)
            FAIL_ON_FINDINGS=true
            ;;
        --output)
            OUTPUT_DIR="${2:?Missing value for --output}"
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage >&2
            exit 2
            ;;
    esac
    shift
done

mkdir -p "$OUTPUT_DIR"
REPORT="$OUTPUT_DIR/ios-accessibility-source-audit-$(date -u +"%Y-%m-%dT%H-%M-%SZ").md"
TMP_DIR="${TMPDIR:-/tmp}/wakeve-a11y-audit-$$"
mkdir -p "$TMP_DIR"
trap 'rm -rf "$TMP_DIR"' EXIT

SOURCE_ROOT="$PROJECT_DIR/iosApp/src"
HARDCODED="$TMP_DIR/hardcoded.txt"
LINE_LIMIT="$TMP_DIR/line-limit.txt"
PROGRESS_VIEW="$TMP_DIR/progress-view.txt"

ruby - "$SOURCE_ROOT" "$HARDCODED" <<'RUBY'
root = ARGV.fetch(0)
out = ARGV.fetch(1)
findings = []
patterns = [
  /\.(accessibility(?:Label|Hint|Value))\(\s*"([^"\\]*(?:\\.[^"\\]*)*)"/,
  /\b(accessibility(?:Label|Hint|Value)):\s*"([^"\\]*(?:\\.[^"\\]*)*)"/
]

Dir.glob(File.join(root, "**", "*.swift")).sort.each do |path|
  lines = File.readlines(path, chomp: true)
  lines.each_with_index do |line, index|
    patterns.each do |pattern|
      match = line.match(pattern)
      next unless match
      literal = match[2]
      next if literal.include?("\\(")

      findings << "#{path}:#{index + 1}: #{line.strip}"
      break
    end
  end
end

File.write(out, findings.join("\n"))
RUBY

ruby - "$SOURCE_ROOT" "$LINE_LIMIT" <<'RUBY'
root = ARGV.fetch(0)
out = ARGV.fetch(1)
risky = []
Dir.glob(File.join(root, "**", "*.swift")).sort.each do |path|
  lines = File.readlines(path, chomp: true)
  lines.each_with_index do |line, index|
    next unless line.include?(".lineLimit(1)")
    window = lines[index, 6].join("\n")
    next if window.include?(".minimumScaleFactor(")
    next if window.include?(".fixedSize(")
    next if window.include?(".allowsTightening(")
    next if window.include?(".dynamicTypeSize(")
    risky << "#{path}:#{index + 1}: #{line.strip}"
  end
end
File.write(out, risky.join("\n"))
RUBY

ruby - "$SOURCE_ROOT" "$PROGRESS_VIEW" <<'RUBY'
root = ARGV.fetch(0)
out = ARGV.fetch(1)
risky = []
Dir.glob(File.join(root, "**", "*.swift")).sort.each do |path|
  lines = File.readlines(path, chomp: true)
  lines.each_with_index do |line, index|
    next unless line.include?("ProgressView()")
    window = lines[index, 6].join("\n")
    next if window.include?(".accessibilityLabel(")
    next if window.include?(".accessibilityHidden(")

    risky << "#{path}:#{index + 1}: #{line.strip}"
  end
end
File.write(out, risky.join("\n"))
RUBY

hardcoded_count="$(grep -c . "$HARDCODED" 2>/dev/null || true)"
line_limit_count="$(grep -c . "$LINE_LIMIT" 2>/dev/null || true)"
progress_view_count="$(grep -c . "$PROGRESS_VIEW" 2>/dev/null || true)"
total_count=$((hardcoded_count + line_limit_count + progress_view_count))

{
    echo "# iOS Accessibility Source Audit"
    echo ""
    echo "Generated: $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
    echo ""
    echo "Status: LOCAL SOURCE AUDIT"
    echo ""
    echo "This report supports roadmap P1.3 by catching source-level accessibility risks. It does not close App Store accessibility evidence without device validation."
    echo ""
    echo "## Summary"
    echo ""
    echo "| Check | Findings |"
    echo "| --- | ---: |"
    echo "| Hardcoded accessibility labels/hints/values | $hardcoded_count |"
    echo "| Single-line text without nearby scaling/wrap fallback | $line_limit_count |"
    echo "| Indeterminate ProgressView without label or explicit hiding | $progress_view_count |"
    echo "| Total | $total_count |"
    echo ""
    echo "## Hardcoded Accessibility Strings"
    echo ""
    if [ "$hardcoded_count" -eq 0 ]; then
        echo "No hardcoded \`.accessibilityLabel(\"...\")\`, \`.accessibilityHint(\"...\")\`, \`.accessibilityValue(\"...\")\`, or named \`accessibilityLabel:\`/\`accessibilityHint:\`/\`accessibilityValue:\` string arguments were found."
    else
        echo '```text'
        sed "s|$PROJECT_DIR/||" "$HARDCODED"
        echo '```'
    fi
    echo ""
    echo "## Single-Line Text Risks"
    echo ""
    if [ "$line_limit_count" -eq 0 ]; then
        echo "No \`.lineLimit(1)\` calls without a nearby \`.minimumScaleFactor\`, \`.fixedSize\`, \`.allowsTightening\`, or \`.dynamicTypeSize\` fallback were found."
    else
        echo '```text'
        sed "s|$PROJECT_DIR/||" "$LINE_LIMIT"
        printf '\n'
        echo '```'
    fi
    echo ""
    echo "## Indeterminate ProgressView Risks"
    echo ""
    if [ "$progress_view_count" -eq 0 ]; then
        echo "No bare \`ProgressView()\` calls without a nearby \`.accessibilityLabel(...)\` or \`.accessibilityHidden(true)\` were found."
    else
        echo '```text'
        sed "s|$PROJECT_DIR/||" "$PROGRESS_VIEW"
        printf '\n'
        echo '```'
    fi
    echo ""
    echo "## Closure Notes"
    echo ""
    echo "- Fix hardcoded VoiceOver strings by using \`String(localized:)\` or localized view text."
    echo "- Label user-visible loading indicators, or hide decorative button spinners when the surrounding control already exposes the action state."
    echo "- Review single-line text in release screens under Dynamic Type accessibility sizes before claiming Larger Text support."
    echo "- Keep the App Store evidence marker false until signed-build device checks cover Dynamic Type, VoiceOver, high contrast, reduced motion, and color-only states."
} > "$REPORT"

echo "$REPORT"

if [ "$FAIL_ON_FINDINGS" = true ] && [ "$total_count" -gt 0 ]; then
    exit 1
fi
