## ADDED Requirements
### Requirement: iOS Create Event Wizard MUST stage multiple proposed slots and preview before creation
The iOS Create Event wizard MUST let organizers stage one or more proposed time slots and MUST show an event preview before the event is persisted.

#### Scenario: Organizer adds multiple proposed slots
- **GIVEN** an organizer is on the Create Event date step
- **WHEN** they save more than one date/time selection
- **THEN** each saved selection is retained as a proposed time slot
- **AND** the organizer can review and remove staged slots before continuing
- **AND** event creation persists all staged slots in `Event.proposedSlots`.

#### Scenario: Organizer previews before creating
- **GIVEN** an organizer has entered a title, description, and at least one proposed slot
- **WHEN** they reach the final wizard step
- **THEN** the primary action opens the event preview
- **AND** the event is not persisted until the organizer confirms creation from the preview.

#### Scenario: Organizer cannot create without a proposed slot
- **GIVEN** an organizer has not added any proposed slot
- **WHEN** they attempt to preview or create the event
- **THEN** the wizard blocks the action and explains that at least one slot is required.
