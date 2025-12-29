## MODIFIED Requirements

### Requirement: Meeting Service State Machine
The system SHALL implement a state machine for the meeting service following the MVI/FSM pattern, centralizing all state and actions in `MeetingServiceStateMachine`.

#### Scenario: Create meeting successfully
- **WHEN** user creates a new meeting
- **THEN** `CreateMeeting(meeting)` intent is dispatched
- **AND** `isLoading = true` during creation
- **WHEN** creation succeeds
- **THEN** `isLoading = false`
- **AND** the meeting is added to `meetings`
- **AND** `ShowToast("Réunion créée")` side effect is emitted
- **AND** `NavigateTo("detail/{meetingId}")` side effect is emitted

#### Scenario: Generate meeting link
- **WHEN** user generates a meeting link
- **THEN** `GenerateMeetingLink(meeting, platform)` intent is dispatched
- **AND** `isLoading = true` during generation
- **WHEN** generation succeeds
- **THEN** `isLoading = false`
- **AND** `generatedLink` contains the generated link
- **AND** `ShareMeetingLink(link)` side effect is emitted

#### Scenario: Cancel meeting
- **WHEN** user cancels a meeting
- **THEN** `CancelMeeting(meetingId)` intent is dispatched
- **AND** the meeting is removed from `meetings`
- **AND** `ShowToast("Réunion annulée")` side effect is emitted
- **AND** `NavigateBack` side effect is emitted

## ADDED Requirements

### Requirement: Meeting Link Generation
The system SHALL generate meeting links for different platforms (Zoom, Google Meet, FaceTime).

#### Scenario: Generate Zoom link
- **WHEN** `GenerateMeetingLink(meeting, ZOOM)` intent is dispatched
- **THEN** `MeetingPlatformProvider` for Zoom is used
- **AND** the Zoom API is called with:
  - Meeting title
  - Date and time (with timezone)
  - Duration
  - List of participants (validated participants only)
- **AND** a Zoom link (https://zoom.us/j/...) is returned
- **AND** the link is stored in `Meeting.platformLink`

#### Scenario: Generate Google Meet link
- **WHEN** `GenerateMeetingLink(meeting, MEET)` intent is dispatched
- **THEN** `MeetingPlatformProvider` for Google Meet is used
- **AND** the Google Calendar API is called
- **AND** a Google Meet link (https://meet.google.com/...) is returned
- **AND** the link is stored in `Meeting.platformLink`

#### Scenario: Generate FaceTime link
- **WHEN** `GenerateMeetingLink(meeting, FACETIME)` intent is dispatched
- **THEN** `MeetingPlatformProvider` for FaceTime is used
- **AND** a native iOS FaceTime link is generated
- **AND** the link is stored in `Meeting.platformLink`

### Requirement: Meeting Participant Validation
The system SHALL validate that only validated participants can access meetings.

#### Scenario: Validate participants before link generation
- **WHEN** a meeting link is generated
- **THEN** only participants with `status = CONFIRMED` are included
- **AND** participants with `status = PENDING` or `DECLINED` are excluded
- **AND** if no participant is validated, an error is returned
- **AND** `ShowToast("Aucun participant validé")` side effect is emitted

#### Scenario: Attempt to invite non-validated participant
- **WHEN** an organizer attempts to invite a non-validated participant
- **THEN** the invitation is rejected
- **AND** `ShowToast("Le participant doit d'abord confirmer sa présence")` side effect is emitted

### Requirement: Meeting Reminder Timing
The system SHALL manage meeting reminders with different timings.

#### Scenario: Set reminder 15 minutes before
- **WHEN** user enables a reminder 15 minutes before
- **THEN** `MeetingReminderTiming.FIFTEEN_MINUTES` is configured
- **AND** a local notification is scheduled for T - 15 min
- **AND** the reminder includes the meeting link

#### Scenario: Set reminder 1 hour before
- **WHEN** user enables a reminder 1 hour before
- **THEN** `MeetingReminderTiming.ONE_HOUR` is configured
- **AND** a local notification is scheduled for T - 1h
- **AND** the reminder includes the meeting link

#### Scenario: Set reminder 1 day before
- **WHEN** user enables a reminder 1 day before
- **THEN** `MeetingReminderTiming.ONE_DAY` is configured
- **AND** a local notification is scheduled for T - 24h
- **AND** the reminder includes the meeting link

### Requirement: Meeting State Management
The system SHALL manage the different states of a meeting (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED).

#### Scenario: Meeting transitions to IN_PROGRESS
- **WHEN** current time exceeds the meeting start time
- **THEN** the meeting status changes to `IN_PROGRESS`
- **AND** a start notification is sent to participants
- **AND** the meeting link becomes active

#### Scenario: Meeting transitions to COMPLETED
- **WHEN** current time exceeds the meeting end time
- **THEN** the meeting status changes to `COMPLETED`
- **AND** a completion notification is sent to participants
- **AND** the meeting link is deactivated

#### Scenario: Meeting is cancelled
- **WHEN** user cancels a meeting
- **THEN** the status changes to `CANCELLED`
- **AND** a cancellation notification is sent to participants
- **AND** the meeting link is deactivated
- **AND** `ShowToast("Réunion annulée")` side effect is emitted

### Requirement: Meeting Platform Provider Interface
The system SHALL define a `MeetingPlatformProvider` interface for integration with different platforms.

#### Scenario: Mock provider for testing
- **WHEN** the test environment is used
- **THEN** `MockMeetingPlatformProvider` is injected
- **AND** test links are generated (https://test.example.com/meeting/...)
- **AND** no external API is called

#### Scenario: Real Zoom provider in production
- **WHEN** the production environment is used
- **THEN** `ZoomMeetingPlatformProvider` is injected
- **AND** real Zoom links are generated via the API
- **AND** OAuth credentials are used for authentication

#### Scenario: Provider error handling
- **WHEN** a provider returns an error
- **THEN** the error is caught and transformed to `Result.failure()`
- **AND** the state machine handles the error
- **AND** `ShowToast("Erreur: {error.message}")` side effect is emitted
- **AND** `error` in the state is updated
