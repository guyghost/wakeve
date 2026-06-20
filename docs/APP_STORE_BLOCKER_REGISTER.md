# App Store Blocker Register - Wakeve

Date: 2026-06-21

Status: NOT READY

This register tracks the current blockers that prevent Wakeve from being submitted to App Review. It complements `docs/APP_STORE_READINESS.md`, `docs/APP_STORE_SUBMISSION_RUNBOOK.md`, `docs/APP_STORE_FINAL_SIGNOFF.md`, and `docs/APP_STORE_PRODUCT_BLOCKER_APPROVAL.md`.

## Apple Source Baseline

Apple-source review date: 2026-05-28.

- Apple says the app review process covers each submitted app version and related submitted items.
- Apple says app versions for each platform are submitted separately, and the status of any app version on one platform does not influence another platform.
- Apple says each platform can typically have one app version submission under review at a time, with a maximum of two submissions under review when one submission contains the app version and another contains items without the app version.
- Apple says items associated with different platforms cannot be added to the same submission.
- Apple says before submitting an app version for review, the required metadata must be provided and the correct uploaded build must be selected for the version.
- Apple says the required role to submit an app for review is Account Holder, Admin, or App Manager.
- Apple says clicking Add for Review moves an app to Ready for Review, but the submission is not sent until Submit for Review is clicked.
- Apple says an app version can be added to an existing draft submission or to a new draft submission.
- Apple says all items submitted together must be accepted to complete the submission.
- Apple says submissions to App Review should be final versions with all necessary metadata and fully functional URLs.
- Apple says placeholder text, empty websites, and other temporary content should be removed before submission.
- Apple says apps should be tested on device for bugs and stability before submission.
- Apple says apps that need account access must include demo account information, or an approved built-in demo mode, plus any other information needed to review the app.
- Apple says apps with user-generated content must include filtering, reporting with timely responses, blocking for abusive users, and published contact information.
- Apple says apps that support account creation must let users initiate account deletion from within the app.
- Apple says App Privacy answers are required for App Store distribution and must accurately describe app and third-party partner data practices.
- Apple says in-app purchase is required for digital content, features, subscriptions, consumables, or other digital goods unlocked in the app unless an exception applies.
- Apple says purchase methods other than in-app purchase can be used for physical goods or services consumed outside the app.
- Apple says app availability must be configured before App Review submission.
- Apple says Digital Services Act trader status information is required for EU distribution workflows where applicable.
- Apple says apps uploaded to App Store Connect must meet the current SDK requirements; starting April 2026, iOS and iPadOS apps must be built with the iOS and iPadOS 26 SDK or later.
- Apple says App Store Connect app statuses include Prepare for Submission, Ready for Review, Waiting for Review, In Review, Unresolved Issues, Developer Rejected, and Waiting for Export Compliance, so this register treats missing export, review, or build evidence as blockers until closed by current evidence.
- Apple says Ready for Review means required metadata is entered and the app is intended for review, but it has not yet been submitted.
- Apple says Waiting for Review means Apple received the submission but has not started review; some information can still be edited, app previews can be deleted, and items can be removed, but screenshots and app previews cannot be uploaded or edited.
- Apple says In Review means App Review is reviewing the submission and the submission can be cancelled to remove it from review.
- Apple says if any item in a submission is rejected, the submission status changes to Unresolved Issues and App Review needs all items approved before the submission is considered approved.
- Apple says a submission with Unresolved Issues cannot have more items added; rejected items must be edited and resubmitted or removed.
- Apple says removing an app version or cancelling a submission changes an app-version submission to Developer Rejected and the review process starts over if resubmitted.
- Apple says submissions may not be reviewed in the order submitted, so the release owner must not rely on review timing to resolve missing evidence.

Current audit baseline:

```bash
APP_REVIEW_PHONE_NUMBER='+33123456789' ./scripts/app-store-submission-audit.sh --skip-preflight
```

Current result on 2026-06-21: 21 blockers, 1 warning. The phone number above is a non-placeholder test value used only to verify blocker accounting without committing personal information; the documented placeholder `+15551234567` is now rejected by the final audit. The warning is expected for this quick baseline because `--skip-preflight` intentionally omits the local Fastlane preflight.

Full local preflight baseline:

```bash
APP_REVIEW_PHONE_NUMBER='+33123456789' ./scripts/app-store-submission-audit.sh
```

Last full local preflight result on 2026-06-13: 21 blockers, 0 warnings. This confirmed the local Fastlane App Store preflight passes; the remaining blockers are Apple/App Store Connect/deployment/signoff gates.

Live deployment baseline:

```bash
APP_REVIEW_PHONE_NUMBER='+33123456789' APPLE_TEAM_ID='A1B2C3D4E5' ./scripts/lint-store-metadata.sh --ios-only --check-live-urls
```

Current result on 2026-06-21 local time: 17 live URL/AASA errors and 1 final-signoff warning remain expected for this blocker state. The API health URL is still reachable, but `wakeve.app` still has no public DNS answer from the local network, so legal/support/terms/third-party-notices, dashboard `/app`, legacy redirects, and both AASA URLs remain unreachable. `docs/app-store-live-url-aasa/live-url-aasa-2026-06-20T22-48-00Z.md` records the matching command output, including `api.wakeve.app` resolving to Cloudflare and responding to `curl -I https://api.wakeve.app/health` with HTTP `405`; a separate `GET /health` check returned HTTP `200 OK`. These errors keep AS-14 open until production `wakeve.app` DNS, legal/support pages, AASA files, and the real Apple Team ID are live; `api.wakeve.app/health` must stay review-accessible through the final live smoke.

## Blockers

| ID | Area | Current Status | Required Evidence | Gate |
| --- | --- | --- | --- | --- |
| AS-01 | Apple account and App Store Connect access | Missing external value and role/agreement evidence. | `APPLE_ID` is set to the App Store Connect Apple account email and passes format validation; `docs/APP_STORE_ACCOUNT_ACCESS_EVIDENCE.md` contains `APP_STORE_ACCOUNT_ACCESS_EVIDENCE_COMPLETE=true` for role, 2FA, app record, team, and agreement status; `docs/APP_STORE_REVIEW_ACCESS_EVIDENCE.md` contains `APP_STORE_REVIEW_ACCESS_EVIDENCE_COMPLETE=true` proving that the uploaded review build has either a release-visible guest/reviewer path matching the notes or manually configured App Store Connect demo credentials. | `./scripts/app-store-submission-audit.sh` |
| AS-02 | App Store Connect team | Missing external value. | `ITC_TEAM_ID` is set to the numeric App Store Connect team ID. | `./scripts/app-store-submission-audit.sh` |
| AS-03 | Apple signing team | Missing external value. | `TEAM_ID` is set to the 10-character Apple Developer Team ID. | `./scripts/app-store-submission-audit.sh` |
| AS-04 | AASA Apple Team ID | Missing external value. | `APPLE_TEAM_ID` is set, matches `TEAM_ID`, and is deployed to both AASA endpoints. | `./scripts/app-store-submission-audit.sh --check-live-urls` |
| AS-05 | Privacy labels/legal approval | Manual signoff incomplete. | App Store Connect privacy labels match `docs/APP_STORE_PRIVACY_LABELS.md`, the live privacy policy, and `iosApp/src/PrivacyInfo.xcprivacy`; `docs/APP_STORE_PRIVACY_EVIDENCE.md` contains `APP_STORE_PRIVACY_EVIDENCE_COMPLETE=true`; `APP_STORE_PRIVACY_SIGNOFF=true`. | `docs/APP_STORE_FINAL_SIGNOFF.md` |
| AS-06 | Accessibility label decision | Manual signoff incomplete. | Accessibility Nutrition Labels are left unpublished or backed by device evidence in `docs/APP_STORE_ACCESSIBILITY_LABELS.md`; `docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md` contains `APP_STORE_ACCESSIBILITY_EVIDENCE_COMPLETE=true`; `APP_STORE_ACCESSIBILITY_SIGNOFF=true`. | `docs/APP_STORE_FINAL_SIGNOFF.md` |
| AS-07 | Mac/Vision Pro availability | Repository Release settings now disable Mac Apple silicon and Apple Vision Pro compatibility for first release; manual App Store Connect confirmation remains incomplete. | App Store Connect availability matches `docs/APP_STORE_AVAILABILITY_DECISIONS.md`; `docs/APP_STORE_AVAILABILITY_EVIDENCE.md` contains `APP_STORE_AVAILABILITY_EVIDENCE_COMPLETE=true`; `APP_STORE_AVAILABILITY_CONFIRMED=true`. | `docs/APP_STORE_FINAL_SIGNOFF.md` |
| AS-08 | EU DSA trader status | Apple-source baseline refreshed on 2026-06-01; manual App Store Connect trader/non-trader/EU storefront decision remains incomplete. | Trader status is verified for EU distribution, non-trader status is declared with owner approval, or EU storefront availability is intentionally disabled as documented; `docs/APP_STORE_DSA_TRADER_STATUS.md` contains `APP_STORE_DSA_TRADER_STATUS_EVIDENCE_COMPLETE=true`; `APP_STORE_DSA_TRADER_STATUS_CONFIRMED=true`. | `docs/APP_STORE_DSA_TRADER_STATUS.md` |
| AS-09 | Account deletion | Local implementation and focused tests are now present; uploaded-build App Review evidence and final signoff remain incomplete. | OpenSpec `openspec/changes/add-in-app-account-deletion/` is completed or explicitly accepted with remaining release-only tasks; iOS Profile exposes Delete Account; backend deletion route, local cleanup, Sign in with Apple revocation evidence, and tests are recorded in `docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md`; `APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=true`; `APP_STORE_ACCOUNT_DELETION_CONFIRMED=true`. | `docs/APP_STORE_REVIEW_GUIDELINE_AUDIT.md`, `docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md`, `docs/APP_STORE_PRODUCT_BLOCKER_APPROVAL.md` |
| AS-10 | UGC moderation | Local implementation, focused tests, iOS discoverability checks, gates, and local final validation are now present; uploaded-build App Review evidence, live support/contact verification, and final signoff remain incomplete. | OpenSpec `openspec/changes/add-ugc-moderation-controls/` is approved using `docs/APP_STORE_PRODUCT_BLOCKER_APPROVAL.md` and implemented; filtering, reporting, blocking, moderation audit, support contact, and reviewer-visible evidence are recorded in `docs/APP_STORE_UGC_MODERATION_EVIDENCE.md`; `APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=true`; `APP_STORE_UGC_MODERATION_CONFIRMED=true`. | `docs/APP_STORE_REVIEW_GUIDELINE_AUDIT.md`, `docs/APP_STORE_UGC_MODERATION_EVIDENCE.md`, `docs/APP_STORE_PRODUCT_BLOCKER_APPROVAL.md` |
| AS-11 | Payment/external purchase compliance | Manual/product evidence incomplete. | `docs/APP_STORE_PAYMENT_COMPLIANCE.md` is verified against the review build; App Review notes explain real-world shared-event expenses and no digital unlocks; `docs/APP_STORE_PAYMENT_EVIDENCE.md` contains `APP_STORE_PAYMENT_EVIDENCE_COMPLETE=true`; `APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED=true`. | `docs/APP_STORE_PAYMENT_COMPLIANCE.md` |
| AS-12 | TestFlight smoke test | Apple-source baseline and repository-side evidence checklist refreshed on 2026-06-01; no uploaded TestFlight build, signed IPA/archive, real-device TestFlight install, 24-hour monitoring window, or uploaded-build crash/feedback evidence is recorded. | TestFlight smoke checklist passes on iPhone and iPad, including account deletion and UGC/payment surfaces if enabled; crash/dSYM/backend/support monitoring is recorded; `docs/APP_STORE_TESTFLIGHT_EVIDENCE.md` contains `TESTFLIGHT_SMOKE_EVIDENCE_COMPLETE=true`; `docs/APP_STORE_OBSERVABILITY_EVIDENCE.md` contains `APP_STORE_OBSERVABILITY_EVIDENCE_COMPLETE=true`; `TESTFLIGHT_SMOKE_PASSED=true`. | `docs/APP_STORE_LAUNCH_CHECKLIST.md` |
| AS-13 | Apple Developer capabilities/profiles | External Apple Developer verification incomplete. | Signed IPA entitlements include Push Notifications, Siri, Sign in with Apple, Associated Domains, and production APNs; `docs/APP_STORE_CAPABILITIES_EVIDENCE.md` contains `APP_STORE_CAPABILITIES_EVIDENCE_COMPLETE=true`; `APP_STORE_CAPABILITIES_CONFIRMED=true`. | `bundle exec fastlane ios validate_ipa_entitlements ipa:build/ios/WakeveApp.ipa` |
| AS-14 | Live URL/AASA validation | Public checks were rerun on 2026-06-21 local time: `api.wakeve.app/health` is reachable, but `wakeve.app` DNS/live web/AASA routes remain unreachable and the real Apple Team ID is not deployed. | `https://wakeve.app/privacy`, `/terms`, `/support`, `/third-party-notices`, both AASA paths, and `https://api.wakeve.app/health` are reachable and validated with real `APPLE_TEAM_ID`; `docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md` contains `APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=true` with deployment, DNS, command-output, cache, rollout, and rollback evidence. | `./scripts/app-store-submission-audit.sh --check-live-urls` |
| AS-15 | Signed final submission gate | Not run with Apple signing. | `bundle exec fastlane ios submission_ready` passes with real Apple credentials and signing, `docs/APP_STORE_APP_INFORMATION_EVIDENCE.md` contains `APP_STORE_APP_INFORMATION_EVIDENCE_COMPLETE=true`, `docs/APP_STORE_VERSIONING_EVIDENCE.md` contains `APP_STORE_VERSIONING_EVIDENCE_COMPLETE=true`, `docs/APP_STORE_RELEASE_ARTIFACT_EVIDENCE.md` contains `APP_STORE_RELEASE_ARTIFACT_EVIDENCE_COMPLETE=true`, `docs/APP_STORE_CONTENT_RIGHTS_EVIDENCE.md` contains `APP_STORE_CONTENT_RIGHTS_EVIDENCE_COMPLETE=true`, `docs/APP_STORE_EXPORT_COMPLIANCE_EVIDENCE.md` contains `APP_STORE_EXPORT_COMPLIANCE_EVIDENCE_COMPLETE=true`, and the final audit is run with `--run-submission-ready`. | `./scripts/app-store-submission-audit.sh --run-submission-ready` |
| AS-16 | App Review contact phone | Missing external value unless supplied through release secrets. | `APP_REVIEW_PHONE_NUMBER` is set in the release shell/CI secret store with a plausible phone number, or `composeApp/metadata/ios/review_information/phone_number.txt` is intentionally populated as a fallback without committing private reviewer-only credentials. | `./scripts/app-store-submission-audit.sh` and `bundle exec fastlane ios preflight strict:true` |
| AS-17 | Pricing and storefront availability | Manual App Store Connect decision incomplete. | App Store Connect Pricing and Availability choices for price, storefronts, pre-order, education/business distribution, custom app distribution, tax category, and Paid Apps Agreement compatibility are recorded in `docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md`; `APP_STORE_PRICING_AVAILABILITY_EVIDENCE_COMPLETE=true`; `APP_STORE_PRICING_AVAILABILITY_CONFIRMED=true`. | `docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md` |
| AS-18 | Third-party SDK privacy manifests/signatures | Signed archive SDK privacy evidence incomplete. | The signed review archive has an SDK inventory, required listed-SDK privacy manifests, SDK signatures for listed binary dependencies, and an Xcode privacy report recorded in `docs/APP_STORE_SDK_PRIVACY_EVIDENCE.md`; `APP_STORE_SDK_PRIVACY_EVIDENCE_COMPLETE=true`; `APP_STORE_SDK_PRIVACY_CONFIRMED=true`. | `docs/APP_STORE_SDK_PRIVACY_EVIDENCE.md` |
| AS-19 | App Store release control | Local Fastlane/audit guards were refreshed on 2026-06-01 and keep upload non-submitting/non-auto-release; manual App Store Connect release option, owner, window, stop/pause criteria, and monitoring evidence remain incomplete. | App Store Connect release option, phased-release decision, release owner, release window, stop/pause criteria, and first-day monitoring plan are recorded in `docs/APP_STORE_RELEASE_CONTROL_EVIDENCE.md`; `APP_STORE_RELEASE_CONTROL_EVIDENCE_COMPLETE=true`; `APP_STORE_RELEASE_CONTROL_CONFIRMED=true`. | `docs/APP_STORE_RELEASE_CONTROL_EVIDENCE.md` |
| AS-20 | App Store media and localization | Screenshot, app preview, and localized metadata review incomplete. | iPhone/iPad screenshots, app preview decision, EN/FR localized metadata, metadata limits, product-page consistency, and App Store Connect media page evidence are recorded in `docs/APP_STORE_MEDIA_LOCALIZATION_EVIDENCE.md`; `APP_STORE_MEDIA_LOCALIZATION_EVIDENCE_COMPLETE=true`; `APP_STORE_MEDIA_LOCALIZATION_CONFIRMED=true`. | `docs/APP_STORE_MEDIA_LOCALIZATION_EVIDENCE.md` |
| AS-21 | License notices and attributions | Third-party dependency license inventory and notice delivery evidence incomplete. | Shipped iOS/KMP/SPM/CocoaPods/Gradle/npm dependencies are inventoried with App Store scope, submitted-iOS unknown/copyleft risks are resolved, attribution obligations are reviewed, required notices are bundled or published at `https://wakeve.app/third-party-notices`, non-iOS scoped exceptions are approved or recorded as not shipped, and `docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md` contains `APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=true`; `APP_STORE_LICENSE_NOTICES_CONFIRMED=true`. | `docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md` |
| AS-22 | EULA and terms alignment | App Store Connect License Agreement decision incomplete. | App Store Connect License Agreement is recorded as Apple standard EULA or custom EULA, Wakeve Terms of Service and `/terms` page are aligned with the choice, legal owner approval is attached, and `docs/APP_STORE_EULA_EVIDENCE.md` contains `APP_STORE_EULA_EVIDENCE_COMPLETE=true`; `APP_STORE_EULA_CONFIRMED=true`. | `docs/APP_STORE_EULA_EVIDENCE.md` |

## Warning

| ID | Area | Current Status | Resolution |
| --- | --- | --- | --- |
| AW-01 | Local preflight | Skipped in the current blocker-count command. | Run the audit without `--skip-preflight` for a real release check. |

## Closure Rules

- Do not set final signoff variables to `true` from this document alone. Each row requires the named evidence source to be current.
- Do not mark a row closed unless the final App Store Connect state is recorded with platform scope, draft submission membership, submitted items accepted or intentionally excluded, no Unresolved Issues state, and any removal, cancellation, or retry decision.
- Product blockers AS-09 and AS-10 required OpenSpec approval before implementation, with approval decisions recorded in `docs/APP_STORE_PRODUCT_BLOCKER_APPROVAL.md`; both are now locally implemented, but release confirmation remains blocked until uploaded-build evidence is complete.
- External blockers AS-01 through AS-04, AS-07, AS-08, AS-12, AS-13, AS-14, AS-15, AS-16, AS-17, AS-18, AS-19, AS-20, AS-21, and AS-22 require Apple/App Store Connect/deployment state that cannot be proven from the repository alone.
- The final state is ready only when `./scripts/app-store-submission-audit.sh --check-live-urls --run-submission-ready` exits 0 and `docs/APP_STORE_FINAL_SIGNOFF.md` contains `APP_STORE_FINAL_SIGNOFF_COMPLETE=true`.

## Apple References

- Overview of submitting for review: https://developer.apple.com/help/app-store-connect/manage-submissions-to-app-review/overview-of-submitting-for-review/
- Submit an app: https://developer.apple.com/help/app-store-connect/manage-submissions-to-app-review/submit-an-app/
- App and submission statuses: https://developer.apple.com/help/app-store-connect/reference/app-and-submission-statuses/
- Manage a submission with unresolved issues: https://developer.apple.com/help/app-store-connect/manage-submissions-to-app-review/manage-a-submission-with-unresolved-issues/
- Remove a submission from review: https://developer.apple.com/help/app-store-connect/manage-submissions-to-app-review/remove-a-submission-from-review
