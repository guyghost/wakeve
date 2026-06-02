# App Store Launch Checklist - Wakeve

Date: 2026-05-27

Use this checklist after the local App Store preflight is green and before submitting the first App Store review build. It complements `APP_STORE_READINESS.md` and `APP_STORE_SUBMISSION_RUNBOOK.md`.

## Apple Source Baseline

Apple-source review date: 2026-05-28.

- Apple says TestFlight builds can be tested for up to 90 days.
- Apple says TestFlight-eligible builds must include application identifiers within provisioning profiles.
- Apple says internal testers can include up to 100 App Store Connect users with access to the app.
- Apple says external testing can include up to 10,000 people and may require App Review.
- Apple says the first build added to an external tester group is sent to App Review to check App Review Guidelines compliance.
- Apple says testers install the TestFlight app, accept invitations, install the app, send feedback, and get updates.
- Apple says build status and metrics include sessions and crashes.
- Apple says tester feedback can include screenshots, crash-related comments, and general comments in App Store Connect.
- Apple says crash reports from TestFlight feedback are available for download for 120 days.
- Apple says a crash report may be missing if the app becomes unresponsive due to a crash.
- Apple says a build can be expired to stop testing, and expired builds no longer allow internal or external testers to install it.
- Apple says TestFlight builds submitted for beta distribution should be intended for public distribution and comply with App Review Guidelines.
- Apple says submitted apps should be final versions with all necessary metadata and fully functional URLs.
- Apple says apps should be tested on device for crashes, bugs, and stability before App Review.
- Apple says App Review contact information should be current, full review access should be provided, and backend services should be live during review.

## Entry Criteria

Do not start TestFlight/App Review operations until these are true:

- `bundle exec fastlane ios preflight` passes locally.
- `bundle exec fastlane ios preflight strict:true live_urls:true` passes with the real `APPLE_TEAM_ID`.
- `bundle exec fastlane ios submission_ready` passes with real Apple credentials and signing.
- `bundle exec fastlane ios validate_ipa_entitlements ipa:build/ios/WakeveApp.ipa` passes for the signed IPA if it is inspected outside the build lane.
- `APP_REVIEW_PHONE_NUMBER` is set in the release environment, or `composeApp/metadata/ios/review_information/phone_number.txt` contains the real App Review contact phone as a fallback.
- `https://wakeve.app/privacy`, `/terms`, `/support`, `/third-party-notices`, `/.well-known/apple-app-site-association`, and `/apple-app-site-association` are live.
- `https://api.wakeve.app/health` is live.
- `docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md` contains `APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=true` for the production legal/support/AASA/backend URL checks.
- App Store Connect privacy labels match `docs/APP_STORE_PRIVACY_LABELS.md` and the live privacy policy.
- App Store Connect availability matches `docs/APP_STORE_AVAILABILITY_DECISIONS.md`.
- App Store Connect EU DSA trader status or EU availability decision matches `docs/APP_STORE_DSA_TRADER_STATUS.md`.
- Account deletion readiness matches `docs/APP_STORE_REVIEW_GUIDELINE_AUDIT.md` and the implementation plan in `openspec/changes/add-in-app-account-deletion/` if account creation remains enabled in the review build; `docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md` contains `APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=true`.
- User-generated content moderation readiness matches `docs/APP_STORE_REVIEW_GUIDELINE_AUDIT.md` and the implementation plan in `openspec/changes/add-ugc-moderation-controls/` if comments/chat/event text are enabled in the review build; `docs/APP_STORE_UGC_MODERATION_EVIDENCE.md` contains `APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=true`.
- Accessibility Nutrition Labels are either left unpublished or backed by the device evidence in `docs/APP_STORE_ACCESSIBILITY_LABELS.md`.
- `docs/APP_STORE_FINAL_SIGNOFF.md` remains `APP_STORE_FINAL_SIGNOFF_COMPLETE=false` until every final evidence item is complete.

## TestFlight Internal Smoke Test

Run this on at least one recent iPhone and one supported iPad before App Review:

- Install from TestFlight, not Xcode.
- Fresh launch shows `Wakeve` without debug UI, preview data, or localhost behavior.
- Guest login works from the first screen.
- Privacy Policy and Terms buttons open `https://wakeve.app/privacy` and `https://wakeve.app/terms`.
- Create a draft event offline, close the app, reopen, and confirm the draft is still present.
- Add at least one time slot and start a poll.
- Vote YES/MAYBE/NO on poll slots.
- Confirm a date and verify the app navigates to scenarios.
- Add or view a scenario, then move to meeting organization where available.
- Add to calendar flow displays the expected iOS permission prompt and does not crash if permission is denied.
- Notification permission flow displays a native prompt only when triggered by an intentional action.
- Universal Link smoke test opens the app for `https://wakeve.app/event/<id>`, `poll/<id>`, `meeting/<id>`, and `invite/<id>` after the app is installed.
- `wakeve://event/<id>` custom-scheme link opens the app.
- Verify the Delete Account action is findable from Profile Settings, starts the documented deletion flow, and is recorded in `docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md`.
- If comments/chat are enabled, verify filtering/report/block or the chosen moderation fallback before App Review and record the result in `docs/APP_STORE_UGC_MODERATION_EVIDENCE.md`.
- Airplane mode shows a clear offline state and no raw stack trace.
- Force quit and relaunch restores auth/session state correctly.
- Dark mode remains legible.
- Dynamic Type at larger sizes does not block primary actions.
- VoiceOver can reach login, create event, poll, calendar, and settings controls.

Record the executed TestFlight build, device matrix, smoke results, and 24-hour monitoring evidence in `docs/APP_STORE_TESTFLIGHT_EVIDENCE.md` and `docs/APP_STORE_OBSERVABILITY_EVIDENCE.md`. Do not set `TESTFLIGHT_SMOKE_PASSED=true` until those files contain `TESTFLIGHT_SMOKE_EVIDENCE_COMPLETE=true` and `APP_STORE_OBSERVABILITY_EVIDENCE_COMPLETE=true` for the uploaded review build.

## Monitoring During TestFlight

For the first TestFlight build, watch these for at least 24 hours before App Review:

- Backend `/health` availability.
- API 4xx/5xx rate by endpoint.
- Authentication failure rate.
- Event creation success rate.
- Poll vote success rate.
- Calendar integration failures.
- Push token registration failures.
- App crashes from TestFlight/App Store Connect, with dSYM retention or symbolicated crash evidence.
- User support email volume at `support@wakeve.app`.

Suggested hold conditions:

- Any new crash in launch, login, event creation, poll voting, or calendar paths.
- API 5xx rate above the normal baseline.
- Universal Links fail on a clean install.
- Privacy/support URLs or AASA become unreachable.
- App Review contact information is incomplete.

## App Review Submission

Submit only after TestFlight smoke testing is complete:

```bash
APP_STORE_PRIVACY_SIGNOFF=true \
APP_STORE_ACCESSIBILITY_SIGNOFF=true \
APP_STORE_AVAILABILITY_CONFIRMED=true \
APP_STORE_DSA_TRADER_STATUS_CONFIRMED=true \
APP_STORE_PRICING_AVAILABILITY_CONFIRMED=true \
APP_STORE_SDK_PRIVACY_CONFIRMED=true \
APP_STORE_RELEASE_CONTROL_CONFIRMED=true \
APP_STORE_MEDIA_LOCALIZATION_CONFIRMED=true \
APP_STORE_LICENSE_NOTICES_CONFIRMED=true \
APP_STORE_EULA_CONFIRMED=true \
APP_STORE_ACCOUNT_DELETION_CONFIRMED=true \
APP_STORE_UGC_MODERATION_CONFIRMED=true \
APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED=true \
TESTFLIGHT_SMOKE_PASSED=true \
APP_STORE_CAPABILITIES_CONFIRMED=true \
APPLE_ID="$APPLE_ID" \
ITC_TEAM_ID="$ITC_TEAM_ID" \
TEAM_ID="$TEAM_ID" \
APPLE_TEAM_ID="$APPLE_TEAM_ID" \
APP_REVIEW_PHONE_NUMBER="$APP_REVIEW_PHONE_NUMBER" \
bundle exec fastlane ios upload_appstore
```

Set the signoff variables and `APP_STORE_FINAL_SIGNOFF_COMPLETE=true` only after privacy/legal approval, accessibility/availability decisions, EU DSA trader status handling, pricing and storefront availability evidence, third-party SDK privacy/signature evidence, App Store release control evidence, App Store media/localization evidence, license notices evidence, EULA evidence, account deletion readiness, user-generated content moderation readiness, payment/external purchase compliance, TestFlight smoke testing, and Apple capability/profile verification are complete. Fastlane uploads the IPA, metadata, screenshots, and age-rating config, but keeps `submit_for_review: false`. Submit manually from App Store Connect after checking the uploaded version.

Before manual submission:

- Confirm `docs/APP_STORE_ACCOUNT_ACCESS_EVIDENCE.md` contains `APP_STORE_ACCOUNT_ACCESS_EVIDENCE_COMPLETE=true` for the Apple account, App Store Connect role, app record ownership, `ITC_TEAM_ID`, 2FA, and agreement status.
- Confirm screenshots are attached for `en-US` and `fr-FR`.
- Confirm `docs/APP_STORE_APP_INFORMATION_EVIDENCE.md` contains `APP_STORE_APP_INFORMATION_EVIDENCE_COMPLETE=true` for Bundle ID, SKU, primary language, category, and generated age rating.
- Confirm `docs/APP_STORE_VERSIONING_EVIDENCE.md` contains `APP_STORE_VERSIONING_EVIDENCE_COMPLETE=true` for the uploaded review build and records the latest App Store Connect build-number comparison.
- Confirm `docs/APP_STORE_RELEASE_ARTIFACT_EVIDENCE.md` contains `APP_STORE_RELEASE_ARTIFACT_EVIDENCE_COMPLETE=true` for the uploaded review build and records the signed IPA SHA-256, Xcode archive, dSYM UUIDs, release commit, Fastlane logs, and App Store Connect build reference.
- Confirm `docs/APP_STORE_CONTENT_RIGHTS_EVIDENCE.md` contains `APP_STORE_CONTENT_RIGHTS_EVIDENCE_COMPLETE=true` for the uploaded review build and records app name, icon, screenshot/preview, metadata, provider-reference, and open-source notice rights checks.
- Confirm `docs/APP_STORE_PRIVACY_EVIDENCE.md` contains `APP_STORE_PRIVACY_EVIDENCE_COMPLETE=true` for the same uploaded build before setting `APP_STORE_PRIVACY_SIGNOFF=true`.
- Confirm `docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md` contains `APP_STORE_ACCESSIBILITY_EVIDENCE_COMPLETE=true` for the same uploaded build before setting `APP_STORE_ACCESSIBILITY_SIGNOFF=true`.
- Confirm `docs/APP_STORE_AVAILABILITY_EVIDENCE.md` contains `APP_STORE_AVAILABILITY_EVIDENCE_COMPLETE=true` for the same uploaded build before setting `APP_STORE_AVAILABILITY_CONFIRMED=true`.
- Confirm `docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md` contains `APP_STORE_PRICING_AVAILABILITY_EVIDENCE_COMPLETE=true` for the same uploaded build before setting `APP_STORE_PRICING_AVAILABILITY_CONFIRMED=true`.
- Confirm `docs/APP_STORE_SDK_PRIVACY_EVIDENCE.md` contains `APP_STORE_SDK_PRIVACY_EVIDENCE_COMPLETE=true` for the same uploaded build before setting `APP_STORE_SDK_PRIVACY_CONFIRMED=true`.
- Confirm `docs/APP_STORE_RELEASE_CONTROL_EVIDENCE.md` contains `APP_STORE_RELEASE_CONTROL_EVIDENCE_COMPLETE=true` for the same uploaded build before setting `APP_STORE_RELEASE_CONTROL_CONFIRMED=true`.
- Confirm `docs/APP_STORE_MEDIA_LOCALIZATION_EVIDENCE.md` contains `APP_STORE_MEDIA_LOCALIZATION_EVIDENCE_COMPLETE=true` for the same uploaded build before setting `APP_STORE_MEDIA_LOCALIZATION_CONFIRMED=true`.
- Confirm `docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md` contains `APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=true` for the same uploaded build before setting `APP_STORE_LICENSE_NOTICES_CONFIRMED=true`.
- Confirm `docs/APP_STORE_LICENSE_INVENTORY_DRAFT.md` was regenerated with `./scripts/app-store-license-inventory.sh --fetch-remote-metadata --output docs/APP_STORE_LICENSE_INVENTORY_DRAFT.md` for the release commit, and every submitted-iOS unknown or copyleft-risk license in that draft has been resolved before App Review. Any remaining unknown or copyleft-risk dependency outside submitted iOS scope must be recorded as not shipped, legally approved, or separately resolved.
- Confirm `docs/APP_STORE_THIRD_PARTY_NOTICES.md` and `https://wakeve.app/third-party-notices` were regenerated from the same inventory and are referenced from App Review notes before setting `APP_STORE_LICENSE_NOTICES_CONFIRMED=true`.
- Confirm `docs/APP_STORE_EULA_EVIDENCE.md` contains `APP_STORE_EULA_EVIDENCE_COMPLETE=true` for the same uploaded build before setting `APP_STORE_EULA_CONFIRMED=true`.
- Confirm privacy/support URLs are correct in each locale.
- Confirm App Review notes match the uploaded build's release-visible guest/reviewer flow, or that App Store Connect demo credentials are configured manually without committing passwords to the repository.
- Confirm `docs/APP_STORE_REVIEW_ACCESS_EVIDENCE.md` contains `APP_STORE_REVIEW_ACCESS_EVIDENCE_COMPLETE=true` for the uploaded review build.
- Confirm contact name, email, and phone are present.
- Confirm the account deletion flow is reachable from the uploaded build.
- Confirm comments/chat moderation evidence is complete if user-generated content is enabled in the review build.
- Confirm `docs/APP_STORE_PAYMENT_EVIDENCE.md` contains `APP_STORE_PAYMENT_EVIDENCE_COMPLETE=true` for the same uploaded build before setting `APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED=true`.
- Confirm `docs/APP_STORE_EXPORT_COMPLIANCE_EVIDENCE.md` contains `APP_STORE_EXPORT_COMPLIANCE_EVIDENCE_COMPLETE=true` for the uploaded review build, and export compliance says the app uses only standard encryption or no non-exempt encryption, matching `ITSAppUsesNonExemptEncryption=false`.
- Confirm `docs/APP_STORE_DSA_TRADER_STATUS.md` contains `APP_STORE_DSA_TRADER_STATUS_EVIDENCE_COMPLETE=true` before setting `APP_STORE_DSA_TRADER_STATUS_CONFIRMED=true`; EU DSA trader status must be provided/verified, or EU storefront availability must be intentionally disabled for the first release.
- Confirm Push Notifications, Siri, Sign in with Apple, and Associated Domains capabilities are visible on the uploaded build.
- Confirm `docs/APP_STORE_CAPABILITIES_EVIDENCE.md` contains `APP_STORE_CAPABILITIES_EVIDENCE_COMPLETE=true` for the same uploaded build before setting `APP_STORE_CAPABILITIES_CONFIRMED=true`.
- Confirm `docs/APP_STORE_TESTFLIGHT_EVIDENCE.md` contains `TESTFLIGHT_SMOKE_EVIDENCE_COMPLETE=true` for the same uploaded build.
- Confirm `docs/APP_STORE_OBSERVABILITY_EVIDENCE.md` contains `APP_STORE_OBSERVABILITY_EVIDENCE_COMPLETE=true` for crash, dSYM/symbolication, backend monitoring, Universal Links/AASA monitoring, and support mailbox evidence.
- Confirm `docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md` contains `APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=true` for the production legal/support/AASA/backend URLs before manual App Review submission.

## Rollback Plan

If a TestFlight build is bad:

- Stop external distribution for the build in App Store Connect.
- Remove the build from any testing group if needed.
- Fix the issue, increment `CFBundleVersion`, rebuild, and upload a new TestFlight build.
- Keep the previous known-good build available for internal testers until the replacement is validated.

If an App Store version is approved but should not ship:

- Keep manual release selected and do not release it.
- Reject the release in App Store Connect if a replacement build is required.
- Upload a corrected build with an incremented build number.

If a released version has a production incident:

- Disable affected backend behavior server-side where possible.
- Keep `wakeve.app` legal/support/AASA endpoints online.
- Submit an expedited fix only after local `preflight`, `submission_ready`, and TestFlight smoke tests pass.

## Exit Criteria

The project is ready to submit to App Review when:

- All entry criteria are green.
- TestFlight smoke test has no blocking issue.
- Monitoring has no new launch-blocking crash or backend regression.
- App Store Connect metadata, privacy labels, screenshots, review information, capabilities, and export compliance are confirmed manually.
- The release owner has accepted the privacy/legal answers and rollback plan.
