# Audit Android (Compose) — Uniformisation UX (DAO #27/#24/#25/#26)

Date : 2026-09-07
Revue : @review — audit équivalent Android des captures Compose
Environnement : émulateur `Wakeve_Audit_API_32` (Android 12L, 1080×2340),
build debug du 2026-09-07 18:22, mode invité (aucune donnée seedée).
Captures : `qa-screenshots/cycle-2026-09-07-android/`.

## Verdict global

**Écart bloquant identifié (F1) + périmètre partiel documenté.** Les tokens
ivoire sont correctement définis côté Compose (tests unitaires verts), mais
ils sont **écrasés à l'exécution** par Material You dynamic color sur
Android 12+. Aucun écran Android n'affiche donc l'ivoire `#F6F1EA` aujourd'hui.

## F1 — Finding majeur : dynamic color écrase les tokens #27

**Constat mesuré** : les 5 écrans capturés (get-started, auth, home invité,
création étapes 1-2) affichent tous un fond `#F6FAFF` (246,250,255 — bleu
froid Material You de l'émulateur), jamais l'ivoire `#F6F1EA` (246,241,234).

**Cause** : `WakeveTheme(dynamicColor = true)` (défaut) — sur `SDK_INT >= S`,
`dynamicLightColorScheme(context)` remplace intégralement le schéma fixe :
`background`, `surface` et tous les rôles viennent du wallpaper système, pas
des tokens `WarmIvory`/`WakeveBackgroundLight`.

**Preuve interne** : les tests instrumentés du projet désactivent déjà le
dynamic color eux-mêmes (`WakeveTheme(dynamicColor = false)` dans
`EventWorkspaceScreenTest`, `EventWorkspaceAdaptiveScreenshotTest`) pour la
déterminisme visuel — l'app de production, elle, tourne avec `true`.

### Options d'arbitrage (décision design à prendre, non tranchée unilatéralement)

> **✅ Arbitrage rendu (2026-09-07, design owner) : O3 — Statu quo.**
> Material You dynamic color reste intégral sur Android. Conséquences assumées :
> - L'AC #27 (« captures QA montrent un fond uniforme ») est **rescopée iOS uniquement** —
>   Android suit la palette dynamique du système (promesse « Material You » du design system) ;
> - Les tokens `WarmIvory`/`WarmIvoryDark` restent définis et testés côté Compose :
>   ils servent de base au schéma fixe (API < S, tests, previews) et de fallback documenté ;
> - `dynamicColor = false` reste l'état des tests instrumentés et previews (déterminisme visuel) ;
> - Les comparaisons QA inter-plateformes de fond ne sont plus apples : les futurs cycles QA
>   Android auditeront la cohérence interne Material You (rôles M3), pas la teinte exacte.

| Option | Description | Pour | Contre |
|---|---|---|---|
| **O1 — Marque fixe** | `dynamicColor = false` par défaut | Fidèle à l'AC #27 (« captures QA montrent un fond uniforme »), rendu identique iOS/Android, captures QA déterministes | Perte du wallpaper theming Material You (pilier annoncé du design system Android) |
| **O2 — Hybride** | Garder dynamic pour accents/containers, forcer `background`/`surface` = `WarmIvory` dans le schéma dynamique | Compromis : fond de marque, teintes d'accent adaptatives | Schéma hybride non standard M3 ; risque de déséquilibres de contrastes selon le wallpaper ; à re-valider a11y par wallpaper |
| **O3 — Statu quo** | Conserver Material You intégral | Cohérent avec la promesse « Material You » de l'AGENTS.md | L'AC #27 (« fond uniforme ») est inatteignable sur Android ; rupture visuelle avec iOS |

Recommandation : **O1** pour le cycle actuel (uniformité inter-plateformes,
captures QA comparables, Material You conservable plus tard via un réglage
utilisateur « couleurs dynamiques » optionnel — pattern Android courant).

## F2 — Inventaire : implémenté vs non implémenté (contexte donné)

Routes Compose présentes : splash, onboarding, get_started, auth (+email),
home/events, event/{id} (workspace) + sous-routes (budget, meals, meetings,
participants, comments, activities, equipment, accommodation, invite),
event/{id}/scenarios (+compare), event_creation (wizard 4 étapes +
assistant de planification), explore, inbox, profile, settings, leaderboard,
organizer_dashboard, notifications.

Écrans du cycle iOS **sans équivalent Android capturable** :
- Vote de sondage et résultats de sondage (flux POLLING)
- Date confirmée / information confirmée / archive « Jour J » (surfaces
  invitation iOS)
- Studio d'invitation iOS (`EventCreationStudioView`, specifique iOS)

Conséquence : les AC #24/#25/#26 (en-têtes, carte héro, boutons) ne sont
pas vérifiables en capture Android sur ces flux ; seuls les tokens (#27) et
les écrans génériques sont auditables. Les composants Compose
(`WakeveScreenHeader`, `WakeveHeroCard`, `WakevePrimaryPillButton`,
`WakeveSecondaryIconButton`, `WakeveMetaChip`) sont livrés et compilés ;
leur consommation s'étendra avec l'implémentation des écrans manquants.

## F3 — Consommation des composants (partielle, attendue)

- `ScenarioManagementScreen` : migré vers `WakeveScreenHeader` ✅ (#24)
- `EventWorkspaceScreen` : utilise encore `WakeveScaffold` (TopAppBar M3) —
  migration #24 restante une fois le flux événement testable en invité
- Cartes : `WakeveCard` blanche + hairline active globalement (#25) ✅

## Contraintes de l'audit

- Auth obligatoire sans mode dev Android (contrairement à
  `--wakeve-debug-authenticated` iOS) : parcours audités en mode invité,
  états vides, pas de seed repository équivalent Android.
- Le wizard de création est fonctionnel (étape 1 remplie, « Suivant »
  actif) ; le flux complet jusqu'aux scénarios exige un créneau horaire
  (sélecteur natif) et un changement de statut — hors périmètre de cette
  passe d'audit.

## Actions proposées

| # | Sévérité | Action | Prérequis |
|---|---|---|---|
| F1 | ✅ Arbitré | **O3 retenu** — dynamic color conservé ; AC #27 rescopée iOS ; tokens WarmIvory maintenus comme base schéma fixe/fallback | — |
| F2 | Information | Implémentation des écrans poll/résultats Android (DAO séparé) | Backlog produit |
| F3 | Mineure | Migrer `EventWorkspaceScreen` vers `WakeveScreenHeader` | Flux événement testable |
| — | Note | Ajouter un `--wakeve-debug-authenticated` équivalent Android pour des QA headless | Backlog QA tooling |
