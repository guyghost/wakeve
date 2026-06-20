# App Store Accessibility Evidence - Wakeve

Date: 2026-05-28

Status: PENDING

Do not change the marker below until the exact uploaded review build has either left Accessibility Nutrition Labels unpublished or has device evidence for every accessibility feature claimed in App Store Connect.

APP_STORE_ACCESSIBILITY_EVIDENCE_COMPLETE=false

## Apple Source Baseline

Last checked: 2026-05-27. Local simulator evidence refreshed: 2026-05-28.

- Apple says Accessibility Nutrition Labels help users learn whether an app will be accessible before download.
- Apple says Accessibility Nutrition Labels appear on the App Store product page and are specific to the device type used to view the product page.
- Apple says providing Accessibility Nutrition Labels is voluntary to start, but developers will over time be required to share accessibility support details to submit new apps and app updates.
- Apple says App Store Connect can show that support has not yet been indicated if accessibility information is not provided for a supported device.
- Apple says developers should audit the app before providing responses and assess each supported device separately.
- Apple says users must be able to complete all common tasks of the app using an accessibility feature before support for that feature is indicated.
- Apple says common tasks include primary app functionality plus first launch, login, purchase, and settings.
- Apple says App Review can contact developers to update Accessibility Nutrition Labels if labels are intentionally misleading or harmful.
- Apple says developers are responsible for keeping accessibility responses accurate and up to date.
- Apple says the optional accessibility URL can provide more detail about supported accessibility features and app-specific limitations.
- Apple says iPhone and iPad labels are shown on Apple Vision Pro and Mac App Store when compatible iPhone and iPad apps run on those platforms.

## Build And Label Scope

- App Store Connect version: TBD
- Build number: TBD
- Release commit: TBD
- Accessibility labels draft: `docs/APP_STORE_ACCESSIBILITY_LABELS.md`
- Local simulator screenshot index: `docs/app-store-evidence/README.md`
- TestFlight evidence reference: `docs/APP_STORE_TESTFLIGHT_EVIDENCE.md`
- iOS audit references: `docs/ACCESSIBILITY_AUDIT.md` and `docs/a11y/ACCESSIBILITY_AUDIT_iOS.md`
- App Store Connect decision: leave labels unpublished, or publish only directly tested claims.

## Device Matrix

| Device Family | Required Evidence | Result | Notes |
| --- | --- | --- | --- |
| iPhone | Dark mode, larger Dynamic Type, VoiceOver, contrast, reduced motion, and color-only status checks for primary flows if any label is claimed. | Pending | TBD |
| iPad | Same as iPhone plus iPad layout, pointer/keyboard navigation, and split-view or large-screen behavior if available. | Pending | TBD |
| Mac with Apple silicon | Either opt out/leave labels unclaimed, or verify Designed for iPad/iPhone runtime keyboard, VoiceOver, resizing, and primary workflows. | Not claimed for first release | Repository Release settings disable `SUPPORTS_MAC_DESIGNED_FOR_IPHONE_IPAD`; App Store Connect availability confirmation remains pending. |
| Apple Vision Pro | Either opt out/leave labels unclaimed, or verify compatibility runtime accessibility behavior in an approved test environment. | Not claimed for first release | Repository Release settings disable `SUPPORTS_XR_DESIGNED_FOR_IPHONE_IPAD`; App Store Connect availability confirmation remains pending. |

## Feature Evidence

| Feature | Publication Rule | Result | Evidence |
| --- | --- | --- | --- |
| Dark Interface | Claim only if current review build is tested in dark mode on each published device family. | Pending | TBD |
| Larger Text / Adjustable Text Size | Claim only if large accessibility text sizes do not block primary actions. | Pending | TBD |
| VoiceOver | Claim only if VoiceOver reaches login, create event, poll, calendar, settings, account deletion, UGC, and payment surfaces when enabled. | Pending | TBD |
| Voice Control | Claim only if device-level Voice Control testing is recorded. | Pending | TBD |
| Sufficient Contrast | Claim only if current screens pass documented contrast checks. | Pending | TBD |
| Reduced Motion | Claim only if primary flows behave correctly with Reduced Motion enabled. | Pending | TBD |
| Differentiate without Color Alone | Claim only if poll, vote, status, payment, and notification states are not color-only. | Pending | TBD |
| Captions | Mark not applicable unless video/audio media requiring captions is introduced. | Pending | TBD |
| Audio Descriptions | Mark not applicable unless video content requiring audio descriptions is introduced. | Pending | TBD |

## Local Debug Simulator Evidence

This section records partial local evidence only. It does not justify setting `APP_STORE_ACCESSIBILITY_EVIDENCE_COMPLETE=true`, because the evidence is from XcodeBuildMCP Debug simulator sessions and not from the signed TestFlight or App Review build.

The local screenshot inventory, dimensions, and SHA-256 hashes are indexed in `docs/app-store-evidence/README.md`.

| Screen | Evidence File | Accessibility Hierarchy Observed | Result |
| --- | --- | --- | --- |
| iPhone onboarding - events | `docs/app-store-evidence/xcodebuildmcp-iphone-onboarding-events-2026-05-27.jpg` | `Wakeve`, `Suivant`, `Passer`, onboarding title, descriptive text, checklist items, and page slider values. | Partial local evidence |
| iPhone onboarding - collaboration | `docs/app-store-evidence/xcodebuildmcp-iphone-onboarding-collaboration-2026-05-27.jpg` | `Wakeve`, `Suivant`, `Passer`, onboarding title, descriptive text, checklist items, and page slider values. | Partial local evidence |
| iPhone login | `docs/app-store-evidence/xcodebuildmcp-iphone-login-2026-05-27.jpg` | `Se connecter`, `Sign in with Apple`, `Development mode: Skip authentication`, `Read Privacy Policy`, and `Read Terms of Service`. | Partial local evidence |
| iPhone login refresh | `docs/app-store-evidence/xcodebuildmcp-iphone-login-refresh-2026-05-28.jpg` | XcodeBuildMCP Debug session on iPhone 17 Pro Max showed `Wakeve`, `Se connecter`, `Continuez avec votre identifiant Apple pour commencer`, `Sign in with Apple`, `Continue as guest`, `Development mode: Skip authentication`, `Read Privacy Policy`, and `Read Terms of Service`. | Partial local evidence |
| iPhone authenticated home | `docs/app-store-evidence/xcodebuildmcp-iphone-post-login-home-2026-05-27.jpg` | `À venir`, `Profil`, `Aucun événement à venir`, empty-state description, `Créer un événement`, and tab bar. | Partial local evidence |
| iPhone authenticated home refresh | `docs/app-store-evidence/xcodebuildmcp-iphone-post-login-home-2026-05-28.jpg` | XcodeBuildMCP Debug session on iPhone 17 Pro Max after guest access showed `À venir`, `Profil`, `Aucun événement à venir`, the empty-state description, `Créer un événement`, and the tab bar. | Partial local evidence |
| iPhone authenticated home - large text/high contrast | `docs/app-store-evidence/xcodebuildmcp-iphone-home-high-contrast-axxxl-fixed-no-truncation-2026-05-28.jpg` | Simulator settings: `content_size accessibility-extra-extra-extra-large` and `increase_contrast enabled`. `snapshot_ui` exposed the full empty-state description `Les événements à venir apparaîtront ici, que vous les organisiez ou non.`; screenshot inspection confirms the visible text reaches `non.` without truncation after the `HomeEmptyStateView` layout fix. | Partial local evidence |
| iPhone create event | `docs/app-store-evidence/xcodebuildmcp-iphone-create-event-2026-05-27.jpg` | `Fermer`, disabled `Aperçu`, `Photo`, `Ajouter un arrière-plan`, `Titre de l'événement`, `Date et heure`, `Lieu`, `Organisé par Dev User`, `Ajouter une description`, `Type d'événement`, and `Créer l'événement`. | Partial local evidence |
| iPad login | `docs/app-store-evidence/xcodebuildmcp-ipad-login-2026-05-27.jpg` | Screenshot captured; accessibility hierarchy extraction was not stable in that simulator session. | Partial local evidence |

Current local gaps before App Store accessibility signoff:

- TestFlight iPhone and iPad evidence for the exact signed review build.
- Dynamic Type at large accessibility sizes on login, create event, event detail, poll, calendar, settings, account deletion, UGC moderation, and payment/Tricount surfaces. Authenticated home has partial local Debug simulator evidence for `accessibility-extra-extra-extra-large` plus increased contrast on 2026-05-28.
- VoiceOver traversal and action confirmation for login, create event, poll, calendar, settings, account deletion, UGC moderation, and payment/Tricount surfaces.
- Contrast checks for the active review build screenshots.
- Reduced Motion and Differentiate without Color Alone checks for status, vote, notification, and payment states.
- App Store Connect confirmation that Mac with Apple silicon and Apple Vision Pro availability remain disabled for the first release, matching `docs/APP_STORE_AVAILABILITY_DECISIONS.md`.

## Local Localization And Accessibility Label Check

This section records local source and simulator checks only. It supports P1 release hardening, but it does not close the uploaded-build accessibility evidence requirement.

Commands refreshed locally on 2026-06-13:

```bash
plutil -lint iosApp/src/Resources/en.lproj/Localizable.strings iosApp/src/Resources/fr.lproj/Localizable.strings
grep -E '^"[^"]+"[[:space:]]*=' iosApp/src/Resources/en.lproj/Localizable.strings | sed -E 's/^"([^"]+)".*/\1/' | sort > /tmp/wakeve-en-keys.txt
grep -E '^"[^"]+"[[:space:]]*=' iosApp/src/Resources/fr.lproj/Localizable.strings | sed -E 's/^"([^"]+)".*/\1/' | sort > /tmp/wakeve-fr-keys.txt
comm -23 /tmp/wakeve-en-keys.txt /tmp/wakeve-fr-keys.txt
comm -13 /tmp/wakeve-en-keys.txt /tmp/wakeve-fr-keys.txt
xcodebuild -project iosApp/iosApp.xcodeproj -scheme WakeveApp -destination 'platform=iOS Simulator,name=iPhone 17 Pro Max' -only-testing:WakeveTests/FindingsRegressionTests test
```

Observed local result:

- `en.lproj/Localizable.strings` and `fr.lproj/Localizable.strings` both pass `plutil -lint`.
- EN and FR each contain 843 localization keys, with no keys missing in either locale.
- Login accessibility labels and hints for Sign in with Apple, guest access, development skip, Privacy Policy, and Terms of Service are localized through `Localizable.strings` instead of hardcoded English strings.
- `FindingsRegressionTests` passed on iPhone 17 Pro Max simulator; latest result bundle: `/Users/guy/Library/Developer/Xcode/DerivedData/iosApp-apbkkjufflidnaalnmfuwfwijqop/Logs/Test/Test-WakeveApp-2026.06.13_14-17-19-+0200.xcresult`.

## Local SwiftUI Source Accessibility Audit

This section records a local source audit only. It supports P1 release hardening, but it does not close the uploaded-build accessibility evidence requirement.

Commands refreshed locally on 2026-06-13:

```bash
bash -n scripts/audit-ios-accessibility-source.sh
./scripts/audit-ios-accessibility-source.sh --fail-on-findings
bash -n scripts/audit-ios-localization-parity.sh
./scripts/audit-ios-localization-parity.sh --fail-on-findings
plutil -lint iosApp/src/Resources/en.lproj/Localizable.strings iosApp/src/Resources/fr.lproj/Localizable.strings iosApp/src/Resources/es.lproj/Localizable.strings iosApp/src/Resources/it.lproj/Localizable.strings iosApp/src/Resources/pt.lproj/Localizable.strings
```

Observed local result:

- `docs/a11y/ios-accessibility-source-audit-2026-06-13T12-34-59Z.md` reports `0` direct hardcoded `.accessibilityLabel("...")`, `.accessibilityHint("...")`, or `.accessibilityValue("...")` calls under `iosApp/src`.
- `docs/a11y/ios-accessibility-source-audit-2026-06-13T13-09-20Z.md` reports `0` hardcoded accessibility label/hint/value findings after extending the audit to named `accessibilityLabel:`/`accessibilityHint:`/`accessibilityValue:` arguments used by shared controls.
- `docs/a11y/ios-accessibility-source-audit-2026-06-13T13-12-50Z.md` reports `0` hardcoded accessibility strings, `0` single-line text risks, and `0` bare indeterminate `ProgressView()` calls without an accessibility label or explicit hiding.
- `docs/a11y/ios-accessibility-source-audit-2026-06-13T13-24-22Z.md` reports `0` findings after broadening the audit to catch hardcoded accessibility literals inside more complex `accessibilityLabel`/`accessibilityHint` expressions such as ternaries.
- `docs/a11y/ios-accessibility-source-audit-2026-06-13T13-45-18Z.md` reports `0` findings after broadening the audit to catch SwiftUI icon-only `Button` blocks without an accessible label or explicit hiding.
- The audit reports `0` single-line text risks where `.lineLimit(1)` lacks a nearby `.minimumScaleFactor`, `.fixedSize`, `.allowsTightening`, or `.dynamicTypeSize` fallback.
- New release-visible accessibility labels for WakeveAI actions, scenario refresh, transport suggestion/departure, sync pending state, home filters, participant actions, calendar add, and organizer options are localized through `Localizable.strings`.
- Additional VoiceOver hints and labels for AI badges, text/password fields, selection chips, search/dictation actions, participant detail lock state, and scenario comparison actions are localized through EN, FR, ES, IT, and PT `Localizable.strings`.
- Additional labels for icon-only controls in background selection, budget, comments, event creation, meal restrictions, meeting actions, and reusable text-field accessories are localized where new keys were required.
- Release-visible loading copy for event lists, WakeveAI preparation, budget, expenses, and meetings is localized through EN, FR, ES, IT, and PT `Localizable.strings` instead of hardcoded source strings.
- EN, FR, ES, IT, and PT `Localizable.strings` pass `plutil -lint` and are key-complete with `848` keys each, no duplicate keys, and no missing/extra keys versus EN after the ES/IT/PT App Store, moderation, data-management, and third-party-notices backfill.
- `docs/a11y/ios-localization-parity-2026-06-13T13-48-19Z.md` records the same 5-locale key parity using the reusable `scripts/audit-ios-localization-parity.sh --write-report --fail-on-findings` audit, and `scripts/test-critical-release-gates.sh` now runs the parity audit in blocking mode.

2026-06-20 local refresh:

- `docs/a11y/ios-accessibility-source-audit-2026-06-20T20-55-42Z.md` reports `0` hardcoded accessibility string findings, `0` single-line text risks, `0` bare indeterminate `ProgressView()` risks, `0` icon-only button risks, and `0` total findings after the last local corrections.
- `docs/a11y/ios-localization-parity-2026-06-20T20-55-45Z.md` reports EN, FR, ES, IT, and PT key parity with `1831` keys per locale, `0` duplicates, and no missing or extra keys versus EN.
- `scripts/test-critical-release-gates.sh` now runs the iOS accessibility source audit in blocking mode with a temporary output directory and requires a generated report with `| Total | 0 |`.

Local limitation:

- Device Dynamic Type screenshots or UI inspection are still required before claiming Larger Text support. The source audit only proves the checked source no longer contains the audited static regressions.

## Evidence Commands And Checks

Run or record equivalent device evidence before setting `APP_STORE_ACCESSIBILITY_SIGNOFF=true`:

```bash
./scripts/lint-store-metadata.sh --ios-only
APP_REVIEW_PHONE_NUMBER=<APP_REVIEW_PHONE_NUMBER> ./scripts/lint-store-metadata.sh --ios-only
./scripts/audit-ios-accessibility-source.sh --fail-on-findings
rg -n "Do not claim yet|Do not publish|VoiceOver|Dynamic Type|Sufficient Contrast|Reduced Motion|Differentiate without Color Alone" docs/APP_STORE_ACCESSIBILITY_LABELS.md docs/APP_STORE_LAUNCH_CHECKLIST.md docs/APP_STORE_TESTFLIGHT_EVIDENCE.md
```

Record the output or attach screenshots/notes showing:

- Whether Accessibility Nutrition Labels are unpublished or published in App Store Connect.
- If any label is published, the exact device family and feature evidence backing that label.
- iPhone and iPad TestFlight smoke results for dark mode, Dynamic Type, and VoiceOver.
- Mac and Apple Vision Pro availability decisions match any accessibility label choices for those device families.
- Remaining accessibility risks from `docs/ACCESSIBILITY_AUDIT.md` and `docs/a11y/ACCESSIBILITY_AUDIT_iOS.md` are either fixed, not in the review build path, or not claimed in App Store Connect labels.

## Closure Rule

Set `APP_STORE_ACCESSIBILITY_EVIDENCE_COMPLETE=true` only after:

- Accessibility Nutrition Labels are intentionally left unpublished, or every published claim is backed by current device evidence.
- iPhone and iPad TestFlight smoke evidence covers dark mode, Dynamic Type, and VoiceOver.
- Mac and Apple Vision Pro label choices match the availability decisions in `docs/APP_STORE_AVAILABILITY_DECISIONS.md`.
- `APP_STORE_ACCESSIBILITY_SIGNOFF=true` is set only in the final release shell or CI secret store after this file is updated.
