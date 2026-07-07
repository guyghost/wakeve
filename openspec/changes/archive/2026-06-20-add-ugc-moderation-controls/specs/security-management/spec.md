## ADDED Requirements
### Requirement: Moderation Audit and Authorization
The system SHALL authorize and audit moderation, report review, and user-blocking operations.

#### Scenario: Moderator reviews a report
- **GIVEN** a user with MODERATOR or ADMIN privileges reviews a content report
- **WHEN** the moderator approves, rejects, hides, restores, or escalates the target content
- **THEN** the system SHALL verify the moderator's role
- **AND** write an audit log with moderator ID, report ID, target type, target ID, action, reason, timestamp, and outcome
- **AND** update the target content visibility according to the decision

#### Scenario: Unauthorized report review is denied
- **GIVEN** a regular user attempts to perform a moderator-only report review action
- **WHEN** the request is processed
- **THEN** the system SHALL deny the request with HTTP 403 Forbidden
- **AND** leave report status and target content unchanged
- **AND** log the authorization failure

#### Scenario: User block is owner-scoped
- **GIVEN** a user creates or removes a block
- **WHEN** the block operation is processed
- **THEN** the system SHALL authenticate the user
- **AND** only create, list, or remove blocks owned by that authenticated user
- **AND** prevent one user from managing another user's block list
