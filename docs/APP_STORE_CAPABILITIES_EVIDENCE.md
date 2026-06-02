# App Store Capabilities Evidence - Wakeve

Date: 2026-06-01

Status: PENDING

Do not change the marker below until the signed IPA intended for App Review has been inspected and the Apple Developer App ID plus Release provisioning profile have the required capabilities enabled.

APP_STORE_CAPABILITIES_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-05-28.

- Apple says an App ID identifies the app in a provisioning profile and can be explicit for one app or wildcard for a set of apps.
- Apple says app capabilities enabled for an App ID serve as an allow list of the capabilities one or more apps may use.
- Apple says configuring capabilities that an app uses also requires adding them to a target in the Xcode project.
- Apple says Xcode edits the needed entitlements and Information Property List files for capabilities, including relevant frameworks and signing assets.
- Apple says capabilities can also be manually configured for apps and websites within the Identifiers section of Certificates, Identifiers & Profiles.
- Apple says enabling app capabilities requires the Account Holder or Admin role.
- Apple says provisioning profiles that contain a modified App ID become invalid and must be regenerated.
- Apple says enabling a capability can affect provisioning profiles for all eligible platforms.
- Apple says Sign in with Apple, App groups, Apple Pay, Data protection, iCloud, and push notifications require additional steps.
- Apple says uploading to App Store Connect requires an app record registered with an explicit App ID.
- Apple says an App Store Connect provisioning profile uses the explicit App ID that matches the bundle ID.
- Apple says an App Store Connect provisioning profile is created for the App ID that matches the bundle ID and contains a single distribution certificate.
- Apple says automatically managed signing can let Xcode manage distribution provisioning profiles during upload.
- Apple says provisioning profiles authorize the app to use certain app services and contain a single App ID plus a distribution certificate.
- Apple says managed capabilities require approval from Apple before they can be used.
- Apple says approved managed capabilities are automatically included in eligible provisioning profiles after the App ID configuration is updated and a profile is created.
- Apple says Sign in with Apple starts by enabling the app's App ID with the Sign in with Apple capability.
- Apple says new Sign in with Apple App IDs should be enabled as primary, while related apps can be grouped with an existing primary App ID.
- Apple says managed capabilities can be enabled only after Apple assigns the entitlement to the account, either through Xcode or Certificates, Identifiers & Profiles.

## App Identifier

- Bundle ID: `com.guyghost.wakeve`
- Apple Team ID: TBD
- App Store Connect version: TBD
- Build number: TBD
- Release commit: TBD

## Required Capabilities

| Capability | Repository Evidence | Apple Developer Evidence | Signed IPA Evidence | Result |
| --- | --- | --- | --- | --- |
| Push Notifications | `aps-environment` in `iosApp/src/Wakeve.entitlements` uses `$(APS_ENVIRONMENT)` and Release sets production APNs. | App ID has Push Notifications enabled and Release provisioning profile was regenerated after enabling it. | Signed IPA entitlement `aps-environment=production`. | Pending |
| Siri | `com.apple.developer.siri=true` in `iosApp/src/Wakeve.entitlements`; `NSSiriUsageDescription` and Siri intents are present in `iosApp/src/Info.plist`. | App ID has Siri enabled and Release provisioning profile was regenerated after enabling it. | Signed IPA entitlement `com.apple.developer.siri=true`. | Pending |
| Sign in with Apple | `com.apple.developer.applesignin` includes `Default` in `iosApp/src/Wakeve.entitlements`; iOS login exposes Sign in with Apple. | App ID has Sign in with Apple enabled and Release provisioning profile was regenerated after enabling it. | Signed IPA entitlement `com.apple.developer.applesignin` includes `Default`. | Pending |
| Associated Domains | `com.apple.developer.associated-domains` includes `applinks:wakeve.app` in `iosApp/src/Wakeve.entitlements`. | App ID has Associated Domains enabled and Release provisioning profile was regenerated after enabling it. | Signed IPA entitlement includes `applinks:wakeve.app`. | Pending |

## Local Unsigned Capability Scan Result

This section records repository and local Release build-setting evidence only. It does not satisfy the signed IPA, Apple Developer App ID, or Release provisioning profile evidence required for final App Store submission.

Commands refreshed locally on 2026-06-01:

```bash
plutil -p iosApp/src/Wakeve.entitlements
plutil -p iosApp/src/Info.plist | rg 'NSSiriUsageDescription|INIntentsSupported|CFBundleURLTypes|UIBackgroundModes' -A8
xcodebuild -project iosApp/iosApp.xcodeproj -scheme WakeveApp -configuration Release -destination 'generic/platform=iOS' -showBuildSettings | rg -n "CODE_SIGN_ENTITLEMENTS|APS_ENVIRONMENT|DEVELOPMENT_TEAM|PRODUCT_BUNDLE_IDENTIFIER|PROVISIONING_PROFILE|CODE_SIGN_STYLE|CODE_SIGN_IDENTITY"
codesign -d --entitlements :- build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app
```

Observed source entitlements:

- `aps-environment` is `$(APS_ENVIRONMENT)` in `iosApp/src/Wakeve.entitlements`.
- `com.apple.developer.siri=true` is declared in `iosApp/src/Wakeve.entitlements`.
- `com.apple.developer.applesignin` includes `Default` in `iosApp/src/Wakeve.entitlements`.
- `com.apple.developer.associated-domains` includes `applinks:wakeve.app` in `iosApp/src/Wakeve.entitlements`.
- `iosApp/src/Info.plist` includes Siri intent declarations, `NSSiriUsageDescription`, `UIBackgroundModes = remote-notification`, and the `wakeve` URL scheme used by local deep links.

Observed Release build settings:

- `APS_ENVIRONMENT = production`
- `CODE_SIGN_ENTITLEMENTS = src/Wakeve.entitlements`
- `CODE_SIGN_STYLE = Automatic`
- `CODE_SIGN_IDENTITY = Apple Development`
- `DEVELOPMENT_TEAM = ${TEAM_ID}` is configured in `iosApp/iosApp.xcodeproj/project.pbxproj`; no concrete Apple Team ID is stored in the repository.
- `PRODUCT_BUNDLE_IDENTIFIER = com.guyghost.wakeve`
- `PROVISIONING_PROFILE_REQUIRED = YES`
- `iosApp/iosApp.xcodeproj/project.pbxproj` still contains `DEVELOPMENT_TEAM = "${TEAM_ID}"`, so the concrete Apple Team ID remains supplied by the release environment rather than source control.

Unsigned limitation:

- The local Release app at `build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app` was built with `CODE_SIGNING_ALLOWED=NO`.
- `codesign -d --entitlements :- build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app` reports `code object is not signed at all`.
- The 2026-06-01 refresh confirms this unsigned limitation is still current for the local Release product.
- Because the local bundle is unsigned, it cannot prove `application-identifier`, `com.apple.developer.team-identifier`, embedded signed entitlements, App ID capability switches, or Release provisioning profile regeneration.
- Final closure still requires `bundle exec fastlane ios validate_ipa_entitlements ipa:build/ios/WakeveApp.ipa` against the signed App Review build and Apple Developer portal capability/profile evidence.

## Required Commands

Run these against the signed build intended for App Review:

```bash
TEAM_ID=<APPLE_TEAM_ID> bundle exec fastlane ios validate_ipa_entitlements ipa:build/ios/WakeveApp.ipa
```

The Fastlane validation checks:

- `application-identifier` equals `<TEAM_ID>.com.guyghost.wakeve`
- `com.apple.developer.team-identifier` equals `<TEAM_ID>`
- `aps-environment` equals `production`
- `com.apple.developer.siri` equals `true`
- `com.apple.developer.applesignin` includes `Default`
- `com.apple.developer.associated-domains` includes `applinks:wakeve.app`

## Evidence To Attach

- Command output from `bundle exec fastlane ios validate_ipa_entitlements ipa:build/ios/WakeveApp.ipa`.
- App ID capability screenshot or export from Apple Developer showing Push Notifications, Siri, Sign in with Apple, and Associated Domains.
- Sign in with Apple configuration screenshot showing whether the App ID is primary or grouped with the approved primary App ID.
- Release provisioning profile identifier/name and regeneration date after the capabilities were enabled.
- Distribution certificate identifier used by the App Store Connect provisioning profile.
- App Store Connect uploaded build number matching the signed IPA inspected above.

## Closure Rule

Set `APP_STORE_CAPABILITIES_EVIDENCE_COMPLETE=true` only after:

- The signed IPA entitlement validation command exits 0 for the App Review build.
- The Apple Developer App ID has Push Notifications, Siri, Sign in with Apple, and Associated Domains enabled.
- The Apple Developer App ID Sign in with Apple configuration is recorded as primary or grouped with the approved primary App ID.
- The Release provisioning profile was regenerated/refreshed after enabling those capabilities.
- The Release provisioning profile uses the explicit App ID matching `com.guyghost.wakeve` and exactly one distribution certificate.
- The App Store Connect uploaded build number matches the inspected IPA.
- `APP_STORE_CAPABILITIES_CONFIRMED=true` is set only in the final release shell or CI secret store after this file is updated.
