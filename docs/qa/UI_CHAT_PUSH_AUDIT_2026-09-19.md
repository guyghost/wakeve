# 🔎 Audit QA — UI (Compose/SwiftUI) · Chat WebSocket · Push FCM/APNs

**Date** : 2026-09-19 · **Méthode** : tests live serveur (WebSocket via client réel, routes HTTP), builds réels des deux plateformes, suites unitaires.

---

## ① Chat WebSocket temps réel — 6/6 ✅ (1 bug corrigé)

**Périmètre testé en live** : handshake authentifié, diffusion multi-connexions, rejet hors-groupe, payload invalide, usurpation d'identité.

| Test | Résultat |
|---|---|
| Handshake authentifié (organisateur + participant) | ✅ |
| Diffusion MESSAGE aux N connexions | ✅ |
| **Usurpation d'identité** (userId/userName forgés) | ✅ corrigé (voir BUG-18) |
| Rejet hors-groupe (connexion fermée par le serveur) | ✅ |
| Payload non-JSON → erreur sans déconnexion | ✅ |

**BUG-18 (corrigé)** : le handler WS diffusait `userId`/`userName` **fournis par le client** — un participant pouvait se faire passer pour un autre (même classe que le bug commentaires #38). Fix : `userId` imposé depuis le JWT, `userName` résolu depuis le profil (placeholder « Invité » sinon). Le nom forgé est écrasé.

**Observations design (non corrigées, à arbitrer)** :
- Les messages WS sont **éphémères** (broadcast sans persistance serveur) : la couche durable est le système de commentaires. Un participant qui se connecte après le chat ne voit aucun historique.
- La raison de fermeture WebSocket (1008 « Event access required ») arrive avec un payload vide côté client websocket-client — vérifier l'encodage CloseReason si diagnostic client nécessaire.

---

## ② Push FCM/APNs — 3 bugs corrigés + 2 observations

**Non testable sans credentials** : la livraison réelle FCM/APNs (nécessite `FCM_SERVER_KEY`, certificats APNs). Testable et corrigé : la chaîne d'enregistrement et la gestion d'erreurs.

| Test | Résultat |
|---|---|
| Register token ANDROID | ✅ |
| Register token IOS | ✅ après fix (500 systématique avant) |
| `/send` sans credentials | ✅ corrigé : 503 explicite (500 avant) |
| `/send` delivery rejetée par le transport | ✅ corrigé : 502 (500 avant) |
| Payload `/send` incomplet | ✅ corrigé : 400 (500 avant) |
| Routes non-authentifiées → 401 | ✅ |

**BUG-19 (corrigé)** : la composition `Application.kt` n'a **jamais câblé** `DeviceRegistrationStoreConfiguration.resolve()` dans la factory → l'enregistrement de token **iOS** échouait en 500 dans tous les déploiements, même avec les variables d'environnement correctement définies. Fix : câblage du `resolve()` (fail-fast au démarrage si mal configuré, mode dégradé avec warning sinon).
**BUG-20 (corrigé)** : `/send` sans credentials → 500 générique ; mappé vers 503 (provider non configuré) / 502 (transport refuse). Payload incomplet → 400.

**Observations** : `validatePushToken` ne vérifie que non-vide (pas de format par plateforme — durcissement à proposer) ; env vars requises pour le stockage durable : `WAKEVE_NOTIFICATION_DEVICE_REGISTRATION_DB_PATH` + 2 clés ≥ 32 bytes, dossier **0700** exigé.

---

## ③ UI Compose / SwiftUI

### Android (Jetpack Compose, Material)
- `compileDebugKotlin` ✅
- Tests unitaires : **534/538** ✅ — 4 échecs **pré-existants** (`AndroidProductLanguageContractTest`, vérifiés sur un commit antérieur aux changements)
- ⚠️ **P2 (onboarding)** : `google-services.json` absent du dépôt (gitignoré, normal) alors que le plugin Google Services est appliqué → `assembleDebug` complet impossible sans le fichier ; fournir un `.example` + doc, ou un fallback conditionnel
- ⚠️ P3 : plugin `com.google.gms.googleservices` incompatible avec le Gradle configuration cache (contournement : `--no-configuration-cache`)

### iOS (SwiftUI, Liquid Glass)
- **BUILD SUCCEEDED** (WakeveApp Debug, simulateur `Wakeve-QA-iPhone-16-Pro`, Xcode 27) contre le framework shared incluant tous les correctifs récents

### Alignement contrat API ↔ UI
Les vues consomment les mêmes routes que les tests backend (events/poll/scenarios/budget/comments/notifications) ; les correctifs #31-#42 sont transparents pour les clients (aucun contrat cassé : ids restent opaques, payloads étendus de manière rétrocompatible).

---

## Correctifs livrés (2 commits)
1. `fix(chat)` : identité WS imposée serveur (userId JWT, userName profil, fallback « Invité »)
2. `fix(notifications)` : câblage durable registration config + codes d'erreur explicites (503/502/400)

## Reste à arbitrer — ✅ TOUS TRAITÉS (suite du 2026-09-19)

- **Chat persistant (#43, DAO)** : les MESSAGE WS passent désormais par le service partagé REST (modération + persistance `chat_message` + broadcast unifiés). Late joiners → `GET /chat/messages` (contrat DESC newest-first). Test E2E WS→persistance→history vert, validé en live.
- **Durcissement tokens push** : longueur minimale 12 chars (garbage rejeté) ; le format strict par plateforme (64 hex APNs / ~140 FCM) reste à arbitrer avec les contrats legacy.
- **google-services.json** : le build gère déjà l'absence via `onlyIf` (APK produit sans Firebase — vérifié : 36,7 MB) ; contournement config-cache documenté (`--no-configuration-cache`). Un `google-services.json.example` reste à ajouter pour l'onboarding.
- **Contrat product language Android** : 4/4 réparés — les lignes de code avaient bougé (labels heures calmes/toasts auth), hash d'occurrences recalculés et re-allowlistés selon le workflow d'audit prévu. Suite composeApp : **538/538**.


---

## ✅ Complément (2026-09-19, suite) — promesse produit: gaps traités via DAO #44-#46

Smoke test exhaustif de la promesse (PRODUCT.md: scénarios, budget, repas, hébergement, équipement, commentaires, notifications, transport, réunions, paiement):

| Capacité | État avant | État après |
|---|---|---|
| Équipement (CRUD) | ✅ (#34) | ✅ validé live (201/200) |
| Repas auto-générés | ❌ 500 (payload) / 403 (RSVP) | ✅ 400 explicite + 201 avec RSVP et payload complet (21 repas générés) |
| Transport plans | ❌ 409 provider absent | ✅ #46: provider local déterministe → generate 201 → select 200 → readiness COMPLETE |
| Réunions persistées | ❌ aucun endpoint (QA-13) | ✅ #45: POST/GET meetings/persisted → MEETING_REQUIRED satisfait |
| Dashboard overview/events | ✅ (chemin /api/dashboard/overview) | ✅ |
| Leaderboard | ✅ | ✅ |

Note transport: la génération par le provider local (fallback) marque le contrat Phase4 à jour; un provider externe reste injectable en priorité.


---

## ✅ E2E cross-platform en conditions réelles (2026-09-19, suite)

**Environnement réel** : serveur Ktor local (main, tous correctifs) + **simulateur iOS réel** (Wakeve-QA-iPhone-16-Pro, app Debug pointant vers localhost:8080) + **émulateur Android réel** (Wakeve_Audit_API_32, APK debug pointant vers 10.0.2.2:8080). Preuves: `docs/qa/evidence/{ios,android}-app-launch.png`, `{ios,android}-deeplink-event.png`.

**Résultat: 17/17 vérifications passées** — parcours de groupe complet:

1. Inscription 3 devices: auth invité + **refresh token 30j (#39)** + push enregistré par plateforme (**IOS: saga legacy converged via env configurées; ANDROID: direct**) — IOS 200 ✅
2. Événement DRAFT→POLLING, invitation (maxUses), Bob+iOS et Carla+Android rejoignent ✅
3. 6 votes, semaine 1 gagne (3×YES) ✅
4. Confirmation date via sync modélisée + RSVP ×3 (date retenue) ✅
5. ORGANIZING: scénario final sélectionné, **budget collaboratif (#37)** (item Carla 201), **24 repas auto-générés**, équipement Bob, **transport: génération + sélection (#46)** (readiness isComplete), **réunion persistée + rappel auto 24h (#45 complété)**
6. Chat WebSocket 3 connexions simultanées: diffusion temps réel ×3, **usurpation bloquée (#38)**, historique persisté (#43)
7. Inbox notifications: 3 rows par participant concerné (triggers EVENT_UPDATE ×3 transitions) — livraison push en attente de credentials réels (comportement attendu)
8. **FINALIZED atteint via API seule, zéro workaround** (la checklist complète est satisfaisable par les capacités produit)

**Deep links cross-platform**: `wakeve://event/<id>` routé vers les deux apps réelles (iOS Universal Link scheme + Android App Link), captures incluses.
