# App Store UGC Moderation Evidence - Wakeve

Date: 2026-06-13

Status: PENDING

Do not change the marker below until the exact App Review build has been inspected and user-generated content filtering, reporting, blocking, moderation review, and support contact handling are verified.

APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-05-28.

- Apple App Review Guideline 1.2, last updated 2026-02-06, applies to apps with user-generated content, including random or anonymous chat.
- Apple's required UGC protections include filtering objectionable material before posting, reporting offensive content with timely responses, blocking abusive users, and published contact information.
- Apple says apps with user-generated content or social networking services must include these protections to prevent abuse.
- Apple says apps with user-generated content that become primarily pornographic, random/anonymous chat, objectification, threat, or bullying services do not belong on the App Store and may be removed without notice.
- Apple says App Review submissions should be final versions with necessary metadata and fully functional URLs, so support/contact paths used for moderation evidence must be live and non-placeholder.
- Wakeve exposes comments, chat, event text, locations, and planning details, so the App Store review build needs real filtering/reporting/blocking/contact evidence or the affected UGC surfaces must be disabled and documented for review.

## Apple References

- App Review Guidelines, Guideline 1.2 User-Generated Content: https://developer.apple.com/app-store/review/guidelines/#user-generated-content
- App Review Guidelines, Guideline 2.1 App Completeness: https://developer.apple.com/app-store/review/guidelines/#app-completeness

## Build Under Review

- App Store Connect version: TBD
- Build number: TBD
- Release commit: TBD
- TestFlight evidence reference: `docs/APP_STORE_TESTFLIGHT_EVIDENCE.md`
- OpenSpec implementation reference: `openspec/changes/add-ugc-moderation-controls/`
- App Review notes reference: `composeApp/metadata/ios/review_information/notes.txt`

## Required UGC Moderation Review

| Area | Required Evidence | Result | Notes |
| --- | --- | --- | --- |
| UGC surface inventory | Comments, chat, event text, locations, planning details, and any disabled review-build surfaces are listed. | Local verified | Comments, chat, event title/description/custom type, potential locations, meal planning text, and budget item planning text are currently in scope. |
| Filtering | Hard-policy objectionable content is rejected before persistence or regular-user visibility. | Local verified | `UgcModerationRoutesTest` covers comments, chat, event text, potential locations, and source-level route wiring for meal/budget planning text. |
| Pending review | Uncertain content can be hidden from regular users while awaiting moderation. | Local verified | Comments and chat persist as `PENDING_REVIEW` and are hidden from regular reads. |
| Reporting | Users can report comments, chat messages, events, and users with stable identifiers. | Local verified | Server report endpoints and iOS action sheets are covered by focused regression tests. |
| Blocking | Users can block abusive users and blocked-user content/notifications are suppressed for the blocker. | Local verified | Block/unblock endpoints, comment/chat read filtering, WebSocket delivery filtering, and notification suppression are covered locally. |
| Moderator audit | Report, block, and moderation decisions create audit records and enforce moderator/admin authorization. | Local verified | Moderator decision tests require moderator role and write accepted decisions. |
| iOS discoverability | Report and block actions are discoverable from the relevant iOS user/content surfaces. | Local verified | `FindingsRegressionTests.testUgcModerationReportBlockControlsAreReviewerVisible` passed on iPhone 17 simulator. |
| Support contact | `support@wakeve.app` and the support URL are reachable for abuse/contact handling. | Pending | TBD |
| Timely response process | Release owner, review cadence, escalation path, and App Review/TestFlight moderation coverage are recorded. | Pending | TBD |
| Reviewer URLs | Moderation support and contact URLs referenced by metadata or notes are live, final, and non-placeholder. | Pending | TBD |
| Disabled surfaces | Any UGC surface disabled for review is unavailable in the uploaded build and documented for App Review. | Pending | TBD |
| Reviewer path | App Review notes explain how to verify reporting, blocking, filtering, and moderation states. | Pending | TBD |

## Evidence Commands

Run or record equivalent release evidence before setting `APP_STORE_UGC_MODERATION_CONFIRMED=true`:

```bash
openspec validate add-ugc-moderation-controls --strict
rg -n "ModerationStatus|ReportTarget|ReportReason|UserBlock|ContentReport|ModerationDecision" shared/src/commonMain/kotlin server/src/main/kotlin
rg -n "moderationPolicy|ModerationService|reportContent|contentReport|blockUser|userBlock|/report|/block" server/src/main/kotlin shared/src/commonMain/kotlin
rg -n "Report Abuse|Report Content|Block User|reportUser|blockUser|ReportReason|abuse report|moderation" iosApp/src
rg -n "ModerationStatus|ReportReason|ContentReport|UserBlock|reportContent|reportOffensive|blockUser|blockedUser|moderation policy" server/src/test shared/src/commonTest
./gradlew :server:test --tests com.guyghost.wakeve.routes.UgcModerationRoutesTest
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17' -only-testing:WakeveTests/FindingsRegressionTests/testUgcModerationReportBlockControlsAreReviewerVisible
./scripts/test-app-store-ugc-gates.sh
```

Record the output or attach screenshots/logs showing:

- The filtering behavior before persistence or broadcast.
- The report flow with stable target identifiers.
- The block/unblock flow and suppressed content/notification behavior.
- Moderator/admin authorization and audit trail.
- The support contact and response process for abuse reports.
- Live moderation/support URLs and any disabled UGC surfaces in the uploaded review build.
- App Review notes that match the uploaded build.

## OpenSpec Proposal Validation Result

Command run on 2026-06-13:

```bash
openspec validate add-ugc-moderation-controls --strict
```

Local result:

- `Change 'add-ugc-moderation-controls' is valid`
- Active tasks are `21/21`; local implementation and validation tasks are complete.
- Local implementation evidence is recorded below, but no App Review readiness credit is taken until the uploaded review build, support/contact URLs, response process, and final evidence marker are complete.

## Local Implementation Evidence

Commands run on 2026-06-13:

```bash
./gradlew :server:test --tests com.guyghost.wakeve.routes.UgcModerationRoutesTest
```

Result: `BUILD SUCCESSFUL`. The focused suite covers hard-policy rejection before visible persistence, pending-review hiding for comments/chat, report and block endpoints, moderator authorization/audit decisions, blocked-user notification suppression, WebSocket block-filter wiring, and potential-location moderation before repository persistence.

```bash
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17' -only-testing:WakeveTests/FindingsRegressionTests/testUgcModerationReportBlockControlsAreReviewerVisible
```

Result: test passed. The regression covers comment, chat, event, and participant/user report/block/unblock entry points plus hidden/pending/rejected user-visible states.

```bash
./scripts/test-app-store-ugc-gates.sh
```

Result: passed. The regression proves the final audit blocks missing `APP_STORE_UGC_MODERATION_CONFIRMED`, rejects forced UGC confirmation while OpenSpec tasks or evidence remain incomplete, and recognizes the current server/iOS implementation evidence.

```bash
./scripts/lint-store-metadata.sh --ios-only
```

Result: passed on 2026-06-13 with `Passed: 3047`, `Errors: 0`, `Warnings: 2`. The lint includes UGC evidence coverage, App Store final audit guard checks, iOS metadata, privacy manifest, release binary hygiene, and local unsigned Release artifact cross-checks.

```bash
bundle exec fastlane ios preflight
```

Result: passed on 2026-06-13. The lane completed the unsigned iOS Release build, verified the Release build log has no diagnostics, ran `./scripts/lint-store-metadata.sh --ios-only`, ran `npx --yes pnpm@10 audit --audit-level low`, `check`, and `build` for `apps/landing`, and verified the local `/privacy`, `/support`, `/terms`, `/third-party-notices`, `/.well-known/apple-app-site-association`, and `/apple-app-site-association` routes.

```bash
APP_REVIEW_PHONE_NUMBER='+33123456789' ./scripts/app-store-submission-audit.sh
```

Result: executed on 2026-06-13. The local App Store preflight passed, then the final audit returned `NOT READY for App Store submission` with `Warnings: 0` and `Blockers: 21` because external release inputs remain unset, including Apple account/team environment values, product/legal/business signoffs, TestFlight smoke confirmation, live URL/AASA validation, and the signed final gate. This is expected local evidence only and does not complete App Store UGC moderation evidence.

## Closure Rule

Set `APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=true` only after:

- OpenSpec `add-ugc-moderation-controls` has no unchecked implementation tasks or has been archived after completion.
- Filtering, report, block, moderation audit, support contact, and timely response evidence is recorded for the review build.
- iOS report/block entry points are reachable from the uploaded build or UGC surfaces are disabled and documented as unavailable for the review build.
- Support/contact URLs used for moderation are live, final, and non-placeholder.
- Backend/shared tests and iOS/simulator verification evidence are recorded in this file or linked from it.
- `APP_STORE_UGC_MODERATION_CONFIRMED=true` is set only in the release shell or CI secret store after this evidence is complete.
