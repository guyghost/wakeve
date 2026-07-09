# Change: Harden event invitation links

## Why

iOS currently cannot prove that a shared invitation is a durable, server-issued invitation that can be opened securely through Universal Links and redeemed exactly once according to event membership policy. Local or predictable tokens, unavailable AASA files, and unmodeled offline/error behavior can produce broken links or unauthorized joins.

## What Changes

- Model invitation issuance, opening, inspection, redemption, rotation, revocation, expiry, retry, and terminal outcomes before code.
- Issue opaque, high-entropy invitation tokens only on the backend; store only a protected digest and sanitized audit identifiers.
- Make `join/redeem` an authenticated, authorized, idempotent backend transition with explicit already-member and terminal results.
- Configure HTTPS Universal Links and valid AASA files for every production host; use no custom-scheme downgrade for sensitive redemption.
- Preserve a pending link offline and retry inspection/redemption without claiming membership before server acknowledgement.
- Require live production-host, device, archive, and TestFlight evidence before readiness.

## Product Excellence Fit

Invitation links directly reduce private-group coordination outside Wakeve. The organizer is responsible for issue, rotation, and revocation; the invited authenticated user is responsible for redeem or cancel; the backend is responsible for validity and membership. Each screen exposes one next action: share, sign in, retry network, join, request a new link, or open the joined event. Warm-link handling targets immediate rendering, and cold/auth handoff shows progress without duplicate taps. Controls, status, errors, and next actions must support VoiceOver, Dynamic Type, sufficient contrast, Reduce Motion, and platform-native focus. The flow shows valid, pending, redeemed, expired, revoked, and failed states without generic social, chat, task, or workspace behavior. No AI or LLM issues, validates, rotates, or redeems invitations.

## Scope

In scope: server issuance and storage, shareable HTTPS link, Universal Links/AASA, secure preview, authenticated join/redeem, rotation/revocation/expiry, offline pending state, retries, audits, and live release proof.

Out of scope: public event discovery, generic referral links, invitation-message generation, Android App Links beyond compatibility of the same HTTPS URL, and implementation before approval.

## Impact

- Affected specs: `collaboration-management`, `security-management`, `offline-sync`, `public-web-presence`.
- Expected future surfaces: backend invitation persistence/routes, iOS Universal Link routing and entitlements, AASA deployment, shared typed contracts, and release gates.
- Dependency: production domains, TLS, Apple Team ID/bundle identifier, authentication handoff, and server membership authority.

## Approval Gate

This proposal is pending approval. No production implementation may start until strict validation, human approval, model creation/review, and RED tests are complete.
