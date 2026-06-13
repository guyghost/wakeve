# App Store Product Blocker Approval - Wakeve

Date: 2026-06-13

Status: LOCAL IMPLEMENTATION COMPLETE; RELEASE EVIDENCE PENDING

This packet records the approval decisions for the two product blockers. Account deletion and UGC moderation are now locally implemented and verified, but both remain blocked on uploaded-build evidence and release signoff.

## Scope

| Blocker | OpenSpec Change | App Store Rule | Current State |
| --- | --- | --- | --- |
| AS-09 Account deletion | `openspec/changes/add-in-app-account-deletion/` | Guideline 5.1.1(v) | Local implementation and focused tests present; uploaded-build evidence pending |
| AS-10 UGC moderation | `openspec/changes/add-ugc-moderation-controls/` | Guideline 1.2 | Local implementation, focused tests, iOS discoverability checks, gates, and local final validation present; uploaded-build evidence pending |

## Approval Decisions Required

### AS-09 Account Deletion

Approved local implementation scope:

- User path: Profile -> Data Management -> Delete Account for authenticated users.
- Guest path: Profile -> Data Management -> Delete Guest Data for local-only guest users.
- Backend contract: authenticated and idempotent `DELETE /api/user/delete`.
- Data handling: erase account/profile/session/push-token data and anonymize shared event/comment/chat references that must remain for other participants.
- Sign in with Apple: attempt provider revocation when usable authorization material exists; do not block Wakeve data erasure when provider revocation is unavailable or transiently fails.
- App Store closure: keep `APP_STORE_ACCOUNT_DELETION_EVIDENCE_COMPLETE=false` until tests, simulator/TestFlight evidence, App Review notes, local cleanup, and backend behavior all match the uploaded review build.

### AS-10 UGC Moderation

Approved local implementation scope:

- Moderation model: deterministic server-side filtering, pending-review quarantine for uncertain text, and platform moderator/admin review.
- Surface scope: comments, chat messages, event title/description/custom type text, potential location text, scenario/accommodation/activity/meal/equipment/budget free-text fields, and user-supplied display/profile names.
- User controls: report comments, chat messages, events, and users; block/unblock users from relevant content/user surfaces.
- Delivery behavior: comment/chat/event text is moderated before persistence, broadcast, or regular-user visibility.
- Operations: support contact, response owner, review cadence, escalation path, moderator audit records, and App Review verification steps are recorded.
- App Store closure: keep `APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=false` until filtering, reporting, blocking, moderation audit, support contact, and iOS discoverability are verified against the uploaded review build.

## Recommended Execution Order

1. Approve both OpenSpec proposals or explicitly reject/modify the approval decisions above.
2. Implement AS-09 first because account deletion affects authentication, session cleanup, profile settings, push tokens, and privacy wording.
3. Implement AS-10 second because it spans more UGC write paths and should reuse any audit/security patterns added by AS-09.
4. Run the test and evidence gates in each proposal before marking any task complete.
5. Upload a signed TestFlight build only after local OpenSpec, metadata lint, backend tests, and iOS tests pass.
6. Record TestFlight evidence, then set release-shell signoff variables only for blockers whose evidence files are complete.
7. Run `./scripts/app-store-submission-audit.sh --check-live-urls --run-submission-ready` with real Apple credentials and signing.

## Approval Record

| Decision | Owner | Date | Result | Notes |
| --- | --- | --- | --- | --- |
| AS-09 proposal approved | Local roadmap gate | 2026-06-13 | Accepted for local implementation | Account deletion implementation is now locally verified; uploaded-build evidence remains required before release confirmation |
| AS-10 proposal approved | Local roadmap gate | 2026-06-13 | Accepted for local implementation | UGC moderation implementation is now locally verified; uploaded-build evidence remains required before release confirmation |
| AS-09 implementation evidence accepted | TBD | TBD | Pending | Required before `APP_STORE_ACCOUNT_DELETION_CONFIRMED=true` |
| AS-10 implementation evidence accepted | TBD | TBD | Pending | Required before `APP_STORE_UGC_MODERATION_CONFIRMED=true` |

## Verification Commands

```bash
openspec validate add-in-app-account-deletion --strict
openspec validate add-ugc-moderation-controls --strict
openspec validate --all --strict
APP_REVIEW_PHONE_NUMBER='+33123456789' ./scripts/app-store-submission-audit.sh --skip-preflight
```

## Closure Rule

This packet is complete only when both product blockers have approved OpenSpec proposals, all implementation tasks are checked, evidence files are updated against the uploaded review build, and the final App Store audit exits 0 with live URLs and signed submission readiness enabled.
