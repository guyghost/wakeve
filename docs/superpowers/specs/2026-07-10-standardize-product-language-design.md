# Standardize Product Language Design

This document records the user-approved design for an exhaustive Wakeve product-language migration. The normative requirements and implementation checklist live in [`openspec/changes/standardize-product-language`](../../../openspec/changes/standardize-product-language/).

## Approved Decisions

- Use one exhaustive change covering Android, iOS, Siri, notifications, accessibility, every supported locale, and CI gates.
- Name the inspiration/template destination **Idées / Ideas**, not Groups or Explorer.
- Use **Notifications** for alerts and updates, with `À traiter` and `Informations` filters.
- Use **Messages** only for event-scoped conversations and comments; keep `Inbox` internal.
- Name AI actions by benefit and label outputs **Proposition à relire** until explicitly accepted.
- Remove social rankings and competitive badges; retain only private, sober preparation milestones.
- Preserve internal domain identifiers and project them explicitly into UI language.

## Behavior Model

```text
domain state + user role + confirmed facts + pending facts + allowed action
    -> localized state title + explanation + next action
```

| Internal | French UI | English UI |
|---|---|---|
| `DRAFT` | Brouillon | Draft |
| `POLLING` | Sondage en cours | Poll in progress |
| `COMPARING` | Options à comparer | Options to compare |
| `CONFIRMED` | Date confirmée | Date confirmed |
| `ORGANIZING` | Détails à préparer | Details to prepare |
| `FINALIZED` | Prêt | Ready |
| `Scenario` | Option | Option |
| `Inbox` | Notifications | Notifications |
| `.groups` | Idées | Ideas |
| AI result | Proposition à relire | Proposal to review |

The projection is deterministic. Copy and LLM output never determine a transition. AI produces reviewable signals or content; existing deterministic models decide whether an explicitly confirmed action is valid.

## Delivery Sequence

1. **Model** the canonical concepts, semantic keys, locales, states, failure modes, and identifier boundaries.
2. **Review** success, errors, cancellation, retries, permissions, offline and sync conflicts, terminal states, long text, and accessibility.
3. **Implement** tests first, then localization foundations, Android, iOS/Siri, notifications, accessibility, and private milestones.
4. **Verify** with release-blocking contract tests, platform tests, pseudo-localization, TalkBack/VoiceOver, offline/retry scenarios, and release builds.

## Scope Guardrails

- No domain status, API, storage, route, or analytics rename.
- No public leaderboard, competitive score, or generic social discovery.
- No generic chat or task-manager drift.
- No notification success claim before deterministic delivery state supports it.
- No generated output applied without explicit user action and deterministic validation.

## Normative Artifacts

- [Proposal](../../../openspec/changes/standardize-product-language/proposal.md)
- [Architecture and migration design](../../../openspec/changes/standardize-product-language/design.md)
- [Implementation checklist](../../../openspec/changes/standardize-product-language/tasks.md)
- [Product-language specification](../../../openspec/changes/standardize-product-language/specs/product-language/spec.md)
