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
- TestFlight evidence reference: `docs/APP_STORE_TESTFLIGHT_EVIDENCE.md`
- iOS audit references: `docs/ACCESSIBILITY_AUDIT.md` and `docs/a11y/ACCESSIBILITY_AUDIT_iOS.md`
- App Store Connect decision: leave labels unpublished, or publish only directly tested claims.

## Device Matrix

| Device Family | Required Evidence | Result | Notes |
| --- | --- | --- | --- |
| iPhone | Dark mode, larger Dynamic Type, VoiceOver, contrast, reduced motion, and color-only status checks for primary flows if any label is claimed. | Pending | TBD |
| iPad | Same as iPhone plus iPad layout, pointer/keyboard navigation, and split-view or large-screen behavior if available. | Pending | TBD |
| Mac with Apple silicon | Either opt out/leave labels unclaimed, or verify Designed for iPad/iPhone runtime keyboard, VoiceOver, resizing, and primary workflows. | Pending | TBD |
| Apple Vision Pro | Either opt out/leave labels unclaimed, or verify compatibility runtime accessibility behavior in an approved test environment. | Pending | TBD |

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
- Mac with Apple silicon and Apple Vision Pro accessibility evidence if those availability options remain enabled.

## Evidence Commands And Checks

Run or record equivalent device evidence before setting `APP_STORE_ACCESSIBILITY_SIGNOFF=true`:

```bash
./scripts/lint-store-metadata.sh --ios-only
APP_REVIEW_PHONE_NUMBER=<APP_REVIEW_PHONE_NUMBER> ./scripts/lint-store-metadata.sh --ios-only
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
