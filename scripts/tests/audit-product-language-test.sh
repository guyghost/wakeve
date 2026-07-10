#!/usr/bin/env bash
set -euo pipefail

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

for locale in en fr de es it pt; do
    dir="values-$locale"
    [[ "$locale" == en ]] && dir="values"
    mkdir -p "$tmp/android/$dir"
    printf '<resources><string name="tab_ideas">Ideas</string></resources>' > "$tmp/android/$dir/strings.xml"
done
mkdir -p "$tmp/composeApp/src/androidMain/kotlin/example"

expect_failure() {
    local expected="$1"
    shift
    local output
    if output="$("$@" 2>&1)"; then
        echo "expected failure containing: $expected" >&2
        exit 1
    fi
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
