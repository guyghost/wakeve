# App Store SDK Privacy And Signature Evidence - Wakeve

Date: 2026-06-01

Status: PENDING

Do not change the marker below until the exact signed review archive has been inspected for third-party SDK privacy manifests, SDK signatures, and privacy-report alignment.

APP_STORE_SDK_PRIVACY_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-05-28.

- Apple says when developers use a third-party SDK, they are responsible for all code the SDK includes in the app and must understand its data collection and use practices.
- Apple says privacy manifest files outline the privacy practices of third-party code in a standard format.
- Apple says Xcode combines privacy manifests across all third-party SDKs used by the app into one report to help create accurate Privacy Nutrition Labels.
- Apple says SDK signatures let Xcode validate that a newly adopted SDK version was signed by the same developer, improving software supply-chain integrity.
- Apple third-party SDK requirements say listed SDKs included in new apps or app updates submitted through App Store Connect must include privacy manifests.
- Apple requires signatures when those listed SDKs are used as binary dependencies.
- Apple says any version of a listed SDK, and SDKs that repackage listed SDKs, are included in the requirement.
- Apple's privacy manifest guidance says apps submitted for review must contain a valid privacy manifest for commonly used third-party SDKs when those SDKs are included.
- Wakeve's final AS-18 closure must therefore inspect the signed `.xcarchive` or IPA, not just source files or the unsigned local Release build.

## Required SDK Review

| Area | Required Evidence | Result |
| --- | --- | --- |
| SDK inventory | Every embedded framework, Swift package, CocoaPod, XCFramework, static framework, and Kotlin/Native framework in the signed archive is listed. | Pending |
| Apple listed SDKs | Any SDK from Apple's third-party SDK requirements list, including Firebase, GoogleSignIn, GoogleUtilities, AppAuth, BoringSSL, OpenSSL, Protobuf, nanopb, Alamofire, Kingfisher, Lottie, OneSignal, RealmSwift, SDWebImage, RxSwift, SnapKit, Starscream, or similar repackaged SDKs, is identified. | Pending |
| Repackaged listed SDKs | Dependency names, binary symbols, and vendor notes are checked for SDKs that repackage Apple-listed SDKs. | Pending |
| Privacy manifests | The app manifest and every listed third-party SDK manifest required by Apple are present and valid in the archive. | Pending |
| SDK signatures | Any listed SDK used as a binary dependency has a valid SDK signature from the expected SDK developer, or the absence of listed binary SDKs is documented. | Pending |
| Xcode privacy report | The Xcode privacy report for the signed archive has been generated and compared with `docs/APP_STORE_PRIVACY_LABELS.md` and `docs/APP_STORE_PRIVACY_EVIDENCE.md`. | Pending |
| Required reason APIs | Required reason API declarations from app and SDK manifests match the app's actual file timestamp/UserDefaults usage and do not hide SDK behavior. | Pending |
| Invalid manifest handling | No invalid third-party privacy manifest warning or App Store Connect email is unresolved. | Pending |

## Apple References

- Apple requires privacy manifests for listed third-party SDKs and signatures when those listed SDKs are used as binary dependencies: https://developer.apple.com/support/third-party-SDK-requirements/
- App privacy manifests describe data collection and required reason API usage for apps and third-party SDKs: https://developer.apple.com/documentation/bundleresources/privacy-manifest-files
- App Store Connect rejects invalid privacy manifests in submitted apps: https://developer.apple.com/documentation/bundleresources/adding-a-privacy-manifest-to-your-app-or-third-party-sdk
- Xcode can summarize app and SDK privacy manifests into a privacy report used to complete App Store privacy details: https://developer.apple.com/documentation/BundleResources/describing-data-use-in-privacy-manifests

## Evidence Commands

Run or record equivalent output for the signed review archive:

```bash
find iosApp build -name 'PrivacyInfo.xcprivacy' -o -name '*.framework' -o -name '*.xcframework' | sort
plutil -lint iosApp/src/PrivacyInfo.xcprivacy
plutil -p iosApp/src/PrivacyInfo.xcprivacy | rg -n "NSPrivacyCollectedDataTypes|NSPrivacyTracking|NSPrivacyAccessedAPITypes"
rg -n "Abseil|AFNetworking|Alamofire|AppAuth|BoringSSL|OpenSSL|Firebase|GoogleSignIn|GoogleUtilities|GoogleDataTransport|GTMAppAuth|GTMSessionFetcher|Kingfisher|Lottie|nanopb|OneSignal|Promises|Protobuf|RealmSwift|RxSwift|SDWebImage|SnapKit|Starscream|SwiftyJSON" iosApp shared fastlane docs
./scripts/lint-store-metadata.sh --ios-only
```

## Local Unsigned Release Scan Result

Local scan date: 2026-06-01

Result: the current local Release build has a small embedded SDK surface, but this is pre-submission evidence only because the signed archive, SDK signatures, and Xcode privacy report have not been produced.

- Local Release app scanned: `build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app`.
- Embedded app frameworks found: 1, `Frameworks/Shared.framework`.
- Bundled privacy manifests found: 1, `Wakeve.app/PrivacyInfo.xcprivacy`.
- Built privacy manifest hash: `38dbda46a737beed9c54a65cf089159fbb2712de1c21b8c9cd5de6877acfbfc3`.
- Source privacy manifest hash: `38dbda46a737beed9c54a65cf089159fbb2712de1c21b8c9cd5de6877acfbfc3`.
- `plutil -lint` passes for both `iosApp/src/PrivacyInfo.xcprivacy` and the built app's `PrivacyInfo.xcprivacy`.
- Built privacy manifest declares `NSPrivacyTracking=false`, an empty `NSPrivacyTrackingDomains` array, collected data types matching the current App Store privacy-label draft, and required-reason API declarations for file timestamps (`C617.1`) and UserDefaults (`CA92.1`).
- `otool -L` for the app binary and `Shared.framework/Shared` shows Apple system frameworks, Swift runtime libraries, system dylibs, and the local Kotlin/Native `Shared.framework`; it does not show third-party binary SDK frameworks such as Firebase, GoogleSignIn, GoogleUtilities, AppAuth, BoringSSL, OpenSSL, Protobuf, nanopb, Alamofire, RealmSwift, SDWebImage, Lottie, OneSignal, AppCenter, Sentry, or Crashlytics.
- File-name inventory under the built app found no Firebase, GoogleSignIn, GoogleUtilities, AppAuth, BoringSSL, OpenSSL, Protobuf, nanopb, Alamofire, Realm, SDWebImage, Lottie, OneSignal, Kingfisher, RxSwift, SnapKit, Starscream, or Sentry binary artifacts.
- Source search still finds Android/JVM Firebase and Google Sign-In code plus app-defined iOS symbols such as `SharedFirebaseAnalyticsProvider` and `validateGoogleSignInIdToken`; the current iOS provider is local-only and does not link a native Firebase iOS SDK bridge.
- `codesign -dv` reports `Shared.framework` is not signed in the local `CODE_SIGNING_ALLOWED=NO` Release build, so SDK signature evidence cannot be closed from this artifact.
- The 2026-06-01 refresh confirms the only embedded framework in the unsigned Release app is still `Frameworks/Shared.framework`, and the built privacy manifest hash still matches the source privacy manifest hash.

This local scan reduces the third-party SDK risk for the current build tree, but it does not close AS-18. The final reviewer must inspect the signed `.xcarchive` or IPA, export the Xcode privacy report, verify any listed SDK manifests/signatures in the archive, and compare the report to `docs/APP_STORE_PRIVACY_LABELS.md` and `docs/APP_STORE_PRIVACY_EVIDENCE.md` before setting `APP_STORE_SDK_PRIVACY_EVIDENCE_COMPLETE=true`.

For a signed `.xcarchive`, also export or attach:

- Xcode archive privacy report for the submitted build.
- Embedded framework and binary SDK inventory from the archive package.
- SDK signature validation result from Xcode Organizer or App Store Connect upload output.
- Any App Store Connect privacy manifest warning email and its resolution.

## Evidence To Attach

Record these before final signoff:

- App Store Connect version, build number, release commit, and archive path.
- List of linked iOS third-party SDKs and whether each appears on Apple's required SDK list.
- For every listed or repackaged listed SDK: manifest path, version, provider, dependency type, signature status if binary, and evidence that Xcode validates the expected SDK developer.
- Confirmation that Firebase/Google SDKs are absent from the signed iOS app, or evidence that their required manifests/signatures are present if enabled.
- Xcode privacy report reviewer/date and comparison with App Store privacy labels.
- Any accepted exception or remediation for a third-party SDK that lacks a valid manifest/signature.

## Closure Rule

Set `APP_STORE_SDK_PRIVACY_EVIDENCE_COMPLETE=true` only after:

- The signed review archive has a complete SDK inventory.
- Required third-party SDK privacy manifests and signatures are present for listed or repackaged listed SDKs, or the signed archive proves no listed binary SDKs are included.
- The Xcode privacy report has been reviewed against App Store privacy labels and live privacy policy evidence.
- No invalid privacy manifest or SDK signature warning remains unresolved.
- `APP_STORE_SDK_PRIVACY_CONFIRMED=true` is set only in the final release shell or CI secret store after this file is updated.
- `docs/APP_STORE_FINAL_SIGNOFF.md` references this completed evidence for the submitted review build.
