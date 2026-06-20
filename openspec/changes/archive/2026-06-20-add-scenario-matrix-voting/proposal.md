# Change: Add Scenario Matrix Voting

## Why
Wakeve supports multiple proposed time slots and multiple potential destinations, but the current workflow treats date polling and destination scenario comparison as separate steps. For events where the best choice depends on both dimensions, organizers need a mode that lets participants vote directly on complete date-and-destination scenarios.

## What Changes
- Add a `SCENARIO_MATRIX` planning mode alongside the existing time-slot poll flow.
- Generate draft scenarios from every `TimeSlot × PotentialLocation` combination.
- Let organizers edit or remove generated scenarios before publishing them.
- Let participants vote on published scenarios with the existing `PREFER`, `NEUTRAL`, and `AGAINST` votes.
- Selecting the final matrix scenario locks both the event date and destination for downstream organization work.

## Impact
- Affected specs: `event-organization`, `scenario-management`, `workflow-coordination`, `destination-planning`, `offline-sync`
- Affected code after approval:
  - Shared KMP event/scenario models, SQLDelight schema, repositories, state machines, and tests
  - Scenario management Android/iOS UI surfaces for generated scenario metadata
  - Server scenario API request/response contracts
