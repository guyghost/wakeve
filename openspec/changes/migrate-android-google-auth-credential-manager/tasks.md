## 1. Android Auth Migration
- [ ] 1.1 Add failing Android auth contract coverage for Google sign-in through Credential Manager.
- [ ] 1.2 Replace deprecated `GoogleSignInClient` launch/result handling in `MainActivity`.
- [ ] 1.3 Replace or remove `GoogleSignInHelper` with a Credential Manager based provider.
- [ ] 1.4 Update `AndroidOAuthProvider` / `AndroidAuthService` Android-specific APIs so shared auth receives the same Google auth result semantics.
- [ ] 1.5 Preserve cancellation, provider-unavailable, invalid-token, and retryable-error behavior.
- [ ] 1.6 Add a release gate that fails if deprecated Google Sign-In classes are reintroduced.
- [ ] 1.7 Run Android debug and release compilation plus targeted auth tests.

## 2. Documentation
- [ ] 2.1 Update roadmap/release notes with the migration result and remaining external Google OAuth configuration assumptions.
