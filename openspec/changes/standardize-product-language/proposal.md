# Change: Standardize Wakeve Product Language

## Why

Wakeve exposes inconsistent, untranslated, technical, and sometimes socially competitive language across Android, iOS, Siri, notifications, and accessibility semantics. The inconsistency obscures event state, creates false expectations, and allows internal identifiers such as `Scenario`, `Inbox`, and AI-provider terminology to leak into the interface.

This change establishes a single deterministic product-language contract before implementation. It applies the mandatory **Model → Review → Implement → Verify** sequence: model the projection from domain state to user language, review every state and failure path, migrate each surface from that model, and enforce the contract in tests and CI.

## What Changes

- Add an exhaustive product-language capability covering Android, iOS, Siri, server-originated notifications, accessibility semantics, and every supported locale.
- Preserve domain, persistence, API, route, analytics, and state-machine identifiers while defining explicit user-facing projections.
- Project event states as `Brouillon`, `Sondage en cours`, `Options à comparer`, `Date confirmée`, `Détails à préparer`, and `Prêt` in French, with equivalent canonical concepts in every locale.
- Rename the user-visible `Groups` / `Explorer` destination to **Idées / Ideas** and migrate the internal iOS tab identifier from `.groups` to `.ideas` without changing event workflow state.
- Reserve **Notifications** for alerts and updates, with `À traiter` and `Informations` filters; reserve **Messages** for event-scoped conversations and comments; keep `Inbox` internal-only.
- Present `Scenario` as **Option** in UI while keeping `Scenario` in domain contracts.
- Name AI-assisted actions by their immediate benefit, such as `Préparer des options`, `Résumer les réponses`, `Proposer un message`, and `Compléter la liste`, and mark outputs **Proposition à relire** until explicitly accepted.
- Remove public rankings and socially competitive badges; retain only private, sober event-preparation milestones such as first event, regular voting, and organization ready.
- Localize notification payloads according to recipient locale and localize Siri through native per-locale resources.
- Replace visible production literals and incomplete catalogs with canonical localization keys across all supported locales.
- Require accessibility labels to express action, target, and relevant state in the active locale.
- Add CI gates for locale-key parity, forbidden user-visible terminology, production literals, state projection, notification/Siri coverage, long text, and accessibility semantics.

## Product Excellence Fit

This change directly helps private groups prepare events by making decisions, pending work, responsible actors, and next actions immediately understandable. It reduces mental load and external clarification, keeps compact mobile copy predictable, and increases confidence in offline and synchronization states. `Idées` remains an event-preparation destination rather than a social feed; Messages remain event-scoped; notifications do not become a generic task manager; and private milestones replace public competition. AI language describes reviewable assistance, while deterministic models retain ownership of every state transition.

## Impact

- Affected specs: `product-language` (new), `ios-design-system`.
- Related existing specs: `cross-platform-organization-ux`, `product-excellence`, `workflow-coordination`, `scenario-management`, `notification-management`, `wakeve-ai`, `android-ui-system`, and `collaboration-management`.
- Affected implementation surfaces: Android Compose resources and semantics; iOS String Catalogs, SwiftUI views, App Intents/Siri resources, and VoiceOver semantics; shared KMP presentation models; backend notification composition; localization and CI scripts.
- No domain event status, persistence schema, route, API contract, or analytics event is renamed by this proposal except the UI-only iOS tab case `.groups` → `.ideas`.
- Implementation is approval-gated and MUST NOT begin until this proposal and its explicit behavior model are reviewed.
