# App Store Content Rights Evidence - Wakeve

Date: 2026-06-01

Status: PENDING

Do not change the marker below until every App Store-facing asset, app-bundle asset, provider name, screenshot, preview, and third-party service reference in the review build has been checked for ownership, license, or permission.

APP_STORE_CONTENT_RIGHTS_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-05-28.

- Apple says App Store metadata, privacy information, descriptions, screenshots, and previews should accurately reflect the app's core experience and stay up to date with new versions.
- Apple says new features, functionality, and product changes must be described with specificity in the Notes for Review section and accessible for review.
- Apple says screenshots should show the app in use, not merely title art, a login page, or a splash screen.
- Apple says app previews may only use video screen captures of the app itself.
- Apple says screenshots and previews may use overlays to demonstrate interaction or extended functionality, but the developer is responsible for securing the rights to all materials in app icons, screenshots, and previews.
- Apple says screenshots and previews should use fictional account information instead of data from a real person.
- Apple says app names and keywords should be unique, accurate, and not packed with trademarked terms, popular app names, pricing information, or irrelevant phrases to game discovery.
- Apple says app names are limited to 30 characters.
- Apple says metadata, icons, screenshots, and previews should be appropriate for all audiences and should be similar enough to avoid confusion.
- Apple says app metadata should focus on the app itself and its Apple-platform experience, without names, icons, or imagery of other mobile platforms or alternative app marketplaces unless there is specific approved interactive functionality.
- Apple says apps should include only content the developer created or has a license to use.
- Apple says apps must not use protected third-party material, including trademarks, copyrighted works, or patented ideas, without permission.
- Apple says apps should be submitted by the person or legal entity that owns or has licensed the intellectual property and other relevant rights.
- Apple says apps that use or access third-party services must be permitted by the service's terms of use.
- Apple says authorization for third-party service usage must be available on request.
- Apple says apps must not falsely suggest association, sponsorship, or endorsement by Apple or another entity.
- Apple says app names, icons, screenshots, previews, and metadata should not be copycat or misleading.
- Apple says App Review notes should include supporting information when rights, permissions, or review access need explanation, and partnership documentation or authorization should be attached in App Store Connect when needed.

## Required Rights Review

| Area | Required Evidence | Result |
| --- | --- | --- |
| App name and brand | Wakeve name, icon, screenshots, and metadata do not create trademark confusion and are owned or licensed by the submitting entity. | Pending |
| App icon assets | `iosApp/src/Assets.xcassets/AppIcon.appiconset/*.png` are original or licensed and contain no third-party trademark, copyrighted artwork, Apple product imagery, or alpha channel. | Pending |
| Screenshots and previews | App Store screenshots/previews show the app experience, use fictional accounts/data, and use only owned or licensed visual material. | Pending |
| App preview videos | Any app previews use video screen captures of the app itself and only owned or licensed narration, overlays, and visual material. | Pending |
| Metadata text | Name, subtitle, keywords, descriptions, promotional text, and release notes do not use protected third-party trademarks to game search or imply endorsement. | Pending |
| Platform references | App Store metadata stays focused on Wakeve's Apple-platform experience and avoids unsupported references to other mobile platforms, alternative app marketplaces, or irrelevant platform imagery. | Pending |
| Provider names and links | Any Tricount, Google, Apple, maps, calendar, transport, payment, or lodging references are accurate, necessary, and permitted by terms or brand rules. | Pending |
| Third-party service content | The review build does not display, stream, download, monetize, or scrape third-party service content without permission. | Pending |
| Open-source notices | Third-party SDKs and libraries used in the app have license obligations reviewed in `docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md` and notices bundled or published where required. | Pending |
| Apple endorsement | App UI and metadata do not imply Apple endorsement, sponsorship, or that Apple supplies Wakeve. | Pending |
| App Review attachments | Any required rights, provider, or partnership authorization files are attached to the App Store review detail and referenced in Review Notes. | Pending |

## Apple References

- App metadata and screenshots must accurately reflect the app and avoid misleading claims: https://developer.apple.com/app-store/review/guidelines/
- App Store screenshots/previews require rights to all materials and fictional account data: https://developer.apple.com/app-store/review/guidelines/
- Apps must include only content created by the developer or licensed for use, and protected third-party material needs permission: https://developer.apple.com/app-store/review/guidelines/
- Third-party service content requires permission under the service terms, with authorization available on request: https://developer.apple.com/app-store/review/guidelines/
- App Review information and authorization attachments: https://developer.apple.com/app-store/review/
- App Store review attachments API: https://developer.apple.com/documentation/appstoreconnectapi/app-store-review-attachments

## Evidence Commands

Run or record equivalent review output:

```bash
./scripts/lint-store-metadata.sh --ios-only
find iosApp/src/Assets.xcassets/AppIcon.appiconset composeApp/screenshots/ios composeApp/metadata/ios -type f | sort
find iosApp/src/Assets.xcassets/AppIcon.appiconset -type f -name "*.png" -print | sort | shasum -a 256
find composeApp/metadata/ios composeApp/screenshots/ios -type f -name "*.png" -print | sort | shasum -a 256
find composeApp/metadata/ios composeApp/screenshots/ios -type f \( -name "*.mov" -o -name "*.mp4" -o -name "*.m4v" \) -print
rg -n "Android|Google Play|Play Store|alternative app marketplace" composeApp/metadata/ios
rg -n "Tricount|Google|Apple|Maps|Calendar|Zoom|Meet|Airbnb|Booking|Uber|Lyft|Stripe|PayPal|YouTube|Spotify|SoundCloud|Vimeo" iosApp/src shared/src composeApp/metadata/ios docs/APP_STORE_*.md
```

## Local Content Rights Scan Result

Local scan date: 2026-06-01

Result: local App Store-facing assets and metadata have been inventoried, but ownership/licensing is not yet signed off for the submitted review build.

- App icon inventory: `AppIcon.png`, `AppIconDark.png`, and `AppIconTinted.png` are PNG images at `1024 x 1024`, reported as RGB by `file`; local app-icon set hash: `9692f367663596a6a43793078d3886d064f04011ada5d4f1ea69000e2f1275bd`.
- Screenshot inventory: 8 PNG files across `composeApp/metadata/ios` and `composeApp/screenshots/ios`, covering `en-US` and `fr-FR` iPhone and iPad screenshots.
- Screenshot dimensions observed locally: iPhone screenshots are `1320 x 2868`; iPad screenshots are `2048 x 2732`.
- App preview videos: 0 `.mov`, `.mp4`, or `.m4v` files found under the iOS metadata and screenshot directories.
- Store metadata was reduced to avoid unnecessary platform trademark/design-system references in the product description text.
- Local metadata scan found no `Android`, `Google Play`, `Play Store`, or `alternative app marketplace` phrases under `composeApp/metadata/ios`.
- Local provider/reference scan still finds necessary product references in code or review evidence, including Sign in with Apple, Google Sign-In, Tricount handoff, FaceTime, Zoom, Google Meet, Microsoft Teams, Webex, Airbnb, Booking.com, Uber, Lyft, and BlaBlaCar.
- Local screenshot set hash: `e1d72a791111bc43b561e7b463043167e860a47f3c290443c0a015f64ef3effe`.
- The 2026-06-01 refresh confirms the app-icon hash and screenshot hash still match the repository paths checked by `scripts/lint-store-metadata.sh`.

This is local pre-submission evidence only. It does not prove the icon artwork, screenshots, provider references, or metadata text are owned, licensed, fictional, approved, or permitted by third-party terms for the uploaded App Store review build. The final reviewer must attach source/ownership records, screenshot capture provenance, visible-data fictional/approved confirmation, provider terms/brand-use decisions, App Review attachment IDs or a no-attachment decision, and license-notice evidence before setting `APP_STORE_CONTENT_RIGHTS_EVIDENCE_COMPLETE=true`.

## Evidence To Attach

Record these before final signoff:

- Owner or license source for app icon PNGs and any launch/store graphics.
- Screenshot capture source and confirmation that visible accounts, names, locations, messages, and budgets are fictional or approved.
- Metadata reviewer/date and confirmation that keywords/descriptions do not use third-party trademarks for ranking.
- Third-party provider list with terms/brand-permission decision for every provider name or link visible in the review build.
- Open-source license review output or notice path for shipped SDK/library obligations, backed by `docs/APP_STORE_LICENSE_INVENTORY_DRAFT.md` and `APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=true`.
- App Review notes decision and attachment IDs if any non-obvious third-party integration, partnership, or authorization requires explanation.

## Closure Rule

Set `APP_STORE_CONTENT_RIGHTS_EVIDENCE_COMPLETE=true` only after:

- Every review-build asset and App Store metadata item is owned by Wakeve or licensed for App Store use.
- Any third-party service or provider reference is permitted by the applicable terms/brand rules and does not imply endorsement.
- Screenshots/previews use fictional or approved data only.
- Required open-source notices are included where license obligations require them.
- Any rights, provider, or partnership authorization that App Review may need is either attached to the review detail or explicitly documented as not required.
- `docs/APP_STORE_LICENSE_NOTICES_EVIDENCE.md` contains `APP_STORE_LICENSE_NOTICES_EVIDENCE_COMPLETE=true`.
- `docs/APP_STORE_FINAL_SIGNOFF.md` references this completed evidence for the submitted review build.
