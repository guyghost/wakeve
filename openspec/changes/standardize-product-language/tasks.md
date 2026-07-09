## 1. Model

- [ ] 1.1 Inventory all user-visible and accessibility strings across Android, iOS, Siri/App Intents, shared KMP surfaces, backend notifications, widgets/extensions, and every supported locale.
- [ ] 1.2 Create the canonical concept and semantic-key registry, including the event-state projection matrix and supported-locales manifest.
- [ ] 1.3 Record the internal identifier allowlist and prove that domain statuses, `Scenario`, `Inbox`, APIs, storage, routes, and analytics remain presentation-independent.
- [ ] 1.4 Model notification locale selection, fallback, retry, offline queueing, and deep-link behavior.
- [ ] 1.5 Model accessibility strings as action + target + relevant state, including errors, permissions, loading, and terminal states.

## 2. Review

- [ ] 2.1 Review every canonical concept in all locales for product belonging, expectation fit, spoken naturalness, compact-display readability, and glossary consistency.
- [ ] 2.2 Review nominal, validation-error, cancellation, retry, permission-denied/restricted, offline, sync-conflict, and terminal projections.
- [ ] 2.3 Review that no free text or LLM output selects a transition and that every AI apply action remains owned by a deterministic validator/use case/state machine.
- [ ] 2.4 Review the `Idées` destination against the product-excellence boundary so it contains event preparation ideas/templates rather than generic social discovery.
- [ ] 2.5 Obtain Android, iOS, localization, accessibility, notification, and product review before implementation.

## 3. Test First

- [ ] 3.1 Add failing cross-platform tests for canonical event-state and internal-identifier projections.
- [ ] 3.2 Add failing locale-key parity, fallback, forbidden-visible-term, and production-literal checks for every supported locale.
- [ ] 3.3 Add failing notification tests for recipient locale, offline queue, retry, permission state, and deterministic fallback.
- [ ] 3.4 Add failing Siri/App Intent per-locale resource and invocation coverage tests.
- [ ] 3.5 Add failing TalkBack and VoiceOver semantics tests for action, target, state, long text, and font scaling.
- [ ] 3.6 Add failing private-milestone tests proving absence of rankings and no effect on permissions or workflow state.

## 4. Localization Foundations

- [ ] 4.1 Introduce the shared concept registry and reconcile Android/iOS semantic localization keys.
- [ ] 4.2 Fill every missing key in every supported locale and remove unintended French/English fallback leakage.
- [ ] 4.3 Replace user-visible production literals with localized semantic resources; retain only reviewed allowlisted diagnostics/previews.
- [ ] 4.4 Add pseudo-localization and representative long-text fixtures.

## 5. Android Migration

- [ ] 5.1 Migrate primary navigation and destination copy to `Idées` and the canonical Notifications/Messages taxonomy.
- [ ] 5.2 Migrate creation, polling, option comparison, confirmation, organization, finalization, meals, profile, settings, and all secondary event surfaces.
- [ ] 5.3 Migrate loading, validation, error, cancellation, permission, retry, offline, pending-sync, and terminal copy.
- [ ] 5.4 Replace TalkBack semantics with localized action + target + state labels.
- [ ] 5.5 Verify Material layouts under font scaling, pseudo-localization, compact, medium, and expanded widths.

## 6. iOS and Siri Migration

- [ ] 6.1 Migrate the UI-only tab case `.groups` to `.ideas` and label it `Idées` / `Ideas` without modifying domain workflows.
- [ ] 6.2 Migrate all SwiftUI visible literals, missing catalog keys, state copy, `Scénario`, `Inbox`, AI technology labels, and locale-forced formatters.
- [ ] 6.3 Migrate loading, validation, error, cancellation, permission, retry, offline, pending-sync, and terminal copy.
- [ ] 6.4 Replace VoiceOver semantics with localized action + target + state labels.
- [ ] 6.5 Move Siri/App Intent phrases and responses into native per-locale resources for every supported locale.
- [ ] 6.6 Verify Dynamic Type, VoiceOver, long text, compact devices, and native tab/toolbar behavior.

## 7. Notifications and Messages

- [ ] 7.1 Resolve server and local notification templates from semantic event types and recipient locale, including deterministic fallback.
- [ ] 7.2 Migrate notification history UI to the visible name `Notifications` with `À traiter` and `Informations` filters while keeping `Inbox` internal-only.
- [ ] 7.3 Keep Messages limited to event-scoped conversations/comments and preserve notification-to-message deep links without naming collision.
- [ ] 7.4 Verify quiet hours, permissions, cancellation, delivery failure, retry, offline queueing, duplicate delivery, and terminal workflow notifications.

## 8. AI Language and Private Milestones

- [ ] 8.1 Rename AI entry points by benefit and label outputs `Proposition à relire` in every supported locale.
- [ ] 8.2 Verify AI unavailable, invalid, rejected, cancelled, and retried outcomes retain a deterministic manual path and never imply applied state.
- [ ] 8.3 Remove public leaderboards, contributor rankings, social-archetype badges, and competitive score copy.
- [ ] 8.4 Add only approved private, non-comparative preparation milestones and verify they do not affect permissions or lifecycle state.

## 9. CI Gates and Final Verification

- [ ] 9.1 Make locale parity, fallback, forbidden visible terms, and production literal checks release-blocking with a narrow reviewed allowlist.
- [ ] 9.2 Make Android/iOS state projection, notification, Siri, accessibility, and private-milestone contract tests release-blocking.
- [ ] 9.3 Run shared, Android, iOS, server, offline/sync, and release-build test suites.
- [ ] 9.4 Perform manual TalkBack and VoiceOver passes plus pseudo-localized/long-text visual verification on representative phones and larger layouts.
- [ ] 9.5 Confirm no business logic, state transition, permission, route, persistence contract, or analytics identifier has moved into or been derived from copy.
- [ ] 9.6 Update language-system documentation and record review evidence before marking the change complete.
