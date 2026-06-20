#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="${OUTPUT_DIR:-$PROJECT_DIR/docs/performance}"
RUN_DIR="${TMPDIR:-/tmp}/wakeve-performance-profile-$$"
RUNS="${RUNS:-5}"
IOS_SIMULATOR="${IOS_SIMULATOR:-iPhone 17 Pro Max}"
IOS_BUNDLE_ID="${IOS_BUNDLE_ID:-com.guyghost.wakeve}"
ANDROID_PACKAGE="${ANDROID_PACKAGE:-com.guyghost.wakeve}"
ANDROID_ACTIVITY="${ANDROID_ACTIVITY:-com.guyghost.wakeve.MainActivity}"
IOS_BUILD_TIMEOUT_SECONDS="${IOS_BUILD_TIMEOUT_SECONDS:-600}"
ANDROID_BUILD_TIMEOUT_SECONDS="${ANDROID_BUILD_TIMEOUT_SECONDS:-600}"
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
  IOS_BUILD_TIMEOUT_SECONDS
                      Timeout for --build-ios. Default: 600.
  ANDROID_BUILD_TIMEOUT_SECONDS
                      Timeout for --build-android. Default: 600.

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

append_numeric_summary() {
    local title="$1"
    local metric="$2"
    local values_file="$3"

    if [ ! -s "$values_file" ]; then
        return 0
    fi

    ruby - "$title" "$metric" "$values_file" <<'RUBY' >> "$REPORT"
title = ARGV.fetch(0)
metric = ARGV.fetch(1)
path = ARGV.fetch(2)
values = File.readlines(path).map(&:strip).reject(&:empty?).map(&:to_f).sort
exit if values.empty?

def percentile(values, pct)
  index = ((pct * values.length).ceil - 1).clamp(0, values.length - 1)
  values.fetch(index)
end

average = values.sum / values.length
median = percentile(values, 0.50)
p95 = percentile(values, 0.95)

puts "### #{title} Summary"
puts
puts "| Metric | Samples | Min ms | Median ms | P95 ms | Max ms | Average ms |"
puts "| --- | ---: | ---: | ---: | ---: | ---: | ---: |"
puts "| #{metric} | #{values.length} | #{values.first.round(1)} | #{median.round(1)} | #{p95.round(1)} | #{values.last.round(1)} | #{average.round(1)} |"
puts
RUBY
}

run_with_timeout() {
    local timeout_seconds="$1"
    shift

    perl -e '
        my $timeout = shift @ARGV;
        alarm $timeout;
        exec @ARGV or die "exec failed: $!";
    ' "$timeout_seconds" "$@"
}

booted_ios_udid() {
    local preferred_name="$1"

    xcrun simctl list devices booted | awk -v preferred="$preferred_name" -F'[()]' '
        /Booted/ {
            name = $1
            sub(/^[[:space:]]+/, "", name)
            sub(/[[:space:]]+$/, "", name)
            if (name == preferred) {
                print $2
                found = 1
                exit
            }
            if (fallback == "") {
                fallback = $2
            }
        }
        END {
            if (!found && fallback != "") {
                print fallback
            }
        }
    '
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
        echo "| iOS build timeout seconds | $IOS_BUILD_TIMEOUT_SECONDS |"
        echo "| Android build timeout seconds | $ANDROID_BUILD_TIMEOUT_SECONDS |"
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

append_release_build_result() {
    local platform="$1"
    local artifact="$2"
    local log="$3"

    local artifact_size="n/a"
    if [ -n "$artifact" ] && [ -f "$artifact" ]; then
        artifact_size="$(wc -c < "$artifact" | tr -d ' ') bytes"
    elif [ -n "$artifact" ] && [ -d "$artifact" ]; then
        artifact_size="directory"
    fi

    {
        echo "## $platform Release Build"
        echo ""
        echo "Status: BUILT_LOCAL_RELEASE_ARTIFACT"
        echo ""
        echo "| Field | Value |"
        echo "| --- | --- |"
        echo "| Artifact | \`${artifact:-not found}\` |"
        echo "| Artifact size | $artifact_size |"
        echo "| Build log | \`$log\` |"
        echo ""
        echo "This proves the local Release artifact can be produced. It does not prove signed App Store/TestFlight readiness or runtime performance on a representative device."
        echo ""
    } >> "$REPORT"
}

build_ios_app() {
    local derived="$RUN_DIR/ios-derived"
    local log="$LOG_DIR/xcodebuild-ios-release.log"
    run_with_timeout "$IOS_BUILD_TIMEOUT_SECONDS" xcodebuild \
        -project "$PROJECT_DIR/iosApp/iosApp.xcodeproj" \
        -scheme WakeveApp \
        -configuration Release \
        -destination "platform=iOS Simulator,name=$IOS_SIMULATOR" \
        -derivedDataPath "$derived" \
        CODE_SIGNING_ALLOWED=NO \
        build > "$log" 2>&1 || return 1

    find "$derived/Build/Products/Release-iphonesimulator" -maxdepth 2 -name "Wakeve.app" -type d | head -n 1
}

profile_ios() {
    if ! command -v xcrun >/dev/null 2>&1; then
        record_skip "iOS Cold Start" "\`xcrun\` is unavailable on this machine."
        return 0
    fi

    local udid
    udid="$(booted_ios_udid "$IOS_SIMULATOR")"
    if [ -z "$udid" ]; then
        record_skip "iOS Cold Start" "No booted simulator was found. Boot \`$IOS_SIMULATOR\` or run with a booted target, then rerun this script."
        return 0
    fi

    if [ "$BUILD_IOS" = true ]; then
        local app_path
        if ! app_path="$(build_ios_app)"; then
            record_skip "iOS Cold Start" "The iOS Release simulator build failed or exceeded ${IOS_BUILD_TIMEOUT_SECONDS}s. Inspect \`$LOG_DIR/xcodebuild-ios-release.log\`."
            return 0
        fi
        if [ -z "$app_path" ] || [ ! -d "$app_path" ]; then
            record_skip "iOS Cold Start" "The iOS Release simulator build completed without a discoverable \`Wakeve.app\`."
            return 0
        fi
        append_release_build_result "iOS" "$app_path" "$LOG_DIR/xcodebuild-ios-release.log"
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
    local values_file="$RUN_DIR/ios-cold-start-ms.txt"
    : > "$values_file"
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
            echo "$ms" >> "$values_file"
        else
            finish="$(monotonic_seconds)"
            ms="$(elapsed_ms "$start" "$finish")"
            result="failed: $(tr '\n' ' ' < "$launch_log" | markdown_escape)"
        fi
        echo "| $run | $ms | $result |" >> "$REPORT"
    done
    echo "" >> "$REPORT"
    append_numeric_summary "iOS Cold Start" "simctl launch elapsed" "$values_file"
}

build_android_app() {
    local log="$LOG_DIR/gradle-android-release.log"
    run_with_timeout "$ANDROID_BUILD_TIMEOUT_SECONDS" "$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" --no-configuration-cache :composeApp:assembleRelease > "$log" 2>&1 || return 1
    find "$PROJECT_DIR/composeApp/build/outputs/apk/release" -maxdepth 1 -name "*.apk" -type f | head -n 1
}

profile_android() {
    local apk_path=""
    if [ "$BUILD_ANDROID" = true ]; then
        if ! apk_path="$(build_android_app)"; then
            record_skip "Android Release Build" "The Android Release build failed or exceeded ${ANDROID_BUILD_TIMEOUT_SECONDS}s. Inspect \`$LOG_DIR/gradle-android-release.log\`."
            return 0
        fi
        if [ -z "$apk_path" ] || [ ! -f "$apk_path" ]; then
            record_skip "Android Release Build" "The Android Release build completed without a discoverable APK."
            return 0
        fi
        append_release_build_result "Android" "$apk_path" "$LOG_DIR/gradle-android-release.log"
    fi

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
    local total_values_file="$RUN_DIR/android-cold-start-total-ms.txt"
    local wait_values_file="$RUN_DIR/android-cold-start-wait-ms.txt"
    : > "$total_values_file"
    : > "$wait_values_file"
    for run in $(seq 1 "$RUNS"); do
        adb shell am force-stop "$ANDROID_PACKAGE" >/dev/null 2>&1 || true
        sleep 1

        local launch_log="$LOG_DIR/android-launch-$run.log"
        local total wait result
        if adb shell am start -W "$ANDROID_PACKAGE/$ANDROID_ACTIVITY" > "$launch_log" 2>&1; then
            total="$(awk -F': ' '/TotalTime/ {print $2; exit}' "$launch_log")"
            wait="$(awk -F': ' '/WaitTime/ {print $2; exit}' "$launch_log")"
            result="ok"
            [ -n "${total:-}" ] && echo "$total" >> "$total_values_file"
            [ -n "${wait:-}" ] && echo "$wait" >> "$wait_values_file"
        else
            total="0"
            wait="0"
            result="failed: $(tr '\n' ' ' < "$launch_log" | markdown_escape)"
        fi
        echo "| $run | ${total:-0} | ${wait:-0} | $result |" >> "$REPORT"
    done
    echo "" >> "$REPORT"
    append_numeric_summary "Android Cold Start" "am start TotalTime" "$total_values_file"
    append_numeric_summary "Android Cold Start" "am start WaitTime" "$wait_values_file"
}

append_runtime_flow_matrix() {
    {
        echo "## Runtime Profiling Matrix"
        echo ""
        echo "These rows define the release-device traces required before the roadmap performance items can be checked off. Local simulator or emulator samples may support regression tracking, but the status remains \`PENDING_DEVICE_TRACE\` until a representative signed build is profiled."
        echo ""
        echo "| Flow | Platform | Required capture | Required measurements | Status |"
        echo "| --- | --- | --- | --- | --- |"
        echo "| Cold start | iOS | Physical device, signed Release/TestFlight build, 5+ launches | Process launch, first meaningful screen, memory after idle | PENDING_DEVICE_TRACE |"
        echo "| Cold start | Android | Representative device or emulator, release APK, 5+ launches | \`am start -W\` TotalTime/WaitTime, first meaningful screen, memory after idle | PENDING_DEVICE_TRACE |"
        echo "| Home/list scrolling | iOS | Instruments SwiftUI + Time Profiler trace | Frame hitches, CPU hot spots, memory growth | PENDING_DEVICE_TRACE |"
        echo "| Home/list scrolling | Android | Android Studio Profiler or Perfetto trace | Jank, CPU hot spots, memory growth | PENDING_DEVICE_TRACE |"
        echo "| Create event | iOS | Instruments trace from opening sheet through saved draft | Sheet open latency, validation/apply latency, frame hitches | PENDING_DEVICE_TRACE |"
        echo "| Create event | Android | Profiler/Perfetto trace for matching flow | Screen open latency, validation/apply latency, jank | PENDING_DEVICE_TRACE |"
        echo "| Scenario matrix | iOS | Instruments trace for render, vote, final selection | Render latency, vote latency, CPU/memory under loaded matrix | PENDING_DEVICE_TRACE |"
        echo "| Scenario matrix | Android | Profiler/Perfetto trace for render, vote, final selection | Render latency, vote latency, CPU/memory under loaded matrix | PENDING_DEVICE_TRACE |"
        echo "| WakeveAI generation | iOS supported device | Instruments + WakeveAI metrics snapshot | Availability, generation latency, cancellation latency, timeout behavior, memory peak | PENDING_DEVICE_TRACE |"
        echo ""
        echo "## Runtime Profiling Checklist"
        echo ""
        echo "For every captured flow, record:"
        echo ""
        echo "- Device model, OS version, app version/build, distribution channel, run count, and dataset size."
        echo "- Raw trace artifact path or App Store/TestFlight attachment reference."
        echo "- Measured latency, CPU, memory, and any skipped subflow with the reason."
        echo "- Whether the capture used a signed App Store/TestFlight build or a local Release build."
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
append_runtime_flow_matrix

echo "$REPORT"
