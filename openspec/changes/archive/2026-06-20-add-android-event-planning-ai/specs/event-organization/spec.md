## ADDED Requirements
### Requirement: AI Draft Review Before Event Creation
Wakeve MUST present AI-generated event planning data as a reviewable draft and MUST NOT create or persist an event automatically from AI output.

#### Scenario: User reviews extracted fields
- **WHEN** the assistant returns an `EventPlanDraft`
- **THEN** the UI shows extracted fields and missing information for review before the user applies the draft to event creation.
