# App Store Release Control Evidence - Wakeve

Date: 2026-06-01

Status: PENDING

Do not change the marker below until the exact App Store Connect version under review has a documented release option, release owner, monitoring window, and post-approval action plan.

APP_STORE_RELEASE_CONTROL_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-06-01.

- Apple App Store Connect release options include manual release, automatic release after App Review approval, and automatic release no earlier than a specified date.
- Apple says if a version is released as a pre-order, the release option must be manual release.
- Apple says the required role to choose an App Store version release option is Account Holder, Admin, or App Manager.
- When manual release is selected, an approved version moves to Pending Developer Release and the release owner must explicitly choose Release This Version in App Store Connect.
- Apple says releasing an app version is platform-specific, so versions for other platforms in Pending Developer Release must be released separately.
- Apple says it sends an email reminder if an app remains in Pending Developer Release for more than 30 days.
- Apple says a manually released app version may take up to 24 hours to appear on the App Store, and a release request sent through the App Store Connect API cannot be cancelled once sent.
- Apple says a manual release can be cancelled from the version page before the release completes.
- Phased Release for Automatic Updates is an update-only rollout option that releases over a 7-day period, can be paused for up to 30 total days, and can be accelerated with Release to All Users.
- Apple says phased release targets a random sample of users with automatic updates on eligible devices and does not notify those users of their participation.
- Apple says phased release percentages are 1%, 2%, 5%, 10%, 20%, 50%, and 100% over days 1 through 7.
- Apple says anyone can manually download a version update from the App Store during phased release.
- Apple says removing the app from sale stops phased release and makes phased release unavailable for that version again.
- Apple says if a Ready for Distribution version has a legal or usability issue, an update must be submitted; if an update cannot be submitted, the app must be removed from the App Store.
- Apple says it is not possible to revert to a previous version on the App Store when an issue exists.

## Current 2026-06-01 Local Status

Repository-side controls still support a conservative first-release path: upload the signed IPA and metadata to App Store Connect, do not submit automatically for App Review, and do not request automatic public release from Fastlane. This protects the final manual App Store Connect review step, but it does not prove the App Store Connect release option, owner assignment, or first-day monitoring plan.

- Recommended first-release setting: manual release in App Store Connect, with no phased release because phased release applies to eligible version updates rather than the initial public version.
- App Store Connect submission action: manual, after the uploaded build, screenshots, metadata, age rating, App Review information, privacy labels, DSA, pricing/availability, and final evidence matrix have been checked.
- Public release action: manual from Pending Developer Release after App Review approval, using a named release owner and backup owner.
- Fastlane upload guard remains local-only evidence: `submit_for_review: false` and `automatic_release: false` prevent the lane from auto-submitting or auto-releasing, but App Store Connect screenshots/exports are still required.
- Current local hashes: `fastlane/Fastfile` `8b999cea44fdab22a4d7e6b3bbc644249d71adb056752655fc1da1472a793a30`; `.env.appstore.example` `fcfca68b5212bab68d1db2e87608957b1774ed265e37c8721d3122f72326af32`; `scripts/app-store-submission-audit.sh` `f7a60958b100158fe689132edbdad3e5ddff60c778150955997aaa2902051cd9`.
- `APP_STORE_RELEASE_CONTROL_EVIDENCE_COMPLETE=false` remains intentional until App Store Connect release-option evidence, release owner approval, release window, stop/pause criteria, and monitoring coverage are recorded.

## Release Decision

| Area | Required Evidence | Result |
| --- | --- | --- |
| Release option | App Store Connect App Store Version Release is set to manual release for the first public release, or an approved automatic/scheduled release rationale is recorded. | Pending |
| Pre-order interaction | If pre-order is enabled in Pricing and Availability, release option is manual release and the release owner records when pre-order countries become downloadable. | Pending |
| Phased release | Phased Release for Automatic Updates is not applicable to the first public version, or is explicitly configured for an eligible update with the 7-day rollout decision recorded. | Pending |
| Release owner | A named release owner and backup owner are assigned for App Review approval, Pending Developer Release, release, and first-day monitoring. | Pending |
| Release timing | Target release date/time, timezone, freeze window, and support coverage are recorded. | Pending |
| Post-approval action | Manual release steps from Pending Developer Release are recorded, including who clicks Release This Version and when. | Pending |
| Stop/pause criteria | Criteria for delaying release, cancelling release before propagation, pausing phased release when applicable, or shipping an expedited fix are recorded. | Pending |
| Store propagation | The 24-hour App Store propagation window and support response plan are acknowledged. | Pending |
| Rollback path | Emergency response chooses between delaying release, cancelling a pending manual release, pausing phased release, submitting a fix update, or removing the app from sale when no update can be submitted. | Pending |

## Apple References

- App Store Connect lets each platform version choose manual release, automatic release after approval, or automatic release no earlier than a specified date: https://developer.apple.com/help/app-store-connect/manage-your-apps-availability/select-an-app-store-version-release-option/
- For manual release, approved versions move to Pending Developer Release and must be released explicitly in App Store Connect: https://developer.apple.com/help/app-store-connect/manage-your-apps-availability/select-an-app-store-version-release-option/
- Apple notes that a manually released version can take up to 24 hours to appear on the App Store and that release requests sent through the API cannot be cancelled once sent: https://developer.apple.com/documentation/appstoreconnectapi/post-v1-appstoreversionreleaserequests
- Phased release for eligible version updates rolls out over 7 days and can be paused for up to 30 total days: https://developer.apple.com/help/app-store-connect/update-your-app/release-a-version-update-in-phases
- Apple documents removing a version from sale when a legal or usability issue cannot be fixed by update: https://developer.apple.com/help/app-store-connect/manage-your-apps-availability/make-a-version-unavailable-for-download
- Apple documents that reverting to a previous version is not possible if an issue exists: https://developer.apple.com/help/app-store-connect/update-your-app/create-a-new-version/

## Evidence To Attach

Record these before final signoff:

- App Store Connect version, build number, platform, and screenshot/export showing the selected App Store Version Release option.
- If the release uses pre-order, screenshot/export proving manual release is selected and listing pre-order countries or regions.
- App Store Connect role proof for the release owner and backup owner: Account Holder, Admin, or App Manager.
- Confirmation that Fastlane upload used `submit_for_review: false` and `automatic_release: false`.
- Manual submitter role and release owner with Account Holder, Admin, or App Manager permission.
- Chosen release path: manual release, automatic after approval, scheduled release, or phased release for an eligible update.
- If manual release: owner, backup, approval monitoring, Pending Developer Release check cadence, exact release window, 30-day reminder handling, and Cancel This Release decision point before propagation completes.
- If scheduled release: target date/time/timezone, support readiness, and pre-release cancellation criteria.
- If phased release: phased release setting, day-by-day monitoring owner, automatic-update eligibility note, 1/2/5/10/20/50/100 percent exposure plan, pause/resume criteria, 30-day total pause budget, Release to All Users criteria, and remove-from-sale consequence.
- First 24-hour monitoring plan covering crash reports, support mailbox, backend health, Universal Links/AASA, and payment/account/UGC surfaces if enabled.
- Emergency rollback plan acknowledging that App Store rollback to a previous version is unavailable; use a fix update, phased-release pause, pending-release cancellation, or removal from sale if required.

## Local Upload Path Scan Result

Result: the repository upload path is configured to keep App Review submission and public release manual, but this is pre-submission evidence only because the App Store Connect release option and release owners have not been captured.

- Local scan date: 2026-06-01.
- Fastlane `upload_appstore` requires `APPLE_ID`, `ITC_TEAM_ID`, `TEAM_ID`, `APPLE_TEAM_ID`, a real App Review phone, every final signoff environment variable, and `docs/APP_STORE_FINAL_SIGNOFF.md` before upload.
- Fastlane `upload_appstore` runs `run_final_app_store_submission_audit`, then `preflight(strict: true, live_urls: true)`, then `build` before `deliver`.
- Fastlane `deliver` is configured with `submit_for_review: false`, so the lane uploads metadata/build artifacts without automatic App Review submission.
- Fastlane `deliver` is configured with `automatic_release: false`, so the lane does not request automatic public release.
- The current local `fastlane/Fastfile` SHA-256 is `8b999cea44fdab22a4d7e6b3bbc644249d71adb056752655fc1da1472a793a30`.
- The current local `.env.appstore.example` SHA-256 is `fcfca68b5212bab68d1db2e87608957b1774ed265e37c8721d3122f72326af32`.
- The current local `scripts/app-store-submission-audit.sh` SHA-256 is `f7a60958b100158fe689132edbdad3e5ddff60c778150955997aaa2902051cd9`.
- `docs/APP_STORE_LAUNCH_CHECKLIST.md` instructs the release operator to submit manually from App Store Connect after checking the uploaded version.
- `docs/APP_STORE_SUBMISSION_RUNBOOK.md` records that submission mode remains manual and that release-control evidence must be recorded before public release.
- `.env.appstore.example` defaults `APP_STORE_RELEASE_CONTROL_CONFIRMED=false`, and the final audit requires `APP_STORE_RELEASE_CONTROL_CONFIRMED=true` plus `APP_STORE_RELEASE_CONTROL_EVIDENCE_COMPLETE=true` before the final App Store gate can report ready.

This local scan does not close AS-19. The final reviewer must still attach App Store Connect evidence for the selected App Store Version Release option, the release owner and backup owner, release timing, Pending Developer Release monitoring, stop/pause criteria, and first-day monitoring coverage before setting `APP_STORE_RELEASE_CONTROL_EVIDENCE_COMPLETE=true`.

## Closure Rule

Set `APP_STORE_RELEASE_CONTROL_EVIDENCE_COMPLETE=true` only after:

- The App Store Connect release option for the submitted version is captured.
- The launch owner has confirmed whether the release is manual, automatic, scheduled, or phased.
- Manual release is selected for the initial public release unless an explicit owner-approved exception is recorded.
- Manual release is selected if any pre-order is configured.
- Post-approval release action, per-platform release steps, stop/pause criteria, emergency update/removal path, and monitoring coverage are documented.
- `APP_STORE_RELEASE_CONTROL_CONFIRMED=true` is set only in the final release shell or CI secret store after this file is updated.
- `docs/APP_STORE_FINAL_SIGNOFF.md` references this completed evidence for the submitted review build.
