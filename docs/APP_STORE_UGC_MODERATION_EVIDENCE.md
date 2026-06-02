# App Store UGC Moderation Evidence - Wakeve

Date: 2026-05-27

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
| UGC surface inventory | Comments, chat, event text, locations, planning details, and any disabled review-build surfaces are listed. | Pending | TBD |
| Filtering | Hard-policy objectionable content is rejected before persistence or regular-user visibility. | Pending | TBD |
| Pending review | Uncertain content can be hidden from regular users while awaiting moderation. | Pending | TBD |
| Reporting | Users can report comments, chat messages, events, and users with stable identifiers. | Pending | TBD |
| Blocking | Users can block abusive users and blocked-user content/notifications are suppressed for the blocker. | Pending | TBD |
| Moderator audit | Report, block, and moderation decisions create audit records and enforce moderator/admin authorization. | Pending | TBD |
| iOS discoverability | Report and block actions are discoverable from the relevant iOS user/content surfaces. | Pending | TBD |
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

Command run on 2026-05-27:

```bash
openspec validate add-ugc-moderation-controls --strict
```

Local result:

- `Change 'add-ugc-moderation-controls' is valid`
- Active tasks remain `0/19`, so this is proposal-readiness evidence only.
- No implementation or App Review readiness credit is taken from this validation until the proposal is approved, implemented, tested, and evidenced against the uploaded review build.

## Closure Rule

Set `APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=true` only after:

- OpenSpec `add-ugc-moderation-controls` has no unchecked implementation tasks or has been archived after completion.
- Filtering, report, block, moderation audit, support contact, and timely response evidence is recorded for the review build.
- iOS report/block entry points are reachable from the uploaded build or UGC surfaces are disabled and documented as unavailable for the review build.
- Support/contact URLs used for moderation are live, final, and non-placeholder.
- Backend/shared tests and iOS/simulator verification evidence are recorded in this file or linked from it.
- `APP_STORE_UGC_MODERATION_CONFIRMED=true` is set only in the release shell or CI secret store after this evidence is complete.
