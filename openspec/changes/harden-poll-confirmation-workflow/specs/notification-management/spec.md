## ADDED Requirements

### Requirement: Confirmed Date Notification Fan-Out
The notification backend SHALL consume an acknowledged confirmed-date domain envelope and derive `recipientKey = (effectKey, participantId, notificationChannel)` before creating `deliveryKey = (recipientKey, installationId, provider)`. Missing targets SHALL remain `pendingTarget` until the envelope's bounded recipient expiry. Recipient resolution and delivery retries SHALL be independent from decision synchronization and each SHALL persist attempts, next-attempt time, acknowledgement, expiry, and terminal reason.

#### Scenario: Participant token becomes available after confirmation
- **GIVEN** a confirmed-date recipient is `pendingTarget`
- **WHEN** an eligible installation registers before expiry
- **THEN** the backend SHALL fan out one delivery for its stable `recipientKey`
- **AND** acknowledge recipient resolution separately from provider delivery
- **AND** SHALL NOT replay or mutate the confirmed decision.

#### Scenario: Pending recipient expires without a target
- **GIVEN** a confirmed-date recipient remains `pendingTarget`
- **WHEN** its bounded recipient expiry elapses
- **THEN** it SHALL transition to terminal `targetExpired`
- **AND** record a terminal acknowledgement without creating a delivery
- **AND** later token registration SHALL NOT resurrect it unless a new domain effect is authorized.
