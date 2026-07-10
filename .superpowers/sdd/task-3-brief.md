### Task 3: Locale Parity, Forbidden Terms, and Literal Audit Foundation

**Delegation:** `@tests` authors fixtures/assertions; `@codegen` implements scanner; `@review` approves every allowlist line.

**Files:**
- Create: `scripts/audit-product-language.sh`
- Create: `scripts/lib/audit_product_language.rb`
- Create: `scripts/product-language-allowlist.txt`
- Create: `scripts/tests/audit-product-language-test.sh`
- Modify: `scripts/test-critical-release-gates.sh`

**Interfaces:**
- Consumes: Android `values*/strings.xml`, iOS `*.lproj/Localizable.strings`, Siri locale resources, Kotlin/Swift production sources.
- Produces: executable `scripts/audit-product-language.sh`; exit 0 only with six-locale parity and no unallowlisted visible literal/term.

- [ ] **Step 1: Create an executable no-op scanner, then write a failing black-box scanner test**

Create the wrapper scaffold and make it executable before the fixture test:

```bash
#!/usr/bin/env bash
set -euo pipefail
exit 0
```

Run: `chmod +x scripts/audit-product-language.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail
tmp="$(mktemp -d)"; trap 'rm -rf "$tmp"' EXIT
for locale in en fr de es it pt; do
  dir="values-$locale"; [[ "$locale" == en ]] && dir="values"
  mkdir -p "$tmp/android/$dir"
  printf '<resources><string name="tab_ideas">Ideas</string></resources>' > "$tmp/android/$dir/strings.xml"
done
mkdir -p "$tmp/composeApp/src/androidMain/kotlin/example"

expect_failure() {
  local expected="$1"; shift
  local output
  if output="$("$@" 2>&1)"; then echo "expected failure containing: $expected" >&2; exit 1; fi
  grep -Fq "$expected" <<<"$output"
}

sed -i '' 's/name="tab_ideas"/name="other"/' "$tmp/android/values-fr/strings.xml"
expect_failure 'android fr: missing tab_ideas' scripts/audit-product-language.sh --fixture-root "$tmp" --catalogs-only
cp "$tmp/android/values/strings.xml" "$tmp/android/values-fr/strings.xml"

sed -i '' 's/>Ideas</>Wakeve AI</' "$tmp/android/values-de/strings.xml"
expect_failure 'android/de:tab_ideas: forbidden visible term' scripts/audit-product-language.sh --fixture-root "$tmp" --forbidden-terms-only
cp "$tmp/android/values/strings.xml" "$tmp/android/values-de/strings.xml"

printf 'fun Example() { Text("Inbox") }\n' > "$tmp/composeApp/src/androidMain/kotlin/example/Example.kt"
expect_failure 'composeApp/src/androidMain/kotlin/example/Example.kt:1: production literal' scripts/audit-product-language.sh --fixture-root "$tmp" --forbidden-terms-only
rm "$tmp/composeApp/src/androidMain/kotlin/example/Example.kt"

scripts/audit-product-language.sh --fixture-root "$tmp"
```

- [ ] **Step 2: Run RED**

Run: `bash scripts/tests/audit-product-language-test.sh`

Expected: fixture script executes and exits 1 at `expected failure containing: android fr: missing tab_ideas`, because the no-op scanner incorrectly accepts invalid catalogs.

- [ ] **Step 3: Implement the scanner contract**

```bash
#!/usr/bin/env bash
set -euo pipefail
root="."
mode="all"
while (($#)); do
  case "$1" in
    --fixture-root) root="${2:?missing fixture root}"; shift 2 ;;
    --catalogs-only) mode="catalogs"; shift ;;
    --forbidden-terms-only) mode="terms"; shift ;;
    *) echo "unknown argument: $1" >&2; exit 64 ;;
  esac
done
exec ruby scripts/lib/audit_product_language.rb "$root" "$mode"
```

Create `scripts/lib/audit_product_language.rb` with this complete executable implementation; any future scanner rule must first add a failing fixture to `scripts/tests/audit-product-language-test.sh`:

```ruby
#!/usr/bin/env ruby
require 'set'
root, mode = ARGV
abort 'usage: audit_product_language.rb ROOT MODE' unless root && %w[all catalogs terms].include?(mode)
locales = %w[en fr de es it pt]
findings = []
fixture = File.directory?(File.join(root, 'android'))
allowlist_path = File.join(root, 'scripts/product-language-allowlist.txt')
allowlist = File.file?(allowlist_path) ? File.readlines(allowlist_path, chomp: true).reject { |line| line.empty? || line.start_with?('#') }.to_set : Set.new
forbidden = /Wakeve AI|\bSc[eé]nario(?:s)?\b|\bScenario(?:s)?\b|\bInbox\b|\bGenerate\b|\bGénérer\b|Party Animal|Social Butterfly|Chatterbox|Event Master|Maître du vote/i

android_paths = locales.to_h do |locale|
  directory = locale == 'en' ? 'values' : "values-#{locale}"
  base = fixture ? 'android' : 'composeApp/src/androidMain/res'
  [locale, File.join(root, base, directory, 'strings.xml')]
end

ios_paths = fixture ? {} : locales.to_h { |locale| [locale, File.join(root, "iosApp/src/Resources/#{locale}.lproj/Localizable.strings")] }
siri_paths = fixture ? {} : locales.to_h { |locale| [locale, File.join(root, "iosApp/src/Siri/#{locale}.lproj/SiriIntents.strings")] }

def android_entries(path)
  return {} unless File.file?(path)
  File.read(path).scan(/<(?:string|plurals|string-array)\b[^>]*\bname="([^"]+)"[^>]*>(.*?)<\/(?:string|plurals|string-array)>/m).to_h.transform_values { |value| value.gsub(/<[^>]+>/, ' ').gsub(/\s+/, ' ').strip }
end

def apple_entries(path)
  return {} unless File.file?(path)
  File.read(path).scan(/^\s*"((?:\\.|[^"])*)"\s*=\s*"((?:\\.|[^"])*)"\s*;/).to_h
end

catalogs = {
  'android' => android_paths.transform_values { |path| android_entries(path) },
  'ios' => ios_paths.transform_values { |path| apple_entries(path) },
  'siri' => siri_paths.transform_values { |path| apple_entries(path) },
}
paths = { 'android' => android_paths, 'ios' => ios_paths, 'siri' => siri_paths }

if %w[all catalogs].include?(mode)
  catalogs.each do |platform, entries_by_locale|
    next if entries_by_locale.empty?
    base_keys = entries_by_locale.fetch('en').keys.to_set
    locales.each do |locale|
      path = paths.fetch(platform).fetch(locale)
      findings << "#{platform} #{locale}: missing file #{path}" unless File.file?(path)
      keys = entries_by_locale.fetch(locale, {}).keys.to_set
      (base_keys - keys).sort.each { |key| findings << "#{platform} #{locale}: missing #{key}" }
      (keys - base_keys).sort.each { |key| findings << "#{platform} #{locale}: extra #{key}" }
    end
  end
end

if %w[all terms].include?(mode)
  catalogs.each do |platform, entries_by_locale|
    entries_by_locale.each do |locale, entries|
      entries.each do |key, value|
        identity = "#{platform}/#{locale}:#{key}"
        findings << "#{identity}: forbidden visible term" if value.match?(forbidden) && !allowlist.include?(identity)
      end
    end
  end

  source_globs = ['composeApp/src/androidMain/**/*.kt', 'iosApp/src/**/*.swift']
  source_globs.flat_map { |glob| Dir.glob(File.join(root, glob)) }.sort.each do |path|
    next if path.include?('/Preview') || path.include?('/Tests/')
    File.readlines(path, chomp: true).each_with_index do |line, index|
      relative = path.delete_prefix("#{root}/")
      identity = "#{relative}:#{index + 1}"
      findings << "#{identity}: forbidden visible term" if line.match?(forbidden) && !allowlist.include?(identity)
      literal = line.match?(/(?:Text|Button|accessibilityLabel|contentDescription)\s*[=(]\s*"[^"\\]+"/)
      findings << "#{identity}: production literal" if literal && !allowlist.include?(identity)
    end
  end
end

puts findings.uniq.sort
exit findings.empty? ? 0 : 1
```

The allowlist format is one exact `relative/path:line` for source or `platform/locale:key` for a resource value. Do not allow glob entries or term-wide exceptions.

- [ ] **Step 4: Run fixture and repository audits**

Run: `bash scripts/tests/audit-product-language-test.sh && scripts/audit-product-language.sh`

Expected: all three RED fixtures observe their precise finding, the clean fixture exits 0, and the repository audit FAILS with current catalog/value/literal debt.

- [ ] **Step 5: Wire the release gate and commit**

Add `run_gate "product language" scripts/audit-product-language.sh` alongside existing critical gates.

```bash
git add scripts/audit-product-language.sh scripts/lib/audit_product_language.rb scripts/product-language-allowlist.txt scripts/tests/audit-product-language-test.sh scripts/test-critical-release-gates.sh
git commit -m "test(product-language): add release audit foundation"
```

