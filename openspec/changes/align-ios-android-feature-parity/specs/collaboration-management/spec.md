## ADDED Requirements
### Requirement: iOS Event Comments Route Parity
Wakeve MUST provide an iOS event comments route equivalent to Android's event-scoped comments route.

The iOS comments route MUST use event, section, and optional section-item context; preserve moderation/reporting actions where available; respect participant access; show offline and pending-sync states; and avoid becoming a generic chat surface.

#### Scenario: Participant opens comments for a budget item on iOS
- **GIVEN** a participant can view an event budget item
- **WHEN** they open comments for that budget item on iOS
- **THEN** Wakeve shows comments scoped to the event and budget item
- **AND** new comments are persisted locally and queued for sync when offline
- **AND** the surface clearly distinguishes pending local comments from synced comments.

#### Scenario: User lacks access to event comments
- **GIVEN** a user is not allowed to view an event's organization details
- **WHEN** they open an iOS comments route for a restricted section
- **THEN** Wakeve shows an access-denied state
- **AND** does not reveal comment content, participants, or section details beyond what the user is allowed to know.
