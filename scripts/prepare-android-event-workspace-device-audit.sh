#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="${OUTPUT_DIR:-$PROJECT_DIR/docs/product}"
TIMESTAMP="$(date -u +"%Y-%m-%dT%H-%M-%SZ")"
REPORT="$OUTPUT_DIR/android-event-workspace-device-audit-$TIMESTAMP.md"

mkdir -p "$OUTPUT_DIR"

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

if [ "$adb_path" = "missing" ]; then
    status="ANDROID_PLATFORM_TOOLS_MISSING"
elif [ -z "$connected_android_devices" ]; then
    status="PENDING_ANDROID_DEVICE_OR_EMULATOR"
else
    status="READY_FOR_ANDROID_EVENT_WORKSPACE_AUDIT"
fi

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

## Non-Closure Conditions

Do not mark roadmap P2.1 complete if any of these are true:

- Only source-level tests or previews were run.
- \`adb devices -l\` has no connected Android device or emulator in \`device\` state.
- The flow stops before invitation/share, vote, date confirmed, or day J coordination.
- Screenshots or screen recordings are missing for the audited flow.
- TalkBack, font scaling, and back navigation were not spot-checked.
- The report uses personal participant, address, price, vote, or chat data that cannot be safely committed.
EOF

echo "$REPORT"
