## Context

La capacité `notification-management` affirme déjà une livraison APNs et une mise en file offline. L'implémentation observée ne respecte pas encore ce contrat :

- `server/.../PushNotificationSender.kt` vérifie seulement Key ID et Team ID, ignore la clé privée `.p8` et termine toujours en erreur ;
- `shared/.../NotificationService.kt` écrit `sent_at` avant l'appel fournisseur et ignore les résultats d'envoi ;
- `Notification.sq` utilise `PRIMARY KEY (user_id, platform)`, donc un second iPhone remplace le premier, et ne stocke ni tentative ni prochaine échéance ;
- `APNsService.swift` transforme et stocke un token, mais l'appel backend ne renvoie aucun événement au workflow ;
- la vue de préférences ne déclenche pas le cas `notDetermined` ;
- `AuthStateManager.signOut()` lance la désinscription et l'effacement d'authentification sans ordre causal garanti ;
- le test iOS de régression interdit déjà une demande de permission au lancement, contrairement au texte canonique actuel.

La chaîne de confiance couvre deux unités runtime distinctes : l'inscription d'une installation iOS et la livraison backend d'une notification à une inscription. Une troisième machine, strictement backend et limitée à la fenêtre de migration, coordonne la convergence entre le token store SQLDelight legacy et `BackendDeviceRegistrationStore` sans prétendre à une transaction cross-datastore. Leur couplage se fait uniquement par des données persistées et des événements typés.

## Goals / Non-Goals

### Goals

- Définir chaque état, événement, guard, effet, erreur, retry, annulation, permission et terminal avant l'implémentation.
- Faire des machines runtime et de la saga de compatibilité XState v5 sous `/models` la source de vérité comportementale de leurs périmètres respectifs.
- Demander la permission iOS uniquement en réponse directe à une action utilisateur.
- Supporter plusieurs appareils et plusieurs installations par utilisateur sans écrasement.
- Ne déclarer un envoi APNs accepté qu'après HTTP `200`.
- Persister l'intention de notification et chaque livraison par association `registrationId` avant tout appel externe.
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
- `/models/legacy-notification-registration-compatibility.machine.ts`

Ils utiliseront XState v5 `setup()` avec contexte et événements typés, guards purs, actions sans mutation et actors invoqués pour les effets asynchrones. La logique de classification APNs, de backoff, de politique et d'idempotence restera dans des fonctions pures appelées par les machines.

Les implémentations Swift et Kotlin consommeront les mêmes noms d'états, événements, résultats et fixtures de transition. Elles ne pourront pas introduire un état métier ou une transition implicite absent du modèle. Les tests de modèle exporteront une matrice de transitions utilisée comme contrat par les tests iOS et backend.

Aucun fichier de production ne sera modifié avant review humaine des modèles et validation des transitions nominales, erreurs, permissions, annulations, retries et terminaux.

### 2. Deux machines runtime et une saga de migration, sans transition directe entre UI et delivery

`iosNotificationRegistration` possède la permission système, le token APNs d'une installation, son association à un compte et sa désinscription. `notificationDelivery` possède une livraison backend vers une inscription persistée.

`legacyNotificationRegistrationCompatibility` ne possède ni permission iOS ni livraison fournisseur. Elle possède uniquement la convergence durable d'une commande d'inscription/désinscription entre le token store SQLDelight legacy et le store backend v2 pendant la fenêtre N/N-1. Elle ne peut pas faire transiter les deux machines runtime.

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

### 5.1 Une installation stable est distincte de son association d'inscription

`installationId` identifie durablement l'installation applicative, indépendamment du compte, du token et d'une réinscription. `registrationId` identifie une association historique précise entre cette installation, un compte, un scope APNs et un token protégé. Ce ne sont pas des alias et ils ne sont jamais interchangeables.

`activeRegistration(installationId)` est la seule association dont l'état est `ACTIVE`; `registrationHistory(installationId)` contient toutes les associations, y compris `INVALID` et `UNREGISTERED`, dans l'ordre de création. La résolution de cible de livraison ne consulte que l'association active correspondant au destinataire et au scope demandé. Une livraison déjà créée conserve sa référence vers son `registrationId` historique : elle ne peut pas être redirigée vers un nouveau compte qui réutiliserait la même installation.

Les deux machines runtime déjà approuvées suffisent pour l'inscription iOS et la livraison : `iosNotificationRegistration.installationId` reste l'identité stable locale et son `backendRegistrationId` est exactement le `registrationId` d'association renvoyé par le backend. `BACKEND_REGISTER_SUCCEEDED` remplace cette valeur par celle de la nouvelle association si nécessaire; `BACKEND_UNREGISTER_*` cible cette valeur. `notificationDelivery` travaille déjà par `registrationId`. La saga de migration n'introduit aucune nouvelle identité métier et ne devient jamais une autorité UI ou provider.

### 6. Le logout attend la désinscription authentifiée

`LOGOUT_REQUESTED` depuis tout état non terminal de l'inscription ouvre un seul vol de logout appartenant à l'actor `iosNotificationRegistration`. Si l'authentification est utilisable, la machine entre toujours dans `unregistering`, même sans `backendRegistrationId` local : la cible fermée est alors le fallback idempotent `{ kind: installation, installationId }`. Sans authentification utilisable, elle reste dans `retry` avec `resumeState=unregistering` et attend `AUTH_BECAME_AVAILABLE`; elle ne fabrique jamais un passage direct à `unregistered`.

La machine d'authentification ne supprime le JWT qu'après le terminal `unregistered` de la machine d'inscription, obtenu par une réponse backend idempotente de succès ou « déjà absent ». Une erreur réseau ou HTTP `408`, `429` ou `5xx` laisse le logout dans un état explicite et retentable. Un HTTP `401`/`403` de désinscription est une erreur `authentication` corrélée, soumise au même budget borné avec `resumeState=unregistering`; il ne devient ni succès ni callback stale. Une validation HTTP `400` est `configuration` et bloque la tentative dans `misconfigured`.

`USER_CANCELLED` ne peut pas interrompre un vol dont `logoutRequested=true`, y compris lorsqu'il attend dans `retry`; il ne s'applique qu'au flux d'inscription antérieur au logout. `misconfigured` rend un résultat bloqué à tous les observateurs sans effacer les credentials. Seul `unregistered` émet `PUSH_UNREGISTERED`. Plusieurs callers peuvent observer le même vol, mais ils sont read-only : aucun callback ou observateur ne possède une transition, ne recrée une invocation ou ne purge le JWT.

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
- `RAW_APNS_TOKEN_UNAVAILABLE`
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
| `checkingPermission` | initial/transient | Lire `UNNotificationSettings`, sans afficher le prompt. | `PERMISSION_STATUS_RESOLVED(notDetermined)` → `notDetermined`; autorisé/provisoire/éphémère → `registeringApns`; refusé → `denied`; erreur transitoire → `retry`; config/entitlement invalide → `misconfigured`; logout avec auth → `unregistering`, sans auth → `retry(resumeState=unregistering)`. |
| `notDetermined` | stable | Afficher l'état et une action contextuelle « Activer ». Aucun effet système automatique. | `USER_REQUESTED_ENABLE` → `requestingPermission`; `USER_CANCELLED` → `cancelled`; `APP_BECAME_ACTIVE` → `checkingPermission`; logout avec auth → `unregistering`, sans auth → `retry(resumeState=unregistering)`. |
| `requestingPermission` | invoking | Appeler `requestAuthorization` uniquement à la suite de `USER_REQUESTED_ENABLE`. | accord → `registeringApns`; refus → `denied`; erreur transitoire → `retry`; config invalide → `misconfigured`; logout avec auth → `unregistering`, sans auth → `retry(resumeState=unregistering)`. Le prompt système déjà affiché ne prétend pas être annulable. |
| `denied` | stable | Montrer l'état refusé et l'action explicite d'ouverture de Réglages. | `USER_OPENED_SETTINGS` reste `denied` avec effet d'ouverture; `APP_BECAME_ACTIVE` → `checkingPermission`; `USER_CANCELLED` → `cancelled`; logout avec auth → `unregistering`, sans auth → `retry(resumeState=unregistering)`. Aucun second prompt système automatique. |
| `registeringApns` | invoking | Appeler `registerForRemoteNotifications`; attendre le callback AppDelegate corrélé. | `APNS_DID_REGISTER` avec auth → `registeringBackend`; sans auth → `awaitingAuthentication`; erreur transitoire → `retry`; entitlement/config invalide → `misconfigured`; logout avec auth → `unregistering`, sans auth → `retry(resumeState=unregistering)`, avec cible `registrationId` connue ou fallback `installationId`. |
| `awaitingAuthentication` | stable | Conserver localement l'intention d'inscription et le token protégé, sans appel backend anonyme. | `AUTH_BECAME_AVAILABLE` → `registeringBackend`; nouveau token → reste dans l'état avec remplacement atomique; `USER_CANCELLED` → `cancelled`; logout sans auth → `retry(resumeState=unregistering)`, puis `AUTH_BECAME_AVAILABLE` → `unregistering`. |
| `registeringBackend` | invoking | Obtenir le token brut exclusivement via la custody privée, puis effectuer le POST authentifié idempotent avec installation, token, topic et environnement. | `RAW_APNS_TOKEN_UNAVAILABLE` avec la corrélation active → `misconfigured` fail-closed et purge de la custody; le même événement stale est audité et ignoré. Succès → `registered`; `401`/`403` → `awaitingAuthentication`; erreur transitoire → `retry`; validation/config non récupérable → `misconfigured`; logout avec auth → `unregistering`, sans auth → `retry(resumeState=unregistering)`. |
| `retry` | delayed | Persister `attempt`, `nextRetryAt` et classe d'erreur ; planifier un backoff borné avec full jitter. | `RETRY_DUE` → dernier état d'invocation sûr; auth perdue → attente récupérable; permission changée → `checkingPermission` seulement hors logout; `USER_CANCELLED` → `cancelled` seulement si `logoutRequested=false`; logout avec auth → `unregistering`, sans auth reste `retry(resumeState=unregistering)`; `AUTH_BECAME_AVAILABLE` pendant le logout → `unregistering`; budget épuisé/config invalide → `misconfigured`. |
| `registered` | stable | Exposer une inscription backend confirmée pour cette installation. | nouveau token APNs → `registeringBackend`; logout avec auth → `unregistering`, sans auth → `retry(resumeState=unregistering)`; permission changée → `checkingPermission`; refresh explicite → `registeringApns`. |
| `unregistering` | invoking | DELETE authentifié idempotent pour la cible fermée `registrationId` ou le fallback `installationId`; conserver le JWT. | succès ou déjà absent corrélé → `unregistered`; erreur transitoire ou d'authentification corrélée → `retry(resumeState=unregistering)` dans le budget; config non récupérable → `misconfigured`; `USER_CANCELLED` ne provoque aucune transition; un logout répété rejoint le même vol sans nouvelle autorité. |
| `unregistered` | final | Émettre `PUSH_UNREGISTERED`; le shell auth peut ensuite effacer le JWT et l'état local associé au compte. | Aucune. Une nouvelle session crée un nouvel actor. |
| `cancelled` | final | Annuler les tâches locales non commencées et ne produire aucune association implicite. | Aucune. Une action ultérieure crée un nouvel actor. |
| `misconfigured` | final | Persister une erreur assainie, exposer un résultat de logout bloqué à tous les observateurs et déclencher une preuve/alerte de configuration, sans effacer les credentials. | Aucune dans cette instance ; aucun `PUSH_UNREGISTERED`; une version/configuration corrigée crée un nouvel actor. |

### Registration guards

- `canRequestPermission`: vrai uniquement si l'événement est `USER_REQUESTED_ENABLE` et le statut est `notDetermined`.
- `permissionAllowsRemoteRegistration`: vrai pour `authorized`, `provisional` ou `ephemeral`.
- `hasUsableAuthentication`: vrai si une session authentifiée et un JWT non expiré sont disponibles.
- `hasBackendAssociation`: vrai si un `backendRegistrationId` confirmé existe.
- `isTransientRegistrationFailure`: réseau, timeout, `5xx` ou indisponibilité temporaire.
- `isConfigurationFailure`: entitlement absent, topic/environnement incohérent, réponse de validation non récupérable.

La frontière pure `classifyRegistrationBackendFailure(statusCode, networkFailure)` est commune aux opérations d'inscription et de désinscription. Elle décide uniquement à partir du statut typé et de la présence d'une erreur réseau :

| Entrée backend | Classe machine |
|---|---|
| HTTP `401` ou `403` | `authentication` |
| HTTP `408`, `429` ou `5xx` | `transient` |
| erreur réseau sans réponse HTTP | `transient` |
| HTTP `400` de validation | `configuration` |
| autre statut ou réponse absente sans erreur réseau | `configuration` fail-closed |

Aucun texte libre, body, message d'exception ou raison fournisseur ne décide d'une transition. Le shell Swift devra consommer cette frontière fermée et émettre seulement la classe typée avec la corrélation active.

### Registration invariants

1. Le lancement et `APP_BECAME_ACTIVE` peuvent lire la permission, jamais afficher le prompt.
2. `requestAuthorization` ne peut être un effet que de `USER_REQUESTED_ENABLE` depuis `notDetermined`.
3. Aucun appel backend d'inscription/désinscription ne part sans session authentifiée.
4. Une inscription n'est `registered` qu'après acquittement backend.
5. Une erreur backend ne disparaît pas dans un log ; elle devient événement et état.
6. Le token brut et le JWT ne figurent dans aucun snapshot, log, métrique ou message d'erreur.
7. Le logout ne peut effacer le JWT avant `unregistered`.
8. Une désinscription cible le `registrationId` actif connu, ou le fallback idempotent `installationId` lorsqu'aucune association locale n'est connue, jamais tous les appareils du compte.
9. Un callback périmé ou non corrélé ne provoque aucune transition.
10. L'UI observe la machine et n'infère pas son propre booléen « notifications activées ».
11. Un logout sans authentification reste récupérable dans `retry(resumeState=unregistering)` et ne produit jamais directement `unregistered`.
12. `USER_CANCELLED` ne peut interrompre ni `unregistering` ni un `retry` appartenant à un vol de logout.
13. Seul `unregistered` émet `PUSH_UNREGISTERED`; `misconfigured` retourne un résultat bloqué sans effacer les credentials.
14. Un vol de logout a une seule autorité de transition, l'actor, et peut avoir plusieurs observateurs en lecture seule.
15. Chaque nouvelle tentative de désinscription renouvelle la corrélation : après `c1 → retry → c2`, tout succès `c1` est stale et seul un succès `c2` peut terminer.
16. L'absence de token brut dans la custody pendant `registeringBackend` n'est acceptée que sous la corrélation active : elle termine en `misconfigured` avec purge; un signal stale est seulement audité et ignoré.

## Machine 2: `notificationDelivery`

### Single provider-delivery authority

`notificationDeliveryMachine` is the only state machine allowed to classify a provider observation or commit a durable `notification_delivery` outcome. The former direct-terminal path and the separate checkpoint wrapper are removed. There is no `PROVIDER_RESULT_OBSERVED(outcome=...)`, `APNS_RESPONSE_RECEIVED` shortcut, or `CUTOVER_CONFIRMED` transition.

A delivery begins in `policyCheck`. The same canonical machine owns `suppressed`, `deferredQuietHours`, `awaitingToken`, `queued`, `auth`, `sending`, `retry`, `unknownOutcome`, `providerAuthBlocked`, `accepted`, `invalidToken`, `rejectedPayload`, `expired`, `retryExhausted`, and `cancelled`; no wrapper or historical direct-terminal actor exists. Policy suppression and safe pre-write cancellation are terminal without provider I/O. Quiet hours and token absence remain durable waits whose persisted monotone clock can reach business expiry.

`DELIVERY_LEASE_DURABLY_ACQUIRED` is accepted only from eligible queued/recovery work when the opaque holder is nonempty, lease version and fence are strictly newer, the lease is future according to the persisted clock, and `logicalNow < businessExpiry`. It records the current checkpoint revision and enters `auth`, not `sending`. Only an exact correlated `PROVIDER_AUTH_READY` creates the send correlation and enters `sending`; exact lease loss during auth requeues safely.

Only the closed observation union is accepted:

- `PROVIDER_HTTP_OBSERVED(status, rawReason?, retryAfter?, jitterSample, deliveryKey, correlationId, attempt, leaseHolderId, leaseVersion, fencingToken)`;
- `PROVIDER_TRANSPORT_OBSERVED(phase=beforeWrite|mayHaveWritten, jitterSample, deliveryKey, correlationId, attempt, leaseHolderId, leaseVersion, fencingToken)`.

The observer cannot supply an outcome. The pure classifier derives `accepted | retry | refreshAuth | invalidToken | rejectedPayload | providerAuthBlocked | unknownOutcome | expired | retryExhausted` from HTTP status, the closed APNs matrix, transport phase, budget and expiry. Every identity, attempt, correlation, lease holder, version and fence must match; stale or foreign observations are audit-only. `mayHaveWritten` always stages durable `unknownOutcome`, never a terminal-unknown shortcut. From `unknownOutcome`, a bounded exact-holder reconciliation retry may reuse the same `deliveryKey` and stable APNs id; it still requires its own durable retry checkpoint.

`ExpiredProviderToken` stages `refreshAuth` on the first correlated occurrence for one send. Only its exact persisted ACK returns to `auth`, with the per-send coordinator marked used. A second occurrence for the same send stages `providerAuthBlocked`, whose exact ACK blocks the process credential circuit without consuming the delivery retry budget. Only a validated different credential version clears that circuit and resumes auth/queueing.

### Durable result checkpoint

A valid observation stages one `ProviderResultCheckpoint(effectId, revision, outcome, sanitizedReason, acceptedAt|nextAttemptAt)` and enters `awaitingProviderResultPersistence`; it does not mutate the durable delivery status. The worker must explicitly emit `PROVIDER_RESULT_PERSISTENCE_REQUESTED` for the exact delivery authority, fence, lease, effect and revision. `PROVIDER_RESULT_DURABLY_RECORDED` is accepted only after that emission marker is durable and all reference fields match.

XState restore never replays entry actions. A restored worker repeats the same idempotent persistence request explicitly. ACK-before-emission, stale revision, foreign lease, old fence and duplicate ACK are inert. Only the exact ACK reaches `accepted`, `retry`, `refreshAuth`, `invalidToken`, `rejectedPayload`, `providerAuthBlocked`, `unknownOutcome`, `expired` or `retryExhausted`. `retry` is nonterminal: it accepts only a schedule-free `RETRY_DUE` after the persisted monotone clock reaches `nextAttemptAt`, or an expiry clock transition. `CLOCK_TICK(expectedClockRevision, logicalNow)` never rewinds and stages a durable expiry checkpoint from policy, quiet-hours, token-wait, queued, auth, sending, retry, unknown-outcome or auth-blocked work. In particular, reaching `providerAuthBlocked` first requires the exact emitted-and-acknowledged auth-block checkpoint; reaching business expiry from that durable state creates a distinct expiry effect/revision that must itself be explicitly emitted and exactly acknowledged before `expired`.

### Backend ingestion and targeting transactions

The local `confirmation_effect_outbox` remains exclusively owned by `DatabaseEventRepository` and its local confirmation transaction. It is never a server outbox and is never part of a backend transaction.

The first backend transaction atomically and idempotently writes:

- `domain_event_ingestion`;
- `notification_logical`;
- each `notification_recipient`;
- when registrations are already resolved, exactly one `notification_delivery` per distinct `registrationId`.

Provider and calendar I/O are forbidden before that commit. When a recipient has no registration, the same transaction writes `pendingTarget(attempt, maxAttempts, nextAttemptAt, expiresAt)` and no delivery. A later resolution first acquires a durable resolver lease by exact checkpoint CAS. The separate idempotent fan-out transaction is identified by `transactionReceiptId + effectId + checkpointRevision + holderId + leaseVersion + fencingToken`. It stages the frozen, sorted registration set; only an explicit exact-holder effect request followed by the exact durable ACK commits `targeted` and its deliveries. Restore emits nothing automatically. After lease expiry, only a strictly newer lease/fence may restage the same receipt under a new effect/revision; stale, foreign and duplicate callbacks remain inert across two restored workers.

`pendingTarget` owns a persisted monotone clock and separate `retry`, `fanout`, `expiry` and `exhausted` checkpoints. A retry schedule is committed only after its checkpoint ACK. `targetExpired` and `targetExhausted` are terminal; `targeted` freezes its delivery set, so restart, duplicate reconciliation or later registration cannot append, replace or resurrect targets.

### Canonical identities and migration

New writes use injective, versioned length-prefix identities:

- `effectKey = ek2.<len(domainEventId)>:<domainEventId><len(effectType)>:<effectType>.v<schemaVersion>`;
- `recipientKey = rk2.<length-prefixed effectKey, participantId, channel>`;
- `deliveryKey = dk2.<length-prefixed recipientKey, registrationId, provider>`;
- `calendarArtifactKey = ck2.<length-prefixed effectKey, participantId, calendarProvider>`.

Colons and other delimiters inside existing domain IDs remain data and cannot collide with tuple boundaries. Reads are dual-version during migration: `poll-date-confirmed-v1 | canonical-v2`; writes are `canonical-v2` only. The only accepted historical local format is the real `poll-date-confirmed:<eventId>:<slotId>:v1:confirmation` envelope. Migration receives the authoritative typed tuple `(format=poll-date-confirmed, legacyVersion=1, eventId, slotId, legacyDomainEventId, legacyEffectKey)` from the owning local row and reconstructs both historical strings for exact equality; it never splits or heuristically parses the key.

The v2 domain identity is the injective length-prefix tuple `pdc2(eventId,slotId)`, and its effect identity is `ek2(pdc2(...),confirmation,v1)`. A durable migration record binds the legacy key to the injective source tuple and canonical identities. An exact replay returns that record idempotently. An unknown version, tuple/key mismatch, malformed authoritative component, conflicting migration record, or reuse of one colliding legacy key by a different source tuple is quarantined without mapping or automatic merge.

### Retry classifier

HTTP `500` and `503` are retryable even without a reason. Raw provider text is never persisted; it maps to the closed sanitized vocabulary such as `http5xx`, `tooManyRequests`, `tokenInvalid`, `payloadRejected`, `providerAuthRejected` or `unknownProviderReason`.

A valid `Retry-After` is a safe integer strictly after the persisted logical clock, at most 300 seconds away, and strictly before business expiry. Past, non-integer and over-bound values become typed `invalidRetryAfter`; a value at or after expiry becomes `retryWouldReachExpiry`.

Without a valid `Retry-After`, the injected full-jitter sample is clamped to `[0,1]`, with `NaN` treated as zero. The delay is `max(1, floor(min(300, 2^(attempt-1)) * sample))`. The exponent saturates before exponentiation, so attempts up to `Number.MAX_SAFE_INTEGER` remain in `[1,300]`. Schedules are strictly future, before expiry and within `maxAttempts`.

### Delivery-authority recovery

`delivery_authority` is the closed set `legacy | outbox-v2`. Cutover and rollback first acquire a durable recovery lease with opaque holder, strictly newer lease version and fencing token, deterministic logical expiry and current checkpoint revision. The sequence is:

- cutover: `pauseLegacy → reconcileLegacy → commitOutboxV2`;
- rollback: `pauseOutboxV2 → reconcileOutboxV2 → commitLegacy`.

Each step has a deterministic `effectId`, monotone checkpoint revision, lease holder/version, fencing token and `effectEmitted=false`. The worker explicitly requests the effect, then accepts only the exact durable ACK. No active delivery attempt may exist. Restore preserves the pending effect without replay; expired recovery requires a newer lease/fence and restages the same step. Foreign, stale, ACK-before-emission and duplicate callbacks cannot advance authority. The old authority remains authoritative until the exact commit ACK.

### Calendar artifact lifecycle

Calendar is in scope as its own durable artifact, not an implied side effect of notification acceptance. `notificationCalendarArtifactMachine` owns an independent attempt, maximum attempts, expiry, next schedule, monotone clock, expected correlation and durable holder/version/fencing lease. Its closed observation event type derives `upserted | removed | retry | expired | retryExhausted`; no outcome string is accepted. It requires explicit persistence emission and an exact effect/revision/lease ACK. `retry` is nonterminal and waits for `CALENDAR_RETRY_DUE` after its own clock reaches the schedule. Restore emits nothing; an expired in-flight checkpoint requires a strictly newer lease/fence and stale callbacks are inert. Provider delivery events have no authority over calendar state, and calendar events have none over provider delivery.

### Delivery invariants

1. One canonical machine owns every provider delivery transition; no direct terminal path exists.
2. Backend ingestion and local `confirmation_effect_outbox` are separate transaction authorities.
3. Resolved registrations create delivery rows in the ingestion transaction before I/O; pending recipients fan out in a later receipt-fenced transaction.
4. An observation is inert unless delivery, correlation, attempt, lease holder/version and fence all match.
5. HTTP/transport signals are classified internally; callers and free text never choose an outcome.
6. Durable status changes only after explicit effect emission and exact durable ACK.
7. Retry delay is overflow-safe in `[1,300]`, strictly future, before expiry and within budget.
8. Target retry, fan-out, expiry and exhaustion are distinct durable checkpoints.
9. Cutover and rollback require leased, fenced pause/reconciliation/commit effects.
10. Calendar provider outcomes have an independent correlated, leased, retryable and expiring durable lifecycle.
11. Every resolver fan-out receipt is exact-holder, lease-version, fence, effect and revision correlated across restore.
12. `mayHaveWritten` persists recoverable `unknownOutcome`; it never aliases an HTTP terminal failure.
13. A provider credential refresh occurs at most once per correlated send before the credential circuit blocks.
14. No transition depends on an LLM.

## Machine 3: `legacyNotificationRegistrationCompatibility`

### Ownership and durable boundary

Cette saga backend existe uniquement pendant la fenêtre N/N-1. Une instance possède une commande d'inscription ou de désinscription et un enregistrement durable de reconciliation. Elle ne possède ni le JWT, ni le token brut, ni l'autorité de livraison. Le contexte observable contient seulement `sagaId`, la clé idempotente, le type d'opération, la génération client, les identités legacy opaques déjà dérivées par HMAC, un fingerprint de token, les résultats durables de chaque store, les compteurs de retry séparés et la réponse autorisée.

Avant toute écriture dans l'un des deux stores, `acceptingIntent → INTENT_PERSISTED` crée une reconciliation durable `pending`. Il n'existe aucune transaction commune entre SQLDelight et `BackendDeviceRegistrationStore`; chaque acquittement `*_WRITE_SUCCEEDED(applied|alreadyApplied)` signifie qu'un seul store a engagé son étape. Le snapshot de saga, comprenant `effectCheckpoint`, est la source de reprise après crash.

La clé de requête est l'encodage JSON canonique du tuple typé `['legacy-notification-registration-compatibility', 'v2', operation, authenticatedSubject, compatibilityGeneration, stableTargetIdentity, tokenFingerprint|null]`. Ce tuple est injectif : les délimiteurs contenus dans le sujet ou la cible restent dans leur champ et ne peuvent pas produire la même clé par concaténation ambiguë. Le sujet authentifié vient exclusivement du JWT validé et empêche qu'un changement de compte partage une saga. `compatibilityGeneration` est un entier monotone alloué et persisté lorsque l'état désiré change; il permet de distinguer une nouvelle inscription après une désinscription d'un simple retry HTTP de l'ancienne commande. Pour N-1, `stableTargetIdentity` est le `legacyRegistrationId` opaque dérivé avec `HMAC-SHA-256(WAKEVE_NOTIFICATION_LEGACY_IDENTITY_HMAC_KEY, immutableLegacyPrimaryKey)`. Une rotation change le fingerprint et donc la commande register; un duplicate exact de la même génération rejoint la même saga. Le token brut, la clé HMAC et la primary key legacy ne figurent jamais dans le snapshot.

### Authority order

| Client / operation | Étapes durables dans l'ordre | Justification |
|---|---|---|
| N register | v2 exact uniquement | Ne jamais écraser la ligne legacy lossy `(user, platform)` avec un second appareil N. |
| N unregister | v2 exact `registrationId`, ou fallback exact `installationId` | Désinscrire uniquement l'installation courante. |
| N-1 register | SQLDelight legacy, puis installation v2 legacy HMAC | Préserver immédiatement la lecture N-1, puis converger vers la cible backend bornée. |
| N-1 unregister | installation v2 legacy HMAC, puis ligne SQLDelight legacy | Fermer d'abord l'éligibilité v2 sans jamais supprimer les autres installations, puis retirer la compatibilité N-1. |

Une étape déjà engagée n'est jamais compensée en supprimant une installation v2 ou en réactivant une association fermée. La compensation est une reconciliation forward-only de l'étape manquante. Une intervention opérateur destructive serait une nouvelle commande explicitement modélisée, hors de cette saga.

### States and transitions

| State | Durable meaning | Allowed transitions |
|---|---|---|
| `acceptingIntent` | Aucun store métier touché; persister l'intention et sa clé. | `INTENT_PERSISTED` corrélé → première étape selon la table d'autorité. |
| `writingLegacy` | Upsert/delete idempotent de la seule ligne legacy déterministe. | succès durable → étape v2 ou `recordingConvergence`; erreur retentable → `recordingRetry`; config/conflit/budget → `recordingBlock`. |
| `writingV2` | Register/unregister idempotent de la cible exacte ou de l'installation legacy HMAC. | succès durable → étape legacy N-1 unregister ou `recordingConvergence`; erreur retentable → `recordingRetry`; config/conflit/budget → `recordingBlock`. |
| `recordingRetry` | Persister store fautif, tentative et prochaine échéance strictement future relativement à l'horloge logique. | `RETRY_RECORDED` valide → `retryWait`; échéance présente/passée → audit sans transition. |
| `retryWait` | Attente durable sans réexécuter l'autre store déjà acquis. | `RETRY_DUE(sagaId, retryScheduleRevision)` sans champ temps → seulement l'étape fautive si l'horloge logique persistée a atteint l'échéance. |
| `recordingConvergence` | Les étapes requises sont acquises; persister le checkpoint convergé. | `CONVERGENCE_RECORDED` → `converged`. |
| `recordingBlock` | Persister erreur terminale assainie et étape manquante. | `BLOCK_RECORDED` → `blocked`. |
| `converged` | terminal | Autorise HTTP `200 {success:true}`. |
| `blocked` | terminal | Autorise seulement une réponse d'échec; la reconciliation reste auditable. |

Une réponse pré-convergence est possible seulement après `INTENT_PERSISTED`, sous forme explicite `202 accepted` qui ne prétend pas `success:true`. Le handler peut aussi attendre `converged` et répondre `200`. Un crash après l'étape 1 reprend à l'étape 2; un crash après l'étape 2 reprend uniquement `recordingConvergence`; un duplicate réutilise les résultats `applied|alreadyApplied` et la même clé.

### Recovery worker contract

XState v5 restaure l'état et le contexte d'un snapshot sans rejouer les actions `entry`. La reprise ne suppose donc jamais que `createActor(machine, { snapshot }).start()` réémette spontanément l'effet de `acceptingIntent`, `writingLegacy`, `writingV2`, `recordingRetry`, `recordingConvergence` ou `recordingBlock`.

Le worker de reconciliation doit d'abord acquérir par compare-and-set un lease durable exclusif sur `(sagaId, effectCheckpoint, checkpointRevision)`. L'acquittement typé `RECOVERY_LEASE_ACQUIRED` n'est émis par le port de saga qu'après engagement durable et fournit une identité de lease opaque, un `holderId` opaque, une version et un fencing token strictement monotones, ainsi qu'une expiration logique. La machine enregistre cette autorité dans le contexte avant tout recovery. Le temps autoritatif est `logicalNowEpochSeconds`, initialisé explicitement et avancé seulement par `CLOCK_ADVANCED(sagaId, clockRevision, nowEpochSeconds)`. L'événement doit viser le saga courant, porter exactement la prochaine révision d'horloge et ne jamais reculer; la valeur et `clockRevision` sont conservées dans le snapshot. Aucune guard ne lit `Date.now()` ou une horloge implicite.

Après restauration, le holder envoie `RECOVERY_REQUESTED` avec l'identité, le holder, la version, le fencing token, l'`effectId`, le checkpoint et sa révision exacts. La machine refuse un holder étranger, un lease expiré, une ancienne version, un ancien fencing token ou un checkpoint périmé. Un lease enregistré peut émettre son effet au plus une fois; deux recoveries concurrents avec le même lease ne produisent donc qu'un signal. Après expiration, un nouveau lease durable de version et fencing supérieurs peut réémettre le même effet logique idempotent; le port retourne `alreadyApplied` si l'engagement avait précédé le crash.

`LEGACY_COMPATIBILITY_EFFECT_REQUESTED` porte `effectId`, `checkpointRevision` et `authorityFencingToken`. Tous les acquittements d'effet — `INTENT_PERSISTED`, `LEGACY_WRITE_*`, `V2_WRITE_*`, `RETRY_RECORDED`, `CONVERGENCE_RECORDED`, `BLOCK_RECORDED` — doivent renvoyer cette référence exacte. La guard vérifie les trois valeurs avant toute transition ou mutation. Un ACK ou échec retardé d'un checkpoint `c1` ne peut donc ni terminer, ni incrémenter un compteur, ni enregistrer un schedule lorsque la saga attend `c2`; il est seulement audité.

`RETRY_RECORDED.nextRetryAtEpochSeconds` doit être strictement supérieur à `logicalNowEpochSeconds`. Le modèle refuse une échéance présente ou passée; il ne possède aucune transition « immédiate » implicite. `RETRY_DUE` ne contient volontairement aucun `nowEpochSeconds` fourni par l'appelant et doit matcher la révision du retry schedule courant. Il ne peut quitter `retryWait` que lorsque le contexte horloge autoritatif a atteint ou dépassé l'échéance. Une valeur future ajoutée arbitrairement au payload est donc sans autorité, tandis qu'un snapshot restauré conserve exactement l'heure et la révision déjà acquises.

Le délai de reconciliation utilise un full jitter pur sur une fenêtre exponentielle `min(300, 2^(attempt-1))` secondes. Le sample injecté est borné à `[0, 1]`; `NaN` suit la borne basse fail-closed. Comme le schedule durable doit rester strictement futur, le résultat est `max(1, floor(window * sample))` : même un sample nul produit une seconde, tandis qu'un sample à `1` atteint la fenêtre ou son plafond de 300 secondes. L'exposant est saturé dès que la fenêtre atteint le plafond, avant l'exponentiation, afin qu'une tentative arbitrairement élevée ne produise ni overflow ni délai hors bornes.

L'état XState, `effectCheckpoint`, `checkpointRevision`, l'autorité fencing, le lease et son marqueur `effectEmitted` sont engagés dans le même snapshot. `acceptingIntent` pointe vers `persistPendingReconciliation`; les cinq états d'effet suivants pointent vers leur effet homonyme. Chaque nouveau checkpoint augmente sa révision et son fencing. `retryWait` et les terminaux n'ont aucun effet en attente et conservent `effectCheckpoint = null`. La reprise préserve l'autorité enregistrée et ne rembobine jamais vers un checkpoint antérieur déjà acquitté.

### Cutover and rollback controller

`legacyRegistrationRollout` sépare l'autorité de lecture de la dual-write. `legacyAuthoritative → cutoverCheckpointPending → v2Authoritative` exige zéro reconciliation pending, writer legacy pausé et unicité confirmée. `v2Authoritative → rollbackCheckpointPending → legacyAuthoritative` exige writer v2 pausé, projection legacy prête et checkpoint de reconciliation confirmé. Un checkpoint incomplet retourne à l'autorité précédente; une configuration invalide termine `blocked`. Le rollback change l'autorité de lecture mais conserve toutes les lignes/historiques v2.

### Compatibility invariants

1. Aucune écriture de store ne précède l'intention de reconciliation durable.
2. Chaque store possède son compteur, son backoff et son acquittement idempotent.
3. Une reprise n'attend aucun rejeu implicite d'une action `entry`; seul un holder possédant le lease durable exact, non expiré et fenced peut réémettre l'effet idempotent courant, au plus une fois par lease.
4. N ne dual-write jamais dans la ligne legacy lossy; N-1 cible seulement les identités HMAC de sa ligne immuable.
5. Une désinscription legacy ne peut ni énumérer, ni fermer, ni masquer une installation v2 distincte.
6. `success:true` exige `converged`; `accepted` exige au minimum la reconciliation durable.
7. Cutover et rollback ne créent jamais deux autorités simultanées et ne suppriment aucune donnée v2.
8. Aucun texte libre ou LLM ne choisit une transition.
9. La clé de requête est un tuple canonique injectif; déplacer `:` ou tout autre délimiteur entre sujet et cible ne peut pas créer de collision.
10. Tout ACK d'effet doit matcher `effectId + checkpointRevision + authorityFencingToken`; un callback ancien est inerte pour l'état, les tentatives et le retry schedule.
11. L'expiration de lease et l'éligibilité des retries dépendent uniquement de l'horloge logique monotone persistée; aucun temps mural ou champ temps de `RETRY_DUE` ne choisit une transition.
12. Un retry schedule présent ou passé est refusé; seul un deadline strictement futur peut être persisté, puis franchi par `CLOCK_ADVANCED` corrélé.
13. Le full jitter de reconciliation retourne toujours un entier dans `[1, 300]`; sample hors bornes, `NaN` et compteur de tentative extrême ne peuvent produire ni zéro, ni `NaN`, ni overflow.

## Machine 4: `legacyCompatibilityUniqueMigration`

### Startup gate and ownership

Cette machine possède uniquement le preflight et la migration additive qui installent l'unicité de `request_key` pour les sagas de compatibilité. Elle est un gate de démarrage : ni les routes de notification, ni le scheduler de reconciliation ne peuvent être activés avant son terminal `ready`. Elle ne possède aucun token, aucune request key brute, aucune décision de fusion métier et aucune transition choisie par LLM.

Le premier effet `scanDuplicates` est strictement read-only et précède tout DDL. Une base sans doublon et sans index passe par `installingUniqueIndex`; une base sans doublon dont l'index est déjà présent atteint directement `ready`. Une base contenant des doublons passe à `blockedDuplicates` sans exécuter de DDL ni modifier la base. Son diagnostic observable est limité à la révision de scan, aux digests de groupes et aux nombres de groupes, lignes et leases; il n'expose ni token, ni request key brute, ni identifiant de saga.

### States and checkpoints

| State | Durable meaning | Allowed transition |
|---|---|---|
| `startupPreflight` | Scanner read-only les groupes dupliqués et la présence de l'index. | Scan exact sans doublon → `ready` ou `installingUniqueIndex`; scan exact avec doublons → `blockedDuplicates`. |
| `installingUniqueIndex` | Installer additivement `UNIQUE(request_key)` après un scan sans doublon. | ACK exact → `ready`; échec exact → `blockedMigrationFailure`. |
| `blockedDuplicates` | Runtime désactivé; base inchangée depuis le scan bloquant. | Résolution opérateur valide → `archivingNoncanonicalRows`; réparation externe explicitement confirmée et fenced → `startupPreflight`. |
| `archivingNoncanonicalRows` | Archiver immuablement les lignes et effets non canoniques choisis explicitement. | ACK d'archive non vide et exact → `deletingArchivedRows`; échec exact → `blockedMigrationFailure`. |
| `deletingArchivedRows` | Supprimer seulement les lignes déjà couvertes par l'archive durable. | ACK exact → `startupPreflight`; échec exact → `blockedMigrationFailure`. |
| `blockedMigrationFailure` | Runtime désactivé après conflit ou échec de checkpoint. | Réparation externe explicitement confirmée et fenced → `startupPreflight`. |
| `ready` | Terminal; le preflight sans doublon et l'index unique sont confirmés. | Autorise ensuite seulement l'ouverture des routes et du scheduler. |

Chaque effet possède un `effectId` déterministe, une `checkpointRevision` et un `authorityFencingToken` monotones. Tous les ACKs et échecs doivent matcher cette référence exacte; une référence antérieure est inerte. Les effets de scan, archive, suppression et installation d'index sont idempotents; un crash avant ou après leur engagement ne peut ni sauter un checkpoint ni créer deux autorités.

### Durable recovery authority

Comme les actions `entry` XState ne sont pas rejouées lors de `createActor(machine, { snapshot }).start()`, un actor restauré n'émet aucun effet spontanément. Avant `RECOVERY_REQUESTED`, le port durable acquiert par compare-and-set un lease portant l'`effectId`, le checkpoint et sa révision exacts. `RECOVERY_LEASE_ACQUIRED` est uniquement l'acquittement de ce CAS engagé : il fournit `leaseId`, `holderId`, version et fencing token strictement supérieurs aux derniers enregistrés, ainsi qu'une expiration strictement future selon l'horloge logique. La machine enregistre cette autorité, la révision d'horloge d'acquisition et `effectEmitted=false` dans le snapshot avant toute reprise.

Le temps autoritatif est `logicalNowEpochSeconds`, initialisé explicitement et avancé uniquement par `CLOCK_ADVANCED(migrationId, clockRevision, nowEpochSeconds)`. L'événement doit porter exactement la prochaine révision et ne peut pas rembobiner le temps. Aucun guard ne lit `Date.now()` ou un champ temps fourni par `RECOVERY_REQUESTED`.

`RECOVERY_REQUESTED` doit matcher le lease ID, le holder, la version, le fencing token, l'effet, le checkpoint et sa révision, et le lease doit être non expiré. La première demande exacte marque `effectEmitted=true` et émet l'effet avec le fencing token du lease; une seconde demande avant ACK, un holder étranger, un ancien fence ou un lease expiré est inerte. Après expiration logique, seul un nouveau CAS de version et fence strictement supérieurs peut remplacer l'autorité et réémettre le même effet idempotent. Le port persiste le snapshot contenant `effectEmitted=true` avant de consommer le signal d'effet; deux workers concurrents ne peuvent donc pas transformer deux copies restaurées en deux autorités. Le perdant du CAS ne reçoit jamais `RECOVERY_LEASE_ACQUIRED`.

Chaque transition vers un nouveau checkpoint efface le lease courant tout en conservant les maxima de version et de fencing. Un ACK après recovery doit porter `effectId + checkpointRevision + fencingToken` de la nouvelle autorité et arriver avant son expiration; l'ACK pré-crash ou issu d'un lease remplacé est ignoré. L'autorité, l'horloge et le marqueur d'émission survivent à la restauration.

### Explicit duplicate resolution

`OPERATOR_RESOLUTION_REQUESTED` doit porter la `scanRevision` exacte, le digest du groupe, un `canonicalSagaId` appartenant à ce groupe et un identifiant de résolution non vide. La garde exige que toutes les lignes aient la même identité métier validée par digest, que le groupe soit quiescent, non divergent et sans lease actif. Aucun classement automatique ne choisit la ligne canonique.

Pour un groupe éligible, `archiveNoncanonicalRowsAndEffects` capture immuablement toutes les lignes non canoniques et leurs effets, avec digest, nombre de lignes, révision de scan, résolution opérateur et saga canonique. Un `ARCHIVE_COMMITTED` sans `archiveId` ou digest non vide est refusé. La suppression ne commence qu'après cet audit durable, puis un nouveau preflight complet est obligatoire avant l'index. Les groupes divergents, non quiescents ou avec lease actif restent bloqués et exigent une réparation externe; une demande périmée, un canonical étranger ou une réparation non fenced est inerte.

### Migration invariants

1. L'ordre de démarrage est `preflight → unique index → routes et scheduler`; seul `ready` ouvre le runtime.
2. Un scan avec doublons n'exécute aucun DDL et ne modifie aucune ligne.
3. Les diagnostics contiennent uniquement des digests et comptes assainis.
4. Une résolution est explicite, fenced par `scanRevision`, limitée à un groupe simple et quiescent, et ne dépend jamais d'un LLM.
5. Les lignes et effets non canoniques sont archivés immuablement avant toute suppression; l'audit survit au restart.
6. Chaque checkpoint est exact, fenced, idempotent et récupérable après restauration sans compter sur un rejeu implicite de `entry`.
7. Seul le holder du lease durable exact, non expiré et enregistré peut émettre; un lease émet au plus une fois et son remplacement exige version et fence strictement supérieurs.
8. L'horloge de recovery est logique, persistée, corrélée et monotone; aucun temps mural implicite ne pilote une transition.
9. Tout ACK matche l'effet, la révision et le fence courants; un ACK pré-crash, expiré ou remplacé ne change aucun état.
10. Tout échec ou rollback reste fail-closed; une réparation externe explicite mène à un nouveau preflight, jamais directement à `ready`.
11. L'installation de l'index exige toujours le dernier preflight sans doublon; une migration répétée sur une base déjà indexée est sans effet.

## Persistence and Outbox Schema

Les noms définitifs seront confirmés lors de la review des modèles, mais le contrat minimal est le suivant.

### `device_installation`

- `installation_id` stable, opaque, clé primaire, généré côté app puis validé par le backend
- `platform` canonique, `created_at`, `updated_at`
- aucun `user_id`, token APNs, statut fournisseur ou diagnostic de token

Cette table est l'ancre de l'installation et n'est jamais réutilisée comme l'identité d'une association de compte. Elle peut avoir plusieurs associations historiques, mais au plus une association active. Elle est exclusivement backend-owned : SQLDelight local ne crée ni ne réplique `device_installation` ou `device_registration`.

### `device_registration`

- `registration_id` opaque, clé primaire, généré côté backend pour une association précise
- `installation_id`, FK non supprimable vers `device_installation`
- `user_id`, `environment` (`sandbox` ou `production`) et `topic` / bundle ID; `platform` est hérité de `device_installation` par FK et ne peut pas être dupliqué divergent
- `status` : `ACTIVE`, `INVALID` ou `UNREGISTERED`
- `token_ciphertext` chiffré au repos avec une clé runtime configurée et accessible seulement au port provider; `token_hash` pour égalité et diagnostic assaini
- `created_at`, `updated_at`, `last_registered_at`, `invalidated_at`, `invalid_reason`, `unregistered_at`, `unregistered_reason`
- contrainte partielle unique `installation_id WHERE status = 'ACTIVE'`
- contrainte partielle unique `(environment, topic, token_hash) WHERE status = 'ACTIVE'`

Les états et transitions sont explicites : une première inscription crée `ACTIVE`; une rotation de token pour la même association active et le même `(user_id, environment, topic)` met à jour seulement `token_ciphertext`, `token_hash`, `updated_at` et `last_registered_at`; une réponse APNs invalidante fait `ACTIVE → INVALID`; logout ou retrait explicite fait `ACTIVE → UNREGISTERED`. `invalid_reason` est réservé à la raison APNs assainie; `unregistered_reason` est un enum fermé distinct : `ACCOUNT_CHANGED`, `SCOPE_CHANGED`, `LOGOUT`, `USER_REQUESTED` ou `ADMIN_REVOKED`. Une réinscription après `INVALID` ou `UNREGISTERED` crée un nouveau `registration_id` `ACTIVE` sans réactiver ni écraser l'historique. Des `CHECK` SQL portent les mêmes invariants : `ACTIVE` n'a aucun timestamp/motif terminal, `INVALID` possède exactement ses métadonnées d'invalidation typées et `UNREGISTERED` exactement ses métadonnées de désinscription typées. Ces contraintes s'appliquent aussi aux écritures SQL directes. Toutes ces colonnes sont relues depuis le datastore après réouverture; elles ne sont pas reconstruites à partir d'un état en mémoire. Les tokens bruts ne sont jamais retournés, journalisés, utilisés comme clé de diagnostic ni conservés dans une projection legacy. Le port provider reçoit le token seulement dans un callback lexical et retourne un type fixe opaque/tokenless, jamais le résultat générique du callback; une alternative valide encapsule entièrement l'envoi provider. L'absence de clé de chiffrement ou de clé HMAC legacy fait échouer la résolution avant toute lecture, migration ou écriture; aucun secret n'apparaît dans l'erreur ou le snapshot de configuration. La fermeture du store et du port provider prend le même verrou de cycle de vie que register/decrypt, attend toute opération commencée, puis seulement détruit les clés en mémoire. Aucun ciphertext ne peut donc être validé avec une clé déjà vidée. `SqliteBackendDeviceRegistrationStoreFactory` accepte une `DeviceRegistrationStoreConfiguration` immutable déjà validée; ce chemin explicite est indépendant de l'environnement du processus. Le chemin est absolu et local : `:memory:`, chemin relatif, URI SQLite/file et tout composant symbolique sont refusés. Sur POSIX, le parent est créé/vérifié en `0700` et le fichier en `0600`; un échec de validation ou de correction échoue fermé. L'adapter sans argument enregistré dans `ServiceLoader` résout la configuration ambiante, puis délègue au même factory durable.

Un changement de compte ou de scope est une unique transaction backend : vérifier l'autorisation et la nouvelle cible, fermer l'ancienne association `ACTIVE` avec `UNREGISTERED` et motif respectivement `ACCOUNT_CHANGED` ou `SCOPE_CHANGED`, créer le nouveau `registration_id` `ACTIVE`, puis mettre à jour la projection de compatibilité. Changer seulement le topic ou seulement l'environnement constitue dans les deux cas un changement de scope et reçoit le même traitement. La contrainte partielle et la transaction garantissent qu'aucune lecture engagée ne voit deux associations actives ni une mutation partielle; une validation en échec ou une faute injectée après la tentative de fermeture annule l'ensemble. Le seam de faute transactionnelle est interne au datastore et ne remplace jamais le store réel dans le test.

La validation de chemin n'est pas un contrôle ponctuel : le factory revalide chaque composant avec une sémantique sans suivi de lien immédiatement avant toute création, chmod ou ouverture JDBC. Un swap symlink entre `DeviceRegistrationStoreConfiguration.resolve` et `open` échoue donc sans mutation de la cible. Un parent POSIX préexistant group/world-accessible peut être partagé avec d'autres usages : il est refusé sans chmod implicite. Seul un parent absent et dédié est créé en `0700`. Sous un parent déjà owner-only, un fichier SQLite préexistant trop permissif peut être refusé ou ramené à `0600` avant JDBC.

Le moniteur de cycle de vie protège aussi la réentrance. Si `close()` est appelé depuis le callback de déchiffrement sur le même thread, l'implémentation le refuse explicitement ou enregistre une fermeture différée; elle ne détruit jamais la clé tant que la profondeur du callback n'est pas revenue à zéro. Un succès signifie que la fermeture différée devient effective dès la sortie complète du callback, avant tout autre appel externe : un succès sans effet est interdit. Un rejet explicite laisse le port utilisable jusqu'à une fermeture externe ultérieure. Cette fermeture externe ferme réellement le port dans ce second cas et reste sûre/idempotente dans le premier.

### `notification`

La table reste l'historique logique utilisateur. Elle gagne au minimum :

- `idempotency_key` unique
- `delivery_state` agrégé
- `sent_at` nullable avec la nouvelle sémantique d'acceptation
- `expires_at`

### `notification_delivery` (outbox)

- `id`, clé primaire et `apns_id` stable
- `notification_id`
- `device_registration_id`, FK non supprimable vers `device_registration.registration_id`
- `idempotency_key` unique
- `state`
- `attempt_count`, `max_attempts`
- `next_attempt_at`, `last_attempt_at`, `expires_at`
- `lease_owner`, `lease_expires_at`
- `accepted_at`
- `provider_status`, `provider_reason`, `provider_request_id`
- `last_error_class`
- `created_at`, `updated_at`
- contrainte unique `(notification_id, device_registration_id)` et `deliveryKey = (recipientKey, registrationId, provider)`

La FK historique interdit que l'arrivée d'une nouvelle association pour une même installation ne redirige une ligne créée pour l'ancienne. Une table append-only `notification_delivery_attempt` peut conserver les métadonnées assainies de chaque tentative : numéro, timestamps, outcome, statut HTTP et raison APNs. Elle ne stocke ni secret, ni JWT, ni token brut, ni payload privé.

## API Contracts

### Register installation

`POST /api/notifications/register` devient idempotent pour :

- `installationId`
- `platform = IOS`
- `token`
- `environment`
- `topic`
- version app optionnelle pour diagnostic

Le `userId` vient exclusivement du JWT. La réponse contient le `registrationId` d'association et l'état confirmé; le client conserve ce seul identifiant comme `backendRegistrationId`. La répétition de la même demande `(installationId, userId, platform, environment, topic, tokenHash)` retourne l'association active existante sans en créer une seconde. Une rotation avec un nouveau hash conserve ce `registrationId` seulement lorsque l'association active a exactement le même compte et scope; un compte ou scope différent est une transaction `UNREGISTERED(ACCOUNT_CHANGED|SCOPE_CHANGED) → ACTIVE` créant une nouvelle association. Les anciens clients sans `installationId` sont temporairement mappés vers une identité legacy déterministe jusqu'à la fin de la migration.

### Unregister installation

Le chemin canonique est `DELETE /api/notifications/registrations/{registrationId}` : il est authentifié, vérifie que l'association active appartient au sujet JWT et est idempotent. `204` ou une réponse « déjà absent » est un succès terminal. Il ne supprime ni l'installation stable, ni l'historique, ni les autres appareils du compte. Un alias temporaire par `installationId` ne peut désinscrire que `activeRegistration(installationId)` appartenant au même sujet; il est réservé à la compatibilité et ne devient jamais ambiguëment multi-compte.

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

1. Faire approuver ce changement puis créer/reviewer les deux modèles runtime XState et la saga de compatibilité legacy/v2 avec leurs tests de transition.
2. Ajouter les nouvelles tables/colonnes de manière additive, avec worker désactivé.
3. Backfiller chaque ancienne ligne `(user_id, platform)` vers une `device_installation` et une `device_registration` legacy déterministes : `installationId = HMAC-SHA-256(WAKEVE_NOTIFICATION_LEGACY_IDENTITY_HMAC_KEY, "wakeve/legacy-installation/v1" || legacyPrimaryKey)` et `registrationId = HMAC-SHA-256(WAKEVE_NOTIFICATION_LEGACY_IDENTITY_HMAC_KEY, "wakeve/legacy-registration/v1" || legacyPrimaryKey)`, encodés de façon opaque. La clé de migration stable est fournie par le secret manager/configuration du backend, n'est ni une constante ni une dérivation réversible, et reste disponible pendant toute la fenêtre de migration. Si cette clé est absente ou vide, le backfill échoue fermé avant toute mutation. Le même `legacyPrimaryKey` immuable produit donc les mêmes deux identités à chaque reprise, y compris si le token brut change; la migration fait un upsert idempotent et, répétée après fermeture/réouverture, retourne `created=false` sans supprimer, fusionner ni réécrire les autres installations v2. La migration ne peut pas recréer les appareils déjà écrasés ; cette limite est documentée.
4. Déployer les endpoints compatibles et faire dual-write ancien/nouveau format pendant la fenêtre de compatibilité.
5. Mettre à jour iOS pour envoyer `installationId`, environnement et topic, et pour exposer la machine d'inscription.
6. Exécuter les tests contractuels et une livraison sandbox sur appareil réel.
7. Configurer la clé APNs production, valider l'archive et l'entitlement `aps-environment=production`, puis livrer à TestFlight avec le worker en shadow mode si possible.
8. Activer un faible pourcentage de livraisons, surveiller outbox/auth/rejets, puis augmenter progressivement.
9. Arrêter le dual-read/dual-write legacy seulement après une version minimale adoptée et un changement de dépréciation approuvé.

## Rollback Plan

- Un feature flag coupe le worker/provider APNs sans supprimer les lignes queued/retry.
- Le rollback applicatif bascule un adapter/feature flag compatible dans le même binaire déployé; il ne revient jamais à un ancien binaire qui exigerait un token brut. Les migrations additives ne sont pas inversées destructivement. La projection backend protégée choisit, pour le `(user_id, platform, environment, topic)` compatible, l'association `ACTIVE` de plus récent `last_registered_at`, puis `created_at`, puis `registration_id` croissant en cas d'égalité. `LegacyNotificationTokenRead` expose uniquement `selectedRegistrationId`, `tokenHash`, scope et métadonnées de cycle de vie; il n'expose jamais `token_ciphertext` ni le token brut. Le port provider reçoit ensuite ce seul `selectedRegistrationId`, déchiffre le secret seulement dans la portée mémoire de l'appel fournisseur et ne retourne ni token ni ciphertext à la projection. La projection ne supprime ni ne désactive les autres installations; le nouveau chemin continue de conserver leur historique. Les clients N-1 restent compatibles par l'API, non par accès direct à la base. Si le schéma legacy ne porte pas le scope, l'environnement/topic de compatibilité est celui explicitement configuré pour la fenêtre de rollout, jamais un défaut implicite.
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
- Annulations : avant prompt, en retry d'inscription, en attente de token et queued ; un test interdit explicitement l'annulation d'un vol de logout en cours ou d'une requête potentiellement écrite.
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
- **Logout offline** : attendre la désinscription peut ralentir la déconnexion. Mitigation : état visible, retry explicite, résultat bloqué explicite et endpoint idempotent ; après ouverture du vol, l'annulation ne sacrifie pas la confidentialité inter-comptes.
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
- La désinscription logout est par `registrationId` actif et précède l'effacement du JWT; `installationId` reste l'ancre stable locale.
- L'idempotence couvre Wakeve et son outbox ; elle ne prétend pas offrir une livraison APNs exactement une fois.
- Le changement reste limité aux notifications d'organisation d'événement existantes.

## Reviewed Outbox Boundary

| Record | Store and exact owner | Producer | Consumer | Acknowledgement |
|---|---|---|---|---|
| `confirmation_effect_outbox` | local SQLDelight; `DatabaseEventRepository` | atomic confirmation command | local sync publisher | backend receipt for `domainEventId` and `effectKey` |
| `domain_event_ingestion` + `notification_logical` | backend database; notification backend | server envelope consumer | ingestion projector | exact backend transaction receipt |
| `notification_recipient` | backend database; notification backend | server envelope consumer | recipient resolver | resolved, pending, suppressed, expired, or terminal |
| `notification_delivery` | backend database; notification backend | recipient fan-out | provider worker | provider result persisted against `deliveryKey` |
| `calendar_artifact` | backend database; calendar backend | calendar fan-out | calendar provider worker | result persisted against `calendarArtifactKey` |

The local transaction ends after the decision, receipt, sync metadata, and exactly one domain-effect envelope are durable. It MUST NOT create a server recipient, provider delivery, or calendar artifact and is never a server outbox. No atomic commit spans local SQLDelight and the backend database. Server acceptance is an idempotent acknowledgement, not evidence of provider or calendar acceptance.

Normative new-write keys use injective length-prefix version 2: `ek2` for `(domainEventId,effectType,schemaVersion)`, `rk2` for `(effectKey,participantId,channel)`, `dk2` for `(recipientKey,registrationId,provider)`, and `ck2` for `(effectKey,participantId,calendarProvider)`. A colon inside a source identifier remains data. The sole local legacy read format is `poll-date-confirmed:<eventId>:<slotId>:v1:confirmation`; migration requires the authoritative local `(eventId,slotId,domainEventId,effectKey)` tuple, verifies both strings exactly, and maps to `pdc2(eventId,slotId)` plus canonical `ek2`. It never parses the legacy string. An exact replay is idempotent, while unknown versions, mismatches, corrupt records and a same-key/different-tuple collision are quarantined. Writes are v2-only. `installationId` remains a stable grouping identity, while `registrationId` binds the historical account, token and APNs scope actually targeted by a delivery.

`decisionSyncStatus` (`pending | acknowledged | conflicted | failed`) describes decision replication only. `effectDispatchStatus` (`notDispatched | pendingRecipient | queued | partiallyDispatched | dispatched | terminalFailure`) describes downstream effects only. They MUST NOT be collapsed. An acknowledged decision may have pending effects; retrying an effect MUST NOT retry or revert decision synchronization.

When registrations are resolved during ingestion, the backend transaction includes `domain_event_ingestion`, `notification_logical`, recipient rows and exactly one delivery per distinct registration before provider I/O. When an intended participant has no eligible target, that transaction retains `notification_recipient(status=pendingTarget)` and no delivery. Token registration or membership reconciliation later uses a distinct effect/checkpoint/receipt-fenced fan-out transaction. The first exact durable ACK to `targeted` freezes a sorted, unique set containing exactly one `deliveryKey` per `registrationId`; duplicate reconciliation and restore do not append, replace or retarget deliveries. `targetExpired` and `targetExhausted` are terminal for that `effectKey`, so a later token cannot resurrect it. The client never guesses a device target.

Retry domains are separate: local envelope publication by `effectKey`; backend recipient resolution by `recipientKey`; APNs delivery by `deliveryKey`; calendar fan-out by `calendarArtifactKey`. Each has its own attempts, expiry, backoff, terminal state, and acknowledgement.

### Durable provider-result checkpoints

`notificationDeliveryMachine` is the single provider delivery authority. A provider attempt may report only a correlated HTTP or transport observation; it cannot submit an outcome. The model checks `deliveryKey + correlationId + attempt + leaseHolder + leaseVersion + fencingToken`, classifies internally, and stages the result. The durable delivery projection stays unchanged until `PROVIDER_RESULT_DURABLY_RECORDED` matches the staged `effectId`, monotonic checkpoint revision, closed delivery authority, authority fence and lease reference. The staged checkpoint contains only the closed outcome, sanitized reason, HTTP status, acceptance time or retry schedule. Unknown or stale observations and acknowledgements are inert.

Restore never relies on XState `entry` replay. A worker explicitly sends `PROVIDER_RESULT_PERSISTENCE_REQUESTED` for the same idempotent effect reference, before or after a crash, and an ACK before this emission marker is ignored. Provider retry reasons use a closed vocabulary. `500` and `503` remain retryable without a reason. `Retry-After` is accepted only when it is a safe integer strictly in the future, no more than 300 seconds from the persisted logical clock and strictly before business expiry. Otherwise the classifier records a typed fail-closed result. An absent valid header uses injected full jitter with a one-second floor, 300-second cap, saturated exponent and attempt budget.

### Shadow-write migration

1. Add schemas and dual-read compatibility; rollback keeps the legacy path authoritative.
2. Shadow-write new rows with sends disabled and reconcile identities/counts; rollback stops shadow writes.
3. Assign exactly one `delivery_authority` from the closed set `legacy | outbox-v2` per logical delivery under a uniqueness constraint. No runtime string can introduce another authority.
4. Enable v2 for a bounded cohort only under a durable unexpired recovery lease and after exact emitted-and-ACKed `pauseLegacy`, `reconcileLegacy`, and `commitOutboxV2` effects with zero active attempts.
5. Rollback requires a durable unexpired recovery lease and exact emitted-and-ACKed `pauseOutboxV2`, `reconcileOutboxV2`, and `commitLegacy` effects with zero active attempts.
6. Retire legacy writes only after the replay horizon and reconciliation checkpoint pass.

At no point may both authorities send the same `deliveryKey`.
