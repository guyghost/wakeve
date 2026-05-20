# Store Readiness Checklist — Wakeve v1.0.0

> **Version:** 1.0  
> **Last Updated:** 2026-04-14  
> **Status:** Pre-submission  
> **Platforms:** iOS (App Store) + Android (Google Play)

This checklist covers all requirements for submitting Wakeve to both the Apple App Store and Google Play Store. Every item **MUST** pass before submission.

---

## 1. Store Compliance

### 1.1 Apple App Store

| # | Requirement | Status | Notes |
|---|-------------|--------|-------|
| 1.1.1 | Apple Developer Account active | ⬜ | $99/year membership |
| 1.1.2 | App Store Connect app record created | ⬜ | Bundle ID: `com.guyghost.wakeve` |
| 1.1.3 | Bundle ID registered and valid | ✅ | `com.guyghost.wakeve` in Info.plist |
| 1.1.4 | Provisioning profiles configured (Distribution) | ⬜ | Production provisioning profile |
| 1.1.5 | Code signing with valid distribution certificate | ⬜ | |
| 1.1.6 | App Privacy Nutrition Label completed | ⬜ | Data collection declarations |
| 1.1.7 | App Review Information filled (contact, demo account) | ⬜ | |
| 1.1.8 | Age Rating assigned (4+, likely) | ⬜ | No user-generated content in v1 |
| 1.1.9 | No private API usage | ⬜ | |
| 1.1.10 | No hardcoded test/demo content | ⬜ | |
| 1.1.11 | Launch screen configured (Storyboard/XIB) | ⬜ | `UILaunchScreen` in Info.plist |
| 1.1.12 | All required device capabilities declared | ✅ | `armv7` in Info.plist |
| 1.1.13 | Siri Intents properly declared | ✅ | 8 intents in Info.plist |
| 1.1.14 | Background modes justified | ✅ | `fetch` + `remote-notification` |
| 1.1.15 | No placeholder or "Lorem ipsum" text | ⬜ | |

### 1.2 Google Play Store

| # | Requirement | Status | Notes |
|---|-------------|--------|-------|
| 1.2.1 | Google Play Developer Account active | ⬜ | $25 one-time fee |
| 1.2.2 | Play Console store listing draft created | ⬜ | |
| 1.2.3 | Application ID valid (`com.guyghost.wakeve`) | ✅ | In build.gradle.kts |
| 1.2.4 | Upload keystore created and secured | ⬜ | |
| 1.2.5 | App signing by Google Play enrolled | ⬜ | Recommended |
| 1.2.6 | Content rating questionnaire completed | ⬜ | IARC rating |
| 1.2.7 | Data safety declaration completed | ⬜ | Google Play data safety section |
| 1.2.8 | Target API level ≤ 1 year old | ✅ | targetSdk 36 (current) |
| 1.2.9 | App Bundle (.aab) format | ⬜ | Not APK |
| 1.2.10 | No placeholder or "Lorem ipsum" text | ⬜ | |
| 1.2.11 | Android Vitals thresholds met | ⬜ | ANR < 0.5%, crash < 1% |

---

## 2. Store Assets

### 2.1 iOS App Store Assets

| # | Asset | Size | Status |
|---|-------|------|--------|
| 2.1.1 | App icon | 1024×1024 px | ⬜ |
| 2.1.2 | iPhone 6.7" screenshots | 1290×2796 px (×3-10) | ⬜ |
| 2.1.3 | iPhone 6.5" screenshots | 1242×2688 px (×3-10) | ⬜ |
| 2.1.4 | iPhone 5.5" screenshots | 1242×2208 px (×3-10) | ⬜ |
| 2.1.5 | iPad 12.9" screenshots (if supported) | 2048×2732 px | ⬜ N/A |

### 2.2 Google Play Assets

| # | Asset | Size | Status |
|---|-------|------|--------|
| 2.2.1 | App icon | 512×512 px | ⬜ |
| 2.2.2 | Feature graphic | 1024×500 px | ⬜ |
| 2.2.3 | Phone screenshots | 16:9 or 9:16, 320-3840px (×4-8) | ⬜ |
| 2.2.4 | Tablet screenshots (optional) | 16:9 or 9:16 | ⬜ |

### 2.3 Store Listing Copy

| # | Item | iOS Limit | Android Limit | Status |
|---|------|-----------|---------------|--------|
| 2.3.1 | App name | 30 chars | 30 chars | ⬜ |
| 2.3.2 | Subtitle (iOS) / Short description (Android) | 30 chars | 80 chars | ⬜ |
| 2.3.3 | Keywords (iOS) / Search terms | 100 chars | — | ⬜ |
| 2.3.4 | Description (EN) | 4000 chars | 4000 chars | ⬜ |
| 2.3.5 | Description (FR) | 4000 chars | 4000 chars | ⬜ |
| 2.3.6 | Promotional text | 170 chars | — | ⬜ |
| 2.3.7 | What's New / Release notes | 4000 chars | 500 chars | ⬜ |
| 2.3.8 | Support URL | required | optional | ⬜ |
| 2.3.9 | Privacy Policy URL | required | required | ⬜ |

---

## 3. Legal

| # | Requirement | Status | Notes |
|---|-------------|--------|-------|
| 3.1 | Privacy Policy published and accessible | ⬜ | URL required by both stores |
| 3.2 | Terms of Service published | ⬜ | |
| 3.3 | Data collection disclosure (GDPR) | ⬜ | EU compliance |
| 3.4 | CCPA compliance (if applicable) | ⬜ | California users |
| 3.5 | Cookie/tracking consent (App Tracking Transparency) | ⬜ | iOS 14.5+ if tracking |
| 3.6 | Export compliance (encryption) | ⬜ | App Store question |
| 3.7 | Copyright notice in app | ⬜ | |

---

## 4. Performance

| # | Metric | Target | Status | Notes |
|---|--------|--------|--------|-------|
| 4.1 | Cold start time (Android) | < 2.0s | ⬜ | Pixel 8 baseline |
| 4.2 | Cold start time (iOS) | < 1.5s | ⬜ | iPhone 15 baseline |
| 4.3 | Memory usage (Android) | < 150 MB | ⬜ | |
| 4.4 | Memory usage (iOS) | < 150 MB | ⬜ | |
| 4.5 | APK size | < 30 MB | ⬜ | App Bundle preferred |
| 4.6 | IPA size | < 50 MB | ⬜ | |
| 4.7 | Crash-free sessions | ≥ 99.5% | ⬜ | |
| 4.8 | ANR rate (Android) | < 0.5% | ⬜ | |
| 4.9 | Battery impact | Minimal | ⬜ | No excessive background work |
| 4.10 | Network efficiency | < 1 MB per session | ⬜ | |

---

## 5. Security

| # | Requirement | Status | Notes |
|---|-------------|--------|-------|
| 5.1 | No hardcoded secrets/API keys in source | ⬜ | Scan with git-secrets/trufflehog |
| 5.2 | No `BuildConfig.DEBUG` gates in production code | ⬜ | |
| 5.3 | HTTPS only for all network calls | ⬜ | No HTTP cleartext |
| 5.4 | Certificate pinning (if applicable) | ⬜ | |
| 5.5 | Secure token storage (Keychain / Keystore) | ⬜ | |
| 5.6 | Input validation on all user fields | ⬜ | XSS/injection prevention |
| 5.7 | Dependency audit — no critical CVEs | ⬜ | `./gradlew dependencyCheck` |
| 5.8 | ProGuard/R8 enabled (Android) | ⬜ | |
| 5.9 | App Transport Security configured (iOS) | ⬜ | |
| 5.10 | Runtime permissions requested properly | ⬜ | Not at launch unless essential |

---

## 6. Accessibility

| # | Requirement | Status | Notes |
|---|-------------|--------|-------|
| 6.1 | Content descriptions on all interactive elements (Android) | ⬜ | Compose `contentDescription` |
| 6.2 | Accessibility labels on all interactive elements (iOS) | ⬜ | SwiftUI `.accessibilityLabel()` |
| 6.3 | Color contrast ratio ≥ 4.5:1 for text | ⬜ | WCAG AA |
| 6.4 | Dynamic Type / Font scaling support (iOS) | ⬜ | |
| 6.5 | Font scaling support (Android) | ⬜ | |
| 6.6 | VoiceOver / TalkBack navigation logical | ⬜ | |
| 6.7 | Touch targets ≥ 44×44 pt (iOS) / 48×48 dp (Android) | ⬜ | |
| 6.8 | No information conveyed by color alone | ⬜ | |

---

## 7. UX & Quality

| # | Requirement | Status | Notes |
|---|-------------|--------|-------|
| 7.1 | No force closes / ANRs in 50+ test sessions | ⬜ | |
| 7.2 | Offline mode works gracefully | ⬜ | Show clear state indicator |
| 7.3 | Loading states for all async operations | ⬜ | Progress indicators |
| 7.4 | Error messages are user-friendly (no stack traces) | ⬜ | |
| 7.5 | Empty states are informative | ⬜ | |
| 7.6 | Deep links resolve correctly | ⬜ | |
| 7.7 | Back navigation works on all screens (Android) | ⬜ | |
| 7.8 | Swipe gestures don't conflict (iOS) | ⬜ | |
| 7.9 | Localization complete (EN + FR) | ⬜ | All visible strings |
| 7.10 | Dark mode supported | ⬜ | |

---

## 8. Pre-Submission Validation

| # | Step | Status | Notes |
|---|------|--------|-------|
| 8.1 | All unit tests pass (`./gradlew shared:test`) | ⬜ | 36+ tests |
| 8.2 | Android instrumented tests pass | ⬜ | |
| 8.3 | iOS tests pass (XCTest) | ⬜ | |
| 8.4 | TestFlight internal testing complete (20+ scenarios) | ⬜ | |
| 8.5 | Google Play Internal Testing track (20+ scenarios) | ⬜ | |
| 8.6 | All checklist items above marked PASS | ⬜ | |
| 8.7 | Final build archived with version tag | ⬜ | |
| 8.8 | Submission to App Store Review | ⬜ | |
| 8.9 | Submission to Google Play Review | ⬜ | |

---

## Pass/Fail Criteria

- **PASS**: All items marked ✅ or verified
- **FAIL**: Any item marked ⬜ or with unresolved issues
- **WAIVER**: Item explicitly waived with documented justification (requires human approval)

## Revision History

| Date | Version | Changes |
|------|---------|---------|
| 2026-04-14 | 1.0 | Initial checklist creation |
