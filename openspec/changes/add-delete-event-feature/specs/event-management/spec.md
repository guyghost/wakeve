# Event Management Specification - Delete Event Delta

## ADDED Requirements

### Requirement: Delete Event

L'organisateur d'un événement DOIT pouvoir supprimer cet événement.

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

Lors de la suppression d'un événement, toutes les données liées DOIVENT être supprimées.

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

L'interface utilisateur DOIT afficher un dialog de confirmation avant la suppression.

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

#### Accessibilité

- Le bouton de suppression DOIT avoir un label accessible ("Supprimer cet événement")
- Le dialog DOIT être focusable par VoiceOver/TalkBack
- L'action destructive DOIT être clairement identifiée (couleur rouge, semantic role)

## MODIFIED Requirements

### Requirement: EventRepositoryInterface

**Modification**: Ajouter la méthode `deleteEvent`

```kotlin
interface EventRepositoryInterface {
    // ... existing methods ...
    
    /**
     * Delete an event and all related data.
     *
     * @param eventId The ID of the event to delete
     * @return Result<Unit> success if deleted, failure with exception otherwise
     */
    suspend fun deleteEvent(eventId: String): Result<Unit>
}
```

### Requirement: Intent.DeleteEvent

**Modification**: Ajouter le paramètre `userId` pour la vérification d'autorisation

```kotlin
data class DeleteEvent(
    val eventId: String,
    val userId: String  // ADDED: Pour vérifier que c'est l'organisateur
) : Intent
```

## Technical Notes

### Transaction SQLite

La suppression DOIT être effectuée dans une transaction pour garantir l'atomicité :

```kotlin
db.transaction {
    // 1. Delete related data first (foreign key constraints)
    voteQueries.deleteByEventId(eventId)
    participantQueries.deleteByEventId(eventId)
    timeSlotQueries.deleteByEventId(eventId)
    potentialLocationQueries.deleteByEventId(eventId)
    scenarioVoteQueries.deleteByEventId(eventId) // via subquery
    scenarioQueries.deleteByEventId(eventId)
    confirmedDateQueries.deleteByEventId(eventId)
    syncMetadataQueries.deleteByEntityId(eventId)
    
    // 2. Delete the event itself
    eventQueries.deleteEvent(eventId)
}
```

### Offline Sync

Pour la synchronisation offline, enregistrer un tombstone dans `sync_metadata` :

```kotlin
syncMetadataQueries.insertSyncMetadata(
    id = "sync_delete_$eventId",
    entityType = "event",
    entityId = eventId,
    operation = "DELETE",
    timestamp = now,
    synced = 0
)
```
