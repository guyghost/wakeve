# Wakeve Apple Design Principles Audit

Audit realise sur le code iOS SwiftUI, les composants partages et les captures existantes du depot. Les constats citent surtout les surfaces iOS, car l'objectif est l'alignement avec des principes Apple de great design.

## Score global

**72 / 100**

Wakeve a deja une base produit serieuse: intention mobile-first claire, architecture d'ecrans riche, design system Liquid Glass, et garde-fous visibles sur les suggestions IA. L'app donne souvent une impression premium par ses cartes, ses fonds, ses etats vides et ses actions principales fixes.

Le frein principal n'est pas l'absence de design, mais l'exces: beaucoup d'ecrans presentent trop de capacites a la fois, avec des heros visuels tres presents, des cartes nombreuses, des textes parfois generiques, et des incoherences de langue qui cassent la confiance. Pour atteindre une sensation Apple-level, Wakeve doit devenir plus calme, plus editorialisee, plus predictable, et plus stricte sur le parcours "prochaine action".

## Resume executif

Wakeve est a un niveau de maturite design intermediaire-avance: l'experience a une identite visuelle, mais elle n'est pas encore totalement disciplinee.

Ses forces sont la clarte du domaine, la presence d'etats vides, l'utilisation d'actions principales persistantes, la prevention de certaines actions automatiques, et l'effort d'accessibilite visible dans le code.

Ses faiblesses sont la surcharge fonctionnelle, les incoherences de wording, des permissions parfois demandees trop vite, et des composants premium utilises meme quand une UI native plus simple serait plus rassurante.

La priorite est de simplifier l'orientation: chaque ecran doit afficher l'etat de l'evenement, la prochaine action et une seule action principale.

Les flux IA sont raisonnablement responsables, mais ils doivent etre davantage presentes comme des brouillons revisables, jamais comme des actions magiques.

Les ecrans messages/commentaires et scenarios sont les moins finis: anglais residuel, donnees exemples, labels non localises, et menus qui semblent encore techniques.

La creation d'evenement est puissante, mais doit mieux separer creation rapide et configuration avancee.

Les permissions doivent rester contextuelles et expliquees avant le prompt systeme.

Les quick wins portent sur copy, localisation, hierarchy, confirmations destructrices et etats disabled/loading.

Les refactors profonds portent sur la navigation par phase, la reduction des cartes, un modele de "next best action", et une bibliotheque de composants plus stricte.

## Scores par principe

- **Purpose: 7.5 / 10** - Les ecrans ont un objectif general clair, mais plusieurs melangent trop d'objectifs secondaires.
- **Agency: 7 / 10** - Retour, preview, edit, ignore et cancel existent souvent; certains lancements et suppressions doivent etre plus explicitement reversibles ou confirmes.
- **Responsibility: 7.5 / 10** - Bons signaux sur privacy, suppression de compte et IA editable; permissions contacts/localisation doivent etre mieux pre-expliquees.
- **Familiarity: 6.5 / 10** - Les patterns iOS sont presents, mais l'app alterne NavigationView, toolbars custom, heros, menus et boutons flottants de maniere parfois incoherente.
- **Flexibility: 7 / 10** - Dark mode, Dynamic Type partiel, textes longs et empty states sont pris en compte; les grands heros et layouts denses restent fragiles.
- **Simplicity: 6 / 10** - Le produit est encore trop large au premier niveau: IA, scenarios, transport, logement, messages, budget, repas, calendrier et preferences se concurrencent.
- **Craft: 7 / 10** - Design system ambitieux et belles captures, mais polish incomplet: copy mixte, statuts internes visibles, alertes generiques, anciennes variantes de composants.
- **Delight: 7 / 10** - L'app a de la chaleur visuelle; le delight doit venir davantage de moments utiles et rassurants que de gradients, emojis et cartes hero.

## Sources auditees

| Source | Evidence utilisee | Impact sur l'audit |
| --- | --- | --- |
| `iosApp/src/Views/Auth/OnboardingView.swift` | Onboarding pagine, skip, listes de features | Evaluation Purpose, Simplicity, Delight |
| `iosApp/src/Views/Events/HomeView.swift` | Home, filtres, empty state, FAB, hero event | Evaluation Purpose, Flexibility, Craft |
| `iosApp/src/Views/Events/CreateEventSheet.swift` | Wizard, IA, preview, validation, sheets | Evaluation Agency, Responsibility, Simplicity |
| `iosApp/src/Views/Polls/PollVotingView.swift` | Vote un creneau a la fois, alerts succes/erreur | Evaluation Familiarity, Craft |
| `iosApp/src/Views/Polls/PollResultsView.swift` | Score, meilleur creneau, confirmation date | Evaluation Agency, Responsibility |
| `iosApp/src/Views/Events/ParticipantManagementView.swift` | Invitations, contacts, groupes, lancer sondage | Evaluation Responsibility, Purpose |
| `iosApp/src/Views/Events/ScenarioOrganizationView.swift` | Scenarios, classement, comparaison, verrouillage | Evaluation Familiarity, Simplicity |
| `iosApp/src/Views/Events/TransportPlanningView.swift` | Readiness, IA, plans, CTA derive de l'etat | Evaluation Purpose, Agency, Simplicity |
| `iosApp/src/Views/Collaboration/CommentListView.swift` | Messages, mentions, moderation, sample users | Evaluation Responsibility, Craft |
| `iosApp/src/Views/Profile/ProfileTabView.swift` | Profil, preferences, data management | Evaluation Responsibility, Simplicity |
| `iosApp/src/Components/LocationSelectionSheet.swift` | Permission localisation et recherche lieu | Evaluation Responsibility, Agency |
| `docs/guides/ios/design-system.md` | Regles Wakeve iOS: glass reserve aux controles, accessibilite native | Evaluation Craft, Familiarity |
| `review-screenshots/ios-2026-05-22-fixed/*.jpg` | Home, detail, participants, resultats vote | Calibration visuelle, densite, hierarchie |
| `docs/app-store-evidence/*.jpg` | Login, onboarding, creation, Dynamic Type high contrast | Calibration accessibilite et flexibilite |

## Alignement iOS et Liquid Glass

Le design system iOS local pose une regle forte: **Liquid Glass est reserve aux controles flottants, barres de navigation et controles contextuels; les contenus longs doivent rester opaques ou tintes**. C'est un bon principe, car il preserve lisibilite, performance percue et accessibilite. L'audit recommande de le rendre plus strict dans les ecrans produit.

- **Conforme**: les boutons circulaires, toolbars flottantes, FAB et certains controles contextuels utilisent bien le langage Glass.
- **A surveiller**: plusieurs ecrans utilisent une accumulation de cartes, heros et surfaces translucides qui donne une sensation premium mais peut fatiguer la lecture.
- **Risque iOS**: quand chaque module devient une carte "speciale", l'utilisateur ne sait plus ce qui est interactif, informatif ou prioritaire.
- **Direction recommandee**: garder le Glass pour navigation, actions flottantes et controles interactifs; utiliser des surfaces opaques simples pour contenus longs, listes, messages, settings et resultats.
- **Verification a ajouter**: tester Reduce Transparency, Increase Contrast, Dynamic Type XXL, orientation et VoiceOver sur Home, Creation, Participants, Vote, Scenarios, Transport et Profile.

## Problemes prioritaires transverses

| Priorite | Probleme | Pourquoi c'est critique | Ecrans touches | Recommandation |
| --- | --- | --- | --- | --- |
| P0 | Donnees exemples dans mentions | Casse immediatement la confiance sociale | Messages | Connecter aux participants reels ou masquer l'autocomplete |
| P0/P1 | Permission localisation au focus recherche | Demande systeme trop precoce, sensation intrusive | Lieu, Creation | Demander uniquement sur action "Utiliser ma position" |
| P1 | Prochaine action insuffisamment visible | L'app montre les modules plus que l'avancement | Home, Detail, Participants, Transport | Ajouter un modele "Next Best Action" |
| P1 | Actions structurantes sans consequence explicite | L'utilisateur ne sait pas ce que confirmation date/sondage declenche | Resultats, Participants | Sheet de confirmation avec impacts |
| P1 | Copy mixte francais/anglais | Signal de produit inacheve | Vote, Scenarios, Messages, Participants | Localiser et mapper les statuts internes |
| P1 | Trop d'options dans creation | Le flux simple ressemble a un configurateur | Creation | Creation rapide + options avancees apres creation |
| P2 | Heros visuels trop dominants | Le contenu utile est sous le fold | Home, Participants, Scenarios | Reduire hauteur ou rendre heros contextuels |
| P2 | Settings vs permissions systeme non distingues | Les toggles peuvent promettre plus que l'etat iOS reel | Profil, Parametres, Notifications | Afficher preference Wakeve + permission systeme |

## Audit detaille par ecran

### Onboarding

- **Intention principale**: expliquer que Wakeve aide a creer, coordonner et finaliser un evenement.
- **Ce qui fonctionne**: progression paginee native, bouton "Suivant"/"Commencer", possibilite de passer l'onboarding, iconographie simple.
- **Problemes observes**: quatre pages listent beaucoup de capacites des le debut: creation, sondage, collaboration, taches, logement, repas, budget, rappels, calendrier. Cela vend un outil complet avant de montrer le moment humain a organiser.
- **Friction utilisateur**: l'utilisateur comprend la richesse fonctionnelle, mais pas forcement "ce que je peux faire en 30 secondes maintenant".
- **Recommandations**: reduire a trois promesses: "cree un moment", "trouve la date", "partage le plan". Garder le reste apres le premier evenement. Remplacer les listes de features par un exemple concret.
- **Priorite**: P2.

Reference: `iosApp/src/Views/Auth/OnboardingView.swift:86` definit 4 etapes et 12 features exposees.

### Home / A venir

- **Intention principale**: voir ce qui arrive bientot et reprendre la prochaine action.
- **Ce qui fonctionne**: filtre Upcoming/Past/Drafts, empty state avec action, featured event, FAB de creation visible.
- **Problemes observes**: la capture home montre une grande carte hero qui consomme presque tout le premier ecran. Le filtre, le profil et le bouton plus sont presents, mais la prochaine action est moins explicite que l'objet visuel.
- **Friction utilisateur**: pour plusieurs evenements, le scan rapide peut devenir lent; pour un evenement urgent, la tache suivante n'est pas assez priorisee.
- **Recommandations**: transformer Home en "Today / Next action first": une ligne "A faire maintenant", puis les evenements. Garder la grande carte seulement si elle porte un evenement vraiment prioritaire. Afficher un badge actionnable: "3 votes manquants", "date a confirmer", "inviter".
- **Priorite**: P1.

Reference: `iosApp/src/Views/Events/HomeView.swift:201` affiche loading, empty state ou hero + liste; `HomeView.swift:250` ajoute un FAB permanent.

### Creation d'evenement

- **Intention principale**: creer vite un evenement partageable.
- **Ce qui fonctionne**: flux par etapes, progression, retour, preview avant creation, validation par etape, suggestion IA annulable/modifiable/applicable/ignorable.
- **Problemes observes**: le flux embarque titre, description, IA, type, background, creneaux, lieu, participants attendus, mode de planning, scenarios et preview. C'est puissant, mais proche d'un configurateur.
- **Friction utilisateur**: pour un diner simple, le cout mental est trop eleve; pour un voyage complexe, la richesse est utile mais devrait apparaitre progressivement.
- **Recommandations**: separer "creation rapide" et "options avancees". Creation minimale: titre, creneaux ou date, lieu optionnel, creer. Apres creation: inviter, transport, logement, budget. L'IA doit etre un bouton secondaire "Pre-remplir", pas le premier bloc permanent.
- **Priorite**: P1.

References: `iosApp/src/Views/Events/CreateEventSheet.swift:16` montre de nombreux etats de formulaire; `CreateEventSheet.swift:315` introduit le bloc IA; `CreateEventSheet.swift:594` regroupe les options avant preview.

### Detail evenement

- **Intention principale**: clarifier l'etat courant et la prochaine action.
- **Ce qui fonctionne**: hero d'etat, cartes pour invitation, sondage, participants et action principale en bas.
- **Problemes observes**: l'ecran peut ressembler a un hub de modules plutot qu'a une orchestration guidee. Les cartes "Inviter", "Sondage", "Participants" sont utiles, mais l'ecran doit dire explicitement ce qui bloque.
- **Friction utilisateur**: l'organisateur doit interpreter lui-meme l'ordre d'action.
- **Recommandations**: ajouter un bloc unique "Prochaine etape" en haut: "Lance le sondage", "Confirme la date", "Choisis le logement". Les modules secondaires passent sous "Details".
- **Priorite**: P1.

Reference visuelle: `review-screenshots/ios-2026-05-22-fixed/04-event-detail-one-slot.jpg`.

### Vote

- **Intention principale**: permettre de repondre aux creneaux sans hesitation.
- **Ce qui fonctionne**: approche "une question a la fois", progression, etats "aucun creneau", bouton bas fixe, reprise des votes existants.
- **Problemes observes**: les alertes "Error" et "Success" restent generiques et en anglais; le succes force un OK au lieu d'une confirmation inline plus calme.
- **Friction utilisateur**: l'utilisateur a l'impression d'un formulaire technique au moment le plus social du produit.
- **Recommandations**: remplacer les alertes succes par une confirmation inline avec action "Voir les resultats" ou "Modifier mes votes". Localiser tous les titres d'alertes et autoriser clairement le changement de vote tant que le sondage est ouvert.
- **Priorite**: P2.

Reference: `iosApp/src/Views/Polls/PollVotingView.swift:34` utilise encore "Error" et "Success".

### Resultats de vote

- **Intention principale**: comprendre le meilleur creneau et confirmer la date.
- **Ce qui fonctionne**: meilleur creneau mis en avant, scores visibles, CTA de confirmation.
- **Problemes observes**: la notion de score est affichee sans expliquer sa logique; confirmer la date est une action structurante mais n'a pas de preview de consequence.
- **Friction utilisateur**: l'organisateur peut confirmer sans voir qui sera exclu ou ce que cela debloque.
- **Recommandations**: afficher "Pourquoi ce creneau ?" avec oui/peut-etre/non et participants impactes. Avant confirmation: sheet native "Confirmer cette date ?" avec consequences: details debloques, invitations calendrier, transport/logement accessibles.
- **Priorite**: P1.

Reference visuelle: `review-screenshots/ios-2026-05-22-fixed/05-poll-results-localized.jpg`.

### Participants

- **Intention principale**: inviter, suivre les reponses, lancer le sondage quand tout est pret.
- **Ce qui fonctionne**: groupes acceptes/en attente/refuses, statut visible, action bas fixe, permission contacts optionnelle.
- **Problemes observes**: l'ecran demande les contacts au moment du choix depuis contacts, mais sans sheet de rationale avant le prompt systeme. Certains statuts internes restent en anglais dans le mapping fallback.
- **Friction utilisateur**: la permission contacts peut sembler trop large; les groupes vides creent de la verticalite inutile.
- **Recommandations**: pre-permission sheet: "Wakeve lit uniquement les noms et emails pour vous aider a inviter". Masquer les groupes vides par defaut. Convertir tous les statuts internes en labels localises utilisateur.
- **Statut implementation**: une rationale localisee est affichee avant le prompt Contacts lorsque l'autorisation n'est pas deja accordee; l'ajout manuel reste disponible. Les groupes participants vides sont masques par defaut, en conservant l'etat vide global.
- **Priorite**: P1 pour permission, P2 pour structure.

References: `iosApp/src/Views/Events/ParticipantManagementView.swift:356` groupe les participants; `ParticipantManagementView.swift:784` declenche le chargement contacts.

### Transport

- **Intention principale**: completer les departs, generer ou choisir un plan de transport.
- **Ce qui fonctionne**: verrouillage selon acces, pending sync, readiness, plan final, suggestion IA editable, action primaire derivee de l'etat.
- **Problemes observes**: l'ecran contient route preview, readiness, IA, depart, optimisation, participants, plans. Cela fait beaucoup pour une phase logistique. Le bouton primaire peut devenir descriptif mais inactif, ce qui est frustrant.
- **Friction utilisateur**: l'utilisateur ne sait pas toujours quelle carte est prioritaire.
- **Recommandations**: organiser en trois modes: "A completer", "Comparer", "Plan final". Ne montrer l'IA qu'apres readiness complete ou via bouton secondaire. Quand le CTA est disabled, afficher la condition precise juste au-dessus.
- **Priorite**: P1.

References: `iosApp/src/Views/Events/TransportPlanningView.swift:84` empile 7 blocs; `TransportPlanningView.swift:180` montre une suggestion IA editable; `TransportPlanningView.swift:513` calcule le CTA principal.

### Logement / Scenarios

- **Intention principale**: comparer des options de destination/logement et choisir un scenario final.
- **Ce qui fonctionne**: verrouillage par acces, classement, comparaison, votes scenario, ouverture vers transport.
- **Problemes observes**: copy non finalisee ("Scenarios", "Acces verrouille", alert "Scenario"), heros tres grand, comparaison et classement melanges.
- **Friction utilisateur**: la valeur "logement" n'est pas toujours evidente; "scenario" est un terme interne plus qu'un langage utilisateur.
- **Recommandations**: renommer cote UI en "Options de sejour" ou "Plans". Separarer "Comparer" et "Choisir". Remplacer les alertes par feedback inline.
- **Priorite**: P1.

Reference: `iosApp/src/Views/Events/ScenarioOrganizationView.swift:73` utilise une alerte "Scenario"; `ScenarioOrganizationView.swift:100` affiche "Scenarios".

### Messages

- **Intention principale**: permettre une coordination humaine et contextuelle.
- **Ce qui fonctionne**: threads, reponses, mentions, pin, soft delete, moderation report/block.
- **Problemes observes**: beaucoup de textes sont en anglais; l'autocomplete mention utilise des utilisateurs exemples; le bouton filtre est vide; suppression de commentaire passe par menu sans confirmation visible dans le composant lu.
- **Friction utilisateur**: l'ecran semble moins produit que prototype, ce qui nuit a la confiance dans un espace social.
- **Recommandations**: remplacer tous les placeholders anglais, brancher les participants reels, clarifier pin/report/delete, confirmer delete/remove avec role destructif, et offrir empty state social en francais.
- **Priorite**: P0/P1 selon exposition en production, car les donnees exemples dans un ecran social cassent la confiance.

References: `iosApp/src/Views/Collaboration/CommentListView.swift:340` contient des sample users; `CommentListView.swift:306` utilise "Add a comment..."; `CommentListView.swift:281` affiche "Load more replies".

### Profil

- **Intention principale**: gerer identite, preferences et compte.
- **Ce qui fonctionne**: header clair, avatar, preferences, data management, suppression de compte avec confirmation.
- **Problemes observes**: le profil devient un hub de dashboard, gamification, badges, leaderboard, preferences, apparence, about et sign out. Ce volume dilue les reglages sensibles.
- **Friction utilisateur**: difficile de distinguer "mon identite", "mes preferences", "mon compte", "mes recompenses".
- **Recommandations**: scinder en Profil public, Preferences, Confidentialite/Compte. Deplacer leaderboard/gamification hors profil si ce n'est pas central pour l'organisation d'evenements.
- **Priorite**: P2.

References: `iosApp/src/Views/Profile/ProfileTabView.swift:188` regroupe notifications/calendrier/email; `ProfileTabView.swift:393` gere la suppression de compte.

### Parametres

- **Intention principale**: regler les preferences sans surprise.
- **Ce qui fonctionne**: toggles standards, navigation vers notifications, data management.
- **Problemes observes**: toggles AppStorage pour notifications/calendrier/email peuvent laisser croire que des permissions systeme sont actives alors que le consentement systeme n'est pas necessairement accorde.
- **Friction utilisateur**: decalage possible entre preference interne et permission iOS.
- **Recommandations**: afficher un etat permission systeme distinct: "Autorise dans iOS", "Active dans Wakeve". Si permission refusee, bouton "Ouvrir Reglages".
- **Priorite**: P1.

### Notifications

- **Intention principale**: choisir les notifications utiles et eviter l'intrusion.
- **Ce qui fonctionne**: types de notifications, heures silencieuses, son/vibration.
- **Problemes observes**: les preferences sont riches, mais il manque un contexte utilisateur: quelles notifications sont critiques pour un evenement ? lesquelles sont silencieuses ?
- **Friction utilisateur**: trop de toggles sans priorisation.
- **Recommandations**: grouper en "Essentielles" (invitations, date confirmee), "Rappels", "Social". Ajouter examples courts.
- **Priorite**: P2.

Reference: `iosApp/src/Views/Notifications/NotificationPreferencesView.swift:57` liste les types, heures silencieuses et son/vibration.

### Etats vides

- **Intention principale**: expliquer quoi faire ensuite.
- **Ce qui fonctionne**: `EmptyState` recurrent, system images, CTA dans Home, transport locked state.
- **Problemes observes**: certains empty states sont informatifs mais pas assez actionnables; messages/commentaires sont encore en anglais.
- **Recommandations**: chaque empty state doit avoir un titre humain, une phrase de contexte, une action principale ou une raison explicite d'absence d'action.
- **Priorite**: P2.

### Etats de chargement

- **Intention principale**: rassurer pendant l'attente.
- **Ce qui fonctionne**: Home a un skeleton; IA et transport ont ProgressView localises.
- **Problemes observes**: chargement parfois simule ou spinner simple, sans preservation de layout.
- **Recommandations**: standardiser skeletons par ecran, eviter les spinners seuls sauf actions courtes, garder la position des CTA stable.
- **Priorite**: P2.

### Erreurs

- **Intention principale**: dire ce qui s'est passe et comment recuperer.
- **Ce qui fonctionne**: erreurs visibles dans plusieurs flux, role cancel.
- **Problemes observes**: erreurs trop generiques, parfois anglais, parfois `localizedDescription` brut.
- **Recommandations**: mapping d'erreurs produit: probleme reseau, permission refusee, donnees invalides, action non autorisee. Toujours proposer retry, modifier, ouvrir reglages ou annuler selon le cas.
- **Priorite**: P1.

### Confirmations

- **Intention principale**: rassurer apres action importante.
- **Ce qui fonctionne**: suppression de compte utilise `confirmationDialog` destructif; creation passe par preview.
- **Problemes observes**: succes sous forme d'alert "OK" casse le rythme. Confirm date et launch poll devraient expliquer les consequences.
- **Recommandations**: confirmations inline pour actions non destructrices; confirmation sheet pour actions structurantes; dialog destructif uniquement pour pertes de donnees.
- **Priorite**: P1.

### Suppressions

- **Intention principale**: proteger l'utilisateur des pertes irreversibles.
- **Ce qui fonctionne**: data management a un dialog destructif et un message de scope; budget/meeting ont des confirmations.
- **Problemes observes**: suppression de commentaires semble appelee directement depuis menu dans le composant lu; les suppressions de contenu social doivent etre explicites.
- **Recommandations**: toute suppression visible par d'autres participants demande confirmation avec consequence: "Le commentaire sera remplace par Supprime".
- **Priorite**: P1.

### Permissions

- **Intention principale**: demander au bon moment, avec justification.
- **Ce qui fonctionne**: localisation a un message permission et reglages; contacts sont optionnels; notification service indique que la demande doit venir d'une action utilisateur.
- **Problemes observes**: la recherche de lieu demande l'autorisation des que le champ search est focus, avant que l'utilisateur choisisse "position actuelle". C'est trop tot.
- **Recommandations**: ne jamais demander localisation au focus du champ. Demander seulement sur "Utiliser ma position", apres une rationale inline. Contacts: meme pattern avec pre-permission.
- **Priorite**: P0/P1 pour confiance.

References: `iosApp/src/Components/LocationSelectionSheet.swift:369` demande la localisation au focus recherche; `LocationSelectionSheet.swift:387` gere aussi le bon cas "position actuelle".

## Quick wins

1. Localiser et humaniser tous les textes residuels anglais: Error, Success, Scenario, Comments, Add a comment, Load more replies, Mention someone, Confirmed/Pending/Member.
2. Remplacer les alertes succes par des confirmations inline ou toasts non bloquants.
3. Ajouter une sheet de consequence avant "Confirmer cette date" et "Lancer le sondage".
4. Supprimer la demande localisation au focus du champ recherche; la garder uniquement sur "Utiliser ma position".
5. Masquer les sections vides dans Participants sauf si elles expliquent une action.
6. Ajouter un bloc "Prochaine action" en haut du detail evenement.
7. Reduire les heros trop grands sur Home, Participants et Scenarios pour laisser voir plus de contenu utile.
8. Afficher l'etat permission systeme dans Parametres/Notifications.
9. Remplacer les sample users de mentions par participants reels ou masquer la fonctionnalite si non branchee.
10. Standardiser les labels disabled: un CTA disabled doit expliquer pourquoi.

## Refactors profonds

1. **Next Best Action Engine UI**: exposer une seule prochaine action par statut d'evenement, partagee par Home, Detail, Participants, Vote, Scenarios, Transport.
2. **Creation rapide vs options avancees**: creer un flux minimal pour les evenements simples et deplacer IA/scenarios/backgrounds apres creation.
3. **Reframe "Scenarios" en langage utilisateur**: "Options", "Plans", "Sejour", "Destination et logement" selon le type d'evenement.
4. **Permission Orchestrator**: composant commun pre-permission + etat systeme + bouton Reglages pour contacts, localisation, calendrier, notifications, photos.
5. **Design System Governance**: definir quand utiliser hero, card, form native, toolbar custom, sheet, menu et CTA bas fixe.
6. **Message Surface Production Hardening**: supprimer donnees exemples, finir localisation, confirmer delete/remove, clarifier moderation, connecter mentions aux participants.
7. **Event Phase Navigation**: rendre les phases DRAFT/POLLING/CONFIRMED/ORGANIZING/FINALIZED visibles dans une timeline simple au lieu d'un hub de modules.

## Composants a revoir

- **Boutons**: clarifier disabled reasons; differencier action primaire, secondaire, destructive, suggestion IA.
- **Cartes**: reduire les cartes imbriquees ou decoratives; reserver les cartes aux objets manipulables.
- **Tab bar**: verifier coherence entre Home/Inbox/Explore et les flux plein ecran qui cachent la tab bar.
- **Toolbar**: standardiser xmark vs chevron.left; xmark pour dismiss modal, chevron pour navigation push.
- **Sheets**: utiliser sheets pour confirmation structurante et permission rationale; eviter alertes bloquantes pour succes.
- **Input fields**: placeholders localises, hints de validation, pas de permission systeme au focus.
- **Chips**: statuts localises et actionnables; eviter les badges purement decoratifs.
- **Avatars**: remplacer initials generiques "U" par identite reelle ou placeholder explicite.
- **Empty states**: toujours action ou raison; copy chaleureuse, pas generique.
- **Loading states**: skeletons par layout; spinners seulement pour actions courtes.
- **Error states**: messages recuperables, pas `localizedDescription` brut.

## Risques produit

- **Surcharge fonctionnelle**: Wakeve risque de paraitre plus complexe qu'un chat + sondage, alors que son avantage devrait etre de reduire la coordination.
- **Manque de clarte**: les modules scenarios/transport/logement/budget peuvent masquer la prochaine action.
- **Permissions trop tot**: localisation au focus recherche et contacts sans rationale forte peuvent degrader la confiance.
- **IA trop visible**: l'IA est responsable dans ses actions, mais trop presente dans certains flux simples.
- **Actions irreversibles**: suppression sociale et confirmation de date doivent mieux exposer les consequences.
- **Manque de feedback**: plusieurs actions utilisent alertes generiques ou boutons disabled muets.
- **Accessibilite**: les grands heros, overlays, gradients et textes scales demandent des tests systematiques avec Dynamic Type et contraste eleve.
- **Incoherence linguistique**: melange francais/anglais = signal de produit inacheve.

## Backlog design actionnable

### P0 - Confiance immediate

1. **[x] Messages: retirer les sample users**
   - Fichiers: `iosApp/src/Views/Collaboration/CommentListView.swift`
   - Critere: aucune mention `alice`, `bob`, `charlie`, `david` n'apparait dans une surface utilisateur; l'autocomplete est branchee aux participants ou masquee.
   - Statut implementation: autocomplete masquee sans liste de participants, bouton mention desactive, copies visibles localisees.

2. **[x] Lieu: supprimer la permission localisation au focus**
   - Fichiers: `iosApp/src/Components/LocationSelectionSheet.swift`
   - Critere: focus du champ recherche ne declenche jamais le prompt iOS; seul le choix "Utiliser ma position" peut le faire apres rationale.
   - Statut implementation: suppression du declenchement au focus; la permission reste liee a l'action explicite "Utiliser ma position".

### P1 - Clarite et controle

3. **[x] Next Best Action**
   - Fichiers probables: `HomeView.swift`, detail evenement, state/event helpers partages.
   - Critere: Home et Detail affichent une action prioritaire unique basee sur `EventStatus`, avec raison visible si elle est bloquee.
   - Statut implementation: `EventNextAction` centralise titre, icone, sous-titre et raison bloquante; Home affiche un bloc "Prochaine action" et le detail reutilise le meme modele pour sa carte et son CTA bas.

4. **[x] Confirmation de date**
   - Fichiers: `PollResultsView.swift`
   - Critere: confirmer la date ouvre une sheet de consequence avant mutation: participants impactes, details debloques, prochaines etapes.
   - Statut implementation: ajout d'un `confirmationDialog` localise avant mutation, avec consequences de confirmation.

5. **[x] Lancement du sondage**
   - Fichiers: `ParticipantManagementView.swift`
   - Critere: lancer le sondage confirme les invitations/notifications prevues et indique si l'action est reversible ou non.
   - Statut implementation: ajout d'un `confirmationDialog` localise avant ouverture du sondage.

6. **[x] Creation rapide**
   - Fichiers: `CreateEventSheet.swift`
   - Critere: le flux de base tient en trois decisions maximum avant preview; IA, backgrounds, scenarios et planning avance passent en disclosure progressive.
   - Statut implementation: le wizard de base est reduit a trois etapes (`Nom`, `Date`, `Confirmer`) avant preview. IA/type passent sous "Options avancees" dans l'etape Nom; lieu, taille de groupe, fond, mode de vote et scenarios passent sous "Options avancees" dans la confirmation.

7. **[x] Localisation et statuts**
   - Fichiers: `PollVotingView.swift`, `ScenarioOrganizationView.swift`, `CommentListView.swift`, participant mappers.
   - Critere: aucun label utilisateur en anglais dans un environnement francais; aucun statut interne brut visible.
   - Statut implementation: copies critiques localisees dans `PollVotingView`, `ScenarioOrganizationView`, `CommentListView`, `ParticipantManagementView` et `PollResultsView`; les groupes participants utilisent un etat type au lieu de comparer des labels affiches.

### P2 - Coherence iOS et polish

8. **[x] Glass governance**
   - Fichiers: composants `WakeveGlass*`, ecrans Home/Participants/Scenarios/Transport/Profile.
   - Critere: contenus longs et listes utilisent surfaces opaques/tintees; Glass reserve aux controles flottants et interactifs.
   - Statut implementation: matrice de gouvernance ajoutee dans `docs/guides/ios/design-system.md`; les ecrans Home, Participants, Scenarios, Transport, Profile et Detail evenement n'utilisent plus `LiquidGlassCard` pour les blocs longs, listes, formulaires ou resumes. Le vrai Glass reste limite aux controles (`WakeveGlassControl`, FAB, boutons).

9. **[x] Settings permission state**
   - Fichiers: `ProfileTabView.swift`, `NotificationPreferencesView.swift`, services APNs/calendrier.
   - Critere: les toggles distinguent preference Wakeve et permission systeme iOS.
   - Statut implementation: `NotificationPreferencesView` affiche une section "Autorisation iOS" separee des preferences Wakeve et propose "Reglages" si iOS refuse les notifications; `ProfileTabView` resume l'etat Wakeve + iOS dans la ligne Notifications.

10. **[x] Dynamic Type pass**
    - Fichiers: tous les ecrans principaux.
    - Critere: captures iPhone petit format + AX XXL sans overlap, troncature critique ni CTA masque.
   - Statut implementation: protocole Dynamic Type ajoute dans `docs/guides/ios/design-system.md`; titres hero Participants/Scenarios et nom Profil assouplis sur deux lignes. `EmptyState` et `LiquidGlassButton` acceptent les textes multi-lignes en tailles accessibilite; Home vide utilise une copie compacte AX XXL et garde le CTA visible via FAB. Onboarding et Login passent sur layouts scrollables avec marges basses et textes non tronques. Captures verifiees: Home AX XXL sur iPhone 17; Onboarding et Login sur iPhone SE 3e generation.

## Plan d'action recommande

### Phase 1: clarte, navigation et actions principales

- Ajouter "Prochaine action" sur Home et Detail.
- Simplifier le premier ecran de creation.
- Confirmer explicitement lancement de sondage et confirmation de date.
- Corriger localisation/copy critique.
- Supprimer permissions trop precoces.
- Remplacer sample users dans Messages.

### Phase 2: etats, accessibilite, permissions et coherence

- Standardiser empty/loading/error/disabled states.
- Creer le composant permission rationale.
- Harmoniser toolbar, bottom CTA, destructive actions.
- Auditer Dynamic Type sur Home, Creation, Participants, Vote, Transport, Profil.
- Reorganiser Profil/Parametres par intention.

### Phase 3: polish, micro-interactions et delight premium

- Ajouter micro-confirmations contextuelles: vote envoye, participant rejoint, date confirmee, plan final choisi.
- Introduire une timeline d'avancement d'evenement.
- Reduire l'usage de heros decoratifs au profit de moments utiles.
- Ajouter des transitions legeres entre phases d'evenement.
- Renforcer la chaleur humaine de la copy: moins de module, plus de moment partage.

## Definition de done design

- Chaque ecran principal expose une intention, une prochaine action et un etat.
- Toute permission est demandee apres action explicite et rationale.
- Toute action destructive a une confirmation claire.
- Toute action structurante expose ses consequences.
- Aucune surface utilisateur ne montre de donnees exemples ou de labels internes.
- Les textes francais sont complets et coherents.
- Les etats vide, chargement, erreur et succes sont actionnables.
- Les flux simples restent simples; les options avancees apparaissent par disclosure progressive.
