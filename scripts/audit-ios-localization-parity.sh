#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
. "$PROJECT_DIR/scripts/lib/report-sanitization.sh"
OUTPUT_DIR="$PROJECT_DIR/docs/a11y"
WRITE_REPORT=false
FAIL_ON_FINDINGS=false
BASE_LOCALE="en"

usage() {
    cat <<'USAGE'
Usage: scripts/audit-ios-localization-parity.sh [options]

Checks iOS Localizable.strings files for plist syntax, duplicate keys, and
missing/extra keys versus a base locale. By default this prints a compact
summary and does not write evidence files.

Options:
  --write-report       Write a Markdown report under docs/a11y/.
  --fail-on-findings   Exit non-zero when any syntax, duplicate, missing, or
                       extra-key finding is present.
  --base LOCALE        Base locale. Default: en.
  --output DIR         Output directory when --write-report is set.
  -h, --help           Show this help.
USAGE
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --write-report)
            WRITE_REPORT=true
            ;;
        --fail-on-findings)
            FAIL_ON_FINDINGS=true
            ;;
        --base)
            BASE_LOCALE="${2:?Missing value for --base}"
            shift
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

if [ "$WRITE_REPORT" = true ]; then
    mkdir -p "$OUTPUT_DIR"
    REPORT="$OUTPUT_DIR/ios-localization-parity-$(date -u +"%Y-%m-%dT%H-%M-%SZ").md"
else
    REPORT=""
fi

sanitize_report() {
    sanitize_report_file "$1"
}

set +e
ruby - "$PROJECT_DIR" "$BASE_LOCALE" "$REPORT" <<'RUBY'
project_dir = ARGV.fetch(0)
base_locale = ARGV.fetch(1)
report = ARGV.fetch(2)
resource_root = File.join(project_dir, "iosApp", "src", "Resources")
files = Dir.glob(File.join(resource_root, "*.lproj", "Localizable.strings")).sort

if files.empty?
  warn "No iOS Localizable.strings files found under #{resource_root}"
  exit 2
end

base_file = File.join(resource_root, "#{base_locale}.lproj", "Localizable.strings")
unless File.exist?(base_file)
  warn "Base locale file not found: #{base_file}"
  exit 2
end

def locale_for(path)
  File.basename(File.dirname(path)).sub(/\.lproj\z/, "")
end

def parse_keys(path)
  keys = []
  File.readlines(path, chomp: true).each_with_index do |line, index|
    key = line[/^"([^"]+)"\s*=/, 1]
    keys << [key, index + 1] if key
  end
  keys
end

syntax = {}
keys_by_file = {}
duplicates_by_file = {}

files.each do |path|
  syntax[path] = system("plutil", "-lint", path, out: File::NULL, err: File::NULL)
  pairs = parse_keys(path)
  keys_by_file[path] = pairs.map(&:first)
  grouped = pairs.group_by(&:first)
  duplicates_by_file[path] = grouped.select { |_key, rows| rows.length > 1 }.transform_values { |rows| rows.map(&:last) }
end

base_keys = keys_by_file.fetch(base_file).sort
rows = files.map do |path|
  keys = keys_by_file.fetch(path).sort
  missing = base_keys - keys
  extra = keys - base_keys
  duplicates = duplicates_by_file.fetch(path)
  {
    path: path,
    locale: locale_for(path),
    syntax_ok: syntax.fetch(path),
    count: keys.length,
    duplicate_count: duplicates.keys.length,
    duplicates: duplicates,
    missing: missing,
    extra: extra
  }
end

total_findings = rows.sum do |row|
  (row[:syntax_ok] ? 0 : 1) + row[:duplicate_count] + row[:missing].length + row[:extra].length
end

puts "iOS localization parity: #{rows.length} locales, base=#{base_locale}, findings=#{total_findings}"
rows.each do |row|
  status = row[:syntax_ok] && row[:duplicate_count].zero? && row[:missing].empty? && row[:extra].empty? ? "PASS" : "FAIL"
  puts "#{status}: #{row[:locale]} count=#{row[:count]} duplicates=#{row[:duplicate_count]} missing=#{row[:missing].length} extra=#{row[:extra].length}"
end

unless report.empty?
  File.open(report, "w") do |file|
    file.puts "# iOS Localization Parity Audit"
    file.puts
    file.puts "Generated: #{Time.now.utc.strftime("%Y-%m-%dT%H:%M:%SZ")}"
    file.puts
    file.puts "Status: LOCAL SOURCE AUDIT"
    file.puts
    file.puts "This report supports roadmap P1.3 by checking iOS localization key parity. It does not prove App Store Connect localized metadata or signed-build UI behavior."
    file.puts
    file.puts "Base locale: `#{base_locale}`"
    file.puts
    file.puts "## Summary"
    file.puts
    file.puts "| Locale | Syntax | Keys | Duplicate Keys | Missing vs #{base_locale} | Extra vs #{base_locale} |"
    file.puts "| --- | --- | ---: | ---: | ---: | ---: |"
    rows.each do |row|
      file.puts "| #{row[:locale]} | #{row[:syntax_ok] ? "OK" : "FAIL"} | #{row[:count]} | #{row[:duplicate_count]} | #{row[:missing].length} | #{row[:extra].length} |"
    end
    file.puts "| Total findings |  |  |  |  | #{total_findings} |"
    file.puts
    file.puts "## Findings"
    file.puts
    if total_findings.zero?
      file.puts "No plist syntax, duplicate-key, missing-key, or extra-key findings were found."
    else
      rows.each do |row|
        next if row[:syntax_ok] && row[:duplicates].empty? && row[:missing].empty? && row[:extra].empty?

        file.puts "### #{row[:locale]}"
        file.puts
        file.puts "- Syntax: #{row[:syntax_ok] ? "OK" : "FAIL"}"
        unless row[:duplicates].empty?
          file.puts "- Duplicate keys:"
          row[:duplicates].each do |key, lines|
            file.puts "  - `#{key}` on lines #{lines.join(", ")}"
          end
        end
        file.puts "- Missing keys: #{row[:missing].map { |key| "`#{key}`" }.join(", ")}" unless row[:missing].empty?
        file.puts "- Extra keys: #{row[:extra].map { |key| "`#{key}`" }.join(", ")}" unless row[:extra].empty?
        file.puts
      end
    end
    file.puts
    file.puts "## Closure Notes"
    file.puts
    file.puts "- Add every release-visible iOS key to all supported `.lproj/Localizable.strings` files."
    file.puts "- Keep App Store metadata localization evidence separate from app source string parity."
  end
  puts report
end

exit(total_findings.zero? ? 0 : 1)
RUBY
status=$?
set -e
sanitize_report "$REPORT"
if [ "$FAIL_ON_FINDINGS" = true ]; then
    exit "$status"
fi

exit 0
