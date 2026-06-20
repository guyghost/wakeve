#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
. "$PROJECT_DIR/scripts/lib/report-sanitization.sh"
OUTPUT_DIR="$PROJECT_DIR/docs/a11y"
FAIL_ON_FINDINGS=false

usage() {
    cat <<'USAGE'
Usage: scripts/audit-ios-accessibility-source.sh [options]

Scans SwiftUI source for release-accessibility risks that are easy to regress:
hardcoded VoiceOver strings, single-line text without a nearby scaling/wrap
fallback, unlabeled loading indicators, and icon-only buttons without an
accessible name. Writes a Markdown report under docs/a11y/.

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

sanitize_report() {
    sanitize_report_file "$REPORT"
}

SOURCE_ROOT="$PROJECT_DIR/iosApp/src"
HARDCODED="$TMP_DIR/hardcoded.txt"
LINE_LIMIT="$TMP_DIR/line-limit.txt"
PROGRESS_VIEW="$TMP_DIR/progress-view.txt"
ICON_BUTTON="$TMP_DIR/icon-button.txt"

ruby - "$SOURCE_ROOT" "$HARDCODED" <<'RUBY'
root = ARGV.fetch(0)
out = ARGV.fetch(1)
findings = []
patterns = [
  /\.(accessibility(?:Label|Hint|Value))\(/,
  /\b(accessibility(?:Label|Hint|Value)):\s*/
]

Dir.glob(File.join(root, "**", "*.swift")).sort.each do |path|
  lines = File.readlines(path, chomp: true)
  lines.each_with_index do |line, index|
    next unless patterns.any? { |pattern| line.match?(pattern) }

    has_hardcoded_literal = false
    line.to_enum(:scan, /"([^"\\]*(?:\\.[^"\\]*)*)"/).each do
      literal = Regexp.last_match(1)
      literal_start = Regexp.last_match.begin(0)
      prefix = line[0...literal_start]

      next if literal.include?("\\(")
      next if prefix.match?(/String\s*\(\s*localized:\s*$/)
      next if prefix.match?(/String\s*\(\s*format:\s*String\s*\(\s*localized:\s*$/)
      next if prefix.match?(/String\.localizedStringWithFormat\s*\(\s*String\s*\(\s*localized:\s*$/)
      next if prefix.match?(/(?:==|!=)\s*$/)

      has_hardcoded_literal = true
      break
    end

    findings << "#{path}:#{index + 1}: #{line.strip}" if has_hardcoded_literal
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

ruby - "$SOURCE_ROOT" "$ICON_BUTTON" <<'RUBY'
root = ARGV.fetch(0)
out = ARGV.fetch(1)
risky = []

def balanced_block(lines, start_index)
  block = []
  depth = 0
  seen_open = false

  lines[start_index, 40].to_a.each do |line|
    block << line
    line.each_char do |char|
      if char == "{"
        depth += 1
        seen_open = true
      elsif char == "}"
        depth -= 1
      end
    end
    break if seen_open && depth <= 0
  end

  block
end

Dir.glob(File.join(root, "**", "*.swift")).sort.each do |path|
  lines = File.readlines(path, chomp: true)
  lines.each_with_index do |line, index|
    next unless line.match?(/\bButton\b/) && line.include?("{")

    block_lines = balanced_block(lines, index)
    block = block_lines.join("\n")
    modifier_window = lines[index, block_lines.length + 8].join("\n")
    next unless block.include?("Image(systemName:")
    next if block.include?("Text(")
    next if block.include?("Label(")
    next if modifier_window.include?(".accessibilityLabel(")
    next if modifier_window.include?(".accessibilityHidden(")

    risky << "#{path}:#{index + 1}: #{line.strip}"
  end
end

File.write(out, risky.join("\n"))
RUBY

hardcoded_count="$(grep -c . "$HARDCODED" 2>/dev/null || true)"
line_limit_count="$(grep -c . "$LINE_LIMIT" 2>/dev/null || true)"
progress_view_count="$(grep -c . "$PROGRESS_VIEW" 2>/dev/null || true)"
icon_button_count="$(grep -c . "$ICON_BUTTON" 2>/dev/null || true)"
total_count=$((hardcoded_count + line_limit_count + progress_view_count + icon_button_count))

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
    echo "| Icon-only Button without accessible label or explicit hiding | $icon_button_count |"
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
    echo "## Icon-Only Button Risks"
    echo ""
    if [ "$icon_button_count" -eq 0 ]; then
        echo "No SwiftUI \`Button\` blocks with only \`Image(systemName:)\` content and no nearby \`.accessibilityLabel(...)\` or \`.accessibilityHidden(true)\` were found."
    else
        echo '```text'
        sed "s|$PROJECT_DIR/||" "$ICON_BUTTON"
        printf '\n'
        echo '```'
    fi
    echo ""
    echo "## Closure Notes"
    echo ""
    echo "- Fix hardcoded VoiceOver strings by using \`String(localized:)\` or localized view text."
    echo "- Give icon-only buttons an \`.accessibilityLabel(String(localized: ...))\`, or hide purely decorative icons inside controls that already expose a text label."
    echo "- Label user-visible loading indicators, or hide decorative button spinners when the surrounding control already exposes the action state."
    echo "- Review single-line text in release screens under Dynamic Type accessibility sizes before claiming Larger Text support."
    echo "- Keep the App Store evidence marker false until signed-build device checks cover Dynamic Type, VoiceOver, high contrast, reduced motion, and color-only states."
} > "$REPORT"

sanitize_report

echo "$REPORT"

if [ "$FAIL_ON_FINDINGS" = true ] && [ "$total_count" -gt 0 ]; then
    exit 1
fi
