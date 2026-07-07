# App Store Export Compliance Evidence - Wakeve

Date: 2026-06-01

Status: PENDING

Do not change the marker below until the exact uploaded App Store Connect build has been reviewed for export compliance and the App Store Connect encryption answer matches the repository evidence.

APP_STORE_EXPORT_COMPLIANCE_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-05-28.

- Apple says apps that use, access, contain, implement, or incorporate encryption must determine export compliance requirements in App Store Connect before upload, testing, and distribution.
- Apple says encryption includes crypto functionality within Apple's operating system and crypto functionality from proprietary or non-Apple sources.
- Apple says the developer is responsible for reviewing export regulations and liability for inaccurate exemption claims.
- Apple says App Store Connect asks encryption questions for each new version unless the required information is provided in the app's Info.plist.
- Apple says HTTPS connections using `NSURLSession` are typically an example of operating-system encryption that is exempt from documentation upload requirements.
- Apple says apps using encryption limited to that within the Apple operating system do not require documentation in App Store Connect.
- Apple says apps using an industry-standard algorithm not provided by the Apple operating system require a French encryption declaration in App Store Connect.
- Apple says apps using proprietary encryption algorithms not accepted by international standard bodies require a US CCATS and French encryption declaration in App Store Connect.
- Apple says the French encryption declaration form is only required if distributing the app on the App Store in France.
- Apple says adding `ITSAppUsesNonExemptEncryption` to the app Info.plist declares whether the app uses encryption.
- Apple says if `ITSAppUsesNonExemptEncryption` is absent, App Store Connect walks the developer through an export compliance questionnaire every time a new app version is uploaded.
- Apple says a value of `false` for `ITSAppUsesNonExemptEncryption` indicates the app doesn't use encryption or only uses exempt encryption.
- Apple says the `ITSAppUsesNonExemptEncryption=false` claim covers the app and third-party libraries linked into the app.
- Apple says if non-exempt encryption is used and documentation is reviewed successfully, Apple provides a code for the Info.plist.
- Apple says App Store Connect API app encryption declarations may be required for builds with `usesNonExemptEncryption=true` before beta app review submission.

## Build Under Review

- App Store Connect version: TBD
- Build number: TBD
- Release commit: TBD
- Info.plist source: `iosApp/src/Info.plist`
- Built Info.plist evidence: `build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app/Info.plist`
- Final signoff reference: `docs/APP_STORE_FINAL_SIGNOFF.md`

## Current Repository Evidence

| Evidence | Expected Result | Status |
| --- | --- | --- |
| Source Info.plist | `ITSAppUsesNonExemptEncryption=false` | Local scan passed on 2026-06-01. |
| Built Release Info.plist | `ITSAppUsesNonExemptEncryption=false` | Local unsigned Release scan passed on 2026-06-01. |
| iOS network transport | iOS app network code uses `URLSession`/HTTPS or DEBUG-only localhost endpoints; Release binary cleartext/localhost scans remain enforced separately. | Local scan passed on 2026-06-01; Debug localhost URLs are guarded by `#if DEBUG`, and the Release binary scan found no localhost, loopback, or cleartext HTTP strings. |
| iOS secure storage | Token storage uses Apple Keychain/Security framework APIs rather than custom encryption. | Local source scan passed on 2026-06-01 for `SecureTokenStorage.swift` and `IosTokenStorage.kt`. |
| Custom crypto scan | No iOS app source uses `CryptoKit`, `CommonCrypto`, `CCCrypt`, `SecKey`, AES/RSA/ChaCha custom encryption APIs, or `ITSEncryptionExportComplianceCode`. Hashing/JWT parsing references are not treated as reversible encryption evidence. | Local source scan passed on 2026-06-01. |
| Non-exempt encryption code | No `ITSEncryptionExportComplianceCode` is required while `ITSAppUsesNonExemptEncryption=false` remains accurate. | Local source and built-plist scan passed on 2026-06-01. |
| App Store Connect answer | The app uses no non-exempt encryption, or uses only encryption exempt from export compliance documentation. | Pending |

## App Store Connect Evidence

Apple's current App Store Connect help says apps that use encryption must determine export compliance requirements, and apps using encryption limited to the Apple operating system do not require documentation in App Store Connect. Apple's `ITSAppUsesNonExemptEncryption` documentation says setting the key to `NO` indicates the app uses no encryption or only exempt encryption.

Record the final App Store Connect encryption answer here before final signoff:

- App Store Connect encryption status: TBD
- Documentation uploaded: No / Yes, reference TBD
- Reviewer/date: TBD
- Screenshot or export reference: TBD

## Evidence Commands

Run these against the exact review build before setting `APP_STORE_EXPORT_COMPLIANCE_EVIDENCE_COMPLETE=true`:

```bash
plutil -p iosApp/src/Info.plist | rg "ITSAppUsesNonExemptEncryption|ITSEncryptionExportComplianceCode"

plutil -p build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app/Info.plist \
  | rg "ITSAppUsesNonExemptEncryption|ITSEncryptionExportComplianceCode"

rg -n "import Security|Keychain|SecItem|SecAccess|CryptoKit|CommonCrypto|CCCrypt|SecKey|AES|ChaCha|RSA|SHA256withECDSA|ITSEncryptionExportComplianceCode|NSURLSession|URLSession" \
  iosApp/src shared/src/iosMain shared/src/commonMain server/src/main -g '*.{swift,kt}'

./scripts/lint-store-metadata.sh --ios-only
```

If non-exempt encryption is introduced later, do not keep this record complete until App Store Connect documentation requirements are re-evaluated and any required export compliance documentation or `ITSEncryptionExportComplianceCode` evidence is recorded.

## Local Export Compliance Scan Result

Commands refreshed on 2026-06-01:

```bash
plutil -extract ITSAppUsesNonExemptEncryption raw -o - iosApp/src/Info.plist
plutil -extract ITSAppUsesNonExemptEncryption raw -o - build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app/Info.plist
rg -n "CryptoKit|CommonCrypto|CCCrypt|SecKey|\bAES\b|\bRSA\b|ChaCha|ITSEncryptionExportComplianceCode" iosApp/src shared/src/iosMain shared/src/commonMain -g '*.swift' -g '*.kt'
strings build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app/Wakeve build/xcode-deriveddata-release/Build/Products/Release-iphoneos/Wakeve.app/Frameworks/Shared.framework/Shared | rg -n "localhost|127\.0\.0\.1|http://|ITSEncryptionExportComplianceCode|CryptoKit|CommonCrypto|CCCrypt|SecKey|AES|RSA|ChaCha"
```

Local result:

- Source Info.plist: `"ITSAppUsesNonExemptEncryption" => false`
- Built Release Info.plist: `"ITSAppUsesNonExemptEncryption" => false`
- `ITSEncryptionExportComplianceCode`: no match in the source scan or built Release Info.plist output.
- iOS app networking uses `URLSession` in `iosApp/src/Services/APNsService.swift` and `iosApp/src/Services/AuthenticationService.swift`.
- Debug localhost API defaults remain present in source under `#if DEBUG`; Release uses `https://api.wakeve.app`, and the current Release binary scan found no localhost, loopback, or cleartext HTTP strings.
- iOS token storage uses Apple Keychain/Security framework APIs in `iosApp/src/Services/SecureTokenStorage.swift` and `shared/src/iosMain/kotlin/com/guyghost/wakeve/auth/shell/services/IosTokenStorage.kt`.
- No iOS app source match was found for `CryptoKit`, `CommonCrypto`, `CCCrypt`, `SecKey`, AES, RSA, ChaCha, or `ITSEncryptionExportComplianceCode`. The refreshed 2026-06-01 command uses word-boundary AES/RSA matching to avoid opaque binary/source substrings that are not API usage.
- The broad Release binary string scan emitted opaque `AES` substrings inside binary data; no matching source usage or plist export-compliance code was found, so these are not treated as custom encryption evidence.
- The scan does find `SHA256withECDSA` in `server/src/main/kotlin/com/guyghost/wakeve/auth/AppleOAuth2Service.kt`, which is server-side Apple Sign in token signing and is not bundled as iOS app encryption evidence.
- The scan finds SHA-256 hashing/JWT/Base64 references in shared code; these are one-way hashing/encoding/parsing helpers, not custom reversible encryption.

This is local pre-submission evidence only. It does not close export compliance until the signed uploaded App Store Connect build is reviewed, the App Store Connect encryption/export compliance answer is recorded, and `APP_STORE_EXPORT_COMPLIANCE_EVIDENCE_COMPLETE=true` is set for that exact build.

## Apple References

- Overview of export compliance: https://developer.apple.com/help/app-store-connect/manage-app-information/overview-of-export-compliance
- Export compliance documentation for encryption: https://developer.apple.com/help/app-store-connect/reference/export-compliance-documentation-for-encryption
- Complying with Encryption Export Regulations: https://developer.apple.com/documentation/security/complying_with_encryption_export_regulations
- `ITSAppUsesNonExemptEncryption`: https://developer.apple.com/documentation/bundleresources/information-property-list/itsappusesnonexemptencryption
- `ITSEncryptionExportComplianceCode`: https://developer.apple.com/documentation/bundleresources/information-property-list/itsencryptionexportcompliancecode

## Closure Rule

Set `APP_STORE_EXPORT_COMPLIANCE_EVIDENCE_COMPLETE=true` only after:

- The uploaded review build contains `ITSAppUsesNonExemptEncryption=false`.
- The App Store Connect encryption/export compliance answer matches the source and built Info.plist evidence.
- The submitted iOS build has been checked for linked third-party libraries and app code that might introduce non-exempt encryption after this scan.
- No non-exempt encryption or required App Store Connect export documentation has been introduced since the evidence commands were run.
- `docs/APP_STORE_FINAL_SIGNOFF.md` references this completed evidence for the uploaded review build.
