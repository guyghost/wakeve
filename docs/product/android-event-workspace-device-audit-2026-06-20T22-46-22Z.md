# Android Event Workspace Device Audit Preparation

Generated: 2026-06-20T22:46:22Z

Status: `PENDING_ANDROID_DEVICE_OR_EMULATOR`

This report supports roadmap P2.1: Android device/emulator audit for the
creation -> invitation -> vote -> date confirmed -> day J path.

It is not completion evidence until the full flow is exercised on an Android
device or emulator and the required observations below are filled.

## Local Readiness

| Field | Value |
| --- | --- |
| adb path | `~/Library/Android/sdk/platform-tools/adb` |
| Connected Android devices/emulators | `missing` |
| Selected adb serial | `TODO` |
| Android device/emulator model | `TODO` |
| Android OS/API level | `TODO` |
| Android build id | `TODO` |
| Wakeve package id | `com.guyghost.wakeve` |
| Wakeve installed package summary | `TODO: install Wakeve before the audit` |
| Evidence asset directory | `~/Developer/dev/wakeve/docs/product/android-event-workspace-device-audit-assets-2026-06-20T22-46-22Z` |
| Focused source harness | `./scripts/test-android-event-workspace.sh` |
| Generated report can close roadmap item | `no - preparation evidence only` |

### adb devices

```text
List of devices attached
```

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

1. Connect an Android physical device or boot an emulator and confirm it appears as `device` in `adb devices -l`.
2. Run `./scripts/test-android-event-workspace.sh` and record the result.
3. Install a Debug or Release build that targets the same backend mode intended for the audit.
4. Launch Wakeve and create a new event from the Android Home/Event workspace.
5. Invite at least one participant or exercise the share/deep-link entry point with a non-personal fixture.
6. Cast a vote as a participant or through the available local test fixture.
7. Confirm a date and verify the event workspace exposes the confirmed decision clearly.
8. Navigate to the day J coordination surface and check next action, participant state, and plan-of-action copy.
9. Capture screenshots or a screen recording covering creation, invitation, vote, date confirmed, and day J.
10. Spot-check TalkBack order, 200 percent font scaling, back navigation, and dynamic color/contrast.
11. Store evidence under `docs/product/` or `docs/testing/reports/` and link it from `ROADMAP.md`.

## Reproducible Capture Commands

Use a fixture event and non-personal participant data. Replace each
`<step>` with `creation`, `invitation`, `vote`, `date-confirmed`,
`day-j`, `talkback`, `font-200`, or `back-navigation`.

```bash
mkdir -p "~/Developer/dev/wakeve/docs/product/android-event-workspace-device-audit-assets-2026-06-20T22-46-22Z"
adb -s "<serial>" shell monkey -p "com.guyghost.wakeve" 1
adb -s "<serial>" shell screencap -p "/sdcard/wakeve-<step>.png"
adb -s "<serial>" pull "/sdcard/wakeve-<step>.png" "~/Developer/dev/wakeve/docs/product/android-event-workspace-device-audit-assets-2026-06-20T22-46-22Z/"
adb -s "<serial>" shell screenrecord --time-limit 120 "/sdcard/wakeve-event-workspace-flow.mp4"
adb -s "<serial>" pull "/sdcard/wakeve-event-workspace-flow.mp4" "~/Developer/dev/wakeve/docs/product/android-event-workspace-device-audit-assets-2026-06-20T22-46-22Z/"
adb -s "<serial>" shell settings get system font_scale
adb -s "<serial>" shell settings put system font_scale 2.0
adb -s "<serial>" shell input keyevent KEYCODE_BACK
adb -s "<serial>" shell settings put system font_scale 1.0
```

If the audit uses another app id or a specific device, run the helper with:

```bash
ANDROID_SERIAL="<serial>" ANDROID_APP_ID="com.guyghost.wakeve" ./scripts/prepare-android-event-workspace-device-audit.sh
```

## Non-Closure Conditions

Do not mark roadmap P2.1 complete if any of these are true:

- Only source-level tests or previews were run.
- `adb devices -l` has no connected Android device or emulator in `device` state.
- The flow stops before invitation/share, vote, date confirmed, or day J coordination.
- Screenshots or screen recordings are missing for the audited flow.
- TalkBack, font scaling, and back navigation were not spot-checked.
- The report uses personal participant, address, price, vote, or chat data that cannot be safely committed.
