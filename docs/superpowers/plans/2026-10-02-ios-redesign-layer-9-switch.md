# Refonte iOS — Couche 9 (bascule et suppression du code legacy) — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal :** La refonte devient l'application : suppression du flag `iosRedesign2026` (toujours actif), suppression de tout le code legacy devenu inatteignable (shell à onglets, ancien accueil, bibliothèque, ancien détail + canvas, Explorer, Inbox, ancienne feuille de création), purge des clés et composants morts, baselines du garde-fou abaissées aux valeurs mesurées.

**Branche :** `claude/ios-redesign-layer-9` (empilée sur la couche 8).

**Décisions (utilisateur, 2026-10-02) :**
- **Tout d'un coup** : pas d'interrupteur conservé ; retour arrière = revert de la PR (spec §13).
- **Suppression de `CreateEventSheet`** : le deep link `.eventCreate` passe par `beginRedesignEventCreation()` (flux 4 questions si flag invitations éteint, studio sinon). Pertes assumées : création en mode scénarios hors studio et brouillon IA à la création (à reporter dans la spec §15 ; à reprendre plus tard dans le flux si besoin).
- Le flag `iosInvitationExperienceV1` reste inchangé.
- Baseline 0 du garde-fou **non atteignable** dans cette couche (les écrans plein écran de repli conservés utilisent l'ancien design system) : baselines abaissées aux valeurs mesurées ; le passage à 0 devient un chantier de restylage séparé.

**Carte « mort après bascule »** : voir le rapport d'exploration résumé ci-dessous (chemins relatifs à `iosApp/`, CV = `src/Views/App/ContentView.swift`).
- Flag : `FeatureFlags` (+ tests), `@AppStorage(FeatureFlags.redesign2026Key)` (CV:188), les deux `onChange(of: iosRedesign2026)`, `AppRouter.preRoute(redesignEnabled:)`, toutes les branches OFF (CV:500, 645, 764, 910, 1071, 1847, 2029).
- Shell : `legacyTabChrome`, `tabBarVisibility`, `tabContent(for:)`, `WakeveTab`, `selectedTab` (+ ses assignations), `unreadInboxCount`, cas `AppView` `.inbox`, `.notifications`, `.notificationPreferences`, `.settings` et leurs corps, `topLevel(.profile) → selectedTab`.
- Accueil : `EventListView`, `EventCard`, `EventListViewModel` (déjà mort), `EventLibraryView`/`EventLibraryViewModel`/`EventLibraryFilter`, `eventLibraryContent`, `canvasAction(for:)`, état QA associé — **déplacer** `InvitationArtworkView` et `invitationAccessibilityIdentifier` (+ pont) dans leurs propres fichiers.
- Explorer : `ExploreTabView`, `ExploreScenarioDetailView`, VM + `ExploreEventItem`, `ExploreFactory` — **déplacer** `EventCategoryItem`, `EventScenario`, `allScenarios` vers `Models/Create/`.
- Détail : `EventDetailView` + helpers privés, `EventDetailInvitationCanvas.swift`, argument QA `--wakeve-qa-invitation-canvas` (iOSApp.swift), `EventNextAction`, environnement `wakeveAccessibilityReduceTransparencyOverride` + override QA, vue `EventWeatherMapCard` + `EventWeatherViewModel` (garder `EventWeatherSummary`/`EventWeatherPlace`), `preparedCreationChecklists` (écrit seul) — garder `EventDetailViewModel` (suppression depuis Infos). Déjà morts : `EventPreviewDetailRow`, `OrganizationUXLabels`.
- Création : `CreateEventSheet.swift` (garder `EventCreationContext`), `fullScreenCover` legacy, partie VM/IA de `CreateEventViewModel` (garder `EventTimeSlotInput`, `EventSlotInputBuilder`, `EventTimeSlotFactory` — déplacer), `EventInfoSheet`, `BackgroundPickerSheet`, `invitationExperienceLegacyCreationFallback` si devenu inatteignable, `CreateFlowEntry.legacySheet`.
- Inbox : `InboxView`, `InboxDetailView`, `InboxViewModel`, `InboxItemFactory`.
- `ProfileViewModel` (déjà mort). `ProfileTabView` **reste**.
- Composants/thème morts (passe C) : `AIBadgeView`, `LiquidGlassTextField`, `WakeveEventPanel`, `ParticipantAvatarStack` (vérifier), `EventListRow`, `BottomSheet`, `WakeveStackedAvatars`, `WakeveListRow`, `WakeveSectionHeader`, `WakeveWidgetPreviewGallery`, `IconographyGuidelines`, aides `LiquidGlassAnimations` inutilisées, statics `Color.wakeve*`/`app*`/`iOS*` et membres `WakeveTheme` inutilisés — **chaque suppression vérifiée par recherche de références** ; le design system legacy utilisé par les écrans conservés reste.
- Risque corrigé : `wakeve://leaderboard` piège l'utilisateur (pas de retour) → ajouter `.leaderboard → .eventList` à `RedesignBackRoute` (ou présentation en sheet).

## Passes (chacune compile, tests verts, commits séparés ; corps `Refs Swarm DAO #47 (layer 9).`)

**Passe A — bascule et shell/accueil/Explorer/Inbox**
1. `feat(ios)!: make the redesign the only app shell` : supprimer le flag et toutes les branches OFF ; `preRoute` sans paramètre ; deep link `.eventCreate` → `beginRedesignEventCreation()` ; retour Leaderboard ; adapter les tests de câblage (RedesignShell, AppRouter, Immersive/HubModuleSheetView/CreateEventFlow/Activity wiring) — corps avec `BREAKING CHANGE: iOS legacy tab shell removed; iosRedesign2026 flag removed.`
2. `refactor(ios): remove the legacy tab shell, home, library, explore and inbox` : suppressions + déplacements (`InvitationArtworkView`, identifiants, `EventScenario`) ; tests qui ne protègent que du code supprimé → supprimés ; tests qui protègent un comportement conservé → réancrés.

**Passe B — détail legacy et ancienne création**
3. `refactor(ios): remove the legacy event detail and invitation canvas` (+ météo carte, EventNextAction, overrides QA) ; réancrer `OrganizationPhase5/7`, `InvitationExperienceSurfaceContractTests`, etc.
4. `refactor(ios)!: remove the legacy create sheet` (+ VM/IA, sheets annexes ; déplacer les aides de créneaux) ; supprimer `PremiumCreateEventContractTests` et les tests ne visant que la feuille ; garder ce qui protège `EventSlotInputBuilder` etc.

**Passe C — nettoyage**
5. `refactor(ios): remove dead legacy components and tokens`.
6. `chore(ios): remove unused localization keys` (≈ 460 + 32 `home.*` par langue ; vérifier les clés construites dynamiquement avant suppression ; 5 langues ; `plutil -lint`).
7. `test(ios): lower the style guard baselines to the measured values` (valeurs mesurées après suppression ; commentaire : 0 reporté au restylage des écrans de repli).

**Fin** : simulateur (lancement sans argument de flag → nouvelle app ; ＋, deep links `wakeve://event/create`, `wakeve://leaderboard` + retour, `wakeve://notifications`, `wakeve://settings`, `wakeve://profile`, invitation DEBUG, jour J ; flag invitations allumé et éteint ; captures `/tmp/wk-l9-*.png` regardées), désinstaller, suite complète (seuls les 5 échecs préexistants, sauf si leurs tests ont été supprimés ou réancrés — le dire), spec §9/§13/§15 « Couche 9 » (pertes assumées, baseline non nulle, retour arrière par revert). Commit `docs(ios): record layer 9 switch in redesign spec`. Puis revue de code.

## Contraintes

- Worktree `/Users/guy/Developer/dev/wakeve/.claude/worktrees/ios-app-design-e1ae98`, branche `claude/ios-redesign-layer-9`. Jamais `git stash`. Jamais indexer `.dao/*`. **Aucune ligne d'attribution.** Ne pas pousser.
- Un seul DerivedData `/tmp/wk-l9-dd` (supprimé à la fin) ; `df -h /System/Volumes/Data` avant chaque build ; arrêt et rapport sous 3 Go. `-parallel-testing-enabled NO` ; tuer son propre `xcodebuild` bloqué.
- Simulateur « iPhone 18 Pro » seulement (`device` explicite), jamais « Wakeve-QA-iPhone-16-Pro ».
- Ne rien supprimer qui reste référencé par du code atteignable (vérifier par recherche à chaque suppression) ; ne pas toucher au code Kotlin/Android/web ; ne pas modifier `project.pbxproj` (dossiers synchronisés : supprimer/déplacer des fichiers suffit).
- Les tests supprimés doivent être listés dans le message de commit (nom de classe et raison).
