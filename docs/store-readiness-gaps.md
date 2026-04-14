# Store Readiness Gap Analysis — Wakeve v1.0.0

> **Date:** 2026-04-14  
> **Baseline:** Current `main` branch state  
> **Reference:** [store-readiness-checklist.md](./store-readiness-checklist.md)

---

## Summary

| Category | Total Items | ✅ Done | ⬜ Gap | ⚠️ Needs Review |
|----------|-------------|---------|--------|-----------------|
| Store Compliance (iOS) | 15 | 5 | 8 | 2 |
| Store Compliance (Android) | 11 | 2 | 8 | 1 |
| Store Assets | 13 | 0 | 13 | 0 |
| Store Listing Copy | 9 | 0 | 9 | 0 |
| Legal | 7 | 0 | 6 | 1 |
| Performance | 10 | 0 | 10 | 0 |
| Security | 10 | 0 | 9 | 1 |
| Accessibility | 8 | 0 | 8 | 0 |
| UX & Quality | 10 | 0 | 10 | 0 |
| Pre-Submission | 9 | 0 | 9 | 0 |
| **TOTAL** | **102** | **7** | **82** | **5** |

**Overall Readiness: ~7%** — Significant work required before submission.

---

## Critical Blockers (Must Fix Before Submission)

### 🔴 iOS Blockers

1. **No App Store Connect setup** — No distribution certificate, provisioning profile, or app record
2. **Missing iOS permission descriptions**:
   - `NSCalendarsUsageDescription` — Required if CalendarService is active
   - `NSContactsUsageDescription` — May be needed for participant invitations
   - `NSPhotoLibraryUsageDescription` — If event images are supported
3. **No TestFlight testing** — Internal testing distribution not configured
4. **App Privacy Nutrition Label** — Not declared in App Store Connect

### 🔴 Android Blockers

1. **No Play Console setup** — No developer account, store listing, or content rating
2. **No upload keystore** — Release build requires signed AAB
3. **Data Safety declaration** — Required by Google Play policy
4. **Target SDK compliance** — Needs verification for SDK 36 requirements (edge-to-edge, foreground service types)

### 🔴 Cross-Platform Blockers

1. **No Privacy Policy** — Legally required by both stores
2. **No Terms of Service** — Required for user accounts
3. **No store assets** — Icons, screenshots, feature graphics all missing
4. **No store listing copy** — Title, descriptions, keywords all missing

---

## High Priority (Should Fix)

### ⚠️ Security

| Issue | Risk | Effort |
|-------|------|--------|
| No secret scanning configured | Credentials could be in git history | S |
| `BuildConfig.DEBUG` gates may be in production code | Debug behavior in release | S |
| No dependency audit for CVEs | Known vulnerabilities | M |
| No ProGuard/R8 configuration | Code easily decompiled | M |
| No App Transport Security verification (iOS) | Cleartext traffic possible | S |

### ⚠️ Performance

| Issue | Target | Current Status |
|-------|--------|----------------|
| Cold start not benchmarked | < 2s Android / < 1.5s iOS | Unknown |
| Memory usage not profiled | < 150 MB | Unknown |
| App size not measured | < 30 MB APK / < 50 MB IPA | Unknown |
| No crash monitoring | 99.5% crash-free | No baseline |

### ⚠️ Accessibility

| Issue | Platform | Effort |
|-------|----------|--------|
| Missing content descriptions on Compose elements | Android | M |
| Missing accessibility labels on SwiftUI views | iOS | M |
| Dynamic Type / font scaling not tested | iOS | S |
| Color contrast not verified against WCAG AA | Both | S |

---

## Medium Priority (Nice to Have for v1.0)

- **Fastlane** setup for automated screenshots and deployment
- **GitHub Actions** store-readiness workflow
- **Store metadata linter** for automated validation
- **Performance benchmark CI job** for regression detection
- **Accessibility audit CI integration**

---

## What's Already In Place ✅

1. **Bundle ID / Application ID**: `com.guyghost.wakeve` — correctly configured on both platforms
2. **iOS Info.plist**: Contains Siri intents, background modes, location permissions, basic launch screen
3. **Android SDK versions**: compileSdk 36, minSdk 24, targetSdk 36 — current and compliant
4. **Version**: versionCode 1, versionName "1.0" on both platforms
5. **iOS device capabilities**: armv7 declared
6. **iOS background modes**: fetch + remote-notification declared
7. **Existing unit tests**: 36+ tests in shared module

---

## Recommended Action Plan

### Week 1: Blockers & Legal
- [ ] Create Apple Developer + Google Play developer accounts (if not done)
- [ ] Write Privacy Policy (use template, adapt for Wakeve)
- [ ] Write Terms of Service
- [ ] Create app icons (1024×1024 master)
- [ ] Create store listing copy (EN + FR)
- [ ] Add missing iOS permission descriptions
- [ ] Create Android upload keystore
- [ ] Run dependency audit and fix critical CVEs

### Week 2: Build & Quality
- [ ] Set up App Store Connect (certificates, profiles, app record)
- [ ] Set up Google Play Console (store listing, content rating, data safety)
- [ ] Take screenshots on both platforms (6-8 per device size)
- [ ] Configure ProGuard/R8 for Android
- [ ] Remove debug artifacts from production builds
- [ ] Run accessibility audit and fix critical issues
- [ ] Benchmark cold start and memory on both platforms

### Week 3: Validation & Submission
- [ ] Upload to TestFlight, distribute to internal testers
- [ ] Upload to Google Play Internal Testing track
- [ ] Run 20+ test scenarios on each platform
- [ ] Fix all discovered issues
- [ ] Final checklist sign-off
- [ ] Submit to both stores

---

## Notes

- This gap analysis is based on code inspection and does not replace actual store review feedback
- Apple's review process is opaque — even a fully compliant app may receive rejections requiring iteration
- Google Play review is typically faster (1-3 days) but has strict data safety requirements
- Both stores may require iterative resubmission — budget 2-3 weeks total timeline
