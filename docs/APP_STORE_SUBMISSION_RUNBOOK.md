# App Store Submission Runbook - Wakeve

Date: 2026-06-01

This runbook is the operational checklist for moving Wakeve from local App Store readiness to a real App Store Connect submission. Keep `docs/APP_STORE_READINESS.md` as the audit report and `docs/APP_STORE_BLOCKER_REGISTER.md` as the active blocker ledger; use this file as the sequence to execute, then use `docs/APP_STORE_LAUNCH_CHECKLIST.md` for TestFlight smoke testing, monitoring, and rollback.

## Apple Source Baseline

Apple-source review date: 2026-05-28.

- Apple says builds can be uploaded after the app is added to the account, using Xcode, Swift Playground, altool, or Transporter.
- Apple says the build must be processed by Apple before it appears in App Store Connect.
- Apple says the bundle ID and version number associate an uploaded build with the App Store Connect app and version record.
- Apple says before submitting an app version for review, required metadata must be provided and the build must be chosen for the version.
- Apple says the required role to submit an app for review is Account Holder, Admin, or App Manager.
- Apple says the Build section should be verified before Add for Review.
- Apple says Add for Review changes the app status to Ready for Review, but the submission is not sent to App Review until Submit for Review is clicked.
- Apple says when review starts, the app status changes to In Review.
- Apple says submissions may not be reviewed in the order they are submitted.
- Apple says App Review information should provide additional context for the review process.
- Apple says app versions for each platform are submitted separately.
- Apple says submissions should be final versions with all necessary metadata and fully functional URLs.
- Apple says placeholder text, empty websites, and temporary content should be removed before submission.
- Apple says apps should be tested on device for crashes, bugs, and stability before submission.
- Apple says App Review contact information must be current.
- Apple says account-based features require full review access through an active demo account or fully featured demo mode, plus needed resources.
- Apple says backend services must be live and accessible during review.

## Current Gate

Do not submit to App Review yet.

The repository passes local iOS preflight, Release build without signing, metadata validation, Svelte checks, web production build, and low-severity web audit gates. `docs/APP_STORE_BLOCKER_REGISTER.md` tracks the active blocker list and evidence source for each row. The remaining blockers are external, product-gated, or require manual release evidence: Apple account and release environment values, App Store Connect team ID, App Store Connect role/agreement evidence, Apple Developer signing team, App Review contact phone, Apple Developer capabilities/profiles, live `wakeve.app` and `api.wakeve.app` deployment, production Apple Team ID in both AASA endpoints, privacy label/legal approval, accessibility evidence, App Store availability evidence, EU DSA trader status evidence, pricing and availability evidence, third-party SDK privacy evidence, App Store release control evidence, App Store media/localization evidence, license notices evidence, App Store EULA evidence, review access evidence, export compliance evidence, content rights/IP evidence, App Store versioning evidence, signed release artifact evidence, App Store observability evidence, account deletion readiness, user-generated content moderation readiness, payment/external purchase compliance, TestFlight smoke testing, signed final submission-ready gate output, and final signoff.

Product blockers AS-09 and AS-10 must be approved through `docs/APP_STORE_PRODUCT_BLOCKER_APPROVAL.md` before implementation starts. Do not set `APP_STORE_ACCOUNT_DELETION_CONFIRMED=true` or `APP_STORE_UGC_MODERATION_CONFIRMED=true` until the relevant OpenSpec proposal is approved, implemented, tested, and the matching evidence document is complete.

## Required External Values

Set these values in the release shell or CI secret store:

```bash
export APPLE_ID="<apple-account-email>"
export ITC_TEAM_ID="<app-store-connect-team-id>"
export TEAM_ID="<apple-developer-team-id>"
export APPLE_TEAM_ID="<apple-developer-team-id>"
export APP_REVIEW_PHONE_NUMBER="<reachable-review-phone>"
```

`TEAM_ID` is used for Xcode signing. `APPLE_TEAM_ID` is also used by the deployed web app to generate the `apple-app-site-association` app ID as `<APPLE_TEAM_ID>.com.guyghost.wakeve`. For this release, `TEAM_ID` and `APPLE_TEAM_ID` must match because the signed app entitlement and the AASA app ID must refer to the same Apple Developer team.
`APP_REVIEW_PHONE_NUMBER` is the App Review contact phone used by strict preflight and Fastlane `deliver`. Prefer storing it as a release secret because it is personally identifiable information.

Use `.env.appstore.example` as the non-secret template for local release shells or CI secret names. Do not commit a filled `.env.appstore` file.

The final audit also requires these explicit signoff variables:

```bash
export APP_STORE_PRIVACY_SIGNOFF=true
export APP_STORE_ACCESSIBILITY_SIGNOFF=true
export APP_STORE_AVAILABILITY_CONFIRMED=true
export APP_STORE_DSA_TRADER_STATUS_CONFIRMED=true
export APP_STORE_PRICING_AVAILABILITY_CONFIRMED=true
export APP_STORE_SDK_PRIVACY_CONFIRMED=true
export APP_STORE_RELEASE_CONTROL_CONFIRMED=true
export APP_STORE_MEDIA_LOCALIZATION_CONFIRMED=true
export APP_STORE_LICENSE_NOTICES_CONFIRMED=true
export APP_STORE_EULA_CONFIRMED=true
export APP_STORE_ACCOUNT_DELETION_CONFIRMED=true
export APP_STORE_UGC_MODERATION_CONFIRMED=true
export APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED=true
export TESTFLIGHT_SMOKE_PASSED=true
export APP_STORE_CAPABILITIES_CONFIRMED=true
```

Set them only after completing the corresponding privacy/legal review, accessibility label decision, Mac/Vision Pro availability decision, EU DSA trader status decision, pricing and storefront availability decision, third-party SDK privacy/signature evidence check, App Store release option and rollout-control check, App Store media/localization review, license notices review, App Store EULA/terms alignment review, account deletion readiness check with `docs/APP_STORE_ACCOUNT_DELETION_EVIDENCE.md` containing `APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=true`, user-generated content moderation readiness check with `docs/APP_STORE_UGC_MODERATION_EVIDENCE.md` containing `APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=true`, payment/external purchase compliance check, TestFlight smoke test, and Apple Developer capability/profile verification.

The final App Store upload also requires `docs/APP_STORE_FINAL_SIGNOFF.md` to contain `APP_STORE_FINAL_SIGNOFF_COMPLETE=true`. Keep it `false` until the evidence checklist in that file is complete.

## Apple Developer Setup

1. Confirm Bundle ID `com.guyghost.wakeve` exists in Apple Developer.
2. Enable these capabilities on the App ID:
   - Push Notifications
   - Siri
   - Sign in with Apple
   - Associated Domains
3. Regenerate or refresh provisioning profiles after enabling capabilities.
4. Confirm the Release profile contains:
   - `aps-environment=production`
   - `com.apple.developer.siri=true`
   - `com.apple.developer.applesignin` with `Default`
   - `com.apple.developer.associated-domains` with `applinks:wakeve.app`

Fastlane verifies these entitlements from the signed IPA after `bundle exec fastlane ios build`. You can rerun that inspection directly with `bundle exec fastlane ios validate_ipa_entitlements ipa:build/ios/WakeveApp.ipa`. Record the signed IPA output, Apple Developer App ID capabilities, Release provisioning profile refresh, and uploaded build number in `docs/APP_STORE_CAPABILITIES_EVIDENCE.md` before setting `APP_STORE_CAPABILITIES_CONFIRMED=true`.

## Production Web/API Setup

1. Deploy `webApp` from the `webApp/` project root. The SvelteKit app uses `@sveltejs/adapter-vercel`, so the Vercel project must keep `webApp/` as the root directory and build from the checked-in `pnpm-lock.yaml`.
2. Configure production `APPLE_TEAM_ID` or `TEAM_ID` on the web deployment with the real Apple Developer Team ID.
3. Point `wakeve.app` DNS to the deployed web app.
4. Point `api.wakeve.app` DNS to the production backend.
5. Verify:

```bash
cd webApp
npx --yes pnpm@10 check
npx --yes pnpm@10 build
```

Optional Vercel CLI deployment sequence:

```bash
cd webApp
vercel pull --yes --environment=production
vercel build --prod
vercel deploy --prebuilt --prod
```

6. Verify public review endpoints:

```bash
curl -I https://wakeve.app/privacy
curl -I https://wakeve.app/terms
curl -I https://wakeve.app/support
curl -I https://wakeve.app/third-party-notices
curl -i https://wakeve.app/.well-known/apple-app-site-association
curl -i https://wakeve.app/apple-app-site-association
curl -I https://api.wakeve.app/health
```

Both AASA responses must be HTTPS, public, JSON, extensionless, and include `<APPLE_TEAM_ID>.com.guyghost.wakeve`. Record the production web/API deployment IDs, DNS state, cache behavior, rollout owner, rollback owner, command output, reviewer, and date in `docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md`; set `APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=true` only after those checks pass with the real `APPLE_TEAM_ID`.

## App Store Connect Setup

1. Create or confirm the app record for Bundle ID `com.guyghost.wakeve`.
   - Record Apple account, two-factor authentication, App Store Connect role, app record ownership, agreement status, Paid Apps Agreement decision, and `ITC_TEAM_ID` evidence in `docs/APP_STORE_ACCOUNT_ACCESS_EVIDENCE.md` before final signoff. The manual submitter must have Account Holder, Admin, or App Manager access.
2. Enter or review the iOS metadata using `docs/APP_STORE_CONNECT_FIELD_MAP.md`.
   - Record Bundle ID, SKU, primary language, category, and generated age rating evidence in `docs/APP_STORE_APP_INFORMATION_EVIDENCE.md` before final signoff.
   - Record the App Store Connect version/build-number comparison in `docs/APP_STORE_VERSIONING_EVIDENCE.md` before final signoff; if the candidate `CFBundleVersion` was already uploaded for the same version, increment `CURRENT_PROJECT_VERSION` before uploading again.
   - Record the signed IPA SHA-256, Xcode archive, dSYM UUIDs, release commit, Fastlane logs, and App Store Connect uploaded build reference in `docs/APP_STORE_RELEASE_ARTIFACT_EVIDENCE.md` before final signoff.
   - Record content rights/IP evidence for app name, icon assets, screenshots/previews, metadata text, provider references, and open-source notices in `docs/APP_STORE_CONTENT_RIGHTS_EVIDENCE.md` before final signoff.
   - Record Pricing and Availability evidence for price, storefronts, pre-order, education/business distribution, custom app distribution, tax category, and Paid Apps Agreement compatibility in `docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md` before final signoff.
   - Record third-party SDK inventory, required listed-SDK privacy manifests, SDK signatures, and Xcode privacy report evidence in `docs/APP_STORE_SDK_PRIVACY_EVIDENCE.md` before final signoff.
   - Record App Store Version Release option, phased-release decision, release owner, release window, stop/pause criteria, and first-day monitoring plan in `docs/APP_STORE_RELEASE_CONTROL_EVIDENCE.md` before final signoff.
   - Record screenshot inventory, app preview decision, localized metadata review, and product-page consistency evidence in `docs/APP_STORE_MEDIA_LOCALIZATION_EVIDENCE.md` before final signoff.
   - Run `./scripts/app-store-license-inventory.sh --fetch-remote-metadata --output docs/APP_STORE_LICENSE_INVENTORY_DRAFT.md`, then run `./scripts/app-store-third-party-notices.sh --markdown-output docs/APP_STORE_THIRD_PARTY_NOTICES.md --web-output webApp/src/routes/third-party-notices/+page.svelte`, then record dependency license inventory, attribution/notice delivery path, prohibited-license review, and signed-build match evidence in `docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md` before final signoff.
   - Record App Store Connect License Agreement choice, Apple standard EULA or custom EULA decision, terms alignment, and legal approval in `docs/APP_STORE_EULA_EVIDENCE.md` before final signoff.
3. Enter App Review contact details.
   - Set `APP_REVIEW_PHONE_NUMBER` in the release shell/CI secret store before final automation. As a fallback, `composeApp/metadata/ios/review_information/phone_number.txt` is also accepted. The strict Fastlane gate accepts common separators but requires a plausible 4-20 digit phone number, optionally prefixed with `+`.
   - Keep `composeApp/metadata/ios/review_information/notes.txt` explicit about the guest review path, or enter demo credentials manually in App Store Connect. Do not commit demo account passwords to repository metadata.
   - Record guest/demo access evidence in `docs/APP_STORE_REVIEW_ACCESS_EVIDENCE.md` before final signoff.
4. Fill privacy labels using `docs/APP_STORE_PRIVACY_LABELS.md`.
5. Record App Store Connect privacy labels, live privacy policy, privacy manifest, Release binary no-tracking checks, and legal/privacy approval in `docs/APP_STORE_PRIVACY_EVIDENCE.md` before setting `APP_STORE_PRIVACY_SIGNOFF=true`.
6. Confirm the live privacy URL matches the final App Store Connect answers.
7. Confirm age rating from `composeApp/metadata/ios/app_rating_config.json`.
8. Record App Store Connect export compliance and encryption evidence in `docs/APP_STORE_EXPORT_COMPLIANCE_EVIDENCE.md` before final signoff.
9. Record App Review guest/demo access evidence in `docs/APP_STORE_REVIEW_ACCESS_EVIDENCE.md` before final signoff.
10. Decide Mac Apple silicon and Apple Vision Pro availability using `docs/APP_STORE_AVAILABILITY_DECISIONS.md`, then record the App Store Connect decision and matching accessibility/test evidence in `docs/APP_STORE_AVAILABILITY_EVIDENCE.md` before setting `APP_STORE_AVAILABILITY_CONFIRMED=true`.
11. Confirm EU Digital Services Act trader status or disable EU availability using `docs/APP_STORE_DSA_TRADER_STATUS.md`.
12. Enter Accessibility Nutrition Labels only after validating the conservative draft in `docs/APP_STORE_ACCESSIBILITY_LABELS.md` and recording the unpublished-label decision or device-backed label evidence in `docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md`.
13. Review `docs/APP_STORE_REVIEW_GUIDELINE_AUDIT.md`, especially the Guideline 1.2 user-generated content gate for comments/chat, the Guideline 3.1 payment compliance gate for payment/Tricount surfaces, and the Guideline 5.1.1(v) account deletion gate.
14. Record payment, settlement, Tricount, StoreKit/IAP absence or compliance, and App Review notes evidence in `docs/APP_STORE_PAYMENT_EVIDENCE.md` before setting `APP_STORE_PAYMENT_COMPLIANCE_CONFIRMED=true`.
15. Complete `docs/APP_STORE_FINAL_SIGNOFF.md` only after all final evidence is current.
16. Keep submission mode manual: Fastlane uses `submit_for_review: false`, and record App Store Connect release control evidence before public release.

## Command Sequence

Local check without signing:

```bash
bundle exec fastlane ios preflight
```

Final gate before uploading anything to App Store Connect:

```bash
APPLE_ID="$APPLE_ID" \
ITC_TEAM_ID="$ITC_TEAM_ID" \
TEAM_ID="$TEAM_ID" \
APPLE_TEAM_ID="$APPLE_TEAM_ID" \
APP_REVIEW_PHONE_NUMBER="$APP_REVIEW_PHONE_NUMBER" \
bundle exec fastlane ios submission_ready
```

Aggregated final audit before upload, using the same release environment and manual signoff variables as the upload lane:

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
./scripts/app-store-submission-audit.sh --check-live-urls --run-submission-ready
```

The aggregated audit treats skipped live URL/AASA validation and skipped `submission_ready` as blockers, and it cannot report a ready result if `--skip-preflight` is used. A ready result is only valid when this command has run local preflight plus both final gates. `bundle exec fastlane ios upload_appstore` also invokes this audit before delivering metadata or an IPA, so the upload path cannot diverge from the final release gate.
It also validates Apple release variable formats and rejects the documented template placeholders: `APPLE_ID` must look like a real Apple account email and not `release@example.com`, `ITC_TEAM_ID` must be numeric and not `123456789`, `TEAM_ID` / `APPLE_TEAM_ID` must be matching 10-character uppercase Apple Developer Team IDs and not `ABCDE12345`, and `APP_REVIEW_PHONE_NUMBER` must be a plausible non-placeholder phone number when supplied through the environment. The upload lane also rejects `docs/APP_STORE_FINAL_SIGNOFF.md` if the final marker is forced while unresolved `TBD`, `Pending`, `Status: NOT`, or unchecked checklist items remain.

Signed IPA entitlement inspection after a build:

```bash
TEAM_ID="$TEAM_ID" \
bundle exec fastlane ios validate_ipa_entitlements ipa:build/ios/WakeveApp.ipa
```

Upload to internal TestFlight after signing works:

```bash
APPLE_ID="$APPLE_ID" \
ITC_TEAM_ID="$ITC_TEAM_ID" \
TEAM_ID="$TEAM_ID" \
APP_REVIEW_PHONE_NUMBER="$APP_REVIEW_PHONE_NUMBER" \
bundle exec fastlane ios upload_testflight
```

The TestFlight lane runs the local App Store preflight before building and uploading the signed IPA. After TestFlight upload, complete `docs/APP_STORE_LAUNCH_CHECKLIST.md` and record the executed build, device matrix, smoke results, 24-hour monitoring window, crash/dSYM or symbolication evidence, backend monitoring, and support mailbox evidence in `docs/APP_STORE_TESTFLIGHT_EVIDENCE.md` and `docs/APP_STORE_OBSERVABILITY_EVIDENCE.md` before uploading App Store metadata or manually submitting for review.

Upload build, metadata, and screenshots to App Store Connect after the final gate passes:

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

## Expected Failures Until Production Is Ready

`bundle exec fastlane ios submission_ready`, `bundle exec fastlane ios upload_appstore`, and `bundle exec fastlane ios preflight strict:true live_urls:true` are expected to fail until:

- `APP_REVIEW_PHONE_NUMBER` is set, or `composeApp/metadata/ios/review_information/phone_number.txt` exists as a fallback.
- `APPLE_ID`, `ITC_TEAM_ID`, `TEAM_ID`, and `APPLE_TEAM_ID` are set with valid formats, and `TEAM_ID` matches `APPLE_TEAM_ID`.
- `https://wakeve.app/privacy` is live.
- `https://wakeve.app/terms` is live.
- `https://wakeve.app/support` is live.
- `https://wakeve.app/third-party-notices` is live.
- `https://wakeve.app/.well-known/apple-app-site-association` and `https://wakeve.app/apple-app-site-association` are live, valid JSON, served as `application/json`, contain `com.guyghost.wakeve`, and contain the real `<APPLE_TEAM_ID>.com.guyghost.wakeve` app ID.
- `https://api.wakeve.app/health` is live.
- `docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md` contains `APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=true` for the same production deployment.
- Xcode signing can create an App Store archive with the real team/profile.
- The App ID and Release provisioning profile include Push Notifications, Siri, Sign in with Apple, and Associated Domains.

In addition, `bundle exec fastlane ios upload_appstore` and `./scripts/app-store-submission-audit.sh --check-live-urls --run-submission-ready` are expected to fail until the manual signoff variables in `.env.appstore.example` are confirmed and `docs/APP_STORE_FINAL_SIGNOFF.md` contains `APP_STORE_FINAL_SIGNOFF_COMPLETE=true`. This intentionally keeps `submission_ready` usable before TestFlight smoke testing, while preventing the final App Store metadata/build upload until privacy, accessibility, availability, DSA trader status, pricing and storefront availability, third-party SDK privacy/signature evidence, App Store release control evidence, App Store media/localization evidence, license notices evidence, App Store EULA evidence, account deletion readiness, user-generated content moderation, TestFlight, App Store observability evidence, Apple capability checks, and the final signoff record are complete.

## Apple References

- App submission requirements: https://developer.apple.com/app-store/submitting/
- App privacy details: https://developer.apple.com/app-store/app-privacy-details/
- App privacy in App Store Connect: https://developer.apple.com/help/app-store-connect/reference/app-information/app-privacy/
- Sign in with Apple entitlement: https://developer.apple.com/documentation/bundleresources/entitlements/com.apple.developer.applesignin
- Associated Domains entitlement: https://developer.apple.com/documentation/bundleresources/entitlements/com.apple.developer.associated-domains
- Supporting associated domains: https://developer.apple.com/documentation/xcode/supporting-associated-domains
- Build uploads: https://developer.apple.com/help/app-store-connect/manage-builds/upload-builds
- Third-party SDK requirements: https://developer.apple.com/support/third-party-SDK-requirements/
- Select an App Store version release option: https://developer.apple.com/help/app-store-connect/manage-your-apps-availability/select-an-app-store-version-release-option/
- Release a version update in phases: https://developer.apple.com/help/app-store-connect/update-your-app/release-a-version-update-in-phases
