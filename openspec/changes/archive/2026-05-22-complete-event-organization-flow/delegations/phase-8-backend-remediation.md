# Delegation: Phase 8 Backend Remediation

Remediate the final Phase 8.2 backend review blockers before closure review.

## Context
- Change: `complete-event-organization-flow`
- Phase: `8. Documentation and Closure`
- Blocking task: `8.2`
- Remediation tasks: `8.5` through `8.7`

## Blockers
1. `PUT /api/events/{id}/status` must require an authenticated organizer actor for every status transition, including `FINALIZED`, before finalization readiness is evaluated.
2. Budget baseline and budget-item mutations must be guarded by organization workflow state. `ORGANIZING` is mutable by the organizer; `FINALIZED` is read-only; pre-organizing states must reject these mutations.

## Required Flow
1. `@tests` adds RED backend coverage for the two blockers.
2. `@codegen` fixes only backend production code needed to satisfy that coverage without widening access.
3. `@review` performs a read-only re-review and explicitly states whether `8.2` can be checked.
4. The orchestrator records verification commands/results in `tasks.md` before running the final agreed suite.

## Verification Commands
```bash
./gradlew :server:test --tests 'com.guyghost.wakeve.routes.EventOrganizationPhase2BackendRoutesTest' --tests 'com.guyghost.wakeve.routes.EventOrganizationPhase5RoutesTest' --no-daemon --no-configuration-cache --rerun-tasks --max-workers=1 -Dkotlin.compiler.execution.strategy=in-process
./gradlew :server:test --no-daemon --no-configuration-cache
git diff --check
openspec validate complete-event-organization-flow --strict
```
