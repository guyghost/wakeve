## Context
The App Store readiness audit treats UGC moderation as a blocker because Wakeve has comments and chat routes registered, plus multiple user-editable event fields. Existing collaboration docs mention moderation and a report endpoint, but the inspected code does not yet prove App Review-required filtering, reporting, blocking, or operational review.

## Goals / Non-Goals
- Goals:
  - Prevent or quarantine clearly objectionable text before it is visible to other users.
  - Let users report offensive comments, chat messages, event text, or users from reviewer-visible app paths.
  - Let users block abusive users and suppress future direct interaction from blocked users.
  - Provide moderator review states, audit evidence, and support contact traceability.
  - Preserve offline transparency: queued UGC cannot bypass moderation once synced.
  - Provide App Review notes and TestFlight smoke-test evidence.
- Non-Goals:
  - Build machine-learning classification in the first release.
  - Add public social discovery feeds.
  - Replace organizer/admin moderation with fully automated enforcement.
  - Expose private report details to non-moderator users.

## Decisions
- The first submission build will use a deterministic server-side policy: length validation, normalization, blocked-term matching, spam/link heuristics, and moderation status assignment.
- Content with a hard policy violation will be rejected before persistence; uncertain content can be persisted as `PENDING_REVIEW` and hidden from non-moderators until reviewed.
- Reports will be stored with reporter ID, target type, target ID, reason, optional details, event ID when applicable, timestamp, and review status.
- Blocking will be user-scoped. Blocked users' comments/chat messages will be hidden from the blocker, and direct notifications from blocked users will be suppressed where possible.
- Moderator/admin actions will be audit logged with actor ID, target ID, action, reason, timestamp, and outcome.
- The App Store review build must either enable and prove these controls or disable comments/chat until the controls are verified.

## Current Repository Inventory
- Server comment writes enter through `server/src/main/kotlin/com/guyghost/wakeve/routes/CommentRoutes.kt` and persist in `shared/src/commonMain/kotlin/com/guyghost/wakeve/comment/CommentRepository.kt`; the current create/update paths accept content and author fields without moderation, report, or block checks.
- Server chat writes enter through `server/src/main/kotlin/com/guyghost/wakeve/routing/ChatService.kt`; `sendMessage` persists the message and broadcasts it over WebSocket in the same flow, so moderation must happen before both persistence and broadcast.
- iOS comment UI exists in `iosApp/src/Views/Collaboration/CommentItemView.swift` and `CommentListView.swift`; the current menu offers reply/edit/pin/delete only when the user can edit/delete, and there is no reviewer-visible report or block action for another user's content.
- Shared chat and comment models currently include message status, comment sections, deleted-state behavior, mentions, and reactions, but no `ModerationStatus`, `ReportTarget`, `ReportReason`, `ContentReport`, `UserBlock`, or `ModerationDecision` model.
- The active UGC surfaces for first App Store review are comments, chat messages, event title/description/custom type text, potential location names, scenario/accommodation/activity/meal/equipment/budget descriptions, and profile/display names where users can supply them.
- `docs/APP_STORE_UGC_MODERATION_EVIDENCE.md` already requires either implemented controls or disabled UGC surfaces. No current evidence proves disabled surfaces in the uploaded build, so the preferred path is implementation rather than relying on a review-build shutdown.

## Resolved Design Answers
- First-release moderation review should use a server-side support queue/admin role, not event organizers as platform moderators. Organizers may delete or hide content in their own event as an event-management action, but App Store Guideline 1.2 enforcement evidence should be owned by a platform moderator/admin path.
- Event organizers can keep existing delete/pin controls, but report/block/moderation decisions should be distinct records so App Review can verify abuse reporting and timely response without conflating it with organizer curation.
- Minimum event text fields for the first submission are title, description, custom event type, potential location name/address/notes where present, scenario/accommodation/activity/meal/equipment/budget free-text fields, comments, and chat messages.
- Offline-created UGC should remain locally queued and clearly not visible to others until server moderation accepts it; sync must not publish queued text directly into visible shared state.

## Risks / Trade-offs
- Overzealous filtering can block legitimate event planning text. Mitigation: start with narrow deterministic rules and allow moderator review for uncertain cases.
- Offline-created content can appear to bypass moderation. Mitigation: show queued state locally and require server moderation before synced content becomes visible to others.
- Blocking in group event contexts can be confusing. Mitigation: hide blocked-user content for the blocker while preserving event integrity for other participants.
- App Review needs discoverability. Mitigation: add visible report/block entry points and document exact reviewer steps.

## Migration Plan
1. Add moderation/report/block models and persistence.
2. Add server tests for comment, chat, event-text moderation, reports, blocks, and audit logs.
3. Add iOS UI tests or simulator evidence for report/block discoverability.
4. Wire server moderation into all UGC write paths and websocket broadcasts.
5. Update App Store notes/docs/gates after tests and device verification pass.

## Open Questions
- Which user roles will be able to review reports in the first release: organizer, moderator, admin, or a server-side support queue only?
- Should event organizers be able to hide content inside their own events, or should all enforcement require a platform moderator?
- Which exact event text fields should be moderated before the first App Store submission?
