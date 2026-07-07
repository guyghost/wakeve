# Spec Delta: Notification Management (iOS UI Connection)

## ADDED Requirements

### Requirement: InboxViewModel iOS
L'application iOS MUST fournir un InboxViewModel connecté au NotificationService KMP
comme unique source de vérité pour les notifications.

#### Scenario: Chargement des notifications
- **WHEN** l'utilisateur ouvre l'InboxView
- **THEN** le InboxViewModel appelle NotificationService.getNotifications()
- **AND** les NotificationMessage KMP sont mappées en InboxItemModel SwiftUI
- **AND** l'état loading est affiché pendant le chargement
- **AND** les erreurs sont affichées si le chargement échoue

#### Scenario: Marquer comme lu
- **WHEN** l'utilisateur ouvre le détail d'une notification
- **THEN** le ViewModel appelle NotificationService.markAsRead(id)
- **AND** le unreadCount est mis à jour
- **AND** le badge de l'app est mis à jour

#### Scenario: Marquer tout comme lu
- **WHEN** l'utilisateur utilise l'action "Mark All Read"
- **THEN** le ViewModel appelle NotificationService.markAllAsRead()
- **AND** toutes les notifications apparaissent comme lues

### Requirement: NotificationPreferencesView iOS
L'application iOS MUST fournir un écran de configuration des préférences de notification
en parité fonctionnelle avec l'Android NotificationPreferencesScreen.

#### Scenario: Configuration des types de notification
- **WHEN** l'utilisateur accède aux préférences de notification
- **THEN** chaque type de notification affiche un toggle on/off
- **AND** les changements sont sauvegardés via NotificationPreferencesRepository

#### Scenario: Configuration des quiet hours
- **WHEN** l'utilisateur configure les heures silencieuses
- **THEN** un time picker permet de choisir start et end
- **AND** les notifications non-urgentes sont silenciées pendant ces heures

### Requirement: InboxView connectée aux données réelles
L'InboxView iOS MUST afficher les notifications réelles du NotificationService
au lieu des données mockées.

#### Scenario: Affichage des notifications réelles
- **WHEN** l'InboxView apparaît
- **THEN** les données proviennent du NotificationService KMP
- **AND** plus aucune donnée mockée n'est utilisée
- **AND** le pull-to-refresh recharge les données
