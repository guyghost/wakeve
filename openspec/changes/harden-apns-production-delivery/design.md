## Context

La capacité `notification-management` affirme déjà une livraison APNs et une mise en file offline. L'implémentation observée ne respecte pas encore ce contrat :

- `server/.../PushNotificationSender.kt` vérifie seulement Key ID et Team ID, ignore la clé privée `.p8` et termine toujours en erreur ;
- `shared/.../NotificationService.kt` écrit `sent_at` avant l'appel fournisseur et ignore les résultats d'envoi ;
- `Notification.sq` utilise `PRIMARY KEY (user_id, platform)`, donc un second iPhone remplace le premier, et ne stocke ni tentative ni prochaine échéance ;
- `APNsService.swift` transforme et stocke un token, mais l'appel backend ne renvoie aucun événement au workflow ;
- la vue de préférences ne déclenche pas le cas `notDetermined` ;
- `AuthStateManager.signOut()` lance la désinscription et l'effacement d'authentification sans ordre causal garanti ;
- le test iOS de régression interdit déjà une demande de permission au lancement, contrairement au texte canonique actuel.

La chaîne de confiance doit donc couvrir deux unités de comportement distinctes : l'inscription d'une installation iOS et la livraison backend d'une notification à une inscription. Leur couplage se fait uniquement par des données persistées et des événements typés.

## Goals / Non-Goals

### Goals

- Définir chaque état, événement, guard, effet, erreur, retry, annulation, permission et terminal avant l'implémentation.
- Faire de deux machines XState v5 sous `/models` la source de vérité comportementale.
- Demander la permission iOS uniquement en réponse directe à une action utilisateur.
- Supporter plusieurs appareils et plusieurs installations par utilisateur sans écrasement.
- Ne déclarer un envoi APNs accepté qu'après HTTP `200`.
- Persister l'intention de notification et chaque livraison par appareil avant tout appel externe.
- Rendre les retries bornés, idempotents côté Wakeve, observables et compatibles avec l'expiration métier.
- Maintenir les secrets APNs exclusivement côté backend et échouer fermé en cas de mauvaise configuration.
- Exiger des preuves appareil réel en sandbox et TestFlight en production avant tout statut « production-ready ».

### Non-Goals

- Garantir que l'utilisateur a reçu, affiché ou lu une notification. APNs est un service best-effort ; HTTP `200` signifie « accepté par APNs ».
- Construire un moteur générique de messagerie.
- Modifier les décisions de workflow événementiel qui produisent les signaux de notification.
- Ajouter de l'IA ou laisser un texte libre choisir un état.
- Refaire FCM dans ce changement.
- Ajouter des notifications riches, Live Activities ou une extension de service iOS.

## Architectural Decisions

### 1. Model → Review → Implement → Verify est un gate bloquant

Les premiers artefacts de la future phase d'implémentation seront :

- `/models/ios-notification-registration.machine.ts`
- `/models/notification-delivery.machine.ts`

Ils utiliseront XState v5 `setup()` avec contexte et événements typés, guards purs, actions sans mutation et actors invoqués pour les effets asynchrones. La logique de classification APNs, de backoff, de politique et d'idempotence restera dans des fonctions pures appelées par les machines.

Les implémentations Swift et Kotlin consommeront les mêmes noms d'états, événements, résultats et fixtures de transition. Elles ne pourront pas introduire un état métier ou une transition implicite absent du modèle. Les tests de modèle exporteront une matrice de transitions utilisée comme contrat par les tests iOS et backend.

Aucun fichier de production ne sera modifié avant review humaine des modèles et validation des transitions nominales, erreurs, permissions, annulations, retries et terminaux.

### 2. Deux machines, aucune transition directe entre UI et delivery

`iosNotificationRegistration` possède la permission système, le token APNs d'une installation, son association à un compte et sa désinscription. `notificationDelivery` possède une livraison backend vers une inscription persistée.

La première machine écrit ou désactive une `device_registration`. La seconde ne reçoit jamais un callback UI ; elle observe seulement des inscriptions actives et des lignes d'outbox. Cette séparation empêche une vue SwiftUI ou un résultat réseau libre de décider qu'une notification est envoyée.

### 3. « Accepté » est distinct de « livré »

Pour APNs, seule une réponse HTTP `200` fait passer la livraison à `acceptedByAPNs` et renseigne `accepted_at`. Le champ historique `sent_at` est un agrégat de compatibilité : il reste `NULL` jusqu'à la première acceptation fournisseur d'au moins une livraison ciblée.

Une connexion coupée après écriture de la requête produit `unknownOutcome`, jamais un succès. Wakeve n'affiche pas « livré » et ne prétend pas connaître la lecture sur l'appareil.

### 4. L'outbox transactionnelle est la frontière de fiabilité

La création de la notification historique et des livraisons par appareil a lieu dans une même transaction. Un worker prend un lease durable sur chaque livraison prête, puis persiste le résultat avant de libérer le lease. Une reprise de processus peut donc continuer sans perdre l'intention ni recréer une livraison logique.

L'idempotence Wakeve est garantie par une clé logique unique et une contrainte `(notification_id, device_registration_id)`. APNs ne garantit pas une livraison exactement une fois ; le même `apns-id` et, seulement lorsque la sémantique le permet, le même `apns-collapse-id` réduisent les doublons sans transformer cette limite en fausse garantie.

### 5. Les identifiants d'installation remplacent le couple utilisateur/plateforme

Chaque installation possède un identifiant stable généré côté app, distinct du token APNs et du compte. Un utilisateur peut donc avoir plusieurs iPhone/iPad. Un appareil partagé qui change de compte désactive d'abord l'association précédente, puis crée l'association authentifiée suivante.

Une réponse APNs `BadDeviceToken`, `DeviceTokenNotForTopic`, `ExpiredToken` ou `Unregistered` invalide uniquement l'inscription concernée. Les autres appareils restent actifs.

### 6. Le logout attend la désinscription authentifiée

La machine d'authentification ne supprime le JWT qu'après le terminal `unregistered` de la machine d'inscription, obtenu par une réponse backend idempotente de succès ou « déjà absent ». Une erreur réseau laisse le logout dans un état explicite et retentable ; elle ne lance pas en parallèle un effacement de credentials.

Le shell d'authentification possède l'effacement du JWT. La machine de notification émet seulement `PUSH_UNREGISTERED`. Ainsi, aucun callback arbitraire ne peut effacer l'authentification.

### 7. La configuration APNs est explicite et fail-closed

Le backend utilise HTTP/2 sur TLS vers :

- sandbox : `api.sandbox.push.apple.com:443` ;
- production/TestFlight/App Store : `api.push.apple.com:443`.

Le provider utilise un JWT ES256 construit avec `APNS_KEY_ID`, `APNS_TEAM_ID` et `APNS_AUTH_KEY`, et envoie au minimum `authorization`, `apns-topic`, `apns-push-type`, `apns-id`, `apns-expiration` et une priorité compatible avec le payload. Le topic/bundle ID et l'environnement sont obligatoires ; aucun défaut silencieux vers sandbox n'est permis lorsque le déploiement est déclaré production.

Le JWT fournisseur est réutilisé pour éviter une rotation trop fréquente, puis renouvelé avant une heure. Une réponse `ExpiredProviderToken` autorise une seule régénération immédiate ; les autres échecs d'authentification bloquent le provider et alertent l'exploitation.

## Machine 1: `iosNotificationRegistration`

### Ownership and context

Une instance représente une installation iOS et, lorsqu'il existe, le compte authentifié courant.

Contexte typé prévu :

- `installationId`
- `authorizationStatus`
- `authSessionId` et présence du JWT, sans valeur du JWT dans le snapshot
- `tokenFingerprint`, jamais le token brut dans les logs ou traces de modèle
- `backendRegistrationId`
- `attempt`
- `nextRetryAt`
- `lastErrorClass`
- `logoutRequested`

### Events

- `APP_BECAME_ACTIVE`
- `PERMISSION_STATUS_RESOLVED`
- `PERMISSION_STATUS_FAILED`
- `USER_REQUESTED_ENABLE`
- `USER_OPENED_SETTINGS`
- `USER_CANCELLED`
- `PERMISSION_GRANTED`
- `PERMISSION_DENIED`
- `PERMISSION_REQUEST_FAILED`
- `APNS_DID_REGISTER`
- `APNS_DID_FAIL`
- `AUTH_BECAME_AVAILABLE`
- `AUTH_BECAME_UNAVAILABLE`
- `BACKEND_REGISTER_SUCCEEDED`
- `BACKEND_REGISTER_FAILED`
- `RETRY_DUE`
- `LOGOUT_REQUESTED`
- `BACKEND_UNREGISTER_SUCCEEDED`
- `BACKEND_UNREGISTER_FAILED`
- `CONFIGURATION_INVALID`

Chaque résultat asynchrone porte un identifiant de corrélation. Un résultat d'une invocation précédente est ignoré et audité ; il ne peut pas faire transiter l'instance courante.

### States and transitions

| State | Kind | Entry / invoked effect | Allowed transitions |
|---|---|---|---|
| `checkingPermission` | initial/transient | Lire `UNNotificationSettings`, sans afficher le prompt. | `PERMISSION_STATUS_RESOLVED(notDetermined)` → `notDetermined`; autorisé/provisoire/éphémère → `registeringApns`; refusé → `denied`; erreur transitoire → `retry`; config/entitlement invalide → `misconfigured`. |
| `notDetermined` | stable | Afficher l'état et une action contextuelle « Activer ». Aucun effet système automatique. | `USER_REQUESTED_ENABLE` → `requestingPermission`; `USER_CANCELLED` → `cancelled`; `LOGOUT_REQUESTED` → `unregistered`; `APP_BECAME_ACTIVE` → `checkingPermission`. |
| `requestingPermission` | invoking | Appeler `requestAuthorization` uniquement à la suite de `USER_REQUESTED_ENABLE`. | accord → `registeringApns`; refus → `denied`; erreur transitoire → `retry`; config invalide → `misconfigured`. Le prompt système déjà affiché ne prétend pas être annulable. |
| `denied` | stable | Montrer l'état refusé et l'action explicite d'ouverture de Réglages. | `USER_OPENED_SETTINGS` reste `denied` avec effet d'ouverture; `APP_BECAME_ACTIVE` → `checkingPermission`; `USER_CANCELLED` → `cancelled`; `LOGOUT_REQUESTED` → `unregistered`. Aucun second prompt système automatique. |
| `registeringApns` | invoking | Appeler `registerForRemoteNotifications`; attendre le callback AppDelegate corrélé. | `APNS_DID_REGISTER` avec auth → `registeringBackend`; sans auth → `awaitingAuthentication`; erreur transitoire → `retry`; entitlement/config invalide → `misconfigured`; logout → `unregistering` si une association backend existe, sinon `unregistered`. |
| `awaitingAuthentication` | stable | Conserver localement l'intention d'inscription et le token protégé, sans appel backend anonyme. | `AUTH_BECAME_AVAILABLE` → `registeringBackend`; nouveau token → reste dans l'état avec remplacement atomique; `USER_CANCELLED` → `cancelled`; logout → `unregistered`. |
| `registeringBackend` | invoking | POST authentifié idempotent avec installation, token, topic et environnement. | succès → `registered`; `401` après refresh impossible → `awaitingAuthentication`; erreur transitoire → `retry`; validation/config non récupérable → `misconfigured`; logout → `unregistering`. |
| `retry` | delayed | Persister `attempt`, `nextRetryAt` et classe d'erreur ; planifier un backoff borné avec full jitter. | `RETRY_DUE` → dernier état d'invocation sûr; auth perdue → `awaitingAuthentication`; permission changée → `checkingPermission`; `USER_CANCELLED` → `cancelled`; logout → `unregistering` ou `unregistered`; budget épuisé/config invalide → `misconfigured`. |
| `registered` | stable | Exposer une inscription backend confirmée pour cette installation. | nouveau token APNs → `registeringBackend`; auth perdue ou logout → `unregistering`; permission changée → `checkingPermission`; refresh explicite → `registeringApns`. |
| `unregistering` | invoking | DELETE authentifié idempotent pour cette seule installation ; conserver le JWT. | succès ou déjà absent → `unregistered`; erreur transitoire → `retry`; `USER_CANCELLED` avant acquittement → état stable antérieur avec session conservée; config non récupérable → `misconfigured`. |
| `unregistered` | final | Émettre `PUSH_UNREGISTERED`; le shell auth peut ensuite effacer le JWT et l'état local associé au compte. | Aucune. Une nouvelle session crée un nouvel actor. |
| `cancelled` | final | Annuler les tâches locales non commencées et ne produire aucune association implicite. | Aucune. Une action ultérieure crée un nouvel actor. |
| `misconfigured` | final | Persister une erreur assainie, exposer un état honnête et déclencher une preuve/alerte de configuration. | Aucune dans cette instance ; une version/configuration corrigée crée un nouvel actor. |

### Registration guards

- `canRequestPermission`: vrai uniquement si l'événement est `USER_REQUESTED_ENABLE` et le statut est `notDetermined`.
- `permissionAllowsRemoteRegistration`: vrai pour `authorized`, `provisional` ou `ephemeral`.
- `hasUsableAuthentication`: vrai si une session authentifiée et un JWT non expiré sont disponibles.
- `hasBackendAssociation`: vrai si un `backendRegistrationId` confirmé existe.
- `isTransientRegistrationFailure`: réseau, timeout, `5xx` ou indisponibilité temporaire.
- `isConfigurationFailure`: entitlement absent, topic/environnement incohérent, réponse de validation non récupérable.

### Registration invariants

1. Le lancement et `APP_BECAME_ACTIVE` peuvent lire la permission, jamais afficher le prompt.
2. `requestAuthorization` ne peut être un effet que de `USER_REQUESTED_ENABLE` depuis `notDetermined`.
3. Aucun appel backend d'inscription/désinscription ne part sans session authentifiée.
4. Une inscription n'est `registered` qu'après acquittement backend.
5. Une erreur backend ne disparaît pas dans un log ; elle devient événement et état.
6. Le token brut et le JWT ne figurent dans aucun snapshot, log, métrique ou message d'erreur.
7. Le logout ne peut effacer le JWT avant `unregistered`.
8. Une désinscription cible une installation, pas tous les appareils du compte.
9. Un callback périmé ou non corrélé ne provoque aucune transition.
10. L'UI observe la machine et n'infère pas son propre booléen « notifications activées ».

## Machine 2: `notificationDelivery`

### Ownership and context

Une instance représente la livraison durable d'une notification logique à une `device_registration` précise.

Contexte typé prévu :

- `notificationId`
- `deliveryId`
- `idempotencyKey`
- `deviceRegistrationId`
- `payloadHash`, type, priorité et deep link validé
- `createdAt`, `expiresAt`
- `attempt`, `maxAttempts`, `nextAttemptAt`
- `leaseOwner`, `leaseExpiresAt`
- `apnsId`
- `lastHttpStatus`, `lastReason`, `lastErrorClass`
- `mayHaveBeenWritten`

Le contexte ne contient ni clé privée APNs, ni JWT fournisseur, ni token brut dans les données observables.

### Events

- `POLICY_ALLOWED`
- `POLICY_SUPPRESSED`
- `QUIET_HOURS_ACTIVE`
- `QUIET_HOURS_ENDED`
- `ACTIVE_TOKEN_FOUND`
- `NO_ACTIVE_TOKEN`
- `TOKEN_REGISTERED`
- `DISPATCH_DUE`
- `LEASE_ACQUIRED`
- `LEASE_LOST`
- `PROVIDER_AUTH_READY`
- `PROVIDER_AUTH_FAILED`
- `APNS_RESPONSE_RECEIVED`
- `TRANSPORT_FAILED_BEFORE_WRITE`
- `TRANSPORT_OUTCOME_UNKNOWN`
- `RETRY_DUE`
- `CREDENTIALS_ROTATED`
- `TOKEN_INVALIDATED`
- `EXPIRED`
- `CANCEL_REQUESTED`

### States and transitions

| State | Kind | Entry / invoked effect | Allowed transitions |
|---|---|---|---|
| `policyCheck` | initial/transient | Évaluer préférences, droits, quiet hours, urgence et expiration avec une fonction pure. | interdit → `suppressed`; quiet hours → `deferredQuietHours`; pas de token → `awaitingToken`; autorisé avec cible → `queued`; déjà expiré → `expired`. |
| `suppressed` | final | Persister la raison de politique sans appel fournisseur. | Aucune. |
| `deferredQuietHours` | delayed | Persister `nextAttemptAt` à la fin des quiet hours. | fin → `policyCheck`; expiration → `expired`; annulation → `cancelled`. |
| `awaitingToken` | waiting | Persister l'absence de cible active sans perdre la notification. | token inscrit → `policyCheck`; expiration → `expired`; annulation → `cancelled`. |
| `queued` | durable | Ligne disponible pour lease, sans marquer `sent_at`. | dispatch + lease → `auth`; expiration → `expired`; annulation avant envoi → `cancelled`. |
| `auth` | invoking | Obtenir/réutiliser un JWT ES256 côté provider, sans l'exposer à la machine. | prêt → `sending`; credentials indisponibles/invalides → `providerAuthBlocked`; échec temporaire → `retry`; lease perdu → `queued`; expiration → `expired`. |
| `sending` | invoking | POST HTTP/2 corrélé vers le bon environnement APNs. | HTTP `200` → `acceptedByAPNs`; token invalide → `invalidToken`; requête/payload non retentable → `rejectedPayload`; auth bloquée → `providerAuthBlocked`; `429`, `5xx`, timeout avant écriture → `retry`; réponse absente après écriture possible → `unknownOutcome`; expiration → classification du résultat si reçu, sinon `unknownOutcome`. |
| `retry` | delayed | Persister erreur, tentative et prochaine échéance avec backoff/jitter. | échéance → `queued`; budget épuisé → `retryExhausted`; expiration → `expired`; annulation → `cancelled`. |
| `unknownOutcome` | waiting/reviewable | Persister que la requête a pu être acceptée sans réponse ; ne pas renseigner `sent_at`. | politique autorisant un retry corrélé → `retry`; expiration → `expired`; budget épuisé → `retryExhausted`; annulation → `cancelled`; réponse tardive corrélée `200` → `acceptedByAPNs`. |
| `acceptedByAPNs` | final | Persister `accepted_at`, `apns-id`, HTTP `200`, puis calculer l'agrégat `notification.sent_at`. | Aucune. |
| `invalidToken` | final | Désactiver cette inscription et conserver raison/timestamp APNs assainis. | Aucune. Une future réinscription crée une nouvelle cible. |
| `rejectedPayload` | final | Persister statut/raison et alerter sur un défaut de code ou de contrat. | Aucune relance aveugle. |
| `providerAuthBlocked` | blocked/recoverable | Ouvrir le circuit provider, alerter et arrêter de consommer le budget de chaque message. | `CREDENTIALS_ROTATED` ou validation de config → `auth`; expiration → `expired`; annulation → `cancelled`. |
| `expired` | final | Persister que la valeur métier a expiré avant acceptation. | Aucune. |
| `retryExhausted` | final | Persister l'échec après budget/âge maximal et alerter selon priorité. | Aucune. |
| `cancelled` | final | Persister la raison métier/opérateur. | Aucune. Une requête déjà potentiellement écrite passe d'abord par `unknownOutcome`. |

### APNs response classification

| APNs result | Transition | Side effects |
|---|---|---|
| HTTP `200` | `acceptedByAPNs` | Sauvegarder `apns-id`/horodatage ; seulement alors renseigner l'acceptation. |
| `400 BadDeviceToken`, `400 DeviceTokenNotForTopic`, `410 ExpiredToken`, `410 Unregistered` | `invalidToken` | Invalider cette inscription ; ne plus lui envoyer jusqu'à un nouveau token. |
| `400` de payload/headers/topic non récupérable, `404`, `405`, `413 PayloadTooLarge` | `rejectedPayload` | Pas de retry identique ; alerte et test de contrat. |
| `400 IdleTimeout` | `retry` | Reconstruire/réutiliser une connexion saine selon la politique provider. |
| `403 ExpiredProviderToken` | `auth`, une fois | Régénérer le JWT ; si la seconde tentative échoue, `providerAuthBlocked`. |
| autres `403` d'auth/certificat/key/topic | `providerAuthBlocked` | Ouvrir le circuit, ne jamais journaliser la clé/JWT, alerter l'exploitation. |
| `429 TooManyRequests` | `retry` | Respecter `Retry-After` s'il existe, sinon backoff avec jitter. |
| `429 TooManyProviderTokenUpdates` | `providerAuthBlocked` | Corriger/cache JWT ; ne pas épuiser toutes les notifications. |
| `500` ou `503` | `retry` | Attendre la fenêtre recommandée par Apple et appliquer le backoff borné. |
| erreur transport avant écriture | `retry` | Même livraison/idempotency key ; aucune acceptation. |
| erreur transport après écriture possible | `unknownOutcome` | Même `apns-id`, aucune acceptation fictive, retry seulement selon politique bornée. |
| raison inconnue | terminal sûr ou blocage opérateur selon classe HTTP | Pas de boucle infinie ni de succès par défaut. |

### Retry policy

- La politique est une fonction pure versionnée par classe d'erreur et priorité.
- Le backoff exponentiel utilise du full jitter, un plafond et un nombre maximal de tentatives.
- `Retry-After` prévaut lorsqu'il est présent.
- Les `5xx` APNs respectent la fenêtre de retry recommandée par la documentation Apple courante ; ce délai reste configurable et testé pour suivre une évolution du fournisseur.
- Une notification ne peut être retentée après `expiresAt`.
- `invalidToken`, `rejectedPayload`, `suppressed`, `expired`, `retryExhausted`, `cancelled` et `acceptedByAPNs` ne sont jamais retentés.
- `providerAuthBlocked` suspend le provider globalement ; il ne consomme pas une tentative par message tant que la configuration n'a pas changé.
- `unknownOutcome` conserve la vérité opérationnelle. Un retry garde la même identité logique ; Wakeve garantit l'idempotence interne, pas l'exactly-once sur l'écran système iOS.

### Delivery invariants

1. Une notification historique et ses livraisons sont créées dans une même transaction.
2. Une seule livraison logique existe par `(notification_id, device_registration_id)`.
3. Deux workers ne peuvent envoyer simultanément la même livraison avec un lease valide.
4. `sent_at` et `accepted_at` restent `NULL` avant une acceptation fournisseur prouvée.
5. APNs HTTP `200` est nécessaire et suffisant pour `acceptedByAPNs`, mais ne prouve pas la livraison ou la lecture sur appareil.
6. Toute erreur fournisseur devient une classe d'état persistée ; aucune boucle `runCatching` ne la transforme en succès.
7. Un token invalide désactive uniquement l'installation correspondante.
8. Aucun retry ne dépasse l'expiration, le budget ou une annulation.
9. Les préférences et quiet hours sont évaluées avant l'envoi et réévaluées après une longue attente.
10. Les payloads, deep links, types et priorités sont validés avant `queued`.
11. Les secrets, JWT, tokens bruts et contenu privé ne sont jamais journalisés.
12. Une transition de livraison ne dépend jamais d'un LLM ou d'un texte libre.

## Persistence and Outbox Schema

Les noms définitifs seront confirmés lors de la review des modèles, mais le contrat minimal est le suivant.

### `device_registration`

- `id` / `installation_id` stable, clé primaire
- `user_id`
- `platform`
- `environment` (`sandbox` ou `production`)
- `topic` / bundle ID
- `token` protégé selon les contrôles de stockage existants
- `token_hash` pour comparaison/diagnostic sans log du token
- `created_at`, `updated_at`, `last_registered_at`
- `invalidated_at`, `invalid_reason`, `unregistered_at`
- unicité de l'installation pour un compte et unicité du token actif dans un environnement/topic

Le schéma autorise plusieurs lignes iOS actives par utilisateur. Un remplacement de token met à jour la même installation de manière atomique sans supprimer les autres.

### `notification`

La table reste l'historique logique utilisateur. Elle gagne au minimum :

- `idempotency_key` unique
- `delivery_state` agrégé
- `sent_at` nullable avec la nouvelle sémantique d'acceptation
- `expires_at`

### `notification_delivery` (outbox)

- `id`, clé primaire et `apns_id` stable
- `notification_id`
- `device_registration_id`
- `idempotency_key` unique
- `state`
- `attempt_count`, `max_attempts`
- `next_attempt_at`, `last_attempt_at`, `expires_at`
- `lease_owner`, `lease_expires_at`
- `accepted_at`
- `provider_status`, `provider_reason`, `provider_request_id`
- `last_error_class`
- `created_at`, `updated_at`
- contrainte unique `(notification_id, device_registration_id)`

Une table append-only `notification_delivery_attempt` peut conserver les métadonnées assainies de chaque tentative : numéro, timestamps, outcome, statut HTTP et raison APNs. Elle ne stocke ni secret, ni JWT, ni token brut, ni payload privé.

## API Contracts

### Register installation

`POST /api/notifications/register` devient idempotent pour :

- `installationId`
- `platform = IOS`
- `token`
- `environment`
- `topic`
- version app optionnelle pour diagnostic

Le `userId` vient exclusivement du JWT. La réponse contient un `registrationId` et l'état confirmé. Les anciens clients sans `installationId` sont temporairement mappés vers une identité legacy déterministe jusqu'à la fin de la migration.

### Unregister installation

`DELETE /api/notifications/registrations/{installationId}` est authentifié et idempotent. `204` ou une réponse « déjà absent » est un succès terminal. Il ne supprime pas les autres appareils du compte.

L'ancien endpoint par plateforme reste compatible pendant la fenêtre de migration, puis sera retiré par un changement séparé si cela devient breaking.

### Enqueue notification

Les producteurs métier fournissent une clé d'idempotence stable dérivée de l'événement de domaine, du destinataire et du type/version de notification. Une répétition retourne la notification existante et ne duplique pas les lignes par appareil.

## Secrets and Security

- `APNS_AUTH_KEY` (`.p8`), Key ID et Team ID ne vivent que dans le secret manager/runtime backend ; jamais dans iOS, Git, fixtures, screenshots ou logs.
- Le backend valide au démarrage `APNS_KEY_ID`, `APNS_TEAM_ID`, `APNS_AUTH_KEY`, `APNS_BUNDLE_ID`/topic et `APNS_ENVIRONMENT` lorsque le provider est activé.
- En production, une configuration absente ou sandbox rend le health/readiness check non prêt ; elle ne bascule jamais silencieusement vers un mock.
- Les logs utilisent `deliveryId`, `notificationId`, `registrationId`, `apns-id`, statut et raison assainie. Les tokens apparaissent au plus sous forme de hash court non réversible.
- Les JWT ES256 sont cachés en mémoire avec une rotation conforme, jamais persistés ni exposés aux métriques.
- Une rotation/révocation de clé ferme les connexions provider existantes et réinitialise le circuit après validation.
- Les payloads restent minimaux, event-scoped, sans donnée sensible inutile ; les deep links repassent par auth et contrôle d'accès.
- Les endpoints d'envoi internes conservent leur autorisation serveur/admin et ne deviennent pas accessibles à un utilisateur arbitraire.

## Observability

Métriques sans contenu privé :

- profondeur et âge maximal de l'outbox ;
- taux par état terminal et type de notification ;
- latence `queued` → `acceptedByAPNs` ;
- retries et `unknownOutcome` par classe ;
- invalidations de tokens ;
- nombre d'inscriptions actives par plateforme, agrégé ;
- état du circuit `providerAuthBlocked` ;
- échecs d'inscription backend et logout en attente.

Alertes minimales : provider auth bloqué, outbox vieillissante, hausse de `rejectedPayload`, aucune acceptation APNs sur une fenêtre active, et TestFlight/config production incohérent.

Les dashboards emploient « accepted by APNs » et jamais « delivered/read » sans une preuve distincte.

## Migration Plan

1. Faire approuver ce changement puis créer/reviewer les deux modèles XState et leurs tests de transition.
2. Ajouter les nouvelles tables/colonnes de manière additive, avec worker désactivé.
3. Backfiller chaque ancienne ligne `(user_id, platform)` vers une inscription legacy. La migration ne peut pas recréer les appareils déjà écrasés ; cette limite est documentée.
4. Déployer les endpoints compatibles et faire dual-write ancien/nouveau format pendant la fenêtre de compatibilité.
5. Mettre à jour iOS pour envoyer `installationId`, environnement et topic, et pour exposer la machine d'inscription.
6. Exécuter les tests contractuels et une livraison sandbox sur appareil réel.
7. Configurer la clé APNs production, valider l'archive et l'entitlement `aps-environment=production`, puis livrer à TestFlight avec le worker en shadow mode si possible.
8. Activer un faible pourcentage de livraisons, surveiller outbox/auth/rejets, puis augmenter progressivement.
9. Arrêter le dual-read/dual-write legacy seulement après une version minimale adoptée et un changement de dépréciation approuvé.

## Rollback Plan

- Un feature flag coupe le worker/provider APNs sans supprimer les lignes queued/retry.
- Le rollback applicatif revient au lecteur legacy tant que le dual-write est actif ; les migrations additives ne sont pas inversées destructivement.
- Les livraisons déjà acceptées par APNs ne peuvent pas être rappelées ; leur état reste auditable.
- Si le provider produit des erreurs, ouvrir le circuit, conserver l'outbox et corriger la configuration avant reprise.
- Si une clé est suspectée compromise, la révoquer dans Apple Developer, fermer les connexions, déployer une nouvelle clé et reprendre seulement après validation.
- Si l'inscription iOS régresse, désactiver l'entrée UI de demande sans réafficher de prompt automatique ; les utilisateurs déjà autorisés restent inscrits via le chemin compatible.
- Aucun rollback ne remet `sent_at` au moment de l'enqueue ni ne réintroduit un succès fictif.

## Test-First Strategy

### Model tests before production code

- Chaque état accepte/rejette explicitement tous les événements pertinents.
- Chemins nominaux : permission explicite → token → auth → backend → registered ; policy → queue → auth → HTTP `200`.
- Permissions : `notDetermined`, denied, authorized, provisional/ephemeral, retour de Réglages, erreur de lecture.
- Erreurs : callbacks APNs, `401`, réseau, validation backend, credentials, chaque classe HTTP APNs.
- Annulations : avant prompt, en retry, en attente de token, queued, logout en cours, requête potentiellement écrite.
- Retries : backoff/jitter bornés, `Retry-After`, expiration, budget épuisé, réponse tardive corrélée.
- Invariants : transitions interdites, JWT conservé jusqu'à unregister, `sent_at` nullable avant HTTP `200`, aucun double lease.

### iOS tests

- Test source/interaction prouvant qu'aucun launch/onboarding n'appelle `requestAuthorization`.
- Tests du bouton explicite `notDetermined`, du refus et du retour Réglages.
- Tests AppDelegate pour token, échec, rotation et callback périmé.
- Tests backend-registration avec succès, `401`, `5xx`, offline et reprise.
- Test de logout prouvant l'ordre unregister acquitté → effacement JWT.
- Tests de plusieurs comptes/installations sans fuite de token.

### Backend/shared tests

- JWT ES256, cache/rotation, configuration et redaction.
- Construction HTTP/2, endpoint sandbox/production, headers et payload.
- Classification table-driven de toutes les réponses APNs supportées.
- Transactions notification/livraisons, idempotency key, leases concurrents et reprise après crash.
- Plusieurs appareils iOS, rotation et invalidation isolée.
- Quiet hours, absence de token, expiration, retries et `unknownOutcome`.
- Migration/backfill et compatibilité avec ancien client.
- Non-régression FCM/Android.

### Real-device and TestFlight evidence

La production readiness exige deux niveaux distincts :

1. **Appareil réel + sandbox APNs** : nouvelle installation, permission accordée/refusée, inscription, foreground/background/app terminée, deep link event-scoped, invalidation/retry et consultation des logs de développement Apple à partir de l'identifiant APNs.
2. **Build archivé/TestFlight + APNs production** : entitlement production, token production, au moins invitation et confirmation/rappel, app en foreground/background/terminée, quiet hours, deux appareils pour un compte, rotation de token, tap/deep link, logout et absence d'envoi après désinscription.

Chaque preuve conserve : commit/build, version iOS, modèle d'appareil, timestamp UTC, environnement, `notificationId`, `deliveryId`, `apns-id`, transitions terminales, résultat visible et capture assainie. Aucun token, JWT, clé, nom de participant ou contenu privé n'est joint.

Une réponse APNs `200` sans observation appareil valide le provider, pas l'expérience complète. Inversement, un push manuel Apple ne valide pas l'outbox Wakeve. Les deux preuves sont requises.

## Release Gates

- `openspec validate harden-apns-production-delivery --strict`
- modèles XState approuvés et tests de transition passants ;
- tests iOS/shared/server ciblés passants ;
- migrations aller/compatibilité/rollback validées ;
- archive signée avec entitlement APNs production vérifié ;
- secrets présents dans le runtime, absents du repo et des logs ;
- health check provider prêt et test synthétique contrôlé ;
- preuves sandbox appareil et TestFlight production attachées ;
- aucune occurrence de `sent_at` écrite avant acceptation fournisseur ;
- aucune déclaration « production-ready » tant qu'un gate reste sans preuve.

## Risks / Trade-offs

- **At-least-once vs doublons** : un résultat réseau inconnu peut avoir été accepté. Mitigation : état `unknownOutcome`, identité stable, collapse seulement lorsque valide, expiration et wording honnête.
- **Logout offline** : attendre la désinscription peut ralentir la déconnexion. Mitigation : état visible, retry/cancel explicite et endpoint idempotent ; ne pas sacrifier la confidentialité inter-comptes.
- **Migration multi-device incomplète** : l'ancien schéma a déjà écrasé des tokens. Mitigation : backfill best-effort et réinscription naturelle de chaque installation après mise à jour.
- **Blocage global credentials** : une mauvaise clé affecte toutes les livraisons. Mitigation : readiness, circuit breaker, alerte, rotation documentée et outbox conservée.
- **Évolution des règles APNs** : endpoints, délais et codes peuvent évoluer. Mitigation : classification isolée, tests table-driven et documentation Apple vérifiée avant chaque release majeure.
- **Chevauchement avec la parité iOS/Android** : les deux changements peuvent toucher la vue de préférences. Mitigation : garder le modèle/contrat ici et rebaser la petite adaptation UI sur la route native finale.

## Normative Provider References

- Apple, [Establishing a connection to APNs](https://developer.apple.com/documentation/usernotifications/establishing-a-connection-to-apns)
- Apple, [Establishing a token-based connection to APNs](https://developer.apple.com/documentation/usernotifications/establishing-a-token-based-connection-to-apns)
- Apple, [Sending notification requests to APNs](https://developer.apple.com/documentation/usernotifications/sending-notification-requests-to-apns)
- Apple, [Handling notification responses from APNs](https://developer.apple.com/documentation/usernotifications/handling-notification-responses-from-apns)
- Apple, [Testing notifications using the Push Notification Console](https://developer.apple.com/documentation/usernotifications/testing-notifications-using-the-push-notification-console)

Ces références doivent être revérifiées pendant l'implémentation et avant la preuve TestFlight ; le modèle ne doit pas figer silencieusement une règle fournisseur devenue obsolète.

## Resolved Scope Decisions

- Le prompt de permission n'est jamais automatique ; un statut déjà autorisé peut déclencher l'inscription APNs sans nouveau prompt.
- `sent_at` signifie première acceptation fournisseur, tandis que `notification_delivery.accepted_at` est la vérité par appareil.
- `registered` et `providerAuthBlocked` sont des états stables/récupérables, pas des succès terminaux définitifs.
- La désinscription logout est par installation et précède l'effacement du JWT.
- L'idempotence couvre Wakeve et son outbox ; elle ne prétend pas offrir une livraison APNs exactement une fois.
- Le changement reste limité aux notifications d'organisation d'événement existantes.

## Reviewed Outbox Boundary

| Record | Store and exact owner | Producer | Consumer | Acknowledgement |
|---|---|---|---|---|
| `confirmation_effect_outbox` | local SQLDelight; `DatabaseEventRepository` | atomic confirmation command | local sync publisher | backend receipt for `domainEventId` and `effectKey` |
| `notification_recipient` | backend database; notification backend | server envelope consumer | recipient resolver | resolved, pending, suppressed, expired, or terminal |
| `notification_delivery` | backend database; notification backend | recipient fan-out | provider worker | provider result persisted against `deliveryKey` |

The local transaction ends after the decision, receipt, sync metadata, and exactly one domain-effect envelope are durable. It MUST NOT create a server recipient, provider delivery, or calendar artifact. No atomic commit spans local SQLDelight and the backend database. Server acceptance is an idempotent acknowledgement, not evidence of recipient resolution or APNs acceptance.

Normative keys are `domainEventId`, `effectKey = (domainEventId, effectType, schemaVersion)`, `recipientKey = (effectKey, participantId, channel)`, `deliveryKey = (recipientKey, installationId, provider)`, and `calendarArtifactKey = (effectKey, participantId, calendarProvider)`.

`decisionSyncStatus` (`pending | acknowledged | conflicted | failed`) describes decision replication only. `effectDispatchStatus` (`notDispatched | pendingRecipient | queued | partiallyDispatched | dispatched | terminalFailure`) describes downstream effects only. They MUST NOT be collapsed. An acknowledged decision may have pending effects; retrying an effect MUST NOT retry or revert decision synchronization.

When an intended participant has no eligible target, the backend retains `notification_recipient(status=pendingTarget)`. Token registration or membership reconciliation resumes fan-out before expiry. The client never guesses a device target.

Retry domains are separate: local envelope publication by `effectKey`; backend recipient resolution by `recipientKey`; APNs delivery by `deliveryKey`; calendar fan-out by `calendarArtifactKey`. Each has its own attempts, expiry, backoff, terminal state, and acknowledgement.

### Shadow-write migration

1. Add schemas and dual-read compatibility; rollback keeps the legacy path authoritative.
2. Shadow-write new rows with sends disabled and reconcile identities/counts; rollback stops shadow writes.
3. Assign exactly one `delivery_authority` (`legacy` or `outbox-v2`) per logical delivery under a uniqueness constraint.
4. Enable v2 for a bounded cohort; rollback pauses v2 leases before atomically restoring legacy authority.
5. Retire legacy writes only after the replay horizon and reconciliation checkpoint pass.

At no point may both authorities send the same `deliveryKey`.
