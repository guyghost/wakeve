#!/usr/bin/env bash
set -euo pipefail

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

for locale in en fr de es it pt; do
    android_dir="values-$locale"
    [[ "$locale" == fr ]] && android_dir="values"
    mkdir -p "$tmp/android/$android_dir" "$tmp/ios/$locale.lproj" "$tmp/siri/$locale.lproj"
    printf '<resources><string name="tab_ideas">Ideas</string><plurals name="event_count"><item quantity="one">%%1$d event</item><item quantity="other">%%1$d events</item></plurals><string-array name="filters"><item>%%1$s first</item><item>Second</item></string-array></resources>\n' > "$tmp/android/$android_dir/strings.xml"
    printf '"tab_ideas" = "Ideas";\n' > "$tmp/ios/$locale.lproj/Localizable.strings"
    printf '"intent_title" = "Plan event";\n' > "$tmp/siri/$locale.lproj/SiriIntents.strings"
done
mkdir -p "$tmp/composeApp/src/androidMain/kotlin/example" "$tmp/iosApp/src/Views" "$tmp/scripts"
printf '# Exact identities only.\n' > "$tmp/scripts/product-language-allowlist.txt"

expect_failure() {
    local expected="$1"
    shift
    local output
    if output="$("$@" 2>&1)"; then
        echo "expected failure containing: $expected" >&2
        exit 1
    fi
    if ! grep -Fq "$expected" <<<"$output"; then
        printf 'missing expected finding: %s\noutput:\n%s\n' "$expected" "$output" >&2
        exit 1
    fi
}

expect_failure_without() {
    local required="$1"
    local forbidden="$2"
    shift 2
    local output status=0
    output="$("$@" 2>&1)" || status=$?
    [[ "$status" -ne 0 ]] || { echo "expected command failure" >&2; exit 1; }
    grep -Fq "$required" <<<"$output"
    if grep -Fq "$forbidden" <<<"$output"; then
        printf 'unexpected finding: %s\noutput:\n%s\n' "$forbidden" "$output" >&2
        exit 1
    fi
}

sed -i '' 's/name="tab_ideas"/name="other"/' "$tmp/android/values/strings.xml"
expect_failure 'android fr: missing tab_ideas' scripts/audit-product-language.sh --fixture-root "$tmp" --catalogs-only
cp "$tmp/android/values-en/strings.xml" "$tmp/android/values/strings.xml"

sed -i '' 's/<string name="tab_ideas">/<plurals name="tab_ideas"><item quantity="other">/' "$tmp/android/values-de/strings.xml"
sed -i '' 's#</string><plurals name="event_count">#</item></plurals><plurals name="event_count">#' "$tmp/android/values-de/strings.xml"
expect_failure 'android de: kind mismatch tab_ideas' scripts/audit-product-language.sh --fixture-root "$tmp" --catalogs-only
cp "$tmp/android/values-en/strings.xml" "$tmp/android/values-de/strings.xml"

sed -i '' 's/<item quantity="one">%1$d event<\/item>//' "$tmp/android/values-es/strings.xml"
expect_failure 'android es: plural quantities mismatch event_count' scripts/audit-product-language.sh --fixture-root "$tmp" --catalogs-only
cp "$tmp/android/values-en/strings.xml" "$tmp/android/values-es/strings.xml"

sed -i '' 's/<item>Second<\/item>//' "$tmp/android/values-it/strings.xml"
expect_failure 'android it: array structure mismatch filters' scripts/audit-product-language.sh --fixture-root "$tmp" --catalogs-only
cp "$tmp/android/values-en/strings.xml" "$tmp/android/values-it/strings.xml"

sed -i '' 's/%1$s first/%2$s first/' "$tmp/android/values-pt/strings.xml"
expect_failure 'android pt: positional placeholders mismatch filters' scripts/audit-product-language.sh --fixture-root "$tmp" --catalogs-only
cp "$tmp/android/values-en/strings.xml" "$tmp/android/values-pt/strings.xml"

sed -i '' 's/"tab_ideas"/"other"/' "$tmp/ios/de.lproj/Localizable.strings"
expect_failure 'ios de: missing tab_ideas' scripts/audit-product-language.sh --fixture-root "$tmp" --catalogs-only
cp "$tmp/ios/en.lproj/Localizable.strings" "$tmp/ios/de.lproj/Localizable.strings"

sed -i '' 's/"intent_title"/"other"/' "$tmp/siri/es.lproj/SiriIntents.strings"
expect_failure 'siri es: missing intent_title' scripts/audit-product-language.sh --fixture-root "$tmp" --catalogs-only
cp "$tmp/siri/en.lproj/SiriIntents.strings" "$tmp/siri/es.lproj/SiriIntents.strings"

sed -i '' 's/>Ideas</>Wakeve AI</' "$tmp/android/values-de/strings.xml"
expect_failure 'android/de:tab_ideas: forbidden visible term' scripts/audit-product-language.sh --fixture-root "$tmp" --forbidden-terms-only
cp "$tmp/android/values/strings.xml" "$tmp/android/values-de/strings.xml"

sed -i '' 's/Ideas/Inbox/' "$tmp/ios/it.lproj/Localizable.strings"
expect_failure 'ios/it:tab_ideas: forbidden visible term' scripts/audit-product-language.sh --fixture-root "$tmp" --forbidden-terms-only
cp "$tmp/ios/en.lproj/Localizable.strings" "$tmp/ios/it.lproj/Localizable.strings"

sed -i '' 's/Plan event/Generate/' "$tmp/siri/pt.lproj/SiriIntents.strings"
expect_failure 'siri/pt:intent_title: forbidden visible term' scripts/audit-product-language.sh --fixture-root "$tmp" --forbidden-terms-only
cp "$tmp/siri/en.lproj/SiriIntents.strings" "$tmp/siri/pt.lproj/SiriIntents.strings"

cat > "$tmp/composeApp/src/androidMain/kotlin/example/Example.kt" <<'EOF'
fun Example() {
    Text(
        text = "Inbox"
    )
    WakeveActionButton(
        title = "Generate"
    )
    // Text("Scenario")
    val InboxRoute = "scenario/inbox"
}
EOF
expect_failure 'composeApp/src/androidMain/kotlin/example/Example.kt:3: production literal' scripts/audit-product-language.sh --fixture-root "$tmp" --forbidden-terms-only
expect_failure 'composeApp/src/androidMain/kotlin/example/Example.kt:6: forbidden visible term' scripts/audit-product-language.sh --fixture-root "$tmp" --forbidden-terms-only
expect_failure_without 'composeApp/src/androidMain/kotlin/example/Example.kt:3: production literal' 'composeApp/src/androidMain/kotlin/example/Example.kt:8:' scripts/audit-product-language.sh --fixture-root "$tmp" --forbidden-terms-only
expect_failure_without 'composeApp/src/androidMain/kotlin/example/Example.kt:3: production literal' 'composeApp/src/androidMain/kotlin/example/Example.kt:9:' scripts/audit-product-language.sh --fixture-root "$tmp" --forbidden-terms-only

cat > "$tmp/iosApp/src/Views/Example.swift" <<'EOF'
struct Example: View {
    var body: some View {
        Text(
            "Inbox"
        )
        WakeveActionButton(
            title: "Generate"
        )
        // Text("Scenario")
        let inboxRoute = "scenario/inbox"
    }
}
EOF
expect_failure 'iosApp/src/Views/Example.swift:4: production literal' scripts/audit-product-language.sh --fixture-root "$tmp" --forbidden-terms-only
expect_failure 'iosApp/src/Views/Example.swift:7: forbidden visible term' scripts/audit-product-language.sh --fixture-root "$tmp" --forbidden-terms-only
expect_failure_without 'iosApp/src/Views/Example.swift:4: production literal' 'iosApp/src/Views/Example.swift:9:' scripts/audit-product-language.sh --fixture-root "$tmp" --forbidden-terms-only
expect_failure_without 'iosApp/src/Views/Example.swift:4: production literal' 'iosApp/src/Views/Example.swift:10:' scripts/audit-product-language.sh --fixture-root "$tmp" --forbidden-terms-only

printf '%s\n' 'composeApp/src/androidMain/kotlin/example/Example.kt:3' > "$tmp/scripts/product-language-allowlist.txt"
expect_failure_without 'composeApp/src/androidMain/kotlin/example/Example.kt:6: production literal' 'composeApp/src/androidMain/kotlin/example/Example.kt:3:' scripts/audit-product-language.sh --fixture-root "$tmp" --forbidden-terms-only
printf '%s\n' 'composeApp/src/androidMain/kotlin/example/Example.kt:30' > "$tmp/scripts/product-language-allowlist.txt"
expect_failure 'composeApp/src/androidMain/kotlin/example/Example.kt:3: production literal' scripts/audit-product-language.sh --fixture-root "$tmp" --forbidden-terms-only

rm "$tmp/composeApp/src/androidMain/kotlin/example/Example.kt" "$tmp/iosApp/src/Views/Example.swift"
printf '# Exact identities only.\n' > "$tmp/scripts/product-language-allowlist.txt"

printf '<resources><string name="dup">One</string><string name="dup">Two</string></resources>\n' > "$tmp/android/values/strings.xml"
expect_failure 'android fr: duplicate key dup' scripts/audit-product-language.sh --fixture-root "$tmp" --catalogs-only
cp "$tmp/android/values-en/strings.xml" "$tmp/android/values/strings.xml"

printf '<resources><string name="tab_ideas">Ideas</resources>\n' > "$tmp/android/values/strings.xml"
expect_failure 'android fr: invalid XML' scripts/audit-product-language.sh --fixture-root "$tmp" --catalogs-only
cp "$tmp/android/values-en/strings.xml" "$tmp/android/values/strings.xml"

scripts/audit-product-language.sh --fixture-root "$tmp"
