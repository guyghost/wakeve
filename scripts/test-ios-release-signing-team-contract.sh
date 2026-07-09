#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
XCODE_PROJECT="$PROJECT_DIR/iosApp/iosApp.xcodeproj"
SCHEME="WakeveApp"

assert_effective_release_team() {
    local expected_team="$1"
    local settings
    local effective_teams

    if ! settings="$(
        TEAM_ID="$expected_team" xcodebuild \
            -project "$XCODE_PROJECT" \
            -scheme "$SCHEME" \
            -configuration Release \
            -sdk iphoneos \
            -showBuildSettings 2>&1
    )"; then
        printf '%s\n' "$settings" >&2
        echo "FAIL: Could not inspect effective WakeveApp Release build settings" >&2
        exit 1
    fi

    effective_teams="$(
        printf '%s\n' "$settings" \
            | awk -F ' = ' '/^[[:space:]]*DEVELOPMENT_TEAM = / { print $2 }' \
            | LC_ALL=C sort -u
    )"

    if [ "$effective_teams" != "$expected_team" ]; then
        echo "FAIL: WakeveApp Release must resolve DEVELOPMENT_TEAM to TEAM_ID=$expected_team" >&2
        echo "Effective DEVELOPMENT_TEAM value(s): ${effective_teams:-<missing>}" >&2
        exit 1
    fi
}

# The production contract uses the documented CI/App Store team sentinel.
assert_effective_release_team "A1B2C3D4E5"

# A second sentinel proves that no local or hardcoded team ID overrides TEAM_ID.
assert_effective_release_team "Z9Y8X7W6V5"

echo "PASS: WakeveApp Release DEVELOPMENT_TEAM follows TEAM_ID with no effective local override"
