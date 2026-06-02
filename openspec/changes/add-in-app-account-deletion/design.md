## Context
Apple requires account deletion to be available inside apps that offer account creation. Wakeve currently documents the blocker and avoids making unsupported public claims, but the app still needs a real user-facing flow plus server-side deletion semantics before submission.

The existing specs already mention GDPR deletion, and `UserRepository.deleteUser(id)` exists as a low-level interface. The missing work is the product and security contract around when deletion can be requested, how far deletion reaches, how collaborative records are preserved without retaining personal data, and what evidence App Review can verify.

## Goals / Non-Goals
- Goals:
  - Make account deletion discoverable from iOS Profile settings.
  - Require authenticated confirmation for server-backed account deletion.
  - Delete or anonymize personal data across auth, sessions, notifications, local cache, and user-owned profile records.
  - Preserve collaborative event integrity only through non-identifying tombstones where deletion would otherwise break shared event history.
  - Clear guest-mode local data without a server round trip.
  - Provide automated and manual evidence for App Store submission.
- Non-Goals:
  - Build user data export in this change.
  - Add a web-only deletion form as the primary deletion path.
  - Add a delayed manual support workflow for standard account deletion.
  - Delete unrelated participants' shared event data.

## Decisions
- The user-facing path will be Profile -> Data Management -> Delete Account, with one destructive confirmation screen that states the irreversible scope.
- The backend endpoint will be authenticated and idempotent, exposed as `DELETE /api/user/delete` to match the existing `security-management` spec text.
- Guest users will see a local "Delete guest data" action that clears local-only data and returns to onboarding/login.
- Authenticated deletion while offline will be blocked with a clear retry message because backend erasure and token/session revocation cannot be proven offline.
- Apple Sign in users will trigger provider revocation when the server has the required authorization material; otherwise the deletion still removes Wakeve data and logs provider-revocation evidence for review.
- Shared event references authored by the deleted user will be anonymized to "Deleted user" where deletion would corrupt collaborative records.

## Current Repository Inventory
- iOS already exposes `settings_sheet.data_management` in `iosApp/src/Views/Profile/ProfileTabView.swift`, but it is currently a non-navigating `ProfilePlainLinkRow`; the approved implementation should replace this with a real Data Management destination rather than adding a second hidden entry point.
- iOS session cleanup is centralized in `iosApp/src/Services/AuthStateManager.swift` and `iosApp/src/Services/AuthenticationService.swift`; logout already unregisters APNs through `APNsService.unregisterToken`, clears Keychain tokens, clears cached profile `UserDefaults`, and clears the local guest session.
- `iosApp/src/Services/APNsService.swift` already calls `DELETE /api/notifications/unregister?platform=ios`; account deletion should either reuse this path before token clearing or have the backend deletion orchestrator remove all push tokens for the authenticated user.
- Server authentication is concentrated in `server/src/main/kotlin/com/guyghost/wakeve/auth/AuthenticationService.kt` and `server/src/main/kotlin/com/guyghost/wakeve/routes/AuthRoutes.kt`; current endpoints cover Google, Apple, email OTP, guest, and refresh, but no account deletion endpoint exists.
- The server-side `com.guyghost.wakeve.repository.UserRepository` has user, token, notification-preference, and sync-metadata operations, including `deleteTokensForUser`, but no full user deletion/anonymization orchestration.
- The shared auth repository interface in `shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/repository/UserRepository.kt` already declares `deleteUser(id)` as a GDPR-facing low-level contract; the App Store blocker is the missing end-to-end product/security flow around that primitive.
- Relevant SQLDelight tables for deletion scope include `user`, `user_token`, `notification_preferences`, `sync_metadata`, `notification_token`, `notification`, `session`, `participant`, `event`, `comment`, `chat_message`, and feature-owned event planning tables that may contain user IDs or user-authored text.

## Resolved Design Answers
- Production data scope should be driven from SQLDelight schema ownership: start with auth/session/push/profile tables, then anonymize user references in collaborative event, participant, comment, and chat records instead of deleting shared event history that other users still need.
- Deleted organizer-owned events should be retained when any other participant or invitee has access; organizer identity should become a non-identifying tombstone such as "Deleted user". Events with no other participant can be hard-deleted only if tests prove no shared references remain.
- Sign in with Apple revocation should be best-effort in the first submission build. If the backend does not store a provider refresh token or authorization code suitable for revocation, the deletion response and audit record should state that Wakeve data was erased and provider revocation material was unavailable.
- Guest deletion should not call the backend for the current iOS local-only guest mode; it should clear `wakeve_guest_user_id`, `wakeve_guest_user_name`, local caches, and any local-only guest event data before returning to onboarding/login.

## Risks / Trade-offs
- Broad cascade deletion can break shared events. Mitigation: distinguish personal data erasure from collaborative record retention and anonymize references.
- Provider token revocation can fail independently of Wakeve deletion. Mitigation: retry where possible, audit the outcome, and never retain Wakeve account data solely because provider revocation failed.
- Local/offline deletion could falsely imply backend erasure. Mitigation: only allow guest local deletion offline; require network for authenticated accounts.
- App Review may reject if the action is hidden behind support contact. Mitigation: make the action directly reachable from Profile settings and document review steps.

## Migration Plan
1. Add tests for backend account deletion scope, idempotency, session revocation, and authorization failures.
2. Add tests for iOS Profile discoverability, confirmation copy, success handling, offline failure, and guest local-data deletion.
3. Implement backend deletion orchestration and wire route registration.
4. Implement iOS client service and Profile UI flow.
5. Update privacy/support docs and App Store audit gates only after implementation evidence exists.

## Open Questions
- Which production datastore tables are authoritative for user-owned backend data at launch, and which currently remain local-only?
- Should deleted organizer-owned events be retained with anonymized organizer identity or hard-deleted when no other participant has access?
- What evidence can the Apple OAuth implementation provide for Sign in with Apple revocation in the first submission build?
