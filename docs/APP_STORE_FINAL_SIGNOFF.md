# App Store Final Signoff - Wakeve

Date: 2026-05-28

Status: NOT SIGNED OFF

Do not change the marker below until every item in this file has direct current evidence. This file is intentionally required in addition to environment variables so the final upload path has an auditable release record.

APP_STORE_FINAL_SIGNOFF_COMPLETE=false

## Apple Source Baseline

Apple-source review date: 2026-05-28.

- Apple says the app review process covers each submitted app version and related submitted items.
- Apple says app versions for each platform are submitted separately, and the status of an app version on one platform does not influence another platform.
- Apple says each platform can typically have one app version submission under review at a time, with a maximum of two submissions under review when one submission contains the app version and another contains items without the app version.
- Apple says items associated with different platforms cannot be added to the same submission.
- Apple says before submitting an app version for review, the required metadata must be provided and the build must be chosen for the version.
- Apple says the required App Store Connect role to submit an app for review is Account Holder, Admin, or App Manager.
- Apple says Add for Review changes the app status to Ready for Review, but the submission is not sent until Submit for Review is clicked.
- Apple says an app version can be added to an existing draft submission or to a new draft submission.
- Apple says all items submitted together must be accepted to complete the submission.
- Apple says submissions may not be reviewed in the order they are submitted.
- Apple says App Review information on the latest approved app version should provide additional context that helps App Review.
- Apple says App Review submissions should be final versions with all necessary metadata and fully functional URLs.
- Apple says placeholder text, empty websites, and other temporary content should be removed before submission.
- Apple says the app should be tested on device for bugs and stability before submission.
- Apple says contact information must be current in case App Review needs to reach the developer.
- Apple says apps with account-based features must provide full review access through an active demo account or a fully featured demo mode, plus any needed resources.
- Apple says backend services must be live and accessible during review.
- Apple says App Store status values distinguish preparation, ready-for-review intent, active review, binary problems, export-compliance waiting, and distribution readiness, so the signoff stays false until the current App Store Connect status and build evidence are recorded.
- Apple says Ready for Review means required metadata is entered and the app is intended for review, but it has not yet been submitted.
- Apple says Waiting for Review means Apple received the submission but has not started review; some information can still be edited, app previews can be deleted, and items can be removed, but screenshots and app previews cannot be uploaded or edited.
- Apple says In Review means App Review is reviewing the submission and the submission can be cancelled to remove it from review.
- Apple says if any item in a submission is rejected, the submission status changes to Unresolved Issues and App Review needs all items approved before the submission is considered approved.
- Apple says a submission with Unresolved Issues cannot have more items added; rejected items must be edited and resubmitted or removed.
- Apple says removing an app version or cancelling a submission changes an app-version submission to Developer Rejected and the review process starts over if resubmitted.
- Apple says Ready for Distribution requires accepted review state plus agreements in effect, and the Account Holder can accept latest agreements in Business.

## Required Release Variables

Before changing the final marker to `true`, these release variables must also be set to `true` in the release shell or CI secret store:

- `APP_STORE_PRIVACY_SIGNOFF`
- `APP_STORE_ACCESSIBILITY_SIGNOFF`
- `APP_STORE_AVAILABILITY_CONFIRMED`
- `APP_STORE_DSA_TRADER_STATUS_CONFIRMED`
- `APP_STORE_PRICING_AVAILABILITY_CONFIRMED`
- `APP_STORE_SDK_PRIVACY_CONFIRMED`
- `APP_STORE_RELEASE_CONTROL_CONFIRMED`
- `APP_STORE_MEDIA_LOCALIZATION_CONFIRMED`
- `APP_STORE_LICENSE_NOTICES_CONFIRMED`
- `APP_STORE_EULA_CONFIRMED`
- `APP_STORE_ACCOUNT_DELETION_CONFIRMED`
- `APP_STORE_UGC_MODERATION_CONFIRMED`
- `APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED`
- `TESTFLIGHT_SMOKE_PASSED`
- `APP_STORE_CAPABILITIES_CONFIRMED`

## Required Evidence

- [ ] App Review contact phone is present in the release environment as `APP_REVIEW_PHONE_NUMBER`, or in `composeApp/metadata/ios/review_information/phone_number.txt` as a fallback.
- [ ] Apple account access, App Store Connect role, two-factor authentication, app record ownership, and agreement status are recorded in `docs/APP_STORE_ACCOUNT_ACCESS_EVIDENCE.md`, which contains `APP_STORE_ACCOUNT_ACCESS_EVIDENCE_COMPLETE=true` for the submitted review build.
- [ ] `docs/APP_STORE_BLOCKER_REGISTER.md` has no open blockers, and every closed row has direct current evidence in its named gate.
- [ ] App Review notes match the uploaded review build's release-visible guest/reviewer path, or demo credentials have been entered manually in App Store Connect without committing passwords to the repository; `docs/APP_STORE_REVIEW_ACCESS_EVIDENCE.md` contains `APP_STORE_REVIEW_ACCESS_EVIDENCE_COMPLETE=true` for the uploaded review build.
- [ ] App Store Connect App Information values for Bundle ID, SKU, primary language, category, and generated age rating match `docs/APP_STORE_CONNECT_FIELD_MAP.md`; `docs/APP_STORE_APP_INFORMATION_EVIDENCE.md` contains `APP_STORE_APP_INFORMATION_EVIDENCE_COMPLETE=true` for the submitted review build.
- [ ] App Store Connect version/build comparison confirms the submitted build number has not been reused for the selected version; `docs/APP_STORE_VERSIONING_EVIDENCE.md` contains `APP_STORE_VERSIONING_EVIDENCE_COMPLETE=true` for the uploaded review build.
- [ ] Signed IPA, Xcode archive, dSYM UUIDs, SHA-256 hash, release commit, Fastlane logs, and App Store Connect uploaded build number are recorded in `docs/APP_STORE_RELEASE_ARTIFACT_EVIDENCE.md`, which contains `APP_STORE_RELEASE_ARTIFACT_EVIDENCE_COMPLETE=true` for the submitted review build.
- [ ] App name, icon assets, screenshots/previews, metadata text, third-party provider references, and open-source notice obligations are reviewed in `docs/APP_STORE_CONTENT_RIGHTS_EVIDENCE.md`, which contains `APP_STORE_CONTENT_RIGHTS_EVIDENCE_COMPLETE=true` for the submitted review build.
- [ ] `bundle exec fastlane ios preflight strict:true live_urls:true` passes with the real `APPLE_TEAM_ID`; `docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md` contains `APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=true` for production legal/support/AASA/backend URLs.
- [ ] `bundle exec fastlane ios submission_ready` passes with real Apple credentials and signing.
- [ ] `bundle exec fastlane ios validate_ipa_entitlements ipa:build/ios/WakeveApp.ipa` passes for the signed IPA.
- [ ] `./scripts/app-store-submission-audit.sh --check-live-urls --run-submission-ready` passes.
- [ ] App Store Connect privacy labels match `docs/APP_STORE_PRIVACY_LABELS.md`, the live privacy policy, and `iosApp/src/PrivacyInfo.xcprivacy`; `docs/APP_STORE_PRIVACY_EVIDENCE.md` contains `APP_STORE_PRIVACY_EVIDENCE_COMPLETE=true` for the uploaded review build.
- [ ] Accessibility Nutrition Labels are left unpublished or backed by the device evidence in `docs/APP_STORE_ACCESSIBILITY_LABELS.md`; `docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md` contains `APP_STORE_ACCESSIBILITY_EVIDENCE_COMPLETE=true` for the uploaded review build.
- [ ] App Store Connect availability matches `docs/APP_STORE_AVAILABILITY_DECISIONS.md`; `docs/APP_STORE_AVAILABILITY_EVIDENCE.md` contains `APP_STORE_AVAILABILITY_EVIDENCE_COMPLETE=true` for the uploaded review build.
- [ ] EU DSA trader status or EU storefront availability matches `docs/APP_STORE_DSA_TRADER_STATUS.md`, and that file contains `APP_STORE_DSA_TRADER_STATUS_EVIDENCE_COMPLETE=true`.
- [ ] App Store Connect Pricing and Availability choices for price, storefronts, pre-order, education/business distribution, custom app distribution, tax category, and Paid Apps Agreement compatibility are recorded in `docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md`, which contains `APP_STORE_PRICING_AVAILABILITY_EVIDENCE_COMPLETE=true` for the submitted review build.
- [ ] Third-party SDK inventory, required SDK privacy manifests, SDK signatures, and the Xcode privacy report are recorded in `docs/APP_STORE_SDK_PRIVACY_EVIDENCE.md`, which contains `APP_STORE_SDK_PRIVACY_EVIDENCE_COMPLETE=true` for the submitted review build.
- [ ] App Store Connect release option, phased-release decision, post-approval owner, release window, stop/pause criteria, and first-day monitoring plan are recorded in `docs/APP_STORE_RELEASE_CONTROL_EVIDENCE.md`, which contains `APP_STORE_RELEASE_CONTROL_EVIDENCE_COMPLETE=true` for the submitted review build.
- [ ] iPhone/iPad screenshots, app preview decision, EN/FR localized metadata, metadata limits, and product-page consistency are reviewed in `docs/APP_STORE_MEDIA_LOCALIZATION_EVIDENCE.md`, which contains `APP_STORE_MEDIA_LOCALIZATION_EVIDENCE_COMPLETE=true` for the submitted review build.
- [ ] Third-party dependency license inventory, attribution obligations, notice delivery path, and unresolved-license checks are recorded in `docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md`, which contains `APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=true` for the submitted review build.
- [ ] App Store Connect License Agreement choice, Apple standard EULA or custom EULA decision, terms alignment, and legal owner approval are recorded in `docs/APP_STORE_EULA_EVIDENCE.md`, which contains `APP_STORE_EULA_EVIDENCE_COMPLETE=true` for the submitted review build.
- [ ] Account deletion readiness matches `docs/APP_STORE_REVIEW_GUIDELINE_AUDIT.md`, including in-app initiation, full account/data deletion scope, completion messaging, and Sign in with Apple token revocation where applicable; `docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md` contains `APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=true` for the uploaded review build.
- [ ] User-generated content moderation readiness matches `docs/APP_STORE_REVIEW_GUIDELINE_AUDIT.md`, including filtering, reporting, blocking, published contact information, and reviewer-visible evidence; `docs/APP_STORE_UGC_MODERATION_EVIDENCE.md` contains `APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=true` for the uploaded review build.
- [ ] Payment and external purchase compliance matches `docs/APP_STORE_PAYMENT_COMPLIANCE.md`, including no external unlock of digital app features and reviewer notes for real-world shared-event expenses if payment surfaces are enabled, and `docs/APP_STORE_PAYMENT_EVIDENCE.md` contains `APP_STORE_PAYMENT_EVIDENCE_COMPLETE=true` for the uploaded review build.
- [ ] TestFlight smoke testing in `docs/APP_STORE_LAUNCH_CHECKLIST.md` is complete on iPhone and iPad, `docs/APP_STORE_TESTFLIGHT_EVIDENCE.md` contains `TESTFLIGHT_SMOKE_EVIDENCE_COMPLETE=true`, and `docs/APP_STORE_OBSERVABILITY_EVIDENCE.md` contains `APP_STORE_OBSERVABILITY_EVIDENCE_COMPLETE=true` for the uploaded review build.
- [ ] Apple Developer App ID and Release provisioning profile include Push Notifications, Siri, Sign in with Apple, and Associated Domains, and `docs/APP_STORE_CAPABILITIES_EVIDENCE.md` contains `APP_STORE_CAPABILITIES_EVIDENCE_COMPLETE=true` for the uploaded review build.
- [ ] Uploaded App Store Connect build, screenshots, metadata, age rating, review notes, and support/privacy URLs have been checked manually; `docs/APP_STORE_EXPORT_COMPLIANCE_EVIDENCE.md` contains `APP_STORE_EXPORT_COMPLIANCE_EVIDENCE_COMPLETE=true` for the uploaded review build.
- [ ] App Store Connect final state is recorded: platform submission scope is iOS only unless other platforms are intentionally included, draft submission membership is understood, all submitted items are accepted or intentionally excluded, there is no Unresolved Issues state, and any removal/cancel/retry decision is documented.

## Blocker Evidence Matrix

Before changing `APP_STORE_FINAL_SIGNOFF_COMPLETE` to `true`, every blocker row below must be copied from the active blocker register with current evidence notes, reviewer, date, and command or App Store Connect screenshot reference. Do not delete rows; mark them closed only after the named gate has passed.

| ID | Evidence to Record Before Final Signoff | Closure Evidence |
| --- | --- | --- |
| AS-01 | Apple account email, App Store Connect role, two-factor authentication, app record access, agreement status, and reviewer access evidence recorded in `docs/APP_STORE_ACCOUNT_ACCESS_EVIDENCE.md` and `docs/APP_STORE_REVIEW_ACCESS_EVIDENCE.md`. | `APPLE_ID` is set and format validation passes; `APP_STORE_ACCOUNT_ACCESS_EVIDENCE_COMPLETE=true`; `APP_STORE_REVIEW_ACCESS_EVIDENCE_COMPLETE=true`. |
| AS-02 | App Store Connect numeric team selected for Wakeve. | `ITC_TEAM_ID` is set and format validation passes. |
| AS-03 | Apple Developer signing team selected for the archive. | `TEAM_ID` is set and format validation passes. |
| AS-04 | Production AASA app ID uses the same Apple Team ID as signing. | `APPLE_TEAM_ID` matches `TEAM_ID` and live AASA validation passes. |
| AS-05 | Privacy/legal owner approval for App Store Connect privacy labels recorded in `docs/APP_STORE_PRIVACY_EVIDENCE.md`. | `APP_STORE_PRIVACY_SIGNOFF=true` and `APP_STORE_PRIVACY_EVIDENCE_COMPLETE=true`. |
| AS-06 | Accessibility label publication decision and device evidence recorded in `docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md` if labels are claimed. | `APP_STORE_ACCESSIBILITY_SIGNOFF=true` and `APP_STORE_ACCESSIBILITY_EVIDENCE_COMPLETE=true`. |
| AS-07 | Mac Apple silicon and Apple Vision Pro availability decision recorded in `docs/APP_STORE_AVAILABILITY_EVIDENCE.md`. | `APP_STORE_AVAILABILITY_CONFIRMED=true` and `APP_STORE_AVAILABILITY_EVIDENCE_COMPLETE=true`. |
| AS-08 | EU DSA trader status or EU storefront exclusion evidence recorded in `docs/APP_STORE_DSA_TRADER_STATUS.md`. | `APP_STORE_DSA_TRADER_STATUS_CONFIRMED=true` and `APP_STORE_DSA_TRADER_STATUS_EVIDENCE_COMPLETE=true`. |
| AS-09 | Account deletion flow evidence, backend deletion evidence, local cleanup, and Sign in with Apple revocation evidence where applicable are recorded in `docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md`. | `APP_STORE_ACCOUNT_DELETION_CONFIRMED=true` and `APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=true`. |
| AS-10 | UGC filtering, reporting, blocking, moderation contact, and reviewer-visible moderation evidence are recorded in `docs/APP_STORE_UGC_MODERATION_EVIDENCE.md`. | `APP_STORE_UGC_MODERATION_CONFIRMED=true` and `APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=true`. |
| AS-11 | Payment/Tricount review evidence recorded in `docs/APP_STORE_PAYMENT_EVIDENCE.md` showing flows are only for real-world shared event expenses and do not unlock digital features. | `APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED=true` and `APP_STORE_PAYMENT_EVIDENCE_COMPLETE=true`. |
| AS-12 | TestFlight smoke checklist results on iPhone and iPad, plus crash/dSYM/backend/support monitoring evidence recorded in `docs/APP_STORE_TESTFLIGHT_EVIDENCE.md` and `docs/APP_STORE_OBSERVABILITY_EVIDENCE.md`. | `TESTFLIGHT_SMOKE_PASSED=true`, `TESTFLIGHT_SMOKE_EVIDENCE_COMPLETE=true`, and `APP_STORE_OBSERVABILITY_EVIDENCE_COMPLETE=true`. |
| AS-13 | Signed IPA entitlement inspection and Apple Developer capability/profile check recorded in `docs/APP_STORE_CAPABILITIES_EVIDENCE.md`. | `APP_STORE_CAPABILITIES_CONFIRMED=true` and `APP_STORE_CAPABILITIES_EVIDENCE_COMPLETE=true`. |
| AS-14 | Public production URL, AASA, and backend health check output recorded in `docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md`. | `./scripts/app-store-submission-audit.sh --check-live-urls` has run successfully and `APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=true`. |
| AS-15 | Signed final submission-ready gate output, App Information evidence, versioning evidence, release artifact evidence, content rights evidence, and export compliance evidence recorded in `docs/APP_STORE_EXPORT_COMPLIANCE_EVIDENCE.md`, `docs/APP_STORE_APP_INFORMATION_EVIDENCE.md`, `docs/APP_STORE_VERSIONING_EVIDENCE.md`, `docs/APP_STORE_RELEASE_ARTIFACT_EVIDENCE.md`, and `docs/APP_STORE_CONTENT_RIGHTS_EVIDENCE.md`. | `./scripts/app-store-submission-audit.sh --run-submission-ready` has run successfully, `APP_STORE_APP_INFORMATION_EVIDENCE_COMPLETE=true`, `APP_STORE_VERSIONING_EVIDENCE_COMPLETE=true`, `APP_STORE_RELEASE_ARTIFACT_EVIDENCE_COMPLETE=true`, `APP_STORE_CONTENT_RIGHTS_EVIDENCE_COMPLETE=true`, and `APP_STORE_EXPORT_COMPLIANCE_EVIDENCE_COMPLETE=true`. |
| AS-16 | App Review contact phone value supplied from the release environment or the intentional metadata fallback, without committing private demo credentials. | `APP_REVIEW_PHONE_NUMBER` passes format validation, or `composeApp/metadata/ios/review_information/phone_number.txt` passes the same strict preflight validation. |
| AS-17 | App Store Connect Pricing and Availability page evidence recorded in `docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md`, including price, storefronts, pre-order, education/business, custom app, tax category, and Paid Apps Agreement compatibility. | `APP_STORE_PRICING_AVAILABILITY_CONFIRMED=true` and `APP_STORE_PRICING_AVAILABILITY_EVIDENCE_COMPLETE=true`. |
| AS-18 | Third-party SDK inventory, required SDK privacy manifests, SDK signatures, and Xcode privacy report evidence recorded in `docs/APP_STORE_SDK_PRIVACY_EVIDENCE.md`. | `APP_STORE_SDK_PRIVACY_CONFIRMED=true` and `APP_STORE_SDK_PRIVACY_EVIDENCE_COMPLETE=true`. |
| AS-19 | App Store Connect release option, phased-release decision, release owner, release window, and stop/pause criteria recorded in `docs/APP_STORE_RELEASE_CONTROL_EVIDENCE.md`. | `APP_STORE_RELEASE_CONTROL_CONFIRMED=true` and `APP_STORE_RELEASE_CONTROL_EVIDENCE_COMPLETE=true`. |
| AS-20 | App Store screenshots, app preview decision, localized metadata, metadata limits, and product-page consistency evidence recorded in `docs/APP_STORE_MEDIA_LOCALIZATION_EVIDENCE.md`. | `APP_STORE_MEDIA_LOCALIZATION_CONFIRMED=true` and `APP_STORE_MEDIA_LOCALIZATION_EVIDENCE_COMPLETE=true`. |
| AS-21 | Third-party dependency license inventory, attribution/notice delivery, prohibited-license scan, and review-build match evidence recorded in `docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md`. | `APP_STORE_LICENSE_NOTICES_CONFIRMED=true` and `APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=true`. |
| AS-22 | App Store Connect License Agreement choice, Apple standard EULA or custom EULA decision, terms alignment, and legal approval recorded in `docs/APP_STORE_EULA_EVIDENCE.md`. | `APP_STORE_EULA_CONFIRMED=true` and `APP_STORE_EULA_EVIDENCE_COMPLETE=true`. |

## Signoff

- Release owner:
- Date:
- App Store Connect version:
- Build number:
- App Store Connect submission status:
- Submitted items:
- Draft submission ID or screenshot:
- Evidence notes:

## Apple References

- Overview of submitting for review: https://developer.apple.com/help/app-store-connect/manage-submissions-to-app-review/overview-of-submitting-for-review/
- Submit an app: https://developer.apple.com/help/app-store-connect/manage-submissions-to-app-review/submit-an-app/
- App and submission statuses: https://developer.apple.com/help/app-store-connect/reference/app-and-submission-statuses/
- Manage a submission with unresolved issues: https://developer.apple.com/help/app-store-connect/manage-submissions-to-app-review/manage-a-submission-with-unresolved-issues/
- Remove a submission from review: https://developer.apple.com/help/app-store-connect/manage-submissions-to-app-review/remove-a-submission-from-review
