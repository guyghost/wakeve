# Android UI Polish - 2026-07-07

## Scope

Polish pass on the Android first-run flow and empty home state. This did not change domain workflows, state transitions, permissions, or AI behavior. The UI continues to call the existing authentication, onboarding completion, and event creation callbacks.

## Device And Build

- Device: `Wakeve_Audit_API_32` emulator, Android 12, `1080x2340`, density `440`
- Build installed with `./gradlew :composeApp:installDebug --no-configuration-cache`
- Package: `com.guyghost.wakeve`

## Evidence

Before:

- `docs/audits/android-ui-screenshots/2026-07-07/01-launch.png`
- `docs/audits/android-ui-screenshots/2026-07-07/02-after-commencer.png`
- `docs/audits/android-ui-screenshots/2026-07-07/03-after-skip.png`
- `docs/audits/android-ui-screenshots/2026-07-07/07-home.png`

After:

- `docs/audits/android-ui-screenshots/2026-07-07-polished/01-launch-polished.png`
- `docs/audits/android-ui-screenshots/2026-07-07-polished/02-auth-polished.png`
- `docs/audits/android-ui-screenshots/2026-07-07-polished/03-onboarding-polished.png`
- `docs/audits/android-ui-screenshots/2026-07-07-polished/04-home-empty-polished.png`

## Changes Shipped

- Replaced emoji feature markers on the get-started and onboarding screens with Material icons.
- Removed the get-started screen fade-in that initially hid content.
- Moved get-started and onboarding copy into Android string resources for French and English.
- Added heading semantics and a localized logo content description on the get-started flow.
- Reworked OAuth buttons to use Material shapes, tokenized colors, and stable minimum touch height.
- Added a visible onboarding `Passer` action using the existing completion callback.
- Replaced the empty home stack of diagnostic cards with one focused empty state and a single `Créer un événement` action.
- Rewrote empty workspace model copy to avoid internal labels such as `Utilité : a activer`.

## Verification

- `git diff --check`
- `./gradlew :composeApp:compileDebugKotlinAndroid --no-configuration-cache`
- `./gradlew :composeApp:testDebugUnitTest --tests com.guyghost.wakeve.ui.event.EventWorkspaceModelsTest --no-configuration-cache`
- `./gradlew :composeApp:installDebug --no-configuration-cache`
- Manual emulator pass through launch, auth, onboarding, and empty home with fresh app data.

## Remaining Observations

- OAuth provider buttons still use local Material placeholders because no official provider brand assets are present in the project.
- The home filter row can crop the last chip at narrow widths; it remains scrollable, but a stronger edge affordance would make that clearer.
- Several downstream workspace summary labels still read as product diagnostics when real events exist. They are no longer shown in the fully empty first-run state.
