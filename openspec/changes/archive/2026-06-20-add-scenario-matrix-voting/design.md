## Context
The existing event lifecycle supports `DRAFT -> POLLING -> CONFIRMED -> COMPARING -> ORGANIZING`. Scenario comparison currently happens after a date has been confirmed, and `Scenario` stores date and destination as text fields.

## Goals
- Preserve the existing time-slot polling flow.
- Add a first-class scenario matrix mode for events where participants should choose a complete date-and-destination option.
- Keep generation deterministic and local-first so organizers can use it offline.

## Decisions
- Add `EventPlanningMode` with `TIME_SLOT_POLL` as the default and `SCENARIO_MATRIX` as the new mode.
- Reuse `COMPARING` for the matrix voting phase rather than adding another `EventStatus`.
- Extend `Scenario` with optional source references to the originating `TimeSlot` and `PotentialLocation`.
- Add `ScenarioGenerationType.MANUAL | MATRIX` and `ScenarioStatus.DRAFT`.
- Make matrix generation idempotent by deduplicating on `(eventId, sourceTimeSlotId, sourcePotentialLocationId)`.

## Non-Goals
- No external destination/lodging provider integration is added by this change.
- No new vote type is introduced.
- No automatic final selection at deadline is introduced.
