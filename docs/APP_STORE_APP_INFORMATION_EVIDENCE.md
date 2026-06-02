# App Store App Information Evidence - Wakeve

Date: 2026-06-01

Status: PENDING

Do not change the marker below until the App Store Connect App Information page for the exact release record has been reviewed and recorded.

APP_STORE_APP_INFORMATION_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-05-28.

- Apple says an app record must be created before uploading a build to App Store Connect.
- Apple says the latest agreement in the Business section must be signed before an app can be added to the account.
- Apple says the required role for adding a new app is Account Holder, App Manager, or Admin.
- Apple says adding a new app record includes app name, primary language, bundle ID, SKU, and whether to limit or give full user access.
- Apple says the app name must be at least two characters and no more than 30 characters, and can be edited only until App Review submission or while a new version/editable status permits it.
- Apple says the subtitle cannot be longer than 30 characters.
- Apple says Privacy Policy URL is required for iOS and macOS apps.
- Apple says Bundle ID is a unique identifier for the app that is used throughout the system.
- Apple says the Bundle ID must match the bundle ID set in the Xcode project and cannot be changed after uploading a build.
- Apple says Apple ID is automatically generated for the app when it is added to the account.
- Apple says SKU is a unique ID for the app that is not visible on the App Store.
- Apple says SKU can contain letters, numbers, hyphens, periods, and underscores; it cannot start with a hyphen, period, or underscore; and it cannot be changed after the app is added to the account.
- Apple says Primary Language is the default language for product-page metadata and is displayed when localized metadata is not provided for a country or region.
- Apple says localized metadata display can vary based on the App Store country or region, device language settings, added languages, and primary language.
- Apple says the primary category set in App Store Connect should match the category set in Xcode.
- Apple says age rating is a required app information property that helps users assess content and potentially objectionable material.
- Apple says the app age rating is determined by answering the age-rating questionnaire in App Store Connect.
- Apple says App Store Connect translates age-rating questionnaire answers into an Apple global age rating and additional region-specific ratings when required.
- Apple says the Made for Kids age-category selection cannot be changed after App Review approval.
- Apple says Unrated apps cannot be published on the App Store.

## Scope

This file records the App Store Connect-only values that are not fully represented by Fastlane metadata files but are required before manual App Review submission.

## Required App Information Values

| Field | Expected Value Or Decision | Status | Evidence |
| --- | --- | --- | --- |
| Bundle ID | `com.guyghost.wakeve` | Pending | App Store Connect app record screenshot or export. |
| App name | `Wakeve`, 2-30 characters in each App Store localization. | Pending | App Store Connect app information screenshot/export plus local metadata. |
| SKU | Stable internal SKU chosen for Wakeve. | Pending | App Store Connect App Information screenshot or export. |
| Primary language | `en-US` unless release owner intentionally chooses another primary locale. | Pending | App Store Connect App Information screenshot or export. |
| User access | Full Access or Limited Access choice intentionally recorded for the App Store Connect app record. | Pending | App Store Connect App Information screenshot or export. |
| Category | `Productivity` preferred because the Xcode project sets `public.app-category.productivity`; any different App Store Connect category must be release-owner-approved and reconciled with Xcode. | Pending | App Store Connect App Information screenshot or export. |
| Age rating | Generated rating matches `docs/APP_STORE_AGE_RATING.md` and `composeApp/metadata/ios/app_rating_config.json`. | Pending | App Store Connect age-rating screenshot or export. |
| Privacy Policy URL | `https://wakeve.app/privacy` for every iOS localization. | Pending | App Store Connect App Information screenshot/export plus local metadata. |
| Subtitle | 30 characters or fewer for each App Store localization. | Pending | App Store Connect app information screenshot/export plus local metadata. |

## Evidence Commands

Run before final signoff:

```bash
./scripts/lint-store-metadata.sh --ios-only
bundle exec fastlane ios validate_metadata
```

## Local App Information Scan Result

Local scan date: 2026-06-01

Result: repository metadata and local bundle identifiers are internally aligned for the current pre-submission candidate.

- Fastlane Appfile iOS app identifier: `com.guyghost.wakeve`.
- Xcode project app target uses `PRODUCT_BUNDLE_IDENTIFIER = com.guyghost.wakeve`, `PRODUCT_NAME = Wakeve`, `MARKETING_VERSION = 1.0`, `CURRENT_PROJECT_VERSION = 1`, and `INFOPLIST_KEY_LSApplicationCategoryType = public.app-category.productivity`.
- Source Info.plist declares `CFBundleDisplayName` as `Wakeve`, `CFBundleIdentifier` as `com.guyghost.wakeve`, `CFBundleShortVersionString` as `1.0`, and `CFBundleVersion` as `1`.
- iOS metadata locales present: `en-US` and `fr-FR`; both localized `name.txt` files contain `Wakeve`.
- Localized app names are 2-30 characters: `Wakeve` in `en-US` and `fr-FR`.
- Localized subtitles are 30 characters or fewer: `Plan events, vote, done.` in `en-US` and `Planifiez, votez, c'est prêt.` in `fr-FR`.
- Localized privacy and support URLs point to `https://wakeve.app/privacy` and `https://wakeve.app/support`.
- `composeApp/metadata/ios/copyright.txt` contains `© 2026 Wakeve`.
- `composeApp/metadata/ios/app_rating_config.json` parses successfully; configured content descriptors are `NONE` or `false`, and `kidsAgeBand` is absent.
- Built local unsigned Release app resolves `CFBundleIdentifier => "com.guyghost.wakeve"`, `CFBundleDisplayName => "Wakeve"`, `CFBundleShortVersionString => "1.0"`, `CFBundleVersion => "1"`, and `LSApplicationCategoryType => "public.app-category.productivity"`.
- `bundle exec fastlane ios validate_metadata` passed locally for both locales and screenshot sets on 2026-06-01.

This is local pre-submission evidence only. It does not close this evidence item until App Store Connect shows the exact app record, immutable SKU, primary language, user access choice, category, generated age rating, privacy policy URL, and uploaded review-build metadata for the release being submitted.

## Apple Field References

- App Store Connect App Information reference: https://developer.apple.com/help/app-store-connect/reference/app-information
- Add a new app record: https://developer.apple.com/help/app-store-connect/create-an-app-record/add-a-new-app/
- App Store localizations reference: https://developer.apple.com/help/app-store-connect/reference/app-information/app-store-localizations/
- App Store Connect age-rating reference: https://developer.apple.com/help/app-store-connect/reference/age-ratings/
- Set an app age rating: https://developer.apple.com/help/app-store-connect/manage-app-information/set-an-app-age-rating

Record:

- App Store Connect app record URL or screenshot reference.
- Reviewer and date.
- Final SKU.
- Primary language.
- User access choice.
- Primary category and secondary category if set.
- Privacy Policy URL.
- Generated age rating shown by App Store Connect.

## Closure Rule

Set `APP_STORE_APP_INFORMATION_EVIDENCE_COMPLETE=true` only after:

- The App Store Connect app record uses Bundle ID `com.guyghost.wakeve`.
- SKU, primary language, user access, and category are intentionally selected.
- App name, subtitle, privacy policy URL, and localization choices match the submitted metadata.
- The generated age rating has been compared with `docs/APP_STORE_AGE_RATING.md`.
- `docs/APP_STORE_FINAL_SIGNOFF.md` references this completed evidence for the submitted review build.
