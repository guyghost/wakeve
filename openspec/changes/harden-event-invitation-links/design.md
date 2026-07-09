## Context

An invitation crosses four trust boundaries: organizer to backend issuance, link transport, iOS Universal Link routing, and authenticated membership redemption. The backend is the sole authority for token validity and event membership. The client carries an opaque capability but never decides membership.

## Model

The future `/models/event-invitation-link.machine.ts` source of truth SHALL cover:

- states: `idle`, `issuing`, `shareReady`, `opening`, `awaitingNetwork`, `awaitingAuthentication`, `inspecting`, `readyToRedeem`, `redeeming`, `joined`, `alreadyMember`, `expired`, `revoked`, `invalid`, `forbidden`, `failed`, and `cancelled`;
- events: `ISSUE`, `ISSUED`, `OPEN_LINK`, `NETWORK_AVAILABLE`, `AUTHENTICATED`, `INSPECT`, `REDEEM`, `REDEEMED`, `ROTATE`, `REVOKE`, `RETRY`, and `CANCEL`;
- guards: HTTPS production host, supported path/version, authenticated actor for redeem, invitation validity, event visibility policy, capacity/policy, and stable operation identity;
- effects: backend issue/inspect/redeem/rotate/revoke calls, encrypted pending-link storage, navigation only after a typed result, and sanitized audit logging.

## Security Decisions

- The backend generates at least 128 bits of CSPRNG entropy and exposes an opaque token once. Persistence uses a keyed digest or equivalent protected lookup, never plaintext token logging.
- Tokens are event-scoped, versioned, expiring, revocable, rotatable, and optionally constrained by intended recipient/policy. Rotation invalidates the prior token according to a typed grace policy.
- Inspection returns the minimum safe preview. Private event details require authorization; redeem always requires authentication and current server-side policy checks.
- `join/redeem` uses a stable operation ID and a uniqueness constraint on event membership. Replays return the prior result; concurrent joins cannot duplicate membership.
- Rate limits, enumeration resistance, generic invalid responses, abuse telemetry, and secret redaction apply to issue, inspect, and redeem endpoints.

## Universal Links and AASA

- Shared links use canonical `https://<production-host>/invite/<opaque-token>` URLs.
- Every associated domain serves a valid, unsigned JSON AASA file over HTTPS at `/.well-known/apple-app-site-association` (and optionally root compatibility path), without redirect, authentication, or incorrect content type.
- AASA `appID` is the production Team ID plus bundle ID, with components restricted to supported invitation paths. iOS entitlements list only controlled production/staging domains appropriate to the build.
- Web fallback explains the safe next action without exposing private event content. Sensitive redemption never silently falls back to a custom URL scheme.

## Offline, Error, and Retry

Opening offline stores only the canonical URL/token in platform-protected local storage with bounded lifetime and shows `awaitingNetwork`. Authentication handoff preserves the pending invitation. Inspection and redemption have separate retry budgets; retry reuses the operation ID. No offline state claims the user joined. Expired, revoked, invalid, and forbidden are non-retryable until a new link or permission is supplied.

## Rollout and Rollback

1. Add server schema/endpoints behind a disabled flag and model/RED tests.
2. Deploy and verify AASA on production/staging hosts before shipping associated-domain entitlements.
3. Shadow-issue server invitations while legacy sharing remains authoritative; compare sanitized reconciliation counts.
4. Enable Universal Link opening and redemption for an internal cohort, then TestFlight.
5. Make server opaque links authoritative and disable local token generation.
6. Retire legacy links after expiry horizon.

Each phase has a checkpoint. Rollback disables new issuance/opening while keeping redemption of already-issued valid server tokens available. It never re-enables predictable/local token authority.

## Verification Evidence

Automated proof covers entropy/digest storage, expiry/revoke/rotate, authorization, rate limits, idempotent concurrency, offline/auth handoff, AASA content, entitlements, and routing. Live proof covers production HTTPS/AASA with no redirect, a signed archive, physical-device cold/warm opens, logged-out handoff, expired/revoked links, and TestFlight join/redeem. Evidence contains no raw token or private event content.
