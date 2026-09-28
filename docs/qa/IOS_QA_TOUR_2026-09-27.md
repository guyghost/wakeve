# QA iOS — Tour de l'app + 4 scénarios multi-utilisateurs (2026-09-27)

**Environnement** : build Debug `WakeveApp` (xcodebuild OK, 0 warning) · simulateurs iPhone 18 Pro (iOS 27) + Wakeve-QA-iPhone-16-Pro (iOS 26.5) · serveur Ktor local `:8080` · install propre (désinstallation préalable).

**Méthode**
- **UI iOS** : parcours manuel de l'onboarding, des 4 onglets, du wizard de création, du vote, des résultats, de la confirmation et de la section Organisation (captures dans `evidence/ios-2026-09-27/`).
- **Multi-utilisateurs** : 22 comptes invités sur l'API réelle (REST + `/api/sync`), 4 scénarios de DRAFT à FINALIZED → [API_MULTIUSER_QA_2026-09-27.md](API_MULTIUSER_QA_2026-09-27.md) (**346 ✅ / 39 ❌**, scripts dans `evidence/api-2026-09-27/`).

> ⚠️ **Le multi-utilisateur n'est pas testable dans l'app iOS en mode invité** : `AuthStateManager.continueAsGuest()` crée une session *locale uniquement* (`guest-<uuid>`, sans jeton serveur). L'événement reste en `syncMetadata synced=0` et n'atteint jamais le serveur ; un lien `wakeve://invite/…` ouvert sur un 2ᵉ appareil retombe silencieusement sur la liste vide (`GET /api/invite/<token>` → 404). Seul Sign in with Apple permet la collaboration réelle.

## Verdict

L'app **n'est pas prête** pour un parcours de bout en bout sur iOS. Le design est soigné et cohérent (Liquid Glass, aperçu d'invitation, vote une question à la fois, résultats avec message à partager). En revanche, plusieurs ruptures bloquent la progression après la confirmation de la date.

| Scénario | Jusqu'où va l'UI iOS | Bloqué par |
|---|---|---|
| Road trip Lisbonne | Création → sondage → vote → confirmation → option créée/votée | IOS-1 (option finale), IOS-2 (pas d'ORGANIZING) |
| Watch party finale LDC | Modèle Explorer → wizard → aperçu | IOS-3 (création sans date bloquée) |
| Mariage Sophie & Karim | Couvert via API uniquement (le wizard n'a aucun sélecteur de type, donc pas de type « Mariage » hors modèle) | IOS-2, API P1 |
| Balade Fontainebleau | Création avec 1 date → sondage imposé | IOS-2, API-I |

---

## ✅ Mise à jour 2026-09-28 — bloquants P0 corrigés

Les 3 bloquants ont été corrigés en TDD, puis vérifiés dans l'app sur simulateur (parcours complet DRAFT → POLLING → CONFIRMED → option finale → ORGANIZING → finalisation refusée avec explication).

| Bloquant | Correctif | Vérification |
|---|---|---|
| IOS-1 | `updateEventStatusInternal` est désormais transactionnel. Une date déjà confirmée par le sondage est conservée et n'est plus réinsérée. Un id de sync unique est utilisé quand un statut est atteint une 2ᵉ fois. Enfin, une auto-réparation supprime les autorisations `event-status:` orphelines laissées par l'ancien writer. | `ScenarioFinalSelectionStatusRedTest` (5 tests). Dans l'app : option retenue sans erreur, 0 autorisation orpheline, et l'événement « Road trip Lisbonne » bloqué par l'ancien bug a été débloqué. |
| IOS-2 | Nouveau `EventLifecycleTransitionController`, qui dispatche `TransitionToOrganizing` et `MarkAsFinalized`. Une carte organisateur dans le détail propose « Passer à l'organisation » puis « Finaliser l'événement », avec confirmation. Les blocages de finalisation sont traduits (`EventLifecycleBlockerFormatter`). Après l'option finale, l'app ramène au détail au lieu d'ouvrir Réunions, encore verrouillées. | `QABlockersRegressionTests`. Dans l'app : ORGANIZING atteint, Réunions, Budget, Cagnotte et Tricount visibles, finalisation refusée avec « Il reste à régler : … ». |
| IOS-3 | Décision produit : **brouillon sans date autorisé**. `CreateEventUseCase` accepte un DRAFT sans créneau. Le repository et `StartPoll` refusent POLLING sans créneau (`POLL_REQUIRES_TIME_SLOT_MESSAGE`), ce qui corrige aussi le P2 API « sondage sans créneau ». L'écran Participants propose « Ajouter des dates » (`DraftDatesSheet`). Le ViewModel de création ne peut plus rester bloqué : l'échec est détecté même quand l'état Kotlin est identique au précédent. | `DraftWithoutDatesRedTest`, `CreateEventUseCaseTest`, `EventManagementStateMachineEdgeCasesTest`, `QABlockersRegressionTests`. Dans l'app : brouillon créé sans spinner, dates ajoutées, sondage lancé. |

Points découverts pendant la correction, non traités ici :
- **IOS-4 confirmé** : le vote et les résultats affichent « 00:00 - 00:00 » pour un créneau jour entier. Le wizard enregistre aussi l'heure courante comme début d'un « jour entier ».
- **Finalisation d'un invité local impossible** : la readiness exige des participants confirmés et une synchro convergée (`CRITICAL_SYNC_PENDING`). Un invité local n'est jamais synchronisé, donc ne peut jamais finaliser. C'est une décision produit à prendre.
- **Android** n'expose pas non plus `TransitionToOrganizing` ni `MarkAsFinalized` : même trou fonctionnel que IOS-2.

## 🔴 P0 — Bloquants iOS

### IOS-1 — « Retenir cette option » échoue et laisse un état incohérent
Affiche « Failed to update event status » (en anglais). Côté base : scénario `SELECTED` et event passé à `CONFIRMED`, alors que l'utilisateur voit une erreur. Une ligne `aggregate_write_authorization` reste orpheline.
**Cause** : `DatabaseEventRepository.updateEventStatusInternal` (`shared/…/repository/DatabaseEventRepository.kt:1103-1115`) ré-insère `confirmedDate` avec l'id fixe `confirmed_<eventId>`, déjà créé lors de la confirmation de date. Le UNIQUE lève une exception. L'`UPDATE` du statut a déjà été exécuté, sans transaction.
**Repro** : créer → sondage → confirmer date → Transport/Options → créer option → « Retenir cette option ».

### IOS-2 — Les phases ORGANIZING et FINALIZED sont inatteignables sur iOS
Aucun code Swift ne dispatche `TransitionToOrganizing` ni `MarkAsFinalized` : seuls `StartPoll`, `SubmitConfirmation`, … sont utilisés. Or Réunions, Budget, Cagnotte et Tricount sont conditionnés à `ORGANIZING/FINALIZED` (`ContentView.swift:991,1015`). Toute la fin du parcours est donc invisible sur iOS.

### IOS-3 — Création sans créneau (« Date à décider avec le groupe ») : spinner infini, puis wizard verrouillé
Le wizard propose explicitement ce chemin, mais `CreateEventUseCase` le rejette (« At least one time slot is required », `CreateEventUseCase.kt:73`). `CreateEventViewModel.onStateDidChange` ne remet jamais `isCreating` à false dans ce cas, d'où un spinner infini. Au retour, « Voir l'aperçu » et « × » restent désactivés : seul un kill de l'app en sort. Ce bug était déjà signalé dans la QA du 2026-06-30 et n'est toujours pas corrigé.

## 🟠 P1

- **IOS-4 — Vote/résultats ignorent « Jour entier »** : un créneau `timeOfDay=ALL_DAY` s'affiche « 23:05 – 00:05 » dans le vote, les résultats et le message à partager (l'aperçu, lui, dit « Jour entier »). De plus, l'heure par défaut d'un créneau est « maintenant » (23:05) plutôt qu'une heure utile.
- **IOS-5 — La liste « À venir » affiche le `toString()` Kotlin** : « Event(id=event-1790…, title=Road trip Lisbonne, descri… ». La cause est `Text(event.description)` au lieu de `event.description_` (`ContentView.swift:2849-2850`). Même erreur dans `ExploreTabView.swift:357,446`.
- **IOS-6 — Chaînes non traduites dans l'app FR** :
  - 12 clés `String(localized:)` absentes de **tous** les `Localizable.strings` : `common.create` (visible sur le bouton du formulaire d'option), `events.status.comparing`, `participants.count_format`, `poll.voting.closed`, `accommodation.empty.title`, `navigation.placeholder.select_event_*`…
  - Les toasts et erreurs des state machines partagées sont codés en dur en anglais (« Scenario created successfully », « Vote submitted successfully », « Failed to update event status »).
- **IOS-7 — Commentaires : l'envoi ne fait rien** (0 ligne `comment` en base, champ vidé sans message). `EventCommentsRouteView` ne passe pas `onAddComment` (`EventSecondaryRouteViews.swift:342-368`).
- **IOS-8 — Invité sans nom** : l'invitation affiche « Organisé par **Invité** » et le Profil ne permet pas de saisir un nom.
- **IOS-9 — Repas : le coût, présenté comme facultatif, est obligatoire** (« Le coût doit être un nombre positif » si vide).
- **IOS-10 — Le détail ne se rafraîchit pas après confirmation** : il affiche toujours « Vote en cours » jusqu'à ce qu'on ressorte et rouvre l'événement.

## 🟡 P2 — UX / design

- Onboarding écran 2 : la liste de bénéfices est coupée et les indicateurs de page sont masqués.
- Wizard :
  - fermer « × » avec des données saisies ne demande aucune confirmation ;
  - le « × » est quasi invisible (blanc sur blanc) ;
  - le placeholder du titre est tronqué (« Titre de ») ;
  - il n'y a pas de sélecteur de type d'événement : les événements créés à la main sont en `OTHER` ;
  - les lignes du récapitulatif (Nom, Créneaux…) ont un chevron mais ne réagissent pas au tap ;
  - la question de l'étape Invités est affichée deux fois ;
  - le calendrier autorise les dates passées ;
  - la recherche de lieu (MapKit) n'affiche ni résultats ni état vide.
- Toggle « Jour entier » : la zone tactile est décalée (`.frame(width: 48, height: 28)`, `CreateEventSheet.swift:1958`). Plusieurs taps restent sans effet.
- Formulaire « Option » :
  - la date s'affiche en ISO brut (« 2026-10-17T21:05:00Z », aussi dans Transport) ;
  - la durée saisie « 1 jour » devient « 1 nuit(s) » ;
  - le titre est tronqué (« Créer une… »).
- Égalité de score (2–2) : un créneau est marqué « Option préférée » sans mention d'égalité.
- Carte de la liste : la date affichée est la date limite du vote (04/10) et non la date retenue (17 oct.).
- Menu « … » → « Ajouter des participants » sans effet visible. Le routage passe par `routeInvitationExperience`, qui ne renvoie que vers `.eventDetail` quand le flag `iosInvitationExperienceV1` est désactivé (défaut) ; même comportement pour la ligne « Invitation » d'Organisation.
- Retour d'un sous-écran : la position de défilement du détail est perdue.
- Chaque vote d'option déclenche une alerte modale à fermer (un toast suffirait).
- Commentaires : le titre « Messages » est affiché deux fois.
- Une date unique déjà connue (balade) impose tout de même un sondage : aucun raccourci « confirmer directement ».

## ✅ Ce qui fonctionne bien
- Build, lancement, onboarding et connexion invité, sans crash sur toute la session.
- Wizard en 5 étapes, clair et progressif. Aperçu d'invitation très réussi (gradient, Oui/Non/Peut-être).
- Modèles Explorer : fiche explicative et pré-remplissage (titre, description, type « Soirée »).
- Lancement du sondage avec confirmation. Vote « une question à la fois » avec progression et fuseau horaire affiché.
- Résultats : meilleur créneau, message prêt à partager, confirmation avec dialogue.
- Écran Options : calcul de budget correct (450 € × 6 = 2 700 €) et votes Je préfère / Neutre / Contre.
- Écrans Repas, Matériel, Transport et Logement présents, avec des états vides propres.
- Persistance offline-first : tout est écrit en SQLite local, avec la file de synchro visible (« Mise à jour en attente de synchronisation »).

## Backend — résumé de la QA API multi-utilisateurs
Les 4 événements vont de DRAFT à FINALIZED par l'API sans workaround en base. Les correctifs Rio (BUG-1 à 9) tiennent. Nouveaux points, détaillés avec curl et file:line dans le rapport API :
- **P1** :
  - Commentaires lisibles et publiables par un non-membre (aucun contrôle d'appartenance dans `CommentRoutes.kt`).
  - Équipement sans aucune authz (`equipmentRoutes(equipmentRepository)`, `Application.kt:606`).
  - Remboursements qui ignorent la table `expense` (`BudgetRepository.kt:485`).
  - `GET /budget/items|summary|statistics` renvoient systématiquement 500.
- **P2** :
  - `Event.validate()` n'est jamais appelé (min > max, deadline invalide, créneau inversé acceptés).
  - Réunion au `startTime` invalide : 500 alors que la réunion est enregistrée.
  - FINALIZED n'est pas en lecture seule.
  - Activités : n'importe quel membre peut inscrire n'importe qui.
  - Événement gratuit impossible à finaliser honnêtement (budget et Tricount imposés).
- **P3** : plusieurs payloads invalides renvoient 500 au lieu de 400, et le rappel manuel renvoie 501.

## Notes d'environnement
- Le serveur local journalise en `trace` (`server/src/main/resources/logback.xml`, `root level="trace"`). En quelques minutes, cela a saturé le disque (ENOSPC) et fait échouer le 1er build. Recommandation : `INFO` par défaut.
- Pour pouvoir saisir du texte depuis l'outil, la disposition clavier matérielle des 2 simulateurs a été passée en US (`AppleKeyboards`).
