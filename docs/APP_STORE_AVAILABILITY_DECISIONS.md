# App Store Availability Decisions - Wakeve

Date: 2026-06-01

This file records App Store Connect availability decisions that cannot be fully enforced from the repository alone.

Evidence for the exact App Review build must be recorded in `docs/APP_STORE_AVAILABILITY_EVIDENCE.md`. Do not set `APP_STORE_AVAILABILITY_CONFIRMED=true` until that file contains `APP_STORE_AVAILABILITY_EVIDENCE_COMPLETE=true`.

## Apple Source Baseline

Apple-source review date: 2026-05-27.

- Apple says app availability must be set before submitting an app for App Store review.
- Apple says apps can be released in any of the 175 countries or regions where the App Store is available.
- Apple says availability is managed in App Store Connect under Pricing and Availability.
- Apple says a customer's Apple Account country or region determines the App Store country or region where they can purchase or download apps.
- Apple says developers can choose All Countries or Regions, Specific Countries or Regions, or Publish as Pre-Order when setting app availability.
- Apple says deselecting a country or region removes the app from the App Store there, while previous purchasers can continue receiving updates and redownloads if the required contract remains active.
- Apple says availability changes can take effect immediately but may require up to 24 hours to be visible to all users.
- Apple says iPhone and iPad apps can be accessed on Macs with Apple silicon unless app availability is edited in App Store Connect.
- Apple says iPhone and iPad apps are available on Apple Vision Pro unless availability is edited in App Store Connect.
- Apple says Mac Apple silicon compatibility uses the same frameworks, resources, and runtime environments as iOS and iPadOS.
- Apple says Apple Vision Pro compatibility uses the same frameworks, resources, and runtime environment as iOS and iPadOS.
- Apple says Mac Apple silicon and Apple Vision Pro availability choices are set at the app level and apply to all versions of the app.
- Apple says Mac Apple silicon availability is managed in Pricing and Availability under the iPhone and iPad Apps on Apple Silicon Mac section.
- Apple says Apple Vision Pro availability is managed in Pricing and Availability under the iPhone and iPad Apps on Apple Vision Pro section.
- Apple says the required role for editing app availability, Mac Apple silicon availability, and Apple Vision Pro availability is Account Holder, Admin, or App Manager.
- Apple says Mac Apple silicon compatibility can be verified in App Store Connect when a compatible app functions as intended.
- Apple says compatibility with Apple Silicon Macs is not available if a build has never been uploaded for the platform.
- Apple says apps that have been in the Kids category cannot be made available on visionOS.

## Current Build Settings

Current Release build settings report:

- Bundle ID: `com.guyghost.wakeve`
- iOS deployment target: `18.2`
- Device families: iPhone and iPad (`TARGETED_DEVICE_FAMILY = 1,2`)
- Designed for iPhone/iPad on Mac: disabled for first release (`SUPPORTS_MAC_DESIGNED_FOR_IPHONE_IPAD = NO`)
- Designed for iPhone/iPad on Apple Vision Pro: disabled for first release (`SUPPORTS_XR_DESIGNED_FOR_IPHONE_IPAD = NO`)

## Initial Release Recommendation

Submit Wakeve first for iPhone and iPad only. Mac Apple silicon and Apple Vision Pro are disabled in the Xcode project for the first App Store review build unless a future release adds device-family smoke tests and accessibility evidence.

Rationale:

- The iOS metadata and screenshots currently cover iPhone and iPad.
- The TestFlight checklist currently targets iPhone and iPad.
- Existing accessibility audits are iOS/iPadOS-focused and do not prove Mac or Vision Pro accessibility support.
- Apple says iPhone/iPad apps can be available on Mac Apple silicon and Apple Vision Pro unless availability is edited in App Store Connect, so the release owner must still confirm the App Store Connect availability switches match the repository decision.

## App Store Connect Actions

Before App Review:

- Confirm iPhone and iPad availability.
- Confirm launch countries or regions, and record whether the release uses All Countries or Regions, Specific Countries or Regions, or Publish as Pre-Order.
- Confirm Mac Apple silicon availability is disabled for the first release.
- Confirm Apple Vision Pro availability is disabled for the first release.
- If Mac or Vision Pro is re-enabled in a future release, add device-specific smoke tests to `docs/APP_STORE_LAUNCH_CHECKLIST.md` and do not publish accessibility labels for those device families until tested.

## Required Evidence

Record the final evidence in `docs/APP_STORE_AVAILABILITY_EVIDENCE.md` and `docs/APP_STORE_FINAL_SIGNOFF.md` before setting `APP_STORE_AVAILABILITY_CONFIRMED=true`:

- App Store Connect iPhone and iPad availability is enabled for the selected launch storefronts.
- App Store Connect countries or regions availability is configured before App Review submission.
- Mac Apple silicon availability is disabled for the first release.
- Apple Vision Pro availability is disabled for the first release.
- Accessibility Nutrition Labels match the selected device-family availability.
- Reviewer/date and App Store Connect screenshot or export reference.

## Verification Commands

```bash
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme WakeveApp \
  -configuration Release \
  -showBuildSettings | rg "TARGETED_DEVICE_FAMILY|SUPPORTS_MAC_DESIGNED_FOR_IPHONE_IPAD|SUPPORTS_XR_DESIGNED_FOR_IPHONE_IPAD|IPHONEOS_DEPLOYMENT_TARGET"
```

## Apple Reference

- Manage availability of iPhone and iPad apps on Apple Vision Pro: https://developer.apple.com/help/app-store-connect/manage-your-apps-availability/manage-availability-of-iphone-and-ipad-apps-on-apple-vision-pro
- Manage availability of iPhone and iPad apps on Macs with Apple silicon: https://developer.apple.com/help/app-store-connect/manage-your-apps-availability/manage-availability-of-iphone-and-ipad-apps-on-macs-with-apple-silicon
