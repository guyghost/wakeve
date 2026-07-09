# Change: Fiabiliser l'inscription iOS et la livraison APNs en production

## Why

Wakeve expose des notifications comme une fonctionnalité iOS implémentée, mais la chaîne actuelle ne peut pas fournir une livraison APNs de production fiable :

- `ServerAPNsSender` ne consomme pas `APNS_AUTH_KEY`, ne construit aucun JWT ES256, conserve un endpoint de développement par défaut et termine chaque tentative par `APNs sender is not implemented` ;
- `NotificationService` écrit `sent_at` avant le transport, absorbe les erreurs FCM/APNs et retourne un succès même lorsque tous les envois échouent ;
- le schéma `notification_token` n'autorise qu'un appareil par couple `(user_id, platform)` et ne possède ni outbox durable, ni idempotence, ni état de retry par appareil ;
- `APNsService` conserve le token localement mais ne rend pas les erreurs d'inscription backend pilotables, et aucun appelant ne déclenche le flux explicite d'autorisation ;
- l'écran de préférences ne propose une action que lorsque la permission est refusée, pas lorsqu'elle est encore `notDetermined` ;
- la déconnexion lance la désinscription sans l'attendre, puis peut supprimer le JWT dont cette requête a besoin ;
- la spécification canonique demande à la fois une permission « au lancement » et une permission uniquement après une action utilisateur explicite.

Cette divergence peut faire croire aux organisateurs qu'une invitation, une confirmation de date ou un rappel a été envoyé alors qu'aucun appareil iOS ne pouvait le recevoir. Le changement doit donc rendre chaque transition explicite et chaque statut de livraison vérifiable avant toute déclaration de préparation à la production.

## What Changes

- Formaliser deux machines déterministes : `iosNotificationRegistration` pour permission/APNs/backend/logout et `notificationDelivery` pour politique/outbox/authentification APNs/envoi/retries/terminaux.
- Imposer, avant tout code de production, deux modèles XState v5 sous `/models` comme source de vérité exécutable, avec tests de transitions autorisées et interdites.
- Remplacer la permission au lancement par une simple lecture de statut ; le prompt système ne peut suivre qu'une action explicite de l'utilisateur.
- Rendre l'inscription backend observable et récupérable, y compris attente d'authentification, erreurs temporaires, annulation et désinscription avant effacement du JWT.
- Autoriser plusieurs installations iOS par utilisateur et gérer rotation, invalidation et déconnexion d'un seul appareil.
- Introduire une outbox durable et idempotente avec un état de livraison par appareil, des leases de worker, une expiration et un backoff borné avec jitter.
- Implémenter ultérieurement un provider APNs HTTP/2/TLS avec authentification JWT ES256, séparation sandbox/production, classification stricte des réponses et secrets exclusivement côté backend.
- Définir `sent_at` comme une acceptation fournisseur prouvée : pour APNs, uniquement après une réponse HTTP `200`, jamais lors de la mise en file ni lors d'une tentative au résultat inconnu.
- Ajouter des gates tests-first, sécurité, observabilité, migration/rollback et preuves sur appareil réel puis TestFlight avec l'environnement APNs production.
- Corriger le requirement canonique `Notification Service` par un delta `MODIFIED` complet, sans modifier directement la spécification canonique.

## Product Excellence Fit

- **Pertinence événementielle** : les notifications concernées restent limitées aux invitations, votes, confirmations, scénarios, logistique, paiements et finalisation d'un événement privé.
- **Réduction de charge mentale** : un organisateur n'a plus à relancer manuellement son groupe parce qu'un succès technique fictif masquait un échec de livraison.
- **Clarté d'état** : l'historique distingue supprimé par préférence, différé, en attente de token, en retry, bloqué, accepté par APNs et terminé en erreur. « Accepté par APNs » n'est jamais présenté comme « vu par l'utilisateur ».
- **Responsable et prochaine action** : l'utilisateur active la permission depuis une action visible ; Wakeve retente les erreurs transitoires ; les erreurs de configuration et de secret déclenchent une alerte opérateur plutôt qu'une fausse confirmation.
- **Mobile et premium** : la demande de permission arrive dans son contexte, sans prompt surprise au lancement, avec un état lisible et une récupération depuis Réglages lorsque nécessaire.
- **Frontière produit** : le changement fiabilise une capacité d'organisation existante ; il n'ajoute ni chat générique, ni réseau social, ni gestionnaire de tâches/calendrier générique.
- **IA** : aucune IA ni aucun LLM ne participe à une permission, une inscription, une politique de notification ou une transition de livraison.

## Scope

### In scope

- Modèles, contrats et implémentation future des deux workflows décrits dans `design.md`.
- Inscription/désinscription APNs iOS et association backend multi-appareil.
- Provider APNs de production, persistance outbox, idempotence, retries et invalidation des tokens.
- Tests unitaires, de modèle, d'intégration, de migration, de sécurité et preuves appareil/TestFlight.
- Compatibilité transitoire des anciens clients et rollback sans perte de l'outbox.

### Out of scope

- Refonte visuelle générale de l'Inbox ou des préférences.
- Nouvelles catégories de notifications métier.
- Garantie de lecture ou de livraison finale sur l'appareil : une réponse APNs `200` prouve seulement l'acceptation par APNs.
- Refonte FCM/Android, sauf adaptation minimale du schéma partagé et maintien de ses tests de non-régression.
- Notification Service Extension, médias riches, Live Activities, broadcast push ou analytics contenant le contenu privé d'événements.
- Implémentation dans cette proposition : l'approbation OpenSpec et la review des modèles sont des gates préalables.

## Impact

- **Affected specs**: `notification-management`
- **Related active change**: `align-ios-android-feature-parity` ajoute la parité de routes de notifications. Les deux changements ne modifient pas le même requirement, mais toute évolution de `NotificationPreferencesView` devra être coordonnée et rebasée.
- **Future model files**:
  - `models/ios-notification-registration.machine.ts`
  - `models/notification-delivery.machine.ts`
- **Expected implementation surfaces after approval**:
  - `iosApp/src/Services/APNsService.swift`
  - `iosApp/src/Services/AppDelegate.swift`
  - `iosApp/src/Services/AuthStateManager.swift`
  - `iosApp/src/Views/Notifications/NotificationPreferencesView.swift`
  - `server/src/main/kotlin/com/guyghost/wakeve/notification/PushNotificationSender.kt`
  - `server/src/main/kotlin/com/guyghost/wakeve/routes/NotificationRoutes.kt`
  - `shared/src/commonMain/kotlin/com/guyghost/wakeve/notification/NotificationService.kt`
  - `shared/src/commonMain/sqldelight/com/guyghost/wakeve/Notification.sq`
  - tests iOS, shared et server correspondants
- **Data impact**: migration additive des tokens vers des inscriptions par installation et création d'une outbox/livraisons par appareil. La donnée historique existante reste lisible.
- **Operational impact**: secrets APNs, worker durable, métriques/alertes et procédure de rotation de clé deviennent des prérequis de déploiement.

## Approval Gate

Cette proposition ne rend pas Wakeve prête pour la production à elle seule. Aucun code de production ne doit commencer avant :

1. validation stricte de ce changement ;
2. approbation humaine de la proposition ;
3. création et review des deux machines XState dans `/models` ;
4. validation des cas nominaux, erreurs, annulations, retries, permissions et terminaux ;
5. écriture des tests en échec qui matérialisent ces modèles.

## Architecture Amendment: Delivery Authority

- `DatabaseEventRepository` owns only the local SQLDelight `confirmation_effect_outbox`: it produces a stable domain-effect envelope and acknowledges local persistence, never provider delivery.
- The backend owns `notification_recipient` and `notification_delivery`; after accepting a synced envelope it resolves recipients and fans out one delivery per eligible installation.
- No transaction or atomicity guarantee spans SQLDelight and the backend database. Producer, consumer, and acknowledgement boundaries are explicit and independently retryable.
- Normative identities are `domainEventId`, `effectKey`, `recipientKey`, `deliveryKey`, and `calendarArtifactKey`. A unique `delivery_authority` prevents two active senders during migration.
- A missing target remains a pending `notification_recipient`; later membership or token availability resumes fan-out without a client-side guessed target.
- Rollout order is schema/read compatibility, shadow-write, reconciliation, authority cutover, then legacy retirement, with a rollback checkpoint at every phase.
