# Event Organization Specification - Delete Event Delta

## ADDED Requirements

### Requirement: Delete Event

The organizer of an event MUST be able to delete that event.

#### Règles métier

| Status de l'événement | Suppression autorisée | Confirmation requise |
|-----------------------|----------------------|---------------------|
| DRAFT | Oui | Simple (1 clic + dialog) |
| POLLING | Oui | Avec avertissement (votes perdus) |
| CONFIRMED | Oui | Avec avertissement fort |
| COMPARING | Oui | Avec avertissement fort |
| ORGANIZING | Oui | Avec avertissement fort |
| FINALIZED | Non | Événement terminé, archivé |

#### Scenario: Suppression d'un événement DRAFT par l'organisateur

- **GIVEN** un événement en status `DRAFT`
- **AND** l'utilisateur courant est l'organisateur
- **WHEN** l'utilisateur dispatch `Intent.DeleteEvent(eventId)`
- **THEN** l'événement est supprimé du repository
- **AND** les données liées sont supprimées (time slots, potential locations)
- **AND** un `SideEffect.ShowToast("Événement supprimé")` est émis
- **AND** un `SideEffect.NavigateBack` est émis
- **AND** le state est mis à jour (événement retiré de la liste)

#### Scenario: Suppression d'un événement POLLING par l'organisateur

- **GIVEN** un événement en status `POLLING`
- **AND** l'utilisateur courant est l'organisateur
- **WHEN** l'utilisateur dispatch `Intent.DeleteEvent(eventId)`
- **THEN** l'événement est supprimé du repository
- **AND** les données liées sont supprimées (participants, votes, time slots)
- **AND** un `SideEffect.ShowToast("Événement supprimé")` est émis
- **AND** un `SideEffect.NavigateBack` est émis

#### Scenario: Tentative de suppression par un non-organisateur

- **GIVEN** un événement existant
- **AND** l'utilisateur courant n'est PAS l'organisateur
- **WHEN** l'utilisateur dispatch `Intent.DeleteEvent(eventId)`
- **THEN** l'événement n'est PAS supprimé
- **AND** un `SideEffect.ShowToast("Seul l'organisateur peut supprimer cet événement")` est émis
- **AND** le state.error contient le message d'erreur

#### Scenario: Tentative de suppression d'un événement FINALIZED

- **GIVEN** un événement en status `FINALIZED`
- **AND** l'utilisateur courant est l'organisateur
- **WHEN** l'utilisateur dispatch `Intent.DeleteEvent(eventId)`
- **THEN** l'événement n'est PAS supprimé
- **AND** un `SideEffect.ShowToast("Impossible de supprimer un événement finalisé")` est émis
- **AND** le state.error contient le message d'erreur

#### Scenario: Tentative de suppression d'un événement inexistant

- **GIVEN** un eventId qui n'existe pas dans le repository
- **WHEN** l'utilisateur dispatch `Intent.DeleteEvent(eventId)`
- **THEN** un `SideEffect.ShowToast("Événement introuvable")` est émis
- **AND** le state.error contient le message d'erreur

### Requirement: Cascade Delete

When deleting an event, all related data MUST be deleted in cascade.

#### Données à supprimer en cascade

1. **Participants** - Table `participant` où `eventId = ?`
2. **Time Slots** - Table `time_slot` où `eventId = ?`
3. **Votes** - Table `vote` où `eventId = ?`
4. **Potential Locations** - Table `potential_location` où `eventId = ?`
5. **Scenarios** - Table `scenario` où `eventId = ?`
6. **Scenario Votes** - Table `scenario_vote` où `scenarioId IN (SELECT id FROM scenario WHERE eventId = ?)`
7. **Confirmed Date** - Table `confirmed_date` où `eventId = ?`
8. **Sync Metadata** - Table `sync_metadata` où `entityId = ?`

#### Scenario: Cascade delete vérifié

- **GIVEN** un événement avec des participants, votes et time slots
- **WHEN** l'événement est supprimé
- **THEN** aucune donnée orpheline ne reste dans la base
- **AND** les queries `SELECT * FROM participant WHERE eventId = ?` retournent 0 résultats
- **AND** les queries `SELECT * FROM time_slot WHERE eventId = ?` retournent 0 résultats
- **AND** les queries `SELECT * FROM vote WHERE eventId = ?` retournent 0 résultats

### Requirement: UI Confirmation Dialog

The user interface MUST display a confirmation dialog before deletion.

#### Android (Material You)

```kotlin
AlertDialog(
    title = "Supprimer l'événement ?",
    text = "Cette action est irréversible. Toutes les données liées seront supprimées.",
    confirmButton = "Supprimer" (destructive, rouge),
    dismissButton = "Annuler"
)
```

#### iOS (SwiftUI)

```swift
Alert(
    title: "Supprimer l'événement ?",
    message: "Cette action est irréversible. Toutes les données liées seront supprimées.",
    primaryButton: .destructive("Supprimer"),
    secondaryButton: .cancel("Annuler")
)
```

#### Scenario: Confirmation dialog accessibility

- **GIVEN** the delete button is visible on screen
- **WHEN** the user activates it with VoiceOver/TalkBack
- **THEN** the accessibility label announces "Delete this event"
- **AND** the confirmation dialog is announced as a modal
- **AND** the destructive action is identified by its semantic role

### Requirement: Delete Event Repository Method

The EventRepositoryInterface MUST include a deleteEvent method for cascade deletion.

```kotlin
interface EventRepositoryInterface {
    /**
     * Delete an event and all related data.
     *
     * @param eventId The ID of the event to delete
     * @return Result<Unit> success if deleted, failure with exception otherwise
     */
    suspend fun deleteEvent(eventId: String): Result<Unit>
}
```

#### Scenario: Delete event via repository

- **GIVEN** an event exists in the repository
- **WHEN** deleteEvent is called with the event ID
- **THEN** the event is removed from the repository
- **AND** Result.success is returned

### Requirement: DeleteEvent Intent Authorization

The DeleteEvent intent MUST include userId for authorization verification.

```kotlin
data class DeleteEvent(
    val eventId: String,
    val userId: String  // For authorization verification
) : Intent
```

#### Scenario: DeleteEvent intent with userId

- **GIVEN** a DeleteEvent intent with eventId and userId
- **WHEN** the state machine processes it
- **THEN** it verifies userId matches the event's organizerId
- **AND** proceeds with deletion only if authorized
