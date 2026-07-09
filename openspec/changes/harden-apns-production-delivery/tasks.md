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
  - Files: `models/notification-delivery.machine.ts` et tests de modèle associés.

- [x] 1.3 Faire la review formelle des deux modèles avant de continuer.
  - Acceptance: cas nominaux, erreurs, annulations, retries, permissions, états terminaux, effets et invariants sont couverts; aucune transition implicite ou pilotée par texte libre.
  - Verify: matrice de couverture approuvée et fixtures de transitions exportées pour Swift/Kotlin.
  - Evidence: review finale `APPROVED`; `pnpm test:models` passe avec 62/62 tests, couvrant les deux machines et leurs invariants revus.
  - Files: `/models`, documentation de review sous ce changement si nécessaire.

## 2. Tests-first contracts

- [ ] 2.1 Écrire d'abord les tests iOS en échec pour la machine d'inscription.
  - Acceptance: les tests prouvent absence de prompt au lancement, action explicite en `notDetermined`, retour Réglages, callbacks APNs, inscription backend récupérable et ordre unregister avant JWT clear.
  - Verify: exécuter les tests ciblés et conserver la preuve RED attendue avant implémentation.
  - Files: tests sous `iosApp/WakeveTests` ou la cible de tests iOS canonique.

- [x] 2.2 Écrire d'abord les tests shared/server en échec pour persistance, provider et delivery. (RED review approved; evidence: `evidence/task-2.2-shared-server-red.md`)
  - Acceptance: faux succès actuel, `sent_at` prématuré, appareil unique, absence d'idempotence/outbox, provider non implémenté et classifications APNs sont tous capturés.
  - Verify: tests ciblés échouant pour la raison attendue, sans supprimer ni inverser les assertions de sécurité existantes.
  - Files: tests `shared` et `server` correspondants.

## 3. Schema, migration and idempotent outbox

- [ ] 3.1 Ajouter le schéma multi-installation et les migrations additives.
  - Acceptance: plusieurs appareils iOS actifs par utilisateur, rotation/invalidation isolée, environnement/topic persistés et backfill legacy documenté.
  - Verify: tests migration aller, données legacy, deux appareils, changement de compte et rollback applicatif.
  - Files: `shared/src/commonMain/sqldelight/com/guyghost/wakeve/Notification.sq`, migrations et tests SQLDelight.

- [ ] 3.2 Ajouter l'outbox et l'état de livraison par appareil.
  - Acceptance: transaction notification/livraisons, contraintes d'idempotence, leases, tentative, prochaine échéance, expiration, résultat fournisseur et `accepted_at` sont persistés.
  - Verify: tests de crash/reprise, workers concurrents, duplicate enqueue, expiration et lecture historique.
  - Files: schéma/queries SQLDelight et repositories/services associés.

- [ ] 3.3 Corriger la sémantique d'envoi shared.
  - Acceptance: aucune erreur transport n'est absorbée comme succès; `sent_at` reste null avant acceptation; chaque appareil conserve son résultat.
  - Verify: remplacer le test de faux succès par des assertions de queue/échec/acceptation conformes au modèle et exécuter la suite notification ciblée.
  - Files: `shared/.../notification/NotificationService.kt` et tests.

## 4. iOS registration implementation

- [ ] 4.1 Brancher `APNsService` et AppDelegate sur le contrat `iosNotificationRegistration`.
  - Acceptance: lecture de statut sans prompt, action explicite, callbacks corrélés, token refresh, auth différée et erreurs observables correspondent aux fixtures du modèle.
  - Verify: tests iOS de machine/adapters et test de régression « no prompt at launch ».
  - Files: `iosApp/src/Services/APNsService.swift`, `AppDelegate.swift`, adapters/fixtures et tests.

- [ ] 4.2 Ajouter l'action accessible pour le statut `notDetermined` et les états de récupération.
  - Acceptance: la vue affiche Activer pour `notDetermined`, Ouvrir Réglages pour `denied`, et l'état registered/retry/misconfigured sans inventer sa propre logique.
  - Verify: tests ViewModel/UI, localisation, Dynamic Type et VoiceOver ciblés.
  - Files: `NotificationPreferencesView.swift`, ViewModel et localisations nécessaires.

- [ ] 4.3 Séquencer le logout avec la désinscription authentifiée par installation.
  - Acceptance: le JWT reste disponible jusqu'à succès idempotent de désinscription; échec/retry/cancel sont explicites; les autres appareils restent inscrits.
  - Verify: test d'ordre des effets, offline, `5xx`, déjà absent et deux appareils.
  - Files: `AuthStateManager.swift`, `APNsService.swift`, auth/notification adapters et tests.

## 5. Backend APNs provider

- [ ] 5.1 Implémenter l'authentification token-based APNs et la validation fail-closed.
  - Acceptance: JWT ES256 avec clé `.p8`, Key ID/Team ID/topic/environnement obligatoires, cache/rotation sûrs, secrets absents des logs et readiness non prête si production est mal configurée.
  - Verify: tests JWT/config/redaction/rotation et scan de secrets.
  - Files: `server/.../notification/PushNotificationSender.kt`, configuration/DI/readiness et tests.

- [ ] 5.2 Implémenter le transport HTTP/2 APNs et les headers obligatoires.
  - Acceptance: sandbox et production utilisent les endpoints Apple courants; `apns-id`, topic, push type, expiration, priorité et payload validé sont envoyés.
  - Verify: faux serveur HTTP/2/contract tests pour endpoint, headers, payload et connexions.
  - Files: provider APNs backend et tests.

- [ ] 5.3 Implémenter la classification déterministe des réponses et le circuit provider.
  - Acceptance: HTTP `200`, token invalide, payload rejeté, auth bloquée, `429`, `5xx`, erreur avant écriture et outcome inconnu produisent exactement les états du modèle.
  - Verify: tests table-driven couvrant chaque code/reason du design et raison inconnue.
  - Files: fonctions pures de classification, machine adapter, worker et tests.

## 6. Registration API and delivery worker

- [ ] 6.1 Étendre les endpoints d'inscription/désinscription avec compatibilité legacy.
  - Acceptance: user issu du JWT, `installationId`, environnement/topic, idempotence, désinscription d'un seul appareil et mapping temporaire des anciens clients.
  - Verify: tests route/auth/validation/multi-device/legacy et accès croisé interdit.
  - Files: `NotificationRoutes.kt`, DTOs/services et tests.

- [ ] 6.2 Implémenter le worker durable conforme à `notificationDelivery`.
  - Acceptance: leases, policy, quiet hours, token absent, retries full-jitter, `Retry-After`, expiration, circuit auth et reprise après crash sont persistés.
  - Verify: tests horloge déterministe, concurrence, crash/restart, retry exhaust et annulation.
  - Files: worker/scheduler backend, repositories et tests.

- [ ] 6.3 Ajouter métriques, alertes et logs assainis.
  - Acceptance: profondeur/âge outbox, états, latence d'acceptation, invalidation et auth blocked sont observables sans token/JWT/payload privé.
  - Verify: tests de redaction, métriques et alerte synthétique provider bloqué.
  - Files: telemetry backend, runbook et tests.

## 7. Migration, rollout and rollback

- [ ] 7.1 Valider dual-read/dual-write et compatibilité de version.
  - Acceptance: ancien et nouveau clients peuvent coexister pendant la fenêtre; aucune inscription supplémentaire n'est écrasée; les lignes queued survivent au rollback.
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

- [ ] 10.1 `@tests` Prove `confirmation_effect_outbox` is local and owned by `DatabaseEventRepository`, while `notification_recipient` and `notification_delivery` are backend-owned.
- [ ] 10.2 `@codegen` Add stable uniqueness for `domainEventId`, `effectKey`, `recipientKey`, `deliveryKey`, and `calendarArtifactKey` without a cross-database transaction claim.
- [ ] 10.3 `@tests` Prove backend acknowledgement advances `decisionSyncStatus` only and cannot imply `effectDispatchStatus` or provider acceptance.
- [ ] 10.4 `@tests` Prove a missing target remains pending and fans out after installation registration, subject to its own expiry and retry policy.
- [ ] 10.5 `@codegen` Execute shadow-write migration with unique `delivery_authority`, reconciliation, cohort cutover, and rollback checkpoints.
- [ ] 10.6 `@review` Verify decision sync, recipient resolution, APNs delivery, and calendar fan-out have independent retries and cannot mutate one another's state authority.
