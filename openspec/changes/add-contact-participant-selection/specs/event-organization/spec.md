## MODIFIED Requirements
### Requirement: Organizers SHALL be able to invite participants and manage the participant list for an event
Organizers SHALL be able to invite participants and manage the participant list for an event. While an event is in `DRAFT`, the organizer SHALL be able to add participants manually by email or by selecting contacts from the device address book. Contact selection SHALL use email addresses only for this version.

#### Scenario: Organizer invites participants manually
- **WHEN** Organizer adds participant emails (e.g., alice@example.com, bob@example.com)
- **THEN** Participants are added to the event, and invitations are queued.

#### Scenario: Organizer selects contacts as participants
- **GIVEN** an organizer is managing participants for a DRAFT event
- **AND** the organizer grants contacts access
- **WHEN** the organizer selects contacts that contain email addresses
- **THEN** the selected email addresses are added as pending participants
- **AND** duplicate emails already present in the event are skipped
- **AND** no unselected contact data is persisted or synchronized.

#### Scenario: Organizer denies contacts access
- **GIVEN** an organizer is managing participants for a DRAFT event
- **WHEN** the organizer denies contacts access
- **THEN** the manual email invitation flow remains available
- **AND** the UI explains that contacts access is optional.
