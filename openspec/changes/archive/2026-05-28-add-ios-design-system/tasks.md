## 1. Foundation
- [x] 1.1 Audit existing iOS style definitions and list local color/radius/typography duplicates.
- [x] 1.2 Extend `iosApp/src/Theme/DesignSystem.swift` with namespaced Wakeve tokens.
- [x] 1.3 Update Liquid Glass modifiers to include Reduce Transparency and high-contrast fallbacks.

## 2. Components
- [x] 2.1 Add shared screen background, glass card, action button, and circular icon button components.
- [x] 2.2 Add shared search field, avatar, stacked avatar, section header, list row, and vote segmented control components.
- [x] 2.3 Add SwiftUI previews covering dark, light, event gradient, and accessibility variants.

## 3. Screen Migration
- [x] 3.1 Refactor notification onboarding/preferences to use the design system.
- [x] 3.2 Refactor profile/friends and participant management screens to use the design system.
- [x] 3.3 Refactor event preview/poll voting/create event surfaces to use event gradients, vote controls, cards, and action tokens.
- [x] 3.4 Remove or deprecate local style helpers made redundant by shared components.

## 4. Documentation
- [x] 4.1 Create `docs/guides/ios/design-system.md` with principles, tokens, components, and migration rules.
- [x] 4.2 Link the guide from `docs/guides/ios/README.md` and `docs/README.md`.

## 5. Validation
- [x] 5.1 Build the iOS app or run the closest available Swift/Xcode validation command.
- [x] 5.2 Run relevant iOS unit/UI tests where available. Latest local evidence: on 2026-05-28, XcodeBuildMCP `test_sim` with `CODE_SIGNING_ALLOWED=NO` completed 94 tests, 94 passed, 0 failed. Build log: `/Users/guy/Library/Developer/XcodeBuildMCP/workspaces/wakeve-cf467b3193b0/logs/test_sim_2026-05-28T02-37-15-278Z_pid87182_d99033a3.log`; result bundle: `/Users/guy/Library/Developer/XcodeBuildMCP/workspaces/wakeve-cf467b3193b0/result-bundles/test_sim_2026-05-28T02-37-15-278Z_pid87182_869fa565.xcresult`.
- [x] 5.3 Capture/inspect representative screens for layout, contrast, Dynamic Type, and visual consistency. Local evidence: XcodeBuildMCP `build_run_sim` builds `com.guyghost.wakeve` for iPad Air 13-inch (M4) and iPhone 17 Pro Max. Evidence exists at `docs/app-store-evidence/xcodebuildmcp-ipad-login-2026-05-27.jpg`, `docs/app-store-evidence/xcodebuildmcp-iphone-onboarding-events-2026-05-27.jpg`, `docs/app-store-evidence/xcodebuildmcp-iphone-onboarding-collaboration-2026-05-27.jpg`, `docs/app-store-evidence/xcodebuildmcp-iphone-login-2026-05-27.jpg`, `docs/app-store-evidence/xcodebuildmcp-iphone-login-refresh-2026-05-28.jpg`, `docs/app-store-evidence/xcodebuildmcp-iphone-post-login-home-2026-05-27.jpg`, `docs/app-store-evidence/xcodebuildmcp-iphone-post-login-home-2026-05-28.jpg`, `docs/app-store-evidence/xcodebuildmcp-iphone-home-high-contrast-axxxl-fixed-no-truncation-2026-05-28.jpg`, and `docs/app-store-evidence/xcodebuildmcp-iphone-create-event-2026-05-27.jpg`. iPhone `snapshot_ui` exposed accessible labels for onboarding buttons, titles, checklist items, page state, Sign in with Apple, legal links, the guest button, the development skip button, the post-login empty home state, and the create-event sheet. Dynamic Type/high-contrast inspection found the authenticated-home empty-state subtitle truncating at `accessibility-extra-extra-extra-large`; `HomeEmptyStateView` was adjusted to prioritize text over decorative imagery at accessibility sizes, and the fixed screenshot shows the full subtitle through `non.`. Signed TestFlight/App Store accessibility signoff remains tracked separately in `docs/APP_STORE_ACCESSIBILITY_EVIDENCE.md`.
