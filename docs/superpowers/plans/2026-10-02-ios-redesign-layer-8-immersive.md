# Refonte iOS — Couche 8 (mode immersif) — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal :** Sous `iosRedesign2026`, ajouter l'ambiance immersive (fond sombre teinté par l'événement, texte clair, pastilles à contour fin, CTA blanc) pour deux moments : **l'invitation reçue** (deep link `invite(token:)`) et **le jour J**.

**Branche :** `claude/ios-redesign-layer-8` (empilée sur la couche 7).

**Périmètre décidé (2026-10-02) :**
- Invitation reçue : présentation immersive de l'événement + CTA « Voir l'événement » (ou « Voter » si le vote est requis, mêmes règles que le hub). **Pas d'Accepter/Décliner** : aucune API client (les routes `/api/invite/{code}/accept` et `/rsvp` n'existent que côté serveur, sans appelant) — à traiter avec la synchro (proposition #48 ou suite). L'état de réponse est seulement affiché.
- Jour J : heure du rendez-vous, lieu, carte si coordonnées disponibles, pastilles logistiques (transport, repas du jour), nombre de participants (statique), CTA « Itinéraire » (Plans) + « Voir l'événement ». **Différé** : présence en direct, « Je suis en route », covoiturage détaillé, notifications du jour J, conflit archive/finalisé quand le flag invitations est allumé.
- Les chemins legacy (`EventDetailView`, carte d'arrivée d'invitation, `resolveInvitationDeepLink`) restent intacts.

**Spec :** §3, §5.6 — **Proposition :** Swarm DAO #47.

## Architecture

1. **Jetons** (`Theme/WK.swift`) : corriger `WK.Mood` (fond aujourd'hui quasi noir) — fond dérivé de `darkSecondaryHex` ou facteur d'assombrissement réduit (≈ 0,2–0,35), en gardant `testImmersiveMoodTextIsReadableOnItsBackground` (texte principal ≥ 7:1, secondaire ≥ 4,5:1 pour **chaque** ambiance ; ajuster `textSecondary` si nécessaire, jamais baisser les seuils) ; ajouter un test qui garantit que deux ambiances différentes ont des fonds distincts (≥ un écart minimal de luminance/teinte).
2. **Composants** (`Components/WK/`) : `WKImmersiveScaffold(mood:content:primary:secondary:onClose:)` (fond `mood.background` plein écran, contenu défilant, CTA blanc `WKImmersiveButton` en bas, bouton fermer), `WKImmersivePill` (contour `pillStroke`, texte `mood.textPrimary`), variante de carte `mood.surface`. Galerie : previews par ambiance, AX5, Reduce Transparency, Increase Contrast. Zéro style en dur.
3. **Invitation** : `Views/Immersive/InvitationLandingView.swift` alimentée par les faits du hub (`EventHubFacts` via `SharedEventHubSource`, + organisateur et illustration si disponible via `invitationExperienceProjectionRepository.artwork(eventId:)` / `InvitationArtworkView`) : « Tu es invité·e » (formulation sans genre à préférer : « Invitation de <organisateur> »), titre, date retenue ou « Vote en cours · N créneaux », participants confirmés/en attente, état de réponse ; CTA principal selon `EventHubModel.primary` (vote → écran de vote ; sinon « Voir l'événement » → hub).
   - Branchement : dans `AuthenticatedView`, sous flag, quand `invitationLandingEventId == event.id` → afficher `InvitationLandingView` à la place du hub ; « Voir l'événement » efface `invitationLandingEventId` (chemin distinct du `onBack` du hub, dont le texte exact est ancré par un test).
4. **Jour J** :
   - Pur `Models/Immersive/EventDayRule.swift` : `isEventDay(phase:finalDate:slotStart:slotEnd:timezone:hasAccess:now:calendar:)` = phase organisation (ou finalisé **sans** flag invitations) ∧ accès accordé ∧ (aujourd'hui = jour de la date retenue dans le fuseau du créneau ∨ maintenant ∈ [début, fin]) ; tests (fuseaux, minuit, créneau sur deux jours, journée entière).
   - Source `Services/SharedEventDaySource.swift` (hors thread principal, schéma existant) : créneau retenu (`confirmedDate` / `selectWithTimeslotDetails`), lieu (scénario retenu `location`, sinon premier lieu potentiel ; coordonnées depuis le JSON des lieux potentiels — extraire une fonction pure de parsing, **sans** modifier `EventWeatherViewModel`), plan de transport (`HubModuleSheetData.transportState/transportPill`), repas du jour (`getMealsByDate`), comptes participants (règle du hub).
   - Vue `Views/Immersive/EventDayView.swift` : heure en très grand (`WK.Typo.display`), date, lieu, carte MapKit compacte si coordonnées (sinon texte seul), pastilles logistiques, participants, CTA « Itinéraire » (`MKMapItem.openInMaps` avec coordonnées ou recherche par nom) et « Voir l'événement ».
   - Entrées : bannière « C'est aujourd'hui » dans le hero du hub quand `isEventDay` (ouvre `EventDayView` en plein écran) ; `HomeNextStep` gagne un cas `.eventDay` (organisateur **et** participants acceptés avec accès), priorité juste après les actions de vote/confirmation, action « Voir le jour J ».
5. **Clés** `immersive.*` (5 langues, fr au tutoiement), réutiliser `event.detail.invite_landing.*` si adaptées, `home.v2.today`, `hub.module.*`.

## Contraintes

- Worktree `/Users/guy/Developer/dev/wakeve/.claude/worktrees/ios-app-design-e1ae98`, branche `claude/ios-redesign-layer-8`. Jamais `git stash`. Jamais indexer `.dao/*`. **Aucune ligne d'attribution.** Ne pas pousser. `plutil -lint` sur les chaînes.
- Un seul DerivedData `/tmp/wk-l8-dd` (supprimé à la fin) ; `df -h /System/Volumes/Data` avant chaque build ; arrêt et rapport sous 3 Go. `-parallel-testing-enabled NO` ; tuer son propre `xcodebuild` bloqué.
- Simulateur « iPhone 18 Pro » seulement (`device` explicite), jamais « Wakeve-QA-iPhone-16-Pro » ; désinstaller l'app avant la suite complète.
- Ne pas modifier `EventDetailView`, `EventDetailInvitationCanvas.swift`, `EventWeatherMapCard.swift`, `resolveInvitationDeepLink`/`handleDeepLinkNavigation` (ancres `PremiumNavigationContractTests`), le texte exact du `onBack` du hub (`HubModuleSheetViewTests:441`), ni les tests d'ancrage d'autres fichiers (`PremiumEventDetailContractTests`, `EventDetailInvitationCanvasContractTests`, `PremiumDesignSystemContractTests`, `AppRouterTests`, `EventHubViewTests`).

## Tâches (TDD, un commit chacune, corps `Refs Swarm DAO #47 (layer 8).`)

1. `fix(ios): give immersive moods a visible tinted background` — jetons + tests.
2. `feat(ios): add the WK immersive scaffold, pill and button` — composants + galerie + tests (AX5, cibles ≥ 44 pt, contrastes du CTA blanc et des pastilles).
3. `feat(ios): add the immersive invitation landing` — vue, branchement, clés, tests.
4. `feat(ios): add the event day rule and data source` — règle pure + source + tests.
5. `feat(ios): add the immersive event day view and its entry points` — vue, bannière hub, `HomeNextStep.eventDay`, tests.
6. Simulateur : invitation via `xcrun simctl openurl "iPhone 18 Pro" wakeve://invite/<token>` (si le serveur local n'est pas joignable, la résolution échoue : vérifier alors l'affichage en positionnant `invitationLandingEventId` par un argument de lancement DEBUG dédié, documenté, ou un test de rendu — le dire honnêtement) ; jour J en fixant la date retenue d'un événement en organisation à aujourd'hui dans la base de l'app (sqlite) ; vérifier hero, carte, pastilles, CTA Itinéraire (ouvre Plans), « Voir l'événement » → hub ; plusieurs ambiances ; AX5 ; Reduce Transparency ; flag éteint → inchangé. Captures `/tmp/wk-l8-*.png` regardées. Désinstaller, suite complète (5 échecs préexistants seulement). Spec §15 « Couche 8 » + ligne §9 ; commit `docs(ios): record layer 8 immersive mode in redesign spec`.
7. Revue de code puis corrections.
