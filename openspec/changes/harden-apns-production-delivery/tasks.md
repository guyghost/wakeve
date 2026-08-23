## 0. Approval gate

- [x] 0.1 Faire approuver `proposal.md`, `design.md` et le delta `notification-management` avant toute modification de code de production.
  - Acceptance: le scope, les deux machines, les terminaux, la sémantique de `sent_at`, la migration et le rollback sont explicitement approuvés.
  - Verify: `openspec validate harden-apns-production-delivery --strict` et trace de review humaine.
  - Evidence: approbation utilisateur explicite reçue dans ce tour; validation OpenSpec stricte réussie après les amendements de review.
  - Files: `openspec/changes/harden-apns-production-delivery/**` uniquement.

## 1. Model

- [x] 1.1 Créer la machine XState v5 `iosNotificationRegistration` comme source de vérité.
  - Acceptance: contexte/événements typés, états `checkingPermission`, `notDetermined`, `requestingPermission`, `denied`, `registeringApns`, `awaitingAuthentication`, `registeringBackend`, `retry`, `registered`, `unregistering`, `unregistered`, `cancelled` et `misconfigured`; guards et effets nommés; aucune logique métier dans l'UI.
  - Verify: tests de transitions nominales, refusées, permissions, erreurs, retry, annulation, callback périmé et logout.
  - Files: `models/ios-notification-registration.machine.ts` et tests de modèle associés.

- [x] 1.2 Créer la machine XState v5 `notificationDelivery` comme source de vérité.
  - Acceptance: tous les états du design sont présents; classification APNs, policy, quiet hours, token absent, lease, retry, résultat inconnu, expiration et terminaux sont déterministes.
  - Verify: tests de transition table-driven et invariants `sent_at`/idempotence/lease.
  - Evidence: machine canonique unique renforcée après trois fix-loops de review; policy, quiet hours, token absent, auth/refresh, leases/fencing, retry/unknown outcome, expiry, fan-out pending, calendrier et autorité de cutover sont couverts par une matrice de régression explicite.
  - Files: `models/notification-delivery.machine.ts` et tests de modèle associés.

- [x] 1.3 Faire la review formelle des deux modèles avant de continuer.
  - Acceptance: cas nominaux, erreurs, annulations, retries, permissions, états terminaux, effets et invariants sont couverts; aucune transition implicite ou pilotée par texte libre.
  - Verify: matrice de couverture approuvée et fixtures de transitions exportées pour Swift/Kotlin.
  - Evidence: review finale `APPROVED` après fermeture des chemins terminaux non durables, corrélation/fencing, compatibilité des 26 scénarios historiques et migration fermée des clés locales existantes; reproduction orchestrateur fraîche `pnpm test:models` 149/149, OpenSpec strict et `git diff --check` verts.
  - Files: `/models`, documentation de review sous ce changement si nécessaire.

- [x] 1.4 Modéliser et faire reviewer la saga de compatibilité d'inscription legacy/v2 avant toute dual-write de production.
  - Acceptance: intention de reconciliation durable avant la première écriture, ordres register/unregister explicites pour N et N-1, clé canonique injective, retries séparés, reprise par lease durable holder/version/fencing/checkpoint avec horloge logique corrélée et monotone sans supposer le rejeu des `entry` XState, `RETRY_DUE` sans temps appelant et schedules strictement futurs, ACKs corrélés par `effectId + checkpointRevision + fencingToken`, terminaux `converged|blocked`, aucune prétention de transaction cross-datastore, cutover/rollback par checkpoint et aucune suppression d'une installation v2 hors cible.
  - Verify: tests XState table-driven register/unregister, crash après chaque étape, acquisition/restauration de lease, holder/foreign/expired/ancien fencing/concurrence, horloge sous/à deadline, rewind, schedule passé et restauration d'heure, ACKs retardés sans mutation, émission réelle de l'effet après restauration + `RECOVERY_REQUESTED`, anti-collision de clé, duplicate, restart, indisponibilité de chaque store, budget épuisé, N/N-1, cutover et rollback; review formelle avant implémentation serveur.
  - Evidence: amendement full-jitter strictement futur ajouté après review production (`sample=0|NaN → 1 s`, clamp `[0,1]`, plafond 300 s et saturation d'exposant); nouvelle re-review du modèle requise avant reprise Kotlin.
  - Files: `models/legacy-notification-registration-compatibility.machine.ts`, tests associés et précisions OpenSpec uniquement.

- [x] 1.5 Modéliser et faire reviewer la migration fail-closed de l'unicité `request_key` avant toute reprise Kotlin.
  - Acceptance: preflight read-only avant index/routes/scheduler, `READY` seulement après absence de doublon et index confirmé, `BLOCKED_DUPLICATES` sans DDL ni mutation, diagnostics digest/count assainis, résolution opérateur fenced par `scanRevision`, identité métier identique, groupe quiescent sans lease, archive immuable des lignes/effets non canoniques avant suppression, reprise par lease CAS durable holder/version/fence/checkpoint avec horloge logique monotone et émission unique, ACK exact effet/révision/fence, rollback fail-closed.
  - Verify: tests XState table-driven fresh/already-indexed, doublons bloqués, diagnostic assaini, résolutions stale/foreign/divergent/active refusées, deux workers réellement restaurés, absence de rejeu `entry`, holder/foreign/concurrent/double recovery, expiration et réacquisition version+fence supérieures, ACK stale, crash/restart aux checkpoints archive/delete/scan/index, migration répétée, ACK d'archive incomplet refusé et audit durable; review formelle avant toute modification Kotlin supplémentaire.
  - Evidence: modèle et tests écrits; test ciblé 8/8 après preuve RED sur l'autorité de recovery absente, review formelle encore requise.
  - Files: `models/legacy-compatibility-unique-migration.{core,machine,test}.ts` et précisions OpenSpec uniquement.

## 2. Tests-first contracts

- [x] 2.1 Écrire d'abord les tests iOS en échec pour la machine d'inscription.
  - Acceptance: les tests prouvent absence de prompt au lancement, action explicite en `notDetermined`, retour Réglages, callbacks APNs, inscription backend récupérable et ordre unregister avant JWT clear.
  - Verify: exécuter les tests ciblés et conserver la preuve RED attendue avant implémentation.
  - Evidence: contrats RED observés avant implémentation (`RegistrationRetryJitterSource`, `RegistrationRetryBackoff` et assainissement des logs absents); review finale `APPROVED`; reproduction indépendante 64/64 ciblés puis 484/484 sur la suite iOS complète, build simulateur vert.
  - Files: tests sous `iosApp/WakeveTests` ou la cible de tests iOS canonique.

- [x] 2.2 Écrire d'abord les tests shared/server en échec pour persistance, provider et delivery. (RED review approved; evidence: `evidence/task-2.2-shared-server-red.md`)
  - Acceptance: faux succès actuel, `sent_at` prématuré, appareil unique, absence d'idempotence/outbox, provider non implémenté et classifications APNs sont tous capturés.
  - Verify: tests ciblés échouant pour la raison attendue, sans supprimer ni inverser les assertions de sécurité existantes.
  - Files: tests `shared` et `server` correspondants.

- [x] 2.3 Écrire d'abord les contrats serveur en échec pour la saga de compatibilité d'inscription legacy/v2.
  - Acceptance: intention durable avant effets, ordre N/N-1, génération N sans écriture legacy, reprise par checkpoint, leases/fencing, ACKs corrélés, horloge/retries, convergence/blocage, identité HMAC assainie, isolation utilisateur et préservation multi-appareil sont observables sans imposer une transaction cross-datastore.
  - Verify: les deux classes ciblées échouent uniquement parce que l'API et le datastore saga sont absents; le `202` exige seulement l'intention durable acceptée, jamais l'exécution immédiate du worker.
  - Evidence: preuve RED à `:server:compileTestKotlin` sur les seuls symboles `LegacyCompatibility*`/`openCompatibilitySagaStore` absents; fix-loop de sur-spécification terminé; review indépendante finale `APPROVED`; `git diff --check` propre.
  - Files: `LegacyNotificationRegistrationCompatibilitySagaRedTest.kt` et `LegacyNotificationCompatibilityRoutesRedTest.kt`.

## 3. Schema, migration and idempotent outbox

- [x] 3.1 Ajouter le schéma multi-installation et les migrations additives.
  - Acceptance: le datastore **backend** possède `device_installation(installationId, platform canonique)` et l'historique `device_registration(registrationId)` avec FK, états `ACTIVE|INVALID|UNREGISTERED`, `invalid_reason` distinct de `unregistered_reason` typé, contrainte d'au plus une association active par installation, environnement/topic et token chiffré au repos. Les lectures ordinaires et `LegacyNotificationTokenRead` n'exposent ni token ni ciphertext; seul un port provider séparé déchiffre le token dans la portée de l'appel fournisseur. Rotation garde l'association seulement au même compte/scope; un changement de compte ou scope clôture atomiquement l'ancienne association avec son motif puis crée une nouvelle association; invalidation par `registrationId` et réinscription conservent l'historique. Le backfill HMAC à clé de migration configurée échoue fermé sans clé et dérive ses identités d'une clé de ligne legacy immuable. La projection rollback sélectionne une association explicitement sans supprimer les autres installations. Le SQLDelight local conserve uniquement `confirmation_effect_outbox` et les compatibilités locales explicitement prévues; il ne possède ni `device_installation`, ni `device_registration`, ni livraison fournisseur.
  - Verify: tests backend avec configuration immutable injectée indépendante de l'environnement ambiant, fermeture/réouverture réelle, ciphertext au repos, DTO/diagnostics assainis, callback provider incapable de retourner le token, fermeture sérialisée avec register/déchiffrement, close réentrant refusé/différé sans effacement de clé en callback et sans succès no-op (succès => port fermé dès la sortie du callback; rejet => port ouvert jusqu'au close externe qui le ferme), deux appareils, relecture de toutes les colonnes après rotation/invalidation/réinscription, changements séparés de topic seul et environnement seul, rollback transactionnel sur faute injectée après tentative de fermeture, changements atomiques de compte avec nouveaux `registrationId`, raisons de fermeture, invalidation ciblée, plateforme canonique, contraintes SQL directes sur chaque combinaison status/timestamps/reasons, rejet des chemins relatifs/mémoire/URI/symboliques y compris swaps après résolution sans mutation de cible, parent POSIX broad rejeté sans chmod, parent dédié créé owner-only et fichier broad rejeté ou normalisé, HMAC configuré/fail-closed, backfill répété après reopen avec token différent mais mêmes IDs, et rollback provider-only déterministe sans mutation des historiques; test SQLDelight excluant les deux tables backend; aucun test ne prétend à une transaction cross-datastore.
  - Evidence: review finale indépendante `APPROVED`; 24/24 contrats registration verts (12 contrat, 8 sécurité, 4 fix-loop), ouverture native `SQLITE_OPEN_NOFOLLOW` partagée par store/provider, cible de swap inchangée, suite serveur complète verte avant le RED suivant.
  - Files: `server/src/main/kotlin/com/guyghost/wakeve/notification/**`, migrations/datastore backend et tests `server/src/test/kotlin/com/guyghost/wakeve/notification/**`; SQLDelight local seulement pour ses enveloppes/compatibilités explicitement séparées.

- [x] 3.2 Ajouter l'outbox et l'état de livraison par inscription.
  - Acceptance: le datastore **backend** possède `notification_recipient`, `notification_delivery` et l'outbox par association d'inscription; chaque livraison garde une FK historique non-cascadée vers `registrationId` et une clé `(recipientKey, registrationId, provider)`. Sa transaction persiste notification/résolution/livraisons, contraintes d'idempotence, leases, tentative, prochaine échéance, expiration, résultat fournisseur et `accepted_at`. SQLDelight local ne crée que l'enveloppe d'effet de confirmation et ne crée jamais de destinataire, livraison ou artefact fournisseur.
  - Verify: tests backend de crash/reprise, workers concurrents, duplicate enqueue, expiration, lecture historique et frontière sans transaction cross-datastore.
  - Evidence: implémentation durable finale indépendamment reviewée `APPROVED`; ingestion atomique de la notification logique, des destinataires et des livraisons gelées, FK historique par `registrationId`, identités injectives `ek2/rk2/dk2`, contraintes SQL, migrations additives/concurrentes et reprise après crash sont couvertes. Reproduction orchestrateur fraîche: serveur 455/455, frontière outbox locale 21/21, OpenSpec strict et `git diff --check` verts.
  - Files: `server/src/main/kotlin/com/guyghost/wakeve/notification/**`, datastore/migrations backend, worker/repositories et tests serveur; `shared/**/confirmation_effect_outbox` seulement pour l'enveloppe locale.

- [x] 3.3 Corriger la sémantique d'envoi shared.
  - Acceptance: aucune erreur transport n'est absorbée comme succès; `sent_at` reste null avant acceptation; chaque `registrationId` conserve son résultat.
  - Verify: remplacer le test de faux succès par des assertions de queue/échec/acceptation conformes au modèle et exécuter la suite notification ciblée.
  - Evidence: review finale indépendante `APPROVED`; ordre durable avant I/O et redaction verrouillés; 63/63 contrats ciblés, 58/58 service, 170/170 package notification et 1773/1773 shared JVM verts.
  - Evidence: `task-apns-shared-delivery-semantics-report.md`; 2/2 contrats APNs ciblés, 168/168 tests notification JVM et 1771/1771 tests shared JVM passent.
  - Files: `shared/.../notification/NotificationService.kt` et tests.

## 4. iOS registration implementation

- [x] 4.1 Brancher `APNsService` et AppDelegate sur le contrat `iosNotificationRegistration`.
  - Acceptance: lecture de statut sans prompt, action explicite, callbacks corrélés, token refresh, auth différée et erreurs observables correspondent aux fixtures du modèle.
  - Verify: tests iOS de machine/adapters et test de régression « no prompt at launch ».
  - Evidence: review indépendante `APPROVED`; callbacks UIKit coalescés et corrélés, inscription backend et retries conformes au modèle; 64/64 contrats ciblés et 484/484 suite iOS complète, build simulateur vert.
  - Files: `iosApp/src/Services/APNsService.swift`, `AppDelegate.swift`, adapters/fixtures et tests.

- [x] 4.2 Ajouter l'action accessible pour le statut `notDetermined` et les états de récupération.
  - Acceptance: la vue affiche Activer pour `notDetermined`, Ouvrir Réglages pour `denied`, et l'état registered/retry/misconfigured sans inventer sa propre logique.
  - Verify: tests ViewModel/UI, localisation, Dynamic Type et VoiceOver ciblés.
  - Evidence: review UI statique `APPROVED`; présentation exhaustive pilotée par l'adapter, retry passif, actions Activer/Réglages, localisations en/fr/es/it/pt, cible 44 pt et libellés VoiceOver; suite iOS complète 484/484.
  - Files: `NotificationPreferencesView.swift`, ViewModel et localisations nécessaires.

- [x] 4.3 Séquencer le logout avec la désinscription authentifiée par installation.
  - Acceptance: le JWT reste disponible jusqu'à succès idempotent de désinscription; échec/retry/cancel sont explicites; les autres appareils restent inscrits.
  - Verify: test d'ordre des effets, offline, `5xx`, déjà absent et deux appareils.
  - Evidence: review indépendante `APPROVED`; JWT conservé jusqu'au terminal `unregistered`, retries corrélés, blocage sans effacement d'identifiants, observers faibles multi-consommateurs, DELETE canonique/fallback borné et `404` non traité comme succès; 64/64 contrats ciblés et 484/484 suite iOS complète.
  - Files: `AuthStateManager.swift`, `APNsService.swift`, auth/notification adapters et tests.

## 5. Backend APNs provider

- [x] 5.1 Implémenter l'authentification token-based APNs et la validation fail-closed.
  - Acceptance: JWT ES256 avec clé `.p8`, Key ID/Team ID/topic/environnement obligatoires, cache/rotation sûrs, secrets absents des logs et readiness non prête si production est mal configurée.
  - Verify: tests JWT/config/redaction/rotation et scan de secrets.
  - Evidence: review indépendante `COMPLETE_REVIEWABLE`; validation PKCS#8/P-256 fail-closed, JWT ES256, cache versionné sans secret et readiness explicite couverts; reproduction orchestrateur fraîche `:server:apnsContractTest --rerun-tasks --no-configuration-cache` verte, 36/36.
  - Files: `server/.../notification/PushNotificationSender.kt`, configuration/DI/readiness et tests.

- [x] 5.2 Implémenter le transport HTTP/2 APNs et les headers obligatoires.
  - Acceptance: sandbox et production utilisent les endpoints Apple courants; `apns-id`, topic, push type, expiration, priorité et payload validé sont envoyés.
  - Verify: faux serveur HTTP/2/contract tests pour endpoint, headers, payload et connexions.
  - Evidence: review indépendante `COMPLETE_REVIEWABLE`; transport Netty HTTP/2 + ALPN, TLS 1.2/1.3, endpoints sandbox/production et headers APNs couverts par le serveur loopback; reproduction orchestrateur fraîche 36/36.
  - Files: provider APNs backend et tests.

- [x] 5.3 Implémenter la classification déterministe des réponses et le circuit provider.
  - Acceptance: HTTP `200`, token invalide, payload rejeté, auth bloquée, `429`, `5xx`, erreur avant écriture et outcome inconnu produisent exactement les états du modèle.
  - Verify: tests table-driven couvrant chaque code/reason du design et raison inconnue.
  - Evidence: review indépendante `COMPLETE_REVIEWABLE` pour la couche provider; matrice fermée, refresh auth unique, circuit persistant, distinction before-write/outcome-unknown et corrélation couverts par 36/36 contrats frais. La mutation durable des états de livraison reste volontairement ouverte en 6.2.
  - Files: fonctions pures de classification, machine adapter, worker et tests.

## 6. Registration API and delivery worker

- [x] 6.1 Étendre les endpoints d'inscription/désinscription avec compatibilité legacy.
  - Acceptance: user issu du JWT, `installationId` stable, `registrationId` d'association, environnement/topic, idempotence, désinscription de cette seule association avec motif typé et mapping temporaire des anciens clients.
  - Verify: tests route/auth/validation/multi-device/legacy et accès croisé interdit.
  - Evidence: review finale indépendante `APPROVED`; custody révocable, coalescence `request_key`, CAS/recovery multi-processus, guards N-1, full-jitter `[1,300]`, migration fail-closed des doublons et gates `READY` sur toutes les surfaces publiques sont couverts. Reproduction orchestrateur fraîche: serveur forcé sans cache 390/390, shared `NotificationServiceTest` 58/58, modèles 131/131, OpenSpec strict et `git diff --check` verts. La tâche 7.1 reste ouverte pour rollout/cutover/rollback opérationnel.
  - Files: `NotificationRoutes.kt`, DTOs/services et tests.

- [x] 6.2 Implémenter le worker durable conforme à `notificationDelivery`.
  - Acceptance: leases, policy, quiet hours, token absent, retries full-jitter, `Retry-After`, expiration, circuit auth et reprise après crash sont persistés.
  - Verify: tests horloge déterministe, concurrence, crash/restart, retry exhaust et annulation.
  - Evidence: review indépendante finale `APPROVED` après fermeture des observations classifiées par l'appelant, des ACKs périmés et des reprises sans nouveau lease/fence. Le worker persiste leases, checkpoints provider/target, retry/expiration, circuit credential, invalidation ciblée, fan-out gelé et agrégat d'effet; 455/455 tests serveur, 36/36 contrats APNs, 63/63 tests shared notification et 149/149 tests modèle passent fraîchement. Le worker reste volontairement non activé dans `Application.kt` avant les gates de rollout 7.x.
  - Files: worker/scheduler backend, repositories et tests.

- [ ] 6.3 Ajouter métriques, alertes et logs assainis.
  - Acceptance: profondeur/âge outbox, états, latence d'acceptation, invalidation et auth blocked sont observables sans token/JWT/payload privé.
  - Verify: tests de redaction, métriques et alerte synthétique provider bloqué.
  - Files: telemetry backend, runbook et tests.

## 7. Migration, rollout and rollback

- [ ] 7.1 Valider dual-read/dual-write et compatibilité de version.
  - Acceptance: ancien et nouveau clients peuvent coexister pendant la fenêtre via API; les identités legacy sont HMAC avec clé de migration configurée, la projection backend provider-only de rollback a un ordre de sélection explicite sans DTO token brut, le rollback est un adapter/feature flag compatible dans le binaire déployé, aucune inscription supplémentaire n'est écrasée et les lignes queued survivent au rollback.
  - Verify: test de déploiement N/N-1 et répétition de migration.
  - Files: migrations, feature flags et tests de compatibilité.

- [ ] 7.2 Documenter et répéter la rotation de clé, le circuit breaker et le rollback.
  - Acceptance: runbook avec désactivation worker, conservation outbox, révocation/rotation clé et reprise contrôlée.
  - Verify: exercice en environnement non-production avec horodatage et résultat.
  - Files: documentation opérationnelle et configuration.

## 8. Verify on real infrastructure

- [ ] 8.1 Prouver le flux complet sur appareil réel avec APNs sandbox.
  - Acceptance: permission accord/refus, inscription, foreground/background/app terminée, deep link, retry/invalidation et logs Apple sont couverts.
  - Verify: artefact assaini contenant build/device/iOS/timestamps, IDs Wakeve/APNs, transitions et résultat visible, sans secrets ni contenu privé.
  - Files: dossier de preuves de release approuvé par le projet, aucun token brut.

- [ ] 8.2 Vérifier l'archive et la signature production.
  - Acceptance: bundle/topic corrects, entitlement `aps-environment=production`, configuration backend production et aucun secret embarqué dans l'app.
  - Verify: inspection de l'archive/signature/entitlements et readiness backend.
  - Files: configuration de build/signing autorisée et preuve assainie.

- [ ] 8.3 Prouver le flux TestFlight/APNs production.
  - Acceptance: invitation et confirmation/rappel sur foreground/background/app terminée, quiet hours, deux appareils, token refresh, tap/deep link et logout sans notification ultérieure.
  - Verify: matrice TestFlight signée avec `notificationId`, `deliveryId`, `apns-id`, terminal et observation appareil ; aucun échec critique ouvert.
  - Files: artefacts de preuve uniquement.

- [ ] 8.4 Exécuter les gates finaux et faire une review indépendante.
  - Acceptance: tests modèles/iOS/shared/server, migrations, sécurité, lint/build ciblés et `git diff --check` passent ; la review ne trouve aucune logique de transition hors modèle.
  - Verify: commandes et sorties attachées ; `openspec validate harden-apns-production-delivery --strict` repasse.
  - Files: aucun changement hors scope.

## 9. Completion gate

- [ ] 9.1 Déclarer la capacité APNs prête pour la production uniquement après toutes les preuves.
  - Acceptance: toutes les tâches précédentes sont cochées, aucun statut fictif, aucun secret exposé, rollback prêt et TestFlight production réussi.
  - Verify: sign-off produit, iOS, backend, sécurité et exploitation.
  - Files: `tasks.md` mis à jour seulement après vérification réelle.

## 10. Reviewed outbox boundary

- [x] 10.1 `@tests` Prove `confirmation_effect_outbox` is local and owned by `DatabaseEventRepository`, while `notification_recipient` and `notification_delivery` are backend-owned.
  - Evidence: les contrats `DatabaseEventRepositoryConfirmDateTest`/`DatabaseEventRepositoryConfirmationDurabilityBlockerTest` sont 21/21 verts; `BackendNotificationIngestionDurabilityRedTest` vérifie que le schéma backend ne contient pas `confirmation_effect_outbox` et possède sa transaction propre.
- [ ] 10.2 `@codegen` Add stable uniqueness for `domainEventId`, `effectKey`, `recipientKey`, `deliveryKey`, and `calendarArtifactKey` without a cross-database transaction claim.
- [x] 10.3 `@tests` Prove backend acknowledgement advances `decisionSyncStatus` only and cannot imply `effectDispatchStatus` or provider acceptance.
  - Evidence: `BackendNotificationIngestionDurabilityRedTest` et `BackendNotificationEffectAggregateReviewRedTest` couvrent les projections `pendingRecipient|queued|partiallyDispatched|dispatched|terminalFailure` sans mutation de `decisionSyncStatus` ni acceptation fournisseur implicite.
- [x] 10.4 `@tests` Prove a missing target remains pending and fans out after installation registration, subject to its own expiry and retry policy.
  - Evidence: `BackendNotificationPendingTargetDurabilityRedTest` et les contrats expiry/checkpoint prouvent reprise après reopen, lease unique, ACK exact receipt/fence, un delivery par registration, retry durable et non-résurrection après expiration.
- [ ] 10.5 `@codegen` Execute shadow-write migration with unique `delivery_authority`, reconciliation, cohort cutover, and rollback checkpoints.
- [ ] 10.6 `@review` Verify decision sync, recipient resolution, APNs delivery, and calendar fan-out have independent retries and cannot mutate one another's state authority.
