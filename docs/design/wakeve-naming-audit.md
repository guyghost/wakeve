# Wakeve Naming Audit

## Scope
Audit initial réalisé sur:
- Android: `composeApp/src/androidMain/res/values*/strings.xml`, navigation Compose principale.
- iOS: `iosApp/src/Resources/fr.lproj/Localizable.strings`, `iosApp/src/Resources/en.lproj/Localizable.strings`, `iosApp/src/Models/WakeveTab.swift`.
- Serveur: localisation des notifications dans `server/src/main/kotlin/com/guyghost/wakeve/i18n/ServerLocalization.kt`.
- Documentation design existante: `docs/design/wakeve-voice-and-tone.md`.

## Résumé

## Implémentation Du 2026-06-15

Les remplacements P0/P1 et quelques P2 courts ont été appliqués dans:
- Android FR/EN resources: `composeApp/src/androidMain/res/values*/strings.xml`
- Android bottom navigation hardcodée: `WakeveBottomBar.kt`
- iOS FR/EN resources: `iosApp/src/Resources/{fr,en}.lproj/Localizable.strings`
- Server notification/gamification copy: `ServerLocalization.kt`

Les remplacements ont ensuite été étendus aux locales Android `es`, `it`, `pt`, `de` et aux locales iOS `es`, `it`, `pt` pour les mêmes familles de labels: options, notifications, réunions, participants et états.

Une passe sur les chaînes visibles hardcodées a aussi aligné les libellés restants dans `ProfileTabScreen`, les écrans et dialogues repas/équipement, `NotificationsScreen`, `CommentListScreen`, `ContentView`, `CreateEventSheet`, `TransportPlanningView`, `MeetingDetailView`, `NotificationPreferencesViewModel` et `MealPlanningSheets`.

### Noms Conservés
- `À venir` / `Upcoming` pour la destination principale des événements futurs.
- `Messages` / `Messages` si la surface contient des conversations, pas des alertes système.
- `Profil` / `Profile` pour compte, préférences et données.
- `Créer un événement` / `Create event` comme action principale claire.
- `Inviter` / `Invite`, `Voter` / `Vote`, `Partager` / `Share`, `Confirmer` / `Confirm`.
- `Transport`, `Logement`, `Budget`, `Participants`, `Notifications`.

### Noms À Modifier Ou Surveiller
- `Scénarios` visible dans l'UI principale doit devenir `Options` quand l'utilisateur compare des destinations/logements/budgets.
- `Accueil` dans la bottom bar Android doit devenir `À venir` pour s'aligner sur iOS.
- `Boîte de réception` doit être clarifié: `Notifications` pour alertes, `Messages` pour conversations.
- Les labels IA doivent éviter `Assistant IA`, `Générer`, `Auto`, `Smart`.
- Les badges de gamification très ludiques doivent être revus pour un ton plus premium.

## Table D'Audit

| Current label | Screen/source | Problem | Proposed label | Reason | Priority |
|---|---|---|---|---|---|
| Accueil | Android bottom navigation | Diverge d'iOS et ne dit pas ce que l'utilisateur y trouve | À venir | Cohérent avec la navigation recommandée et les événements futurs | P1 |
| Notifications | Android bottom navigation | Contenu décrit comme notifications, tâches et messages | Notifications ou Messages, selon contenu réel | Réduire l'ambiguïté entre alertes et conversations | P1 |
| Explorer | Android/iOS Explore | Peut promettre une découverte publique alors que l'app est centrée organisation privée | Idées ou Explorer | Conserver seulement si la surface contient templates, conseils et découverte | P2 |
| Groupes | iOS bottom navigation | Acceptable seulement si groupes persistants existent | Groupes | À garder; sinon remplacer par Participants ou Contacts | P2 |
| Créer un événement | Android/iOS home | Clair et fiable | Créer un événement | Meilleur choix pour action principale | P0 keep |
| Créer un moment | Voice guide | Trop vague pour action critique | Créer un événement | Garder `moment` pour empty states et cartes éditoriales | P1 |
| Détails de l'événement | Android event detail | Correct mais un peu administratif | Détail de l'événement | Plus naturel comme titre d'écran | P3 |
| Je participe | Android RSVP | Clair et humain | Je participe | À garder pour RSVP personnel | P0 keep |
| Going | Android EN RSVP | Standard mais moins naturel que invitation sociale | I'm in | Plus oral pour RSVP; garder `Going` dans listes/états | P2 |
| Présents | iOS event participants | Peut exclure les futurs participants non encore présents physiquement | Participants confirmés | Meilleure attente sur l'accès aux détails | P1 |
| Acceptés | iOS participant tabs | Son administratif et flou | Confirmés | Reflète la participation/accessibilité | P1 |
| Refusés | iOS participant tabs | Peut sembler juger l'utilisateur | Indisponibles | Plus neutre pour une invitation sociale | P1 |
| Sondage en cours | Android/iOS status | Clair | Sondage en cours | À garder | P0 keep |
| Vote en cours | Suggested copy | Ambigu: vote comme action ou état | Sondage en cours | Le système organise un sondage, l'utilisateur vote | P1 |
| Votes envoyés | iOS poll voting | Clair | Votes envoyés | À garder | P0 keep |
| Vote soumis | Android | Plus administratif et moins naturel | Vote envoyé | Cohérent avec iOS et plus oral | P2 |
| Scénarios | Android primary section | Terme technique pour un utilisateur | Options | L'utilisateur compare des options concrètes | P1 |
| Créer un scénario | Android | Technique et peut sembler fictionnel | Ajouter une option | Action concrète, extensible destination/logement/budget | P1 |
| Sélectionner comme scénario final | Android | Long et technique | Retenir cette option | Court, naturel, clair sur le résultat | P1 |
| Scénario sélectionné | Android | Technique | Option retenue | Même concept que la décision finale | P1 |
| Comparer les scénarios | Android | Technique | Comparer les options | Plus naturel à l'oral | P1 |
| Leader actuel | Android scenario voting | Ton compétitif inutile | Option préférée | Plus collectif et clair | P2 |
| Vote for this | Android EN with emoji | Emoji et objet flou | Prefer this option | Plus précis et localisable | P1 |
| Préférer | Scenario vote | Correct mais un peu abrupt | Je préfère | Plus naturel pour le vote personnel si l'espace le permet | P2 |
| Réunions virtuelles | Android meetings | Clair mais peut être redondant | Réunions | Garder le détail dans le sous-texte si besoin | P3 |
| Générer le lien | Android meeting | Verbe technique | Créer le lien | Décrit le bénéfice sans mécanisme | P2 |
| Assistant vocal | Android notification channel | Met la technologie au centre | Commandes vocales | Plus descriptif du bénéfice | P2 |
| Erreur Assistant Vocal | Android notification | Capitalisation et technologie | Commande vocale indisponible | Plus actionable | P1 |
| Smart Sharing / Partage intelligent | Android albums | Marketing et technique | Suggestions de partage | Bénéfice utilisateur | P2 |
| Auto | Android albums | Trop vague | Automatique | Clarifie l'état | P3 |
| Préparer des suggestions | iOS AI | Bon bénéfice utilisateur | Préparer des suggestions | À garder | P0 keep |
| AI / WakeveAI | iOS code-facing strings | Ne doit pas apparaître en UI visible | Suggestions Wakeve si visible | Ne pas mettre l'IA au centre | P1 |
| Tableau de bord | iOS profile/dashboard | Peut paraître outil professionnel | Activité ou Aperçu | À décider selon usage réel; garder Dashboard si analytics avancés | P2 |
| Moy. / évt | iOS dashboard | Abrégé, peu naturel | Moyenne / événement | Lisible, surtout accessibilité | P2 |
| Taux rép. | iOS dashboard | Abréviation obscure | Taux de réponse | Plus clair | P1 |
| Bloqué | iOS next action | Trop anxiogène si action possible | À compléter | Moins anxiogène et plus actionnable | P1 |
| Prévisualisation organisateur | iOS status | Long et technique | Brouillon organisateur | Plus court et cohérent | P2 |
| Finalisé | Event status | Statut métier clair | Finalisé | À garder pour événement prêt | P0 keep |
| Terminé | General done | Peut être ambigu pour publication | Terminé / Fermer / Enregistrer selon contexte | Tester chaque bouton dans une phrase | P2 |
| Tout lire | Android notification action | Peut signifier ouvrir tout, pas marquer lu | Tout marquer comme lu | Résultat explicite | P1 |
| Read all | iOS notification action | Same ambiguity | Mark all as read | Result-oriented | P1 |
| Deadline approche | Server FR notification | Anglicisme | Échéance proche | Français naturel | P1 |
| Vote now! | Server EN notification | Trop pressant | Vote when you can | Moins injonctif; contexte social | P2 |
| C'est aujourd'hui ! | Server FR notification | Correct pour émotion | C'est aujourd'hui | Retirer exclamation si notification critique | P3 |
| Explorer / Explorateur badge | Server/iOS gamification | Peut entrer en conflit avec tab Explorer | Découvreur ou Curieux | Éviter collision conceptuelle | P3 |
| Master / Maître du Vote / Party Animal | iOS gamification | Ton incohérent et difficile à localiser | Badges sobres: Organisateur fiable, Vote régulier | Plus premium et plus localisable | P2 |
| Suppression du compte | Data management | Clair et critique | Supprimer le compte | À garder avec message explicite | P0 keep |
| Signaler ou bloquer | Moderation | Clair | Signaler ou bloquer | À garder | P0 keep |

## Think / Feel / Do

### Créer Un Événement
- Think: c'est simple, je peux commencer vite, je pourrai modifier ensuite.
- Feel: calme, confiance, envie d'inviter.
- Do: nommer l'idée, proposer des créneaux, inviter, lancer le sondage.
- Best label: `Créer un événement` / `Create event`.
- Avoid as primary CTA: `Créer un moment`, `Lancer une invitation`, `Organiser une sortie`.

### Comparer Les Options
- Think: je vais voir plusieurs choix concrets.
- Feel: contrôle, clarté, décision collective.
- Do: comparer destination, logement, budget, dates, préférences.
- Best label: `Comparer les options` / `Compare options`.
- Avoid in user UI: `Comparer les scénarios`.

### Confirmer Une Date
- Think: cette date devient la référence.
- Feel: responsabilité, confiance.
- Do: verrouiller la date et ouvrir l'organisation.
- Best label: `Confirmer la date` / `Confirm date`.
- Avoid: `Valider`, `Terminer`, `Publier`.

## Centralisation Recommandée

- Android: continuer à utiliser `composeApp/src/androidMain/res/values*/strings.xml`; remplacer les hardcoded labels de navigation par des ressources.
- iOS: continuer à utiliser `Localizable.strings`; remplacer `WakeveTab.title` hardcodé par des clés localisées.
- Shared/server: conserver les clés serveur mais aligner le wording FR/EN avec ce glossaire.
- AI: exposer des labels de bénéfice (`Préparer`, `Suggérer`, `Résumer`) plutôt que des labels de mécanisme (`Générer`, `Assistant IA`).

## Prochaines Décisions À Surveiller

- Confirmer si `Explorer` reste une tab produit ou devient `Idées`.
- Confirmer si `Groupes` représente des groupes persistants ou seulement les participants d'un événement.
- Décider si la gamification reste visible dans l'app premium ou passe derrière un ton plus discret.
- Décider si `I'm in` remplace `Going` dans les CTA anglais RSVP.
- Établir une règle stricte pour `Options` visible vs `Scenario` interne.
