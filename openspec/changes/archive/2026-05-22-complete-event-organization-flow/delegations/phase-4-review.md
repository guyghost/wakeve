# Delegation: @review Phase 4 Transport Remediation

## Scope
Read-only review after `@tests` and `@codegen` complete the Phase 4 remediation loop.

## Context
- Change: `complete-event-organization-flow`
- Phase: `4. Transport Planning`
- Source of truth: `phase-4-remediation.md`
- Related task: `4.7`

## Review Focus
1. Shared/local actor guards:
   - `generatePlan` cannot be called without an explicit organizer actor.
   - rejected generation writes no plans, routes, or pending sync metadata.
2. Android repository-backed direct entry:
   - transport destination, event status, organizer identity, participant access, and confirmed date survive direct route/process restart from local repositories.
   - no `DRAFT` fallback disables valid persisted transport planning.
3. Shared organizer departure access:
   - organizer can write departures only for confirmed participants.
   - unknown/unconfirmed targets persist nothing and queue no sync.
4. Offline-first sync:
   - successful transport mutations queue replayable sync metadata.
   - rejected mutations queue no sync metadata.
5. Workflow:
   - no Phase 5 work has started.
   - `DRAFT -> POLLING -> CONFIRMED -> COMPARING -> ORGANIZING -> FINALIZED` guards remain intact.

## Evidence To Check
- New RED tests added by `@tests`.
- Production fixes by `@codegen`.
- Verification command output listed in `phase-4-codegen.md`.
- `openspec validate complete-event-organization-flow --strict`.

## Return Format
- Findings first, ordered by severity, with file/line references.
- Explicit statement: "Phase 4 can be checked" or "Phase 4 cannot be checked".
- If Phase 4 can be checked, list the tasks that may be updated in `tasks.md`.
