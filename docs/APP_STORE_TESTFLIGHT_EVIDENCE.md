# App Store TestFlight Evidence - Wakeve

Date: 2026-06-01

Status: PENDING

Do not change the marker below until the exact uploaded TestFlight build has passed the smoke checklist on a real iPhone and a supported iPad, and the monitoring window has no launch-blocking issue.

TESTFLIGHT_SMOKE_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-06-01.

- Apple says TestFlight lets developers distribute beta builds, manage beta testers, and collect feedback.
- Apple says developers should make improvements and continue distributing builds until all issues are resolved before submitting the app for review.
- Apple says TestFlight builds can be tested for up to 90 days.
- Apple says TestFlight builds must include application identifiers within the provisioning profiles.
- Apple says external testing can include up to 10,000 external testers and internal testing can include up to 100 App Store Connect users with access to the app.
- Apple says external testers may require TestFlight App Review and the first build added to a group is sent to App Review to make sure it follows the App Review Guidelines.
- Apple says external testing requires TestFlight test information, including a beta app description and feedback email.
- Apple says the required role for providing TestFlight test information is Account Holder, Admin, App Manager, Developer, or Marketing.
- Apple says builds uploaded as TestFlight Internal Only can only be added to internal tester groups and cannot be submitted for external testing or to customers.
- Apple says testers can submit feedback, and feedback from TestFlight 2.3 or later appears in App Store Connect.
- Apple says crash reports for TestFlight-distributed apps can be opened in Xcode from the Crash Feedback page in the TestFlight tab.
- Apple says tester metrics are only available after testers install the app and can take up to 24 hours to appear in App Store Connect.
- Apple says TestFlight feedback can include screenshots, crash-related comments, and general comments.
- Apple says TestFlight crash reports are available for download for 120 days, but a crash report may be missing when the app becomes unresponsive.

## Current 2026-06-01 Local Status

This 2026-06-01 refresh validates the repository-side TestFlight evidence checklist against current Apple guidance, but it does not close the TestFlight blocker.

- No uploaded TestFlight build is recorded in this repository evidence.
- No signed IPA, signed `.xcarchive`, App Store Connect build number, or TestFlight install evidence is recorded for the build intended for App Review.
- The local unsigned Release build and dSYM evidence do not replace TestFlight installation from App Store Connect on real iPhone and iPad hardware.
- `docs/APP_STORE_OBSERVABILITY_EVIDENCE.md` records matching local unsigned app/dSYM UUIDs, but the observability closure still requires signed archive retention, uploaded-build dSYM/symbol evidence, App Store Connect/TestFlight crash dashboard review, TestFlight feedback review, and backend/support monitoring for the same uploaded build.
- `docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md` records that live DNS/AASA checks currently fail because `wakeve.app` and `api.wakeve.app` do not resolve; Universal Link and backend smoke steps cannot pass until those production endpoints are live.
- AS-09 account deletion and AS-10 UGC moderation remain product blockers, so the uploaded TestFlight smoke checklist cannot be closed until those review-build behaviors are implemented or explicitly approved as not applicable.
- The validated Apple source links were refreshed on 2026-06-01 and are listed in the Apple References section.

## Build Under Test

- App Store Connect version: TBD
- Build number: TBD
- TestFlight build uploaded at: TBD
- Tester Apple IDs or internal group: TBD
- Release commit: TBD
- Signed IPA entitlement validation command: `bundle exec fastlane ios validate_ipa_entitlements ipa:build/ios/WakeveApp.ipa`
- Submission readiness command: `bundle exec fastlane ios submission_ready`
- Observability evidence reference: `docs/APP_STORE_OBSERVABILITY_EVIDENCE.md`

## Device Matrix

| Device | OS Version | Tester | Result | Evidence |
| --- | --- | --- | --- | --- |
| iPhone | TBD | TBD | Pending | App Store Connect/TestFlight screenshot or notes reference |
| iPad | TBD | TBD | Pending | App Store Connect/TestFlight screenshot or notes reference |

## Smoke Checklist Evidence

Record the result for each item from `docs/APP_STORE_LAUNCH_CHECKLIST.md` against the uploaded TestFlight build. Do not reuse Xcode-run evidence.

| Area | Required Evidence | iPhone Result | iPad Result |
| --- | --- | --- | --- |
| Install source | Install from TestFlight, not Xcode. | Pending | Pending |
| Launch state | Fresh launch shows Wakeve without debug UI, preview data, localhost behavior, or raw stack trace. | Pending | Pending |
| Guest access | Guest login works from the first screen. | Pending | Pending |
| Legal links | Privacy Policy and Terms open `https://wakeve.app/privacy` and `https://wakeve.app/terms`. | Pending | Pending |
| Offline draft | Create a draft event offline, close the app, reopen, and confirm persistence. | Pending | Pending |
| Poll flow | Add a time slot, start a poll, vote YES/MAYBE/NO, confirm a date, and navigate to scenarios. | Pending | Pending |
| Calendar | Add to calendar prompts correctly and denial does not crash. | Pending | Pending |
| Notifications | Notification permission appears only after an intentional action. | Pending | Pending |
| Links | Universal Links for `/event/`, `/poll/`, `/meeting/`, `/invite/` and `wakeve://event/<id>` open the app. | Pending | Pending |
| Account deletion | Delete Account is reachable from Profile Settings and starts the documented deletion flow. | Pending | Pending |
| UGC moderation | Comments/chat filtering, reporting, blocking, or the approved moderation fallback is verified if UGC is enabled. | Pending | Pending |
| Recovery | Airplane mode, force quit, relaunch, and session restoration behave without blocking errors. | Pending | Pending |
| Accessibility | Dark mode, larger Dynamic Type, and VoiceOver keep login, create event, poll, calendar, and settings usable. | Pending | Pending |

## Monitoring Window

Observe the uploaded TestFlight build for at least 24 hours before setting `TESTFLIGHT_SMOKE_PASSED=true`.

| Signal | Source | Result | Notes |
| --- | --- | --- | --- |
| Backend `/health` availability | Production monitoring | Pending | TBD |
| API 4xx/5xx rate by endpoint | Production monitoring | Pending | TBD |
| Authentication failure rate | Production monitoring | Pending | TBD |
| Event creation success rate | Production monitoring | Pending | TBD |
| Poll vote success rate | Production monitoring | Pending | TBD |
| Calendar integration failures | App/backend logs | Pending | TBD |
| Push token registration failures | App/backend logs | Pending | TBD |
| App crashes | TestFlight/App Store Connect crashes | Pending | TBD |
| dSYM/symbolication | `docs/APP_STORE_OBSERVABILITY_EVIDENCE.md` records dSYM retention and symbolicated crash evidence. | Pending | TBD |
| Support email volume | `support@wakeve.app` | Pending | TBD |

## Closure Rule

Set `TESTFLIGHT_SMOKE_EVIDENCE_COMPLETE=true` only after:

- Every smoke item above is pass or intentionally not applicable with an App Review-safe reason.
- The TestFlight build number matches the build intended for App Review.
- Account deletion and UGC moderation evidence reflects the final review build behavior.
- The 24-hour monitoring window has no new launch, login, event creation, poll voting, calendar, Universal Link, privacy URL, support URL, AASA, or backend availability blocker.
- `docs/APP_STORE_OBSERVABILITY_EVIDENCE.md` contains `APP_STORE_OBSERVABILITY_EVIDENCE_COMPLETE=true` for the same uploaded build.
- `TESTFLIGHT_SMOKE_PASSED=true` is set only in the final release shell or CI secret store after this file is updated.

## Apple References

- TestFlight overview: https://developer.apple.com/help/app-store-connect/test-a-beta-version/testflight-overview/
- View tester feedback: https://developer.apple.com/help/app-store-connect/test-a-beta-version/view-tester-feedback
- Acquiring crash reports and diagnostic logs: https://developer.apple.com/documentation/xcode/acquiring-crash-reports-and-diagnostic-logs
- Building your app to include debugging information: https://developer.apple.com/documentation/xcode/building-your-app-to-include-debugging-information
- View builds and metadata: https://developer.apple.com/help/app-store-connect/manage-builds/view-builds-and-metadata/
