# Refonte iOS Wakeve — Design

- **Date** : 2026-09-28
- **Statut** : validé en brainstorming, en attente de relecture
- **Périmètre** : application iOS (SwiftUI) uniquement — Android et Web hors périmètre de cette itération
- **Type Swarm DAO** : `product-feature` (proposition à créer avant implémentation)

## 1. Contexte et problème

L'application iOS fonctionne mais son rendu et son parcours manquent de finition :

- **Navigation monolithique** : `iosApp/src/Views/App/ContentView.swift` (~5 600 lignes) route 36 écrans via un `switch` sur `enum AppView`. Pas de `NavigationStack` : pas de retour natif, pas de swipe-back, pas de restauration d'état.
- **Trop d'écrans** : ~15 écrans d'organisation distincts par événement (transport, repas, matériel, activités, budget, paiement, photos…), chacun poussé en plein écran.
- **Deux accueils et deux flux de création** coexistent derrière `@AppStorage("iosInvitationExperienceV1")` (désactivé par défaut), plus du code mort (`Views/Events/HomeView.swift`, `Views/EventDetailExperienceView.swift`, `LiquidGlassTabBar`).
- **Prolifération de tokens** : trois échelles d'espacements/rayons parallèles dans `Theme/DesignSystem.swift`, cinq fichiers de couleurs (`WakeveColors`, `BrandColor`, `SemanticColor`, `EventMoodPalette`, `WakeveTheme`) avec quatre bleus concurrents (#2563EB, #0969DA, #2F6F9F, #3F8FF2).
- **Styles en dur dans `Views/`** : ~77 `Color(hex:)`, ~101 `.font(.system(size:))`, ~78 `cornerRadius` littéraux (mesuré après la couche 0 : 65 / 87 / 68).
- **Incohérences de nommage** : onglet `.groups` libellé « Explorer ».
- Les listes d'événements n'affichent ni statut ni « qui doit agir ».

## 2. Objectifs

1. Un langage visuel unique, doux et léché, inspiré des références fournies (cartes blanches très arrondies, avatars empilés, grand chiffre héro, barre flottante, actions en pastilles, mode immersif sombre).
2. Rendre évident, sur chaque écran : **ce qui est confirmé, ce qui est en attente, qui doit agir, et la prochaine action utile** (gate Product Excellence).
3. Réduire le nombre d'écrans et d'allers-retours : un hub par événement, des modules en sheets.
4. Une navigation native (`NavigationStack`) pilotée par les routes typées existantes (`IosRoute`).
5. Un socle de tokens/composants unique, garanti par un test de contrat à cliquet (`WKStyleGuardTests`).

### Non-objectifs

- Pas de changement du modèle de données, des state machines partagées (KMP) ni de l'API serveur.
- Pas de refonte Android/Web dans cette itération.
- Pas de nouvelle fonctionnalité métier (pas de nouvel agent) ; on réorganise et on restyle l'existant.
- Pas de montée du deployment target (reste iOS 18.2) : Liquid Glass via `#available(iOS 26, *)` avec repli `.regularMaterial`.

## 3. Direction visuelle — hybride

Deux ambiances, choisies selon le moment du cycle de vie :

| Ambiance | Quand | Caractéristiques |
|---|---|---|
| **Claire & douce** | Accueil, hub, organisation, vote, création, Activité | Fond gris perle, cartes blanches sans bordure, ombres quasi invisibles, couleur réservée aux statuts |
| **Immersive** | Invitation reçue, jour J, événement en cours | Fond sombre teinté par la palette de l'événement (`EventMoodPalette`), texte clair, pastilles à contour fin, CTA blanc |

Principes :

- **Un seul accent** (indigo) + **trois couleurs de statut**. La couleur encode l'état de l'événement et l'action requise, jamais la décoration.
- **Un grand chiffre en tête** de chaque écran principal (votes reçus, heure de rendez-vous, budget/personne).
- **Avatars empilés** sur chaque carte d'événement et de module.
- **Une seule action principale par écran** (capsule noire en bas) ; actions secondaires en pastilles.
- **Liquid Glass réservé aux contrôles flottants** (barre de navigation, boutons ronds de chrome) — conforme à `docs/guides/ios/design-system.md`. Jamais sur le contenu.
- Coins toujours `.continuous`.

## 4. Architecture de navigation

```
WakeveRootView
├─ Onboarding / Login (inchangés fonctionnellement, restylés)
└─ AppShell
   ├─ Zone « Événements » → NavigationStack(path: [IosRoute])
   │    EventsHomeView → EventHubView(eventId)
   │                       ├─ .sheet(item: EventModule)      ← modules
   │                       └─ push : PollVoting, ScenarioComparison, BudgetDetail…
   ├─ Zone « Activité »   → NavigationStack (ActivityView → push EventHubView)
   ├─ « ＋ »              → .fullScreenCover(CreateEventFlow)
   └─ WKFloatingNavBar (overlay via .safeAreaInset(edge: .bottom))
        Profil   → avatar en haut à gauche → sheet
        Réglages → WKCircleButton en haut à droite → push
```

Règles :

- **`AppRouter`** (`@Observable`), un par zone, détient `path: [IosRoute]` et `presentedModule: EventModule?`. Deep links, notifications push et liens d'invitation passent tous par `router.open(_ route: IosRoute)`.
- La **barre flottante** est visible uniquement à la racine de chaque zone ; elle se masque dès qu'on descend dans une pile ou qu'une sheet est présentée.
- Barre personnalisée plutôt que `TabView` : le « ＋ » central ouvre un cover, pas un onglet.
- **Onglet Explorer supprimé** ; ses modèles d'événements migrent dans `CreateEventFlow`.
- **Onglet Messages supprimé** en tant que tel ; les conversations vivent dans leur événement et remontent dans Activité.
- `enum WakeveTab` remplacé par `enum AppZone { case events, activity }`.

## 5. Écrans

### 5.1 Accueil — `EventsHomeView`

- En-tête : avatar (profil) · logo discret · bouton rond Réglages.
- **Carte « Prochaine étape »** (`WKHeroMetric`) : l'action la plus urgente tous événements confondus (ex. « 5/8 votes reçus · clôture dans 2 j » + « Relancer les 3 »). Masquée s'il n'y a rien à faire.
- **Grille 2 colonnes de cartes d'événement** : avatars, titre, statut coloré (« Vote en cours », « Sam. 12 oct », « À toi d'agir », « Brouillon »). Tri : action requise → en cours → confirmés → brouillons.
- Section « Passés » repliée en bas.
- Appui long : menu contextuel (modifier, dupliquer, archiver/supprimer selon le rôle).
- État vide : invitation à créer un premier événement (CTA « Créer un événement »).
- Remplace `EventListView` (legacy) et `EventLibraryView` (flag).

### 5.2 Hub d'événement — `EventHubView`

- **Hero** : pastille de statut, titre, résumé (« 3 créneaux · 8 invités »), avatars ; fond teinté léger par la palette de l'événement.
- **Grille de modules** (`WKModuleTile`, 2 colonnes) : icône, titre, résumé d'une ligne, état. Le module qui porte la prochaine action est mis en évidence (contour accent).
- **Modules affichés selon `EventStatus`** :

| Statut | Modules |
|---|---|
| DRAFT | Date, Lieu, Invités (édition) |
| POLLING | Date (vote), Lieu, Invités, Budget estimé |
| CONFIRMED / COMPARING | Date, Scénarios, Invités, Budget |
| ORGANIZING | Transport, Hébergement, Repas, Matériel, Activités, Budget, Paiements, Réunions |
| FINALIZED | Récap, Photos, Paiements/Tricount |

- Carte contextuelle sous la grille pour l'action en cours (ex. vote rapide Oui/Peut-être/Non sur le créneau en tête).
- **CTA principal** en bas (`WKPrimaryButton`) dérivé de la prochaine action (« Confirmer le 18 oct », « Voter », « Passer en organisation »…).
- Remplace `EventDetailView` + `EventDetailInvitationCanvas` + la carte `organizationDetails`.

### 5.3 Sheets de module — `WKModuleSheet`

- `enum EventModule` regroupe : `participants`, `transport`, `accommodation`, `meals`, `equipment`, `activities`, `budget`, `payment` (inclut Tricount), `photos`, `comments`, `meetings`.
- Anatomie commune : poignée · titre + `WKStatusPill` (ex. « 2 sans place ») · cartes internes `.inset` avec avatars · phrase « ce qui manque » · `WKActionBar` (action principale + groupe d'icônes).
- Detents `.medium` / `.large`.
- **Restent en push plein écran** : vote (`pollVoting`, `pollResults`), comparaison de scénarios (`scenarioComparison`, `scenarioDetail`), détail de dépense (`budgetDetail`), réunion (`meetingDetail`).

### 5.4 Activité — `ActivityView`

- Fusion de `InboxView` et des messages.
- Filtres : **À traiter** (défaut, avec compteur) · **Tout**.
- Entrées **groupées par événement** ; points rouges pour ce qui attend une action de l'utilisateur ; messages résumés en une ligne (« 3 messages ») ouvrant le fil de l'événement.
- Tap → `EventHubView` (ou directement la sheet/écran concerné).
- Badge sur l'icône Activité de la barre flottante = nombre d'éléments « À traiter ».

### 5.5 Création — `CreateEventFlow`

- Remplace `CreateEventSheet` (2 621 lignes).
- Quatre étapes DRAFT (cf. AGENTS.md) formulées en questions, **une par écran** :
  1. **Quoi ?** — titre, description, type (modèles ex-Explorer en pastilles : anniversaire, week-end, dîner…).
  2. **Qui ?** — invités, estimation min/max/attendus.
  3. **Où ?** — lieux potentiels (optionnel).
  4. **Quand ?** — créneaux + moment de la journée (au moins un requis).
- Indicateur de progression en segments, fermeture par croix.
- Brouillon auto-enregistré à chaque étape (`UpdateDraftEvent`) avec mention « Brouillon enregistré ».
- Validation par étape conforme au tableau des règles DRAFT ; navigation bloquée si invalide, message au niveau du champ.
- Dernière étape : « Lancer le sondage » → `StartPoll` → ouverture du hub.

### 5.6 Mode immersif — `WKImmersiveScaffold`

- Utilisé pour : réception d'une invitation (`invite(token:)`), jour J, événement en cours.
- Jour J : carte/itinéraire, heure du rendez-vous en grand, lieu, état des participants (« 4 arrivés · Léa à 12 min »), pastilles logistiques (covoiturage, dîner), CTA blanc (« Je suis en route »).
- Palette dérivée de `EventMoodPalette`.
- Les données de présence en direct sont hors périmètre si elles n'existent pas encore : l'écran affiche alors les informations statiques (heure, lieu, logistique).

### 5.7 Profil et Réglages

- Profil : sheet depuis l'avatar (identité, compte, déconnexion, RGPD).
- Réglages : push depuis le bouton rond (notifications, préférences, à propos).
- Restylage uniquement ; contenu fonctionnel inchangé.

## 6. Tokens — `Theme/WK.swift`

Source unique, namespace `WK`. Remplace `WakeveTheme`, les `Typography`/`Spacing`/`CornerRadius`/`AdaptiveColors` legacy, `WakeveColors`, `BrandColor`, `SemanticColor`. `EventMoodPalette` est conservé et consommé par `WK.Mood(palette:)`.

| Famille | Tokens (clair / sombre) |
|---|---|
| Surfaces | `canvas` #F2F2F4 / #000000 · `card` #FFFFFF / #1C1C1E · `cardInset` #F6F6F8 / #2C2C2E |
| Texte | `textPrimary` / `textSecondary` / `textTertiary` = `label` / `secondaryLabel` / `tertiaryLabel` |
| Accent | `accent` #5B54D6 / #8B85F0 (+ `accentFill` pour fonds doux) |
| Statuts | `confirmed` (vert), `pending` (ambre), `actionNeeded` (rouge), `draft` (gris) — chacun avec `fill` (fond doux) et `onFill` (texte, contraste AA) |
| Immersif | `WK.Mood(palette:)` → `background`, `surface`, `textPrimary`, `textSecondary`, `pillStroke`, `accent` |
| Rayons | `sm` 12 · `md` 18 · `lg` 28 ; forme `WK.pill` (rayon `lg`, capsule tant que la hauteur ≤ 56 pt, rectangle arrondi au-delà) — toujours `.continuous` |
| Espacements | 4 · 8 · 12 · 16 · 24 · 32 ; marge d'écran 16 |
| Typographie | `display` (SF Pro Rounded, chiffres héros) · `title` · `headline` · `body` · `caption` — toutes basées sur les `Font.TextStyle` (Dynamic Type) |
| Mouvement | `snappy` (tap), `smooth` (sheet/push), `bouncy` (validation) ; fondus si Reduce Motion |

Valeurs de statut en mode clair (couleur / `fill` / `onFill`) :

| Statut | Couleur | `fill` | `onFill` |
|---|---|---|---|
| `confirmed` | #2E9D5B | #DFF3E6 | #1E6B3C |
| `pending` | #B7791F | #FFF1D6 | #8A5A0B |
| `actionNeeded` | #D64545 | #FDE7E7 | #A12E2E |
| `draft` | #8A8A8E | #F2F2F4 | #5A5A5F |

En mode sombre, valeurs explicites définies dans `Theme/WK.swift` (`WK.Status`), contraste AA vérifié par `WKTokensTests`. `status.color` n'est **jamais** utilisé pour du texte : seulement comme repère non textuel (≥ 3:1) ; le texte de statut utilise `onFill`.

## 7. Composants — `Components/WK/`

| Composant | Rôle |
|---|---|
| `WKCard` | Carte blanche ; variantes `.inset`, `.selected` |
| `WKStatusPill` | Statut texte + couleur, dérivé de `EventStatus` et du rôle utilisateur |
| `WKAvatarStack` | Jusqu'à 4 avatars chevauchés puis « +N » ; libellé VoiceOver « Léa, Tom et 4 autres » |
| `WKHeroMetric` | Libellé, grand chiffre, sous-titre, action optionnelle |
| `WKPrimaryButton` | Capsule pleine ; une seule par écran |
| `WKActionBar` | Action principale + groupe de boutons-icônes en pastille |
| `WKChip` | Suggestion ou filtre, état sélectionné |
| `WKModuleTile` | Tuile du hub (icône, titre, résumé, état, mise en évidence) |
| `WKModuleSheet` | Gabarit standard des sheets de module |
| `WKFloatingNavBar` | Barre flottante 3 zones ; `.glassEffect` iOS 26, `.regularMaterial` sinon ; badge Activité |
| `WKCircleButton` | Bouton rond de chrome (retour, réglages, plus) en verre |
| `WKImmersiveScaffold` | Conteneur du mode immersif |

Exigences transverses :

- Previews systématiques : clair, sombre, Dynamic Type AX5, Reduce Transparency, Increase Contrast.
- Cibles tactiles ≥ 44 pt.
- La couleur n'est jamais le seul porteur d'information (toujours un texte).
- Identifiants d'accessibilité stables sur chaque composant interactif (pour les tests UI).

## 8. Offline et états

- Chaque écran principal affiche un bandeau discret d'état hors ligne et marque les actions en file d'attente (pastille « En attente de synchro »), conformément à la stratégie offline-first.
- États vides, de chargement (squelettes de cartes) et d'erreur définis pour Accueil, Hub, Activité et chaque sheet.

## 9. Plan de migration par couches

Couches 2 à 8 derrière le flag `iosRedesign2026` (`FeatureFlags`) ; `iosInvitationExperienceV1` est resté séparé. Le flag est supprimé à la couche 9 (la refonte est l'unique app). Chaque couche est une PR indépendante et livrable.

| # | Couche | Contenu |
|---|---|---|
| 0 | Nettoyage | Suppression du code mort (`HomeView`, `EventDetailExperienceView`, `LiquidGlassTabBar`) |
| 1 | Socle | `WK` tokens + composants + previews + garde-fou `WKStyleGuardTests` (cliquet) |
| 2 | Shell | Shell progressif : `AppRouter` (plan de routage + présentation unique profil/réglages), `RedesignShellView` + `WKFloatingNavBar`, zones Événements (aiguillage `AppView` existant) et Activité (`InboxView`), derrière `iosRedesign2026` |
| 3 | Accueil | `EventsHomeView` |
| 4 | Hub | `EventHubView` + `WKModuleTile` (modules ouvrent encore les anciens écrans) |
| 5 | Modules | Conversion en `WKModuleSheet`, en trois sous-couches (ordre modifié le 2026-09-30, voir §15) : **5a** socle + Repas, Matériel, Activités, Hébergement, Photos ; **5b** Budget, Cagnotte (+ Tricount), Réunions ; **5c** Commentaires, Transport, Invités (résumés avec repli plein écran). Couches 5a à 5c livrées : tous les modules à résumé sont en sheet ; Date, Lieu, Scénarios et Récap restent des écrans pleins (vote, résultats, comparaison) |
| 6 | Activité | `ActivityView` (fusion Inbox + messages), livrée le 2026-10-01 : entrées groupées par événement, « À traiter (n) » calculé depuis l'état réel (règles de l'accueil), « Tout » = + notifications et « N nouveaux messages » ; `InboxView` reste sur le chemin legacy (voir §15) |
| 7 | Création | `CreateEventFlow`, livrée le 2026-10-02 : quatre questions (Quoi ? · Qui ? · Où ? · Quand ?), brouillon enregistré à chaque étape, « Lancer le sondage » → hub ; ＋ ouvre le flux sous la refonte quand le rollout invitation est éteint (studio sinon, Swarm DAO #48), les brouillons du studio se rouvrent toujours dans le studio (voir §15) |
| 8 | Immersif | `WKImmersiveScaffold`, livrée le 2026-10-02 : invitation reçue (à la place du hub tant que `invitationLandingEventId` désigne l'événement ; « Voter » ou « Voir l'événement », pas d'Accepter/Décliner) et jour J (heure, lieu, carte, pastilles logistiques, « Itinéraire » ; bannière du hub et « Prochaine étape » de l'accueil) ; fond des ambiances corrigé (voir §15) |
| 9 | Bascule | Livrée le 2026-10-02 : flag `iosRedesign2026` supprimé (refonte inconditionnelle), shell à onglets, ancien accueil, bibliothèque, Explorer, Inbox, ancien détail + canvas et ancienne feuille de création supprimés, composants/jetons et clés de traduction morts purgés ; baselines du garde-fou abaissées aux valeurs mesurées (non nulles : écrans plein écran de repli encore sur l'ancien design system ; 0 reporté à un restylage séparé) ; voir §15 |

Garde-fou : `WKStyleGuardTests` (XCTest, SwiftLint n'étant pas installé) — zéro style en dur dans `Components/WK`, compteurs de `Views/` à cliquet (ne peuvent que baisser), baseline abaissée aux valeurs mesurées à la couche 9 (0 reporté au restylage des écrans de repli). Périmètre : `Views/` et `Components/WK` uniquement. `WKModuleSheet` arrive en couche 5, `WKImmersiveScaffold` en couche 8.

## 10. Tests

- **TDD** : tests écrits avant chaque couche (règle AGENTS.md).
- **Unitaires** : `AppRouter` (résolution des routes, deep links, push/pop, présentation de module), dérivation des statuts et de la « prochaine action », tri de l'accueil, groupement d'Activité, sélection des modules selon `EventStatus`.
- **UI (XCUITest)** : parcours création → sondage → confirmation → organisation → finalisation dans la nouvelle UI ; adaptation des tests existants via identifiants d'accessibilité.
- **Rendu** des composants `WK` : tests de mesure `UIHostingController.sizeThatFits` (cibles ≥ 44 pt, plafonds Dynamic Type, non-débordement AX5 sur 375 pt) + galerie DEBUG (clair/sombre/AX5/Reduce Transparency/Increase Contrast/mood). Pas de snapshots image pour l'instant.
- **Offline** : un scénario par écran principal (création hors ligne, vote hors ligne, actions en file visibles).
- **Accessibilité** : audit VoiceOver et Dynamic Type par couche.

## 11. Critères d'acceptation

1. La navigation est entièrement native (retour et swipe-back fonctionnels partout) ; `enum AppView` n'existe plus.
2. Depuis l'accueil, chaque événement montre son statut et si l'utilisateur doit agir, sans l'ouvrir.
3. Tout module d'organisation est accessible en ≤ 2 taps depuis le hub, sans quitter le contexte de l'événement.
4. Aucune occurrence de `Color(hex:`, `.font(.system(size:` ou `cornerRadius` littéral dans `Views/` (`WKStyleGuardTests` avec baseline 0). *Non atteint en couche 9 (baselines 14 / 48 / 44 / 3 / 6) : reporté au restylage des écrans de repli, voir §15.*
5. Un seul fichier de tokens ; les anciens fichiers de couleurs sont supprimés. *Non atteint en couche 9 : `DesignSystem.swift`, `WakeveColors.swift`, `BrandColor`/`SemanticColor` restent (élagués) pour les écrans de repli.*
6. Tous les écrans passent Dynamic Type AX5 sans troncature bloquante et VoiceOver sans élément non libellé.
7. Tests unitaires, UI et offline verts.

## 12. Métriques de succès

- Taps moyens pour voter depuis l'ouverture de l'app : cible ≤ 3.
- Taux de complétion du flux de création (DRAFT → POLLING) : en hausse vs l'actuel.
- Nombre de vues SwiftUI de premier niveau par événement : ~15 → 1 hub + sheets.

## 13. Rollback

- Le flag `iosRedesign2026` permettait de revenir à l'ancienne UI jusqu'à la couche 8.
- Depuis la couche 9 (2026-10-02), **aucun interrupteur** n'est conservé : le retour arrière se fait par revert de la PR de la couche 9 (branche `claude/ios-redesign-layer-9`), qui restaure le flag, le code legacy, les clés de traduction et les baselines ; les couches 0-8 restent en place.

## 14. Risques

| Risque | Mitigation |
|---|---|
| Régressions de navigation (deep links, notifications) | Tests unitaires `AppRouter` exhaustifs sur toutes les `IosRoute` avant la couche 2 |
| Durée longue avec deux UI en parallèle | Couches courtes, une PR par module, bascule planifiée dès la couche 8 |
| Tests UI existants cassés | Identifiants d'accessibilité posés dès la couche 1 |
| Liquid Glass indisponible sur iOS 18 | Replis `.regularMaterial` testés ; verre limité aux contrôles |

## 15. Écarts d'implémentation constatés (couches 0-1)

Consignés après les couches 0-1 (proposition #47) ; ils font désormais partie de la spec.

- **Tokens ajoutés** : `textMuted` (texte secondaire opaque AA sur cartes), `onCardInset`, `onAccent`, `onAccentFill`, `Typo.micro`, `Size.*` (`minTapTarget`, `avatar`, `primaryButtonHeight`), `Stroke.*`, `Space.xxxs`, forme `WK.pill`. `Typo.caption` = `.footnote`, `Typo.micro` = `.caption`.
- **Helpers** : `WK.appLocale` (langue de l'app + région de l'utilisateur), `WK.localizedFormat(_:locale:)`, modificateur `wkAccessibilityID(_:)`, `WK.shape(_:)`.
- **`AppZone`** introduit en couche 1 ; coexiste avec `WakeveTab` jusqu'à la couche 2.
- **`WKStatusPill`** prend `(text, WK.Status)` ; la dérivation depuis `EventStatus` et le rôle utilisateur arrive en couches 3-4.
- **`WKCard`** n'a pas d'ombre (fond blanc sur canvas gris suffit) ; à réévaluer en couche 3.
- **Pluriels** : `wk.nav.activity.badge_format` vit dans `Localizable.stringsdict` (5 langues).
- **Avatars** : le compteur « +N » est numérique ; le libellé VoiceOver nomme jusqu'à 3 personnes, sinon « A, B et N autres ».
- **Barre flottante** : Dynamic Type plafonné à AX1 ; aux tailles d'accessibilité, la zone sélectionnée n'affiche que son icône (titre via VoiceOver et Large Content Viewer) — validé (2026-09-28).

### Couche 2 (shell progressif, décidé le 2026-09-28)

- Pas de `NavigationStack(path:)` en couche 2 : la zone Événements héberge l'aiguillage `AppView` existant ; la migration vers une pile native se fait écran par écran quand chacun est refondu (couches 3-5) ; `AppView` disparaît en couche 9.
- `presentedModule` / `EventModule` reportés à la couche 5.
- Le flag `iosInvitationExperienceV1` reste tel quel ; `FeatureFlags` ne porte que `iosRedesign2026` (activable en debug via `-iosRedesign2026 YES`) ; fusion en couche 9.
- Réglages : le bouton rond ouvre les préférences de notification ; profil et réglages sont une présentation unique (`AppRouter.presentation`, `.sheet(item:)`).
- Sémantique changée sous flag : le deep link `wakeve://settings` ouvre les préférences de notification (en legacy, il ouvrait `ProfileTabView`) ; le profil reste accessible via `wakeve://profile` et l'avatar.
- Zone Activité : `InboxView` reçoit `reloadToken` (rechargement à chaque entrée dans la zone) et `onRootStateChange` (la barre flottante se masque dans le détail et en mode sélection).
- Création depuis la barre : `beginRedesignEventCreation()` reprend la logique du deep link `.eventCreate`.
- Correctif inclus : les taps de notification portant un `deepLink` sont désormais ouverts (`NotificationDeepLink`).
- Limites visibles connues (résolues par la couche 3) : l'en-tête avatar/réglages s'empile au-dessus du titre de l'ancien accueil ; la zone Activité affiche le titre « Messages » d'`InboxView`.

### Couche 3 (nouvel accueil, 2026-09-28)

- Source de l'accueil : projections de la bibliothèque d'invitations (filtrage par utilisateur, rôle, passé/à venir, état de synchro) + dépôt d'événements (votes reçus, bulletin de l'utilisateur, noms), via `SharedEventsHomeSource` → `EventsHomeViewModel` → règles pures `HomeEventSummary` / `HomeNextStep`.
- Votants éligibles = participants ayant accepté + organisateur ; seuls leurs bulletins complets comptent.
- Un participant n'est invité à voter que si l'invitation est acceptée, le sondage ouvert (échéance future) et l'événement non en lecture seule.
- Organisateur : « Prêt à confirmer » dès que tous les **autres** votants éligibles ont voté (au moins un), qu'il ait voté ou non ; après l'échéance, il est « prêt » dès qu'un bulletin est complet ; seul sur l'événement (sondage ouvert), il n'est jamais « prêt ». Tant que le sondage est ouvert et qu'il n'a pas voté (et que les autres n'ont pas tous voté), l'accueil lui affiche « À toi de voter ».
- Les sondages à créneaux flexibles ne sont pas en lecture seule (lecture seule = finalisé, ou passé et non gardé actif). Si les votes sont illisibles, aucune action n'est proposée.
- Un sondage dont l'échéance est passée ou dont tous les créneaux sont terminés n'est plus une action de vote ; seuls les brouillons et les sondages sans créneau daté restent actifs malgré le classifieur.
- Écarts §8 : chargement par `ProgressView` (pas de squelettes de cartes) ; seul le bandeau « modifications en attente de synchro » existe (pas encore de bandeau hors ligne dédié).
- Pas d'action « Relancer » (aucune API) : l'action de « Prochaine étape » ouvre le vote, les résultats ou l'organisation.
- Menu contextuel : Ouvrir, Modifier le brouillon, Supprimer (organisateur, non finalisé). Pas de Dupliquer ni Archiver (aucune API).
- En-tête du shell : mot-symbole « wakeve » au centre et fond opaque (le contenu défile dessous).
- Défauts repérés hors couche, à traiter plus tard : « Invités et invitations » sans bouton retour ; « Terminé » de l'aperçu du studio ferme tout le studio ; message « Date enregistrée sur cet appareil » après un vote ; en-tête du shell très grand en AX5.

### Couche 4 (hub d'événement, 2026-09-30)

- Sous `iosRedesign2026`, `case .eventDetail` ouvre `EventHubContainer` (via `eventHubContent(for:)`, hors de la tranche legacy) ; flag éteint, `EventDetailView` est inchangé. Chaîne : règles pures `EventHubModel` ← faits `EventHubFacts` lus hors du fil principal par `SharedEventHubSource` → `EventHubViewModel` → `EventHubView` (composants `WK` uniquement).
- Modules par phase : brouillon date/lieu/invités ; sondage + budget ; confirmé et comparaison date/scénarios/invités/budget ; organisation transport/hébergement/repas/matériel/activités/budget/paiements/réunions (paiements ajoutés en couche 5b) ; finalisé récap/photos/paiements. Au plus une tuile mise en évidence (date pour vote/résultats/confirmation/ajout de dates, scénarios en comparaison).
- Vote rapide Oui/Peut-être/Non : ouvre `PollVotingView` (aucune soumission depuis le hub, le journal de bulletins n'accepte que des bulletins complets). La carte n'apparaît que si un créneau est en tête (`leadingSlotStart`), sinon seul le CTA « Voter » reste.
- Transitions confirmé → organisation et organisation → finalisé : dans le hub, toujours après confirmation (`confirmationDialog`), via `EventLifecycleTransitionController` possédé par le conteneur (même chemin que `EventDetailView`) ; succès → relecture de l'événement et `eventHubReloadToken += 1`. Invité local : « Connecte-toi pour finaliser » reprend `authStateManager.signOut()`.
- Règle « prêt à confirmer » partagée : `PollReadiness.readyToConfirm` est utilisée par `HomeEventFacts` et `EventHubFacts` (pas de copie) ; elle l'emporte sur le vote de l'organisateur, comme sur l'accueil.
- Menu « … » en `confirmationDialog` : Infos de l'événement, Ajouter des participants (organisateur, non finalisé), Signaler (non-organisateur, `ModerationActionSheet`), Contacter le support (mailto existant).
- Hero teinté par le type d'événement : `WKCard(tint:)` avec `WK.Tint.surface` (0,12) sur `EventMoodPalette` ; texte principal conservé pour le contraste. Teinte discrète en pratique (palette par défaut presque neutre).
- Verrous alignés sur les gardes réelles des `case` de destination, pour qu'aucune tuile ouverte ne mène à `AccessDenied` : budget, réunions, paiements = `canAccessOrganizationDashboard` (organisation/finalisé + accès) — le budget est donc verrouillé en sondage, confirmé et comparaison ; transport = confirmé/organisation/finalisé + accès (pas la comparaison) ; hébergement, repas, matériel, activités, photos = `canAccessDetailedPlanning` (confirmé/comparaison/organisation/finalisé + accès) ; date, lieu, invités, scénarios, récap jamais verrouillés.
- Aiguillage pur et testé `EventHubRouting`, dépendant du rollout `iosInvitationExperienceV1`. Rollout actif : invités et « Ajouter des participants » → routeur invitation (`EventAudienceView`), dates d'un brouillon → studio (`editDraftFromHome`), récap et Infos → `EventInformationView`. Rollout éteint (défaut ; ces écrans retombaient sur le hub, donc sans effet) : invités, « Ajouter des participants », « Ajouter des dates » et la tuile date d'un brouillon → `ParticipantManagementView` (liste, partage d'invitation, `DraftDatesSheet`, lancement du sondage) ; récap → `PollResultsView` (date et horaires retenus, écran sans garde) ; l'entrée « Infos » est masquée.
- Retour hors racine : la barre flottante étant masquée hors racine, les écrans sans retour propre atteints depuis le hub (budget, dépenses, réunions, détail de réunion, cagnotte, Tricount, « Invités et invitations ») reçoivent un `WKCircleButton` « Retour » (`RedesignBackRoute`, fonction pure) vers le hub, ou vers l'écran parent (dépenses → budget, réunion → réunions, Tricount → cagnotte). Rangée en `safeAreaInset` du shell, sauf pour le budget et les réunions qui poussent un détail dans leur propre pile : le bouton y est lu dans leur barre d'outils (`\.redesignBackAction`) pour que le retour système le remplace, sans superposition. Rien sur les écrans `AccessDenied`, qui ont déjà leur retour. Les `case` legacy ne sont pas modifiés ; flag éteint, `redesignBackAction` vaut nil. Corrige le défaut « Invités et invitations sans bouton retour » relevé en couche 3 (sous flag).
- Défauts repérés hors couche : `PollResultsView` ne reconnaît que `CONFIRMED` comme date retenue — en organisation/finalisé, il présente le « meilleur créneau » avec l'annonce « je le confirme » (touche la tuile date et, sans rollout, le récap) ; clé `accommodation.empty.title` non traduite ; « 1 confirmés » (format non pluriel réutilisé) ; « À préparer » comme indice du récap d'un événement finalisé ; en AX5 le contenu du hub défile sous les boutons ronds de la barre haute (sans fond).

### Couche 5a (sheets de modules, 2026-09-30)

- **Découpage et ordre modifiés** (vs §9, qui commençait par Transport) : 5a socle + Repas, Matériel, Activités, Hébergement, Photos (petites listes, mêmes dépôts que le hub) ; 5b Budget, Cagnotte (+ Tricount), Réunions ; 5c Commentaires (brancher l'envoi), Transport, Invités. Raison : Transport est l'écran le plus lourd et le plus verrouillé par les tests ; commencer par les modules simples rend la conversion incrémentale et sûre.
- **Anatomie de `WKModuleSheet`** (`Components/WK`, zéro style en dur) : en-tête titre `WK.Typo.title` + `WKStatusPill` optionnelle + `WKCircleButton` fermer (`wk.sheet.close`, ≥ 44 pt) ; corps défilant de cartes `WKCard(.inset)` puis phrase « ce qui manque » en `textMuted` ; bas en `safeAreaInset` : `WKActionBar` (action principale + icônes secondaires) ou, sans action principale, pastilles secondaires libellées ; detents medium/large, poignée visible, fond `canvas`.
- **Routage** : `EventHubRoute.sheet(HubModule)` pour les modules convertis (`EventHubRouting.sheetModules`). La sheet n'est présentée que si la garde du `case` legacy (`canAccessDetailedPlanning`) est satisfaite ; sinon l'écran legacy s'ouvre et affiche son refus comme avant (`EventHubRouting.sheetRoute`). `performHubRoute` relit toujours l'événement avant de décider.
- **Présentation et repli** : `presentedHubModule` + `.sheet(item:)` sur le hub dans `AuthenticatedView`. « Plein écran » et « Commentaires » (repas, matériel, activités, hébergement) posent `pendingHubFallback` puis ferment la sheet ; l'écran legacy (ou `.comments` avec la section du module) n'est ouvert que dans `onDismiss`, jamais pendant la présentation. Fermeture simple (croix, glissement, tap hors sheet) → rechargement du hub (`eventHubReloadToken`). « Plein écran » puis glissement immédiat : un seul `onDismiss`, l'écran legacy s'ouvre (vérifié au simulateur). Les `case` legacy et leurs vues sont inchangés ; flag éteint, le détail legacy est inchangé.
- **Écarts d'implémentation** :
  - Modèle de données localisé : `HubModuleSheetData.make(raw:…, locale:)` produit directement les textes (pastille, détail, phrase manquante) au lieu de clés, pour que les règles restent pures et testées par langue ; la source `SharedEventModuleSheetSource` lit hors du fil principal (`Task.detached` + annulation) et convertit les objets Kotlin en entrées Swift simples.
  - Ajout d'un repas : `MealFormSheet` existant présenté **depuis la sheet** (organisateur, non finalisé), pas depuis le hub.
  - `MealFormSheet` n'écrit pas en base (lacune préexistante, identique à l'écran legacy) : le repas ajouté reste affiché localement dans la sheet, puis disparaît après fermeture ; la tuile Repas du hub n'évolue pas.
  - Photos : sheet réduite à l'indice « Partage tes photos » et au repli plein écran (pas encore d'album).
- **Défauts trouvés et corrigés** : clé `accommodation.empty.title` absente des 5 langues — l'état vide de l'écran legacy Hébergement, désormais atteint par « Plein écran », affichait la clé brute (relevé en couche 4).
- **Constats non corrigés** : en AX5 au detent medium, peu de place pour le contenu (pastilles secondaires empilées, texte coupé) — le detent large reste lisible ; pendant le chargement des Repas, les secondaires sont d'abord libellées puis passent en icônes quand « Ajouter un repas » apparaît (léger saut) ; clé `events.status.comparing` absente des 5 langues (écran legacy, hors 5a) ; les responsables d'un repas ajouté sont affichés par identifiant, comme dans le formulaire legacy (`participantModels(for:)`). Avec le rollout invitation actif, un événement finalisé ouvre l'archive, pas le hub : la sheet Photos n'est atteignable que rollout éteint.

### Couche 5b (Budget, Paiements, Réunions en sheets, 2026-10-01)

- **Routage** : `EventHubRouting.sheetModules` inclut désormais `.budget`, `.payments`, `.meetings`. Garde par module (`EventHubRouting.sheetGuard`) identique au `case` legacy : `canAccessDetailedPlanning` pour les modules 5a, `canAccessOrganizationDashboard` pour budget, réunions et cagnotte (le `case .paymentPot` écrit la même règle en ligne). Sans accès, l'écran legacy s'ouvre et affiche son refus. Repli « Plein écran » : `.budgetOverview`, `.paymentPot`, `.meetingList`.
- **Contenu** (`HubModuleSheetData.make`, lectures dans `SharedEventModuleSheetSource`, détachées et annulables, seulement pour un module routé en sheet) :
  - Budget : `BudgetRepository.getBudgetByEventId` en lecture seule (jamais `BudgetViewModel.load()`, qui crée un budget) ; pastille « Réel x € / estimé y € » (`.pending` si dépassement, `.confirmed` sinon) ; une carte par catégorie non nulle (`budget.category.*`, pastille « Dépassé » si réel > estimé) ; « Aucun budget pour l'instant. » sans budget ou budget à zéro ; « Le réel dépasse l'estimé de x € » en cas de dépassement.
  - Paiements : carte cagnotte (`getActivePotForEvent`, même texte que la tuile via `HubSummaryText.paymentPot`, pastille « Ouverte »/« Clôturée ») et carte Tricount (`getPaymentReadiness`, même ordre que `tricountSummaryValue` : non requis, lien vérifié, lien à vérifier, à décider ; pastille « Prêt »/« À faire ») ; « Aucune cagnotte pour l'instant. » sans cagnotte.
  - Réunions : réunions non annulées triées par date (date/heure locales courtes, plateforme, « Lien prêt »/« Sans lien ») ; pastille « N à venir » (ni annulées ni terminées) ; « N réunion(s) sans lien » ; « Aucune réunion prévue pour l'instant. ». Tuile et sheet partagent `HubSummaryText.meetingCounts`.
- **Actions** (`HubModuleSheetData.primaryAction`) : « Voir les dépenses » pour tous, même finalisé (consultation) → `.budgetOverview` ; « Gérer la cagnotte » (organisateur, non finalisé, règle `canManagePayment`) → `.paymentPot` ; « Planifier une réunion » (organisateur, non finalisé, règle `canCreateMeetings`) → `.meetingList` ; secondaire « Tricount » → `.tricount`. Tous les replis passent par `HubSheetLifecycle.requestFallback` (appliqués après fermeture) ; les écrans legacy gardent leurs retours (`RedesignBackRoute`) : budget et réunions reviennent au hub, Tricount revient à la cagnotte puis au hub.
- **Écarts** :
  - Les totaux du budget sont portés par la pastille d'en-tête, sans carte « Total » séparée.
  - La tuile Budget reste « estimé » (inchangée) ; la sheet ajoute le réel.
  - Budget et Réunions n'ont pas de tuile en phase finalisée : la sheet en lecture seule n'est vérifiée qu'en tests.
- **Vérifié au simulateur** (iPhone 18 Pro, données QA + budget, cagnotte et réunions insérés en base) : tuile Budget verrouillée avant organisation ; passage en organisation ; sheets Budget (vide puis dépassement), Réunions (medium/large) ; « Voir les dépenses » → écran budget → retour hub ; « Planifier une réunion » → liste legacy → retour hub ; Paiements (finalisé) : « Tricount » → Tricount → Cagnotte → hub, « Plein écran » → cagnotte ; AX5 (detent large, pastilles empilées) ; sombre ; flag éteint → accueil et détail legacy inchangés. Captures `/tmp/wk-l5b-*.png`.
- **Revue de la couche 5b (2026-10-01)** :
  - Tuile Paiements ajoutée en organisation (entre Budget et Réunions) : « Gérer la cagnotte » est atteignable pendant l'organisation ; verrou inchangé (`canAccessOrganizationDashboard`).
  - Cagnotte : tuile et sheet lisent la dernière cagnotte de l'événement, ouverte ou clôturée (`potQueries.selectByEvent`, la plus récente par `createdAt` via `HubSummaryText.latest`) ; une cagnotte clôturée garde « Clôturée » (pastille de la sheet, suffixe de la tuile).
  - Réunions : une réunion terminée (`ENDED`, ou programmée dont l'heure est passée) n'a pas de pastille et affiche « Terminée » (`meetings.ended`) ; les réunions à venir passent avant les terminées, puis par date ; seules les réunions à venir comptent dans « N à venir » et « sans lien » ; titre vide → nom de la plateforme. L'instant courant est injecté (`now`) pour les tests.
  - Budget : dépassement comparé en centimes (`HubModuleSheetData.isOverspent`) ; une dépense sans estimation est un dépassement. Action principale « Voir le budget » (`hub.sheet.budget.view`), sans « Plein écran » en doublon.
  - Synchro en attente des sheets Budget et Paiements : file du flux, plus la file de la phase 5 avec le filtre de l'écran legacy (`BudgetViewModel.isPhase5PendingSync`).
- **Constat corrigé hors couche** : l'ajout d'une dépense dans l'écran Budget ne plante plus (`sharedBy` vide remplacé par les participants de l'événement ou l'organisateur, exceptions Kotlin déclarées par `@Throws` et rattrapées) ; « 1 personnes » sous la dépense reste à accorder.
- **Constats non corrigés (hors 5b)** : lancement avec `--wakeve-qa-seed-invitation-experience --wakeve-qa-open-invitation-route library` parfois bloqué sur le chargement de l'accueil (sélecteur Ktor en attente, aucune trame app active) — relancer sans ces arguments charge l'accueil.

### Couche 5c (Commentaires, Transport, Invités, 2026-10-01)

- **Commentaires (correctif)** : `EventCommentsRouteView` n'injectait aucun callback d'écriture dans `CommentListView` (envoyer, répondre, modifier, supprimer, épingler sans effet). Chaque écriture passe désormais par `CommentRepository` puis relit la liste : envoi (`createComment`, auteur = nom du compte, sinon identifiant), réponse et modification par une alerte avec champ texte, suppression douce (`softDeleteComment`) après confirmation, épinglage (`pinComment`/`unpinComment`). Droits revérifiés sur l'auteur en base, mêmes règles que le menu de `CommentItemView` : modifier ses propres commentaires, supprimer les siens (l'organisateur peut retirer ceux des autres), épingler réservé à l'organisateur. Contenu vide ignoré, > 2 000 caractères refusé avant `CommentRequest` (dont l'`init` Kotlin lève une exception).
  - `createComment` et `updateComment` déclarent `@Throws(IllegalArgumentException::class)` (+ `CancellationException` pour la fonction suspendue) : un refus de modération (`ModerationRejectedException`) ou un parent introuvable arrive en Swift comme erreur localisée (« Ce message ne respecte pas les règles de la communauté. Reformule-le. ») au lieu d'arrêter l'app.
  - Sections : les 9 sections de l'app correspondent aux 9 de `CommentSection` (`EventCommentsRouteView.repositorySection`) ; repas, scénario, sondage et budget lisaient auparavant toutes les sections mélangées (`sharedValue == nil`). Le `case .comments` legacy et `CommentListView` sont inchangés.
  - Deux extensions Swift masquaient le modèle partagé (`CommentItemView.swift`) : `Comment_.isPinned` (toujours faux, l'épinglage ne s'affichait jamais) et `CommentThread.hasMoreReplies` (bouton « Charger plus de réponses » sans effet après une suppression douce). Supprimées.
- **Transport en sheet** : garde `canAccessTransportPlanning` (`EventHubRouting.SheetGuard.transportPlanning`), repli `.transportPlanning`. Lecture seule via `TransportRepositoryBridge` (plans, plan retenu) et `transportQueries` (« non requis », départs), jamais via `TransportPlanningViewModel`. Cartes : plan retenu seul, ou plans proposés (optimisation, coût total, durée du trajet le plus long), plus une carte « Départs participants » tant qu'aucun plan n'est retenu (participants confirmés comptés comme l'écran legacy, `confirmedParticipantIds`). Pastille « Plan choisi » / « À décider » / « Pas nécessaire » (neutre) ; la tuile du hub lit la même règle (`HubModuleSheetData.transportState` + `transportSummary` : texte de la pastille, ou « N options » à départager). Action principale « Organiser le transport » pour tous ceux qui ont accès (l'écran legacy applique ses droits) ; secondaire « Commentaires » (section transport), sans « Plein écran » en double.
- **Invités en sheet** : aucune garde (`SheetGuard.unguarded`). Même lecture et même règle que la tuile (`getParticipantRecords` + `ParticipantAccessMapper`, refusés à part, confirmés = accès aux détails) ; sections « Confirmés » / « En attente » / « Ont décliné », organisateur en tête et marqué « Organisateur » ; pastille « N confirmés » ; « N invités en attente » ou « Personne n'est invité pour l'instant. ». « Inviter » (organisateur, non finalisé) et « Plein écran » (sinon) suivent `EventHubRouting.fullScreenRoute` → `addParticipantsRoute` : participants legacy flag invitations éteint, audience du routeur d'invitations allumé. `HubSheetLifecycle` accepte désormais une route du hub comme repli (`Effect.perform`), toujours appliquée après la fermeture.
- **Écarts** : sans enregistrement de participants, les participants de l'événement sont listés, l'organisateur « Confirmé » (il a toujours accès aux détails) et les autres « En attente » ; un organisateur absent des enregistrements n'est pas ajouté. La tuile et la sheet lisent la même liste (`SharedEventHubSource.guestEntries`), donc les mêmes comptes. La durée d'un plan est formatée par `DateComponentsFormatter` (« 1 h et 35 min » en français).
- **Correctifs de revue (couche 5c)** :
  - **Plantages** : la longueur d'un commentaire se compte en unités UTF-16 comme Kotlin (`String.length`) — 1 001 emojis passaient le décompte Swift puis faisaient échouer l'`init` de `CommentRequest` (arrêt de l'app) ; les espaces Kotlin U+001C…U+001F comptent comme vides. `CommentRepository.updateComment` valide le contenu (non vide, ≤ 2 000) avant l'`UPDATE` : un texte trop long n'est plus écrit en base (la ligne serait devenue illisible par `Comment.init`, et la section ne s'ouvrait plus).
  - **Modération** : un message accepté mais en attente de vérification (non listé) affiche « Ton message sera visible après vérification. » (`comments.notice.pending_review`) au lieu de disparaître.
  - **Texte conservé** : un envoi refusé remet le texte dans le champ (`CommentListView.restoredDraft`, binding ajouté avec valeur par défaut) ; une réponse ou une modification refusée rouvre l'alerte de saisie avec son texte après l'alerte d'erreur. Les alertes d'erreur sont présentées au tour suivant de la boucle principale (après la fermeture de l'alerte de saisie).
  - **Épinglage** : réservé aux commentaires de premier niveau (route et menu de `CommentItemView`). Un seul `CommentRepository` par écran (`@StateObject`). `CommentSectionType.sharedValue` (code mort) supprimé.
  - **Transport** : la pastille de synchro lit aussi la file transport (`TransportRepositoryBridge.hasPendingTransportSync`, comme l'écran legacy) ; action principale « Organiser le transport » pour l'organisateur d'un événement modifiable, « Voir le transport » sinon (même écran).
  - **Invités** : le libellé VoiceOver d'une carte inclut son groupe (« Nom, En attente »).
  - **Non modifiés** : mentions (`MentionParser` reprend déjà le nom saisi comme identifiant, et l'autocomplétion propose les identifiants des participants, donc `usernameToUserIdMap` vide suffit) ; `onOpenFullScreen` de `ContentView` garde ses deux branches (comportement identique à un seul `fullScreenRoute`, mais `HubModuleSheetViewTests` ancre `EventHubRouting.fullScreenFallback(for: module)`).
  - **Vérifié au simulateur** : envoi de 1 001 emojis refusé sans plantage, texte conservé ; modification trop longue refusée sans plantage, alerte de saisie rouverte avec le texte, section rouverte avec le commentaire intact ; envoi normal affiché. Captures `/tmp/wk-l5c-fix-*.png`.
- **Vérifié au simulateur** (iPhone 18 Pro, données QA) : commentaires repas — envoyer, répondre, modifier, épingler (affiché après correctif), supprimer (avec confirmation), refus de modération sans plantage ; section transport séparée de la section repas ; sheet Transport (aucun plan, départ à compléter comme l'écran legacy) → « Organiser le transport » → écran legacy → retour hub ; sheet Invités → « Inviter » flag éteint (participants legacy) et allumé (audience), retours au hub ; AX5 + sombre (detent large) ; flag refonte éteint → accueil legacy. Captures `/tmp/wk-l5c-*.png`.
- **Non vérifié au simulateur** : vue non-organisateur des Invités (« Plein écran ») et sheet Transport avec plans générés (aucune donnée QA ; couverts par les tests unitaires). Après passage en organisation de l'événement QA confirmé, le lancement avec `--wakeve-qa-seed-invitation-experience` reste bloqué sur le chargement (la graine refuse un statut modifié) : relancer sans les arguments QA, ou désinstaller l'app.

### Couche 6 (Activité, 2026-10-01)

- **Zone Activité** : `ActivityView` remplace `InboxView` sous `iosRedesign2026` (`InboxView`, `InboxViewModel`, `InboxDetailView` inchangés, toujours utilisés par les onglets legacy). Titre « Activité », segments natifs « À traiter (n) » (défaut) · « Tout », une `WKCard` par événement, états chargement / vide (« Rien à traiter pour l'instant » / « Aucune activité pour l'instant ») / échec + « Réessayer », `.refreshable`.
- **Règles pures** (`Models/Activity/ActivityFeed.swift`) : « À traiter » vient de l'état réel, jamais du type de notification — vote manquant (`HomeEventFacts.voteRequired` → écran de vote), prêt à confirmer pour l'organisateur (`readyToConfirm` → résultats), invitation en attente (membre non organisateur, RSVP `PENDING`, événement modifiable → hub). « Tout » ajoute la ligne messages puis les notifications (non lues d'abord, puis récentes). Notifications sans événement connu localement → groupe « Général » (`activity.feed.general`) en fin de liste. Tri : groupes avec action d'abord (échéance la plus proche), puis activité la plus récente, puis titre.
- **Données** (`Services/SharedActivitySource.swift`, hors fil principal, `Task.detached` + annulation) : événements et faits via `SharedEventsHomeSource` (non dupliqué) ; RSVP via `getParticipantRecords` + `ParticipantAccessMapper` ; `notificationQueries.getNotifications(user_id:value_: 50)` (`created_at` en ms, `data` JSON → `eventId` texte ou nombre, lue si `read_at` ou `is_read`) ; nouveaux messages = `commentQueries.countRecentActivityInSection(event_id:section:created_at:)` sur la section générale seulement (celle qu'ouvre la ligne messages), depuis un marqueur local (`UserDefaultsActivitySeenStore`, par utilisateur, `eventId → dernière ouverture des commentaires depuis Activité`, marqueurs de plus de 90 jours supprimés à l'écriture), sinon 7 jours. Le compte ne filtre pas l'auteur : la borne avance jusqu'à son propre dernier commentaire de la même section (`selectLastCommentAtByAuthorInSection`, `MAX(created_at)`, une seule ligne, chaîne exacte conservée), donc ses propres messages ne sont jamais comptés. Messages comptés seulement là où le spectateur peut lire les commentaires (`SharedActivitySource.canReadComments`, même règle que `canAccessDetailedPlanning` : statut confirmé / comparaison / organisation / finalisé, puis organisateur ou participant confirmé via `OrganizationDetailsAccess.isGrantedToParticipant`).
- **Badge** : badge de la barre flottante = « À traiter (n) » (`ActivityViewModel.toDoCount`, actions seulement, §5.4) ; les notifications non lues restent mises en avant (gras, « Non lu ») sous « Tout ». Rechargé à l'entrée dans la zone, quand `eventsHomeReloadToken` ou `eventHubReloadToken` changent, au retour à la liste (`currentView == .eventList`) et au retour au premier plan (`scenePhase == .active`). Le badge legacy garde `unreadInboxCount`. Toucher une notification la marque lue (`markAsRead`), y compris une notification générale sans destination.
- **Navigation** : `AuthenticatedView.openActivityTarget` passe d'abord `redesignRouter.zone = .events` (ce qui ferme les sheets du hub via l'`onChange` de la zone), puis navigue au tour suivant (`Task { @MainActor in … }`) : hub → `openEventFromHome`, vote / résultats → `openHomeAction` (corps extrait de `handleHomeNextStep`, mêmes gardes, sans ou avec rollout invitation), commentaires → section générale + `navigateInvitationDeepLink(destination: .comments)` (mêmes gardes que le lien profond). La ligne messages met le marqueur à jour au tap (même si l'écran affiche ensuite un refus d'accès).
- **Lien profond** : `wakeve://notifications?filter=unread` → « Tout », sinon « À traiter » (`AppRouter.activityFilter` + compteur de demandes `activityFilterRequest`, appliqué même si la vue est déjà montée) ; flag éteint, routeur intact.
- **Correctif** : le point d'action suit Dynamic Type (`@ScaledMetric`), il restait à 8 pt en AX5.
- **Vérifié au simulateur** (iPhone 18 Pro, données QA + notifications et commentaires insérés en base) : « À traiter (1) » avec point rouge ; « Tout » avec notifications, « 3 nouveaux messages » (son propre commentaire exclu) et groupe général ; tap vote → écran de vote ; tap messages → commentaires, retour → ligne disparue ; tap notification → hub, notification marquée lue en base, badge 3 → 2 → 1 ; liens profonds `wakeve://notifications` (→ À traiter) et `?filter=unread` (→ Tout) depuis le hub ; AX5 + sombre ; flag éteint → onglets legacy et Messages inchangés. Captures `/tmp/wk-l6-*.png`.
- **Suite complète** : 980 tests, 5 échecs préexistants seulement (`InvitationExperienceRuntimeSurfaceTests`).
- **Non vérifié au simulateur** : invitation en attente (les données QA n'ont aucun événement dont le spectateur est invité ; couvert par les tests unitaires) et « prêt à confirmer » (aucun autre votant dans les données QA).
- **Correctifs de revue (couche 6)** :
  - **Plantage** : `selectParticipantActivity` regroupe par `(author_id, author_name)` ; un auteur renommé donnait plusieurs lignes et `executeAsOneOrNull()` levait une exception Kotlin non rattrapable. Requêtes dédiées `selectLastCommentAtByAuthorInSection` et `countRecentActivityInSection` (mêmes filtres que `countRecentActivity` : non supprimés, approuvés), testées en `jvmTest` (`CommentActivityQueriesTest`).
  - **Compte** : section générale seulement, borne de son propre commentaire dans la même section ; événements dont les commentaires sont lisibles seulement (sinon la ligne ouvrait un refus d'accès).
  - **Badge** = « À traiter (n) » (`badgeCount` supprimé) et rechargé quand un événement change ailleurs (voir **Badge**).
  - **Divers** : groupe « Général » (`activity.feed.general`) ; libellé du sélecteur « Filtre » (`activity.feed.filter.label`, lu par VoiceOver) ; `RelativeDateTimeFormatter` mis en cache par langue ; marqueurs de consultation de plus de 90 jours supprimés ; commentaires de navigation différée et d'annulation corrigés. Les `deepLink` des notifications ne sont pas encore lus (seul `data.eventId` rattache une notification à un événement).
  - **Vérifié au simulateur** (iPhone 18 Pro, données QA + 2 notifications et 6 commentaires insérés, dont son propre auteur sous deux noms) : pas de plantage ; badge 1 = « À traiter (1) » malgré 2 notifications non lues ; « Tout » : « 2 nouveaux messages » (repas, siens et événement en sondage exclus), notification d'événement, groupe « Général » ; tap messages → commentaires généraux ; vote depuis le hub → retour à la liste → badge 1 → 0 sans repasser par Activité. Captures `/tmp/wk-l6-fix-*.png`.
  - **Tests** : suite complète 986 tests, 5 échecs préexistants seulement (`InvitationExperienceRuntimeSurfaceTests`) ; `:shared:jvmTest --tests "*Comment*"` 12 tests verts.
- **Points ouverts** : le marqueur de consultation reste local (pas d'état de lecture serveur).

### Couche 7 (Création, 2026-10-02)

- **Entrée** : sous `iosRedesign2026`, ＋ (`beginRedesignEventCreation`, aussi appelé par « Créer » de l'accueil vide) suit `CreateFlowEntry.newEventRoute(redesign:invitationRollout:)` : `CreateEventFlow` en plein écran (nouveau `.fullScreenCover(isPresented: $showCreateEventFlow)` placé après la feuille des préférences de notification) quand `iosInvitationExperienceV1` est éteint ; studio d'invitation (`currentView = .eventCreation`, comme avant la couche 7) quand il est allumé (voir Décisions). Lien profond `.eventCreate`, `case .eventCreation`, `CreateEventSheet`, `CreateEventViewModel` et `EventCreationStudioView` inchangés ; flag refonte éteint → ancienne feuille.
- **Écrans** (`Views/Create/`) : une question par écran (`WK.Typo.title`, en-tête VoiceOver), progression en 4 segments (« Étape n sur 4 »), croix de fermeture (`WKCircleButton`), « Brouillon enregistré » discret, bas `WKPrimaryButton` « Continuer » / « Lancer le sondage » + retour ; une seule action principale par écran. **Quoi ?** titre, description, modèles ex-Explorer en `WKChip` (`EventScenario.allScenarios` : préremplit titre/description/type et la checklist, sans écraser un texte saisi), types courants en pastilles + « Autre » avec libellé. **Qui ?** minimum / attendus / maximum optionnels (interrupteur + `Stepper` ≥ 1) et « Tu inviteras ton groupe juste après le lancement » (pas de saisie d'e-mails). **Où ?** lieux saisis ou via `LocationSelectionSheet`, suppression, « Passer » tant que la liste est vide. **Quand ?** créneaux (Journée, Matin 9-12 h, Après-midi 14-18 h, Soir 19-23 h, Heure précise avec début et fin) dans une feuille `Form` native.
- **Validation** (`Models/Create/CreateEventFlowModel.swift`, pure) : règles DRAFT d'AGENTS.md par étape — titre et description non vides, libellé exigé pour « Autre », effectifs ≥ 1, `max ≥ min`, lieux non vides et sans doublon (insensible à la casse, vérifié aussi à l'ajout), au moins un créneau, heure précise avec début < fin. Erreurs affichées sous le champ après un « Continuer » refusé (icône + texte, lues « Erreur : … »), et annoncées à VoiceOver dans l'ordre des champs.
- **Persistance** (`EventDraftFlowController`, machine `EventManagementStateMachine` réelle, règlement sur `ShowToast` puis relecture du dépôt) : étape 1 → `CreateEvent` à la première sauvegarde, puis `UpdateEvent` avec l'événement relu (révision d'agrégat à jour) pour titre/description/type (une écriture ; `UpdateDraftEvent` ne sait pas effacer `eventTypeCustom`) ; étape 2 → `UpdateDraftEvent`, `UpdateEvent` relu pour remettre une valeur à nil ; étape 3 → SQL `potentialLocationQueries` (comme `persistCreationContext`, l'intent `AddPotentialLocation` ne persiste pas) ; étape 4 → `UpdateEvent` relu (`saveEvent` synchronise les créneaux, identifiants logiques conservés) ; lancement → `StartPoll` via `EventPollStartController` (`POLL_REQUIRES_TIME_SLOT_MESSAGE` → « Ajoute au moins un créneau… »). Étape inchangée : aucune écriture. Fermer enregistre l'étape courante si elle est valide et qu'un brouillon existe, ou si c'est une étape 1 valide pas encore continuée (`savesOnClose`) ; rien de saisi → aucun brouillon.
- **Dépôt sans `SyncManager`** pour le flux (comme `CreateEventViewModel`) : avec le gestionnaire de synchro, chaque écriture attendait `triggerSync()` (≈ 7 s de nouvelles tentatives, serveur injoignable) avant son toast. **Les événements créés par le flux restent locaux**, comme ceux de l'ancienne feuille : la ligne `syncMetadata` écrite à la création n'est jamais transportée, et aucune mise à jour (créneaux, type, effectifs, lieux, passage en `POLLING`) n'est envoyée au serveur (Swarm DAO #48).
- **Reprise** : `editDraftFromHome` (menu « Modifier le brouillon » de l'accueil, « Ajouter des dates » du hub avec rollout invitation) rouvre dans le flux un brouillon **sans reçu d'invitation** (`CreateFlowEntry` : aucune ligne `event_operation_receipt`, que le studio écrit à chaque enregistrement et que `CreateEvent` n'écrit jamais ; l'illustration `NONE` existe pour tout événement et ne distingue rien), à l'étape de la première validation en échec ; les brouillons du studio gardent le chemin actuel.
- **Lancement réussi** : checklist du modèle conservée (`persistCreationContext`), zone Événements, hub de l'événement en sondage, accueil et Activité rechargés.
- **Corrections de revue** : seuls les brouillons `TIME_SLOT_POLL` se rouvrent dans le flux (matrice : chemin actuel) ; la reprise garde les créneaux sans date (moment flou conservé, heure précise signalée « à choisir ») et le fuseau d'origine de chaque créneau ; au lancement, une échéance à moins de 7 jours est repoussée à maintenant + 7 jours (`UpdateEvent` relu) avant `StartPoll` ; toasts réglés par rang d'envoi (un toast tardif après délai de garde ne règle pas l'intent suivant, sauf effet déjà visible dans le dépôt) ; création expirée reprise par son identifiant (`event-<UUID>`, réutilisé à la nouvelle tentative : aucun doublon) ; fermer et retour inactifs pendant un enregistrement ou un lancement, lancement ignoré après fermeture ; créneaux comparés sans ordre ; changement de casse d'un lieu enregistré ; « Brouillon enregistré » seulement après un enregistrement réussi ; erreur d'un champ en indice VoiceOver ; clé `create_flow.title` et états publiés inutilisés retirés ; tests du contrôleur nettoyés de la base de l'app hôte.
- **Correctif (simulateur)** : en AX5 le contenu défilait sous la progression et le bas occupait un quart de l'écran → en-tête opaque, « Brouillon enregistré » plafonné (AX1), pas d'icône dans « Lancer le sondage » aux tailles d'accessibilité, ajout de lieu/créneau en pastilles standard.
- **Vérifié au simulateur** (iPhone 18 Pro, base vierge) : erreurs de l'étape 1 ; modèle « Fête d'anniversaire surprise » (titre, description, type) ; effectifs ; lieu ajouté, doublon refusé ; fermeture à l'étape 3 → brouillon à l'accueil (lieu enregistré à la fermeture) ; « Modifier le brouillon » → reprise à Quand ? ; « Lancer » sans créneau refusé ; créneau Soir ajouté → hub « À toi de voter » (base : `POLLING`, créneau `EVENING`) ; AX5 + sombre ; flag invitations allumé → ＋ ouvre le flux (avant la décision du 2026-10-02, voir Décisions) ; flag refonte éteint → ancienne feuille. Captures `/tmp/wk-l7-*.png`.
- **Non vérifié au simulateur** : erreur `max < min` et heure précise avec fin avant le début (couvertes par les tests) ; recherche de lieu (`LocationSelectionSheet`) ; VoiceOver réel.
- **Points ouverts** : la checklist du modèle choisi n'est pas persistée avec le brouillon : un brouillon fermé puis repris garde son titre, sa description et son type, mais plus la checklist préparée (elle n'est remise à l'événement que si le lancement se fait dans la même session) ; avec le rollout invitation éteint, « Ajouter des dates » du hub d'un brouillon ouvre toujours l'écran des participants legacy (aiguillage `EventHubRouting` inchangé) ; les événements du flux ne sont pas synchronisés (Swarm DAO #48).
- **Tests** : 53 nouveaux tests (modèle 18, textes 4, contrôleur 13, vues 12, branchement 6, sur base réelle pour le contrôleur et le marqueur studio) ; suite complète 1 039 tests, 5 échecs préexistants seulement (`InvitationExperienceRuntimeSurfaceTests`).

### Couche 8 (Immersif, 2026-10-02)

- **Jetons** (`WK.Mood`) : fond = couleur secondaire sombre de la palette (`darkSecondaryHex`) au lieu de la primaire assombrie de 72 % (luminance ≈ 0,001-0,002, presque noire) ; `surface` blanc 6 %, `textPrimary` #FFFFFF, `textSecondary` #C9D3D1, `pillStroke` blanc 45 %, CTA `cta` #FFFFFF / `onCta` #1C1C1E. Contrastes (texte principal / secondaire sur le fond ; sur `surface`) : Soirée 14,5 / 9,5 ; 12,1 / 7,9 — Voyage 13,0 / 8,5 ; 10,9 / 7,1 — Anniversaire 10,6 / 6,9 ; 8,9 / 5,8 — Famille 11,1 / 7,2 ; 9,3 / 6,1 — Dîner 11,7 / 7,6 ; 9,8 / 6,4 — Plage 8,8 / 5,7 ; 7,5 / 4,9 — Week-end 12,8 / 8,4 ; 10,7 / 7,0. Contour des pastilles ≥ 3,1:1, CTA 17:1 (texte) et ≥ 8,7:1 sur le fond. Tests : fond ≥ 0,02 de luminance, fonds distincts (≥ 12/255 sur un canal), seuils 7:1 / 4,5:1 sur le fond **et** sur `surface`.
- **Composants** (`Components/WK/WKImmersive.swift`) : `WKImmersiveScaffold` (fond plein écran, contenu défilant, croix `WKCircleButton`, CTA blanc + action secondaire à contour, rendu toujours sombre ; actions plafonnées à AX2), `WKImmersiveButton`, `WKImmersivePill`, `WKImmersiveCard` (contour sous Increase Contrast). Galerie DEBUG : chaque ambiance, AX5, Reduce Transparency, Increase Contrast.
- **Invitation reçue** (`Views/Immersive/InvitationLandingView.swift`) : sous `iosRedesign2026`, `case .eventDetail` affiche `InvitationLandingContainer` au lieu du hub quand `invitationLandingEventId == event.id` (posé par `resolveInvitationDeepLink`, inchangé). Contenu : illustration de l'invitation (`InvitationArtworkView`, sauf `Artwork.None`), « Invitation de <organisateur> » (« Invitation » si inconnu ou pour l'organisateur lui-même), titre, date retenue ou « Vote en cours · N créneaux », invités (règle du hub), état de réponse affiché seulement (« Tu as accepté », « Ta réponse est attendue », « Tu as décliné », « Tu organises »). Action : « Voter » si `EventHubModel.primary == .vote` (route du hub), sinon « Voir l'événement » qui efface le marqueur (hub) ; la croix revient à la liste. Le `onBack` du hub et le détail legacy sont inchangés.
- **Jour J** : règle pure `EventDayRule.isEventDay` = organisation (ou finalisé **sans** rollout invitation) ∧ accès aux détails ∧ (aujourd'hui = jour du créneau retenu dans son fuseau ∨ maintenant ∈ [début, fin[). Source `SharedEventDaySource` (hors fil principal) : créneau `confirmedDate.selectWithTimeslotDetails` + `timeSlot.timeOfDay`, lieu = scénario retenu (`getSelectedScenario`) sinon premier lieu potentiel, coordonnées du JSON `potentialLocation.coordinates` (lieu source du scénario ou même nom ; `EventDayPlaceResolver`, sans toucher `EventWeatherMapCard`), transport (`HubModuleSheetData.transportState`), repas du jour (`getMealsByDate`, non annulés), invités (règle du hub). Vue `EventDayView` : « C'est aujourd'hui », heure en `WK.Typo.display` dans le fuseau du créneau (« Toute la journée » pour un créneau journée), titre, date et « jusqu'à », carte MapKit compacte non interactive si coordonnées, pastilles (transport, repas, invités), « Itinéraire » (`MKMapItem.openInMaps` avec coordonnées, sinon `maps://?daddr=<nom, adresse>`) et « Voir l'événement » (ferme et ouvre le hub par `openEventFromHome`). Entrées : bannière « C'est aujourd'hui » dans le hero du hub ; `HomeNextStep.eventDay` (« Voir le jour J », grand chiffre « aujourd'hui »), juste après voter / choisir la date, même si le créneau du jour est terminé. Plein écran unique `.fullScreenCover(item: $eventDayPresentation)` (refonte seulement).
- **QA (DEBUG)** : `--wakeve-qa-open-invitation-landing <eventId>` ouvre l'invitation reçue comme après `resolveInvitationDeepLink` (le serveur local qui résout `wakeve://invite/<token>` est injoignable en QA).
- **Correctifs (simulateur)** : en AX5, les deux actions du bas occupaient un tiers de l'écran → plafonnées à AX2 ; l'organisateur ouvrant son propre lien lisait « Invitation de » son propre nom → « Invitation ».
- **Vérifié au simulateur** (iPhone 18 Pro, données QA, événement passé en organisation au 2 oct. 19:30-23:00 Paris avec lieu à coordonnées et dîner du jour insérés en base) : bannière du hub → jour J (ambiance Anniversaire, carte, pastilles « Dîner au bord du lac · 20:00 » et « 1 confirmé ») ; « Itinéraire » ouvre Plans ; « Voir l'événement » → hub ; accueil : « Voter » reste prioritaire, puis « aujourd'hui · C'est le jour J · Voir le jour J » ; invitation reçue (ambiances Week-end et Anniversaire, illustration) : « Voter » → écran de vote, « Voir l'événement » → hub ; AX5 (après correctif) ; Reduce Transparency (croix opaque) ; appareil en clair (immersif toujours sombre) ; flag refonte éteint → détail legacy inchangé. Captures `/tmp/wk-l8-*.png`.
- **Non vérifié au simulateur** : lien `wakeve://invite/<token>` réel (résolution serveur), recherche Plans par nom (lieu sans coordonnées), pastille transport (aucun plan en QA), VoiceOver réel.
- **Revue (2026-10-02)** : marqueur `invitationLandingEventId` effacé quand la navigation quitte le détail sans l'afficher (second `.onChange(of: currentView)`) et à l'ouverture d'un autre événement (`openEventFromHome`, `openHomeAction`, `openEventDay`) ; règle pure `InvitationLandingRoute` (jamais d'invitation reçue pour l'organisateur : il voit le hub, qui efface son marqueur) ; la source ne renvoie pas le nom de l'organisateur au spectateur organisateur. Jour J : `EventDayFacts.finalDate` (`event.finalDate`) en repli de date ; créneau sans début → moment de la journée (« Matin », clés `create_flow.moment.*`) au lieu d'une heure ; repas = jour de *maintenant* dans le fuseau du créneau (deuxième jour d'un créneau sur deux jours). Légende fr « Organisé par <nom> » (plus d'élision « Invitation de Anaïs ») ; identifiants de pastilles uniques ; bannière du hub recalculée chaque minute (`TimelineView(.everyMinute)`) ; Increase Contrast : `pillStroke` blanc opaque, `textSecondary` #F2F5F4 (≥ 7:1 sur le fond, ≥ 6,5:1 sur `surface`) ; accent ≥ 3:1 sur `surface` (testé) ; Reduce Transparency : croix sur `mood.closeFill` (fond de l'ambiance + 18 % de blanc, opaque, glyphe ≥ 4,5:1) ; `InvitationLandingContainer` en `.preferredColorScheme(.dark)` ; code mort retiré (`EventDayFacts.isEventDay`, `EventDayPlace.hasCoordinate`). Seed QA : `qa-invitation-received` (« Anniversaire de Noé », sondage, organisé par Noé Bernard, réponse du spectateur en attente). Vérifié au simulateur (iPhone 18 Pro) : invité → « Organisé par Noé Bernard », « Ta réponse est attendue », « Voir l'événement » ; organisateur avec l'argument d'invitation → hub ; créneau Matin du jour → bannière puis jour J « 09:00 · jusqu'à 12:00 ». Constat : un créneau flexible du flux de création a des heures nominales (9-12, 14-18, 19-23) et `confirmDate` exige un début, donc le repli « Matin » ne s'affiche que pour des données sans début (défensif) ; afficher le moment plutôt que l'heure nominale reste à décider. Suite complète 1 131 tests, 5 échecs préexistants (`InvitationExperienceRuntimeSurfaceTests`).
- **Points ouverts** : Accepter/Décliner depuis l'invitation (aucune API client, avec la synchro #48) ; présence en direct, « Je suis en route », covoiturage détaillé, notifications du jour J ; jour J d'un événement finalisé avec le rollout invitation (archives).
- **Tests** : nouveaux tests de jetons (fond teinté, fonds distincts, contrastes sur `surface`, contour, CTA), composants (`WKImmersiveTests`), invitation (`InvitationLandingTests`), règle et lieu du jour J (`EventDayRuleTests`), vue et entrées du jour J (`EventDayViewTests`), branchement (`ImmersiveWiringTests`) ; suite complète 1 117 tests, 5 échecs préexistants seulement (`InvitationExperienceRuntimeSurfaceTests`).

### Couche 9 (Bascule et suppression du legacy, 2026-10-02)

- **Bascule** : flag `iosRedesign2026` supprimé (`FeatureFlags`, `@AppStorage`, réinitialisations `onChange`, toutes les branches « flag éteint » d'`AuthenticatedView`) ; `AppRouter.preRoute(_:router:)` sans paramètre de flag ; le lien `wakeve://event/create` suit la route de ＋ (`beginRedesignEventCreation` : flux 4 questions, ou studio sous le rollout invitation) ; `CreateFlowEntry.newEventRoute(invitationRollout:)` perd `.legacySheet`. `iosInvitationExperienceV1` inchangé. **BREAKING** : shell à onglets legacy et flag supprimés.
- **Supprimé** (≈ 26 100 lignes retirées sous `iosApp/`, dont ≈ 22 200 dans `src/`) : shell à onglets (`legacyTabChrome`, `WakeveTab`, `selectedTab`, cas `AppView` `.inbox`/`.notifications`/`.notificationPreferences`/`.settings`), ancien accueil (`EventListView`, `EventCard`), bibliothèque d'invitations (`EventLibraryView`…), Explorer, Inbox legacy (≈ 5 400 lignes) ; ancien détail `EventDetailView` + canvas d'invitation, `EventNextAction`, carte météo, override QA de transparence (≈ 7 600) ; `CreateEventSheet`, `CreateEventViewModel` et son brouillon IA, `EventInfoSheet`, `BackgroundPickerSheet` (≈ 4 300) ; composants et jetons morts (`AIBadgeView` + `AISuggestionModels`, `LiquidGlassTextField`, `LiquidGlassAnimations`, `AddToCalendarButton`, `GlassBadge`, `EventListRow`, `ParticipantAvatarStack`, `BottomSheet`, `LoadingSkeleton`, `WakeveEventPanel`, `WakeveListRow`…, composants de `SharedComponents` inutilisés, `IconographyGuidelines`, structs de premier niveau `AdaptiveColors`/`Typography`/`Spacing`/`CornerRadius`/`Shadows`, statics `Color.wakeve*`/`app*`/`iOS*` et membres `WakeveTheme` inutilisés, `ProfileViewModel`, `ScenarioDetailViewModel`, `LocalizationService`, `EventDraftGenerator`, `preparedChecklist` écrit seul, feuille des préférences de notification jamais ouverte) (≈ 4 200) ; 852 clés de traduction inutilisées par langue (2 305 → 1 453, 5 langues ; préfixes construits dynamiquement conservés) (≈ 4 500). Déplacés dans leurs propres fichiers : `InvitationArtworkView`, identifiants d'accessibilité d'invitation, `EventScenario` (`Models/Create/`), `EventCreationContext`, `EventSlotInputBuilder`/`EventTimeSlotFactory`, `EventWeatherSummary`/`EventWeatherPlace`.
- **Pertes assumées (BREAKING)** : avec l'ancienne feuille de création disparaissent la création en mode « matrice de scénarios » hors studio et le **brouillon IA à la création** (smart draft) ; à reprendre plus tard dans le flux si besoin. Le client IA garde `generateEventDraft` (frontière IA testée) ; seule la suggestion transport reste branchée à l'UI.
- **Garde-fou** : baselines `Views/` abaissées aux valeurs mesurées — `Color(hex:` 14 (65), `.font(.system(size:` 48 (87), `cornerRadius` littéral 44 (68), `.cornerRadius(` 3 (13), `Color(red:` 6 (6). Non nulles car les écrans plein écran de repli conservés (vote, résultats, participants, scénarios, transport, budget, réunions, profil, réglages…) utilisent encore l'ancien design system ; le passage à 0 devient un chantier de restylage séparé.
- **Navigation** : `wakeve://leaderboard` ne piège plus l'utilisateur (`RedesignBackRoute` : `.leaderboard` → `.eventList`, bouton retour en incrustation). `wakeve://settings` ouvre la feuille **préférences de notification** du shell (`AppRouter.presentation = .settings`) et non plus le profil ; `wakeve://profile` et l'avatar ouvrent le profil ; `wakeve://notifications` ouvre la zone Activité.
- **Vérifié au simulateur** (iPhone 18 Pro, app désinstallée puis réinstallée) : lancement sans argument (nouvel accueil) ; lancement QA amorcé (`--wakeve-qa-seed-invitation-experience --wakeve-qa-open-invitation-route library`) : accueil, hub « Week-end confirmé », sheet Invités, Activité, studio (rollout allumé par l'amorçage), flux 4 questions et ses erreurs (rollout éteint), écran de vote (repli plein écran), profil, réglages (préférences de notification) ; AX5 + sombre sur l'accueil et la création. Aucune clé brute visible. Captures `/tmp/wk-l9c-*.png`.
- **Tests** : tests ne protégeant que du code supprimé supprimés (listés dans chaque message de commit), tests de comportement conservé réancrés ; suite complète 1 003 tests, **2 échecs préexistants** seulement (`InvitationExperienceRuntimeSurfaceTests.testStudioArtworkChoicesUseAdaptiveWidthAtStandardAndAccessibilityDynamicType` et `.testArchiveRendersRepositoryFreshnessSyncWarningsAndNoMutationControls` ; les 3 autres des 5 échecs connus visaient la bibliothèque supprimée).
- **Problèmes connus restants** : les 2 échecs ci-dessus ; l'amorçage QA n'agit qu'avec l'argument `--wakeve-qa-open-invitation-route` (sans lui, pas de route) et, juste après une installation ou avec `--wakeve-debug-authenticated`, l'accueil peut rester longtemps (plus de 50 s observées) sur l'indicateur de chargement QA (`invitationQALibraryIsSeedReady`) — relancer l'app ; l'amorçage active durablement `iosInvitationExperienceV1` ; l'ancien design system reste utilisé par les écrans de repli ; `EventCreationContext.potentialLocationName` est toujours nil depuis le flux (donc `persistCreationContext` n'écrit rien) et `sourceScenario` n'est pas lu ; `WakeveAIClientFactory` / client Foundation Models ne sont plus atteints par l'UI (gardés comme frontière IA) ; aux tailles AX5 la barre flottante recouvre le bas du contenu de l'accueil vide.

### Décisions

- **＋ et rollout invitation** (décidé le 2026-10-02, remplace « ＋ ouvre toujours le flux ») : sous la refonte, ＋ ouvre `CreateEventFlow` **seulement** quand `iosInvitationExperienceV1` est éteint ; allumé, le studio reste le point d'entrée (`CreateFlowEntry.newEventRoute`). Raison : hors studio, aucun chemin client ne transporte au serveur l'événement créé ni ses mises à jour (ligne `syncMetadata` jamais envoyée, `SyncEventData` limité à titre/description/échéance) : un événement du flux n'aurait ni lien de partage ni votes distants opérants, alors que le studio passe par son outbox synchronisée. Le flux deviendra l'entrée unique quand la proposition **Swarm DAO #48** (« synchroniser les brouillons et le lancement du sondage créés hors studio ») sera livrée. `editDraftFromHome` continue de rouvrir dans le flux les brouillons sans reçu d'invitation (créés rollout éteint).
- **Registre du français : tutoiement** (décidé le 2026-09-28). Toute nouvelle chaîne `fr` tutoie ; les chaînes existantes qui vouvoient (`fr.lproj/Localizable.strings`) sont à convertir.

### Points ouverts

- **Increase Contrast** : `textMuted` n'a pas encore de variante haut contraste.
- ~~**Clés orphelines** : ~32 clés `home.*` laissées par la suppression de `HomeView`~~ — nettoyées en couche 9 (852 clés par langue).
