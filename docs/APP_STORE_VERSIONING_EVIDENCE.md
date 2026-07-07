# App Store Versioning Evidence - Wakeve

Date: 2026-06-01

Status: PENDING

Do not change the marker below until the exact App Store Connect version and uploaded build selected for App Review have been verified.

APP_STORE_VERSIONING_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-05-28.

- Apple says the required role to upload builds is Account Holder, Admin, App Manager, or Developer.
- Apple says a build can be uploaded with Xcode, Swift Playground, altool, or Transporter after the app has been added to the developer account.
- Apple says a build must be processed in Apple's system before it appears in App Store Connect.
- Apple says App Store Connect sends an email after build processing is complete.
- Apple says each build upload uses the app bundle's bundle ID and version number to associate the build with the app and version record in App Store Connect.
- Apple says the build string uniquely identifies the build throughout the system.
- Apple says a failed build upload can reuse the same build number for the next upload, but a processed build for the same version still requires incrementing before another upload.
- Apple says a new App Store version requires an incremental App Store version number.
- Apple says the build string should be incremented in Xcode before uploading the build to App Store Connect.
- Apple says the new build should be added to the latest version before submitting the app to App Review.
- Apple says the required role to choose a build for submission is Account Holder, Admin, or App Manager.
- Apple says before submitting an app version for review, the required metadata must be provided and the build for the version must be chosen.
- Apple says the app version Build section should be checked to verify that the right build was added for the version.
- Apple says only one build can be associated with each app version, though the selected build can be changed until the version is submitted to App Review.
- Apple says the selected build's app icon, version number, build string, and upload date appear in the Build section after the build is added to the version.
- Apple says if the selected build has Missing Compliance status, export compliance questions or encryption documentation must be completed before review submission.
- Apple says uploaded builds can be viewed by version number in App Store Connect, and selecting a build shows metadata such as file size and processing details.
- Apple says App Store Connect can show a build's version, build number, upload status, and creation date.

## Scope

This file records release-version and build-number evidence that cannot be proven from the repository alone. App Store Connect rejects duplicate build numbers for the same app version, so the release owner must compare the current Xcode build settings with the latest uploaded App Store Connect build before upload and record the selected review build after upload.

## Current Repository Values

| Field | Source | Current Value | Evidence |
| --- | --- | --- | --- |
| Marketing version | `MARKETING_VERSION` in `iosApp/iosApp.xcodeproj/project.pbxproj` | `1.0` | `./scripts/lint-store-metadata.sh --ios-only` |
| Build number | `CURRENT_PROJECT_VERSION` / `CFBundleVersion` in `iosApp/iosApp.xcodeproj/project.pbxproj` | `1` | `./scripts/lint-store-metadata.sh --ios-only` |
| Built app version | `build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app/Info.plist` | Pending signed release build | `plutil -p .../Info.plist` |

## Required App Store Connect Comparison

Record these values before the final App Store upload:

| Field | Required Evidence | Status |
| --- | --- | --- |
| App Store Connect version record | Version number selected for submission. | Pending |
| Latest uploaded build for that version | App Store Connect build list screenshot/export or Fastlane/App Store Connect output. | Pending |
| Candidate build number | The build number produced by the signed archive. | Pending |
| Duplicate-build decision | Candidate build number is greater than the latest processed uploaded build for the same version, there is no prior build for that version, or documented evidence shows the previous upload failed and the build number can be reused. | Pending |
| Bump command or Xcode change | Evidence of incrementing `CURRENT_PROJECT_VERSION` / `CFBundleVersion` if the candidate build number was already used. | Pending |
| Build processing status | App Store Connect shows the uploaded build processing completed before it is selected for the version. | Pending |
| Uploaded review build | App Store Connect screenshot/export showing the exact build selected for App Review, including app icon, version number, build string, and upload date. | Pending |
| Missing Compliance status | If the build shows Missing Compliance, export compliance questions or encryption documentation are completed before review submission. | Pending |

## Evidence Commands

Run before final signoff:

```bash
./scripts/lint-store-metadata.sh --ios-only
bundle exec fastlane ios preflight strict:true
plutil -p build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app/Info.plist | rg -n "CFBundleShortVersionString|CFBundleVersion"
```

When an App Store Connect API key is available, also record the output from the release operator's preferred App Store Connect build lookup, for example a Fastlane `latest_testflight_build_number` or `app_store_build_number` check scoped to Bundle ID `com.guyghost.wakeve` and version `1.0`.

## Local Versioning Scan Result

Commands refreshed on 2026-06-01:

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme WakeveApp -configuration Release -showBuildSettings | rg 'PRODUCT_BUNDLE_IDENTIFIER|PRODUCT_NAME|MARKETING_VERSION|CURRENT_PROJECT_VERSION|INFOPLIST_KEY_LSApplicationCategoryType|INFOPLIST_KEY_CFBundleShortVersionString|INFOPLIST_KEY_CFBundleVersion'
plutil -p build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app/Info.plist | rg -n "CFBundleShortVersionString|CFBundleVersion"
```

Local result:

- Xcode project Release and Debug build settings use `MARKETING_VERSION = 1.0` and `CURRENT_PROJECT_VERSION = 1`.
- Xcode project Info.plist generation maps `CFBundleShortVersionString` from `$(MARKETING_VERSION)` and `CFBundleVersion` from `$(CURRENT_PROJECT_VERSION)`.
- Built local Release app has `CFBundleShortVersionString => "1.0"` and `CFBundleVersion => "1"`.
- Xcode Release build settings also resolve `PRODUCT_BUNDLE_IDENTIFIER = com.guyghost.wakeve`, `PRODUCT_NAME = Wakeve`, and `INFOPLIST_KEY_LSApplicationCategoryType = public.app-category.productivity`.
- Source Info.plist currently resolves `CFBundleIdentifier => "com.guyghost.wakeve"`, `CFBundleDisplayName => "Wakeve"`, `CFBundleShortVersionString => "1.0"`, and `CFBundleVersion => "1"`.
- Local Fastlane upload paths still require Apple account/team variables before upload, so no App Store Connect build lookup or selected review-build proof has been captured.

This is local pre-submission evidence only. It does not close versioning until the signed archive or IPA is compared with the latest App Store Connect build for version `1.0`, any duplicate build-number conflict is resolved before upload, build processing has completed, the Build section shows the exact build selected for App Review, any Missing Compliance state is resolved, and `APP_STORE_VERSIONING_EVIDENCE_COMPLETE=true` is set for the uploaded build selected for App Review.

## Apple References

- Upload builds: https://developer.apple.com/help/app-store-connect/manage-builds/upload-builds
- Choose a build to submit: https://developer.apple.com/help/app-store-connect/manage-builds/choose-a-build-to-submit
- View builds and metadata: https://developer.apple.com/help/app-store-connect/manage-builds/view-builds-and-metadata
- Submit an app: https://developer.apple.com/help/app-store-connect/manage-submissions-to-app-review/submit-an-app
- Build upload statuses: https://developer.apple.com/help/app-store-connect/reference/app-uploads/build-upload-statuses/
- App Information reference: https://developer.apple.com/help/app-store-connect/reference/app-information/

## Closure Rule

Set `APP_STORE_VERSIONING_EVIDENCE_COMPLETE=true` only after:

- The signed IPA or Release app bundle has a marketing version matching the App Store Connect version record.
- The candidate `CFBundleVersion` has been compared with the latest uploaded App Store Connect build for that same version.
- Any required build-number increment has been applied before upload, unless a failed-upload retry is documented as reusable.
- App Store Connect shows the build processing completed and the selected Build section values match the signed IPA.
- Any Missing Compliance state has been resolved before Add for Review or Submit for Review.
- The uploaded App Store Connect build selected for App Review matches the signed IPA, release artifact evidence, and TestFlight smoke evidence.
- `docs/APP_STORE_FINAL_SIGNOFF.md` references this completed evidence for the submitted review build.
