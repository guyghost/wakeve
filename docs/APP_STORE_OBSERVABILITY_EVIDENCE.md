# App Store Observability Evidence - Wakeve

Date: 2026-06-01

Status: PENDING

Do not change the marker below until the uploaded TestFlight/App Review build has an observable crash, support, and backend monitoring path.

APP_STORE_OBSERVABILITY_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-06-01.

- Apple says crash reports and diagnostic logs can be gathered from the App Store, TestFlight, and devices.
- Apple says Xcode's Crashes organizer shows crash reports for apps distributed with TestFlight or through the App Store.
- Apple says TestFlight users automatically share crash logs with developers.
- Apple says App Store crash reports require users to agree to share crash and usage data with developers.
- Apple says Xcode can refresh crash reports for a selected app version and build.
- Apple says Xcode downloads top crash reports from the past two weeks, with a possible delay of up to one day after first distribution.
- Apple says selecting a crash or energy report uses the app, version number, and build string.
- Apple says fully or partially symbolicated crash reports are needed to diagnose app issues.
- Apple says unsymbolicated crash reports are rarely useful.
- Apple says third-party crash reports may omit necessary information, so Apple crash reports should be used whenever possible.
- Apple says dSYM files can be used to symbolicate crash reports, and the build UUIDs should be verified with `dwarfdump`.
- Apple says if symbols are included when uploading to App Store Connect, the service automatically symbolicates logs.
- Apple says if symbols are not included, Xcode can add symbol names when the correct dSYM files are available locally.
- Apple says developers must retain the Xcode archive for each distributed build because without the archive they may not be able to diagnose issues from crash reports.
- Apple says App Store Connect can provide dSYM files generated for a build when applicable, but dSYM downloads are no longer available for submissions from Xcode 14 or later.
- Apple says TestFlight feedback submitted by testers running TestFlight 2.3 or later appears in App Store Connect and includes screenshots, crash comments, and general comments.
- Apple says the required role to view tester feedback is Account Holder, Admin, App Manager, Developer, or Marketing.
- Apple says crash feedback can be filtered by platform, app version, build group, build, OS version, or device.
- Apple says TestFlight crash feedback downloads include the crash report and associated comments.
- Apple says TestFlight crash reports are available for download for 120 days.
- Apple says a crash report may not be included in feedback if the app becomes unresponsive because of a crash.
- Apple says uploaded build metadata can be inspected in App Store Connect, including build status and related build information.
- Apple says Complete uploaded builds can still contain warnings or other details that should be reviewed before the next upload, and Processing builds remaining over 24 hours may need a Feedback Assistant ticket or Apple contact.

## Scope

This file records the operational monitoring evidence for the first App Store review build. It complements `docs/APP_STORE_TESTFLIGHT_EVIDENCE.md` by proving that launch-blocking crashes, backend regressions, Universal Link failures, and support-contact issues can be detected before manual App Review submission.

## Required Evidence

| Area | Required Evidence | Status |
| --- | --- | --- |
| App crashes | App Store Connect/TestFlight crashes dashboard checked for the uploaded build, with symbolicated stack traces or an explicit dSYM/symbolication evidence note. | Pending |
| dSYM handling | The signed archive dSYM is retained, or the configured crash provider has received symbols for the uploaded build. | Pending |
| Xcode archive retention | The exact Xcode archive for the distributed build is retained, with owner, path, and backup location. | Pending |
| Build processing health | App Store Connect build upload status is Complete, warnings are reviewed, and any Processing-over-24h case is escalated. | Pending |
| TestFlight feedback | Screenshot/general/crash feedback are checked for the uploaded build by an authorized role, with crash report downloads retained before the 120-day window expires. | Pending |
| Backend health | Production `https://api.wakeve.app/health` monitoring is active for the 24-hour TestFlight window. | Pending |
| API errors | API 4xx/5xx rate by endpoint is reviewed during the TestFlight window. | Pending |
| Critical flows | Event creation, poll voting, calendar integration, push token registration, and Universal Links have monitored failure signals or manual log checks. | Pending |
| Support handoff | `support@wakeve.app` can receive mail, and support volume is reviewed during the TestFlight window. | Pending |
| Privacy alignment | Crash/analytics providers used by the review build match `docs/APP_STORE_PRIVACY_LABELS.md`, `docs/APP_STORE_PRIVACY_EVIDENCE.md`, and the live privacy policy. | Pending |

## Local Unsigned dSYM Scan Result

This section records local symbolication readiness for the unsigned Release build only. It does not satisfy the uploaded TestFlight/App Review build observability requirement.

Commands refreshed locally on 2026-06-01:

```bash
find build/xcode-deriveddata-release/Build/Products/Release-iphoneos -maxdepth 2 -name '*.dSYM' -print -exec du -sh {} \;
dwarfdump --uuid build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app/Wakeve build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app.dSYM build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Shared.framework/Shared build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Shared.framework.dSYM
find build -maxdepth 4 \( -name "*.xcarchive" -o -name "*.ipa" \) -print
codesign -dv --verbose=2 build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app 2>&1 || true
```

Observed local dSYM artifacts:

- `build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app.dSYM` exists and is approximately `15M`.
- `build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Shared.framework.dSYM` exists but reports `0B` in this local unsigned product scan.
- The app executable UUID is `1A770D94-184E-3238-B52E-9B272592D1AD (arm64)`.
- The `Wakeve.app.dSYM` UUID is `1A770D94-184E-3238-B52E-9B272592D1AD (arm64)`.
- The `Shared.framework/Shared` executable UUID is `4D687CF2-C7B7-3D15-8A5A-E1892EABF90A (arm64)`.
- The `Shared.framework.dSYM` UUID is `4D687CF2-C7B7-3D15-8A5A-E1892EABF90A (arm64)`.
- Repository build scan found no `.ipa` or `.xcarchive` under `build/` at max depth 4.
- `codesign -dv` result for `Wakeve.app`: `code object is not signed at all`.
- `codesign -dv` result for `Wakeve.app/Frameworks/Shared.framework`: `code object is not signed at all`.
- The 2026-06-01 refresh confirms the local unsigned app executable/dSYM UUID pair and the local unsigned Shared framework/dSYM UUID pair both match.

Unsigned limitation:

- This dSYM evidence belongs to the local `CODE_SIGNING_ALLOWED=NO` Release build, not an uploaded App Store Connect build.
- Final closure still requires the signed archive retained for the distributed build, uploaded-build dSYM or symbol-inclusion evidence, App Store Connect/TestFlight crash dashboard evidence, TestFlight feedback review, backend health/API monitoring, Universal Links/AASA monitoring, and support mailbox evidence for the same uploaded build number.

## Local Server Log Hygiene Result

This section records local release-path log hygiene only. It does not satisfy the uploaded TestFlight/App Review build observability requirement.

Commands refreshed locally on 2026-06-13:

```bash
./scripts/test-critical-release-gates.sh
```

Observed local server log hygiene:

- `OtpManager` no longer logs email addresses, generated OTP codes, verification email values, or OTP code values.
- `PushNotificationSender` no longer logs push tokens, notification title/body values, notification data payloads, or provider response bodies.
- `EventNotificationTrigger` no longer logs voter display names, user IDs, participant IDs, event IDs, comment previews, or notification send failure payload details on the checked paths.
- `NotificationScheduler` no longer logs event IDs, user IDs, participant IDs, or scheduled job keys on the checked paths.
- `scripts/test-critical-release-gates.sh` now fails if these auth, notification, or push files reintroduce direct logging of email/OTP/token/payload/user/event identifiers in checked `logger` calls.

Local limitation:

- This is a static local regression gate for high-risk server paths. Final closure still requires production backend monitoring, API error review, support handoff, crash/dSYM evidence, and uploaded-build TestFlight evidence for the same build number.

## Local Analytics And Crash Provider Privacy Alignment

This section records local privacy alignment for the current source tree only. It does not satisfy App Store Connect privacy signoff or uploaded-build observability evidence.

Commands refreshed locally on 2026-06-13:

```bash
plutil -p iosApp/src/PrivacyInfo.xcprivacy
rg -n "Firebase|Crashlytics|GoogleService|firebase|crashlytics|analytics|Analytics|ATTracking|NSUserTrackingUsageDescription|IdentifierForAdvertising|IDFA|advertisingIdentifier" iosApp shared composeApp server apps gradle build.gradle.kts settings.gradle.kts gradle/libs.versions.toml --glob '!**/build/**' --glob '!**/.gradle/**'
rg -n "Crashlytics|crashlytics|Firebase/Crash|FirebaseCrash|CrashReporting|Sentry|Bugsnag|AppCenter|Crashlytics" iosApp shared composeApp server apps gradle build.gradle.kts settings.gradle.kts gradle/libs.versions.toml --glob '!**/build/**' --glob '!**/.gradle/**'
```

Observed local result:

- `iosApp/src/PrivacyInfo.xcprivacy` declares `NSPrivacyTracking=false` and an empty `NSPrivacyTrackingDomains` array.
- The privacy manifest declares product interaction as collected for analytics and linked to identity, matching the conservative App Store privacy labels draft.
- `shared/src/iosMain/kotlin/com/guyghost/wakeve/analytics/FirebaseAnalyticsProvider.kt` is local-only for the first App Store build: it queues events locally, does not link to a Firebase iOS SDK bridge, and treats `setUserProperty`/`setUserId` as no-ops.
- Android still contains Firebase Analytics integration; this is outside the iOS App Store first-release submission but remains covered by the broader privacy labels draft.
- Local source search found no active Crashlytics, Sentry, Bugsnag, AppCenter, Firebase Crash, IDFA, `ATTrackingManager`, or `NSUserTrackingUsageDescription` integration in the checked release paths.

Local limitation:

- Final closure still requires checking the exact signed iOS archive/binary, the uploaded TestFlight/App Review build, App Store Connect privacy answers, and the live privacy policy for the same build number.

## Local Performance Capture Harness

This section records the local performance capture harness only. It does not satisfy the uploaded TestFlight/App Review build observability requirement or the roadmap device-profiling requirements.

Commands refreshed locally on 2026-06-13:

```bash
bash -n scripts/profile-release-performance.sh
./scripts/profile-release-performance.sh --runs 1
IOS_BUILD_TIMEOUT_SECONDS=1 ./scripts/profile-release-performance.sh --ios-only --build-ios --runs 1
```

Observed local result:

- `scripts/profile-release-performance.sh` writes Markdown reports to `docs/performance/`.
- `--build-ios` and `--build-android` are bounded by `IOS_BUILD_TIMEOUT_SECONDS` and `ANDROID_BUILD_TIMEOUT_SECONDS` so a failed or slow release build is recorded as `SKIPPED` with a raw log path instead of hanging the evidence run.
- `docs/performance/release-profiling-runbook.md` defines the device closure requirements for cold start, home/list scrolling, event creation, scenario matrix, and WakeveAI generation/cancellation/memory.
- `docs/performance/release-performance-2026-06-13T12-08-21Z.md` captured one local simulator iOS launch sample via `simctl launch` at `308.9 ms`.
- The same report skipped Android because no Android device or emulator was connected.
- The report states that local/simulator evidence does not close the release device-performance items.

Local limitation:

- The iOS sample is a host-observed process-launch measurement for an already installed simulator app. Final closure still requires Release build captures on representative physical devices, plus Instruments/Profiler traces for the listed flows and WakeveAI on a supported device.

## Evidence Commands

Run before final signoff:

```bash
APP_REVIEW_PHONE_NUMBER=<APP_REVIEW_PHONE_NUMBER> ./scripts/lint-store-metadata.sh --ios-only
APP_REVIEW_PHONE_NUMBER=<APP_REVIEW_PHONE_NUMBER> ./scripts/app-store-submission-audit.sh --skip-preflight
./scripts/profile-release-performance.sh --build-ios --build-android --runs 5
curl -I https://api.wakeve.app/health
curl -I https://wakeve.app/support
xcrun dwarfdump --uuid <signed-archive-or-exported-app-dsym-path>
```

Also record:

- App Store Connect/TestFlight crashes screenshot or export for the uploaded build.
- TestFlight feedback screenshot/export showing screenshot, general, and crash feedback filters for the uploaded build.
- Crash feedback download path and retention date before the 120-day availability window expires.
- Signed archive/dSYM storage location, owner, backup location, or crash-provider symbol upload output.
- Build metadata screenshot or API/export showing Complete upload status, warning review, version, build number, and build string.
- Backend health and API error dashboard screenshot or query output for the 24-hour TestFlight window.
- Universal Links/AASA monitoring result for `https://wakeve.app/.well-known/apple-app-site-association` and `https://wakeve.app/apple-app-site-association`.
- Support mailbox delivery test result for `support@wakeve.app`.
- Reviewer/date and the App Store Connect build number.

## Closure Rule

Set `APP_STORE_OBSERVABILITY_EVIDENCE_COMPLETE=true` only after:

- `docs/APP_STORE_TESTFLIGHT_EVIDENCE.md` references this evidence for the same uploaded build.
- The 24-hour TestFlight monitoring window has no launch-blocking crash or backend regression.
- Crash reports for the uploaded build are either absent or symbolicated enough to triage, and Apple crash reports are checked before relying on third-party crash tooling.
- The exact Xcode archive and dSYM/symbol evidence for the uploaded build are retained.
- TestFlight feedback, including crash feedback, is reviewed or exported before crash reports age out after 120 days.
- App Store Connect build upload status is Complete and delivery warnings/errors are reviewed.
- Support email, live legal/support URLs, backend health, and AASA monitoring are all available.
- Privacy labels and evidence accurately describe the analytics/crash providers enabled in the review build.

## Apple References

- Acquiring crash reports and diagnostic logs: https://developer.apple.com/documentation/xcode/acquiring-crash-reports-and-diagnostic-logs
- Building your app to include debugging information: https://developer.apple.com/documentation/xcode/building-your-app-to-include-debugging-information
- View tester feedback: https://developer.apple.com/help/app-store-connect/test-a-beta-version/view-tester-feedback
- TestFlight overview: https://developer.apple.com/help/app-store-connect/test-a-beta-version/testflight-overview/
- View builds and metadata: https://developer.apple.com/help/app-store-connect/manage-builds/view-builds-and-metadata/
