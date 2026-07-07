# App Store Privacy Evidence - Wakeve

Date: 2026-06-13

Status: PENDING

Do not change the marker below until the exact App Review build, App Store Connect privacy labels, live privacy policy, and bundled privacy manifest have been compared and approved.

APP_STORE_PRIVACY_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-05-27.

- Apple says App Store privacy details are required to submit new apps and app updates.
- Apple says App Store Connect privacy responses must include the privacy practices of third-party partners whose code is integrated into the app.
- Apple says developers are responsible for keeping privacy responses accurate and up to date when practices change.
- Apple defines collection as transmitting data off device in a way that allows the developer or third-party partners to access it longer than needed to service the request in real time.
- Apple says data processed only on device is not collected, but derived data sent off device must be evaluated separately.
- Apple says users must be told whether each collected data type is linked to their identity and whether data is used to track them.
- Apple requires a publicly accessible Privacy Policy URL on the App Store product page.

## Build And Policy Scope

- App Store Connect version: TBD
- Build number: TBD
- Release commit: TBD
- Live privacy URL: `https://wakeve.app/privacy`
- Privacy labels draft: `docs/APP_STORE_PRIVACY_LABELS.md`
- Privacy policy source: `docs/PRIVACY_POLICY.md`
- Privacy manifest source: `iosApp/src/PrivacyInfo.xcprivacy`
- Bundled privacy manifest check: `build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app/PrivacyInfo.xcprivacy`

## Required App Store Connect Answers

| Area | Required Evidence | Result | Notes |
| --- | --- | --- | --- |
| Tracking | App Store Connect says no tracking; `NSPrivacyTracking=false`; no tracking domains; no IDFA/App Tracking Transparency strings in Release binaries. | Pending | TBD |
| Linked data | App Store Connect labels include name, email address, user ID, device ID, other user content, coarse location, and product interaction when those paths remain enabled. | Pending | TBD |
| Not collected data | Contacts, browsing history, search history, financial info, health and fitness, sensitive info, purchases, and advertising data remain not collected unless a production path is added. | Pending | TBD |
| Photos/media | Confirm whether selected photos/media are uploaded. If uploaded, App Store Connect labels and privacy policy must include Photos or Videos. | Pending | TBD |
| Calendar | Confirm calendar operations write only and do not collect or transmit existing calendar data. | Pending | TBD |
| Siri/speech | Confirm Siri/speech features process locally or update labels/policy if audio or transcripts are transmitted. | Pending | TBD |
| Analytics/crash | Confirm whether analytics/crash providers are enabled in the review build and whether data is linked to identity. | Pending | TBD |
| Legal approval | Privacy/legal owner approves the App Store Connect answers and live privacy policy for the review build. | Pending | TBD |

## Evidence Commands

Run these before setting `APP_STORE_PRIVACY_SIGNOFF=true`:

```bash
./scripts/lint-store-metadata.sh --ios-only
./scripts/audit-app-store-privacy-alignment.sh --fail-on-findings
APP_REVIEW_PHONE_NUMBER=<APP_REVIEW_PHONE_NUMBER> ./scripts/lint-store-metadata.sh --ios-only
plutil -p iosApp/src/PrivacyInfo.xcprivacy | rg -n "NSPrivacyCollectedDataType(Name|EmailAddress|UserID|DeviceID|OtherUserContent|CoarseLocation|ProductInteraction)|NSPrivacyTracking|NSPrivacyTrackingDomains"
plutil -p build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app/PrivacyInfo.xcprivacy | rg -n "NSPrivacyCollectedDataType(Name|EmailAddress|UserID|DeviceID|OtherUserContent|CoarseLocation|ProductInteraction)|NSPrivacyTracking"
/usr/bin/strings build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app/Wakeve build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app/Frameworks/Shared.framework/Shared | rg -n "IDFA|IdentifierForAdvertising|ATTrackingManager|NSUserTrackingUsageDescription"
```

## Local Privacy Alignment Scan Result

Local scan date: 2026-06-13.

Generated audit report: `docs/app-store-privacy/privacy-alignment-2026-06-13T12-26-10Z.md`.

Result: `PASS for local privacy alignment. AS-05 remains open for App Store Connect, live URL, legal/privacy owner, and uploaded-build evidence.`

The standalone privacy audit passed with 0 local findings and 4 external pending confirmations:

- `iosApp/src/PrivacyInfo.xcprivacy` SHA-256: `38dbda46a737beed9c54a65cf089159fbb2712de1c21b8c9cd5de6877acfbfc3`.
- `docs/APP_STORE_PRIVACY_LABELS.md` SHA-256: `6b8817f3013c36f1ef60b3d1d67d4aa8071aba02d36224cae6aa8e01438cd638`.
- `docs/PRIVACY_POLICY.md` SHA-256: `8eb134c37318846c8c3ffbac075ee76d606204f44a17a1618e94b8c6f078b285`.
- Privacy manifest declares `NSPrivacyTracking=false` and no tracking domains.
- Privacy-label draft declares no tracking.
- `iosApp/src/Info.plist` does not declare `NSUserTrackingUsageDescription`.
- iOS/shared source contains no IDFA or App Tracking Transparency API references.
- Privacy manifest and privacy-label draft both cover `NSPrivacyCollectedDataTypeName`, `NSPrivacyCollectedDataTypeEmailAddress`, `NSPrivacyCollectedDataTypeUserID`, `NSPrivacyCollectedDataTypeDeviceID`, `NSPrivacyCollectedDataTypeOtherUserContent`, `NSPrivacyCollectedDataTypeCoarseLocation`, and `NSPrivacyCollectedDataTypeProductInteraction`.
- Privacy-label draft explicitly lists Contacts, Browsing History, Search History, Financial Info, Health and Fitness, Sensitive Info, Purchases, and Advertising Data under data not collected.
- Privacy policy source includes `privacy@wakeve.app` and Data Collection wording.
- External pending confirmations remain: App Store Connect privacy labels must be compared against the draft, `https://wakeve.app/privacy` must be live and match `docs/PRIVACY_POLICY.md`, the uploaded review build must be checked for bundled `PrivacyInfo.xcprivacy` and no tracking strings, and photos/media, calendar data, Siri/speech, analytics, and crash behavior need final owner confirmation.

Record the output or attach screenshots/exports showing:

- App Store Connect privacy labels match `docs/APP_STORE_PRIVACY_LABELS.md`.
- Live `https://wakeve.app/privacy` matches `docs/PRIVACY_POLICY.md`.
- The bundled privacy manifest matches `iosApp/src/PrivacyInfo.xcprivacy`.
- Release binaries contain no IDFA/App Tracking Transparency usage when tracking is declared as no.
- Legal/privacy owner approval date and reviewer.

## Closure Rule

Set `APP_STORE_PRIVACY_EVIDENCE_COMPLETE=true` only after:

- App Store Connect privacy labels, privacy manifest, live privacy policy, and review build behavior match.
- Open verification questions in `docs/APP_STORE_PRIVACY_LABELS.md` are resolved for photos/media, calendar data, Siri/speech, analytics, and crash reporting.
- Legal/privacy owner approval is recorded with date and reviewer.
- `APP_STORE_PRIVACY_SIGNOFF=true` is set only in the final release shell or CI secret store after this file is updated.
