#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="$PROJECT_DIR/docs/performance"
RUN_DIR="${TMPDIR:-/tmp}/wakeve-performance-profile-$$"
RUNS="${RUNS:-5}"
IOS_SIMULATOR="${IOS_SIMULATOR:-iPhone 17 Pro Max}"
IOS_BUNDLE_ID="${IOS_BUNDLE_ID:-com.guyghost.wakeve}"
ANDROID_PACKAGE="${ANDROID_PACKAGE:-com.guyghost.wakeve}"
ANDROID_ACTIVITY="${ANDROID_ACTIVITY:-com.guyghost.wakeve.MainActivity}"
PROFILE_IOS=true
PROFILE_ANDROID=true
BUILD_IOS=false
BUILD_ANDROID=false

usage() {
    cat <<'USAGE'
Usage: scripts/profile-release-performance.sh [options]

Captures local release-performance evidence for Wakeve and writes a Markdown
report under docs/performance/.

Options:
  --ios-only          Capture iOS only.
  --android-only      Capture Android only.
  --build-ios         Build and install the iOS Release simulator app first.
  --build-android     Build and install the Android release APK first.
  --runs N            Launch-count per platform. Default: RUNS env or 5.
  --output DIR        Output directory. Default: docs/performance.
  -h, --help          Show this help.

Environment:
  IOS_SIMULATOR       Simulator destination name. Default: iPhone 17 Pro Max.
  IOS_BUNDLE_ID       iOS bundle ID. Default: com.guyghost.wakeve.
  ANDROID_PACKAGE     Android package. Default: com.guyghost.wakeve.
  ANDROID_ACTIVITY    Android launch activity. Default: com.guyghost.wakeve.MainActivity.

Notes:
  - iOS launch timings use `xcrun simctl launch` elapsed time. They are cold
    process-launch samples, not first-render Instruments traces.
  - Android launch timings use `adb shell am start -W`.
  - Device-grade closure still requires Instruments/Profiler captures on target
    hardware for list, create-event, scenario-matrix, and WakeveAI flows.
USAGE
}

while [ "$#" -gt 0 ]; do
    case "$1" in
        --ios-only)
            PROFILE_IOS=true
            PROFILE_ANDROID=false
            ;;
        --android-only)
            PROFILE_IOS=false
            PROFILE_ANDROID=true
            ;;
        --build-ios)
            BUILD_IOS=true
            ;;
        --build-android)
            BUILD_ANDROID=true
            ;;
        --runs)
            RUNS="${2:?Missing value for --runs}"
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

if ! [[ "$RUNS" =~ ^[0-9]+$ ]] || [ "$RUNS" -lt 1 ]; then
    echo "RUNS must be a positive integer." >&2
    exit 2
fi

mkdir -p "$OUTPUT_DIR" "$RUN_DIR"
TIMESTAMP="$(date -u +"%Y-%m-%dT%H-%M-%SZ")"
REPORT="$OUTPUT_DIR/release-performance-$TIMESTAMP.md"
LOG_DIR="$RUN_DIR/logs"
mkdir -p "$LOG_DIR"

monotonic_seconds() {
    ruby -e 'puts Process.clock_gettime(Process::CLOCK_MONOTONIC)'
}

elapsed_ms() {
    ruby -e 'start = ARGV[0].to_f; finish = ARGV[1].to_f; puts (((finish - start) * 1000.0).round(1))' "$1" "$2"
}

markdown_escape() {
    sed 's/|/\\|/g'
}

append_env() {
    {
        echo "# Wakeve Release Performance Capture"
        echo ""
        echo "Generated: $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
        echo ""
        echo "Status: LOCAL EVIDENCE"
        echo ""
        echo "This report is a reproducible local capture. It does not close the App Store or roadmap device-performance items until the same flows are profiled on representative release hardware."
        echo ""
        echo "## Configuration"
        echo ""
        echo "| Field | Value |"
        echo "| --- | --- |"
        echo "| Project | \`$PROJECT_DIR\` |"
        echo "| Runs per platform | $RUNS |"
        echo "| iOS enabled | $PROFILE_IOS |"
        echo "| Android enabled | $PROFILE_ANDROID |"
        echo "| iOS build requested | $BUILD_IOS |"
        echo "| Android build requested | $BUILD_ANDROID |"
        echo "| iOS simulator | \`$IOS_SIMULATOR\` |"
        echo "| iOS bundle ID | \`$IOS_BUNDLE_ID\` |"
        echo "| Android package | \`$ANDROID_PACKAGE\` |"
        echo "| Android activity | \`$ANDROID_ACTIVITY\` |"
        echo ""
        echo "## Toolchain"
        echo ""
        echo '```text'
        if command -v xcodebuild >/dev/null 2>&1; then
            xcodebuild -version || true
        else
            echo "xcodebuild: unavailable"
        fi
        if command -v xcrun >/dev/null 2>&1; then
            xcrun simctl list devices booted || true
        else
            echo "xcrun: unavailable"
        fi
        if command -v adb >/dev/null 2>&1; then
            adb devices || true
        else
            echo "adb: unavailable"
        fi
        echo '```'
        echo ""
    } > "$REPORT"
}

record_skip() {
    local section="$1"
    local reason="$2"
    {
        echo "## $section"
        echo ""
        echo "Status: SKIPPED"
        echo ""
        echo "$reason"
        echo ""
    } >> "$REPORT"
}

build_ios_app() {
    local derived="$RUN_DIR/ios-derived"
    local log="$LOG_DIR/xcodebuild-ios-release.log"
    xcodebuild \
        -project "$PROJECT_DIR/iosApp/iosApp.xcodeproj" \
        -scheme WakeveApp \
        -configuration Release \
        -destination "platform=iOS Simulator,name=$IOS_SIMULATOR" \
        -derivedDataPath "$derived" \
        CODE_SIGNING_ALLOWED=NO \
        build > "$log" 2>&1

    find "$derived/Build/Products/Release-iphonesimulator" -maxdepth 2 -name "Wakeve.app" -type d | head -n 1
}

profile_ios() {
    if ! command -v xcrun >/dev/null 2>&1; then
        record_skip "iOS Cold Start" "\`xcrun\` is unavailable on this machine."
        return 0
    fi

    local udid
    udid="$(xcrun simctl list devices booted | awk -F'[()]' '/Booted/ {print $2; exit}')"
    if [ -z "$udid" ]; then
        record_skip "iOS Cold Start" "No booted simulator was found. Boot \`$IOS_SIMULATOR\` or run with a booted target, then rerun this script."
        return 0
    fi

    if [ "$BUILD_IOS" = true ]; then
        local app_path
        app_path="$(build_ios_app)"
        if [ -z "$app_path" ] || [ ! -d "$app_path" ]; then
            record_skip "iOS Cold Start" "The iOS Release simulator build completed without a discoverable \`Wakeve.app\`."
            return 0
        fi
        xcrun simctl install "$udid" "$app_path" > "$LOG_DIR/simctl-install-ios.log" 2>&1
    fi

    {
        echo "## iOS Cold Start"
        echo ""
        echo "Measurement: elapsed host time for \`xcrun simctl launch --terminate-running-process $IOS_BUNDLE_ID\`."
        echo ""
        echo "| Run | Elapsed ms | Result |"
        echo "| --- | ---: | --- |"
    } >> "$REPORT"

    local run
    for run in $(seq 1 "$RUNS"); do
        xcrun simctl terminate "$udid" "$IOS_BUNDLE_ID" >/dev/null 2>&1 || true
        sleep 1

        local launch_log="$LOG_DIR/ios-launch-$run.log"
        local start finish ms result
        start="$(monotonic_seconds)"
        if xcrun simctl launch --terminate-running-process "$udid" "$IOS_BUNDLE_ID" > "$launch_log" 2>&1; then
            finish="$(monotonic_seconds)"
            ms="$(elapsed_ms "$start" "$finish")"
            result="ok"
        else
            finish="$(monotonic_seconds)"
            ms="$(elapsed_ms "$start" "$finish")"
            result="failed: $(tr '\n' ' ' < "$launch_log" | markdown_escape)"
        fi
        echo "| $run | $ms | $result |" >> "$REPORT"
    done
    echo "" >> "$REPORT"
}

build_android_app() {
    local log="$LOG_DIR/gradle-android-release.log"
    "$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" :composeApp:assembleRelease > "$log" 2>&1
    find "$PROJECT_DIR/composeApp/build/outputs/apk/release" -maxdepth 1 -name "*.apk" -type f | head -n 1
}

profile_android() {
    if ! command -v adb >/dev/null 2>&1; then
        record_skip "Android Cold Start" "\`adb\` is unavailable on this machine."
        return 0
    fi

    local device_count
    device_count="$(adb devices | awk 'NR > 1 && $2 == "device" {count++} END {print count + 0}')"
    if [ "$device_count" -eq 0 ]; then
        record_skip "Android Cold Start" "No connected Android device or emulator was found."
        return 0
    fi

    if [ "$BUILD_ANDROID" = true ]; then
        local apk_path
        apk_path="$(build_android_app)"
        if [ -z "$apk_path" ] || [ ! -f "$apk_path" ]; then
            record_skip "Android Cold Start" "The Android Release build completed without a discoverable APK."
            return 0
        fi
        adb install -r "$apk_path" > "$LOG_DIR/adb-install-android.log" 2>&1
    fi

    {
        echo "## Android Cold Start"
        echo ""
        echo "Measurement: \`adb shell am start -W $ANDROID_PACKAGE/$ANDROID_ACTIVITY\`."
        echo ""
        echo "| Run | TotalTime ms | WaitTime ms | Result |"
        echo "| --- | ---: | ---: | --- |"
    } >> "$REPORT"

    local run
    for run in $(seq 1 "$RUNS"); do
        adb shell am force-stop "$ANDROID_PACKAGE" >/dev/null 2>&1 || true
        sleep 1

        local launch_log="$LOG_DIR/android-launch-$run.log"
        local total wait result
        if adb shell am start -W "$ANDROID_PACKAGE/$ANDROID_ACTIVITY" > "$launch_log" 2>&1; then
            total="$(awk -F': ' '/TotalTime/ {print $2; exit}' "$launch_log")"
            wait="$(awk -F': ' '/WaitTime/ {print $2; exit}' "$launch_log")"
            result="ok"
        else
            total="0"
            wait="0"
            result="failed: $(tr '\n' ' ' < "$launch_log" | markdown_escape)"
        fi
        echo "| $run | ${total:-0} | ${wait:-0} | $result |" >> "$REPORT"
    done
    echo "" >> "$REPORT"
}

append_manual_checklist() {
    {
        echo "## Runtime Profiling Checklist"
        echo ""
        echo "Collect these traces before closing the roadmap performance items:"
        echo ""
        echo "- iOS Release on a supported physical device: Instruments SwiftUI + Time Profiler for home/list scrolling."
        echo "- iOS Release on a supported physical device: create-event sheet from open to saved draft."
        echo "- iOS Release on a supported physical device: scenario matrix render, vote, and final selection."
        echo "- iOS Release on a supported physical device: WakeveAI generation latency, cancellation latency, and memory peak."
        echo "- Android Release on a representative device: cold start with \`am start -W\`, home/list scrolling, create event, and scenario matrix."
        echo ""
        echo "Use focused captures: one trace per flow, record device model, OS version, build number, run count, and any caveats."
        echo ""
        echo "Raw command logs for this run were written to:"
        echo ""
        echo "- \`$LOG_DIR\`"
        echo ""
    } >> "$REPORT"
}

append_env
[ "$PROFILE_IOS" = true ] && profile_ios
[ "$PROFILE_ANDROID" = true ] && profile_android
append_manual_checklist

echo "$REPORT"
