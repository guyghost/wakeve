# App Store Connect Field Map - Wakeve

Date: 2026-05-27

Use this map when entering or reviewing the App Store Connect record for Bundle ID `com.guyghost.wakeve`. Fastlane can upload most metadata, but App Review submission remains manual because `submit_for_review` is disabled.

## Apple Source Baseline

Apple-source review date: 2026-05-27.

- Apple says App Information properties are shared across platforms and include Name, Subtitle, Privacy Policy URL, Bundle ID, SKU, Age Rating, License Agreement, Primary Language, Category, and Content Rights.
- Apple says the app Name must be at least two characters and no more than 30 characters.
- Apple says the Subtitle cannot be longer than 30 characters.
- Apple says the Privacy Policy URL is required for iOS and macOS apps, and App Privacy separately requires a Privacy Policy URL for all apps.
- Apple says Bundle ID must match the Xcode project Bundle ID and cannot be changed after a build is uploaded.
- Apple says SKU is an internal tracking ID, is not visible to customers, and cannot be changed after the app is added to the account.
- Apple says Age Rating is required and is set at the app level across platforms.
- Apple says Apple provides a standard EULA and developers may provide a custom license agreement for one or more regions.
- Apple says Primary Language is the default metadata language shown when localized metadata is not provided for a country or region.
- Apple says the primary category should match the category set in Xcode for macOS apps.
- Apple says privacy answers are app-level, must represent data practices across all platforms, and must include third-party partner practices.
- Apple says screenshots and app previews are managed on the platform section of the app page and require a new version to update after approval.
- Apple says before submitting an app version for review, required metadata must be provided and the right build must be selected for the version.
- Apple says the required role to submit an app for review is Account Holder, Admin, or App Manager.
- Apple says clicking Add for Review moves the app status to Ready for Review, but the submission is not sent until Submit for Review is clicked.
- Apple says App Review information should provide additional information or context that helps App Review during the review process.
- Apple says app versions for each platform are submitted separately.

## App Information

| App Store Connect field | Source |
| --- | --- |
| Name | `composeApp/metadata/ios/<locale>/name.txt` |
| Subtitle | `composeApp/metadata/ios/<locale>/subtitle.txt` |
| Bundle ID | `com.guyghost.wakeve` |
| SKU | App Store Connect-only value, choose a stable internal SKU and record evidence in `docs/APP_STORE_APP_INFORMATION_EVIDENCE.md` |
| Primary language | App Store Connect-only decision recorded in `docs/APP_STORE_APP_INFORMATION_EVIDENCE.md` |
| Category | App Store Connect-only decision recorded in `docs/APP_STORE_APP_INFORMATION_EVIDENCE.md` |
| Age rating | `composeApp/metadata/ios/app_rating_config.json` and `docs/APP_STORE_AGE_RATING.md` |
| Copyright | `composeApp/metadata/ios/copyright.txt` |
| License Agreement | Apple standard EULA or custom EULA decision recorded in `docs/APP_STORE_EULA_EVIDENCE.md` |

## Version Information

| App Store Connect field | Source |
| --- | --- |
| Description | `composeApp/metadata/ios/<locale>/description.txt` |
| Keywords | `composeApp/metadata/ios/<locale>/keywords.txt` |
| Promotional text | `composeApp/metadata/ios/<locale>/promotional_text.txt` |
| What's New | `composeApp/metadata/ios/<locale>/release_notes.txt` |
| Version and build number | `MARKETING_VERSION` and `CURRENT_PROJECT_VERSION` from Xcode, with App Store Connect duplicate-build evidence in `docs/APP_STORE_VERSIONING_EVIDENCE.md` |
| Release artifact evidence | Signed IPA/archive/dSYM/hash evidence in `docs/APP_STORE_RELEASE_ARTIFACT_EVIDENCE.md` |
| Content rights evidence | App name, icon, screenshots/previews, metadata text, provider references, and open-source notice review in `docs/APP_STORE_CONTENT_RIGHTS_EVIDENCE.md` |
| License notices evidence | Third-party dependency license inventory, attributions, notices, and prohibited-license review in `docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md` |
| EULA evidence | App Store Connect License Agreement, Terms of Service alignment, and legal approval in `docs/APP_STORE_EULA_EVIDENCE.md` |
| Support URL | `composeApp/metadata/ios/<locale>/support_url.txt` |
| Marketing URL | Optional, leave empty unless a product page is ready |
| Privacy Policy URL | `composeApp/metadata/ios/<locale>/privacy_url.txt` |
| Live URL/AASA evidence | Production legal/support/third-party-notices/AASA/backend evidence in `docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md` |
| Screenshots | `composeApp/screenshots/ios/<locale>/` |
| Media and localization evidence | Screenshot inventory, app preview decision, localized metadata review, and product-page consistency in `docs/APP_STORE_MEDIA_LOCALIZATION_EVIDENCE.md` |
| Pricing and availability evidence | Price, storefront, pre-order, education/business, custom app, tax category, and Paid Apps Agreement decisions in `docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md` |
| Version release option | Manual release, scheduled release, automatic release, and phased-release decision in `docs/APP_STORE_RELEASE_CONTROL_EVIDENCE.md` |

Current locales:

- `en-US`
- `fr-FR`

## App Review Information

| App Store Connect field | Source |
| --- | --- |
| Contact first name | `composeApp/metadata/ios/review_information/first_name.txt` |
| Contact last name | `composeApp/metadata/ios/review_information/last_name.txt` |
| Contact email | `composeApp/metadata/ios/review_information/email_address.txt` |
| Contact phone | `APP_REVIEW_PHONE_NUMBER` release secret, or `composeApp/metadata/ios/review_information/phone_number.txt` fallback |
| Notes | `composeApp/metadata/ios/review_information/notes.txt` |
| Account access evidence | `docs/APP_STORE_ACCOUNT_ACCESS_EVIDENCE.md` |
| Review access evidence | `docs/APP_STORE_REVIEW_ACCESS_EVIDENCE.md` |
| Demo account | App Store Connect-only value if review requires login |

`phone_number.txt` is intentionally not populated yet because it must be a real reachable review contact. Prefer `APP_REVIEW_PHONE_NUMBER` as a release secret to avoid committing PII. Do not commit a demo account password under review metadata; enter demo credentials directly in App Store Connect if the guest review path is not enough.

## Privacy And Compliance

| App Store Connect field | Source |
| --- | --- |
| Privacy labels | `docs/APP_STORE_PRIVACY_LABELS.md` |
| Privacy policy page | `docs/PRIVACY_POLICY.md` and deployed `/privacy` page |
| Terms of Service / EULA alignment | `docs/TERMS_OF_SERVICE.md`, deployed `/terms` page, and `docs/APP_STORE_EULA_EVIDENCE.md` |
| Privacy manifest | `iosApp/src/PrivacyInfo.xcprivacy` |
| SDK privacy/signature evidence | Third-party SDK inventory, required privacy manifests, SDK signatures, and Xcode privacy report evidence in `docs/APP_STORE_SDK_PRIVACY_EVIDENCE.md` |
| License notices | Third-party dependency notices and attribution evidence in `docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md` |
| Export compliance | `ITSAppUsesNonExemptEncryption=false` in `iosApp/src/Info.plist` and evidence in `docs/APP_STORE_EXPORT_COMPLIANCE_EVIDENCE.md` |
| Pricing and availability | `docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md` |
| Account deletion | iOS Profile Settings plus backend deletion flow, tracked in `docs/APP_STORE_REVIEW_GUIDELINE_AUDIT.md` |
| Accessibility Nutrition Labels | `docs/APP_STORE_ACCESSIBILITY_LABELS.md` |
| EU DSA trader status | `docs/APP_STORE_DSA_TRADER_STATUS.md` |
| App Review Guideline / UGC readiness | `docs/APP_STORE_REVIEW_GUIDELINE_AUDIT.md` |
| Final release signoff | `docs/APP_STORE_FINAL_SIGNOFF.md` |

Privacy, account deletion, accessibility, and user-generated content moderation answers require final product/legal approval before App Review submission.

## Capabilities And Availability

| App Store Connect / Apple Developer area | Source |
| --- | --- |
| Push Notifications | Apple Developer App ID capability; app entitlement in `iosApp/src/Wakeve.entitlements` |
| Siri | Apple Developer App ID capability; app entitlement in `iosApp/src/Wakeve.entitlements` |
| Sign in with Apple | Apple Developer App ID capability; native iOS login and app entitlement in `iosApp/src/Wakeve.entitlements` |
| Associated Domains | Apple Developer App ID capability; app entitlement in `iosApp/src/Wakeve.entitlements` |
| Universal Links AASA | Deployed `https://wakeve.app/.well-known/apple-app-site-association` and `https://wakeve.app/apple-app-site-association`, with `APP_STORE_LIVE_URL_AASA_EVIDENCE_COMPLETE=true` recorded in `docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md` |
| Mac Apple silicon availability | `docs/APP_STORE_AVAILABILITY_DECISIONS.md` |
| Apple Vision Pro availability | `docs/APP_STORE_AVAILABILITY_DECISIONS.md` |
| EU storefront availability | `docs/APP_STORE_DSA_TRADER_STATUS.md` if EU availability is disabled instead of confirming trader status |

## Final Review Checks

Use this order before manual App Review submission:

1. Upload to internal TestFlight after signing is configured:

```bash
bundle exec fastlane ios upload_testflight
```

2. Complete TestFlight smoke testing and record evidence in `docs/APP_STORE_TESTFLIGHT_EVIDENCE.md`, then record the live legal/support/AASA/backend evidence in `docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md`, signed IPA/archive/dSYM/hash evidence in `docs/APP_STORE_RELEASE_ARTIFACT_EVIDENCE.md`, content rights/IP evidence in `docs/APP_STORE_CONTENT_RIGHTS_EVIDENCE.md`, pricing/storefront evidence in `docs/APP_STORE_PRICING_AVAILABILITY_EVIDENCE.md`, SDK privacy/signature evidence in `docs/APP_STORE_SDK_PRIVACY_EVIDENCE.md`, App Store release control evidence in `docs/APP_STORE_RELEASE_CONTROL_EVIDENCE.md`, media/localization evidence in `docs/APP_STORE_MEDIA_LOCALIZATION_EVIDENCE.md`, license notices evidence in `docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md`, and EULA evidence in `docs/APP_STORE_EULA_EVIDENCE.md`.
3. Set the release signoff variables and `APP_STORE_FINAL_SIGNOFF_COMPLETE=true` only after every evidence file is current.
4. Run the final aggregated non-uploading audit:

```bash
./scripts/app-store-submission-audit.sh --check-live-urls --run-submission-ready
```

The audit command runs `submission_ready` when `--run-submission-ready` is supplied and fails if that final gate is skipped. It is expected to stay non-zero until live URLs, signed build readiness, TestFlight evidence, App Review access evidence, final signoffs, and Apple/App Store Connect values are all complete.

5. Upload the App Store build, metadata, screenshots, and age-rating config without automatic review submission:

```bash
bundle exec fastlane ios upload_appstore
```

Only submit manually after checking the uploaded version in App Store Connect.
