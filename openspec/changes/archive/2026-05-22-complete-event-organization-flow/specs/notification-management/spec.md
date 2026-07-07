## ADDED Requirements

### Requirement: Workflow Notification Triggers MUST be emitted
Wakeve MUST schedule notifications for key workflow events including invitation, poll start, vote reminder, date confirmation, scenario publication, logistics changes, payment updates, and finalization.

#### Scenario: Date confirmation notifies confirmed participants
- **GIVEN** an event poll has a selected final date
- **WHEN** the organizer confirms the date
- **THEN** confirmed and invited participants receive a notification according to preferences
- **AND** notification history stores the event id, type, delivery state, and deep link
- **AND** delivery is queued if the device or backend is offline

### Requirement: Notification Preference Enforcement MUST be respected
Wakeve MUST respect user notification preferences and quiet hours for all organization notifications except critical security messages.

#### Scenario: Quiet hours defer a budget update notification
- **GIVEN** a participant has quiet hours enabled
- **WHEN** a non-critical budget update notification is scheduled during quiet hours
- **THEN** the notification is deferred until quiet hours end
- **AND** the notification history records the deferred state
