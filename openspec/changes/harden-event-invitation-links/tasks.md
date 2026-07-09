## 1. Model and review

- [ ] 1.1 `@codegen` Create `/models/event-invitation-link.machine.ts` with every state, event, guard, effect, retry, cancellation, permission, and terminal from `design.md`.
- [ ] 1.2 `@review` Approve nominal, offline, authentication, error, retry, rotation, revocation, expiry, and terminal paths before production code.

## 2. RED tests

- [ ] 2.1 `@tests` Add failing backend tests for opaque issuance, digest storage, authorization, inspection minimization, idempotent redeem, concurrency, expiry, revoke, rotate, and rate limiting.
- [ ] 2.2 `@tests` Add failing iOS tests for Universal Link routing, offline persistence, authentication handoff, retry, cancellation, and truthful terminal UI.
- [ ] 2.3 `@tests` Add failing release gates for AASA URL/status/content/type/body, associated-domain entitlement, production Team ID/bundle ID, and absence of local token generation.

## 3. Server authority

- [ ] 3.1 `@codegen` Add additive invitation schema, protected token lookup, stable operation IDs, membership uniqueness, and sanitized audit records.
- [ ] 3.2 `@codegen` Implement authenticated issue/rotate/revoke and minimal inspect plus authenticated idempotent join/redeem endpoints.
- [ ] 3.3 `@review` Perform security review for entropy, enumeration, IDOR, replay, leakage, rate limits, expiry, and rotation.

## 4. iOS and web routing

- [ ] 4.1 `@codegen` Deploy verified AASA files and safe web fallback for every associated domain.
- [ ] 4.2 `@codegen` Add associated-domain entitlement and state-machine-backed Universal Link routing.
- [ ] 4.3 `@codegen` Render pending network/authentication, preview, redeeming, joined, already-member, expired, revoked, invalid, forbidden, retry, and cancellation states accessibly.

## 5. Migration and release

- [ ] 5.1 Shadow-issue and reconcile server links before authority cutover; record rollback checkpoint per phase.
- [ ] 5.2 Prove legacy/local token creation is disabled only after server link redemption is live and monitored.
- [ ] 5.3 Verify production AASA live, signed archive entitlements, physical-device cold/warm links, logged-out handoff, and TestFlight redemption without exposing raw tokens.
- [ ] 5.4 Run focused and full tests, release gates, `openspec validate harden-event-invitation-links --strict`, and `git diff --check`.
- [ ] 5.5 `@review` Sign off only when all live proofs pass; otherwise keep production readiness blocked.
