## 1. Tests First
- [ ] 1.1 Add backend tests for authenticated account deletion success, unauthenticated denial, idempotent repeat deletion, session/token revocation, push token removal, and audit logging.
- [ ] 1.2 Add backend tests for collaborative data handling: personal profile data is erased while shared event records either cascade safely or anonymize deleted-user references.
- [ ] 1.3 Add iOS tests or verifiable UI checks for Profile -> Data Management -> Delete Account discoverability, destructive confirmation, success state, offline authenticated failure, and guest local deletion.
- [ ] 1.4 Add App Store gate tests proving the final audit cannot pass when the route, Profile flow, cleanup evidence, or implementation checklist is missing.

## 2. Backend Implementation
- [ ] 2.1 Add a deletion orchestration service that coordinates user repository deletion, session revocation, push token unregistration, analytics identifier clearing, and audit logging.
- [ ] 2.2 Expose authenticated `DELETE /api/user/delete` and return a stable response that the iOS client can display and tests can assert.
- [ ] 2.3 Add provider-revocation handling for Sign in with Apple where authorization material is available, without blocking Wakeve data erasure on provider transient failures.
- [ ] 2.4 Add deletion/anonymization coverage for shared event, participant, comment, chat, notification, session, token, sync metadata, and profile-owned records.

## 3. iOS Implementation
- [ ] 3.1 Add an `AuthenticationService.deleteAccount()` API call that sends the stored bearer token and handles success, auth failure, offline failure, and server errors.
- [ ] 3.2 Add a Profile Data Management screen with Delete Account for authenticated users and Delete Guest Data for guest users.
- [ ] 3.3 Clear Keychain tokens, cached profile values, local synced user data, and analytics identifiers after successful deletion or guest data deletion.
- [ ] 3.4 Return the user to the unauthenticated onboarding/login state after deletion completes.

## 4. App Store Readiness
- [ ] 4.1 Update public privacy/support wording to describe the in-app deletion path once implementation is verified.
- [ ] 4.2 Update `docs/APP_STORE_REVIEW_GUIDELINE_AUDIT.md`, `docs/APP_STORE_READINESS.md`, and `docs/APP_STORE_LAUNCH_CHECKLIST.md` with deletion evidence and App Review steps.
- [ ] 4.3 Update App Store audit/linter gates so `APP_STORE_ACCOUNT_DELETION_CONFIRMED` can only pass when the iOS flow, backend route, local cleanup, and tests are present.
- [ ] 4.4 Run OpenSpec validation, metadata linting, backend tests, iOS build/tests or equivalent simulator verification, and final submission audit.
