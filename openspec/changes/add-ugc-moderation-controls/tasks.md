## 1. Tests First
- [ ] 1.1 Add server tests that reject hard-policy comments, chat messages, and event text before persistence or broadcast.
- [ ] 1.2 Add server tests that uncertain UGC is hidden from regular users while pending review.
- [ ] 1.3 Add server tests for reporting comments, chat messages, events, and users with stable target identifiers.
- [ ] 1.4 Add server tests for blocking users and suppressing blocked-user content/notifications for the blocker.
- [ ] 1.5 Add tests that moderation/report/block actions produce audit records and enforce moderator/admin authorization.
- [ ] 1.6 Add iOS UI or simulator verification for report/block discoverability from comment/chat/event/user surfaces.
- [ ] 1.7 Add App Store gate tests proving the final audit cannot pass when filtering, reporting, blocking, support contact, moderator audit, or iOS discoverability evidence is missing.

## 2. Server and Shared Models
- [ ] 2.1 Add shared moderation status, report target, report reason, and block models.
- [ ] 2.2 Add persistence for content reports, moderation decisions, and user blocks.
- [ ] 2.3 Add a moderation policy service used by comment, chat, and event text write paths.
- [ ] 2.4 Add authenticated report endpoints and moderator review endpoints.
- [ ] 2.5 Add block/unblock endpoints and apply block filters to comment/chat reads, websocket delivery, and notifications.
- [ ] 2.6 Apply moderation to event title, event description, custom event type text, potential location text, and planning free-text fields that can be authored by users.

## 3. iOS Implementation
- [ ] 3.1 Add report actions on comments, chat messages, and event text surfaces with reason selection and confirmation.
- [ ] 3.2 Add block/unblock actions on user-facing surfaces where abusive interaction can continue.
- [ ] 3.3 Add hidden/pending/rejected states so moderated content does not appear as successfully published when it is not visible to others.
- [ ] 3.4 Add support/help path visibility for abuse contact and reviewer verification.

## 4. App Store Readiness
- [ ] 4.1 Update App Review notes with exact report/block/moderation verification steps once implementation is verified.
- [ ] 4.2 Update `docs/APP_STORE_REVIEW_GUIDELINE_AUDIT.md`, `docs/APP_STORE_READINESS.md`, and `docs/APP_STORE_LAUNCH_CHECKLIST.md` with evidence.
- [ ] 4.3 Update App Store audit/linter gates so `APP_STORE_UGC_MODERATION_CONFIRMED` can only pass when moderation, report, block, contact, and review-process evidence are present.
- [ ] 4.4 Run OpenSpec validation, metadata linting, server tests, iOS build/tests or equivalent simulator verification, and final submission audit.
