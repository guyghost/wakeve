# Proposal: Connect iOS Notifications to Real Data

## Why

L'InboxView iOS utilise des données mockées (`InboxItemFactory.mixedList()` en DEBUG, `[]` en release). Le backend, le shared module et l'APNsService sont 100% prêts, mais l'UI iOS n'est pas branchée. L'utilisateur iOS ne voit aucune notification réelle ni ne peut configurer ses préférences.

## What Changes

### Nouveaux fichiers
1. `iosApp/src/ViewModels/InboxViewModel.swift` — ViewModel connecté au NotificationService KMP
2. `iosApp/src/Views/Notifications/NotificationPreferencesView.swift` — Écran de préférences (parité Android)

### Fichiers modifiés
3. `iosApp/src/Views/InboxView.swift` — Remplacer mock data par InboxViewModel
4. `iosApp/src/Views/InboxDetailView.swift` — Connecter actions (markAsRead, RSVP) au ViewModel
5. `iosApp/src/iOSApp.swift` — Brancher NotificationPreferencesView dans la navigation

### Fichiers de test
6. `iosApp/iosAppTests/InboxViewModelTests.swift` — Tests du ViewModel

## Périmètre fonctionnel

### InboxViewModel
- `loadNotifications()` → appelle NotificationService KMP
- `markAsRead(id)` → delegate to NotificationService
- `markAllAsRead()` → bulk read
- `deleteNotification(id)` → remove from history
- Mapping `NotificationMessage` (KMP) → `InboxItemModel` (SwiftUI)
- Gestion état : isLoading, errorMessage, items, unreadCount

### NotificationPreferencesView
- Toggle par type de notification (13 types)
- Quiet hours picker (start/end)
- Sound / vibration toggles
- Sauvegarde via NotificationPreferencesRepository KMP

### InboxView (modifié)
- Remplacer `loadItems()` mock par `viewModel.loadNotifications()`
- Remplacer `markItemAsRead()` local par `viewModel.markAsRead()`
- Binding `unreadCount` depuis le ViewModel
- Pull-to-refresh

## Impact

- **Aucun breaking change** — modification de fichiers iOS uniquement
- **Shared module** : aucune modification
- **Android** : aucune modification
- **Server** : aucune modification

## Hors périmètre

- Android notification UI (déjà implémenté)
- Backend notification sending (déjà implémenté)
- Rich notifications / notification extensions iOS
