# App Store Availability Evidence - Wakeve

Date: 2026-06-01

Status: PENDING

Do not change the marker below until the exact App Store Connect device-family availability decision has been recorded for the review build.

APP_STORE_AVAILABILITY_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-05-27.

- Apple says iPhone and iPad apps can be accessed on Macs with Apple silicon unless app availability is edited in App Store Connect.
- Apple says iPhone and iPad apps are available on Apple Vision Pro unless availability is edited in App Store Connect.
- Apple says Mac Apple silicon compatibility uses the same frameworks, resources, and runtime environments as iOS and iPadOS.
- Apple says Apple Vision Pro compatibility runs natively using the same frameworks, resources, and runtime environment as iOS and iPadOS.
- Apple says developers can choose to make iPhone and iPad apps available or not available on Mac Apple silicon and Apple Vision Pro.
- Apple says the Mac Apple silicon and Apple Vision Pro availability choices are set at the app level and apply to all versions of the app.
- Apple says Mac Apple silicon availability is managed in Pricing and Availability under the iPhone and iPad Apps on Apple Silicon Mac section.
- Apple says Apple Vision Pro availability is managed in Pricing and Availability under the iPhone and iPad Apps on Apple Vision Pro section.
- Apple says the required role for editing these availability choices is Account Holder, Admin, or App Manager.
- Apple says Mac Apple silicon compatibility can be verified in App Store Connect when a compatible app functions as intended.
- Apple says compatibility with Apple Silicon Macs is not available if a build has never been uploaded for the platform.
- Apple says apps that have been in the Kids category cannot be made available on visionOS.

## Build And Store Scope

- App Store Connect version: TBD
- Build number: TBD
- Release commit: TBD
- Availability decision source: `docs/APP_STORE_AVAILABILITY_DECISIONS.md`
- Accessibility evidence source: `docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md`
- TestFlight evidence source: `docs/APP_STORE_TESTFLIGHT_EVIDENCE.md`
- App Store Connect decision: iPhone and iPad enabled; Mac Apple silicon and Apple Vision Pro disabled for first release.

## Current Xcode Settings

| Setting | Expected Repository Evidence | App Store Connect Decision | Result |
| --- | --- | --- | --- |
| iPhone/iPad | `TARGETED_DEVICE_FAMILY = 1,2` | Enabled for first release. | Pending |
| Mac Apple silicon | `SUPPORTS_MAC_DESIGNED_FOR_IPHONE_IPAD = NO` | Disabled for first release. | Repository setting updated on 2026-06-01; App Store Connect switch still pending confirmation. |
| Apple Vision Pro | `SUPPORTS_XR_DESIGNED_FOR_IPHONE_IPAD = NO` | Disabled for first release. | Repository setting updated on 2026-06-01; App Store Connect switch still pending confirmation. |
| Accessibility Labels | `docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md` | Device-family labels match selected availability. | Pending |

## Evidence Commands

Run these before setting `APP_STORE_AVAILABILITY_CONFIRMED=true`:

```bash
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme WakeveApp \
  -configuration Release \
  -showBuildSettings | rg "TARGETED_DEVICE_FAMILY|SUPPORTS_MAC_DESIGNED_FOR_IPHONE_IPAD|SUPPORTS_XR_DESIGNED_FOR_IPHONE_IPAD|IPHONEOS_DEPLOYMENT_TARGET"

./scripts/lint-store-metadata.sh --ios-only
```

Record the output or attach screenshots/exports showing:

- App Store Connect iPhone and iPad availability is enabled for the selected launch storefronts.
- Mac Apple silicon availability is disabled for the first release in Xcode and must be confirmed disabled in App Store Connect.
- Apple Vision Pro availability is disabled for the first release in Xcode and must be confirmed disabled in App Store Connect.
- Accessibility Nutrition Labels match the selected device-family availability.
- EU storefront availability, if changed for DSA reasons, does not contradict `docs/APP_STORE_DSA_TRADER_STATUS.md`.

## Closure Rule

Set `APP_STORE_AVAILABILITY_EVIDENCE_COMPLETE=true` only after:

- App Store Connect availability for iPhone, iPad, Mac Apple silicon, and Apple Vision Pro matches `docs/APP_STORE_AVAILABILITY_DECISIONS.md`.
- Any enabled Mac Apple silicon or Apple Vision Pro compatibility target is backed by current smoke-test evidence.
- Accessibility Nutrition Labels match the chosen device-family availability.
- `APP_STORE_AVAILABILITY_CONFIRMED=true` is set only in the final release shell or CI secret store after this file is updated.
