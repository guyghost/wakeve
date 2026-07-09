#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
PLATFORM_SERVICE="$PROJECT_DIR/shared/src/iosMain/kotlin/com/guyghost/wakeve/calendar/PlatformCalendarService.ios.kt"
SOURCE_PLIST="$PROJECT_DIR/iosApp/src/Info.plist"
MODEL="$PROJECT_DIR/models/calendar-permission/calendarPermission.machine.ts"
REVIEW="$PROJECT_DIR/models/calendar-permission/review.md"
SETTINGS_FILE="$(mktemp)"
FAILURES=0

cleanup() {
    rm -f "$SETTINGS_FILE"
}
trap cleanup EXIT

record_failure() {
    echo "FAIL: $*" >&2
    FAILURES=$((FAILURES + 1))
}

for artifact in "$MODEL" "$REVIEW"; do
    if [ -f "$artifact" ]; then
        echo "PASS: ${artifact#$PROJECT_DIR/} exists"
    else
        record_failure "Missing calendar permission model artifact: ${artifact#$PROJECT_DIR/}"
    fi
done

BUILD_SETTINGS_READY=false
if ! command -v xcodebuild >/dev/null 2>&1; then
    record_failure "xcodebuild is required to resolve effective WakeveApp settings"
elif xcodebuild \
    -project "$PROJECT_DIR/iosApp/iosApp.xcodeproj" \
    -scheme WakeveApp \
    -configuration Release \
    -destination "generic/platform=iOS" \
    -showBuildSettings \
    -json >"$SETTINGS_FILE" 2>/dev/null; then
    BUILD_SETTINGS_READY=true
else
    record_failure "Could not resolve effective WakeveApp Release build settings"
fi

build_setting() {
    ruby -rjson -e '
        data = JSON.parse(File.read(ARGV.fetch(0)))
        target = data.find { |entry| entry["target"] == "WakeveApp" }
        exit 1 unless target
        print target.fetch("buildSettings")[ARGV.fetch(1)].to_s
    ' "$SETTINGS_FILE" "$1"
}

if [ "$BUILD_SETTINGS_READY" = true ]; then
    deployment_target="$(build_setting IPHONEOS_DEPLOYMENT_TARGET)"
    deployment_major="${deployment_target%%.*}"
    if [[ "$deployment_major" =~ ^[0-9]+$ ]] && [ "$deployment_major" -ge 17 ]; then
        echo "PASS: WakeveApp Release deployment target $deployment_target activates the iOS 17+ EventKit contract"
    else
        record_failure "WakeveApp Release deployment target must be iOS 17+ (found: $deployment_target)"
    fi
fi

if grep -Fq 'requestFullAccessToEventsWithCompletion' "$PLATFORM_SERVICE"; then
    echo "PASS: PlatformCalendarService uses the iOS 17+ full-access EventKit request"
else
    record_failure "PlatformCalendarService must call requestFullAccessToEventsWithCompletion for the iOS 17+ target"
fi

if grep -Fq 'requestAccessToEntityType' "$PLATFORM_SERVICE"; then
    record_failure "PlatformCalendarService still calls deprecated requestAccessToEntityType for the iOS 17+ target"
else
    echo "PASS: PlatformCalendarService does not call deprecated requestAccessToEntityType"
fi

FULL_ACCESS_KEY="NSCalendarsFullAccessUsageDescription"
if plutil -extract "$FULL_ACCESS_KEY" raw -o - "$SOURCE_PLIST" >/dev/null 2>&1; then
    echo "PASS: Effective $FULL_ACCESS_KEY comes from the source Info.plist"
elif [ "$BUILD_SETTINGS_READY" = true ] && [ -n "$(build_setting "INFOPLIST_KEY_$FULL_ACCESS_KEY")" ]; then
    echo "PASS: Effective $FULL_ACCESS_KEY comes from WakeveApp Release build settings"
else
    record_failure "WakeveApp must configure a non-empty effective $FULL_ACCESS_KEY"
fi

if [ "$FAILURES" -gt 0 ]; then
    echo "FAIL: iOS calendar permission contract has $FAILURES violation(s)" >&2
    exit 1
fi

echo "PASS: iOS calendar permission contract uses full EventKit access safely"
