# App Store Media And Localization Evidence - Wakeve

Date: 2026-06-01

Status: PENDING

Do not change the marker below until the exact App Store Connect version under review has current screenshots, optional preview decisions, and localized metadata reviewed against the uploaded build.

APP_STORE_MEDIA_LOCALIZATION_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-05-28.

- Apple App Store Connect accepts a minimum of one and a maximum of ten screenshots per app preview and screenshot set in `.jpeg`, `.jpg`, or `.png` formats.
- Apple says screenshots and app previews visually communicate the app's user experience on the product page.
- Apple requires iPad screenshots when the app runs on iPad; Wakeve targets both iPhone and iPad, so both device families need review-build media evidence.
- App previews are optional; when provided, App Store Connect accepts up to three app previews per supported device size and language, and app previews always precede screenshots on iPhone and iPad.
- Apple says iPhone 6.9-inch screenshot sizes include `1320 x 2868` portrait, and iPad 13-inch screenshot sizes include `2048 x 2732` portrait.
- Apple says if an app's user interface is the same across device sizes and localizations, the highest-resolution required screenshots can be provided and scaled down for smaller device sizes.
- Apple says Media Manager can be used to provide specific screenshots and app previews for additional device sizes and localizations when the release owner does not want scaled versions.
- Apple app preview specifications require videos to be 15 to 30 seconds, up to 500MB, up to 30 frames per second, use accepted H.264 or ProRes 422 formats, and include a poster-frame decision.
- Apple app preview specifications list accepted extensions `.mov`, `.m4v`, and `.mp4` for H.264 and `.mov` for ProRes 422.
- Apple says a localized app preview falls back to the next best available language when a localized preview is missing.
- Apple says screenshots and app previews can be uploaded from the platform section when an app is in Prepare for Submission, Ready for Review, Invalid Binary, Rejected, Metadata Rejected, or Developer Rejected status.
- Apple says after an app is submitted for review and approved, a new version is required to update screenshots.
- Apple localized metadata can display to users by language or country/region fallback, so `en-US` and `fr-FR` metadata and screenshots must be checked for meaning parity and no misleading claims.

## Required Review

| Area | Required Evidence | Result |
| --- | --- | --- |
| Screenshot inventory | Every iPhone and iPad screenshot uploaded for `en-US` and `fr-FR` is listed with filename, dimensions, locale, and device family. | Pending |
| Screenshot accuracy | Screenshots show the submitted review build, current UI, allowed account/data fixtures, and no unavailable features. | Pending |
| Screenshot ordering | Screenshot order in App Store Connect matches the intended product story for first-run review and customer discovery. | Pending |
| App preview decision | App previews are intentionally omitted for first release, or every uploaded preview has format, duration, poster frame, locale, and rights evidence. | Pending |
| Localization parity | `en-US` and `fr-FR` name, subtitle, description, keywords, promotional text, release notes, privacy URL, and support URL are reviewed for meaning parity and no misleading claims. | Pending |
| Metadata limits | Localized metadata fits App Store field limits and matches the current Fastlane metadata payload. | Pending |
| Device-family coverage | iPhone and iPad media are present because the Release build targets both device families. | Pending |
| Scaling/Media Manager decision | The release owner records whether the highest-resolution screenshots are allowed to scale down, or whether Media Manager contains device-specific overrides. | Pending |
| Editable-status check | App Store Connect status allows screenshot/app-preview upload or the release owner has created a new version before changing approved media. | Pending |
| Product-page consistency | Screenshots, descriptions, privacy labels, age rating, pricing, support URL, and App Review notes do not contradict each other. | Pending |

## Apple References

- App Store Connect requires one to ten screenshots in `.jpeg`, `.jpg`, or `.png` formats: https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications/
- Apps that run on iPad need iPad screenshots, and Apple lists accepted screenshot sizes by display family: https://developer.apple.com/help/app-store-connect/reference/app-information/screenshot-specifications/
- Apple says app previews are optional; if provided, App Store Connect accepts up to three previews per supported device size and language: https://developer.apple.com/help/app-store-connect/manage-app-information/upload-app-previews-and-screenshots/
- App previews must follow App Store Connect video requirements such as accepted formats, 15-30 second duration, poster frame, and device capture requirements: https://developer.apple.com/help/app-store-connect/reference/app-preview-specifications
- Localized app information controls the metadata shown to customers in each App Store country or region: https://developer.apple.com/help/app-store-connect/manage-app-information/localize-app-information

## Evidence Commands

Run or record equivalent output for the submitted review build:

```bash
./scripts/lint-store-metadata.sh --ios-only
bundle exec fastlane ios validate_metadata
find composeApp/screenshots/ios composeApp/metadata/ios -type f | sort
sips -g pixelWidth -g pixelHeight composeApp/screenshots/ios/*/*.{png,jpg,jpeg} 2>/dev/null
find composeApp/screenshots/ios composeApp/metadata/ios -type f \( -iname '*.png' -o -iname '*.jpg' -o -iname '*.jpeg' \) -print0 | sort -z | xargs -0 shasum -a 256 | shasum -a 256
```

## Local Media And Metadata Scan Result

Result: local Fastlane media and localized metadata are structurally ready for upload, but this is pre-submission evidence only because the App Store Connect media page and uploaded review build have not been reviewed.

- Local media scan date: 2026-06-01.
- Locales present: `en-US` and `fr-FR`.
- Upload screenshot folders present: `composeApp/screenshots/ios/en-US` and `composeApp/screenshots/ios/fr-FR`.
- Metadata screenshot folders present: `composeApp/metadata/ios/en-US/screenshots` and `composeApp/metadata/ios/fr-FR/screenshots`.
- Screenshot inventory: 8 PNG files across upload and metadata folders.
- Screenshot dimensions: every `01-iphone-home.png` is `1320x2868`; every `02-ipad-home.png` is `2048x2732`.
- Device-family coverage: each locale has one iPhone screenshot and one iPad screenshot in both the Fastlane upload folder and the metadata folder.
- Screenshot sizes match Apple-accepted iPhone 6.9-inch portrait `1320 x 2868` and iPad 13-inch portrait `2048 x 2732` sizes.
- Screenshot set hash: `02874fd36e52a53083d3b6cecf22c23d0d640d5d402e57f4457fd6c821e0be97`.
- App preview videos: 0 `.mov`, `.mp4`, or `.m4v` files found under the iOS metadata and screenshot directories.
- App preview decision: omit previews for the first release; no preview localization fallback is relied on.
- Fastlane metadata validation: `bundle exec fastlane ios validate_metadata` passed locally for `fr-FR` metadata, `en-US` metadata, `fr-FR` screenshots, and `en-US` screenshots on 2026-06-01.
- Local field lengths are within App Store limits after trimming surrounding whitespace: `en-US` name 6 chars, subtitle 24, description 1540, keywords 70, promotional text 85, release notes 179; `fr-FR` name 6 chars, subtitle 29, description 1987, keywords 88, promotional text 109, release notes 196.
- Both locales use `https://wakeve.app/privacy` and `https://wakeve.app/support` for privacy and support URLs.

This local scan does not close AS-20. The final reviewer must compare the App Store Connect media page and localized metadata against the uploaded review build, confirm screenshot ordering and visible data, record whether scaled screenshots or Media Manager overrides are used, verify the App Store Connect status still permits media edits or that a new version is being edited, verify live production URLs, and record reviewer/date before setting `APP_STORE_MEDIA_LOCALIZATION_EVIDENCE_COMPLETE=true`.

## Evidence To Attach

Record these before final signoff:

- App Store Connect media page screenshot/export for every locale and device family.
- App Store Connect status at the time of media review, proving screenshots and app previews are editable for the version being submitted.
- Filename, dimensions, locale, and device family for each screenshot uploaded by Fastlane.
- Reviewer/date confirming screenshots are from the uploaded review build or are still accurate for that build.
- Confirmation that app previews are omitted for first release, or per-preview format/duration/poster-frame evidence if enabled.
- Scaling or Media Manager decision for every enabled device family and locale.
- If app previews are added later, evidence for max file size, duration, frame rate, accepted extension, format, poster frame, device capture source, language fallback, and rights must be attached.
- EN/FR localized metadata reviewer/date, with notes for any intentional copy differences.
- Confirmation that privacy/support URLs in both locales point to the live production pages.
- Confirmation that screenshots and descriptions do not show unavailable account deletion, UGC moderation, payment, calendar, Siri, push, or AI behavior unless those flows are enabled and tested in the review build.

## Closure Rule

Set `APP_STORE_MEDIA_LOCALIZATION_EVIDENCE_COMPLETE=true` only after:

- App Store Connect media and localized metadata match the uploaded review build.
- iPhone and iPad screenshots are uploaded for every enabled App Store locale.
- Screenshot/app-preview edits are made only while the app version status permits edits, or through a new version after approval.
- Any reliance on scaled screenshots or Media Manager overrides is explicitly recorded.
- App previews are either intentionally omitted or fully validated.
- Localized copy has been reviewed for field limits, meaning parity, and consistency with privacy/legal/support evidence.
- `APP_STORE_MEDIA_LOCALIZATION_CONFIRMED=true` is set only in the final release shell or CI secret store after this file is updated.
- `docs/APP_STORE_FINAL_SIGNOFF.md` references this completed evidence for the submitted review build.
