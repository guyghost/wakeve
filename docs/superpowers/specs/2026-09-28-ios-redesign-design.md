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
| ORGANIZING | Transport, Hébergement, Repas, Matériel, Activités, Budget, Réunions |
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

Tout est derrière un flag `redesign2026` lu via un `FeatureFlags` unique (remplace `iosInvitationExperienceV1`). Chaque couche est une PR indépendante et livrable.

| # | Couche | Contenu |
|---|---|---|
| 0 | Nettoyage | Suppression du code mort (`HomeView`, `EventDetailExperienceView`, `LiquidGlassTabBar`) |
| 1 | Socle | `WK` tokens + composants + previews + garde-fou `WKStyleGuardTests` (cliquet) |
| 2 | Shell | Shell progressif : `AppRouter` (plan de routage + présentation unique profil/réglages), `RedesignShellView` + `WKFloatingNavBar`, zones Événements (aiguillage `AppView` existant) et Activité (`InboxView`), derrière `iosRedesign2026` |
| 3 | Accueil | `EventsHomeView` |
| 4 | Hub | `EventHubView` + `WKModuleTile` (modules ouvrent encore les anciens écrans) |
| 5 | Modules | Conversion en `WKModuleSheet`, un module par PR : Transport → Repas → Matériel → Activités → Hébergement → Budget → Paiement → Photos → Commentaires → Invités → Réunions |
| 6 | Activité | `ActivityView` (fusion Inbox + messages) |
| 7 | Création | `CreateEventFlow` |
| 8 | Immersif | `WKImmersiveScaffold` : invitation, jour J |
| 9 | Bascule | Flag activé par défaut ; suppression du `switch` de `ContentView`, de l'ancien design system et des écrans remplacés ; baselines du garde-fou ramenées à 0 |

Garde-fou : `WKStyleGuardTests` (XCTest, SwiftLint n'étant pas installé) — zéro style en dur dans `Components/WK`, compteurs de `Views/` à cliquet (ne peuvent que baisser), baseline ramenée à 0 à la couche 9. Périmètre : `Views/` et `Components/WK` uniquement. `WKModuleSheet` arrive en couche 5, `WKImmersiveScaffold` en couche 8.

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
4. Aucune occurrence de `Color(hex:`, `.font(.system(size:` ou `cornerRadius` littéral dans `Views/` (`WKStyleGuardTests` avec baseline 0).
5. Un seul fichier de tokens ; les anciens fichiers de couleurs sont supprimés.
6. Tous les écrans passent Dynamic Type AX5 sans troncature bloquante et VoiceOver sans élément non libellé.
7. Tests unitaires, UI et offline verts.

## 12. Métriques de succès

- Taps moyens pour voter depuis l'ouverture de l'app : cible ≤ 3.
- Taux de complétion du flux de création (DRAFT → POLLING) : en hausse vs l'actuel.
- Nombre de vues SwiftUI de premier niveau par événement : ~15 → 1 hub + sheets.

## 13. Rollback

- Le flag `redesign2026` permet de revenir à l'ancienne UI jusqu'à la couche 9.
- Après la couche 9, rollback par revert de la PR de bascule (les couches précédentes restent inertes sans le flag).

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
- Modules par phase : brouillon date/lieu/invités ; sondage + budget ; confirmé et comparaison date/scénarios/invités/budget ; organisation transport/hébergement/repas/matériel/activités/budget/réunions ; finalisé récap/photos/paiements. Au plus une tuile mise en évidence (date pour vote/résultats/confirmation/ajout de dates, scénarios en comparaison).
- Vote rapide Oui/Peut-être/Non : ouvre `PollVotingView` (aucune soumission depuis le hub, le journal de bulletins n'accepte que des bulletins complets). La carte n'apparaît que si un créneau est en tête (`leadingSlotStart`), sinon seul le CTA « Voter » reste.
- Transitions confirmé → organisation et organisation → finalisé : dans le hub, toujours après confirmation (`confirmationDialog`), via `EventLifecycleTransitionController` possédé par le conteneur (même chemin que `EventDetailView`) ; succès → relecture de l'événement et `eventHubReloadToken += 1`. Invité local : « Connecte-toi pour finaliser » reprend `authStateManager.signOut()`.
- Règle « prêt à confirmer » partagée : `PollReadiness.readyToConfirm` est utilisée par `HomeEventFacts` et `EventHubFacts` (pas de copie) ; elle l'emporte sur le vote de l'organisateur, comme sur l'accueil.
- Menu « … » en `confirmationDialog` : Infos de l'événement, Ajouter des participants (organisateur, non finalisé), Signaler (non-organisateur, `ModerationActionSheet`), Contacter le support (mailto existant).
- Hero teinté par le type d'événement : `WKCard(tint:)` avec `WK.Tint.surface` (0,12) sur `EventMoodPalette` ; texte principal conservé pour le contraste. Teinte discrète en pratique (palette par défaut presque neutre).
- Verrous alignés sur les gardes réelles des `case` de destination, pour qu'aucune tuile ouverte ne mène à `AccessDenied` : budget, réunions, paiements = `canAccessOrganizationDashboard` (organisation/finalisé + accès) — le budget est donc verrouillé en sondage, confirmé et comparaison ; transport = confirmé/organisation/finalisé + accès (pas la comparaison) ; hébergement, repas, matériel, activités, photos = `canAccessDetailedPlanning` (confirmé/comparaison/organisation/finalisé + accès) ; date, lieu, invités, scénarios, récap jamais verrouillés.
- Aiguillage pur et testé `EventHubRouting`, dépendant du rollout `iosInvitationExperienceV1`. Rollout actif : invités et « Ajouter des participants » → routeur invitation (`EventAudienceView`), dates d'un brouillon → studio (`editDraftFromHome`), récap et Infos → `EventInformationView`. Rollout éteint (défaut ; ces écrans retombaient sur le hub, donc sans effet) : invités, « Ajouter des participants », « Ajouter des dates » et la tuile date d'un brouillon → `ParticipantManagementView` (liste, partage d'invitation, `DraftDatesSheet`, lancement du sondage) ; récap → `PollResultsView` (date et horaires retenus, écran sans garde) ; l'entrée « Infos » est masquée.
- Retour hors racine : la barre flottante étant masquée hors racine, les écrans sans retour propre atteints depuis le hub (budget, dépenses, réunions, détail de réunion, cagnotte, Tricount, « Invités et invitations ») reçoivent un `WKCircleButton` « Retour » (`RedesignBackRoute`, fonction pure) vers le hub, ou vers l'écran parent (dépenses → budget, réunion → réunions, Tricount → cagnotte). Rangée en `safeAreaInset` du shell, sauf pour le budget et les réunions qui poussent un détail dans leur propre pile : le bouton y est lu dans leur barre d'outils (`\.redesignBackAction`) pour que le retour système le remplace, sans superposition. Rien sur les écrans `AccessDenied`, qui ont déjà leur retour. Les `case` legacy ne sont pas modifiés ; flag éteint, `redesignBackAction` vaut nil. Corrige le défaut « Invités et invitations sans bouton retour » relevé en couche 3 (sous flag).
- Défauts repérés hors couche : `PollResultsView` ne reconnaît que `CONFIRMED` comme date retenue — en organisation/finalisé, il présente le « meilleur créneau » avec l'annonce « je le confirme » (touche la tuile date et, sans rollout, le récap) ; clé `accommodation.empty.title` non traduite ; « 1 confirmés » (format non pluriel réutilisé) ; « À préparer » comme indice du récap d'un événement finalisé ; en AX5 le contenu du hub défile sous les boutons ronds de la barre haute (sans fond).

### Décisions

- **Registre du français : tutoiement** (décidé le 2026-09-28). Toute nouvelle chaîne `fr` tutoie ; les chaînes existantes qui vouvoient (`fr.lproj/Localizable.strings`) sont à convertir.

### Points ouverts

- **Increase Contrast** : `textMuted` n'a pas encore de variante haut contraste.
- **Mood immersif** : le fond (palette sombre assombrie de 72 %) est presque noir ; facteur à revoir en couche 8.
- **Clés orphelines** : ~32 clés `home.*` laissées par la suppression de `HomeView` ; nettoyage en couche 9.
