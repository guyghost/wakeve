## ADDED Requirements
### Requirement: User-Generated Content Moderation
Wakeve MUST moderate user-generated content before it becomes visible to other users.

#### Scenario: Hard-policy comment is rejected
- **GIVEN** a participant is allowed to comment on an event
- **WHEN** the participant submits a comment containing hard-policy objectionable content
- **THEN** the system SHALL reject the comment before persistence
- **AND** return a user-safe error message
- **AND** record moderation evidence without exposing the rejected text to other participants

#### Scenario: Pending-review content remains hidden
- **GIVEN** a participant submits user-generated content that requires review
- **WHEN** the moderation policy marks the content as pending review
- **THEN** the system SHALL persist the content with `PENDING_REVIEW` status
- **AND** hide the content from non-moderator users until approved
- **AND** show the author a clear pending state rather than claiming the content is publicly posted

#### Scenario: Offline queued UGC is moderated on sync
- **GIVEN** a user creates comment or chat content while offline
- **WHEN** the content syncs to the backend
- **THEN** the backend moderation policy SHALL run before the content is visible to other users
- **AND** any rejection or pending-review state SHALL sync back to the originating device

### Requirement: User Reporting
Wakeve MUST let users report offensive or abusive content and users from reviewer-visible app paths.

#### Scenario: Report a comment or chat message
- **GIVEN** a user can view a comment or chat message
- **WHEN** the user reports it with a reason
- **THEN** the system SHALL create a report containing reporter ID, target type, target ID, event ID when applicable, reason, timestamp, and review status
- **AND** acknowledge the report without exposing private moderator notes

#### Scenario: Report event text or a user
- **GIVEN** a user can view event text or a user profile/identity inside an event
- **WHEN** the user reports the event text or user
- **THEN** the system SHALL create a report with stable identifiers that support moderator follow-up
- **AND** include a path for the user to contact support if more context is needed

### Requirement: User Blocking
Wakeve MUST let users block abusive users to prevent continued unwanted contact.

#### Scenario: Blocked user content is suppressed for the blocker
- **GIVEN** user A has blocked user B
- **WHEN** user A views event comments or chat messages
- **THEN** content authored by user B SHALL be hidden or collapsed for user A
- **AND** direct notifications caused by user B SHALL be suppressed for user A where supported
- **AND** other participants' view of the event SHALL remain intact unless a moderator removes the content

#### Scenario: Unblock restores visibility
- **GIVEN** user A has blocked user B
- **WHEN** user A unblocks user B
- **THEN** future reads SHALL include user B content according to normal event permissions and moderation status
