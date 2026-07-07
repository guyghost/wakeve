# iOS Accessibility Source Audit

Generated: 2026-06-13T13:12:51Z

Status: LOCAL SOURCE AUDIT

This report supports roadmap P1.3 by catching source-level accessibility risks. It does not close App Store accessibility evidence without device validation.

## Summary

| Check | Findings |
| --- | ---: |
| Hardcoded accessibility labels/hints/values | 0 |
| Single-line text without nearby scaling/wrap fallback | 0 |
| Indeterminate ProgressView without label or explicit hiding | 0 |
| Total | 0 |

## Hardcoded Accessibility Strings

No hardcoded `.accessibilityLabel("...")`, `.accessibilityHint("...")`, `.accessibilityValue("...")`, or named `accessibilityLabel:`/`accessibilityHint:`/`accessibilityValue:` string arguments were found.

## Single-Line Text Risks

No `.lineLimit(1)` calls without a nearby `.minimumScaleFactor`, `.fixedSize`, `.allowsTightening`, or `.dynamicTypeSize` fallback were found.

## Indeterminate ProgressView Risks

No bare `ProgressView()` calls without a nearby `.accessibilityLabel(...)` or `.accessibilityHidden(true)` were found.

## Closure Notes

- Fix hardcoded VoiceOver strings by using `String(localized:)` or localized view text.
- Label user-visible loading indicators, or hide decorative button spinners when the surrounding control already exposes the action state.
- Review single-line text in release screens under Dynamic Type accessibility sizes before claiming Larger Text support.
- Keep the App Store evidence marker false until signed-build device checks cover Dynamic Type, VoiceOver, high contrast, reduced motion, and color-only states.
