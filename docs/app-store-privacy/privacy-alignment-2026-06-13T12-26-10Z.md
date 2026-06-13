# App Store Privacy Alignment Audit

Generated: 2026-06-13T12:26:10Z

Status: LOCAL EVIDENCE

This report checks local privacy-label alignment. It does not close AS-05 until App Store Connect privacy labels, the live privacy URL, legal/privacy owner approval, and the uploaded review build are verified.

## Inputs

| File | SHA-256 |
| --- | --- |
| `iosApp/src/PrivacyInfo.xcprivacy` | `38dbda46a737beed9c54a65cf089159fbb2712de1c21b8c9cd5de6877acfbfc3` |
| `docs/APP_STORE_PRIVACY_LABELS.md` | `6b8817f3013c36f1ef60b3d1d67d4aa8071aba02d36224cae6aa8e01438cd638` |
| `docs/PRIVACY_POLICY.md` | `8eb134c37318846c8c3ffbac075ee76d606204f44a17a1618e94b8c6f078b285` |

## Local Checks

| Result | Check |
| --- | --- |
| PASS | Privacy manifest declares NSPrivacyTracking=false. |
| PASS | Privacy manifest declares no tracking domains. |
| PASS | Privacy-label draft declares no tracking. |
| PASS | Info.plist does not declare NSUserTrackingUsageDescription. |
| PASS | iOS/shared source contains no IDFA or App Tracking Transparency API references. |
| PASS | Privacy manifest declares NSPrivacyCollectedDataTypeName. |
| PASS | Privacy-label draft mirrors NSPrivacyCollectedDataTypeName. |
| PASS | Privacy manifest declares NSPrivacyCollectedDataTypeEmailAddress. |
| PASS | Privacy-label draft mirrors NSPrivacyCollectedDataTypeEmailAddress. |
| PASS | Privacy manifest declares NSPrivacyCollectedDataTypeUserID. |
| PASS | Privacy-label draft mirrors NSPrivacyCollectedDataTypeUserID. |
| PASS | Privacy manifest declares NSPrivacyCollectedDataTypeDeviceID. |
| PASS | Privacy-label draft mirrors NSPrivacyCollectedDataTypeDeviceID. |
| PASS | Privacy manifest declares NSPrivacyCollectedDataTypeOtherUserContent. |
| PASS | Privacy-label draft mirrors NSPrivacyCollectedDataTypeOtherUserContent. |
| PASS | Privacy manifest declares NSPrivacyCollectedDataTypeCoarseLocation. |
| PASS | Privacy-label draft mirrors NSPrivacyCollectedDataTypeCoarseLocation. |
| PASS | Privacy manifest declares NSPrivacyCollectedDataTypeProductInteraction. |
| PASS | Privacy-label draft mirrors NSPrivacyCollectedDataTypeProductInteraction. |
| PASS | Privacy-label draft explicitly lists Contacts under data not collected. |
| PASS | Privacy-label draft explicitly lists Browsing History under data not collected. |
| PASS | Privacy-label draft explicitly lists Search History under data not collected. |
| PASS | Privacy-label draft explicitly lists Financial Info under data not collected. |
| PASS | Privacy-label draft explicitly lists Health and Fitness under data not collected. |
| PASS | Privacy-label draft explicitly lists Sensitive Info under data not collected. |
| PASS | Privacy-label draft explicitly lists Purchases under data not collected. |
| PASS | Privacy-label draft explicitly lists Advertising Data under data not collected. |
| PASS | Privacy policy source includes contact and Data Collection sections. |
| PASS | Privacy-label draft records required open verification questions. |
| PENDING | Open external question: App Store Connect privacy labels still need owner comparison against this draft. |
| PENDING | Open external question: live https://wakeve.app/privacy must be reachable and match docs/PRIVACY_POLICY.md. |
| PENDING | Open external question: uploaded review build must be checked for bundled PrivacyInfo.xcprivacy and no tracking strings. |
| PENDING | Open product/legal question: photos/media upload, calendar data, Siri/speech, analytics, and crash behavior need final owner confirmation. |

## Manifest Data Types

```text
NSPrivacyCollectedDataTypeCoarseLocation
NSPrivacyCollectedDataTypeDeviceID
NSPrivacyCollectedDataTypeEmailAddress
NSPrivacyCollectedDataTypeName
NSPrivacyCollectedDataTypeOtherUserContent
NSPrivacyCollectedDataTypeProductInteraction
NSPrivacyCollectedDataTypeUserID
```

## Summary

| Metric | Count |
| --- | ---: |
| Local findings | 0 |
| External pending confirmations | 4 |

Result: PASS for local privacy alignment. AS-05 remains open for App Store Connect, live URL, legal/privacy owner, and uploaded-build evidence.
