## ADDED Requirements

### Requirement: On-Device Contextual Suggestions
The Suggestion Engine SHALL distinguish provider-backed scored recommendations from on-device WakeveAI contextual drafting suggestions.

WakeveAI suggestions SHALL be used for local drafting, summarization, checklist, invitation, poll, and coordination assistance. Provider-backed destination, accommodation, transport, scoring, personalization, and A/B testing flows SHALL remain governed by the existing Suggestion Engine requirements unless explicitly changed by another spec.

#### Scenario: User requests destination recommendations
- **GIVEN** the user requests ranked destination recommendations with budget and departure constraints
- **WHEN** the system needs provider-backed ranking, costs, or availability
- **THEN** the existing Suggestion Engine scoring/provider flow is used
- **AND** WakeveAI may only help phrase or summarize suggestions that are grounded in returned app/provider data.

### Requirement: Suggestion Validation Boundary
The Suggestion Engine SHALL require WakeveAI outputs to pass validation before they can be surfaced as actionable suggestions.

Validation SHALL enforce maximum suggestion counts, supported suggestion types, authorized context scope, and no invented business facts.

#### Scenario: Model returns too many poll suggestions
- **GIVEN** WakeveAI returns five poll suggestions
- **WHEN** the suggestion validation boundary processes the output
- **THEN** at most three suggestions are surfaced
- **AND** the remaining generated options are discarded.
