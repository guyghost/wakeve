# Wakeve Naming Guidelines

## Principes
- Clarté avant créativité.
- Nommer le bénéfice utilisateur, pas la technologie.
- Rester naturel à l'oral en français et en anglais.
- Garder une cohérence FR/EN entre Android, iOS, notifications, raccourcis, Siri/App Actions et textes partagés.
- Éviter les termes techniques quand un mot utilisateur existe.
- Éviter les formulations qui jugent l'utilisateur ou dramatisent un état normal.
- Plus l'action est critique, plus le label doit être clair, neutre et prévisible.
- Plus le moment est émotionnel, plus la microcopie peut être chaleureuse.

## Vocabulaire Validé

| Concept | Nom français | Nom anglais | Usage | Exemple | À éviter |
|---|---|---|---|---|---|
| App | Wakeve | Wakeve | Nom de marque uniquement | `Invitation Wakeve` | Variantes descriptives du nom |
| Primary event object | événement | event | Objet métier, paramètres, suppression, sécurité, invitations | `Supprimer l'événement` | Moment pour les actions critiques |
| Emotional event reference | moment | moment | Empty states, onboarding, carte éditoriale | `Prochain moment` | Utilisé seul dans navigation critique |
| Home tab | À venir | Upcoming | Accueil des événements futurs | `Va dans À venir` | Accueil, Home si locale FR |
| Groups tab | Groupes | Groups | Groupes persistants ou cercles de participants | `Ouvre Groupes` | Participants si l'écran ne gère pas des groupes |
| Messages tab | Messages | Messages | Conversations et commentaires | `Regarde les messages` | Inbox si cela mélange tout |
| Profile tab | Profil | Profile | Compte, préférences, données | `Ouvre Profil` | Mon Profil avec capitalisation lourde |
| Event creation | Créer un événement | Create event | Bouton principal compact et critique | `Je vais créer un événement` | Préparer un moment comme action principale |
| Event preparation | Préparer | Prepare | Sous-titres, next actions, onboarding | `Continuer la préparation` | Finaliser si rien n'est final |
| Invitation action | Inviter | Invite | Ajouter des personnes ou partager l'accès | `Inviter le groupe` | Ajouter des participants si le canal est une invitation |
| Participant | participant | participant | Personne impliquée dans l'événement | `1 participant` | invité une fois rejoint |
| Invitee | invité | guest / invitee | Personne invitée avant réponse | `Invitations en attente` | participant confirmé |
| Attendance yes | Je participe / Présent | I'm in / Going | RSVP selon surface | `Confirme ta participation` | Going en FR, Participating en bouton |
| Attendance no | Je ne participe pas / Absent | Not going | RSVP | `Je ne participe pas` | Refusé pour l'utilisateur lui-même |
| Uncertain attendance | Peut-être | Maybe | RSVP et vote souple | `Peut-être` | Possible dans les boutons |
| Poll | sondage | poll | Vote sur dates/créneaux | `Sondage en cours` | vote comme nom d'écran global |
| Vote action | Voter | Vote | Soumettre un choix | `Voter` | Valider si l'action envoie un vote |
| Time option | créneau | time slot | Date/heure proposée dans un sondage | `Ajouter un créneau` | slot en UI française |
| Scenario planning | option | option | Choix destination/logement/budget présenté aux participants | `Comparer les options` | scénario en UI principale |
| Internal scenario | scénario | scenario | Architecture, docs techniques, analytics si nécessaire | `scenario_id` | scénario visible quand option suffit |
| Final selection | option retenue | selected option | Choix final non destructif | `Sélectionner cette option` | scénario final |
| Transport | transport | transport | Section logistique | `Transport` | mobilité si non utilisé partout |
| Route/rides | trajet | ride / route | Proposition concrète de déplacement | `Proposer un trajet` | transport option pour une seule route |
| Lodging | logement | lodging | Section hébergement | `Logement` | accommodation en FR |
| Budget | budget | budget | Section coût partagé | `Aperçu du budget` | finances si ce n'est pas bancaire |
| Checklist | liste | checklist | Équipement, tâches, courses | `Liste d'équipement` | todo en UI FR |
| Meeting | réunion | meeting | Lien virtuel ou point de coordination | `Créer une réunion` | call si ce n'est pas toujours un appel |
| Notification center | Notifications | Notifications | Alertes et mises à jour | `Notifications` | Boîte de réception si messages séparés |
| Active state | en cours | in progress | Processus ouvert | `Sondage en cours` | actif si état métier non clair |
| Pending state | en attente | pending | Réponse ou sync non terminée | `Invitation en attente` | bloqué sauf vraie impossibilité |
| Finished state | terminé | complete | Tâche terminée | `Terminé` | finalisé sauf statut d'événement |
| Final event state | finalisé | finalized | Événement prêt et verrouillé | `Événement finalisé` | terminé si implications métier fortes |
| Draft state | brouillon | draft | Travail non publié | `Brouillon` | prévisualisation organisateur comme statut |
| AI suggestions | suggestions | suggestions | Résultat utilisateur | `Préparer des suggestions` | Assistant IA, Générer |
| AI draft | brouillon | draft | Texte ou plan proposé | `Préparer un brouillon` | générer automatiquement |

## Vocabulaire À Éviter

| Terme | Problème | Remplacement |
|---|---|---|
| Assistant IA | Met la technologie au centre | Suggestions, Préparer, Résumer |
| Générer | Technique et vague | Préparer, Proposer, Créer un brouillon |
| Automatiser | Promesse trop large | Préparer, Remplir, Suggérer |
| Scénario final | Son technique et lourd | Option retenue |
| Leader actuel | Ton compétitif pour une décision collective | Option préférée |
| Bloqué | Peut inquiéter si l'utilisateur peut agir | À compléter, En attente |
| Soumettre | Administratif | Envoyer, Confirmer, Voter |
| Valider | Ambigu sans résultat | Confirmer, Enregistrer, Envoyer |
| Terminer | Ambigu si l'action publie ou verrouille | Finaliser, Enregistrer, Fermer |
| Boîte de réception | Trop e-mail si elle contient notifications et tâches | Notifications ou Messages selon surface |
| Smart sharing / Partage intelligent | Marketing et technique | Suggestions de partage |
| Gamification names like Party Animal | Ton trop ludique et difficile à localiser | Badges sobres liés à l'organisation |

## Règles Pour Les Boutons

- Commencer par un verbe court: `Créer`, `Inviter`, `Voter`, `Partager`, `Confirmer`, `Supprimer`.
- Décrire le résultat si l'action a un effet durable: `Confirmer la date`, `Supprimer l'événement`, `Lancer le sondage`.
- Garder les actions destructrices neutres et explicites.
- Éviter les labels qui promettent une étape finale si l'utilisateur pourra encore modifier après.
- Éviter les emojis dans les boutons principaux; ils réduisent la lisibilité et compliquent la localisation.

## Règles Pour Les États

- Nommer l'état, puis expliquer l'action possible dans une phrase courte si nécessaire.
- Utiliser `En attente` pour une réponse ou une synchronisation non terminée.
- Utiliser `À compléter` quand l'utilisateur peut débloquer la suite.
- Réserver `Bloqué` aux situations réellement impossibles sans action externe.
- Ne pas transformer un état métier en jugement: éviter `mauvais`, `échec`, `refusé` quand `indisponible` suffit.

## Règles Pour Les Empty States

- Dire ce qui manque.
- Donner une prochaine action claire.
- Rester court et non culpabilisant.
- Adapter le ton à la surface:
  - fonctionnel: `Aucun participant. Ajoutez au moins une personne pour ouvrir le sondage.`
  - chaleureux: `Aucun moment à venir. Créez un événement quand vous êtes prêt.`

## Grille De Score

| Critère | Score |
|---|---|
| Appartient à Wakeve | /5 |
| Définit les attentes | /5 |
| Fonctionne FR/EN et sur petits écrans | /5 |
| Naturel à l'oral | /5 |
| Clair à petite taille | /5 |
| Cohérent avec les autres labels | /5 |

Score total sur 30. Un label sous 22 doit être réécrit ou justifié.
