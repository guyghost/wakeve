#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
. "$PROJECT_DIR/scripts/lib/report-sanitization.sh"
OUTPUT_DIR="${OUTPUT_DIR:-$PROJECT_DIR/docs/product}"
APP_ID="${ANDROID_APP_ID:-com.guyghost.wakeve}"
TIMESTAMP="$(date -u +"%Y-%m-%dT%H-%M-%SZ")"
REPORT="$OUTPUT_DIR/android-event-workspace-device-audit-$TIMESTAMP.md"
ASSET_DIR="$OUTPUT_DIR/android-event-workspace-device-audit-assets-$TIMESTAMP"

mkdir -p "$OUTPUT_DIR"

sanitize_report() {
    sanitize_report_file "$REPORT"
}

if command -v adb >/dev/null 2>&1; then
    adb_path="$(command -v adb)"
    adb_devices="$(adb devices -l 2>&1 || true)"
else
    adb_path="missing"
    adb_devices="adb is not installed or not on PATH."
fi

connected_android_devices="$(
    printf '%s\n' "$adb_devices" \
        | awk 'NR > 1 && $2 == "device" { print }'
)"
selected_serial="${ANDROID_SERIAL:-}"
if [ -z "$selected_serial" ] && [ -n "$connected_android_devices" ]; then
    selected_serial="$(printf '%s\n' "$connected_android_devices" | awk 'NR == 1 { print $1 }')"
fi

if [ "$adb_path" = "missing" ]; then
    status="ANDROID_PLATFORM_TOOLS_MISSING"
elif [ -z "$connected_android_devices" ]; then
    status="PENDING_ANDROID_DEVICE_OR_EMULATOR"
else
    status="READY_FOR_ANDROID_EVENT_WORKSPACE_AUDIT"
fi

adb_device_shell() {
    if [ -n "$selected_serial" ]; then
        adb -s "$selected_serial" shell "$@" 2>/dev/null || true
    fi
}

device_value() {
    local value="$1"
    local fallback="$2"

    if [ -n "$value" ]; then
        printf '%s' "$value" | tr -d '\r'
    else
        printf '%s' "$fallback"
    fi
}

device_manufacturer="$(device_value "$(adb_device_shell getprop ro.product.manufacturer)" "TODO")"
device_model="$(device_value "$(adb_device_shell getprop ro.product.model)" "TODO")"
device_api_level="$(device_value "$(adb_device_shell getprop ro.build.version.sdk)" "TODO")"
device_release="$(device_value "$(adb_device_shell getprop ro.build.version.release)" "TODO")"
device_build_id="$(device_value "$(adb_device_shell getprop ro.build.display.id)" "TODO")"
if [ -n "$selected_serial" ]; then
    device_model_display="$device_manufacturer $device_model"
    device_os_display="$device_release / API $device_api_level"
else
    device_model_display="TODO"
    device_os_display="TODO"
fi
if [ -n "$selected_serial" ]; then
    package_summary="$(adb -s "$selected_serial" shell dumpsys package "$APP_ID" 2>/dev/null \
        | awk '
            /versionName=|versionCode=|firstInstallTime=|lastUpdateTime=/ {
                gsub(/^[[:space:]]+/, "")
                print
            }
        ' \
        | head -n 8 \
        | tr '\n' ';' \
        | sed 's/;*$//' \
        || true)"
else
    package_summary=""
fi
package_state="$(device_value "$package_summary" "TODO: install Wakeve before the audit")"
capture_serial="${selected_serial:-<serial>}"

cat > "$REPORT" <<EOF
# Android Event Workspace Device Audit Preparation

Generated: $(date -u +"%Y-%m-%dT%H:%M:%SZ")

Status: \`$status\`

This report supports roadmap P2.1: Android device/emulator audit for the
creation -> invitation -> vote -> date confirmed -> day J path.

It is not completion evidence until the full flow is exercised on an Android
device or emulator and the required observations below are filled.

## Local Readiness

| Field | Value |
| --- | --- |
| adb path | \`$adb_path\` |
| Connected Android devices/emulators | \`$(if [ -n "$connected_android_devices" ]; then printf '%s' "present"; else printf '%s' "missing"; fi)\` |
| Selected adb serial | \`$(if [ -n "$selected_serial" ]; then printf '%s' "$selected_serial"; else printf '%s' "TODO"; fi)\` |
| Android device/emulator model | \`$device_model_display\` |
| Android OS/API level | \`$device_os_display\` |
| Android build id | \`$device_build_id\` |
| Wakeve package id | \`$APP_ID\` |
| Wakeve installed package summary | \`$package_state\` |
| Evidence asset directory | \`$ASSET_DIR\` |
| Focused source harness | \`./scripts/test-android-event-workspace.sh\` |
| Generated report can close roadmap item | \`no - preparation evidence only\` |

### adb devices

\`\`\`text
$adb_devices
\`\`\`

## Required Closure Evidence

Record all fields below before checking off roadmap P2.1's Android audit item:

| Field | Value |
| --- | --- |
| Android device/emulator model | TODO |
| Android OS/API level | TODO |
| Wakeve build variant | TODO |
| Wakeve commit / build number | TODO |
| Wakeve installed package summary reviewed | TODO |
| Install or launch result | TODO |
| Focused source harness result | TODO: run ./scripts/test-android-event-workspace.sh |
| Creation step result | TODO |
| Invitation/share step result | TODO |
| Vote step result | TODO |
| Date confirmed step result | TODO |
| Day J coordination step result | TODO |
| TalkBack spot-check result | TODO |
| Font scaling spot-check result | TODO |
| Navigation/back-stack result | TODO |
| Screenshots or screen recording path | TODO |
| Reviewer/date | TODO |

## Recommended Audit Flow

1. Connect an Android physical device or boot an emulator and confirm it appears as \`device\` in \`adb devices -l\`.
2. Run \`./scripts/test-android-event-workspace.sh\` and record the result.
3. Install a Debug or Release build that targets the same backend mode intended for the audit.
4. Launch Wakeve and create a new event from the Android Home/Event workspace.
5. Invite at least one participant or exercise the share/deep-link entry point with a non-personal fixture.
6. Cast a vote as a participant or through the available local test fixture.
7. Confirm a date and verify the event workspace exposes the confirmed decision clearly.
8. Navigate to the day J coordination surface and check next action, participant state, and plan-of-action copy.
9. Capture screenshots or a screen recording covering creation, invitation, vote, date confirmed, and day J.
10. Spot-check TalkBack order, 200 percent font scaling, back navigation, and dynamic color/contrast.
11. Store evidence under \`docs/product/\` or \`docs/testing/reports/\` and link it from \`ROADMAP.md\`.

## Reproducible Capture Commands

Use a fixture event and non-personal participant data. Replace each
\`<step>\` with \`creation\`, \`invitation\`, \`vote\`, \`date-confirmed\`,
\`day-j\`, \`talkback\`, \`font-200\`, or \`back-navigation\`.

\`\`\`bash
mkdir -p "$ASSET_DIR"
adb -s "$capture_serial" shell monkey -p "$APP_ID" 1
adb -s "$capture_serial" shell screencap -p "/sdcard/wakeve-<step>.png"
adb -s "$capture_serial" pull "/sdcard/wakeve-<step>.png" "$ASSET_DIR/"
adb -s "$capture_serial" shell screenrecord --time-limit 120 "/sdcard/wakeve-event-workspace-flow.mp4"
adb -s "$capture_serial" pull "/sdcard/wakeve-event-workspace-flow.mp4" "$ASSET_DIR/"
adb -s "$capture_serial" shell settings get system font_scale
adb -s "$capture_serial" shell settings put system font_scale 2.0
adb -s "$capture_serial" shell input keyevent KEYCODE_BACK
adb -s "$capture_serial" shell settings put system font_scale 1.0
\`\`\`

If the audit uses another app id or a specific device, run the helper with:

\`\`\`bash
ANDROID_SERIAL="$capture_serial" ANDROID_APP_ID="$APP_ID" ./scripts/prepare-android-event-workspace-device-audit.sh
\`\`\`

## Non-Closure Conditions

Do not mark roadmap P2.1 complete if any of these are true:

- Only source-level tests or previews were run.
- \`adb devices -l\` has no connected Android device or emulator in \`device\` state.
- The flow stops before invitation/share, vote, date confirmed, or day J coordination.
- Screenshots or screen recordings are missing for the audited flow.
- TalkBack, font scaling, and back navigation were not spot-checked.
- The report uses personal participant, address, price, vote, or chat data that cannot be safely committed.
EOF

sanitize_report

echo "$REPORT"
