# Change: Add UGC Moderation Controls

## Why
Wakeve exposes user-generated content through event comments, chat, event text, locations, and planning details. App Store Review Guideline 1.2 requires apps with user-generated content to provide moderation protections such as filtering, reporting, blocking, published contact information, and a timely response process before submission.

## What Changes
- Add server-side moderation checks for comments, chat messages, and event text before content is persisted or broadcast.
- Add report flows for comments, chat messages, events, and users with stable identifiers that support reviewer and moderator follow-up.
- Add block controls so a user can prevent continued unwanted contact from another user.
- Add moderator review state and audit logging for reported content and enforcement actions.
- Add iOS reviewer-visible paths for reporting content/users and blocking users.
- Update App Store readiness gates, App Review notes, and launch checklist only after implementation evidence exists.

## Approval Checklist
- Approve the first-release moderation model: deterministic server-side filtering, pending-review quarantine for uncertain text, and platform moderator/admin review rather than organizer-only enforcement.
- Approve the UGC surface scope: comments, chat messages, event title/description/custom type text, potential location text, scenario/accommodation/activity/meal/equipment/budget free-text fields, and profile/display names where users supply them.
- Approve the user controls: report comments, chat messages, events, and users with stable target identifiers; block/unblock users from relevant content/user surfaces; show pending/hidden/rejected content states when applicable.
- Approve operational evidence: support contact, response owner, review cadence, escalation path, moderator audit records, and TestFlight/App Review verification steps must be recorded before final signoff.
- Approve App Store closure rules: do not set `APP_STORE_UGC_MODERATION_EVIDENCE_COMPLETE=true` or `APP_STORE_UGC_MODERATION_CONFIRMED=true` until filtering, reporting, blocking, moderation audit, support contact, and iOS discoverability are verified against the uploaded review build.

## Impact
- Affected specs: `collaboration-management`, `security-management`
- Affected code:
  - `shared/src/commonMain/kotlin/com/guyghost/wakeve/models/CommentModels.kt`
  - `shared/src/commonMain/kotlin/com/guyghost/wakeve/chat/*`
  - `server/src/main/kotlin/com/guyghost/wakeve/routes/CommentRoutes.kt`
  - `server/src/main/kotlin/com/guyghost/wakeve/routing/ChatService.kt`
  - `server/src/main/kotlin/com/guyghost/wakeve/routing/ChatWebSocket.kt`
  - event creation/update routes that accept user text
  - iOS comment/chat/event detail/profile surfaces
  - App Store readiness scripts/docs
