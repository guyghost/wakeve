## MODIFIED Requirements

### Requirement: Scenario Management State Machine
The system SHALL implement a state machine for scenario management following the MVI/FSM pattern, centralizing all state and actions in `ScenarioManagementStateMachine`.

#### Scenario: Load scenarios successfully
- **WHEN** user opens the scenario list
- **THEN** `LoadScenarios` intent is automatically dispatched
- **AND** `isLoading = true` initially
- **WHEN** scenarios are loaded successfully
- **THEN** `isLoading = false`
- **AND** `scenarios` contains the full list
- **AND** `votingResults` contains current votes
- **AND** `error = null`

#### Scenario: Create scenario flow
- **WHEN** user creates a new scenario
- **THEN** `CreateScenario(scenario)` intent is dispatched
- **AND** `isLoading = true` during creation
- **WHEN** creation succeeds
- **THEN** `isLoading = false`
- **AND** the new scenario is added to `scenarios`
- **AND** `NavigateTo("detail/{scenarioId}")` side effect is emitted

#### Scenario: Vote on scenario
- **WHEN** user votes for a scenario
- **THEN** `VoteScenario(scenarioId, vote)` intent is dispatched
- **AND** the vote (PREFER, NEUTRAL, AGAINST) is recorded
- **AND** `votingResults` is updated for the scenario
- **AND** `ScenarioLogic.calculateBestScenario()` is called
- **AND** the highest-scoring scenario is highlighted

#### Scenario: Compare multiple scenarios
- **WHEN** user requests a comparison
- **THEN** `CompareScenarios([scenarioId1, scenarioId2, ...])` intent is dispatched
- **AND** `comparison` is calculated with side-by-side details
- **AND** scores are shown for each scenario
- **AND** `NavigateTo("comparison")` side effect is emitted

#### Scenario: Delete scenario with confirmation
- **WHEN** user deletes a scenario
- **THEN** `DeleteScenario(scenarioId)` intent is dispatched
- **AND** the scenario is removed from `scenarios`
- **AND** `ShowToast("Scénario supprimé")` side effect is emitted
- **AND** if it was the selected scenario, `selectedScenario = null`

## ADDED Requirements

### Requirement: Scenario Comparison State
The system SHALL manage a scenario comparison state to allow users to visualize differences.

#### Scenario: Comparison calculation
- **WHEN** multiple scenarios are selected for comparison
- **THEN** details are extracted for each scenario (date, location, budget, participants)
- **AND** scores are calculated via `ScenarioLogic.calculateScenarioScore()`
- **AND** scenarios are sorted by descending score
- **AND** the best scenario is visually highlighted (★ Best Score)

#### Scenario: Comparison UI rendering
- **WHEN** the comparison screen is displayed
- **THEN** a side-by-side table is rendered with:
  - Name of each scenario
  - Date or period
  - Location
  - Duration
  - Estimated budget per person
  - Estimated participant count
  - Total score
  - Aggregated votes (PREFER, NEUTRAL, AGAINST)
- **AND** the user can scroll horizontally and vertically

### Requirement: Scenario Voting State
The system SHALL manage the voting state on scenarios in real-time.

#### Scenario: Real-time vote aggregation
- **WHEN** a participant votes for a scenario
- **THEN** the vote is immediately recorded
- **AND** `votingResults[scenarioId]` is updated
- **AND** the scenario score is recalculated (PREFER=2, NEUTRAL=1, AGAINST=-1)
- **AND** the best scenario can change dynamically

#### Scenario: Vote validation
- **WHEN** a user attempts to vote
- **THEN** the system checks that the user is a participant of the event
- **AND** if the event is not in `COMPARING` status, the action is rejected
- **AND** `ShowToast("Vote non permis")` side effect is emitted on error

### Requirement: Scenario Selection State
The system SHALL manage the scenario selection state when the organizer selects one.

#### Scenario: Organizer selects scenario
- **WHEN** the organizer selects a scenario as final
- **THEN** `SelectScenario(scenarioId)` intent is dispatched
- **AND** the scenario status changes to `SELECTED`
- **AND** other scenarios change to `REJECTED`
- **AND** the event status changes from `COMPARING` to `CONFIRMED`
- **AND** `NavigateTo("detail")` side effect is emitted

#### Scenario: Non-organizer cannot select
- **WHEN** a non-organizer participant attempts to select a scenario
- **THEN** the action is rejected
- **AND** `ShowToast("Seul l'organisateur peut sélectionner")` side effect is emitted
