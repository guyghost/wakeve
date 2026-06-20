# Change: Migrate Android Google auth to Credential Manager

## Why
Android release builds still compile against the deprecated Google Sign-In API (`GoogleSignIn`, `GoogleSignInClient`, and `GoogleSignInOptions`). This keeps auth on an aging API surface and leaves avoidable release warnings in the Android build.

## What Changes
- Replace the Android Google sign-in launcher/provider with AndroidX Credential Manager backed by Google ID credentials.
- Preserve the existing shared auth state-machine contract: Google sign-in still returns a validated token/result to the shared auth flow.
- Keep Apple Sign-In, email OTP, guest mode, token storage, and backend auth contracts unchanged.
- Add regression coverage proving the Android Google provider does not depend on deprecated Google Sign-In classes.

## Impact
- Affected specs: `user-auth`
- Affected code: `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/MainActivity.kt`, `composeApp/src/androidMain/kotlin/com/guyghost/wakeve/auth/GoogleSignInHelper.kt`, `shared/src/androidMain/kotlin/com/guyghost/wakeve/auth/shell/services/`
- Affected tests: Android auth provider tests and release build/gate checks
