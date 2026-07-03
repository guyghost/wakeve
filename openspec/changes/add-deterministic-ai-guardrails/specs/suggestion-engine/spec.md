## ADDED Requirements

### Requirement: Deterministic Suggestion Scoring
Suggestion ranking, scoring, filtering, and eligibility decisions SHALL be deterministic functions of typed request data, provider facts, user preferences, and explicit scoring weights. AI MAY generate bounded explanatory copy or rationale only after deterministic scoring has selected the facts it may describe.

#### Scenario: LLM wording changes
- **GIVEN** two AI-generated rationales use different wording for the same scored destination
- **WHEN** Wakeve ranks the suggestions
- **THEN** the ranking and scores remain unchanged
- **AND** only the reviewable rationale text differs.

#### Scenario: AI rationale invents a score factor
- **WHEN** generated rationale references a cost, availability, participant preference, or transport fact that is absent from the deterministic scoring inputs
- **THEN** Wakeve rejects or downgrades the rationale
- **AND** the suggestion score and rank remain based only on deterministic inputs.
