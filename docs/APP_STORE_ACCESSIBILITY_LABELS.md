# App Store Accessibility Labels Draft - Wakeve

Date: 2026-05-27

Apple's Accessibility Nutrition Labels can be entered in App Store Connect under **App Accessibility**. They are voluntary at the time of this audit, but Apple states they will appear on product pages on iOS 26, iPadOS 26, macOS 26, visionOS 26, tvOS 26, and watchOS 26 or later, and that support should be indicated per device type.

This is a conservative draft. Do not publish these labels until the TestFlight smoke tests in `docs/APP_STORE_LAUNCH_CHECKLIST.md` pass on each device family.

Evidence for the exact App Review build must be recorded in `docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md`. Do not set `APP_STORE_ACCESSIBILITY_SIGNOFF=true` until that file contains `APP_STORE_ACCESSIBILITY_EVIDENCE_COMPLETE=true`.

## Apple Source Baseline

Apple-source review date: 2026-05-27.

- Apple says Accessibility Nutrition Labels help users learn whether an app will be accessible before they download it.
- Apple says Accessibility Nutrition Labels appear on the App Store product page and are specific to the device type used to view the page.
- Apple says Accessibility Nutrition Labels appear on devices running iOS 26, iPadOS 26, macOS 26, tvOS 26, visionOS 26, and watchOS 26 or later.
- Apple says providing Accessibility Nutrition Labels is voluntary to start, but developers will over time be required to share accessibility support details to submit new apps and app updates.
- Apple says App Store Connect can show that support has not yet been indicated when accessibility information is not provided for a supported device.
- Apple says developers should review the evaluation criteria before indicating support for accessibility features.
- Apple says developers should identify common tasks and can create a testing matrix for each device before providing labels.
- Apple says common tasks include primary app functionality plus first launch, login, purchase, and settings.
- Apple says a user should be able to complete all common tasks using an accessibility feature before support for that feature is indicated.
- Apple says some accessibility labels are not available on some devices, so App Store Connect prompts only for applicable features.
- Apple says Accessibility Nutrition Label responses are saved as a draft until published.
- Apple says published accessibility information appears immediately on the product page, may take up to 24 hours to be visible to all users, and cannot be unpublished.
- Apple says accessibility responses can be updated at any time.
- Apple says the optional accessibility URL appears on the product page across all devices except Apple TV.
- Apple says App Review may ask developers to update intentionally misleading or harmful Accessibility Nutrition Labels.
- Apple says developers are responsible for keeping accessibility responses accurate and up to date.
- Apple says iPhone and iPad apps are available on Apple Vision Pro unless availability is edited in App Store Connect, so Vision Pro availability must match accessibility label choices.
- Apple says iPhone and iPad apps can be available on Macs with Apple silicon unless availability is edited in App Store Connect, so Mac availability must match accessibility label choices.

## Device Scope

The current iOS target builds as a universal iPhone/iPad app:

- `TARGETED_DEVICE_FAMILY = 1,2`
- `SUPPORTS_MAC_DESIGNED_FOR_IPHONE_IPAD = NO`
- `SUPPORTS_XR_DESIGNED_FOR_IPHONE_IPAD = NO`

The repository decision for the first release is iPhone/iPad only. Mac with Apple silicon and Apple Vision Pro compatibility are disabled in the Xcode project and must be confirmed disabled in App Store Connect before review. If either platform is re-enabled later, do not publish accessibility support for that device family until runtime-specific smoke tests and accessibility evidence are recorded.

## Recommended App Store Connect Answers

### iPhone

| Feature | Draft answer | Evidence / caveat |
|---|---:|---|
| Dark Interface | Yes | iOS views use SwiftUI color system and Liquid Glass components with dark-mode support. Must verify in TestFlight. |
| Larger Text / Adjustable Text Size | Do not claim yet | Existing audits still flag Dynamic Type risks in several views. Claim only after large accessibility sizes are smoke tested. |
| VoiceOver | Do not claim yet | Existing audits show partial coverage and remaining unlabeled controls. Claim only after VoiceOver smoke test covers login, event creation, polling, calendar, and settings. |
| Voice Control | Do not claim yet | No current evidence of device-level Voice Control testing. |
| Sufficient Contrast | Do not claim yet | Existing audits identify historical contrast issues in gradient/secondary text areas. Claim only after current screens are checked. |
| Reduced Motion | Do not claim yet | No current evidence of Reduced Motion testing across primary flows. |
| Differentiate without Color Alone | Do not claim yet | Poll/vote/status flows need manual verification. |
| Captions | Not applicable | No required video/audio media content in the App Store experience currently documented. |
| Audio Descriptions | Not applicable | No video content requiring audio descriptions currently documented. |

### iPad

Use the same answers as iPhone after completing the iPad TestFlight smoke test. Do not publish iPad accessibility labels until iPad layout, Dynamic Type, VoiceOver, and pointer/keyboard navigation have been checked.

### Mac with Apple Silicon

Do not claim accessibility support for Mac with Apple silicon in the first release because the repository release settings disable Designed for iPhone/iPad on Mac. If the platform is re-enabled later, do not claim accessibility support until the Designed for iPad/iPhone Mac runtime is tested. If the app becomes available on Mac, verify at minimum:

- Keyboard navigation through login, event creation, poll voting, and settings.
- VoiceOver on macOS can identify all primary controls.
- Window resizing does not hide primary actions.
- Calendar, notification, and deep link flows behave acceptably on macOS.

### Apple Vision Pro

Do not claim accessibility support for Apple Vision Pro in the first release because the repository release settings disable iPhone/iPad app compatibility on Apple Vision Pro. If the platform is re-enabled later, do not claim accessibility support until the iPhone/iPad app compatibility runtime is tested on Apple Vision Pro or in an approved equivalent test environment. If the app is not tested, opt out of Apple Vision Pro availability in App Store Connect for the initial release.

## Before Publishing Labels

- Re-run or update `docs/ACCESSIBILITY_AUDIT.md` and `docs/a11y/ACCESSIBILITY_AUDIT_iOS.md` against the current SwiftUI files.
- Complete the accessibility section of `docs/APP_STORE_LAUNCH_CHECKLIST.md`.
- Keep screenshots or notes from VoiceOver, Dynamic Type, dark mode, and contrast checks.
- Publish only labels that are directly supported by current device evidence.
- Record the final publication decision and device evidence in `docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md` before setting `APP_STORE_ACCESSIBILITY_SIGNOFF=true`.

## Apple References

- Manage Accessibility Nutrition Labels: https://developer.apple.com/help/app-store-connect/manage-app-accessibility/manage-accessibility-nutrition-labels
- Overview of Accessibility Nutrition Labels: https://developer.apple.com/help/app-store-connect/manage-app-accessibility/overview-of-accessibility-nutrition-labels/
- Manage availability of iPhone and iPad apps on Apple Vision Pro: https://developer.apple.com/help/app-store-connect/manage-your-apps-availability/manage-availability-of-iphone-and-ipad-apps-on-apple-vision-pro
