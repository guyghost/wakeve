## ADDED Requirements

### Requirement: Event Notification Preferences SHALL Preserve Event, Account, and System Axes
Wakeve MUST represent event-scoped preference, account notification preference, and iOS system authorization as three independent axes in Event Information.

`EventNotificationPreference` MUST be persisted by `(eventId, userId)` as `INHERIT_ACCOUNT | ALL_EVENT_UPDATES | ESSENTIAL_ONLY | MUTED`. `AccountNotificationPreference` MUST remain the existing account-level enabled types, quiet hours, sound, and vibration policy. `SystemNotificationAuthorization` MUST be total as `UNAVAILABLE | NOT_DETERMINED | PROVISIONAL | AUTHORIZED | EPHEMERAL | DENIED | RESTRICTED`. `UNAVAILABLE` MUST be used when the iOS authorization port has not yet supplied a snapshot or is unavailable, and MUST remain distinct from the concrete iOS value `NOT_DETERMINED`.

While system authorization is `UNAVAILABLE`, Wakeve MUST deliver no system notification and Event Information MUST display an honest unavailable state. It MUST NOT request system permission or mutate the event/account notification axes. `UNAVAILABLE` MAY transition to a concrete authorization value only through a typed `SYSTEM_NOTIFICATION_AUTHORIZATION_SNAPSHOT_RECEIVED(value)` callback from the iOS authorization port; UI appearance, copy, retry affordances, and preference writes MUST NOT synthesize the transition.

Effective routing MUST apply this priority:

1. `UNAVAILABLE`, `DENIED`, `RESTRICTED`, or `NOT_DETERMINED` system authorization prevents system delivery.
2. Critical security messages use the existing security/account policy, still subject to the system gate.
3. Non-critical routing begins with account-enabled event notification types.
4. Event preference intersects that set: `INHERIT_ACCOUNT` and `ALL_EVENT_UPDATES` add no further type restriction, `ESSENTIAL_ONLY` keeps essential event types, and `MUTED` keeps none.
5. Account quiet hours always defer or silence eligible non-critical delivery.

Event Information MUST edit only the event-scoped record. It MUST NOT change account preference, trigger the system permission prompt, or imply that an event setting can bypass either axis.

#### Scenario: iOS system authorization snapshot is unavailable
- **GIVEN** the iOS authorization port has not supplied a snapshot or is unavailable
- **WHEN** Event Information and effective delivery are projected
- **THEN** `SystemNotificationAuthorization` is `UNAVAILABLE`
- **AND** no system notification is delivered
- **AND** Event Information displays an honest unavailable state
- **AND** no permission prompt, event-preference mutation, or account-preference mutation occurs
- **AND** only a typed OS authorization callback may replace `UNAVAILABLE` with a concrete authorization value.

#### Scenario: iOS system authorization is denied
- **GIVEN** an event preference is `ALL_EVENT_UPDATES`
- **AND** account notification types are enabled
- **AND** iOS system authorization is `DENIED`
- **WHEN** effective delivery is computed
- **THEN** no system notification is delivered
- **AND** Event Information shows the system gate separately from event and account preferences.

#### Scenario: Account disabled a notification type
- **GIVEN** the account preference disables one event notification type
- **AND** the event preference is `ALL_EVENT_UPDATES`
- **WHEN** effective event types are computed
- **THEN** the disabled type remains disabled
- **AND** `ALL_EVENT_UPDATES` does not re-enable it.

#### Scenario: Event is muted during quiet hours
- **GIVEN** the event preference is `MUTED`
- **AND** the account has quiet hours
- **WHEN** a non-critical event update is routed
- **THEN** the event-level intersection is empty
- **AND** Wakeve does not bypass account quiet hours or system authorization
- **AND** critical-security handling remains owned by the existing account/security policy.

#### Scenario: Information sheet changes the event preference
- **GIVEN** Event Information has a ready event preference repository capability
- **WHEN** the user saves `ESSENTIAL_ONLY`
- **THEN** Wakeve persists only `(eventId, userId) -> ESSENTIAL_ONLY`
- **AND** account preference is unchanged
- **AND** no iOS permission prompt is requested.

### Requirement: Event Notification Preference Writes SHALL Use Exact Operation Identity
Every event notification preference write MUST carry `OperationKey(EVENT_NOTIFICATION(eventId, userId), SAVE_EVENT_PREFERENCE, userId, operationId)`. Loading MUST capture the exact prior stable Information state. Acknowledgement, failure, retry, and late-callback handling MUST match subject, action, target, and operation id.

An offline-capable repository MAY persist the preference locally and enter pending sync. The UI MUST identify the event preference as pending and MUST NOT claim remote acknowledgement. Cancellation before commit MUST restore the exact prior stable state; cancellation after commit MUST NOT roll back repository truth implicitly.

#### Scenario: Event preference is saved offline
- **GIVEN** the event preference repository supports local-first writes
- **AND** the device is offline
- **WHEN** the user saves an event preference
- **THEN** the record and exact operation key are persisted locally
- **AND** a sync operation is queued
- **AND** Event Information shows pending sync without claiming backend success.

#### Scenario: Late acknowledgement has the wrong target
- **GIVEN** Event Information is saving a preference for one event and user
- **WHEN** an acknowledgement arrives with a different subject, target, action, or operation id
- **THEN** Wakeve ignores it
- **AND** preserves the current save/pending state and repository projection.

#### Scenario: User cancels before the preference commit
- **GIVEN** a confirmation or transient pre-commit state was opened from a ready snapshot
- **WHEN** the user cancels before a repository commit
- **THEN** the exact ready snapshot is restored
- **AND** no preference or sync operation is written.
