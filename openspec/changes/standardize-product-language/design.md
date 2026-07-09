## Context

Wakeve currently has useful naming guidance but no exhaustive, executable contract spanning platforms, delivery channels, locales, and accessibility. UI copy can therefore bypass localization, expose internal concepts, disagree between Android and iOS, or imply that a queued, generated, or failed action is complete.

The model is a deterministic projection:

```text
domain state + user role + confirmed facts + pending facts + allowed action
    -> canonical concept + localized title + explanation + next action
```

An LLM may propose content, but it never selects the workflow state, permission, delivery status, or projection branch.

## Goals / Non-Goals

### Goals

- Establish one canonical concept registry and locale-complete key registry.
- Make state, pending work, responsible actor, and next action clear on primary screens.
- Keep Android, iOS, Siri, notifications, and accessibility semantics conceptually aligned.
- Cover success, validation error, cancellation, retry, permission denial, offline queueing, synchronization conflict, and terminal states.
- Prevent recurrence with automated release gates.

### Non-Goals

- Rename domain models, database columns, API fields, routes, analytics identifiers, or state-machine statuses.
- Change event lifecycle transitions or authorization rules.
- Add a social feed, generic task system, generic chat, or public gamification.
- Let generated language apply changes without explicit user action and deterministic validation.

## Behavior Model

### Canonical state projection

| Internal identifier | Canonical French UI | Canonical English UI | Required context |
|---|---|---|---|
| `DRAFT` | Brouillon | Draft | Missing information and next editable step |
| `POLLING` | Sondage en cours | Poll in progress | Response progress, deadline, actor who should respond |
| `COMPARING` | Options à comparer | Options to compare | Options remaining and comparison/selection action |
| `CONFIRMED` | Date confirmée | Date confirmed | Confirmed date and next preparation action |
| `ORGANIZING` | Détails à préparer | Details to prepare | Complete, incomplete, optional, and responsible sections |
| `FINALIZED` | Prêt | Ready | Confirmed summary; no editable action unless explicitly authorized |
| pending local mutation | En attente de synchronisation | Waiting to sync | Local availability and retry/reconnect guidance |
| failed mutation | Modification non enregistrée | Change not saved | Cause when safe, retry action, preserved input |

Translations in other supported locales MUST preserve these concepts and expectations rather than translating internal enum names literally.

### Identifier boundary

| Internal/source-of-truth term | User-facing concept | Boundary |
|---|---|---|
| `Scenario` | Option | Domain/API/storage/analytics stay unchanged |
| `Inbox` | Notifications | Type and implementation names may remain internal |
| `.groups` | Idées / Ideas | UI-only tab identifier migrates to `.ideas`; no group domain migration |
| AI provider or `Wakeve AI` | Contextual benefit | Provider metadata remains internal and reviewable |
| generated result | Proposition à relire / Proposal to review | Never implies applied or confirmed state |

### Notifications and messages

- **Notifications** contain alerts and updates and expose `À traiter` / `To do` and `Informations` / `Information` filters.
- **Messages** contain event-scoped conversations and comments.
- A notification may deep-link to a message but MUST NOT cause the two concepts to share a visible name.
- `Inbox` MUST NOT be displayed.

### AI-assisted language

AI entry points name the benefit: `Préparer des options`, `Résumer les réponses`, `Proposer un message`, or `Compléter la liste`. Every output that could influence event work is labeled `Proposition à relire`, exposes safe provenance/validation metadata where appropriate, and requires explicit acceptance through an existing deterministic use case or state machine.

### Private milestones

Public leaderboards, rankings, competitive contributor scores, and social archetype badges are removed. Optional milestones are private, event-preparation-specific, non-comparative, and sober, such as `Premier événement`, `Vote régulier`, and `Organisation prête`. They neither unlock permissions nor modify workflow state.

## Review Matrix

Before implementation, reviewers MUST verify every projection for:

| Case | Required behavior |
|---|---|
| Nominal success | Result-specific confirmation and next useful action |
| Validation error | Field/action-specific correction without state change |
| Cancellation | No success copy and no implied persistence |
| Retry | Stable input, explicit progress, idempotent result language |
| Permission denied/restricted | Missing permission, impact, and safe recovery action |
| Offline | Local/pending state distinguished from shared/confirmed state |
| Sync conflict | Deterministic resolution state and truthful affected data |
| Terminal state | Read-only/complete expectation with no invalid CTA |
| AI unavailable/rejected | Manual path remains usable; no state transition |
| Long text/accessibility | No truncation of meaning; semantic label stays contextual |

No transition may be inferred from free text. No copy string may be used as a state discriminator.

## Decisions

### Decision: One product-language registry owns concepts

The repository will contain a documented registry mapping stable semantic keys to canonical concepts and all supported translations. Platform resource formats may differ, but concepts and coverage are shared.

Alternatives considered:

- Platform-owned naming was rejected because it caused semantic drift.
- Runtime machine translation was rejected because it cannot guarantee product meaning, offline behavior, accessibility, or release review.

### Decision: Stable internal identifiers, explicit UI projection

Domain identifiers remain stable to avoid data and workflow migrations. Presentation adapters/resources map them to canonical language. The sole identifier rename in scope is the UI-only iOS navigation case `.groups` → `.ideas`, because the existing case misrepresents its destination.

### Decision: Recipient-locale notification composition

The notification system resolves semantic template keys using the recipient’s stored locale, with a documented fallback locale. Delivery retries reuse the same semantic event and locale decision and do not concatenate ad hoc strings.

### Decision: Native Siri localization

Siri/App Intent phrases and responses use per-locale native resources for every locale supported by the application. Language suffixes inside a single default resource file are not a localization mechanism.

### Decision: Gates are release-blocking

CI fails on missing keys, unexpected fallback, forbidden visible terminology, unapproved production literals, incomplete accessibility semantics, or absent required notification/Siri templates. Explicit allowlists are narrow, reviewed, and limited to internal identifiers, tests, previews, or non-user-visible diagnostics.

## Risks / Trade-offs

- Large migration surface may introduce regressions → split implementation into independently verified lots while retaining one proposal and registry.
- Compact layouts may not fit translated copy → test pseudo-localization, Dynamic Type/font scaling, and representative long locales before release.
- Existing identifiers may be accidentally renamed → add contract tests around domain, persistence, routes, and analytics.
- Literal scanners can create false positives → use syntax-aware or scoped scanning plus a reviewed allowlist.
- Notification locale may be missing/stale → define deterministic fallback and update locale on authenticated device registration.
- Users may confuse `Idées` with generic discovery → constrain content and copy to event preparation ideas and templates.

## Migration Plan

1. **Model**: inventory every visible/semantic string and notification/Siri template; establish the concept registry, semantic keys, projection table, supported-locales manifest, and explicit internal identifier allowlist.
2. **Review**: review nominal, error, cancellation, retry, permission, terminal, offline, and accessibility variants; obtain product, localization, and platform review.
3. **Implement**: first add failing contract tests, then migrate localization foundations, Android, iOS/Siri, notifications/backend, accessibility semantics, and private milestones in bounded lots.
4. **Verify**: run locale parity and forbidden-term gates, platform tests, state projection tests, notification/Siri tests, pseudo-localization and long-text checks, TalkBack/VoiceOver checks, offline/retry tests, and release builds.
5. Rollback per lot by restoring the previous presentation mapping only; never roll back domain state or data because those contracts are unchanged.

## Open Questions

None. The product decisions for `Idées`, Notifications versus Messages, benefit-led AI language, review labels, and private milestones were explicitly approved before this proposal.
