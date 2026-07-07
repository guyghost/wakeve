## MODIFIED Requirements
### Requirement: Participant Invitation and RSVP Lifecycle
Wakeve MUST manage event participants through invitation, join, RSVP, date validation, attendance confirmation, and decline states. Participants added from contacts MUST enter the same pending invitation state as participants added manually by email.

#### Scenario: Invited participant joins and confirms attendance
- **GIVEN** an organizer creates an invitation link for an event
- **WHEN** an invited user opens the invitation, joins the event, votes on the poll, and confirms attendance for the retained date
- **THEN** the participant is associated with the event
- **AND** their RSVP and date-validation state is persisted locally
- **AND** the update is queued for sync when offline
- **AND** confirmed-attendee sections become available after the final date is confirmed

#### Scenario: Contact-selected participant is invited
- **GIVEN** an organizer selects a contact email as an event participant
- **WHEN** Wakeve adds the selected email to the participant list
- **THEN** the participant has pending RSVP state
- **AND** the same offline queue and sync behavior applies as for manually entered email invitations
- **AND** Wakeve does not store the contact's full address book record.
