# Accessibility Audit — Wakeve v1.0.0

> **Date:** 2026-04-14  
> **Status:** Pre-submission assessment  
> **Platforms:** Android (Compose) + iOS (SwiftUI)

---

## Summary

| Platform | Audit Item | Count | Status |
|----------|-----------|-------|--------|
| Android | Files with `contentDescription` | 15+ | ✅ Good coverage |
| Android | `contentDescription` occurrences | 290 | ✅ |
| Android | Icons missing `contentDescription` | ~15 | ⚠️ Needs fixing |
| iOS | Files with `accessibilityLabel` | 39 | ✅ Moderate |
| iOS | Views with Button/Image but no a11y | 15 | ⚠️ Needs audit |

**Overall:** 🟡 Moderate — Android has better coverage than iOS, but both have gaps.

---

## Android (Jetpack Compose)

### ✅ Passing

- **290 `contentDescription` usages** across 15+ files
- Key screens covered: Inbox, Auth, CreateEvent, Leaderboard, Profile, FilterChips, TimeSlotInput
- StatusIndicator component has content descriptions for state

### ⚠️ Needs Attention

| File | Issue | Severity |
|------|-------|----------|
| `InboxScreen.kt:257` | Icon without `contentDescription` | Medium |
| `InboxScreen.kt:275` | Icon without `contentDescription` | Medium |
| `InboxScreen.kt:354` | Icon without `contentDescription` | Medium |
| `EmailAuthScreen.kt:223` | Icon without `contentDescription` | Medium |
| `AuthScreen.kt:174` | Icon without `contentDescription` | Medium |
| `AuthScreen.kt:263` | Icon without `contentDescription` | Medium |
| `CreateEventScreen.kt:282` | Icon without `contentDescription` | Medium |
| `CreateEventScreen.kt:332` | Icon without `contentDescription` | Medium |
| `CreateEventScreen.kt:533` | Icon without `contentDescription` | Medium |
| `LeaderboardScreen.kt:81` | Icon without `contentDescription` | Low |
| `LeaderboardScreen.kt:94` | Icon without `contentDescription` | Low |
| `CreateEventPreview.kt:148,168,255,344` | 4 Icons without `contentDescription` | Medium |

### Action Items (Android)

1. **Add `contentDescription = null`** to purely decorative icons (acceptable per WCAG)
2. **Add meaningful `contentDescription`** to interactive icons (e.g., options menu, search)
3. **Test with TalkBack** — navigate entire app with screen reader
4. **Verify touch targets ≥ 48×48 dp** for all interactive elements
5. **Verify color contrast** ≥ 4.5:1 for text (WCAG AA)
6. **Test font scaling** — ensure layouts work at 200% font scale

---

## iOS (SwiftUI)

### ✅ Passing

- **39 accessibility label/hint/value usages** across views
- Basic accessibility coverage exists in some views

### ⚠️ Needs Attention

15 SwiftUI view files contain `Button`, `Image`, or `Toggle` but may lack accessibility labels. Files requiring audit:

| View | Accessibility Status |
|------|---------------------|
| `MealPlanningSheets.swift` | ⬜ Audit needed |
| `ProfileSettingsSheet.swift` | ⬜ Audit needed |
| `CommentListView.swift` | ⬜ Audit needed |
| `CommentItemView.swift` | ⬜ Audit needed |
| `CreateEventSheet.swift` | ⬜ Audit needed |
| `LoginView.swift` | ⬜ Audit needed |
| `ParticipantManagementView.swift` | ⬜ Audit needed |
| `LeaderboardView.swift` | ⬜ Audit needed |
| `HomeView.swift` | ⬜ Audit needed |
| `MeetingDetailView.swift` | ⬜ Audit needed |
| `MeetingListView.swift` | ⬜ Audit needed |
| `ExploreScenarioDetailView.swift` | ⬜ Audit needed |
| `GetStartedView.swift` | ⬜ Audit needed |
| `InboxView.swift` | ⬜ Audit needed |
| `MeetingGenerateLinkSheet.swift` | ⬜ Audit needed |

### Action Items (iOS)

1. **Add `.accessibilityLabel()`** to all interactive elements in the 15 views above
2. **Add `.accessibilityHint()`** where action is not obvious from label
3. **Test with VoiceOver** — navigate entire app with gestures
4. **Verify touch targets ≥ 44×44 pt** for all interactive elements
5. **Test Dynamic Type** — verify at largest accessibility sizes
6. **Verify contrast ratios** — especially for secondary text and placeholder text
7. **Test with Switch Control** — ensure logical navigation order

---

## Cross-Platform Checklist

| # | Item | Android | iOS |
|---|------|---------|-----|
| 1 | All interactive elements have labels | 🟡 ~15 missing | ⚠️ 15 views to audit |
| 2 | Color contrast ≥ 4.5:1 for text | ⬜ Not tested | ⬜ Not tested |
| 3 | Touch targets meet minimum size | ⬜ Not tested | ⬜ Not tested |
| 4 | Screen reader navigation logical | ⬜ Not tested | ⬜ Not tested |
| 5 | Font scaling works at 200% | ⬜ Not tested | ⬜ Not tested |
| 6 | No info conveyed by color alone | ⬜ Not tested | ⬜ Not tested |
| 7 | Focus order is logical | ⬜ Not tested | ⬜ Not tested |
| 8 | Error messages are accessible | ⬜ Not tested | ⬜ Not tested |

---

## Recommended Approach

### Before v1.0 Submission (Must Do)

1. **Fix Android missing contentDescriptions** — ~15 icons, ~1 hour
2. **Add iOS accessibility labels** — 15 views, ~2-3 hours
3. **Basic VoiceOver/TalkBack smoke test** — navigate all screens, ~1 hour

### Post v1.0 (Nice to Have)

- Full WCAG 2.1 AA audit
- Automated accessibility testing in CI
- Dynamic Type / font scaling testing
- Switch Control testing
- Color contrast automated validation

---

## Tools

| Platform | Tool | Purpose |
|----------|------|---------|
| Android | Android Studio Accessibility Scanner | Scan for a11y issues |
| Android | Espresso Accessibility Checking | Automated tests |
| Android | TalkBack | Screen reader testing |
| iOS | Xcode Accessibility Inspector | Scan for a11y issues |
| iOS | VoiceOver | Screen reader testing |
| Both | axe (browser) | Color contrast validation |

---

*This audit should be revisited before each release.*
