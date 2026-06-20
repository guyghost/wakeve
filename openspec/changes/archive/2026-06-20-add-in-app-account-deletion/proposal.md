# Change: Add In-App Account Deletion

## Why
Wakeve supports account creation through Apple, Google, and email OTP, so App Store Review Guideline 5.1.1(v) requires users to be able to initiate account deletion inside the app. The current App Store readiness audit correctly blocks submission because the iOS Profile surface and backend routes do not yet expose a verified, end-to-end deletion flow.

## What Changes
- Add a reachable iOS Delete Account flow from Profile settings / data management.
- Add an authenticated backend account deletion endpoint that deletes or anonymizes user-owned personal data, revokes sessions/tokens, unregisters push tokens, and records audit evidence.
- Clear local iOS credentials, profile cache, synced local data, and analytics identifiers after successful deletion.
- Handle guest mode locally without requiring a backend call.
- Update App Store readiness gates, public privacy wording, and verification docs once the flow is implemented and tested.

## Approval Checklist
- Approve the product path: Profile -> Data Management -> Delete Account for authenticated users, and Profile -> Data Management -> Delete Guest Data for guest users.
- Approve collaborative data semantics: erase account/profile/session/push-token data, anonymize deleted-user references in shared event/comment/chat history, and hard-delete only records that are not shared with other participants.
- Approve the backend contract: authenticated, idempotent `DELETE /api/user/delete` with a stable response body that includes deletion status, local-cleanup guidance, and provider-revocation outcome.
- Approve Sign in with Apple behavior: attempt provider revocation only when usable authorization material exists, and never retain Wakeve account data solely because provider revocation is unavailable or transiently fails.
- Approve App Store evidence requirements: do not set `APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=true` or `APP_STORE_ACCOUNT_DELETION_CONFIRMED=true` until backend tests, iOS verification, local cleanup proof, and reviewer notes all match the uploaded review build.

## Impact
- Affected specs: `user-auth`, `security-management`
- Affected code:
  - `iosApp/src/Views/Profile/ProfileTabView.swift`
  - `iosApp/src/ViewModels/ProfileViewModel.swift`
  - `iosApp/src/Services/AuthenticationService.swift`
  - `iosApp/src/Services/SecureTokenStorage.swift`
  - `server/src/main/kotlin/com/guyghost/wakeve/routes/*`
  - `server/src/main/kotlin/com/guyghost/wakeve/auth/*`
  - shared auth/session/user repositories and tests
  - App Store readiness scripts/docs
